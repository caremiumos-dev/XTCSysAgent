package com.xtc.bigdata.collector.utils;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.xtc.bigdata.common.utils.ContextUtils;

/**
 * 网络类型的中文/英文短名。
 *
 * <p>与 {@code NetUtils} 的区别：那边返回编号，这边直接返回上报用的字符串
 * （{@code "WIFI"/"2G"/"3G"/"4G"/"无网络"}）。事件 extend 里的
 * {@code NetWorkType} 就取这里的值。</p>
 *
 * <p>注意 {@link #getNetworkType()} 的 default 分支返回的是子类型名本身
 * （例如 {@code "LTE"}），而不是"未知" —— 服务端按这个分支做兜底统计。</p>
 */
public class NetworkUtil {

    /** 全局唯一的 ConnectivityManager。 */
    private static ConnectivityManager connectivityManager;

    /** 单例。 */
    private static NetworkUtil instance;

    /** 构造时取一次系统服务（getInstance 里没有加锁，这是原厂行为）。 */
    private NetworkUtil() {
        if (ContextUtils.isEmpty()) {
            connectivityManager = null;
        }
        connectivityManager = (ConnectivityManager) ContextUtils.getContext().getSystemService("connectivity");
    }

    /**
     * 取单例。
     *
     * @return 单例
     */
    public static NetworkUtil getInstance() {
        if (instance == null) {
            instance = new NetworkUtil();
        }
        return instance;
    }

    /**
     * 取当前网络类型名。
     *
     * @return {@code "WIFI"/"2G"/"3G"/"4G"/"无网络"} 或子类型名
     */
    public String getNetworkType() {
        NetworkInfo activeNetworkInfo;
        if (connectivityManager == null
                || (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) == null
                || !activeNetworkInfo.isConnected()) {
            return "无网络";
        }
        if (activeNetworkInfo.getType() == 1) {
            return "WIFI";
        }
        if (activeNetworkInfo.getType() != 0) {
            return "无网络";
        }
        String subtypeName = activeNetworkInfo.getSubtypeName();
        switch (activeNetworkInfo.getSubtype()) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
                return "2G";
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
                return "3G";
            case 13:
                return "4G";
            default:
                return ("TD-SCDMA".equalsIgnoreCase(subtypeName) || "WCDMA".equalsIgnoreCase(subtypeName)
                        || "CDMA2000".equalsIgnoreCase(subtypeName)) ? "3G" : subtypeName;
        }
    }
}
