package com.xtc.systemagent.module.log.serve.impl;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.xtc.architecture.mvp.BasePresenter;
import com.xtc.log.LogUtil;
import com.xtc.system.account.InitServiceData;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.module.Constants;
import com.xtc.systemagent.module.log.bean.CallLogStatus;
import com.xtc.systemagent.module.log.serve.ICallFailureServer;
import com.xtc.systemagent.net.bean.CallOtherParms;
import com.xtc.systemagent.net.bean.ExtraMes;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import com.xtc.systemagent.util.LmkPersistHelper;
import com.xtc.systemagent.util.LogCollectUtil;
import com.xtc.systemagent.util.NetworkUtil;
import com.xtc.systemagent.util.PropertiesTemp;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

/**
 * 通话异常（call failure）日志的抓取与上传实现。
 *
 * <p>两条业务线：</p>
 * <ul>
 *   <li>"主动"策略（{@code unActive == false}）：正常拨号/来电时抓取 modem 日志，
 *       并按 {@code fetchingTime} 重复多轮；</li>
 *   <li>"被动"策略（{@code unActive == true}）：只有在通话出错时才抓取一次。</li>
 * </ul>
 *
 * <p>抓取结束后按照 <{@link CallLogStatus.Status}> 状态机压缩上传，
 * 失败按指数退避重试，最多 10 次。</p>
 */
public class CallFailureLogServeImpl extends BasePresenter<Object>
        implements ICallFailureServer, WifiHelper.ConnectivityCallback {

    private static final String TAG = "Log_CallFailureLogServeImpl";

    /** 重试上传的最大次数。 */
    private static final int MAX_RETRY_UPLOAD_NUM = 10;

    /** 单次监控最多记录的通话网络次数。 */
    private static final int MAX_MONITOR_NUMBER = 30;

    /** 通知小伙伴"我异常了"的最大重试次数。 */
    private static final int MAX_CALL_OTHER_RETRY = 3;

    /** 上传网络限制：仅 WiFi。 */
    private static final int UPLOAD_NET_WIFI = 2;

    /** 通知线程存活标记。 */
    private static volatile boolean notifyThreadRunable = true;

    private static SoftReference<CallFailureLogServeImpl> softRef;

    private final AlarmManager am;
    private final PowerManager.WakeLock wakeLock;
    private final Object mNotifyLock;
    private final Intent mTimeOutIntent;
    private final Intent mWaitModenIntent;

    /** 抓取超时（分钟）对应的状态：超时。 */
    private final int OUTOFTIMES_STATE;
    /** 抓取超时（分钟）对应的状态：正常结束。 */
    private final int NORMALENT_STATE;
    /** 通话出错后延迟多久开始抓取（毫秒）。 */
    private final int DELAY_GRAB_TIME;

    private LogStrategy logStrategy;
    private ExtraMes extraStrategy;
    private CallLogStatus callLogStatus;
    private String notifyStr;
    private LogStrategy mRportlogStrategy;
    private String zipFileName;

    private NotifiServerThread mNotifiThread;
    private Thread mNotifiThreadRef;

    private broadcastReceiverHolder unusedHolder;

    private BroadcastReceiver callLogReceiver;
    private BroadcastReceiver retryReceiver;

    private PendingIntent closePI;
    private PendingIntent retryPI;
    private PendingIntent waitModenPI;
    private PendingIntent CapturelogPI;

    private Subscription callOtherErrorSub;
    private int callOtherNum;
    private int waitModemTimes;
    private int limitrate;
    private int currentCallState;
    private int preCallState;

    private boolean isModemError;
    private boolean isActiveDialing;
    private boolean isActiveOffHock;
    private boolean isRetryTellOtherCallError;
    private boolean callLogregState;
    private boolean netChangeregState;
    private boolean firstStart;
    private boolean mobConnected;
    private boolean wifiConnected;
    private boolean uploading;

    private String friendWatchId;
    private WifiHelper wifiHelper;

    /** 占位内部类，仅为保留原厂编译产物中的空类。 */
    private static final class broadcastReceiverHolder {
    }

    /**
     * 串行上报线程：等到 {@link #mNotifyLock} 被唤醒后把 {@link #notifyStr}
     * 上报给服务端。
     */
    private class NotifiServerThread extends Thread {

        private NotifiServerThread() {
        }

        @Override
        public void run() {
            super.run();
            while (notifyThreadRunable) {
                try {
                    synchronized (mNotifyLock) {
                        try {
                            mNotifyLock.wait();
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                    if (notifyThreadRunable) {
                        if (mRportlogStrategy != null) {
                            LogStateServeImpl.getInstance(mContext).notifyErrorInfo(mRportlogStrategy, notifyStr);
                        } else {
                            LogUtil.i(TAG, "mRportlogStrategy is null, not report");
                        }
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

    private CallFailureLogServeImpl(Context context) {
        super(context);
        this.OUTOFTIMES_STATE = 1;
        this.NORMALENT_STATE = 2;
        this.DELAY_GRAB_TIME = 6000;
        this.waitModemTimes = 2;
        this.limitrate = 50;
        this.isModemError = false;
        this.currentCallState = -1;
        this.preCallState = -1;
        this.mNotifyLock = new Object();
        this.netChangeregState = false;
        this.callLogregState = false;
        this.firstStart = true;

        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            LogUtil.e(TAG, "PowerManager is null !");
            this.wakeLock = null;
            this.mTimeOutIntent = new Intent(Constants.callLogBroadcast.RECEIVE_CALL_LOG_TIME_OUT);
            this.mWaitModenIntent = new Intent(Constants.callLogBroadcast.RECEIVE_CALL_WAITMODEM_TIME_OUT);
            return;
        }
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        this.wakeLock.setReferenceCounted(false);
        this.mTimeOutIntent = new Intent(Constants.callLogBroadcast.RECEIVE_CALL_LOG_TIME_OUT);
        this.mWaitModenIntent = new Intent(Constants.callLogBroadcast.RECEIVE_CALL_WAITMODEM_TIME_OUT);
        if (this.mNotifiThread == null) {
            this.mNotifiThread = new NotifiServerThread();
            this.mNotifiThread.start();
        }
    }

    public static synchronized CallFailureLogServeImpl getInstance(Context context) {
        CallFailureLogServeImpl instance = softRef == null ? null : softRef.get();
        if (instance == null) {
            instance = new CallFailureLogServeImpl(context);
            softRef = new SoftReference<>(instance);
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // 唤醒锁
    // ------------------------------------------------------------------

    private void acquireCpuForLong() {
        LogUtil.i(TAG, "acquire Cpu For Long");
        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        this.wakeLock.acquire();
    }

    private void wakeUpCpuForShort() {
        LogUtil.i(TAG, "call Log Wake up Cpu for short");
        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        this.wakeLock.acquire(3000L);
    }

    private void releaseWakeUp() {
        LogUtil.i(TAG, "release Wake Up !");
        if (this.wakeLock == null || !this.wakeLock.isHeld()) {
            return;
        }
        this.wakeLock.release();
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    private String callLogStatusToString(int status) {
        switch (status) {
            case CallLogStatus.Status.MONITORING:
                return "UPLOAD_FILE_SUCCESS";
            case CallLogStatus.Status.FETCHING:
                return "UPLOAD_FILE_FAILURE";
            case CallLogStatus.Status.PRE_UPLOAD:
                return "MONITORING";
            case CallLogStatus.Status.UPLOAD_FAILURE:
                return "FETCHING";
            default:
                return "";
        }
    }

    private void saveCurrentStatus(int currentStatus) {
        if (this.callLogStatus != null) {
            this.callLogStatus.setCurrentStatus(currentStatus);
            SharedTool.saveCallLogStatus(this.mContext, JSONUtil.toJSON(this.callLogStatus));
        }
    }

    private void setCurrentCallState(int callState) {
        this.currentCallState = callState;
    }

    private synchronized void setModemError(boolean modemError) {
        this.isModemError = modemError;
    }

    private synchronized boolean judgeModemError() {
        LogUtil.i(TAG, "judgeModemError isModemError = " + this.isModemError);
        return this.isModemError;
    }

    /** 开关 modem 日志属性；打开时顺带把进程挂到 LMK 白名单。 */
    private void setCallLogSwitch(boolean open) {
        boolean currentOn = "1".equals(SystemPropertiesUtils.get(SystemConstants.PROP_LOG_TAG_MODEM, ""));
        LogUtil.i(TAG, "setModemLogSwitch --> oldStatus = " + currentOn + " , current isOpen = " + open);
        if (currentOn != open) {
            wakeUpCpuForShort();
            SystemPropertiesUtils.set(SystemConstants.PROP_LOG_TAG_MODEM, open ? 1 : 0);
            LogUtil.i(TAG, "PROPERTY_SYSTEM_MODEM_LOG: "
                    + SystemPropertiesUtils.get(SystemConstants.PROP_LOG_TAG_MODEM, ""));
            if (open) {
                LmkPersistHelper.registerLmkPersist(this.mContext.getPackageName());
            }
        }
    }

    private void clearLastLogMes() {
        LogUtil.i(TAG, "clearLastLogMes --> sp and property");
        SharedTool.saveCallLogStrategy(this.mContext, "");
        SharedTool.saveCallLogStatus(this.mContext, "");
        SharedTool.saveCallNetNumber(this.mContext, 0);
        PropertiesTemp.setCallLogFetchDuration(-1);
    }

    private void clearOldFile(boolean closeSwitch) {
        LogUtil.i(TAG, "clearOldFile , isCloseSwitch = " + closeSwitch);
        SharedTool.saveCallLogOverTime(this.mContext, "");
        clearOldFile(closeSwitch, true, false);
    }

    /**
     * 清理残留。
     *
     * @param closeSwitch 是否同时关掉 modem 日志开关
     * @param clearFile   是否删除原始日志目录
     * @param isReboot    开机场景下若 modem 自启动就不清理
     */
    private void clearOldFile(boolean closeSwitch, boolean clearFile, boolean isReboot) {
        LogUtil.d(TAG, "clearOldFile , closeSwitch = " + closeSwitch + " , clearFile = " + clearFile);
        if (isReboot) {
            String modemSelf = SystemPropertiesUtils.get(Constants.SystemProperty.MODEM_SELF, "");
            boolean modemSelfOn = !TextUtils.isEmpty(modemSelf) && modemSelf.equals("1");
            LogUtil.i(TAG, "modemSelfStatus = " + modemSelfOn);
            if (modemSelfOn) {
                return;
            }
        }
        if (closeSwitch) {
            setCallLogSwitch(false);
        }
        if (!clearFile || isReboot) {
            return;
        }
        UploadFile.deleteFile(Constants.ModemLogPath.MODEM_LOG_FILE_PATH, getClass().getSimpleName());
    }

    private void clearPreOldFile() {
        UploadFile.deleteFile(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH, getClass().getSimpleName());
    }

    // ------------------------------------------------------------------
    // 闹钟
    // ------------------------------------------------------------------

    private void cancelLogRegister() {
        LogUtil.i(TAG, "cancelLogRegister callLogregState=" + this.callLogregState);
        if (!this.callLogregState) {
            return;
        }
        try {
            if (this.callLogReceiver != null) {
                this.mContext.unregisterReceiver(this.callLogReceiver);
                LogUtil.i(TAG, "unregister success");
                this.callLogregState = false;
            }
            if (this.am == null || this.closePI == null) {
                return;
            }
            this.am.cancel(this.closePI);
        } catch (Exception e) {
            LogUtil.i(TAG, "unRegister error = " + e);
        }
    }

    private void cancelRetryAlarm() {
        LogUtil.d(TAG, "cancelRetryAlarm");
        try {
            if (this.retryReceiver != null) {
                this.mContext.unregisterReceiver(this.retryReceiver);
            }
            if (this.am != null && this.retryPI != null) {
                this.am.cancel(this.retryPI);
            }
            this.retryReceiver = null;
        } catch (Exception e) {
            LogUtil.i(TAG, "cancelRetryAlarm error = " + e);
        }
    }

    private void startRetryAlarm(long delayMinutes) {
        LogUtil.i(TAG, "startRetryAlarm = " + delayMinutes);
        registerRetryReceiver();
        this.retryPI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(Constants.callLogBroadcast.CALL_LOG_RETRY_UPLOAD),
                NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (60 * delayMinutes * 1000), this.retryPI);
    }

    private void registerRetryReceiver() {
        LogUtil.i(TAG, "registerRetryReceiver");
        this.retryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "retryReceiver = " + action);
                if (action.equals(Constants.callLogBroadcast.CALL_LOG_RETRY_UPLOAD)) {
                    cancelRetryAlarm();
                    if (lastOnTheWay(false)) {
                        try {
                            preUploadLogFile();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.callLogBroadcast.CALL_LOG_RETRY_UPLOAD);
        this.mContext.registerReceiver(this.retryReceiver, filter);
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
        boolean hasPreFile = false;
        LogUtil.d(TAG, "dealNetworkChange current mobConnected = " + mobConnected
                + " , wifiConnected = " + wifiConnected
                + " , old mobConnected = " + this.mobConnected
                + " , wifiConnected = " + this.wifiConnected
                + " , uploading = " + this.uploading);

        if ((!mobConnected && !wifiConnected)
                || ((mobConnected == this.mobConnected && wifiConnected == this.wifiConnected)
                || this.uploading)) {
            LogUtil.d(TAG, "不符合 上传条件 , pass");
        } else if (lastOnTheWay(false)) {
            int currentStatus = this.callLogStatus.getCurrentStatus();
            int uploadNet = this.logStrategy.getUploadNet();
            if ((uploadNet == UPLOAD_NET_WIFI && wifiConnected)
                    || (uploadNet != UPLOAD_NET_WIFI && mobConnected)) {
                if (new File(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH).exists()) {
                    saveCurrentStatus(CallLogStatus.Status.PRE_UPLOAD);
                    hasPreFile = true;
                }
                LogUtil.d(TAG, "modem prefile " + Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH
                        + " is " + hasPreFile);
            }
            if ((currentStatus == CallLogStatus.Status.PRE_UPLOAD
                    || currentStatus == CallLogStatus.Status.UPLOAD_FAILURE
                    || hasPreFile)
                    && this.callLogStatus.getCurrentRetryUploadNum() < MAX_RETRY_UPLOAD_NUM) {
                cancelRetryAlarm();
                try {
                    preUploadLogFile();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        this.mobConnected = mobConnected;
        this.wifiConnected = wifiConnected;
    }

    // ------------------------------------------------------------------
    // 上报
    // ------------------------------------------------------------------

    /** 唤醒上报线程，把 {@link #notifyStr} 发给服务端。 */
    public void notifyThreadReport() {
        synchronized (this.mNotifyLock) {
            this.mNotifyLock.notify();
        }
    }

    private void notifyUploadSuccess(String key, LogStrategy strategy, int collectStatus,
                                     int spaceType,
                                     LogStateServeImpl.UploadSuccessCallback callback) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            LogUtil.e(TAG, "bindNumber is null , return !");
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

    // ------------------------------------------------------------------
    // 调用状态判断
    // ------------------------------------------------------------------

    /** 判断当前是否"有一通电话正在进行"（主动拨号或免提接听）。 */
    private boolean judgedCalling(boolean isActive) {
        if (!isActive || this.isActiveDialing || this.isActiveOffHock) {
            LogUtil.i(TAG, "judgedCalling is true , have call");
            return true;
        }
        LogUtil.i(TAG, "judgedCalling is false active , need use start prepare");
        return false;
    }

    /** 判断上一次的任务是否还在进行；不在则清理残留。 */
    private boolean lastOnTheWay(boolean isReboot) {
        String strategyJson = SharedTool.getCallLogStrategy(this.mContext);
        String statusJson = SharedTool.getCallLogStatus(this.mContext);
        LogUtil.i(TAG, "judge lastOnTheWay , isReboot = " + isReboot
                + "\nin sp , oldLogStrategy = " + strategyJson + ", oldStatus = " + statusJson);
        if (TextUtils.isEmpty(strategyJson) || TextUtils.isEmpty(statusJson)) {
            LogUtil.w(TAG, "return false ! at lease one sp value is empty , reset something !");
            clearLastLogMes();
            clearOldFile(true, true, isReboot);
            return false;
        }
        this.logStrategy = JSONUtil.fromJSON(strategyJson, LogStrategy.class);
        this.callLogStatus = JSONUtil.fromJSON(statusJson, CallLogStatus.class);
        LogUtil.i(TAG, "return true ! ok , get from sp , current callLogStatus = "
                + this.callLogStatus + "\nlogStrategy = " + this.logStrategy);
        return true;
    }

    /** 首次启动时若正在振铃，直接开始抓取。 */
    private void fistCheckPhone(Context context) {
        this.firstStart = false;
        if (this.callLogStatus.getCurrentFetchingTime() >= this.callLogStatus.getFetchingTime()) {
            return;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(com.xtc.bigdata.common.constants.Constants.PHONE);
        if (telephonyManager == null) {
            LogUtil.e(TAG, "TelephonyManager is null , return !");
            return;
        }
        int callState = telephonyManager.getCallState();
        if (1 == callState || 2 == callState) {
            LogUtil.i(TAG, "fistCheckPhone call state = RINGING  and active offHock");
            this.isActiveOffHock = true;
            if (this.logStrategy == null || this.extraStrategy == null) {
                LogUtil.i(TAG, "fistCheckPhone last on the way");
            } else {
                startFetchCallLog(this.logStrategy, this.extraStrategy.getFetchingDuration(), true);
            }
        }
    }

    // ------------------------------------------------------------------
    // 异常上报给小伙伴
    // ------------------------------------------------------------------

    /**
     * 每 50s 重试一次"告诉小伙伴我异常了"，最多 {@link #MAX_CALL_OTHER_RETRY} 次。
     */
    private void friendCallLog(String accountId, LogStrategy strategy) {
        this.isRetryTellOtherCallError = true;
        String watchId = InitServiceData.getWatchId(this.mContext);
        LogUtil.i(TAG, "watchId = " + watchId + "--syncNetOtherCallLog = " + strategy);

        final CallLogServeImpl callLogServe = new CallLogServeImpl(this.mContext);
        final CallOtherParms params = new CallOtherParms();
        params.setAccountId(accountId);
        params.setCallerWatchId(watchId);
        params.setId(strategy.getLogId());
        params.setLogType("isError");

        this.callOtherErrorSub = Observable.interval(0L, 50L, TimeUnit.SECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long value) {
                        LogUtil.i(TAG, "onNext aLong = " + value + " , callOtherNum = " + callOtherNum);
                        callOtherNum++;
                        if (callOtherNum > MAX_CALL_OTHER_RETRY) {
                            LogUtil.i(TAG, "retry time is over");
                            cancelFriendRetry();
                        } else {
                            LogUtil.d(TAG, "告诉小伙伴 , 我异常了 ~");
                            callLogServe.openOtherWatchCallLog(params, new CallLogServeImpl.OtherCallBack() {
                                @Override
                                public void onFailure() {
                                    LogUtil.i(TAG, "tell friend onFailure");
                                }

                                @Override
                                public void onSuccess() {
                                    LogUtil.i(TAG, "tell friend onSuccess");
                                    cancelFriendRetry();
                                }
                            });
                        }
                    }
                });
    }

    private void cancelFriendRetry() {
        LogUtil.i(TAG, "cancelFriendRetry");
        if (this.callOtherErrorSub != null) {
            this.isRetryTellOtherCallError = false;
            this.callOtherErrorSub.unsubscribe();
            this.callOtherNum = 0;
        }
    }

    /** 通知服务端"我要打电话了"。 */
    private void syncNetOtherCallLog(String accountId, LogStrategy strategy) {
        String watchId = InitServiceData.getWatchId(this.mContext);
        LogUtil.i(TAG, "告诉小伙伴 , 我要打电话了 , watchId = " + watchId
                + " , syncNetOtherCallLog = " + strategy);
        CallLogServeImpl callLogServe = new CallLogServeImpl(this.mContext);
        CallOtherParms params = new CallOtherParms();
        params.setAccountId(accountId);
        params.setCallerWatchId(watchId);
        params.setId(strategy.getLogId());
        callLogServe.openOtherWatchCallLog(params, null);
    }

    /** 记录并限制单次监控内的通话次数（最多 {@link #MAX_MONITOR_NUMBER} 次）。 */
    private boolean dealCallNetNumber(Context context, String friendWatchId) {
        if (TextUtils.isEmpty(friendWatchId)) {
            return false;
        }
        int callNetNumber = SharedTool.getCallNetNumber(context);
        LogUtil.i(TAG, "callNetNumber = " + callNetNumber);
        if (callNetNumber > MAX_MONITOR_NUMBER) {
            LogUtil.i(TAG, "net time is over");
            cancelLogRegister();
            unRegisterNetChange();
            clearLastLogMes();
            LogCollectUtil.updateCollectState(this.mContext, this.logStrategy.getLogId().longValue(), 2);
            return true;
        }
        SharedTool.saveCallNetNumber(context, callNetNumber + 1);
        syncNetOtherCallLog(friendWatchId, this.logStrategy);
        return false;
    }

    // ------------------------------------------------------------------
    // 抓取流程
    // ------------------------------------------------------------------

    private void startMonitorLog(boolean isActive) {
        LogUtil.i(TAG, "startMonitorLog isActive = " + isActive);
        registerCallLog(true);
    }

    private void startNextMonitoring() {
        LogUtil.i(TAG, "startNextMonitoring getCurrentFetchingTime:"
                + this.callLogStatus.getCurrentFetchingTime());
        this.callLogStatus.setCurrentRetryUploadNum(0);
        this.callLogStatus.setCurrentRetryUploadTime(0L);
        saveCurrentStatus(CallLogStatus.Status.MONITORING);
        startMonitorLog(true);
        cancelFriendRetry();
    }

    /** 一轮抓取结束后决定是否进入下一轮。 */
    private void dealIsNeedNextMonitor() {
        if (this.callLogStatus.getCurrentFetchingTime() < this.callLogStatus.getFetchingTime()) {
            LogUtil.i(TAG, "start next monitor");
            startNextMonitoring();
            LogCollectUtil.updateCollectState(this.mContext, this.logStrategy.getLogId().longValue(), 3);
        } else {
            LogUtil.i(TAG, "fetch time over");
            if (new File(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH).exists()) {
                return;
            }
            unRegisterNetChange();
            clearLastLogMes();
        }
    }

    /** 一次抓取没有捕获到异常时的处理。 */
    private boolean judgeCanFetchCallLog(int state) {
        LogUtil.i(TAG, "getCurrentFetchingTime:" + this.callLogStatus.getCurrentFetchingTime()
                + " getFetchingTime:" + this.callLogStatus.getFetchingTime());
        this.notifyStr = "";
        if (state == this.OUTOFTIMES_STATE) {
            this.notifyStr = "超时 ";
        }
        this.notifyStr += "第" + this.callLogStatus.getCurrentFetchingTime() + "次未捕获到异常";
        this.mRportlogStrategy = this.logStrategy;
        notifyThreadReport();
        setCallLogSwitch(false);
        if (this.callLogStatus.getCurrentFetchingTime() < this.callLogStatus.getFetchingTime()) {
            return true;
        }
        LogUtil.i(TAG, "judeCanFetchCallLog fetch time over");
        cancelLogRegister();
        if (new File(Constants.ModemLogPath.ZIP_FILE_PATH).exists()
                || new File(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH).exists()
                || new File(Constants.ModemLogPath.ZIP_FILE_PATH).exists()) {
            return false;
        }
        unRegisterNetChange();
        clearLastLogMes();
        return false;
    }

    /**
     * 结束一次通话后的处理。
     *
     * @param isActive   是否为主动触发的策略
     * @param isNormalCall 是否是一次正常通话（异常通话走上传流程）
     */
    private void dealCloseLog(boolean isActive, boolean isNormalCall) {
        LogUtil.i(TAG, "dealCloseLog isCallNormal = " + isNormalCall);
        cancelLogRegister();
        setCallLogSwitch(false);
        this.isActiveDialing = false;
        this.isActiveOffHock = false;
        this.friendWatchId = null;
        if (!isNormalCall) {
            SharedTool.saveCallLogOverTime(this.mContext, DateUtils.format("yyyyMMdd"));
            saveCurrentStatus(CallLogStatus.Status.PRE_UPLOAD);
            try {
                preUploadLogFile();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            clearOldFile(true);
            if (isActive) {
                startMonitorLog(true);
            } else {
                clearLastLogMes();
            }
        }
    }

    /** 正常通话结束，直接收工。 */
    private void isNormalCall(boolean isActive) {
        if (isActive && this.isActiveDialing && TextUtils.isEmpty(this.friendWatchId)) {
            LogUtil.i(TAG, "call failure is normal");
            dealCloseLog(isActive, true);
        }
        if (isActive && this.isActiveOffHock) {
            dealCloseLog(isActive, true);
        }
    }

    private void resetAlarmAndLog(boolean isActive) {
        if (!judgedCalling(isActive)) {
            return;
        }
        LogUtil.i(TAG, "resetAlarmAndLog");
        if (this.am != null && this.closePI != null) {
            this.am.cancel(this.closePI);
        }
        cancelFriendRetry();
        this.isActiveDialing = false;
        this.isActiveOffHock = false;
        this.friendWatchId = null;
        clearOldFile(true);
    }

    // ------------------------------------------------------------------
    // 压缩与上传
    // ------------------------------------------------------------------

    /** 归档目录（跨天用旧 tar.gz）。 */
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

    /** 上传前的网络检查。 */
    private void preUploadLogFile() throws Throwable {
        LogUtil.d(TAG, "preUploadLogFile");
        if (!lastOnTheWay(false)) {
            LogUtil.d(TAG, "no previous task , return !");
            return;
        }
        if (this.logStrategy.getUploadNet() == UPLOAD_NET_WIFI) {
            if (NetworkUtil.isWifiConnected(this.mContext)) {
                LogUtil.i(TAG, "wifi net is ok");
                uploadLogFile(this.logStrategy, this.callLogStatus);
                return;
            }
            LogUtil.e(TAG, "network is not Wifi");
            currentNetWorkLimit(true);
        } else if (NetworkUtil.isConnected(this.mContext)) {
            LogUtil.i(TAG, "wifi or traffic net is ok");
            uploadLogFile(this.logStrategy, this.callLogStatus);
            return;
        } else {
            LogUtil.e(TAG, "network is not Connected");
            currentNetWorkLimit(false);
        }
        LogUtil.i(TAG, "all net work is not ok !");
        retryUploadLog();
    }

    /**
     * 网络不满足时的处理：先上报原因；被动策略直接收尾，
     * 主动策略把已抓到的文件转存到 pre 目录等待下次。
     */
    private void currentNetWorkLimit(boolean needWifi) throws Throwable {
        boolean unActive = this.logStrategy.getExtra().isUnActive();
        if (unActive) {
            this.notifyStr = "从机文件已经保存";
        } else {
            this.notifyStr = "文件已经保存";
        }
        if (needWifi) {
            this.notifyStr += "请连接wifi";
        } else {
            this.notifyStr += "网络未连接";
        }
        this.mRportlogStrategy = this.logStrategy;
        notifyThreadReport();

        if (unActive) {
            LogUtil.i(TAG, "unActive close");
            clearLastLogMes();
            releaseWakeUp();
            return;
        }
        // modem 还在抓的时候不要动文件。
        if (!"1".equals(SystemPropertiesUtils.get(SystemConstants.PROP_LOG_TAG_MODEM, ""))) {
            if (new File(Constants.ModemLogPath.MODEM_LOG_FILE_PATH).exists()) {
                saveModemErrFile(Constants.ModemLogPath.MODEM_LOG_FILE_PATH, false);
            } else if (new File(Constants.ModemLogPath.ZIP_FILE_PATH).exists()) {
                saveModemErrFile(Constants.ModemLogPath.ZIP_FILE_PATH, true);
            }
        }
        LogUtil.i(TAG, "[F] isLimitLog: "
                + "1".equals(SystemPropertiesUtils.get("persist.sys.modem.limit", "")));
        releaseWakeUp();
    }

    /** 把多个路径压成一个 zip。 */
    private void preZipfile(String zipName, List<String> paths) throws Throwable {
        if (paths.isEmpty()) {
            LogUtil.e(TAG, "after remove , originalFilePaths is Empty");
            return;
        }
        // 去掉不存在的路径。
        for (int i = paths.size() - 1; i >= 0; i--) {
            String path = paths.get(i);
            if (!new File(path).exists()) {
                LogUtil.e(TAG, "preZipfile " + path + " , but file dose not exists");
                paths.remove(i);
            }
        }

        File dir = new File(Constants.ModemLogPath.ZIP_FILE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(new File(Constants.ModemLogPath.ZIP_FILE_PATH + zipName))));
            try {
                for (String path : paths) {
                    UploadFile.zipDirectory(zipOutputStream, new File(path), "");
                }
            } catch (Exception e) {
                e.printStackTrace();
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

        LogUtil.d(TAG, "ZIP_FILE_PATH has save " + zipName + " file");
        clearOldFile(true);
        UploadFile.deleteFile(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH, getClass().getSimpleName());
        dealIsNeedNextMonitor();
    }

    /**
     * 保存异常现场的 modem 日志到 pre 目录。
     *
     * @param path    源（原始目录或 zip 目录）
     * @param isZip   源是否是 zip
     */
    private void saveModemErrFile(String path, boolean isZip) throws Throwable {
        if (!SdCardUtil.execSpaceCheck(this.limitrate)) {
            return;
        }
        File source = new File(path);
        if (!source.exists()) {
            source = new File(Constants.ModemLogPath.ZIP_FILE_PATH);
            if (!source.exists()) {
                return;
            }
        }

        File preDir = new File(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH);
        long availableSize = SdCardUtil.getAvailableSize();
        long usedSize = 0;
        LogUtil.i(TAG, "saveModemErrFile sdcard Space: " + availableSize);

        if (source.isDirectory()) {
            for (File child : source.listFiles()) {
                usedSize += child.length();
            }
        }
        if (usedSize * 2 > availableSize) {
            LogUtil.i(TAG, "sdcard has Insufficient space");
            return;
        }

        if (isZip) {
            // 已经是 zip：直接搬过去；若目标已存在则逐个复制。
            boolean preExists = preDir.exists();
            LogUtil.i(TAG, "save pre ModemErrFile :" + preExists);
            if (!preExists) {
                source.renameTo(preDir);
            } else if (source.isDirectory()) {
                File[] children = source.listFiles();
                for (int i = 0; i < children.length; i++) {
                    if (children[i].getName().endsWith(".zip")) {
                        UploadFile.copyZipFile(source,
                                new File(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH
                                        + children[i].getName()));
                    }
                }
            }
        } else {
            // 原始目录：再压一层放到 pre 目录。
            LogUtil.i(TAG, "zip pre ModemErrFile");
            if (!preDir.exists()) {
                preDir.mkdirs();
            }
            String prePath = Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH + "ModemLog_"
                    + this.logStrategy.getLogId() + "_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Long.valueOf(System.currentTimeMillis()))
                    + "_" + this.callLogStatus.getCurrentFetchingTime() + ".zip";
            File preFile = new File(prePath);
            LogUtil.i(TAG, "saveModemErrFile: " + prePath);
            if (!preFile.exists()) {
                ZipOutputStream zipOutputStream = null;
                try {
                    zipOutputStream = new ZipOutputStream(new BufferedOutputStream(
                            new FileOutputStream(preFile)));
                    try {
                        UploadFile.zipDirectory(zipOutputStream, source, "");
                    } catch (Exception e) {
                        e.printStackTrace();
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
            }
        }

        clearOldFile(true);
        dealIsNeedNextMonitor();
    }

    /**
     * 上传日志文件。
     *
     * <p>状态 3（待上传）：先压缩再上传；状态 4（上传失败）：直接重传已有 zip。</p>
     */
    private void uploadLogFile(final LogStrategy strategy, final CallLogStatus status) throws Throwable {
        if (status.getCurrentStatus() == CallLogStatus.Status.UPLOAD_FAILURE) {
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
                            saveCurrentStatus(CallLogStatus.Status.UPLOAD_FAILURE);
                            try {
                                retryUploadLog();
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                            releaseWakeUp();
                            uploading = false;
                        }

                        @Override
                        public void onUploadFileSuccess(String key) {
                            LogUtil.d(TAG, "UPLOAD_FAILURE uploadModemLogFile onUploadFileSuccess " + key);
                            saveCurrentStatus(CallLogStatus.Status.UPLOAD_SUCCESS);
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

        if (status.getCurrentStatus() != CallLogStatus.Status.PRE_UPLOAD) {
            LogUtil.e(TAG, "uploadModemLogFile is not fetching");
            return;
        }

        this.uploading = true;
        acquireCpuForLong();

        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            bindNumber = "NoBindNumber";
        }
        String prefix = "LOG_CALL_" + bindNumber + "_" + strategy.getLogId() + "_"
                + System.currentTimeMillis() + "_";
        if (strategy.getExtra() == null || !strategy.getExtra().isUnActive()) {
            this.zipFileName = prefix + status.getCurrentFetchingTime() + ".zip";
        } else {
            this.zipFileName = prefix + ".zip";
        }
        LogUtil.d(TAG, "uploadLogFile uploadZipFile currentStatus = " + status.getCurrentStatus()
                + " , zipFileName = " + this.zipFileName);

        ArrayList<String> paths = new ArrayList<>();
        paths.add(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
        paths.add(getLogsCollectorPath());
        paths.add(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH);
        preZipfile(this.zipFileName, paths);

        final String finalZipName = this.zipFileName;
        UploadFile.dealUploadFile(this.mContext, this.zipFileName,
                Constants.ModemLogPath.ZIP_FILE_PATH, true, new UploadFile.UploadFileCallback() {
                    @Override
                    public void onNoLogFile() {
                        LogUtil.d(TAG, "onNoLogFile !");
                        dealUploadFailure(strategy, status);
                        releaseWakeUp();
                        uploading = false;
                    }

                    @Override
                    public void onUploadFileFailure(String error) {
                        LogUtil.e(TAG, "uploadModemLogFile onUploadFileFailure " + error);
                        status.setZipFileName(finalZipName);
                        saveCurrentStatus(CallLogStatus.Status.UPLOAD_FAILURE);
                        try {
                            retryUploadLog();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        releaseWakeUp();
                        uploading = false;
                    }

                    @Override
                    public void onUploadFileSuccess(String key) {
                        LogUtil.d(TAG, "PRE_UPLOAD uploadModemLogFile onUploadFileSuccess " + key);
                        saveCurrentStatus(CallLogStatus.Status.UPLOAD_SUCCESS);
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
                                        LogUtil.i(TAG, "upload file onSuccess");
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

    private void dealUploadFailure(LogStrategy strategy, CallLogStatus status) {
        LogUtil.i(TAG, "dealUploadFailure upload file failed");
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 2);
        if (strategy.getExtra().isUnActive()) {
            LogUtil.i(TAG, "dealUploadFailure unActive close");
            clearLastLogMes();
            releaseWakeUp();
        } else {
            try {
                if (new File(Constants.ModemLogPath.ZIP_FILE_PATH).exists()) {
                    saveModemErrFile(Constants.ModemLogPath.ZIP_FILE_PATH, true);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            releaseWakeUp();
        }
    }

    private void dealUploadFileSuccess(LogStrategy strategy, CallLogStatus status) {
        LogUtil.i(TAG, "dealUploadFileSuccess = " + strategy);
        if (!strategy.getExtra().isUnActive()) {
            dealIsNeedNextMonitor();
            releaseWakeUp();
        } else {
            LogUtil.i(TAG, "unActive close");
            clearLastLogMes();
            releaseWakeUp();
        }
    }

    /** 上传失败后的指数退避重试。 */
    private void retryUploadLog() throws Throwable {
        LogUtil.i(TAG, "retryUploadLog");
        if (this.callLogStatus.getCurrentRetryUploadNum() >= MAX_RETRY_UPLOAD_NUM) {
            LogUtil.i(TAG, "retryUploadLog retry end");
            dealUploadFailure(this.logStrategy, this.callLogStatus);
            return;
        }
        long now = System.currentTimeMillis();
        if (this.callLogStatus.getCurrentRetryUploadNum() != 0
                && this.callLogStatus.getCurrentRetryUploadTime() > now) {
            long remainMinutes = ((this.callLogStatus.getCurrentRetryUploadTime() - now) / 60) / 1000;
            LogUtil.d(TAG, "retryUploadLog durationMinutes = " + remainMinutes);
            startRetryAlarm(remainMinutes);
            return;
        }
        int nextNum = this.callLogStatus.getCurrentRetryUploadNum() + 1;
        long nextDelayMinutes = UploadFile.getNextRetryUploadTime(nextNum);
        long nextTime = now + (60 * nextDelayMinutes * 1000);
        LogUtil.d(TAG, "retryUploadLog nextNum = " + nextNum + "---nextTime = " + nextTime
                + "---durationMinutes = " + nextDelayMinutes);
        this.callLogStatus.setCurrentRetryUploadNum(nextNum);
        this.callLogStatus.setCurrentRetryUploadTime(nextTime);
        saveCurrentStatus(this.callLogStatus.getCurrentStatus());
        startRetryAlarm(nextDelayMinutes);
    }

    // ------------------------------------------------------------------
    // 广播接收
    // ------------------------------------------------------------------

    private void registerCallLog(final boolean isActive) {
        LogUtil.i(TAG, "broadcast registerCallPre , isActive = " + isActive
                + " ,callLogregState=" + this.callLogregState);
        if (this.callLogregState) {
            return;
        }
        this.callLogregState = true;
        this.callLogReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.i(TAG, "log call action = " + action
                        + " , isActiveDialing = " + isActiveDialing
                        + "--isActiveOffHock = " + isActiveOffHock + " , isActive = " + isActive);
                if (action == null) {
                    LogUtil.e(TAG, "action = null , return !");
                    return;
                }
                try {
                    dispatch(action, context, isActive, intent);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void dispatch(String action, Context context, boolean isActive, Intent intent)
                    throws Throwable {
                switch (action) {
                    case Constants.callLogBroadcast.RECEIVE_PRE_USER_CALL:
                        whenPrepareToCallOther(context, isActive, intent);
                        break;
                    case "android.intent.action.PHONE_STATE":
                        whenPhoneStateChange(context, isActive);
                        break;
                    case Constants.callLogBroadcast.RECEIVE_CALL_FAILURE:
                        receiveModenError(isActive, intent);
                        break;
                    case Constants.callLogBroadcast.RECEIVE_CALL_DELAYGRAB_TIMEOUT:
                        whenModemCatchEnd(isActive);
                        break;
                    case Constants.callLogBroadcast.CALL_LOG_OTHER_CALL_ERROR:
                        whenRemoteFriendError(isActive);
                        break;
                    case Constants.callLogBroadcast.RECEIVE_CALL_LOG_TIME_OUT:
                        whenClosePiTrigger(isActive);
                        break;
                    case Constants.callLogBroadcast.RECEIVE_CALL_WAITMODEM_TIME_OUT:
                        whenWaitTimeOut(isActive);
                        break;
                    case Constants.callLogBroadcast.BROADCAST_END_CALL:
                        break;
                    default:
                        LogUtil.i(TAG, "do not care action");
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        if (isActive) {
            filter.addAction(Constants.callLogBroadcast.RECEIVE_PRE_USER_CALL);
        }
        filter.addAction(Constants.callLogBroadcast.RECEIVE_CALL_FAILURE);
        filter.addAction(Constants.callLogBroadcast.RECEIVE_CALL_DELAYGRAB_TIMEOUT);
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction(Constants.callLogBroadcast.BROADCAST_END_CALL);
        filter.addAction(Constants.callLogBroadcast.CALL_LOG_OTHER_CALL_ERROR);
        filter.addAction(Constants.callLogBroadcast.RECEIVE_CALL_LOG_TIME_OUT);
        filter.addAction(Constants.callLogBroadcast.RECEIVE_CALL_WAITMODEM_TIME_OUT);
        this.mContext.registerReceiver(this.callLogReceiver, filter);
    }

    /** 收到 modem 错误：马上告诉小伙伴，并延迟 6s 开始抓取。 */
    private void receiveModenError(boolean isActive, Intent intent) throws Throwable {
        boolean isNormal = intent.getBooleanExtra(
                Constants.callLogBroadcast.REC_EXTRA_IS_NORMAL_CALL, true);
        LogUtil.i(TAG, "receiveModenError isNormal = " + isNormal);
        if (isNormal) {
            isNormalCall(isActive);
            return;
        }
        if (!judgedCalling(isActive)) {
            LogUtil.i(TAG, "收到modem错误, 但是本次抓取已经结束了");
            this.notifyStr = "收到modem错误, 但是本次监控结束了";
            this.mRportlogStrategy = this.logStrategy;
            notifyThreadReport();
            return;
        }
        setModemError(true);
        if (!isActive) {
            friendCallLog(this.logStrategy.getExtra().getCallerWatchId(), this.logStrategy);
        } else if (this.isActiveDialing && !TextUtils.isEmpty(this.friendWatchId)) {
            friendCallLog(this.friendWatchId, this.logStrategy);
        }
        this.CapturelogPI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(Constants.callLogBroadcast.RECEIVE_CALL_DELAYGRAB_TIMEOUT),
                NotificationFlag.NOTIFICATION_FLAG_HEADER);
        LogUtil.i(TAG, "start CapturelogPI time am");
        this.am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ((long) this.DELAY_GRAB_TIME), this.CapturelogPI);
    }

    private void whenModemCatchEnd(boolean isActive) throws Throwable {
        setModemError(false);
        if (judgedCalling(isActive)) {
            dealCloseLog(isActive, false);
        }
    }

    private void whenClosePiTrigger(boolean isActive) throws Throwable {
        if (judgedCalling(isActive) && !judgeModemError()) {
            cancelFriendRetry();
            if (isActive) {
                judgeCanFetchCallLog(this.OUTOFTIMES_STATE);
            } else {
                dealCloseLog(isActive, true);
            }
        }
    }

    private void whenRemoteFriendError(boolean isActive) throws Throwable {
        if (judgeModemError()) {
            return;
        }
        if (!isActive) {
            dealCloseLog(false, false);
        } else {
            if (!this.isActiveDialing || TextUtils.isEmpty(this.friendWatchId)) {
                return;
            }
            dealCloseLog(true, false);
        }
    }

    private void whenWaitTimeOut(boolean isActive) throws Throwable {
        if (!judgedCalling(isActive)) {
            if (isActive) {
                judgeCanFetchCallLog(this.NORMALENT_STATE);
                LogUtil.i(TAG, "whenWaitTimeOut judgedCalling return");
            }
            return;
        }
        if (judgeModemError()) {
            return;
        }
        if (isActive) {
            resetAlarmAndLog(isActive);
            judgeCanFetchCallLog(this.NORMALENT_STATE);
        } else {
            cancelFriendRetry();
            dealCloseLog(isActive, true);
        }
    }

    private void whenPhoneStateChange(Context context, boolean isActive) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(com.xtc.bigdata.common.constants.Constants.PHONE);
        if (telephonyManager == null) {
            LogUtil.e(TAG, "TelephonyManager is null , return !");
            return;
        }
        int callState = telephonyManager.getCallState();
        setCurrentCallState(callState);
        LogUtil.i(TAG, "callState = " + callState);

        if (this.isRetryTellOtherCallError) {
            LogUtil.i(TAG, "is retry tell other callError , return !");
            return;
        }

        if (callState != this.preCallState) {
            if (1 == callState && isActive) {
                LogUtil.i(TAG, "[F]来电话了");
            }
            // 通话结束后等 2s，让 modem 侧把日记刷盘。
            if (callState == 0 && this.preCallState != callState) {
                LogUtil.i(TAG, "[F]电话结束了");
                this.waitModenPI = PendingIntent.getBroadcast(this.mContext, 0,
                        this.mWaitModenIntent, NotificationFlag.NOTIFICATION_FLAG_HEADER);
                this.am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + ((long) (this.waitModemTimes * 1000)),
                        this.waitModenPI);
            }
            this.preCallState = callState;
        }

        if (judgedCalling(isActive)) {
            LogUtil.i(TAG, "have one calling , return !");
            return;
        }
        if (1 == callState && isActive) {
            LogUtil.i(TAG, "call state = RINGING  and active offHock");
            this.isActiveOffHock = true;
            if (this.logStrategy == null || this.extraStrategy == null) {
                LogUtil.i(TAG, "last on the way");
            } else {
                startFetchCallLog(this.logStrategy, this.extraStrategy.getFetchingDuration(), isActive);
            }
        }
    }

    private void whenPrepareToCallOther(Context context, boolean isActive, Intent intent) {
        if (this.isRetryTellOtherCallError) {
            LogUtil.i(TAG, "is retry to tell other callError , return !");
            return;
        }
        LogUtil.i(TAG, "[F] 开始打电话");
        if (judgedCalling(isActive)) {
            LogUtil.i(TAG, "already have one calling , return !");
            return;
        }
        this.isActiveDialing = true;
        this.friendWatchId = intent.getStringExtra(Constants.callLogBroadcast.REC_EXTRA_FRIEND_ID);
        LogUtil.i(TAG, "friendWatchId = " + this.friendWatchId);
        if (dealCallNetNumber(context, this.friendWatchId)) {
            return;
        }
        if (this.logStrategy == null || this.extraStrategy == null) {
            LogUtil.i(TAG, "last on the way");
        } else {
            startFetchCallLog(this.logStrategy, this.extraStrategy.getFetchingDuration(), isActive);
        }
    }

    // ------------------------------------------------------------------
    // 任务恢复
    // ------------------------------------------------------------------

    private void whenHasNegativeTask(int currentStatus) throws Throwable {
        LogUtil.d(TAG, "whenHasNegativeTask , currentStatus = " + currentStatus);
        if (currentStatus == CallLogStatus.Status.PRE_UPLOAD
                || currentStatus == CallLogStatus.Status.UPLOAD_FAILURE) {
            retryUploadLog();
        } else {
            clearOldFile(true);
            clearLastLogMes();
        }
    }

    private void whenHasPositiveTask(int currentStatus) throws Throwable {
        LogUtil.d(TAG, "whenHasPositiveTask , currentStatus = " + currentStatus);
        switch (currentStatus) {
            case CallLogStatus.Status.MONITORING:
                clearOldFile(true);
                startMonitorLog(true);
                break;
            case CallLogStatus.Status.FETCHING:
                clearOldFile(true);
                startMonitorLog(true);
                break;
            case CallLogStatus.Status.PRE_UPLOAD:
                File originDir = new File(Constants.ModemLogPath.MODEM_LOG_FILE_PATH);
                File zipDir = new File(Constants.ModemLogPath.ZIP_FILE_PATH);
                File preDir = new File(Constants.ModemLogPath.PRE_MODEM_LOG_FILE_PATH);
                if (originDir.exists()) {
                    this.notifyStr = "文件已经保存";
                    this.mRportlogStrategy = this.logStrategy;
                    notifyThreadReport();
                    LogUtil.i(TAG, "modem origin file is exists");
                    retryUploadLog();
                } else if (zipDir.exists()) {
                    LogUtil.i(TAG, "modem zip file is exists");
                    this.notifyStr = "压缩文件已经保存";
                    this.mRportlogStrategy = this.logStrategy;
                    notifyThreadReport();
                    saveCurrentStatus(CallLogStatus.Status.UPLOAD_FAILURE);
                    retryUploadLog();
                } else if (!preDir.exists()) {
                    clearOldFile(true);
                    dealIsNeedNextMonitor();
                } else {
                    LogUtil.i(TAG, "modem prefile is exists");
                    this.notifyStr = "prefile文件已经保存";
                    this.mRportlogStrategy = this.logStrategy;
                    notifyThreadReport();
                    retryUploadLog();
                }
                break;
            case CallLogStatus.Status.UPLOAD_FAILURE:
                retryUploadLog();
                break;
            default:
                clearOldFile(true);
                dealIsNeedNextMonitor();
                break;
        }
    }

    // ------------------------------------------------------------------
    // ICallFailureServer
    // ------------------------------------------------------------------

    @Override
    public void checkLastCallLog() {
        LogUtil.i(TAG, "checkLastCallLog ------------------->");
        try {
            if (!lastOnTheWay(true)) {
                LogUtil.d(TAG, "no previous task , return !");
                return;
            }
            int currentStatus = this.callLogStatus.getCurrentStatus();
            this.extraStrategy = this.logStrategy.getExtra();
            if (this.extraStrategy.isUnActive()) {
                whenHasNegativeTask(currentStatus);
            } else {
                if (this.firstStart) {
                    fistCheckPhone(this.mContext);
                }
                whenHasPositiveTask(currentStatus);
            }
            registerNetChange();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        LogUtil.i(TAG, "checkLastCallLog done ~");
    }

    @Override
    public void onDestroy() {
        LogUtil.i(TAG, "onDestroy");
        if (this.mNotifiThread != null) {
            synchronized (this.mNotifyLock) {
                this.mNotifiThread.stopNotifyThread();
                this.mNotifyLock.notify();
            }
            this.mNotifiThread = null;
        }
        this.logStrategy = null;
        this.extraStrategy = null;
        this.callLogStatus = null;
        this.wifiHelper = null;
    }

    @Override
    public void startDealCallLog(List<LogStrategy> strategies, boolean immediately) {
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "monitorCallLog logStrategies is empty");
            return;
        }
        LogStrategy strategy = strategies.get(0);
        LogUtil.i(TAG, "startDealCallLog isActive = " + immediately + ", --> logStrategy = " + strategy);

        ExtraMes extra = strategy.getExtra();
        if (extra == null) {
            LogUtil.e(TAG, "call extra strategy log is null , return !");
            return;
        }
        if (!TextUtils.isEmpty(SharedTool.getModemLogStatus(this.mContext))) {
            LogUtil.e(TAG, "fetchingCallFialLog last ModemLog on the way");
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 21);
            return;
        }
        if (lastOnTheWay(false)) {
            LogUtil.e(TAG, "last call Log on the way , return !");
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 21);
            return;
        }

        this.logStrategy = strategy;
        this.extraStrategy = extra;
        SharedTool.saveCallLogStrategy(this.mContext, JSONUtil.toJSON(strategy));
        this.callLogStatus = new CallLogStatus();
        clearOldFile(true);
        this.isActiveDialing = false;
        this.isActiveOffHock = false;
        PropertiesTemp.setCallLogFetchDuration(extra.getFetchingDuration());

        if (immediately) {
            this.callLogStatus.setFetchingTime(extra.getFetchingTime());
            this.callLogStatus.setCurrentFetchingTime(0);
            saveCurrentStatus(CallLogStatus.Status.MONITORING);
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), 3);
            startMonitorLog(true);
        } else {
            saveCurrentStatus(CallLogStatus.Status.FETCHING);
            registerCallLog(false);
            startFetchCallLog(strategy, strategy.getExtra().getFetchingDuration(), false);
        }
        registerNetChange();
    }

    @Override
    public void startFetchCallLog(LogStrategy strategy, int fetchingTime, boolean immediately) {
        Long logId = strategy.getLogId();
        LogUtil.i(TAG, "startFetchCallLog , logId = " + logId + " , duration = " + fetchingTime
                + " , isActive = " + immediately);

        String checkResult = SdCardUtil.execNormalCheck(Constants.LogCollect.MIN_REMAIN_STORAGE_SPACE);
        LogUtil.d(TAG, "checkString = " + checkResult);
        if (!TextUtils.isEmpty(checkResult)) {
            LogCollectUtil.updateCollectState(this.mContext, logId.longValue(),
                    Integer.valueOf(checkResult.split("-")[0]).intValue());
            return;
        }

        clearOldFile(false);
        this.callLogStatus.setCurrentFetchingTime(this.callLogStatus.getCurrentFetchingTime() + 1);
        int currentStatus = this.callLogStatus.getCurrentStatus();
        LogUtil.i(TAG, "CurrentFetchingTime: " + this.callLogStatus.getCurrentFetchingTime()
                + " , currentStatus: " + callLogStatusToString(currentStatus));
        saveCurrentStatus(currentStatus);
        setCallLogSwitch(true);
        this.closePI = PendingIntent.getBroadcast(this.mContext, 0,
                this.mTimeOutIntent, NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ((long) (fetchingTime * 60 * 1000)), this.closePI);
    }

    /** 取消通话异常日志策略。 */
    public boolean cancelCallFailLog(List<LogStrategy> strategies) {
        String oldStrategy = SharedTool.getCallLogStrategy(this.mContext);
        LogUtil.i(TAG, "cancelCallFailLog oldLogStrategy:" + oldStrategy);
        if (TextUtils.isEmpty(oldStrategy)) {
            return false;
        }
        this.notifyStr = "策略被取消";
        this.mRportlogStrategy = this.logStrategy;
        this.logStrategy = null;
        notifyThreadReport();
        if (this.isActiveOffHock || this.isActiveDialing) {
            setCallLogSwitch(false);
        }
        cancelFriendRetry();
        this.isActiveDialing = false;
        this.isActiveOffHock = false;
        this.friendWatchId = null;
        cancelLogRegister();
        unRegisterNetChange();
        clearLastLogMes();
        return true;
    }
}
