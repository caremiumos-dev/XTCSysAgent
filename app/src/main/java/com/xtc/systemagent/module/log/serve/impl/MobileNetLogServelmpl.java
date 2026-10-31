package com.xtc.systemagent.module.log.serve.impl;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.xtc.architecture.mvp.BasePresenter;
import com.xtc.httplib.ConfigOptions;
import com.xtc.log.LogUtil;
import com.xtc.qiniu.ICloudManager;
import com.xtc.qiniu.ICloudService;
import com.xtc.qiniu.TokenManager;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.module.Constants;
import com.xtc.systemagent.module.log.bean.NetWorkModemLogStatus;
import com.xtc.systemagent.module.log.serve.IMobileNetLogServe;
import com.xtc.systemagent.net.bean.LogStrategy;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * 网络侧（telephony / QCOM）modem 日志监控实现。
 *
 * <p>与 {@link ModemLogServeImpl} 的区别：这里的抓取由 modem 侧广播驱动
 * （{@code com.android.qcom.network.startmodemlog} 等），并且同时抓 tcpdump。
 * 抓取结束后把 modem 日志目录 + tcpdump 目录 + system_log 归档一起压缩上传。</p>
 *
 * <p>上传状态通过一个常驻的 {@link NotifiServerThread} 串行上报，
 * 网络恢复后由 {@link #onConnectivityChanged(boolean, boolean)} 驱动补传。</p>
 */
public class MobileNetLogServelmpl extends BasePresenter<Object>
        implements IMobileNetLogServe, WifiHelper.ConnectivityCallback {

    private static final String TAG = "Log_MobileNetLogServelmpl";

    /** grabType 取该值表示强制取消。 */
    private static final String FORCED_CANCEL = "999";

    /** init.rc 里的 tcpdump 服务名。 */
    private static final String TCP_DUMP_SERVICE = "tcp_dump";

    private static final int BASE_HOUR_TIME = 3600000;
    private static final int BASE_MIN_TIME = 60000;

    /** tcpdump 最长连续抓取时长（小时）。 */
    private static final long TCP_DUMP_OUTTIME = 8;

    private static final int UPLOAD_NET_WIFI = 2;

    /** 空间占用率阈值（%）。 */
    private static final int LIMIT_SPACE_RATE = 50;

    private static SoftReference<MobileNetLogServelmpl> mobikeNetLogServeImplRef = null;

    /** 上报线程的存活标记。 */
    private static volatile boolean notifyThreadRunable = true;

    private final String ACTION_NETWORK_MODEN_LOG_START;
    private final String ACTION_NETWORK_MODEN_LOG_STOP;
    private final String ACTION_QCOM_SERVER_NETWORK;
    private final String ACTION_NOTIFI_SERVER_NETWORK;
    private final String ACTION_PENDINT_TCPDUMP_OUTTIME;
    private final String ACTION_PENDINT_TCPDUMP_RESTART;
    private final String ACTION_PENDINT_MODEM_NETWORK_OUTTIME;
    private final String ACTION_PENDINT_MODEM_NETWORK_DELAYTIME;
    private final String PROPERTY_SYSTEM_GSM_NETWORK;
    private final String PROPERTY_SYSTEM_GSM_NETWORK_TYPE;
    private final String PROPERTY_DETERMINE_UPLOAD;

    private AlarmManager aManager;
    private PendingIntent ModemDelayPendIntent;
    private PendingIntent ModemEndPendIntent;
    private PendingIntent TcmpDumpEndPendIntent;
    private PendingIntent TcmpDumpRestartPendIntent;

    private ConnectivityManager mConnectivityManager;
    private LogStrategy mLogStrategy;
    private NetWorkModemLogStatus mNetWorkLogStatus;
    private LogStrategy mReportLogStrategy;
    private NotifiServerThread mNotifiThread;
    private PowerManager.WakeLock mWakeLock;
    private String mZipLogFileName;
    private String notifyStr;
    private BroadcastReceiver startMonitorReceiver;
    private WifiHelper wifiHelper;

    private final Object mNotifyLock;
    private final Object mCallBackLock;

    private boolean isRegistBrodReceive;
    private boolean mobConnected;
    private boolean wifiConnected;
    private boolean uploading;
    private boolean needUploadState;
    private boolean receiveModemState;
    private boolean netChangeregState;

    /**
     * 串行上报线程：由 {@link #notifyThreadReport(LogStrategy)} 唤醒，
     * 网络可用时把 {@link #notifyStr} 上报给服务端。
     */
    private class NotifiServerThread extends Thread {

        private NotifiServerThread() {
        }

        @Override
        public void run() {
            super.run();
            while (notifyThreadRunable) {
                try {
                    LogUtil.i(TAG, "NotifiServerThread  notifyThreadRunable:" + notifyThreadRunable);
                    if (notifyThreadRunable && notifyStr != null) {
                        LogUtil.i(TAG, "notifyStr:" + notifyStr);
                        if (mConnectivityManager == null) {
                            mConnectivityManager = (ConnectivityManager)
                                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        }
                        NetworkInfo networkInfo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                ? mConnectivityManager.getNetworkInfo(mConnectivityManager.getActiveNetwork())
                                : mConnectivityManager.getActiveNetworkInfo();
                        if (networkInfo != null && networkInfo.getState() != null
                                && networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                            Log.d(TAG, "networkInfo: " + networkInfo.getState()
                                    + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR
                                    + networkInfo.getTypeName()
                                    + " mReportLogStrategy: " + (mReportLogStrategy != null));
                            if (mReportLogStrategy != null) {
                                LogStateServeImpl.getInstance(mContext).notifyErrorInfo(mReportLogStrategy, notifyStr);
                            }
                        }
                    }
                    synchronized (mCallBackLock) {
                        mCallBackLock.notify();
                    }
                    synchronized (mNotifyLock) {
                        mNotifyLock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        void stopNotifyThread() {
            notifyThreadRunable = false;
        }
    }

    private MobileNetLogServelmpl(Context context) {
        super(context);
        this.ACTION_NETWORK_MODEN_LOG_START = "com.android.qcom.network.startmodemlog";
        this.ACTION_NETWORK_MODEN_LOG_STOP = "com.android.internal.telephony.modemlog.ACTION_STOP_GRASP";
        this.ACTION_QCOM_SERVER_NETWORK = "com.android.qcom.server.network";
        this.ACTION_NOTIFI_SERVER_NETWORK = "com.android.internal.telephony.modemlog.ACTION_COMMAND";
        this.ACTION_PENDINT_TCPDUMP_OUTTIME = "com.android.qcom.tcmpdump.outime";
        this.ACTION_PENDINT_TCPDUMP_RESTART = "com.android.qcom.tcmpdump.restart";
        this.ACTION_PENDINT_MODEM_NETWORK_OUTTIME = "com.android.modem.network.outime";
        this.ACTION_PENDINT_MODEM_NETWORK_DELAYTIME = "com.android.modem.network.delaytime";
        this.PROPERTY_SYSTEM_GSM_NETWORK = "log.tag.network.server";
        this.PROPERTY_SYSTEM_GSM_NETWORK_TYPE = "log.tag.network.server.type";
        this.PROPERTY_DETERMINE_UPLOAD = "gsm.data.matched";

        this.startMonitorReceiver = null;
        this.isRegistBrodReceive = false;
        this.mNotifyLock = new Object();
        this.mCallBackLock = new Object();
        this.mobConnected = false;
        this.wifiConnected = false;
        this.uploading = false;
        this.needUploadState = false;
        this.receiveModemState = false;
        this.netChangeregState = false;

        LogUtil.d(TAG, "new MobileNetLogServelmpl");
        this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        this.mWakeLock.setReferenceCounted(false);
        if (this.mNotifiThread == null) {
            LogUtil.d(TAG, "new MobileNetLogServelmpl.NotifiServerThread");
            this.mNotifiThread = new NotifiServerThread();
            this.mNotifiThread.start();
        }
    }

    public static synchronized MobileNetLogServelmpl getInstance(Context context) {
        MobileNetLogServelmpl instance = mobikeNetLogServeImplRef == null
                ? null : mobikeNetLogServeImplRef.get();
        if (instance == null) {
            instance = new MobileNetLogServelmpl(context);
            mobikeNetLogServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // 状态机入口
    // ------------------------------------------------------------------

    /**
     * 按已持久化的状态决定接下来做什么。
     *
     * @param currentStatus {@link NetWorkModemLogStatus.Status} 取值
     */
    private void checkStatus(int currentStatus) throws Throwable {
        LogUtil.i(TAG, "checkStatus currentStatus: " + currentStatus);
        if (currentStatus == NetWorkModemLogStatus.Status.IDLE
                || currentStatus == NetWorkModemLogStatus.Status.MONITORING) {
            if (SystemPropertiesUtils.getBoolean(PROPERTY_SYSTEM_GSM_NETWORK, false)) {
                startMoniter();
            } else {
                stopMoniter(true, this.mLogStrategy);
            }
            return;
        }
        if (currentStatus != NetWorkModemLogStatus.Status.FETCHING
                && currentStatus != NetWorkModemLogStatus.Status.UPLOAD_FAILURE
                && currentStatus != NetWorkModemLogStatus.Status.PRE_UPLOAD
                && currentStatus != NetWorkModemLogStatus.Status.FETCHED) {
            stopMoniter(true, this.mLogStrategy);
            return;
        }
        File modemLogFile = new File(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
        if (new File(Constants.NetWorkModemLog.ZIP_FILE_PATH).exists()) {
            if (this.mNetWorkLogStatus != null) {
                this.mZipLogFileName = this.mNetWorkLogStatus.getZipFileName();
            }
            uploadNetworkLogFile(this.mLogStrategy, this.mNetWorkLogStatus, this.mZipLogFileName);
        } else if (modemLogFile.exists()) {
            preUploadNetworkLogFile(this.mLogStrategy, this.mNetWorkLogStatus);
        } else {
            stopMoniter(true, this.mLogStrategy);
        }
    }

    /** 清理残留：modem 日志目录与 tcpdump 目录。 */
    private void clearOldFile(boolean clearTcpDump, boolean clearModemLog) {
        LogUtil.i(TAG, "clearOldFile");
        if (clearModemLog) {
            UploadFile.deleteFile(Constants.ModemLogPath.MODEM_LOG_FILE_PATH, getClass().getSimpleName());
        }
        if (clearTcpDump) {
            UploadFile.deleteFile(SdCardUtil.getTcpDumpFilePath(), getClass().getSimpleName());
        }
    }

    /** 关闭 modem / tcpdump 抓取，3s 后进入上传。 */
    private void closeNetModemLog() {
        LogUtil.d(TAG, "closeNetModemLog");
        if (this.aManager != null && this.TcmpDumpEndPendIntent != null) {
            this.aManager.cancel(this.TcmpDumpEndPendIntent);
        }
        if (this.aManager != null && this.TcmpDumpRestartPendIntent != null) {
            this.aManager.cancel(this.TcmpDumpRestartPendIntent);
        }
        graspModemLog(false);
        graspTcpDump(false);
        saveCurrentStatus(NetWorkModemLogStatus.Status.FETCHED);
        // 延迟一会儿再取文件：等待 modem 侧把缓冲区刷盘。
        this.ModemDelayPendIntent = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(ACTION_PENDINT_MODEM_NETWORK_DELAYTIME), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.aManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ConfigOptions.MonitorCondition.FREQUENT_REQUEST_PERIOD,
                this.ModemDelayPendIntent);
    }

    /** 收尾：清空 sp 里的策略与状态并释放资源。 */
    private void dealcloseNetLogMoniter() {
        LogUtil.d(TAG, "dealcloseNetLogMoniter");
        SharedTool.saveNetWorkModemLogStatus(this.mContext, "");
        SharedTool.saveNetWorkModemLogStrategy(this.mContext, "");
        this.uploading = false;
        this.receiveModemState = false;
        onRelease();
    }

    // ------------------------------------------------------------------
    // tcpdump / modem 开关
    // ------------------------------------------------------------------

    private void graspModemLog(boolean open) {
        LogUtil.d(TAG, "graspModemLog = " + open);
        boolean currentOn = "1".equals(SystemPropertiesUtils.get(SystemConstants.PROP_LOG_TAG_MODEM, ""));
        LogUtil.d(TAG, "modemLogStatus = " + currentOn);
        if (open != currentOn) {
            wakeUpCpuForShort();
            SystemPropertiesUtils.set(SystemConstants.PROP_LOG_TAG_MODEM, open ? 1 : 0);
        }
    }

    /**
     * 开关 tcpdump。
     *
     * @return true 表示状态发生了变化（真的发了广播）
     */
    private boolean graspTcpDump(boolean open) {
        LogUtil.d(TAG, "graspTcpDump = " + open);
        if ((open && isTcpdumpRunning()) || (!open && !isTcpdumpRunning())) {
            return false;
        }
        Intent intent = new Intent("com.android.server.xtclog.TCPDUMP_TRIGGERED");
        intent.putExtra("start", open);
        this.mContext.sendBroadcast(intent);
        return true;
    }

    /** 通过反射查询 init 的 {@code tcp_dump} 服务是否在运行。 */
    private boolean isTcpdumpRunning() {
        boolean running;
        try {
            Class<?> clazz = Class.forName("android.os.SystemService");
            running = ((Boolean) clazz.getMethod("isRunning", String.class).invoke(clazz, TCP_DUMP_SERVICE))
                    .booleanValue();
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

    private void wakeUpCpuForShort() {
        LogUtil.i(TAG, "call Log Wake up Cpu for short");
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        this.mWakeLock.acquire(5000L);
    }

    // ------------------------------------------------------------------
    // 冲突检测
    // ------------------------------------------------------------------

    /**
     * 判断上一轮网络 modem 日志是否还在进行中。
     *
     * @param needReport 是否需要在冲突时上报原因
     * @param grabType   modem 侧下发的抓取类型
     */
    private boolean lastOnTheWay(boolean needReport, String grabType) {
        String oldStrategy = SharedTool.getNetWorkModemLogStrategy(this.mContext);
        String oldStatus = SharedTool.getNetWorkModemLogStatus(this.mContext);
        LogUtil.i(TAG, "judge lastNetModemLogOnTheWay in sp , oldNetModemStrategy = " + oldStrategy
                + ", oldNetModemStatus = " + oldStatus);
        LogUtil.i(TAG, "grabType:" + grabType);

        if (FORCED_CANCEL.equals(grabType)) {
            if (needReport) {
                stopMoniter(false, this.mLogStrategy);
            }
            SharedTool.saveNetWorkModemLogStatus(this.mContext, "");
            SharedTool.saveNetWorkModemLogStrategy(this.mContext, "");
            clearOldFile(true, true);
            return true;
        }

        if (TextUtils.isEmpty(oldStrategy) || TextUtils.isEmpty(oldStatus)) {
            LogUtil.i(TAG, "telephony oldNetModemStrategy log is null");
            return false;
        }

        NetWorkModemLogStatus status = JSONUtil.fromJSON(oldStatus, NetWorkModemLogStatus.class);
        LogStrategy previous = this.mLogStrategy;
        this.mLogStrategy = JSONUtil.fromJSON(oldStrategy, LogStrategy.class);

        int currentStatus = status.getCurrentStatus();
        if (currentStatus == NetWorkModemLogStatus.Status.UPLOAD_SUCCESS) {
            LogUtil.i(TAG, "the oldNetModemStrategy has upload success");
            if (needReport) {
                this.notifyStr = "此策略已经上传成功";
                notifyThreadReport(this.mLogStrategy);
            }
            this.mLogStrategy = previous;
            return false;
        }

        boolean gsmNetOn = SystemPropertiesUtils.getBoolean(PROPERTY_SYSTEM_GSM_NETWORK, false);
        LogUtil.i(TAG, "gsmNetState:" + gsmNetOn + " currentState:" + currentStatus);
        if (gsmNetOn || !(currentStatus == NetWorkModemLogStatus.Status.MONITORING
                || currentStatus == NetWorkModemLogStatus.Status.IDLE)) {
            this.mNetWorkLogStatus = status;
            return true;
        }

        // 是上一次开机遗留的监控状态，直接丢弃。
        LogUtil.i(TAG, "the oldNetModemStrategy has restart machine");
        if (needReport) {
            stopMoniter(false, this.mLogStrategy);
        }
        SharedTool.saveNetWorkModemLogStatus(this.mContext, "");
        SharedTool.saveNetWorkModemLogStrategy(this.mContext, "");
        this.mLogStrategy = previous;
        return false;
    }

    /** 判断 modem 日志或 tcpdump 是否正在被别的策略占用。 */
    private boolean modemAndtcpOnTheWay() {
        String callLogStrategy = SharedTool.getCallLogStrategy(this.mContext);
        String callLogStatus = SharedTool.getCallLogStatus(this.mContext);
        LogUtil.i(TAG, "judge lastLogOnTheWay in sp , oldLogStrategy = " + callLogStrategy
                + ", oldStatus = " + callLogStatus);
        if (!TextUtils.isEmpty(callLogStrategy) || !TextUtils.isEmpty(callLogStatus)) {
            LogUtil.i(TAG, "telephony modem log is not null.");
            this.notifyStr = "有策略在抓modemlog,下发无效";
            notifyThreadReport(this.mLogStrategy);
            return true;
        }

        String tcpDumpStrategy = SharedTool.getTcpDumpStrategy(this.mContext);
        LogUtil.i(TAG, "judge lastTcpLogOnTheWay in sp , oldTcpStrategy = " + tcpDumpStrategy
                + ", oldTcpStatus = " + SharedTool.getTcpDumpStatus(this.mContext));
        if (TextUtils.isEmpty(tcpDumpStrategy)) {
            return false;
        }
        LogUtil.i(TAG, "telephony tcp dump log is not null.");
        this.notifyStr = "有策略在抓tcpdump,下发无效";
        notifyThreadReport(this.mLogStrategy);
        return true;
    }

    // ------------------------------------------------------------------
    // 与 modem 侧交互
    // ------------------------------------------------------------------

    /** 通知 modem 侧按当前策略的 grabType 打开抓取。 */
    private void nofityModem() {
        Intent intent = new Intent(ACTION_NOTIFI_SERVER_NETWORK);
        String grabType = "";
        if (this.mLogStrategy != null) {
            grabType = this.mLogStrategy.getExtra().getGrabType();
            LogUtil.d(TAG, "set network GrabType:" + grabType);
        }
        LogUtil.d(TAG, "open network set GrabType :" + grabType);
        intent.putExtra("commandwords", grabType);
        this.mContext.sendBroadcast(intent);
    }

    private void sendNetworkBroadcast() {
        this.mContext.sendBroadcast(new Intent(ACTION_QCOM_SERVER_NETWORK));
    }

    private void setGsmNetWorkSwitch(boolean open) {
        boolean old = SystemPropertiesUtils.getBoolean(PROPERTY_SYSTEM_GSM_NETWORK, false);
        LogUtil.d(TAG, "setGsmNetWorkSwitch , isOpen = " + open + " oldStatus:" + old);
        if (old != open) {
            SystemPropertiesUtils.setBoolean(PROPERTY_SYSTEM_GSM_NETWORK, open);
            LogUtil.d(TAG, "PROPERTY_SYSTEM_GSM_NETWORK: "
                    + SystemPropertiesUtils.getBoolean(PROPERTY_SYSTEM_GSM_NETWORK, false));
        }
    }

    // ------------------------------------------------------------------
    // 监控启停
    // ------------------------------------------------------------------

    private void startMoniter() {
        LogUtil.i(TAG, "startMoniter");
        saveCurrentStatus(NetWorkModemLogStatus.Status.MONITORING);
        registerMoniterReceiver();
        nofityModem();
        if (graspTcpDump(true)) {
            if (this.aManager == null) {
                this.aManager = (AlarmManager) this.mContext.getSystemService(Context.ALARM_SERVICE);
            }
            if (this.aManager != null) {
                this.TcmpDumpEndPendIntent = PendingIntent.getBroadcast(this.mContext, 0,
                        new Intent(ACTION_PENDINT_TCPDUMP_OUTTIME), NotificationFlag.NOTIFICATION_FLAG_HEADER);
                this.aManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + TCP_DUMP_OUTTIME * BASE_HOUR_TIME,
                        this.TcmpDumpEndPendIntent);
            }
        }
    }

    /**
     * 停止监控。
     *
     * @param syncServer 是否同步关闭服务端监控状态
     * @param strategy   策略；为 null 时从 sp 里取 modem 状态策略
     */
    private void stopMoniter(final boolean syncServer, LogStrategy strategy) {
        LogUtil.i(TAG, "stopMoniter sysnc server closeMoniter:" + syncServer);
        this.notifyStr = "监控结束";

        LogStrategy reportStrategy;
        if (strategy == null) {
            String modemStateStrategy = SharedTool.getModemStateStrategy(this.mContext);
            if (!TextUtils.isEmpty(modemStateStrategy)) {
                LogUtil.d(TAG, "stopMoniter getModemStateStrategy");
            }
            reportStrategy = JSONUtil.fromJSON(modemStateStrategy, LogStrategy.class);
        } else {
            reportStrategy = strategy;
        }

        if (notifyThreadReport(reportStrategy)) {
            // 已直接上报成功：清掉待上报标记，延时后收尾。
            this.needUploadState = false;
            LogUtil.d(TAG, "saveModemStateStrategy null");
            SharedTool.saveModemStateStrategy(this.mContext, "");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (mCallBackLock) {
                            mCallBackLock.wait(200L);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (syncServer) {
                        dealcloseNetLogMoniter();
                    }
                }
            }).start();
            return;
        }

        // 网络不可用：记下待上报，等网络恢复后再发。
        this.needUploadState = true;
        if (reportStrategy != null) {
            LogUtil.d(TAG, "saveModemStateStrategy");
            SharedTool.saveModemStateStrategy(this.mContext, JSONUtil.toJSON(reportStrategy));
        }
        registerNetChange();
    }

    private void saveCurrentStatus(int currentStatus) {
        if (this.mNetWorkLogStatus != null) {
            this.mNetWorkLogStatus.setCurrentStatus(currentStatus);
            SharedTool.saveNetWorkModemLogStatus(this.mContext, JSONUtil.toJSON(this.mNetWorkLogStatus));
        }
    }

    // ------------------------------------------------------------------
    // 网络监听
    // ------------------------------------------------------------------

    private synchronized void registerNetChange() {
        if (!this.netChangeregState) {
            LogUtil.i(TAG, "registerNetChange");
            this.netChangeregState = true;
            this.wifiHelper = WifiHelper.getInstance(this.mContext);
            this.wifiHelper.setConnectivityCallback(this);
        }
    }

    private synchronized void unRegisterNetChange() {
        if (this.netChangeregState) {
            LogUtil.i(TAG, "unRegisterNetChange");
            if (this.wifiHelper != null) {
                this.netChangeregState = false;
                this.wifiHelper.removeConnectivityCallback(this);
            }
        }
    }

    @Override
    public void onConnectivityChanged(boolean mobConnected, boolean wifiConnected) {
        LogUtil.d(TAG, "MobileNetLog dealNetworkChange mobConnected = " + mobConnected
                + "---wifiConnected = " + wifiConnected
                + "---this.mobConnected = " + this.mobConnected
                + "---this.wifiConnected = " + this.wifiConnected
                + "---uploading = " + this.uploading
                + "--needUploadState = " + this.needUploadState);

        if ((mobConnected || wifiConnected)
                && ((mobConnected != this.mobConnected || wifiConnected != this.wifiConnected)
                && !this.uploading)) {
            if (this.needUploadState) {
                if (this.mReportLogStrategy == null) {
                    LogUtil.d(TAG, "mReportLogStrategy has null");
                    String modemStateStrategy = SharedTool.getModemStateStrategy(this.mContext);
                    if (!TextUtils.isEmpty(modemStateStrategy)) {
                        LogUtil.d(TAG, "SharedTool getModemStateStrategy");
                        this.mReportLogStrategy = JSONUtil.fromJSON(modemStateStrategy, LogStrategy.class);
                    }
                }
                stopMoniter(true, this.mReportLogStrategy);
            }

            String netStrategy = SharedTool.getNetWorkModemLogStrategy(this.mContext);
            String netStatus = SharedTool.getNetWorkModemLogStatus(this.mContext);
            if (!TextUtils.isEmpty(netStrategy) && !TextUtils.isEmpty(netStatus)) {
                LogStrategy strategy = JSONUtil.fromJSON(netStrategy, LogStrategy.class);
                NetWorkModemLogStatus status = JSONUtil.fromJSON(netStatus, NetWorkModemLogStatus.class);
                if (status.getCurrentStatus() == NetWorkModemLogStatus.Status.PRE_UPLOAD
                        || status.getCurrentStatus() == NetWorkModemLogStatus.Status.UPLOAD_FAILURE) {
                    if (status.getZipFileName() != null) {
                        this.mZipLogFileName = status.getZipFileName();
                    } else {
                        File[] files = new File(Constants.NetWorkModemLog.ZIP_FILE_PATH).listFiles();
                        if (files != null) {
                            Log.d(TAG, "filelist: " + files.length);
                            for (File file : files) {
                                String name = file.getName();
                                if (name.endsWith("zip")) {
                                    Log.d(TAG, "fileName: " + name);
                                    this.mZipLogFileName = name;
                                }
                            }
                        }
                    }
                    if (this.mZipLogFileName != null) {
                        uploadNetworkLogFile(strategy, status, this.mZipLogFileName);
                    }
                }
            }
        }
        this.mobConnected = mobConnected;
        this.wifiConnected = wifiConnected;
    }

    // ------------------------------------------------------------------
    // 上报
    // ------------------------------------------------------------------

    /**
     * 唤醒上报线程。
     *
     * @return true 表示当前网络可用并已提交上报；false 表示网络不可用
     */
    public boolean notifyThreadReport(LogStrategy strategy) {
        this.mReportLogStrategy = strategy;
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager)
                    this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo networkInfo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ? this.mConnectivityManager.getNetworkInfo(this.mConnectivityManager.getActiveNetwork())
                : this.mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getState() == null
                || !networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
            Log.d(TAG, "networkInfo is unavailable");
            return false;
        }
        Log.d(TAG, "networkInfo: " + networkInfo.getState()
                + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + networkInfo.getTypeName());
        synchronized (this.mNotifyLock) {
            this.mNotifyLock.notify();
        }
        return true;
    }

    private void notifyServerSuccess(@NonNull LogStrategy strategy, final String key, int spaceType) {
        LogStateServeImpl.getInstance(this.mContext).notifyUploadSuccess(key, strategy, 1, spaceType,
                new LogStateServeImpl.UploadSuccessCallback() {
                    @Override
                    public void onFailure() {
                        LogUtil.d(TAG, "uploadMoblieNetlogFile to server fail! " + key);
                        dealcloseNetLogMoniter();
                    }

                    @Override
                    public void onSuccess() {
                        LogUtil.d(TAG, "uploadMoblieNetlogFile to server success. " + key);
                        dealcloseNetLogMoniter();
                    }
                });
    }

    // ------------------------------------------------------------------
    // 压缩与上传
    // ------------------------------------------------------------------

    /** 归档目录（跨天时用旧的 tar.gz）。 */
    public String getLogsCollectorPath() {
        String today = DateUtils.format("yyyyMMdd");
        String overTime = SharedTool.getCallLogOverTime(this.mContext);
        String prefix = SdCardUtil.getSystemLogDirPrefix();
        if (!TextUtils.isEmpty(overTime) && !TextUtils.isEmpty(today) && !overTime.equals(today)) {
            LogUtil.i(TAG, "over one day");
            String oldPath = prefix + overTime + ".tar.gz";
            if (new File(oldPath).exists()) {
                LogUtil.i(TAG, "file is exist");
                return oldPath;
            }
        }
        return prefix + today;
    }

    /**
     * 把 modem 日志、tcpdump、system_log 一起压缩成 zip，然后上传。
     *
     * <p>若 modem 侧认为本次抓取不需要上传（{@code gsm.data.matched} 为 false），
     * 直接清理不压缩。</p>
     */
    private void preUploadNetworkLogFile(LogStrategy strategy, NetWorkModemLogStatus status)
            throws Throwable {
        boolean modemNeedUpload = SystemPropertiesUtils.getBoolean(PROPERTY_DETERMINE_UPLOAD, false);
        LogUtil.d(TAG, "preUploadNetworkLogFile modemthinkNeedupload:" + modemNeedUpload);
        if (!modemNeedUpload) {
            LogUtil.d(TAG, "preUploadNetworkLogFile modemthink not Needupload clearOldFile");
            clearOldFile(true, true);
            return;
        }

        this.mZipLogFileName = "LOG_NETWORK_"
                + SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "") + "_"
                + strategy.getLogId() + "_" + System.currentTimeMillis() + ".zip";

        ArrayList<String> paths = new ArrayList<>();
        paths.add(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
        paths.add(SdCardUtil.getTcpDumpFilePath());
        paths.add(getLogsCollectorPath());
        if (paths.isEmpty()) {
            LogUtil.e(TAG, "after remove , originalFilePaths is Empty");
            return;
        }

        String zipDir = Constants.NetWorkModemLog.ZIP_FILE_PATH;
        // 去掉不存在的路径，避免 zip 里出现空条目。
        for (int i = paths.size() - 1; i >= 0; i--) {
            String path = paths.get(i);
            if (!new File(path).exists()) {
                LogUtil.e(TAG, "preZipfile " + path + " , but file dose not exists");
                paths.remove(i);
            }
        }

        File dir = new File(zipDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(new File(zipDir + this.mZipLogFileName))));
            try {
                for (String path : paths) {
                    UploadFile.zipDirectory(zipOutputStream, new File(path), "");
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e(TAG, "ZipOutputStream error");
            } finally {
                if (zipOutputStream != null) {
                    try {
                        zipOutputStream.finish();
                        zipOutputStream.flush();
                        zipOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogUtil.d(TAG, "preUploadNetworkLogFile compress success");
        clearOldFile(true, true);
        saveCurrentStatus(NetWorkModemLogStatus.Status.PRE_UPLOAD);
        if (this.mNetWorkLogStatus != null) {
            this.mNetWorkLogStatus.setZipFileName(this.mZipLogFileName);
        }
        uploadNetworkLogFile(strategy, status, this.mZipLogFileName);
    }

    /** 按策略的网络要求上传 zip。 */
    private void uploadNetworkLogFile(LogStrategy strategy, NetWorkModemLogStatus status, String zipFileName) {
        LogUtil.d(TAG, "uploadNetworkLogFile");
        if (strategy == null || status == null) {
            return;
        }
        boolean modemNeedUpload = SystemPropertiesUtils.getBoolean(PROPERTY_DETERMINE_UPLOAD, false);
        if (!modemNeedUpload) {
            LogUtil.d(TAG, "uploadNetworkLogFile modemthinkNeedupload: " + modemNeedUpload);
            return;
        }

        int uploadNet = strategy.getUploadNet();
        LogUtil.d(TAG, "uploadNetworkLogFile uploadNet: " + uploadNet);
        if (uploadNet == UPLOAD_NET_WIFI) {
            if (NetworkUtil.isWifiConnected(this.mContext)) {
                startQiniuUpload(strategy, new File(Constants.NetWorkModemLog.ZIP_FILE_PATH + zipFileName));
                return;
            }
            this.notifyStr = "文件已经保存, wifi未连接";
            notifyThreadReport(this.mLogStrategy);
            registerNetChange();
            return;
        }

        if (NetworkUtil.isConnected(this.mContext)) {
            startQiniuUpload(strategy, new File(Constants.NetWorkModemLog.ZIP_FILE_PATH + zipFileName));
            return;
        }
        this.notifyStr = "文件已经保存, 数据网络未打开";
        notifyThreadReport(this.mLogStrategy);
        registerNetChange();
    }

    /** 申请 token 后把 zip 上传到七牛。 */
    private void startQiniuUpload(@NonNull final LogStrategy strategy, final File file) {
        this.uploading = true;
        wakeUpCpuForShort();
        TokenManager.getUploadToken(this.mContext, 1, new TokenManager.OnUpLoadTokenListener() {
            @Override
            public void onFailure(String message) {
                LogUtil.e(TAG, "uploadMoblieNetlogFile getUploadToken failure: " + message);
            }

            @Override
            public void onSuccess(String token) {
                final String name = file.getName();
                LogUtil.i(TAG, "startQiniuUpload token value = " + token + " , fileName= " + name);
                ICloudService.upLoadFile(mContext, 1, name, file, new ICloudManager.OnUpLoadListener() {
                    @Override
                    public void onFailure(String key, int statusCode, String error) {
                        LogUtil.e(TAG, "uploadMoblieNetlogFile onUploadFileFailure to qiniu key: "
                                + key + " , statusCode: " + statusCode + " ,error: " + error);
                    }

                    @Override
                    public void onProgress(String key, double progress) {
                        LogUtil.d(TAG, "startQiniuUpload upload mobileNetlog progress:" + progress);
                    }

                    @Override
                    public void onSuccess(String key) {
                        LogUtil.d(TAG, "startQiniuUpload upload mobileNetlog progress:" + key);
                        UploadFile.deleteFile(SdCardUtil.getNertWorkModemLogZipFilePath(),
                                getClass().getSimpleName());
                        LogCollectUtil.updateCollectState(mContext, strategy.getLogId().longValue(), 1);
                        notifyServerSuccess(strategy, key, 1);
                    }
                });
            }
        });
    }

    // ------------------------------------------------------------------
    // 广播接收
    // ------------------------------------------------------------------

    private void registerMoniterReceiver() {
        LogUtil.i(TAG, "registerMoniterReceiver :" + this.isRegistBrodReceive);
        if (this.isRegistBrodReceive || this.startMonitorReceiver != null) {
            LogUtil.i(TAG, "BroadcastReceiver has register");
            return;
        }
        this.startMonitorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "MoniterReceiver , action = " + action);
                try {
                    handleMonitorAction(action, intent);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void handleMonitorAction(String action, Intent intent) throws Throwable {
                if (ACTION_NETWORK_MODEN_LOG_START.equals(action)) {
                    int monitorTime = intent.getIntExtra("time", 0);
                    LogUtil.d(TAG, "startMoniterReceiver , moniterTime = " + monitorTime);
                    if (monitorTime > 0) {
                        whenNetModemCatchStart(monitorTime);
                    }
                    return;
                }
                if (ACTION_NETWORK_MODEN_LOG_STOP.equals(action)) {
                    setGsmNetWorkSwitch(false);
                    return;
                }
                if (ACTION_PENDINT_TCPDUMP_OUTTIME.equals(action)) {
                    graspTcpDump(false);
                    // modem 还在抓的时候重启 tcpdump，保证覆盖整个 modem 抓取窗口。
                    if (receiveModemState) {
                        return;
                    }
                    if (aManager != null) {
                        TcmpDumpRestartPendIntent = PendingIntent.getBroadcast(mContext, 0,
                                new Intent(ACTION_PENDINT_TCPDUMP_RESTART),
                                NotificationFlag.NOTIFICATION_FLAG_HEADER);
                        aManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime()
                                        + ConfigOptions.MonitorCondition.FREQUENT_REQUEST_PERIOD,
                                TcmpDumpRestartPendIntent);
                    }
                    clearOldFile(true, false);
                    return;
                }
                if (ACTION_PENDINT_TCPDUMP_RESTART.equals(action)) {
                    graspTcpDump(true);
                    if (aManager == null) {
                        aManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                    }
                    if (aManager != null) {
                        TcmpDumpEndPendIntent = PendingIntent.getBroadcast(mContext, 0,
                                new Intent(ACTION_PENDINT_TCPDUMP_OUTTIME),
                                NotificationFlag.NOTIFICATION_FLAG_HEADER);
                        aManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime() + TCP_DUMP_OUTTIME * BASE_HOUR_TIME,
                                TcmpDumpEndPendIntent);
                    }
                    return;
                }
                if (ACTION_PENDINT_MODEM_NETWORK_OUTTIME.equals(action)) {
                    closeNetModemLog();
                    return;
                }
                if (ACTION_PENDINT_MODEM_NETWORK_DELAYTIME.equals(action)) {
                    whenNetModemCatchEnd();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NETWORK_MODEN_LOG_START);
        filter.addAction(ACTION_NETWORK_MODEN_LOG_STOP);
        filter.addAction(ACTION_PENDINT_TCPDUMP_OUTTIME);
        filter.addAction(ACTION_PENDINT_TCPDUMP_RESTART);
        filter.addAction(ACTION_PENDINT_MODEM_NETWORK_OUTTIME);
        filter.addAction(ACTION_PENDINT_MODEM_NETWORK_DELAYTIME);
        this.mContext.registerReceiver(this.startMonitorReceiver, filter);
        this.isRegistBrodReceive = true;
    }

    /** modem 侧开始抓取：记录状态并挂超时闹钟。 */
    private void whenNetModemCatchStart(int moniterTime) {
        LogUtil.d(TAG, "whenNetModemCatchStart moniterTime:" + moniterTime);
        wakeUpCpuForShort();
        if (this.mNetWorkLogStatus.getCurrentStatus() == NetWorkModemLogStatus.Status.FETCHING) {
            LogUtil.d(TAG, "current is FETCHING");
            return;
        }
        if (this.receiveModemState) {
            return;
        }
        this.receiveModemState = true;

        // 空间不足先停 tcpdump；仍然不足就直接结束监控。
        if (!TextUtils.isEmpty(SdCardUtil.execNormalCheck(Constants.LogCollect.MIN_REMAIN_STORAGE_SPACE))) {
            LogUtil.d(TAG, "Insufficient sdcard space");
            graspTcpDump(false);
            clearOldFile(true, false);
            if (!SdCardUtil.execSpaceCheck(LIMIT_SPACE_RATE)) {
                LogUtil.d(TAG, "Insufficient sdcard space");
                this.notifyStr = "空间不足, 停止监控";
                notifyThreadReport(this.mLogStrategy);
                dealcloseNetLogMoniter();
                return;
            }
        }

        saveCurrentStatus(NetWorkModemLogStatus.Status.FETCHING);
        graspModemLog(true);
        if (this.aManager == null) {
            this.aManager = (AlarmManager) this.mContext.getSystemService(Context.ALARM_SERVICE);
        }
        if (moniterTime <= 0 || this.aManager == null) {
            return;
        }
        this.ModemEndPendIntent = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(ACTION_PENDINT_MODEM_NETWORK_OUTTIME), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.aManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ((long) (BASE_MIN_TIME * moniterTime)),
                this.ModemEndPendIntent);
    }

    /** 抓取窗口结束：进入压缩上传。 */
    private void whenNetModemCatchEnd() throws Throwable {
        LogUtil.d(TAG, "whenNetModemCatchEnd: " + this.receiveModemState);
        if (this.aManager != null && this.ModemDelayPendIntent != null) {
            this.aManager.cancel(this.ModemDelayPendIntent);
        }
        if (this.mLogStrategy != null) {
            preUploadNetworkLogFile(this.mLogStrategy, this.mNetWorkLogStatus);
        } else {
            LogUtil.d(TAG, "mLogStrategy is null");
        }
    }

    // ------------------------------------------------------------------
    // IMobileNetLogServe
    // ------------------------------------------------------------------

    @Override
    public void checkLastMoblieNetWorkLog() {
        LogUtil.i(TAG, "checkLastMoblieNetWorkLog");
        if (modemAndtcpOnTheWay()) {
            return;
        }
        boolean hasModemStateStrategy = false;
        if (!lastOnTheWay(true, "")) {
            String modemStateStrategy = SharedTool.getModemStateStrategy(this.mContext);
            LogUtil.i(TAG, "judge strategy in sp , old ModemStateStrategy = " + modemStateStrategy);
            if (TextUtils.isEmpty(modemStateStrategy)) {
                return;
            }
            LogUtil.d(TAG, "checkLastMoblieNetWorkLog getModemStateStrategy");
            hasModemStateStrategy = true;
        }
        try {
            if (this.mNetWorkLogStatus != null && this.mLogStrategy != null) {
                checkStatus(this.mNetWorkLogStatus.getCurrentStatus());
            } else if (hasModemStateStrategy) {
                stopMoniter(true, null);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void endMobileNetLog() {
        onRelease();
    }

    @Override
    public void startMobileNetLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "startMobileNetLog");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
            return;
        }
        this.mLogStrategy = strategies.get(0);
        if (this.mLogStrategy.getExtra() == null) {
            LogUtil.d(TAG, "grabType is null, return");
            return;
        }
        String grabType = this.mLogStrategy.getExtra().getGrabType();
        LogUtil.d(TAG, "logStrategies getAppPackage:" + this.mLogStrategy.getAppPackage()
                + " getUserId:" + this.mLogStrategy.getUserId() + " grabType:" + grabType);
        if (modemAndtcpOnTheWay() || lastOnTheWay(true, grabType)) {
            return;
        }

        this.mNetWorkLogStatus = new NetWorkModemLogStatus(
                new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(Long.valueOf(System.currentTimeMillis())));
        SharedTool.saveNetWorkModemLogStrategy(this.mContext, JSONUtil.toJSON(this.mLogStrategy));
        SharedTool.saveNetWorkModemLogStatus(this.mContext, JSONUtil.toJSON(this.mNetWorkLogStatus));
        setGsmNetWorkSwitch(true);
        startMoniter();

        LogCollectUtil.updateCollectState(this.mContext, this.mLogStrategy.getLogId().longValue(), 3,
                new LogStateServeImpl.UploadSuccessCallback() {
                    @Override
                    public void onFailure() {
                        LogUtil.d(TAG, "updateCollectState MONITORING fail! ");
                        LogCollectUtil.updateCollectState(mContext,
                                mLogStrategy.getLogId().longValue(), 3);
                    }

                    @Override
                    public void onSuccess() {
                        LogUtil.d(TAG, "updateCollectState MONITORING success. ");
                    }
                });
    }

    /** 释放所有资源（tcpdump、modem 抓取、上报线程、闹钟、广播、网络监听）。 */
    public void onRelease() {
        LogUtil.i(TAG, "onRelease");
        graspTcpDump(false);
        graspModemLog(false);
        if (this.mNotifiThread != null) {
            synchronized (this.mNotifyLock) {
                this.mNotifiThread.stopNotifyThread();
                this.mNotifyLock.notify();
            }
            this.mNotifiThread = null;
        }
        if (this.aManager != null && this.ModemEndPendIntent != null) {
            this.aManager.cancel(this.ModemEndPendIntent);
        }
        if (this.aManager != null && this.TcmpDumpEndPendIntent != null) {
            this.aManager.cancel(this.TcmpDumpEndPendIntent);
        }
        if (this.startMonitorReceiver != null) {
            this.mContext.unregisterReceiver(this.startMonitorReceiver);
            this.startMonitorReceiver = null;
            this.isRegistBrodReceive = false;
        }
        unRegisterNetChange();
    }
}
