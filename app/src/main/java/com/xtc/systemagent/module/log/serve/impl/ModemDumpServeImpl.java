package com.xtc.systemagent.module.log.serve.impl;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.xtc.architecture.mvp.BasePresenter;
import com.xtc.log.LogUtil;
import com.xtc.qiniu.ICloudManager;
import com.xtc.qiniu.ICloudService;
import com.xtc.qiniu.TokenManager;
import com.xtc.systemagent.R;
import com.xtc.systemagent.module.log.bean.ModemDumpStatus;
import com.xtc.systemagent.module.log.serve.IModemDumpServe;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.util.LogCollectUtil;
import com.xtc.systemagent.util.NetworkUtil;
import com.xtc.systemagent.util.SdCardUtil;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.systemagent.util.UploadFile;
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
 * MODEM dump 的采集与上传。
 *
 * <p>流程：收到策略 → 打开自检开关让系统开始抓 dump → 等系统发
 * {@code com.xtc.modem.dump.end} 广播 → 把 dump 目录打包成 zip →
 * 传到七牛 → 回报服务端。</p>
 *
 * <p>上传失败会重试，最多 {@link #MAX_RETRY_UPLOAD} 次；次数存在
 * SharedPreferences 里，跨进程重启也能接着数。</p>
 */
public class ModemDumpServeImpl extends BasePresenter<String> implements IModemDumpServe {

    private final String TAG = "Log_ModemDumpServeImpl";

    /** 抓 dump 总开关：0 关，2 开。 */
    public static final String PROP_FETCHING_DUMP_CONTROL = "persist.vendor.ssr.enable_ramdumps";

    /** 4100 平台的抓取控制属性（总开关为 0 时才看这个）。 */
    public static final String PROP_FETCHING_DUMP_CONTROL_4100 = "persist.vendor.ssr.enable_ramdumps_4100";

    /** 本模块单独的抓取开关。 */
    private static final String PROP_SUB_FETCHING_MODEM_DUMP = "log.tag.modem.dump";

    /** 自检触发。 */
    public static final int SELF_TEST_FETCH_MODEM_DUMP = 1;

    /** 服务端触发。 */
    private static final int SERVER_FETCH_MODEM_DUMP = 2;

    /** 关闭。 */
    private static final int CLOSE_MODEM_DUMP = 0;

    /** 打开。 */
    private static final int OPEN_MODEM_DUMP = 1;

    /** 重试上限。 */
    private static final int MAX_RETRY_UPLOAD = 5;

    /** WakeLock 类型：{@code PARTIAL_WAKE_LOCK}。 */
    private static final int WAKE_LOCK_PARTIAL = 1;

    /** WakeLock 持有时间：3 秒。 */
    private static final long WAKE_LOCK_TIMEOUT = 3000L;

    /** 采集状态：成功。 */
    private static final int STATE_SUCCESS = 1;
    /** 采集状态：失败。 */
    private static final int STATE_FAIL = 2;
    /** 采集状态：采集中。 */
    private static final int STATE_COLLECTING = 3;

    /** 只在 WiFi 下上传。 */
    private static final int UPLOAD_NET_WIFI_ONLY = 2;

    private static SoftReference<ModemDumpServeImpl> modemDumpServeImplRef;

    /** 锁对象，保证同一时刻只有一次打包/上传。 */
    private final Object SYNC_OBJ = new Object();

    private PowerManager.WakeLock wakeLock;

    private ModemDumpServeImpl(Context context) {
        super(context);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            LogUtil.e(TAG, "error , PowerManager is null !");
        } else {
            this.wakeLock = powerManager.newWakeLock(WAKE_LOCK_PARTIAL, TAG);
            this.wakeLock.setReferenceCounted(false);
        }
    }

    public static synchronized ModemDumpServeImpl getInstance(Context context) {
        if (modemDumpServeImplRef == null) {
            modemDumpServeImplRef = new SoftReference<>(new ModemDumpServeImpl(context));
        }
        ModemDumpServeImpl instance = modemDumpServeImplRef.get();
        if (instance == null) {
            instance = new ModemDumpServeImpl(context);
            modemDumpServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    @Override
    public void startModemDump(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "startModemDump");
        if (com.xtc.utils.common.CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
            return;
        }
        LogStrategy strategy = strategies.get(0);
        saveCurrentStrategy(strategy);
        if (!hasModemDumpFiles()) {
            LogUtil.d(TAG, "startModemDump logStrategy = " + strategy);
            switchFetchingModemDump(true);
            LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_COLLECTING);
        } else {
            synchronized (this.SYNC_OBJ) {
                LogUtil.d(TAG, "startModemDump hasModemDumpFiles logStrategy= " + strategy);
                preUploadModemDumpFile(strategy);
            }
        }
    }

    @Override
    public void onDestroy() {
        releaseWakeUp();
    }

    /** 收到系统"dump 采集完成"广播后开始上传。 */
    public void receiveModemDumpCollected() {
        LogStrategy strategy = (LogStrategy) JSONUtil.fromJSON(
                SharedTool.getModemDumpStrategy(this.mContext), LogStrategy.class);
        if (strategy == null) {
            LogUtil.d(TAG, "receiveModemDumpCollected but last strategy is null.");
            return;
        }
        synchronized (this.SYNC_OBJ) {
            preUploadModemDumpFile(strategy);
        }
    }

    /** 上传前的网络检查：策略要求"仅 WiFi"时不能走移动网络。 */
    private void preUploadModemDumpFile(@NonNull LogStrategy strategy) {
        LogUtil.d(TAG, "preUploadModemDumpFile logStrategy: " + strategy);
        if (strategy.getUploadNet() == UPLOAD_NET_WIFI_ONLY) {
            if (NetworkUtil.isWifiConnected(this.mContext)) {
                LogUtil.i(TAG, "wifi only net is ok");
                uploadModemDumpFile(strategy);
            } else {
                LogUtil.e(TAG, "preUploadModemDumpFile Network is not Wifi");
                LogStateServeImpl.getInstance(this.mContext).notifyErrorInfo(
                        strategy, this.mContext.getString(R.string.collect_error_info_network_not_match));
            }
            return;
        }
        if (NetworkUtil.isConnected(this.mContext)) {
            LogUtil.i(TAG, "net is ok");
            uploadModemDumpFile(strategy);
        } else {
            LogUtil.e(TAG, "preUploadModemDumpFile Network is not connected");
            LogStateServeImpl.getInstance(this.mContext).notifyErrorInfo(
                    strategy, this.mContext.getString(R.string.collect_error_info_network_not_match));
        }
    }

    /** 打包并上传。 */
    private void uploadModemDumpFile(@NonNull LogStrategy strategy) {
        if (!hasModemDumpFiles()) {
            LogUtil.d(TAG, "uploadModemDumpFile onNoLogFile");
            dealUploadNoFileFailure(strategy);
            return;
        }
        clearModemDumpZipFile();
        acquireCpuForLong();
        File zipFile = getModemDumpZipFile(strategy);
        if (zipFile != null) {
            startQiniuUpload(strategy, zipFile);
            return;
        }
        LogUtil.d(TAG, "modem dump zip file onNoLogFile");
        dealUploadNoFileFailure(strategy);
        releaseWakeUp();
    }

    /** 把 dump 目录下所有文件压成一个以 bindNumber + logId + 时间戳命名的 zip。 */
    private File getModemDumpZipFile(@NonNull LogStrategy strategy) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            bindNumber = "NoBindNumber";
        }
        List<File> files = new ArrayList<>();
        File[] dumpFiles = new File(SdCardUtil.getModemDumpFilePath()).listFiles();
        if (dumpFiles == null || dumpFiles.length == 0) {
            LogUtil.d(TAG, "getModemDumpZipFile modemDumpFiles is null ");
            return null;
        }
        Collections.addAll(files, dumpFiles);
        String zipFileName = "MODEM_DUMP_" + bindNumber + "_" + strategy.getLogId()
                + "_" + DateUtils.format("yyyyMMddHHmmss") + ".zip";
        LogUtil.d(TAG, "getModemDumpZipFile uploadZipFile zipFileName = " + zipFileName);
        String zipDirPath = SdCardUtil.getModemDumpZipFilePath();
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

    private boolean hasModemDumpFiles() {
        return new File(SdCardUtil.getModemDumpFilePath()).listFiles() != null;
    }

    /** 先取七牛 token，再上传。 */
    private void startQiniuUpload(@NonNull final LogStrategy strategy, final File file) {
        TokenManager.getUploadToken(this.mContext, ICloudService.Type_QiNiu,
                new TokenManager.OnUpLoadTokenListener() {
                    @Override
                    public void onFailure(String error) {
                        LogUtil.e(TAG, "uploadModemDumpFile getUploadToken failure: " + error);
                        dealUploadFailure(strategy);
                        releaseWakeUp();
                    }

                    @Override
                    public void onSuccess(String token) {
                        final String fileName = file.getName();
                        LogUtil.i(TAG, "startQiniuUpload token value = " + token
                                + " , fileName= " + fileName);
                        ICloudService.upLoadFile(ModemDumpServeImpl.this.mContext, ICloudService.Type_QiNiu,
                                fileName, file, new ICloudManager.OnUpLoadListener() {
                                    @Override
                                    public void onFailure(String key, int statusCode, String error) {
                                        LogUtil.e(TAG, "uploadModemDumpFile onUploadFileFailure to qiniu key: "
                                                + key + " , statusCode: " + statusCode + " ,error: " + error);
                                        retryUploadModemDumpLog(strategy);
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
    private void retryUploadModemDumpLog(@NonNull LogStrategy strategy) {
        ModemDumpStatus status = (ModemDumpStatus) JSONUtil.fromJSON(
                SharedTool.getModemDumpStatus(this.mContext), ModemDumpStatus.class);
        LogUtil.e(TAG, "retryUploadModemDumpLog modemDumpStatus: " + status);
        if (status.getCurrentRetryUploadNum() >= MAX_RETRY_UPLOAD) {
            LogUtil.e(TAG, "retryUploadModemDumpLog retry end...");
            dealUploadFailure(strategy);
            return;
        }
        preUploadModemDumpFile(strategy);
        status.setCurrentRetryUploadNum(status.getCurrentRetryUploadNum() + 1);
        saveCurrentStatus(status);
    }

    private void dealUploadFailure(@NonNull LogStrategy strategy) {
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_FAIL);
    }

    private void dealUploadSuccess(@NonNull LogStrategy strategy) {
        clearLastModemDumpInfo();
        clearModemDumpFile();
        clearModemDumpZipFile();
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_SUCCESS);
    }

    /** 有文件但打包失败：清掉残留并判定失败。 */
    private void dealUploadNoFileFailure(@NonNull LogStrategy strategy) {
        clearLastModemDumpInfo();
        clearModemDumpZipFile();
        LogCollectUtil.updateCollectState(this.mContext, strategy.getLogId().longValue(), STATE_FAIL);
    }

    /** 上报服务端"上传成功"。 */
    private void notifyServerSuccess(@NonNull LogStrategy strategy, final String key, int logType) {
        LogStateServeImpl.getInstance(this.mContext).notifyUploadSuccess(
                key, strategy, logType, 1, new LogStateServeImpl.UploadSuccessCallback() {
                    @Override
                    public void onFailure() {
                        LogUtil.d(TAG, "uploadModemDumpFile to server fail! " + key);
                        releaseWakeUp();
                    }

                    @Override
                    public void onSuccess() {
                        LogUtil.d(TAG, "uploadModemDumpFile to server success. " + key);
                        releaseWakeUp();
                    }
                });
    }

    private void clearLastModemDumpInfo() {
        LogUtil.i(TAG, "clearLastModemDumpInfo -->");
        SharedTool.saveModemDumpStrategy(this.mContext, "");
        SharedTool.saveModemDumpStatus(this.mContext, "");
    }

    private void clearModemDumpFile() {
        UploadFile.deleteFile(SdCardUtil.getModemDumpFilePath(), getClass().getSimpleName());
    }

    private void clearModemDumpZipFile() {
        UploadFile.deleteFile(SdCardUtil.getModemDumpZipFilePath(), getClass().getSimpleName());
    }

    private void saveCurrentStatus(ModemDumpStatus status) {
        SharedTool.saveModemDumpStatus(this.mContext, JSONUtil.toJSON(status));
    }

    private void saveCurrentStrategy(LogStrategy strategy) {
        SharedTool.saveModemDumpStrategy(this.mContext, JSONUtil.toJSON(strategy));
    }

    /** 开关抓取：总开关写 2 表示开，本模块开关写 1 表示开。 */
    private void switchFetchingModemDump(boolean open) {
        SystemPropertiesUtils.set(PROP_FETCHING_DUMP_CONTROL, open ? OPEN_MODEM_DUMP * 2 : CLOSE_MODEM_DUMP);
        SystemPropertiesUtils.set(PROP_SUB_FETCHING_MODEM_DUMP, open ? OPEN_MODEM_DUMP : CLOSE_MODEM_DUMP);
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
