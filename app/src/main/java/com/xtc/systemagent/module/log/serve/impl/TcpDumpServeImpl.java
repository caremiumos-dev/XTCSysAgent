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
import com.xtc.systemagent.module.log.bean.TcpDumpStatus;
import com.xtc.systemagent.module.log.serve.ITcpDumpServe;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import com.xtc.systemagent.util.LmkPersistHelper;
import com.xtc.systemagent.util.LogCollectUtil;
import com.xtc.systemagent.util.NetworkUtil;
import com.xtc.systemagent.util.SdCardUtil;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.systemagent.util.UploadFile;
import com.xtc.utils.common.CollectionUtils;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * tcpdump 的采集生命周期管理。
 *
 * <p>tcpdump 有两种来源：</p>
 * <ul>
 *   <li><b>独立抓取</b>（{@code isFromModem == false}）—— 抓 8 小时后收尾；</li>
 *   <li><b>随 modem 日志连带抓取</b>（{@code isFromModem == true}）—— 抓 24 小时，
 *       到点后如果 modem 日志还在监控就再续一轮。</li>
 * </ul>
 *
 * <p>状态机（{@link TcpDumpStatus#getCurrentStatus()}）：</p>
 * <pre>
 *   1 = 正在抓
 *   2 = 抓完待上传
 *   3 = 上传失败待重试
 *   4 = 未知/异常
 *   5 = 等 modem 日志
 *   6 = modem 日志正在上传（暂停 tcpdump）
 * </pre>
 *
 * <p>重试上限 10 次，间隔用 {@link UploadFile#getNextRetryUploadTime(int)}（分钟）。</p>
 */
public class TcpDumpServeImpl extends BasePresenter<String> implements ITcpDumpServe {

    private final String TAG = "Log_TcpDumpServeImpl";

    /** 随 modem 连带抓取的时长：24 小时。 */
    private static final long COMBINE_FETCHING_TCP_DUMP_DURATION = 86400000L;

    /** 独立抓取的时长：8 小时。 */
    private static final long INDEPENDENT_FETCHING_TCP_DUMP_DURATION = 28800000L;

    /** 到点收尾的广播。 */
    private static final String TCP_DUMP_CLOSE_ALARM = "com.xtc.i3launcher.ACTION.TcpDumpClose";

    /** 重试上传的广播。 */
    private static final String TCP_DUMP_RETRY_UPLOAD = "com.xtc.i3launcher.ACTION.TcpDumpRetryUpload";

    /** 通知底层开关 tcpdump 的广播。 */
    private static final String ACTION_TRIGGER_TCP_DUMP = "com.android.server.xtclog.TCPDUMP_TRIGGERED";

    /** 系统服务名。 */
    private static final String TCP_DUMP_SERVICE = "tcp_dump";

    /** {@code AlarmManager.RTC_WAKEUP}。 */
    private static final int RTC_WAKEUP = 2;

    /** WakeLock 类型：{@code PARTIAL_WAKE_LOCK}。 */
    private static final int WAKE_LOCK_PARTIAL = 1;

    /** 重试上限。 */
    private static final int MAX_RETRY_UPLOAD = 10;

    /** 重试间隔单位换算：分钟 → 毫秒。 */
    private static final long MINUTE_IN_MILLIS = 60000L;

    /** 1 分钟。 */
    private static final long ONE_MINUTE = 60L;

    /** 采集状态：成功。 */
    private static final int STATE_SUCCESS = 1;
    /** 采集状态：失败。 */
    private static final int STATE_FAIL = 2;
    /** 采集状态：采集中。 */
    private static final int STATE_COLLECTING = 4;
    /** 采集状态：上一个任务还没结束。 */
    private static final int STATE_LAST_TASK_ON_THE_WAY = 21;

    /** 只在 WiFi 下上传。 */
    private static final int UPLOAD_NET_WIFI_ONLY = 2;

    private static SoftReference<TcpDumpServeImpl> tcpDumpServeImplRef;

    private AlarmManager am;
    private PendingIntent closePI;
    private BroadcastReceiver closeReceiver;
    private PendingIntent retryPI;
    private BroadcastReceiver retryReceiver;
    private PowerManager.WakeLock wakeLock;

    private TcpDumpServeImpl(Context context) {
        super(context);
        this.closeReceiver = null;
        this.retryReceiver = null;
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            LogUtil.e(TAG, "error , PowerManager is null !");
        } else {
            this.wakeLock = powerManager.newWakeLock(WAKE_LOCK_PARTIAL, TAG);
            this.wakeLock.setReferenceCounted(false);
        }
    }

    public static synchronized TcpDumpServeImpl getInstance(Context context) {
        if (tcpDumpServeImplRef == null) {
            tcpDumpServeImplRef = new SoftReference<>(new TcpDumpServeImpl(context));
        }
        TcpDumpServeImpl instance = tcpDumpServeImplRef.get();
        if (instance == null) {
            instance = new TcpDumpServeImpl(context);
            tcpDumpServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    // ------------------------------------------------------------------ 对外入口

    @Override
    public void startTcpDump(List<LogStrategy> strategies, boolean withModem) {
        LogUtil.i(TAG, "startTcpDump , withModem = " + withModem);
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
            return;
        }
        LogStrategy strategy = strategies.get(0);
        LogUtil.d(TAG, "startTcpDump logStrategy = " + strategy);
        String lastStatus = SharedTool.getTcpDumpStatus(this.mContext);
        LogUtil.i(TAG, "lastStatus = " + lastStatus);
        if (!TextUtils.isEmpty(lastStatus)) {
            LogUtil.e(TAG, "last TcpDump is on the way , return !");
            if (withModem) {
                return;
            }
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(),
                    STATE_LAST_TASK_ON_THE_WAY);
            return;
        }
        String checkResult = SdCardUtil.execNormalCheck(
                Constants.LogCollect.MIN_REMAIN_STORAGE_SPACE);
        LogUtil.d(TAG, "checkString = " + checkResult);
        if (!TextUtils.isEmpty(checkResult)) {
            if (withModem) {
                return;
            }
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(),
                    Integer.valueOf(checkResult.split("-")[0]).intValue());
            return;
        }
        clearOldFile(false);
        SharedTool.saveTcpDumpStrategy(this.mContext, JSONUtil.toJSON(strategy));
        TcpDumpStatus status = new TcpDumpStatus();
        if (withModem) {
            status.setIsIndependent(false);
            saveCurrentStatus(status, 5);
        } else {
            saveCurrentStatus(status, 1);
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(),
                    STATE_COLLECTING);
        }
        startToDump(getCloseAlarmTime(withModem));
    }

    /** 由 modem 日志流程调用的启动入口（策略可为 null）。 */
    @Override
    public void startLocalTcpDump(LogStrategy strategy, boolean withModem) {
        LogUtil.i(TAG, "startLocalTcpDump , isFromModem = " + withModem);
        String checkResult = SdCardUtil.execNormalCheck(
                Constants.LogCollect.MIN_REMAIN_STORAGE_SPACE);
        LogUtil.d(TAG, "checkString = " + checkResult);
        if (TextUtils.isEmpty(checkResult)) {
            clearOldFile(false);
            startToDump(getCloseAlarmTime(withModem));
            return;
        }
        // 连带抓取时静默失败，不回报错误码。
        if (withModem) {
            return;
        }
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(),
                Integer.valueOf(checkResult.split("-")[0]).intValue());
    }

    @Override
    public void cancelTcpDump() {
        if (TextUtils.isEmpty(SharedTool.getTcpDumpStatus(this.mContext))) {
            cancelAlarmsAndClear();
        } else {
            LogUtil.e(TAG, "cancelTcpDump , last ModemLog is on the way , return !");
        }
    }

    @Override
    public void cancelTcpDump(List<LogStrategy> strategies) {
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty");
            return;
        }
        LogUtil.d(TAG, "logStrategy = " + strategies.get(0));
        clearLastTcpInfo();
        cancelAlarmsAndClear();
    }

    @Override
    public void checkLastTcpDump() {
        LogUtil.i(TAG, "checkLastTcpDump -->");
        String strategy = SharedTool.getTcpDumpStrategy(this.mContext);
        String status = SharedTool.getTcpDumpStatus(this.mContext);
        if (isOnTheWay(strategy, status, true, false)) {
            whenHasOldTask(strategy, status);
        } else {
            LogUtil.d(TAG, "no previous task , done");
        }
    }

    @Override
    public void dealCallIdle() {
        if (TextUtils.isEmpty(SharedTool.getTcpDumpStatus(this.mContext))) {
            return;
        }
        checkLastTcpDump();
    }

    @Override
    public void dealCallRinging() {
        cancelAlarmsAndClear();
    }

    @Override
    public void onDestroy() {
        releaseWakeUp();
    }

    @Override
    public void whenLocalModemLogDone() {
        LogUtil.i(TAG, "whenLocalModemLogDone -->");
        isOnTheWay(SharedTool.getTcpDumpStrategy(this.mContext),
                SharedTool.getTcpDumpStatus(this.mContext), false, true);
    }

    @Override
    public void whenModemLogPrepareToUpload() {
        LogUtil.i(TAG, "whenModemLogPrepareToUpload -->");
        cancelRetryAlarm();
        cancelCloseAlarm();
        String statusJson = SharedTool.getTcpDumpStatus(this.mContext);
        if (!TextUtils.isEmpty(statusJson)) {
            LogUtil.i(TAG, "change tcpDump status");
            TcpDumpStatus status = (TcpDumpStatus) JSONUtil.fromJSON(statusJson, TcpDumpStatus.class);
            status.setCurrentStatus(6);
            SharedTool.saveTcpDumpStatus(this.mContext, JSONUtil.toJSON(status));
        }
        switchLocalTcpDumpStatus(false);
    }

    @Override
    public void whenUploadModemLogSucceed(boolean needContinue) {
        LogUtil.i(TAG, "needContinue = " + needContinue);
        if (!needContinue) {
            clearLastTcpInfo();
            clearOldFile(true);
            return;
        }
        String statusJson = SharedTool.getTcpDumpStatus(this.mContext);
        if (!TextUtils.isEmpty(statusJson)) {
            LogUtil.i(TAG, "change tcpDump status , start next modem monitor -->");
            TcpDumpStatus status = (TcpDumpStatus) JSONUtil.fromJSON(statusJson, TcpDumpStatus.class);
            status.setCurrentStatus(5);
            SharedTool.saveTcpDumpStatus(this.mContext, JSONUtil.toJSON(status));
        }
        checkLastTcpDump();
    }

    // ------------------------------------------------------------------ 状态机

    /** 处理上一次未完成的任务。 */
    private void whenHasOldTask(String strategyJson, String statusJson) {
        LogUtil.d(TAG, "whenHasOldTask -->");
        LogStrategy strategy = (LogStrategy) JSONUtil.fromJSON(strategyJson, LogStrategy.class);
        TcpDumpStatus status = (TcpDumpStatus) JSONUtil.fromJSON(statusJson, TcpDumpStatus.class);
        switch (status.getCurrentStatus()) {
            case 1:
                clearOldFile(true);
                startLocalTcpDump(strategy, false);
                break;
            case 2:
            case 3:
                retryUploadTcpDump(strategy, status);
                break;
            case 5:
                clearOldFile(true);
                startLocalTcpDump(null, true);
                break;
            case 6:
                LogUtil.d(TAG, "do nothing ~");
                break;
            case 4:
            default:
                clearOldFile(true);
                clearLastTcpInfo();
                break;
        }
    }

    /** 到点收尾。 */
    public void closeTcpDump() {
        LogUtil.i(TAG, "时间到 , closeTcpDump -->");
        cancelCloseAlarm();
        switchLocalTcpDumpStatus(false);
        String strategyJson = SharedTool.getTcpDumpStrategy(this.mContext);
        String statusJson = SharedTool.getTcpDumpStatus(this.mContext);
        if (!isOnTheWay(strategyJson, statusJson, false, true)) {
            return;
        }
        TcpDumpStatus status = (TcpDumpStatus) JSONUtil.fromJSON(statusJson, TcpDumpStatus.class);
        if (status.getIsIndependent()) {
            LogUtil.i(TAG, "独立抓取 tcpDump done~");
            saveCurrentStatus(status, 2);
            preUploadTcpDumpFile((LogStrategy) JSONUtil.fromJSON(strategyJson, LogStrategy.class),
                    status);
        } else {
            LogUtil.i(TAG, "modem 连带抓取 , 24小时 监控时间到，modem日志在监控时，需要 继续开启tcpDump");
            clearOldFile(false);
            startLocalTcpDump(null, true);
        }
    }

    /**
     * 策略和状态都非空才认为"任务在进行中"。
     *
     * <p>否则顺手清理残留（清开关、清文件）。</p>
     */
    private boolean isOnTheWay(String strategyJson, String statusJson, boolean closeSwitch,
                               boolean clearFile) {
        LogUtil.d(TAG, "isOnTheWay strategy = " + strategyJson + ", status = " + statusJson
                + ", closeSwitch = " + closeSwitch + ", clearFile = " + clearFile);
        if (!TextUtils.isEmpty(strategyJson) && !TextUtils.isEmpty(statusJson)) {
            LogUtil.i(TAG, "return true !");
            return true;
        }
        LogUtil.w(TAG, "return false ! at lease one value is empty , reset something !");
        clearLastTcpInfo();
        clearOldFile(closeSwitch, clearFile);
        return false;
    }

    // ------------------------------------------------------------------ 上传

    /** 上传前的网络检查；不满足就走重试排期。 */
    private void preUploadTcpDumpFile(LogStrategy strategy, TcpDumpStatus status) {
        LogUtil.d(TAG, "准备上传 , preUploadTcpDumpFile logStrategy = " + strategy
                + " , tcpDumpStatus = " + status);
        if (strategy.getUploadNet() == UPLOAD_NET_WIFI_ONLY) {
            if (NetworkUtil.isWifiConnected(this.mContext)) {
                LogUtil.i(TAG, "net is ok");
                uploadTcpDumpFile(strategy, status);
                return;
            }
            LogUtil.e(TAG, "preUploadTcpDumpFile Network is not Wifi");
            retryUploadTcpDump(strategy, status);
            return;
        }
        if (NetworkUtil.isConnected(this.mContext)) {
            LogUtil.i(TAG, "net is ok");
            uploadTcpDumpFile(strategy, status);
        } else {
            LogUtil.e(TAG, "preUploadTcpDumpFile Network is not Wifi");
            retryUploadTcpDump(strategy, status);
        }
    }

    /**
     * 执行上传。
     *
     * <p>状态 2（已打包）走 {@code dealUploadFile}；其它状态（含 3）都要先打包。</p>
     */
    private void uploadTcpDumpFile(final LogStrategy strategy, final TcpDumpStatus status) {
        LogUtil.i(TAG, "uploadTcpDumpFile begin -->");
        if (status.getCurrentStatus() != 2) {
            if (status.getCurrentStatus() != 3) {
                LogUtil.e(TAG, "uploadTcpDumpFile is not fetching");
                return;
            }
            LogUtil.d(TAG, "uploadTcpDumpFile uploadZipFile currentStatus = "
                    + status.getCurrentStatus() + "---zipFileName = " + status.getZipFileName());
            UploadFile.dealUploadFile(this.mContext, status.getZipFileName(),
                    SdCardUtil.getTcpDumpZipFilePath(), true, newUploadCallback(strategy, status));
            return;
        }
        acquireCpuForLong();
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            bindNumber = "NoBindNumber";
        }
        final String zipFileName = "TCP_DUMP_" + bindNumber + "_" + strategy.getLogId()
                + "_" + System.currentTimeMillis() + ".zip";
        LogUtil.d(TAG, "uploadTcpDumpFile uploadZipFile currentStatus = "
                + status.getCurrentStatus() + " , zipFileName = " + zipFileName);
        UploadFile.uploadZipFile(this.mContext, SdCardUtil.getTcpDumpFilePath(), zipFileName,
                SdCardUtil.getTcpDumpZipFilePath(), true,
                new UploadFile.UploadFileCallback() {
                    @Override
                    public void onNoLogFile() {
                        LogUtil.d(TAG, "onNoLogFile");
                        dealUploadFailure(strategy);
                        releaseWakeUp();
                    }

                    @Override
                    public void onUploadFileFailure(String error) {
                        LogUtil.e(TAG, "uploadTcpDumpFile onUploadFileFailure " + error);
                        status.setZipFileName(zipFileName);
                        saveCurrentStatus(status, 3);
                        retryUploadTcpDump(strategy, status);
                        releaseWakeUp();
                    }

                    @Override
                    public void onUploadFileSuccess(String key) {
                        LogUtil.d(TAG, "uploadTcpDumpFile onUploadFileSuccess " + key);
                        notifyUploadSuccess(key, strategy, 1, 1,
                                new UploadSuccessHandler());
                    }

                    @Override
                    public void onZipFileFailure(String error) {
                        LogUtil.e(TAG, "uploadTcpDumpFile onZipFileFailure " + error);
                        dealUploadFailure(strategy);
                        releaseWakeUp();
                    }
                });
    }

    /** 状态 3 的重传回调（zip 已经存在，直接传）。 */
    private UploadFile.UploadFileCallback newUploadCallback(final LogStrategy strategy,
                                                            final TcpDumpStatus status) {
        return new UploadFile.UploadFileCallback() {
            @Override
            public void onNoLogFile() {
                LogUtil.d(TAG, "onNoLogFile");
                dealUploadFailure(strategy);
                releaseWakeUp();
            }

            @Override
            public void onUploadFileFailure(String error) {
                LogUtil.e(TAG, "uploadTcpDumpFile onUploadFileFailure " + error);
                saveCurrentStatus(status, 3);
                retryUploadTcpDump(strategy, status);
                releaseWakeUp();
            }

            @Override
            public void onUploadFileSuccess(String key) {
                LogUtil.d(TAG, "uploadTcpDumpFile onUploadFileSuccess " + key);
                notifyUploadSuccess(key, strategy, 1, 1, new UploadSuccessHandler());
            }

            @Override
            public void onZipFileFailure(String error) {
                LogUtil.e(TAG, "uploadTcpDumpFile onZipFileFailure " + error);
                dealUploadFailure(strategy);
                releaseWakeUp();
            }
        };
    }

    /** 上报完成后的收尾。成功和失败的处理完全一样（都是清理 + 释放锁）。 */
    private final class UploadSuccessHandler
            implements LogStateServeImpl.UploadSuccessCallback {
        @Override
        public void onFailure() {
            clearLastTcpInfo();
            clearOldFile(true);
            releaseWakeUp();
        }

        @Override
        public void onSuccess() {
            clearLastTcpInfo();
            clearOldFile(true);
            releaseWakeUp();
        }
    }

    /** 上报服务端"上传成功"。 */
    private void notifyUploadSuccess(String key, LogStrategy strategy, int collectStatus,
                                     int spaceType,
                                     LogStateServeImpl.UploadSuccessCallback callback) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            LogUtil.e(TAG, "bindNumber is null !");
            return;
        }
        UploadNotifyParam param = new UploadNotifyParam();
        param.setAccountId(bindNumber);
        param.setKey(key);
        param.setSpaceType(Integer.valueOf(spaceType));
        param.setAppPackage(strategy.getAppPackage());
        param.setId(strategy.getLogId());
        param.setStrategyId(strategy.getId());
        param.setOperatorId(strategy.getUserId());
        param.setMachineId(SharedTool.getMachineId(this.mContext));
        param.setCollectStatus(Integer.valueOf(collectStatus));
        LogStateServeImpl.getInstance(this.mContext).notifyUploadSuccess(param, callback);
    }

    /**
     * 排下一次重试。
     *
     * <p>如果上次已经排过且时间还没到，就按剩余时间重排；否则算下一次的间隔。</p>
     */
    private void retryUploadTcpDump(@NonNull LogStrategy strategy, @NonNull TcpDumpStatus status) {
        LogUtil.d(TAG, "retryUploadTcpDump logStrategy = " + strategy
                + "---tcpDumpStatus = " + status);
        if (status.getCurrentRetryUploadNum() >= MAX_RETRY_UPLOAD) {
            LogUtil.d(TAG, "retryUploadTcpDump Retry End");
            dealUploadFailure(strategy);
            return;
        }
        long now = System.currentTimeMillis();
        if (status.getCurrentRetryUploadNum() != 0 && status.getCurrentRetryUploadTime() > now) {
            // 原厂的换算写成了 (ms / 60) / 1000，等价于 ms / 60000，即分钟数。
            long remainMinutes = ((status.getCurrentRetryUploadTime() - now) / ONE_MINUTE) / 1000;
            LogUtil.d(TAG, "retryUploadTcpDump durationMinutes = " + remainMinutes);
            startRetryAlarm(remainMinutes);
            return;
        }
        int retryCount = status.getCurrentRetryUploadNum() + 1;
        long intervalMinutes = UploadFile.getNextRetryUploadTime(retryCount);
        long nextTime = now + (MINUTE_IN_MILLIS * intervalMinutes);
        LogUtil.d(TAG, "retryUploadTcpDump nextNum = " + retryCount + "---nextTime = " + nextTime);
        status.setCurrentRetryUploadNum(retryCount);
        status.setCurrentRetryUploadTime(nextTime);
        saveCurrentStatus(status, status.getCurrentStatus());
        startRetryAlarm(intervalMinutes);
    }

    private void dealUploadFailure(@NonNull LogStrategy strategy) {
        clearLastTcpInfo();
        clearOldFile(true);
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(),
                STATE_FAIL);
    }

    // ------------------------------------------------------------------ 定时/开关

    private long getCloseAlarmTime(boolean isFromModem) {
        LogUtil.d(TAG, "getCloseAlarmTime , isFromModem = " + isFromModem);
        return (isFromModem ? COMBINE_FETCHING_TCP_DUMP_DURATION
                : INDEPENDENT_FETCHING_TCP_DUMP_DURATION) + SystemClock.elapsedRealtime();
    }

    /** 启动抓取：注册收尾广播、打开开关、挂收尾闹钟。 */
    private void startToDump(long triggerTime) {
        registerCloseReceiver();
        switchLocalTcpDumpStatus(true);
        this.closePI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(TCP_DUMP_CLOSE_ALARM), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(RTC_WAKEUP, triggerTime, this.closePI);
    }

    /** 挂重试闹钟，单位分钟。 */
    private void startRetryAlarm(long minutes) {
        LogUtil.i(TAG, "startRetryAlarm = " + minutes);
        registerRetryReceiver();
        this.retryPI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(TCP_DUMP_RETRY_UPLOAD), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(RTC_WAKEUP, SystemClock.elapsedRealtime() + (MINUTE_IN_MILLIS * minutes),
                this.retryPI);
    }

    /**
     * 开/关 tcpdump。
     *
     * <p>关的时候如果本来就没在跑就不发广播了；开的时候顺手注册 LMK 保活，
     * 关的时候注销 —— 抓包进程被杀掉就白抓了。</p>
     */
    private void switchLocalTcpDumpStatus(boolean open) {
        boolean running = isTcpdumpRunning();
        LogUtil.d(TAG, "切换TcpDump开关 switch = " + open + " , tcpdumpRunning: " + running);
        if (!open && !running) {
            return;
        }
        if (open) {
            LmkPersistHelper.registerLmkPersist(this.mContext.getPackageName());
        } else {
            LmkPersistHelper.unregisterLmkPersist(this.mContext.getPackageName());
        }
        Intent intent = new Intent(ACTION_TRIGGER_TCP_DUMP);
        intent.putExtra("start", open);
        this.mContext.sendBroadcast(intent);
    }

    /** 反射问系统 tcpdump 服务在不在跑。 */
    private boolean isTcpdumpRunning() {
        boolean running;
        try {
            Class<?> cls = Class.forName("android.os.SystemService");
            running = ((Boolean) cls.getMethod("isRunning", String.class)
                    .invoke(cls, TCP_DUMP_SERVICE)).booleanValue();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            running = false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            running = false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            running = false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            running = false;
        }
        LogUtil.d(TAG, "isTcpdumpRunning = " + running);
        return running;
    }

    private void registerCloseReceiver() {
        LogUtil.i(TAG, "registerCloseReceiver -->");
        this.closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "closeReceiver , action = " + action);
                if (TCP_DUMP_CLOSE_ALARM.equals(action)) {
                    closeTcpDump();
                }
            }
        };
        this.mContext.registerReceiver(this.closeReceiver, new IntentFilter(TCP_DUMP_CLOSE_ALARM));
    }

    private void registerRetryReceiver() {
        this.retryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "retryReceiver , action = " + action);
                if (!TCP_DUMP_RETRY_UPLOAD.equals(action)) {
                    return;
                }
                cancelRetryAlarm();
                String strategyJson = SharedTool.getTcpDumpStrategy(TcpDumpServeImpl.this.mContext);
                String statusJson = SharedTool.getTcpDumpStatus(TcpDumpServeImpl.this.mContext);
                if (isOnTheWay(strategyJson, statusJson, true, true)) {
                    preUploadTcpDumpFile(
                            (LogStrategy) JSONUtil.fromJSON(strategyJson, LogStrategy.class),
                            (TcpDumpStatus) JSONUtil.fromJSON(statusJson, TcpDumpStatus.class));
                }
            }
        };
        this.mContext.registerReceiver(this.retryReceiver, new IntentFilter(TCP_DUMP_RETRY_UPLOAD));
    }

    private void cancelAlarmsAndClear() {
        cancelCloseAlarm();
        cancelRetryAlarm();
        clearOldFile(true);
    }

    private void cancelCloseAlarm() {
        LogUtil.d(TAG, "cancelCloseAlarm -->");
        if (this.closeReceiver != null) {
            this.mContext.unregisterReceiver(this.closeReceiver);
        }
        if (this.am != null && this.closePI != null) {
            this.am.cancel(this.closePI);
        }
        this.closeReceiver = null;
    }

    private void cancelRetryAlarm() {
        LogUtil.d(TAG, "cancelRetryAlarm -->");
        if (this.retryReceiver != null) {
            this.mContext.unregisterReceiver(this.retryReceiver);
        }
        if (this.am != null && this.retryPI != null) {
            this.am.cancel(this.retryPI);
        }
        this.retryReceiver = null;
    }

    // ------------------------------------------------------------------ 清理

    private void clearLastTcpInfo() {
        LogUtil.i(TAG, "clearLastTcpInfo -->");
        SharedTool.saveTcpDumpStrategy(this.mContext, "");
        SharedTool.saveTcpDumpStatus(this.mContext, "");
    }

    private void clearOldFile(boolean closeSwitch) {
        clearOldFile(closeSwitch, true);
    }

    private void clearOldFile(boolean closeSwitch, boolean clearFile) {
        LogUtil.d(TAG, "clearOldFile closeSwitch = " + closeSwitch);
        if (closeSwitch) {
            switchLocalTcpDumpStatus(false);
        }
        if (clearFile) {
            UploadFile.deleteFile(SdCardUtil.getTcpDumpFilePath(), getClass().getSimpleName());
            UploadFile.deleteFile(SdCardUtil.getTcpDumpZipFilePath(), getClass().getSimpleName());
        }
    }

    private void saveCurrentStatus(TcpDumpStatus status, int currentStatus) {
        LogUtil.i(TAG, "saveCurrentStatus = " + currentStatus);
        status.setCurrentStatus(currentStatus);
        SharedTool.saveTcpDumpStatus(this.mContext, JSONUtil.toJSON(status));
    }

    private void acquireCpuForLong() {
        LogUtil.d(TAG, "acquireCpuForLong -->");
        if (this.wakeLock == null) {
            LogUtil.e(TAG, "error, wakeLock = null !");
            return;
        }
        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        // 注意这里是无限期持有，不是 acquire(timeout) —— 原厂如此。
        this.wakeLock.acquire();
    }

    private void releaseWakeUp() {
        LogUtil.d(TAG, "release Wake Up");
        if (this.wakeLock == null) {
            LogUtil.e(TAG, "error, wakeLock = null !");
        } else if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
    }
}
