package com.xtc.bigdata.common.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.xtc.bigdata.common.constants.Constants;
import com.xtc.log.LogUtil;

/**
 * 网络状态与网络类型判断。
 *
 * <p>{@link #getNetworkType(NetworkInfo)} 的返回值是一套自定义编号
 * （1=wifi、2=2G、3=3G、4=4G、5=未知、6=蓝牙、-1=无网），
 * 上报事件里的 {@code NetWorkType} 就是靠它翻译成 {@code "WIFI"/"2G"/"无网络"}。</p>
 *
 * <p>两个 {@code ping} 方法标了 {@code @Deprecated}：它们会 fork 子进程，
 * 在手表上既慢又耗电，只在老代码里还有调用点。</p>
 */
public final class NetUtils {

    /** 网络类型：2G。 */
    public static final int NETWORK_2G = 2;

    /** 网络类型：3G。 */
    public static final int NETWORK_3G = 3;

    /** 网络类型：4G。 */
    public static final int NETWORK_4G = 4;

    /** 网络类型：蓝牙。 */
    public static final int NETWORK_BLUETOOTH = 6;

    /** 网络类型：无网络。 */
    public static final int NETWORK_NO = -1;

    /** 子类型 GSM（API 25 才有常量，这里写字面量避免引用无关类）。 */
    private static final int NETWORK_TYPE_GSM = 16;

    /** 子类型 IWLAN。 */
    private static final int NETWORK_TYPE_IWLAN = 18;

    /** 子类型 TD-SCDMA。 */
    private static final int NETWORK_TYPE_TD_SCDMA = 17;

    /** 网络类型：未知。 */
    public static final int NETWORK_UNKNOWN = 5;

    /** 网络类型：wifi。 */
    public static final int NETWORK_WIFI = 1;

    private static final String TAG = "NetUtils";

    private NetUtils() {
        throw new UnsupportedOperationException("Are u ok ?");
    }

    /**
     * 开关 wifi。
     *
     * @param context 上下文
     * @param enable  是否开启
     */
    public static void enableWifi(Context context, boolean enable) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService("wifi");
        if (wifiManager == null) {
            return;
        }
        wifiManager.setWifiEnabled(enable);
    }

    /**
     * 取当前活动网络的 NetworkInfo。
     *
     * @param context 上下文
     * @return NetworkInfo；不可用时返回 null
     */
    private static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService("connectivity");
        if (connectivityManager == null) {
            return null;
        }
        return connectivityManager.getActiveNetworkInfo();
    }

    /**
     * 取当前网络类型编号。
     *
     * @param context 上下文
     * @return 类型编号
     */
    public static int getNetWorkType(Context context) {
        return getNetworkType(getActiveNetworkInfo(context));
    }

    /**
     * 把 NetworkInfo 翻译成自定义网络类型编号。
     *
     * <p>子类型 1/2/4/7/11/16 归 2G，3/5/6/8/9/10/12/14/15/17 归 3G，
     * 13/18 归 4G；其它再看子类型名里有没有 TD-SCDMA/WCDMA/CDMA2000。</p>
     *
     * @param networkInfo 网络信息，可为 null
     * @return 类型编号；无网返回 -1
     */
    public static int getNetworkType(NetworkInfo networkInfo) {
        if (networkInfo == null || !networkInfo.isAvailable()) {
            return NETWORK_NO;
        }
        if (networkInfo.getType() == 1) {
            return NETWORK_WIFI;
        }
        if (networkInfo.getType() != 0) {
            return networkInfo.getType() == 7 ? NETWORK_BLUETOOTH : NETWORK_UNKNOWN;
        }
        switch (networkInfo.getSubtype()) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
            case NETWORK_TYPE_GSM:
                return NETWORK_2G;
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
            case NETWORK_TYPE_TD_SCDMA:
                return NETWORK_3G;
            case 13:
            case NETWORK_TYPE_IWLAN:
                return NETWORK_4G;
            default:
                String subtypeName = networkInfo.getSubtypeName();
                return ("TD-SCDMA".equalsIgnoreCase(subtypeName) || "WCDMA".equalsIgnoreCase(subtypeName)
                        || "CDMA2000".equalsIgnoreCase(subtypeName)) ? NETWORK_3G : NETWORK_UNKNOWN;
        }
    }

    /**
     * 是否已联网。
     *
     * @param context 上下文
     * @return 是否已连接
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * 移动数据是否已连接。
     *
     * @param context 上下文
     * @return 是否已连接
     */
    public static boolean isMobileDataConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo.State state = connectivityManager.getNetworkInfo(0).getState();
        return state != null && NetworkInfo.State.CONNECTED == state;
    }

    /**
     * wifi 是否已连接。
     *
     * @param context 上下文
     * @return 是否已连接
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo.State state = connectivityManager.getNetworkInfo(1).getState();
        return state != null && NetworkInfo.State.CONNECTED == state;
    }

    /**
     * wifi 开关是否打开（不代表已连上）。
     *
     * @param context 上下文
     * @return 是否打开
     */
    public static boolean isWifiEnable(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService("wifi");
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    /**
     * ping 一次。
     *
     * @param host 目标地址
     * @return 是否连通
     * @deprecated 会 fork 子进程，不要在新代码里用
     */
    @Deprecated
    public static boolean ping(String host) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        try {
            return Runtime.getRuntime().exec(new StringBuilder().append("/system/bin/ping -c1 ").append(host).toString()).waitFor() == 0;
        } catch (Exception e) {
            if (!Constants.isDebug) {
                return false;
            }
            LogUtil.w(TAG, e + ":ping 网络出错");
            return false;
        }
    }

    /**
     * ping 指定次数与超时。
     *
     * @param host    目标地址
     * @param count   次数
     * @param timeout 超时秒数
     * @return 是否连通
     * @deprecated 同 {@link #ping(String)}
     */
    @Deprecated
    public static boolean ping(String host, int count, int timeout) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        try {
            // 最后的空格是原厂拼命令时用 MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR 加的
            return Runtime.getRuntime().exec(new StringBuilder().append("/system/bin/ping -c").append(count).append(" -w").append(timeout).append(" ").append(host).toString()).waitFor() == 0;
        } catch (Exception e) {
            if (!Constants.isDebug) {
                return false;
            }
            LogUtil.w(TAG, e + ":ping 网络出错");
            return false;
        }
    }
}
