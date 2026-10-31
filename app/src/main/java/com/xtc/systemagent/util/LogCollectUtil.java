package com.xtc.systemagent.util;

import android.content.Context;
import android.os.PowerManager;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.log.serve.impl.LogStateServeImpl;
import com.xtc.systemagent.net.bean.LogStateParams;
import java.io.File;

/**
 * 采集过程的公共辅助：强制 4G、回滚网络模式、上报采集状态、持有 WakeLock。
 *
 * <p>抓 Modem / tcpdump 时必须保证数据通道可用，所以采集前会先把网络模式
 * 抬到 4G（{@code NETWORK_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA} = 22），
 * 采集完再恢复用户原本的模式。</p>
 */
public class LogCollectUtil {

    private static final String TAG = "Log_LogCollectUtil";

    /** 4G 优先的网络模式。 */
    private static final int NETWORK_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA = 22;

    /** WakeLock 持有时间：10 分钟。 */
    private static final long WAKE_LOCK_TIME_OUT = 600000L;

    /**
     * WakeLock 类型：{@code PARTIAL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP}
     * （原厂写死为字面量 536870913 = 0x20000001，这里保留同样的位组合）。
     */
    private static final int WAKE_LOCK_FLAGS =
            PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;

    private static LogCollectUtil instance;

    private PowerManager.WakeLock wakeLock = null;

    private LogCollectUtil() {
    }

    public static LogCollectUtil getInstance() {
        if (instance == null) {
            synchronized (LogCollectUtil.class) {
                if (instance == null) {
                    instance = new LogCollectUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 删除文件。
     *
     * <p>先把文件 rename 成时间戳名字再删 —— 这样即使删除失败，原文件名也已经
     * 不在了，不会影响下一轮同名文件的生成。</p>
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        File renamed = new File(file.getParent() + File.separator + System.currentTimeMillis());
        file.renameTo(renamed);
        return renamed.delete();
    }

    /** 记录当前网络模式并切到 4G。 */
    public static boolean forceMobileNetworkTo4G(Context context) {
        int currentNetworkMode = NetworkUtil.getCurrentNetworkMode(context);
        LogUtil.d(TAG, "HXQ-mCurrentNetWorkMode:" + currentNetworkMode);
        SharedTool.saveCurrentNetworkType(context, currentNetworkMode);
        boolean success = set4NetworkMode(context);
        LogUtil.d(TAG, "HXQ-set 4g network result :" + success);
        return success;
    }

    /** 恢复采集前的网络模式。 */
    public static void resumeMobileNetwork(Context context) {
        int savedNetworkType = SharedTool.getCurrentNetworkType(context);
        LogUtil.d(TAG, "HXQ-mDefaultNetMobileWork:" + savedNetworkType);
        NetworkUtil.setPreferredNetworkType(context, savedNetworkType);
    }

    /** 切到 4G。 */
    public static boolean set4NetworkMode(Context context) {
        LogUtil.d(TAG, "HXQ-setNetworkMode，mode：" + NETWORK_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA);
        return NetworkUtil.setPreferredNetworkType(
                context, NETWORK_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA);
    }

    /** 上报采集状态。 */
    public static void updateCollectState(Context context, long id, int collectStatus) {
        LogStateParams params = new LogStateParams();
        params.setId(id);
        params.setCollectStatus(collectStatus);
        LogStateServeImpl.getInstance(context).collectStateUpdate(params);
    }

    /** 上报采集状态，并接收结果。 */
    public static void updateCollectState(Context context, long id, int collectStatus,
                                          LogStateServeImpl.UploadSuccessCallback callback) {
        LogStateParams params = new LogStateParams();
        params.setId(id);
        params.setCollectStatus(collectStatus);
        LogStateServeImpl.getInstance(context).collectStateUpdate(params, callback);
    }

    /**
     * 申请一个 10 分钟的 WakeLock。
     *
     * <p>只在第一次调用时创建，且关闭引用计数（{@code setReferenceCounted(false)}），
     * 所以重复申请不会累加。</p>
     */
    public void acquireWakeLockForTenMinutes(Context context) {
        if (this.wakeLock != null) {
            return;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            LogUtil.e(TAG, "PowerManager is null , return !");
            return;
        }
        this.wakeLock = powerManager.newWakeLock(WAKE_LOCK_FLAGS, TAG);
        this.wakeLock.setReferenceCounted(false);
        this.wakeLock.acquire(WAKE_LOCK_TIME_OUT);
    }

    /** 释放 WakeLock。 */
    public void releaseWakeLock() {
        if (this.wakeLock == null || !this.wakeLock.isHeld()) {
            return;
        }
        try {
            this.wakeLock.release();
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        this.wakeLock = null;
    }
}
