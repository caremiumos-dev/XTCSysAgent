package com.xtc.systemagent.module.log.serve.impl;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xtc.architecture.mvp.BasePresenter;
import com.xtc.log.LogUtil;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.module.Constants;
import com.xtc.systemagent.module.log.bean.ModemLogStatus;
import com.xtc.systemagent.module.log.serve.IModemLogServe;
import com.xtc.systemagent.net.bean.ExtraMes;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import com.xtc.systemagent.util.BatteryMonitor;
import com.xtc.systemagent.util.LmkPersistHelper;
import com.xtc.systemagent.util.LogCollectUtil;
import com.xtc.systemagent.util.NetworkUtil;
import com.xtc.systemagent.util.SdCardUtil;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.systemagent.util.UploadFile;
import com.xtc.systemagent.util.WifiHelper;
import com.xtc.utils.common.CollectionUtils;
import com.xtc.utils.date.DateUtils;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Modem 日志抓取与上传实现。
 *
 * <p>状态机见 {@link ModemLogStatus.Status}：
 * 监控(1) → 抓取(2) → 待上传(3) → 上传失败(4)/上传成功(5)。</p>
 *
 * <p>抓取由 {@link BatteryMonitor}（按电量/时长条件）或立刻开始的闹钟触发；
 * 关闭由 {@link #MODEM_LOG_CLOSE_ALARM} 闹钟触发；上传失败后由
 * {@link #MODEM_LOG_RETRY_UPLOAD} 闹钟按指数退避重试，最多 10 次。</p>
 *
 * <p>本类同时监听网络变化：一旦网络恢复且当时处于待上传/上传失败状态，
 * 立即取消重试闹钟并重新尝试上传。</p>
 */
public class ModemLogServeImpl extends BasePresenter<Object>
        implements IModemLogServe, WifiHelper.ConnectivityCallback {

    private static final String TAG = "Log_ModemLogServeImpl";

    /** 归档日志的扩展名（modem 原始日志目录 + tcpdump + system_log 一并打包）。 */
    private static final String LOGS_COLLECTOR_FILE_TYPE = ".tar.gz";

    /** 到点关闭 modem 日志抓取的闹钟 action。 */
    private static final String MODEM_LOG_CLOSE_ALARM = "com.xtc.i3launcher.ACTION.ModemLogClose";

    /** 上传失败后重试的闹钟 action。 */
    private static final String MODEM_LOG_RETRY_UPLOAD = "com.xtc.i3launcher.ACTION.ModemLogRetryUpload";

    /** 重试上传的最大次数。 */
    private static final int MAX_RETRY_UPLOAD_NUM = 10;

    /** 上传网络限制：仅 WiFi。 */
    private static final int UPLOAD_NET_WIFI = 2;

    /**
     * 是否由本地（用户在手表上主动触发）发起的抓取。
     * 本地抓取不受服务端策略约束，也不会向上报状态。
     */
    private static boolean isLocalOperating = false;

    private static SoftReference<ModemLogServeImpl> modemLogServeImplRef;

    private final Context mContext;
    private final AlarmManager am;

    private PendingIntent closePI;
    private BroadcastReceiver closeReceiver;

    private PendingIntent retryPI;
    private BroadcastReceiver retryReceiver;

    private PowerManager.WakeLock wakeLock;
    private WifiHelper wifiHelper;

    private boolean mobConnected = false;
    private boolean wifiConnected = false;
    private boolean uploading = false;

    private ModemLogServeImpl(Context context) {
        super(context);
        this.mContext = context;
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            LogUtil.e(TAG, "PowerManager is null !");
        } else {
            this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            this.wakeLock.setReferenceCounted(false);
        }
    }

    public static synchronized ModemLogServeImpl getInstance(Context context) {
        ModemLogServeImpl instance = modemLogServeImplRef == null ? null : modemLogServeImplRef.get();
        if (instance == null) {
            instance = new ModemLogServeImpl(context);
            modemLogServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // 唤醒锁
    // ------------------------------------------------------------------

    /** 长时间持有 CPU 锁（抓取/压缩阶段）。 */
    private void acquireCpuForLong() {
        LogUtil.d(TAG, "Modem Log acquireCpuForLong");
        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        this.wakeLock.acquire();
    }

    /** 短暂持有 CPU 锁 3s（用于写系统属性后等待生效）。 */
    private void wakeUpCpu() {
        LogUtil.d(TAG, "Modem Log Wake Cpu");
        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        this.wakeLock.acquire(3000L);
    }

    private void releaseWakeUp() {
        LogUtil.d(TAG, "release Wake Up");
        if (this.wakeLock == null || !this.wakeLock.isHeld()) {
            return;
        }
        this.wakeLock.release();
    }

    // ------------------------------------------------------------------
    // 闹钟
    // ------------------------------------------------------------------

    private void cancelCloseAlarm() {
        LogUtil.d(TAG, "cancelCloseAlarm");
        if (this.closeReceiver != null) {
            this.mContext.unregisterReceiver(this.closeReceiver);
        }
        if (this.am != null && this.closePI != null) {
            this.am.cancel(this.closePI);
        }
        this.closeReceiver = null;
    }

    private void cancelRetryAlarm() {
        LogUtil.d(TAG, "cancelRetryAlarm");
        if (this.retryReceiver != null) {
            this.mContext.unregisterReceiver(this.retryReceiver);
        }
        if (this.am != null && this.retryPI != null) {
            this.am.cancel(this.retryPI);
        }
        this.retryReceiver = null;
    }

    /**
     * 注册重试闹钟。
     *
     * @param delayMinutes 延迟分钟数
     */
    private void startRetryAlarm(long delayMinutes) {
        LogUtil.i(TAG, "startRetryAlarm = " + delayMinutes);
        registerRetryReceiver();
        this.retryPI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(MODEM_LOG_RETRY_UPLOAD), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (60 * delayMinutes * 1000), this.retryPI);
    }

    private void registerCloseReceiver() {
        LogUtil.i(TAG, "registerCloseReceiver");
        this.closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "closeReceiver , action = " + action);
                if (MODEM_LOG_CLOSE_ALARM.equals(action)) {
                    closeModemLog();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(MODEM_LOG_CLOSE_ALARM);
        this.mContext.registerReceiver(this.closeReceiver, filter);
    }

    private void registerRetryReceiver() {
        this.retryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "retryReceiver " + action);
                if (action.equals(MODEM_LOG_RETRY_UPLOAD)) {
                    cancelRetryAlarm();
                    String strategy = SharedTool.getModemLogStrategy(mContext);
                    String status = SharedTool.getModemLogStatus(mContext);
                    if (isOnTheWay(strategy, status, true, true, false)) {
                        preUploadModemLogFile(JSONUtil.fromJSON(strategy, LogStrategy.class),
                                JSONUtil.fromJSON(status, ModemLogStatus.class));
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(MODEM_LOG_RETRY_UPLOAD);
        this.mContext.registerReceiver(this.retryReceiver, filter);
    }

    // ------------------------------------------------------------------
    // 网络监听
    // ------------------------------------------------------------------

    private void registerNetChange() {
        LogUtil.i(TAG, "registerNetChange");
        this.wifiHelper = WifiHelper.getInstance(this.mContext);
        this.wifiHelper.setConnectivityCallback(this);
    }

    private void unRegisterNetChange() {
        LogUtil.i(TAG, "unRegisterNetChange");
        if (this.wifiHelper != null) {
            this.wifiHelper.removeConnectivityCallback(this);
        }
    }

    @Override
    public void onConnectivityChanged(boolean mobConnected, boolean wifiConnected) {
        LogUtil.d(TAG, "dealNetworkChange mobConnected = " + mobConnected
                + "---wifiConnected = " + wifiConnected
                + "---this.mobConnected = " + this.mobConnected
                + "---this.wifiConnected = " + this.wifiConnected
                + "---uploading = " + this.uploading);
        if ((mobConnected || wifiConnected)
                && !((mobConnected == this.mobConnected && wifiConnected == this.wifiConnected)
                || this.uploading)) {
            String strategy = SharedTool.getModemLogStrategy(this.mContext);
            String status = SharedTool.getModemLogStatus(this.mContext);
            if (isOnTheWay(strategy, status, false, false, false)) {
                LogUtil.d(TAG, "dealNetworkChange mobConnected strategy = " + strategy
                        + ",status = " + status);
                LogStrategy logStrategy = JSONUtil.fromJSON(strategy, LogStrategy.class);
                ModemLogStatus modemLogStatus = JSONUtil.fromJSON(status, ModemLogStatus.class);
                if ((modemLogStatus.getCurrentStatus() == ModemLogStatus.Status.PRE_UPLOAD
                        || modemLogStatus.getCurrentStatus() == ModemLogStatus.Status.UPLOAD_FAILURE)
                        && modemLogStatus.getCurrentRetryUploadNum() < MAX_RETRY_UPLOAD_NUM) {
                    cancelRetryAlarm();
                    preUploadModemLogFile(logStrategy, modemLogStatus);
                }
            }
        } else {
            LogUtil.d(TAG, "不符合 上传条件 , pass");
        }
        this.mobConnected = mobConnected;
        this.wifiConnected = wifiConnected;
    }

    // ------------------------------------------------------------------
    // 状态与清理
    // ------------------------------------------------------------------

    private void clearLastModemLogStrategy() {
        LogUtil.i(TAG, "clearLastModemLogStrategy");
        SharedTool.saveModemLogStrategy(this.mContext, "");
        SharedTool.saveModemLogStatus(this.mContext, "");
    }

    private void clearOldFile(boolean closeSwitch, boolean deleteZip) {
        LogUtil.d(TAG, "clearOldFile closeSwitch = " + closeSwitch);
        SharedTool.saveModemLogOverTime(this.mContext, "");
        clearOldFile(closeSwitch, true, deleteZip, false);
    }

    /**
     * 清理上一轮的残留。
     *
     * @param closeSwitch 是否同时关闭 modem 日志开关
     * @param clearFile   是否删除原始日志目录
     * @param deleteZip   是否删除 zip 目录
     * @param isReboot    是否为开机场景（此时 modem 自启动状态下不清理）
     */
    private void clearOldFile(boolean closeSwitch, boolean clearFile, boolean deleteZip, boolean isReboot) {
        LogUtil.d(TAG, "clearOldFile closeSwitch = " + closeSwitch
                + "---clearFile = " + clearFile + "--deleteZip = " + deleteZip);
        if (isReboot) {
            String modemSelf = SystemPropertiesUtils.get(Constants.SystemProperty.MODEM_SELF, "");
            boolean modemSelfOn = !TextUtils.isEmpty(modemSelf) && modemSelf.equals("1");
            LogUtil.i(TAG, "modemSelfStatus = " + modemSelfOn);
            if (modemSelfOn) {
                return;
            }
        }
        if (closeSwitch) {
            setModemLog(false);
        }
        if (!clearFile || isReboot) {
            return;
        }
        UploadFile.deleteFile(Constants.ModemLogPath.MODEM_LOG_FILE_PATH, getClass().getSimpleName());
        if (deleteZip) {
            UploadFile.deleteFile(Constants.ModemLogPath.ZIP_FILE_PATH, getClass().getSimpleName());
        }
    }

    /**
     * 开关 modem 日志，并同步控制进程是否被 LMK 保活。
     *
     * @param open true 打开
     */
    private void setModemLog(boolean open) {
        String current = SystemPropertiesUtils.get(SystemConstants.PROP_LOG_TAG_MODEM, "");
        boolean currentOn = !TextUtils.isEmpty(current) && current.equals("1");
        LogUtil.d(TAG, "setModemLog currentStatus = " + currentOn + "---isOpen = " + open);
        if (currentOn != open) {
            wakeUpCpu();
            SystemPropertiesUtils.set(SystemConstants.PROP_LOG_TAG_MODEM, open ? 1 : 0);
        }
        if (open) {
            LmkPersistHelper.registerLmkPersist(this.mContext.getPackageName());
        } else {
            LmkPersistHelper.unregisterLmkPersist(this.mContext.getPackageName());
        }
    }

    private void saveCurrentStatus(ModemLogStatus status, int currentStatus) {
        LogUtil.i(TAG, "saveCurrentStatus = " + currentStatus);
        status.setCurrentStatus(currentStatus);
        SharedTool.saveModemLogStatus(this.mContext, JSONUtil.toJSON(status));
    }

    /**
     * 判断上一轮任务是否还在进行中（策略和状态都存在即为进行中）。
     *
     * <p>只要有一个为空就认为没有任务，并顺手把残留清理掉。</p>
     */
    private boolean isOnTheWay(String strategy, String status, boolean closeSwitch,
                              boolean clearFile, boolean isReboot) {
        LogUtil.d(TAG, "isOnTheWay strategy = " + strategy + ", status = " + status
                + ", closeSwitch = " + closeSwitch + ", clearFile = " + clearFile
                + ", isReboot = " + isReboot);
        if (!TextUtils.isEmpty(strategy) && !TextUtils.isEmpty(status)) {
            LogUtil.i(TAG, "return true !");
            return true;
        }
        LogUtil.w(TAG, "return false ! at lease one value is empty , reset something !");
        clearLastModemLogStrategy();
        clearOldFile(closeSwitch, clearFile, true, isReboot);
        return false;
    }

    // ------------------------------------------------------------------
    // 抓取流程
    // ------------------------------------------------------------------

    private void startMonitoring(LogStrategy strategy, ExtraMes extra, ModemLogStatus status) {
        saveCurrentStatus(status, ModemLogStatus.Status.MONITORING);
        BatteryMonitor.getInstance(this.mContext).startBatteryMonitor(true, strategy, extra);
    }

    /** 进入下一轮监控：抓取次数 +1，重试计数清零。 */
    private void startNextMonitoring(LogStrategy strategy, ExtraMes extra, ModemLogStatus status) {
        status.setCurrentFetchingTime(status.getCurrentFetchingTime() + 1);
        status.setCurrentRetryUploadNum(0);
        status.setCurrentRetryUploadTime(0L);
        startMonitoring(strategy, extra, status);
    }

    /**
     * 开始抓取 modem 日志（立刻开始的场景）。
     *
     * @param strategy     策略；logId 为 -1 表示本地触发
     * @param fetchingTime 抓取分钟数
     */
    @Override
    public void startModemLog(LogStrategy strategy, int fetchingTime) {
        Long logId = strategy.getLogId();
        LogUtil.i(TAG, "startModemLog , logId = " + logId + " , duration = " + fetchingTime);

        // 存储空间不足时直接上报错误码，不启动抓取。
        String checkResult = SdCardUtil.execNormalCheck(Constants.LogCollect.MIN_REMAIN_STORAGE_SPACE);
        LogUtil.d(TAG, "checkString = " + checkResult);
        if (!TextUtils.isEmpty(checkResult)) {
            LogCollectUtil.updateCollectState(this.mContext, logId.longValue(),
                    Integer.valueOf(checkResult.split("-")[0]).intValue());
            return;
        }

        clearOldFile(false, true);
        registerCloseReceiver();
        setModemLog(true);
        this.closePI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(MODEM_LOG_CLOSE_ALARM), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ((long) (fetchingTime * 60 * 1000)), this.closePI);

        String strategyJson = SharedTool.getModemLogStrategy(this.mContext);
        String statusJson = SharedTool.getModemLogStatus(this.mContext);
        if (isOnTheWay(strategyJson, statusJson, false, true, false)) {
            LogUtil.i(TAG, "is fetching");
            LogCollectUtil.updateCollectState(this.mContext,
                    JSONUtil.fromJSON(strategyJson, LogStrategy.class).getLogId().longValue(), 4);
            saveCurrentStatus(JSONUtil.fromJSON(statusJson, ModemLogStatus.class),
                    ModemLogStatus.Status.FETCHING);
        }
    }

    /** 到点关闭抓取，并把已抓到的内容转入上传流程。 */
    private void closeModemLog() {
        LogUtil.i(TAG, "closeModemLog");
        SharedTool.saveModemLogOverTime(this.mContext, DateUtils.format("yyyyMMdd"));
        cancelCloseAlarm();
        setModemLog(false);
        TcpDumpServeImpl.getInstance(this.mContext).whenModemLogPrepareToUpload();

        String strategyJson = SharedTool.getModemLogStrategy(this.mContext);
        String statusJson = SharedTool.getModemLogStatus(this.mContext);
        if (isOnTheWay(strategyJson, statusJson, false, false, false)) {
            ModemLogStatus status = JSONUtil.fromJSON(statusJson, ModemLogStatus.class);
            saveCurrentStatus(status, ModemLogStatus.Status.PRE_UPLOAD);
            preUploadModemLogFile(JSONUtil.fromJSON(strategyJson, LogStrategy.class), status);
        } else {
            // 本地抓取没有策略，构造一个 logId = -1 的占位策略走同一条上传链路。
            LogStrategy localStrategy = new LogStrategy();
            localStrategy.setLogId(-1L);
            ModemLogStatus status = new ModemLogStatus();
            status.setCurrentStatus(ModemLogStatus.Status.PRE_UPLOAD);
            preUploadModemLogFile(localStrategy, status);
        }
    }

    // ------------------------------------------------------------------
    // 上传流程
    // ------------------------------------------------------------------

    /**
     * 上传前的网络检查：策略要求 WiFi 就必须 WiFi，否则只要有网即可。
     * 不满足时通知服务端并进入重试。
     */
    private void preUploadModemLogFile(LogStrategy strategy, ModemLogStatus status) {
        LogUtil.d(TAG, "preUploadModemLogFile logStrategy = " + strategy
                + "---modemLogStatus = " + status);
        if (strategy.getUploadNet() == UPLOAD_NET_WIFI) {
            if (NetworkUtil.isWifiConnected(this.mContext)) {
                LogUtil.i(TAG, "net is ok");
                uploadModemLogFile(strategy, status);
                return;
            }
            LogUtil.e(TAG, "preUploadModemLogFile Network is not Wifi");
        } else {
            if (NetworkUtil.isConnected(this.mContext)) {
                LogUtil.i(TAG, "===net is ok====");
                uploadModemLogFile(strategy, status);
                return;
            }
            LogUtil.e(TAG, "preUploadModemLogFile Network is not Connected");
        }
        LogStateServeImpl.getInstance(this.mContext).notifyErrorInfo(strategy,
                "network not matched! retry it.");
        retryUploadModemLog(strategy, status);
    }

    /**
     * 上传失败后的重试调度（指数退避，最多 {@link #MAX_RETRY_UPLOAD_NUM} 次）。
     *
     * <p>若本地已记录了下一次重试时间且还没到，就直接按剩余时间重新挂闹钟，
     * 避免重复累加。</p>
     */
    private void retryUploadModemLog(@NonNull LogStrategy strategy, @NonNull ModemLogStatus status) {
        LogUtil.d(TAG, "retryUploadModemLog logStrategy = " + strategy
                + "---modemLogStatus = " + status);
        if (status.getCurrentRetryUploadNum() >= MAX_RETRY_UPLOAD_NUM) {
            LogUtil.d(TAG, "retryUploadModemLog Retry End");
            dealUploadFailure(strategy, status);
            return;
        }

        long now = System.currentTimeMillis();
        if (status.getCurrentRetryUploadNum() != 0 && status.getCurrentRetryUploadTime() > now) {
            long remainMinutes = ((status.getCurrentRetryUploadTime() - now) / 60) / 1000;
            LogUtil.d(TAG, "retryUploadModemLog durationMinutes = " + remainMinutes);
            startRetryAlarm(remainMinutes);
            return;
        }

        int nextNum = status.getCurrentRetryUploadNum() + 1;
        long nextDelayMinutes = UploadFile.getNextRetryUploadTime(nextNum);
        long nextTime = now + (60 * nextDelayMinutes * 1000);
        LogUtil.d(TAG, "retryUploadModemLog nextNum = " + nextNum + "---nextTime = " + nextTime
                + "---durationMinutes = " + nextDelayMinutes);
        status.setCurrentRetryUploadNum(nextNum);
        status.setCurrentRetryUploadTime(nextTime);
        saveCurrentStatus(status, status.getCurrentStatus());
        startRetryAlarm(nextDelayMinutes);
    }

    /**
     * 上传失败的处理：清残留、上报失败状态；若还有剩余抓取次数就进入下一轮监控。
     */
    private void dealUploadFailure(@NonNull LogStrategy strategy, @NonNull ModemLogStatus status) {
        clearOldFile(true, true);
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 2);
        if (status.getCurrentFetchingTime() < status.getFetchingTime()) {
            startNextMonitoring(strategy, strategy.getExtra(), status);
        } else {
            unRegisterNetChange();
            LogUtil.i(TAG, "fetchingTime is used up");
        }
    }

    /** 单个文件上传成功后的收尾。 */
    private void dealUploadFileSuccess(LogStrategy strategy, ModemLogStatus status) {
        LogUtil.i(TAG, "dealUploadFileSuccess");
        if (status.getCurrentFetchingTime() >= status.getFetchingTime()) {
            unRegisterNetChange();
            clearLastModemLogStrategy();
            TcpDumpServeImpl.getInstance(this.mContext).whenUploadModemLogSucceed(false);
        } else {
            LogUtil.i(TAG, "start next monitor");
            startNextMonitoring(strategy, strategy.getExtra(), status);
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 3);
            TcpDumpServeImpl.getInstance(this.mContext).whenUploadModemLogSucceed(true);
        }
        releaseWakeUp();
    }

    /** 上报上传结果给服务端。 */
    private void notifyUploadSuccess(String key, LogStrategy strategy, int collectStatus,
                                     int spaceType,
                                     LogStateServeImpl.UploadSuccessCallback callback) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        LogUtil.i(TAG, "notifyUploadSuccess logStrategy = " + strategy);
        if (TextUtils.isEmpty(bindNumber)) {
            LogUtil.e(TAG, "bindNumber is null !");
            return;
        }
        UploadNotifyParam param = new UploadNotifyParam();
        param.setAccountId(bindNumber);
        param.setKey(key);
        param.setSpaceType(Integer.valueOf(spaceType));
        if (strategy.getLogId().longValue() != -1) {
            param.setAppPackage(strategy.getAppPackage());
            param.setId(strategy.getLogId());
            param.setStrategyId(strategy.getId());
            param.setOperatorId(strategy.getUserId());
        } else {
            param.setAppPackage(Constants.LogStrategyPackageName.PACKAGE_NAME_MODEM_LOG);
        }
        param.setMachineId(SharedTool.getMachineId(this.mContext));
        param.setCollectStatus(Integer.valueOf(collectStatus));
        LogStateServeImpl.getInstance(this.mContext).notifyUploadSuccess(param, callback);
    }

    /**
     * 归档目录，供 modem 日志一起打包。
     *
     * <p>若上一次抓取跨天（记录的日期不是今天）且旧归档文件还在，就用旧的。</p>
     */
    private String getLogsCollectorPath() {
        String today = DateUtils.format("yyyyMMdd");
        String overTime = SharedTool.getModemLogOverTime(this.mContext);
        if (!TextUtils.isEmpty(overTime) && !TextUtils.isEmpty(today) && !overTime.equals(today)) {
            LogUtil.i(TAG, "over one day");
            String oldPath = SdCardUtil.getSystemLogDirPrefix() + overTime + LOGS_COLLECTOR_FILE_TYPE;
            if (new File(oldPath).exists()) {
                LogUtil.i(TAG, "file is exist");
                return oldPath;
            }
        }
        return SdCardUtil.getSystemLogDirPrefix() + today;
    }

    /**
     * 上传 modem 日志。
     *
     * <p>状态 3（待上传）表示原始目录还在，需要先压缩再上传；
     * 状态 4（上传失败）表示 zip 已经存在，直接重传即可。</p>
     */
    private void uploadModemLogFile(final LogStrategy strategy, final ModemLogStatus status) {
        LogUtil.i(TAG, "uploadModemLogFile");
        File originFile = new File(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
        File zipFile = new File(Constants.ModemLogPath.ZIP_FILE_PATH);
        if (originFile.exists()) {
            LogUtil.i(TAG, "modem origin file is exists");
        } else if (zipFile.exists()) {
            LogUtil.i(TAG, "modem zip file is exists");
            saveCurrentStatus(status, ModemLogStatus.Status.UPLOAD_FAILURE);
        }

        if (status.getCurrentStatus() == ModemLogStatus.Status.UPLOAD_FAILURE) {
            // 已有 zip，直接重传。
            this.uploading = true;
            LogUtil.d(TAG, "uploadModemLogFile uploadFile currentStatus = " + status.getCurrentStatus()
                    + "---zipFileName = " + status.getZipFileName());
            UploadFile.dealUploadFile(this.mContext, status.getZipFileName(),
                    Constants.ModemLogPath.ZIP_FILE_PATH, true, new UploadFile.UploadFileCallback() {
                        @Override
                        public void onNoLogFile() {
                            LogUtil.d(TAG, "onNoLogFile");
                            dealUploadFailure(strategy, status);
                            releaseWakeUp();
                            uploading = false;
                        }

                        @Override
                        public void onUploadFileFailure(String error) {
                            LogUtil.e(TAG, "uploadModemLogFile onUploadFileFailure " + error);
                            saveCurrentStatus(status, ModemLogStatus.Status.UPLOAD_FAILURE);
                            retryUploadModemLog(strategy, status);
                            releaseWakeUp();
                            uploading = false;
                        }

                        @Override
                        public void onUploadFileSuccess(String key) {
                            LogUtil.d(TAG, "uploadModemLogFile onUploadFileSuccess " + key);
                            saveCurrentStatus(status, ModemLogStatus.Status.UPLOAD_SUCCESS);
                            notifyUploadSuccess(key, strategy, 1, 1,
                                    new LogStateServeImpl.UploadSuccessCallback() {
                                        @Override
                                        public void onFailure() {
                                            dealUploadFileSuccess(strategy, status);
                                            uploading = false;
                                        }

                                        @Override
                                        public void onSuccess() {
                                            dealUploadFileSuccess(strategy, status);
                                            uploading = false;
                                        }
                                    });
                        }

                        @Override
                        public void onZipFileFailure(String error) {
                            LogUtil.e(TAG, "uploadModemLogFile onZipFileFailure " + error);
                            dealUploadFailure(strategy, status);
                            releaseWakeUp();
                            uploading = false;
                        }
                    });
            return;
        }

        if (status.getCurrentStatus() != ModemLogStatus.Status.PRE_UPLOAD) {
            LogUtil.e(TAG, "uploadModemLogFile is not fetching");
            return;
        }

        this.uploading = true;
        acquireCpuForLong();

        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            bindNumber = "NoBindNumber";
        }
        String fileName;
        if (strategy.getLogId().longValue() == -1) {
            fileName = "MODEM_LOG_" + bindNumber + "_" + System.currentTimeMillis() + "_Local.zip";
        } else {
            fileName = "MODEM_LOG_" + bindNumber + "_" + strategy.getLogId() + "_"
                    + System.currentTimeMillis() + "_" + status.getCurrentFetchingTime() + ".zip";
        }
        final String zipFileName = fileName;
        LogUtil.d(TAG, "uploadModemLogFile uploadZipFile currentStatus = " + status.getCurrentStatus()
                + "---zipFileName = " + zipFileName);

        ArrayList<String> paths = new ArrayList<>();
        paths.add(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
        paths.add(SdCardUtil.getTcpDumpFilePath());
        paths.add(getLogsCollectorPath());

        UploadFile.uploadZipFile(this.mContext, paths, zipFileName,
                Constants.ModemLogPath.ZIP_FILE_PATH, true, new UploadFile.UploadFileCallback() {
                    @Override
                    public void onNoLogFile() {
                        LogUtil.d(TAG, "onNoLogFile");
                        dealUploadFailure(strategy, status);
                        releaseWakeUp();
                        uploading = false;
                    }

                    @Override
                    public void onUploadFileFailure(String error) {
                        LogUtil.e(TAG, "uploadModemLogFile onUploadFileFailure " + error);
                        status.setZipFileName(zipFileName);
                        saveCurrentStatus(status, ModemLogStatus.Status.UPLOAD_FAILURE);
                        retryUploadModemLog(strategy, status);
                        releaseWakeUp();
                        uploading = false;
                    }

                    @Override
                    public void onUploadFileSuccess(String key) {
                        LogUtil.d(TAG, "uploadModemLogFile onUploadFileSuccess " + key);
                        saveCurrentStatus(status, ModemLogStatus.Status.UPLOAD_SUCCESS);
                        notifyUploadSuccess(key, strategy, 1, 1,
                                new LogStateServeImpl.UploadSuccessCallback() {
                                    @Override
                                    public void onFailure() {
                                        LogUtil.i(TAG, "onFailure");
                                        dealUploadFileSuccess(strategy, status);
                                        uploading = false;
                                    }

                                    @Override
                                    public void onSuccess() {
                                        LogUtil.i(TAG, "onSuccess");
                                        dealUploadFileSuccess(strategy, status);
                                        uploading = false;
                                    }
                                });
                    }

                    @Override
                    public void onZipFileFailure(String error) {
                        LogUtil.e(TAG, "uploadModemLogFile onZipFileFailure " + error);
                        dealUploadFailure(strategy, status);
                        releaseWakeUp();
                        uploading = false;
                    }
                });
    }

    // ------------------------------------------------------------------
    // 开机/恢复
    // ------------------------------------------------------------------

    /** 处理上一次没做完的任务。 */
    private void whenHasOldTask(String strategyJson, String statusJson) {
        LogUtil.d(TAG, "whenHasOldTask -->");
        LogStrategy strategy = JSONUtil.fromJSON(strategyJson, LogStrategy.class);
        ModemLogStatus status = JSONUtil.fromJSON(statusJson, ModemLogStatus.class);
        ExtraMes extra = strategy.getExtra();
        switch (status.getCurrentStatus()) {
            case ModemLogStatus.Status.MONITORING:
                clearOldFile(true, true);
                BatteryMonitor.getInstance(this.mContext).startBatteryMonitor(true, strategy, extra);
                break;
            case ModemLogStatus.Status.FETCHING:
                clearOldFile(true, true);
                if (!extra.getIsImmediately()) {
                    startMonitoring(strategy, extra, status);
                } else {
                    startModemLog(strategy, extra.getFetchingDuration());
                }
                break;
            case ModemLogStatus.Status.PRE_UPLOAD:
            case ModemLogStatus.Status.UPLOAD_FAILURE:
                retryUploadModemLog(strategy, status);
                break;
            default:
                clearOldFile(true, true);
                if (status.getCurrentFetchingTime() >= status.getFetchingTime()) {
                    clearLastModemLogStrategy();
                } else {
                    startNextMonitoring(strategy, extra, status);
                }
                break;
        }
        registerNetChange();
    }

    /** 本地压缩遗留的 modem 日志（不涉及服务端）。 */
    private void zipDirectory() {
        LogUtil.i(TAG, "zipDirectory");
        acquireCpuForLong();
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            bindNumber = "NoBindNumber";
        }
        String zipFileName = "MODEM_LOG_" + bindNumber + "_" + System.currentTimeMillis() + "_Local.zip";
        LogUtil.d(TAG, "zipDirectory uploadZipFile currentStatus = ---zipFileName = " + zipFileName);

        ArrayList<String> paths = new ArrayList<>();
        paths.add(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
        paths.add(SdCardUtil.getTcpDumpFilePath());

        UploadFile.zipDirectory(this.mContext, paths, zipFileName,
                Constants.ModemLogPath.ZIP_FILE_PATH, true, new UploadFile.ZipFileCallback() {
                    @Override
                    public void onNoLogFile() {
                        LogUtil.e(TAG, "zipDirectory onNoLogFile");
                        TcpDumpServeImpl.getInstance(mContext).whenLocalModemLogDone();
                        releaseWakeUp();
                    }

                    @Override
                    public void onZipFileFailure(String error) {
                        LogUtil.e(TAG, "zipDirectory onZipFileFailure " + error);
                        if (isOnTheWay(SharedTool.getModemLogStrategy(mContext),
                                SharedTool.getModemLogStatus(mContext), true, false, false)) {
                            TcpDumpServeImpl.getInstance(mContext).whenLocalModemLogDone();
                            clearOldFile(true, true);
                            releaseWakeUp();
                        } else {
                            LogUtil.i(TAG, "is local fetch modem log");
                            UploadFile.deleteFile(Constants.ModemLogPath.MODEM_LOG_FILE_PATH,
                                    getClass().getSimpleName());
                            releaseWakeUp();
                        }
                    }

                    @Override
                    public void onZipFileSuccess(String zipName, String zipPath) {
                        LogUtil.d(TAG, "zipDirectory onZipFileSuccess zipFileName " + zipName
                                + "---zipFilePath = " + zipPath);
                        TcpDumpServeImpl.getInstance(mContext).whenLocalModemLogDone();
                        releaseWakeUp();
                    }
                });
    }

    // ------------------------------------------------------------------
    // IModemLogServe
    // ------------------------------------------------------------------

    @Override
    public void cancelModemLog() {
        if (!TextUtils.isEmpty(SharedTool.getModemLogStatus(this.mContext))) {
            LogUtil.e(TAG, "cancelModemLog last ModemLog on the way");
            return;
        }
        isLocalOperating = false;
        cancelCloseAlarm();
        cancelRetryAlarm();
        clearOldFile(true, true);
        BatteryMonitor.getInstance(this.mContext).cancelBatteryMonitor();
        TcpDumpServeImpl.getInstance(this.mContext).cancelTcpDump();
    }

    @Override
    public void cancelModemLog(List<LogStrategy> strategies) {
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty");
            return;
        }
        isLocalOperating = false;
        LogUtil.d(TAG, "cancelModemLog logStrategy = " + strategies.get(0));
        clearLastModemLogStrategy();
        unRegisterNetChange();
        cancelCloseAlarm();
        cancelRetryAlarm();
        clearOldFile(true, true);
        BatteryMonitor.getInstance(this.mContext).cancelBatteryMonitor();
        TcpDumpServeImpl.getInstance(this.mContext).cancelTcpDump(strategies);
    }

    @Override
    public void checkLastModemLog(boolean isLauncherStart) {
        LogUtil.i(TAG, "checkLastModemLog ----------------------> isLauncherStart = " + isLauncherStart);
        String strategy = SharedTool.getModemLogStrategy(this.mContext);
        String status = SharedTool.getModemLogStatus(this.mContext);
        if (isOnTheWay(strategy, status, false, false, isLauncherStart)) {
            whenHasOldTask(strategy, status);
        } else {
            LogUtil.d(TAG, "no previous task , isLocalOperating = " + isLocalOperating);
            if (!isLocalOperating && !isLauncherStart) {
                zipDirectory();
            }
        }
        LogUtil.i(TAG, "checkLastModemLog done ~");
    }

    @Override
    public void dealCallIdle() {
        String strategyJson = SharedTool.getModemLogStrategy(this.mContext);
        LogUtil.d(TAG, "dealCallIdle strategy = " + strategyJson);
        if (TextUtils.isEmpty(strategyJson)) {
            LogUtil.d(TAG, "dealCallIdle local do nothing!");
            return;
        }
        LogStrategy strategy = JSONUtil.fromJSON(strategyJson, LogStrategy.class);
        if (strategy == null || strategy.getExtra() == null || !strategy.getExtra().getIsImmediately()) {
            checkLastModemLog(false);
        }
    }

    @Override
    public void dealCallRinging() {
        String strategyJson = SharedTool.getModemLogStrategy(this.mContext);
        LogUtil.d(TAG, "dealCallRinging strategy = " + strategyJson);
        if (TextUtils.isEmpty(strategyJson)) {
            LogUtil.d(TAG, "dealCallRinging local do nothing!");
            return;
        }
        LogStrategy strategy = JSONUtil.fromJSON(strategyJson, LogStrategy.class);
        if (strategy == null || strategy.getExtra() == null || !strategy.getExtra().getIsImmediately()) {
            cancelCloseAlarm();
            cancelRetryAlarm();
            clearOldFile(true, false);
            BatteryMonitor.getInstance(this.mContext).cancelBatteryMonitor();
        }
    }

    @Override
    public void fetchingModemLog(ExtraMes extra) {
        if (!TextUtils.isEmpty(SharedTool.getModemLogStatus(this.mContext))) {
            LogUtil.e(TAG, "fetchingModemLog last ModemLog on the way");
            return;
        }
        String modemStatus = SystemPropertiesUtils.get(SystemConstants.PROP_LOG_TAG_MODEM, "");
        if (!TextUtils.isEmpty(modemStatus) && modemStatus.equals("1")) {
            LogUtil.e(TAG, "fetchingModemLog is fetching modem log");
            return;
        }
        isLocalOperating = true;
        clearOldFile(true, true);
        LogStrategy strategy = new LogStrategy();
        strategy.setLogId(-1L);
        if (extra.getIsImmediately()) {
            startModemLog(strategy, extra.getFetchingDuration());
        } else {
            BatteryMonitor.getInstance(this.mContext).startBatteryMonitor(true, strategy, extra);
        }
        TcpDumpServeImpl.getInstance(this.mContext).startLocalTcpDump(null, true);
    }

    @Override
    public void fetchingModemLog(List<LogStrategy> strategies) {
        if (strategies.size() == 0) {
            LogUtil.e(TAG, "fetchingModemLog logStrategies is empty");
            return;
        }
        LogStrategy strategy = strategies.get(0);
        LogUtil.d(TAG, "fetchingModemLog logStrategy = " + strategy);
        ExtraMes extra = strategy.getExtra();
        if (extra == null) {
            LogUtil.e(TAG, "fetchingModemLog extraModemLog is null");
            return;
        }
        if (!TextUtils.isEmpty(SharedTool.getModemLogStatus(this.mContext))) {
            LogUtil.e(TAG, "fetchingModemLog last ModemLog on the way");
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 21);
            return;
        }

        isLocalOperating = false;
        ModemLogStatus status = new ModemLogStatus();
        clearOldFile(true, true);
        SharedTool.saveModemLogStrategy(this.mContext, JSONUtil.toJSON(strategy));
        if (extra.getIsImmediately()) {
            status.setFetchingTime(1);
            status.setCurrentFetchingTime(1);
            saveCurrentStatus(status, ModemLogStatus.Status.FETCHING);
            startModemLog(strategy, extra.getFetchingDuration());
        } else {
            status.setFetchingTime(extra.getFetchingTime());
            status.setCurrentFetchingTime(1);
            saveCurrentStatus(status, ModemLogStatus.Status.MONITORING);
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 3);
            BatteryMonitor.getInstance(this.mContext).startBatteryMonitor(true, strategy, extra);
        }
        TcpDumpServeImpl.getInstance(this.mContext).startTcpDump(strategies, true);
        registerNetChange();
    }

    @Override
    public void onDestroy() {
        releaseWakeUp();
        unRegisterNetChange();
    }
}
