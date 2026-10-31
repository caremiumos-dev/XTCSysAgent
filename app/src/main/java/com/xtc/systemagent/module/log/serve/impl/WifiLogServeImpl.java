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
import com.xtc.qiniu.ICloudManager;
import com.xtc.qiniu.ICloudService;
import com.xtc.qiniu.TokenManager;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.R;
import com.xtc.systemagent.module.log.bean.WifiLogStatus;
import com.xtc.systemagent.module.log.serve.IWifiLogServe;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.util.LogCollectUtil;
import com.xtc.systemagent.util.NetworkUtil;
import com.xtc.systemagent.util.SdCardUtil;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.systemagent.util.UploadFile;
import com.xtc.utils.common.CollectionUtils;
import com.xtc.utils.date.DateUtils;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.encode.ZipUtils;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WiFi 日志的采集与上传。
 *
 * <p>WiFi 日志由底层（bsp）自己抓，本类只负责开关和时效：</p>
 * <ol>
 *   <li>发 {@code com.android.server.xtclog.WLAN_LOG_TRIGGERED} 广播并带 {@code start=true} 让底层开始抓；</li>
 *   <li>同时挂一个 30 分钟的闹钟，到点发 {@code com.xtc.xtclog.WIFI_LOG_CLOSE_ALARM} 自己收尾；</li>
 *   <li>收尾后打包上传七牛并回报服务端。</li>
 * </ol>
 *
 * <p>注意 {@code uploadWifiLogFile} 里"没有文件"的分支<strong>没有 return</strong>，
 * 会继续往下走一遍打包流程 —— 原厂如此，保留。</p>
 */
public class WifiLogServeImpl extends BasePresenter<String> implements IWifiLogServe {

    private final String TAG = "Log_WifiLogServeImpl";

    /** 通知底层开始/停止抓 WiFi 日志。 */
    private final String ACTION_TRIGGER_WIFI_LOG = "com.android.server.xtclog.WLAN_LOG_TRIGGERED";

    /** 30 分钟后的收尾闹钟。 */
    private final String ACTION_WIFI_LOG_COLSE_ALARM = "com.xtc.xtclog.WIFI_LOG_CLOSE_ALARM";

    /** 独立抓取的时长：30 分钟。 */
    private static final long INDEPENDENT_FETCHING_WIFI_LOG_DURATION = 1800000L;

    /** {@code AlarmManager.RTC_WAKEUP}。 */
    private static final int RTC_WAKEUP = 2;

    /** WakeLock 类型：{@code PARTIAL_WAKE_LOCK}。 */
    private static final int WAKE_LOCK_PARTIAL = 1;

    /** WakeLock 兜底持有时间：3 秒。 */
    private static final long WAKE_LOCK_TIMEOUT = 3000L;

    /** 重试上限。 */
    private static final int MAX_RETRY_UPLOAD = 5;

    /** 采集状态：成功。 */
    private static final int STATE_SUCCESS = 1;
    /** 采集状态：失败。 */
    private static final int STATE_FAIL = 2;

    /** 只在 WiFi 下上传。 */
    private static final int UPLOAD_NET_WIFI_ONLY = 2;

    private static SoftReference<WifiLogServeImpl> wifiLogServeImplRef;

    private AlarmManager am;
    private PendingIntent closePI;
    private BroadcastReceiver closeReceiver;
    private PowerManager.WakeLock wakeLock;

    private WifiLogServeImpl(Context context) {
        super(context);
        this.closeReceiver = null;
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            LogUtil.e(TAG, "error , PowerManager is null !");
        } else {
            this.wakeLock = powerManager.newWakeLock(WAKE_LOCK_PARTIAL, TAG);
            this.wakeLock.setReferenceCounted(false);
        }
    }

    public static synchronized WifiLogServeImpl getInstance(Context context) {
        if (wifiLogServeImplRef == null) {
            wifiLogServeImplRef = new SoftReference<>(new WifiLogServeImpl(context));
        }
        WifiLogServeImpl instance = wifiLogServeImplRef.get();
        if (instance == null) {
            instance = new WifiLogServeImpl(context);
            wifiLogServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    @Override
    public void startWifiLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "startWifiLog");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
            return;
        }
        saveCurrentStrategy(strategies.get(0));
        sendBroadcast2Bsp(true);
        registerCloseReceiver();
        startAlarm();
    }

    @Override
    public void endWifiLog() {
        sendBroadcast2Bsp(false);
        cancelAlarm();
        preUploadWifiLogFile();
    }

    /** 通知底层开始/停止。 */
    private void sendBroadcast2Bsp(boolean start) {
        Intent intent = new Intent(ACTION_TRIGGER_WIFI_LOG);
        intent.putExtra("start", start);
        this.mContext.sendBroadcast(intent);
    }

    private void registerCloseReceiver() {
        LogUtil.i(TAG, "registerCloseReceiver -->");
        this.closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "closeReceiver , action = " + action);
                if (ACTION_WIFI_LOG_COLSE_ALARM.equals(action)) {
                    endWifiLog();
                }
            }
        };
        this.mContext.registerReceiver(this.closeReceiver, new IntentFilter(ACTION_WIFI_LOG_COLSE_ALARM));
    }

    /**
     * 挂 30 分钟后的收尾闹钟。
     *
     * <p>原厂把 {@code NotificationFlag.NOTIFICATION_FLAG_HEADER} 当 PendingIntent flags 用，
     * 数值上等于 {@code FLAG_UPDATE_CURRENT}，保留原常量。</p>
     */
    private void startAlarm() {
        this.closePI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(ACTION_WIFI_LOG_COLSE_ALARM), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        long triggerTime = SystemClock.elapsedRealtime() + INDEPENDENT_FETCHING_WIFI_LOG_DURATION;
        this.am.setExact(RTC_WAKEUP, triggerTime, this.closePI);
        LogUtil.d(TAG, "startAlarm--> triggerTime: " + triggerTime);
    }

    private void cancelAlarm() {
        if (this.closeReceiver != null) {
            this.mContext.unregisterReceiver(this.closeReceiver);
        }
        if (this.am != null && this.closePI != null) {
            this.am.cancel(this.closePI);
            this.closePI.cancel();
        }
        this.closeReceiver = null;
    }

    /** 上传前的网络检查：策略要求"仅 WiFi"时不能走移动网络。 */
    private void preUploadWifiLogFile() {
        LogStrategy strategy = (LogStrategy) JSONUtil.fromJSON(
                SharedTool.getWifiLogStrategy(this.mContext), LogStrategy.class);
        if (strategy == null) {
            LogUtil.d(TAG, "receiveModemDumpCollected but last strategy is null.");
            return;
        }
        LogUtil.d(TAG, "preUploadWifiLogFile logStrategy: " + strategy);
        if (strategy.getUploadNet() == UPLOAD_NET_WIFI_ONLY) {
            if (NetworkUtil.isWifiConnected(this.mContext)) {
                LogUtil.i(TAG, "wifi only net is ok");
                uploadWifiLogFile(strategy);
            } else {
                LogUtil.e(TAG, "preUploadModemDumpFile Network is not Wifi");
                LogStateServeImpl.getInstance(this.mContext).notifyErrorInfo(
                        strategy, this.mContext.getString(R.string.collect_error_info_network_not_match));
            }
            return;
        }
        if (NetworkUtil.isConnected(this.mContext)) {
            LogUtil.i(TAG, "net is ok");
            uploadWifiLogFile(strategy);
        } else {
            LogUtil.e(TAG, "preUploadModemDumpFile Network is not connected");
            LogStateServeImpl.getInstance(this.mContext).notifyErrorInfo(
                    strategy, this.mContext.getString(R.string.collect_error_info_network_not_match));
        }
    }

    /**
     * 打包并上传。
     *
     * <p>"没有文件"的分支原厂没写 return，后面还会继续走一遍打包 ——
     * 打包返回 null 后再报一次失败。行为保留。</p>
     */
    private void uploadWifiLogFile(@NonNull LogStrategy strategy) {
        if (!hasWifiLogFiles()) {
            LogUtil.d(TAG, "uploadWifiLogFile onNoLogFile");
            dealUploadNoFileFailure(strategy);
        }
        clearWifiLogZipFile();
        acquireCpuForLong();
        File zipFile = getWifiLogZipFile(strategy);
        if (zipFile != null) {
            startQiniuUpload(strategy, zipFile);
            return;
        }
        LogUtil.d(TAG, "wifi log zip file onNoLogFile");
        dealUploadNoFileFailure(strategy);
        releaseWakeUp();
    }

    /** 把 WifiLog 目录下所有文件压成一个 zip。 */
    private File getWifiLogZipFile(@NonNull LogStrategy strategy) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            bindNumber = "NoBindNumber";
        }
        List<File> files = new ArrayList<>();
        File[] wifiLogFiles = new File(SdCardUtil.getWifiLogFilePath()).listFiles();
        if (wifiLogFiles == null || wifiLogFiles.length == 0) {
            LogUtil.d(TAG, "getWifiLogZipFile wifiLogFiles is null ");
            return null;
        }
        Collections.addAll(files, wifiLogFiles);
        String zipFileName = "WIFI_LOG_" + bindNumber + "_" + strategy.getLogId()
                + "_" + DateUtils.format("yyyyMMddHHmmss") + ".zip";
        LogUtil.d(TAG, "getWifiLogZipFile uploadZipFile zipFileName = " + zipFileName);
        String zipDirPath = SdCardUtil.getWifiLogZipFilePath();
        File zipDir = new File(zipDirPath);
        if (!zipDir.exists()) {
            zipDir.mkdirs();
        }
        String zipFilePath = zipDirPath + zipFileName;
        try {
            if (ZipUtils.zipFiles(files, zipFilePath)) {
                return new File(zipFilePath);
            }
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    private boolean hasWifiLogFiles() {
        return new File(SdCardUtil.getWifiLogFilePath()).listFiles() != null;
    }

    /** 先取七牛 token，再上传。 */
    private void startQiniuUpload(@NonNull final LogStrategy strategy, final File file) {
        TokenManager.getUploadToken(this.mContext, ICloudService.Type_QiNiu,
                new TokenManager.OnUpLoadTokenListener() {
                    @Override
                    public void onFailure(String error) {
                        LogUtil.e(TAG, "uploadWifiLogFile getUploadToken failure: " + error);
                        dealUploadFailure(strategy);
                        releaseWakeUp();
                    }

                    @Override
                    public void onSuccess(String token) {
                        final String fileName = file.getName();
                        LogUtil.i(TAG, "startQiniuUpload token value = " + token
                                + " , fileName= " + fileName);
                        ICloudService.upLoadFile(WifiLogServeImpl.this.mContext,
                                ICloudService.Type_QiNiu, fileName, file,
                                new ICloudManager.OnUpLoadListener() {
                                    @Override
                                    public void onFailure(String key, int statusCode, String error) {
                                        LogUtil.e(TAG, "uploadWifiLogFile onUploadFileFailure to qiniu key: "
                                                + key + " , statusCode: " + statusCode + " ,error: " + error);
                                        retryUploadWifiLog(strategy);
                                        releaseWakeUp();
                                    }

                                    @Override
                                    public void onProgress(String key, double percent) {
                                        LogUtil.d(TAG, "startQiniuUpload upload progress:" + percent);
                                    }

                                    @Override
                                    public void onSuccess(String key) {
                                        dealUploadSuccess(strategy);
                                        notifyServerSuccess(strategy, key, 1);
                                        releaseWakeUp();
                                    }
                                });
                    }
                });
    }

    /** 上传失败后重试；达到上限就判定整体失败。 */
    private void retryUploadWifiLog(@NonNull LogStrategy strategy) {
        WifiLogStatus status = (WifiLogStatus) JSONUtil.fromJSON(
                SharedTool.getWifiLogStatus(this.mContext), WifiLogStatus.class);
        LogUtil.e(TAG, "retryUploadWifiLog wifiLogStatus: " + status);
        if (status.getCurrentRetryUploadNum() >= MAX_RETRY_UPLOAD) {
            LogUtil.e(TAG, "retryUploadModemDumpLog retry end...");
            dealUploadFailure(strategy);
            return;
        }
        preUploadWifiLogFile();
        status.setCurrentRetryUploadNum(status.getCurrentRetryUploadNum() + 1);
        saveCurrentStatus(status);
    }

    private void dealUploadFailure(@NonNull LogStrategy strategy) {
        clearLastWifiLogInfo();
        clearWifiLogZipFile();
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_FAIL);
    }

    private void dealUploadSuccess(@NonNull LogStrategy strategy) {
        clearLastWifiLogInfo();
        clearWifiLogFile();
        clearWifiLogZipFile();
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_SUCCESS);
    }

    private void dealUploadNoFileFailure(@NonNull LogStrategy strategy) {
        clearLastWifiLogInfo();
        clearWifiLogZipFile();
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_FAIL);
    }

    /** 上报服务端"上传成功"。 */
    private void notifyServerSuccess(@NonNull LogStrategy strategy, final String key, int logType) {
        LogStateServeImpl.getInstance(this.mContext).notifyUploadSuccess(
                key, strategy, logType, 1, new LogStateServeImpl.UploadSuccessCallback() {
                    @Override
                    public void onFailure() {
                        LogUtil.d(TAG, "uploadWifiLogFile to server fail! " + key);
                        releaseWakeUp();
                    }

                    @Override
                    public void onSuccess() {
                        LogUtil.d(TAG, "uploadWifiLogFile to server success. " + key);
                        releaseWakeUp();
                    }
                });
    }

    private void clearLastWifiLogInfo() {
        LogUtil.i(TAG, "clearLastWifiLogInfo -->");
        SharedTool.saveWifiLogStrategy(this.mContext, "");
        SharedTool.saveWifiLogStatus(this.mContext, "");
    }

    private void clearWifiLogFile() {
        UploadFile.deleteFile(SdCardUtil.getWifiLogFilePath(), getClass().getSimpleName());
    }

    private void clearWifiLogZipFile() {
        UploadFile.deleteFile(SdCardUtil.getWifiLogZipFilePath(), getClass().getSimpleName());
    }

    private void saveCurrentStatus(WifiLogStatus status) {
        SharedTool.saveWifiLogStatus(this.mContext, JSONUtil.toJSON(status));
    }

    private void saveCurrentStrategy(LogStrategy strategy) {
        SharedTool.saveWifiLogStrategy(this.mContext, JSONUtil.toJSON(strategy));
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
        this.wakeLock.acquire(WAKE_LOCK_TIMEOUT);
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
