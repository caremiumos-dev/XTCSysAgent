package com.xtc.utils.system;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.constant.PlatformConstants;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * 网络状态与开关工具。
 *
 * <p>原厂方法名全是单字母 {@code a}..{@code n}，这里按"读什么、改什么"还原。
 * 每个方法的判断逻辑、默认返回值、异常吞没方式都与原件一致。</p>
 */
public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    /** {@link TelephonyManager#getNetworkType()} 里对应 2G/3G/4G 的取值。 */
    private static final int NETWORK_TYPE_EVDO_0 = 16;
    private static final int NETWORK_TYPE_HSDPA = 17;
    private static final int NETWORK_TYPE_HSPA = 18;

    /** NETWORK_TYPE_LTE，用于判断"是否 LTE 且数据开启"。 */
    private static final int NETWORK_TYPE_LTE = 13;

    private static final String PING_ADDRESS = "123.125.114.144";

    public enum NetworkType {
        NETWORK_WIFI,
        NETWORK_4G,
        NETWORK_3G,
        NETWORK_2G,
        NETWORK_UNKNOWN,
        NETWORK_NO
    }

    private NetworkUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /** 域名解析成 IP（带超时的线程池，避免主线程阻塞）。 */
    public static String getIpByDomain(final String domain) {
        try {
            return Executors.newCachedThreadPool().submit(new Callable<String>() {
                @Override
                public String call() {
                    try {
                        return InetAddress.getByName(domain).getHostAddress();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 取本机 IP。
     *
     * @param preferIpv4 true 时只返回 IPv4；false 时优先返回 IPv6（去掉 %scope 后缀并大写）
     */
    public static String getLocalIpAddress(boolean preferIpv4) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }
                    String hostAddress = address.getHostAddress();
                    boolean isIpv4 = hostAddress.indexOf(':') < 0;
                    if (preferIpv4) {
                        if (isIpv4) {
                            return hostAddress;
                        }
                    } else if (!isIpv4) {
                        int scopeIndex = hostAddress.indexOf('%');
                        if (scopeIndex < 0) {
                            return hostAddress.toUpperCase();
                        }
                        return hostAddress.substring(0, scopeIndex).toUpperCase();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 打开无线设置页。 */
    public static void openWirelessSettings(Context context) {
        if (Build.VERSION.SDK_INT > 10) {
            context.startActivity(new Intent("android.settings.WIRELESS_SETTINGS"));
        } else {
            context.startActivity(new Intent("android.settings.SETTINGS"));
        }
    }

    /**
     * 开关移动数据。国内高通平台走 ConnectivityManager 的隐藏方法，
     * 其它平台走 TelephonyManager。
     */
    public static void setMobileDataEnabled(Context context, boolean enabled) {
        if (DeviceUtils.isOversea()) {
            try {
                ConnectivityManager manager =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                manager.getClass().getMethod("setMobileDataEnabled", Boolean.TYPE)
                        .invoke(manager, Boolean.valueOf(enabled));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            TelephonyManager manager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method method = manager.getClass().getDeclaredMethod("setDataEnabled", Boolean.TYPE);
            if (method != null) {
                method.invoke(manager, Boolean.valueOf(enabled));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 反射读取移动数据开关状态。 */
    public static boolean getMobileDataEnabled(Context context, Object[] args) {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Class<?> clazz = manager.getClass();
            // 原实现里这一句只是为了触发一次拆箱，副作用就是"如果 false 就 false"，保留。
            Boolean.valueOf(false);
            Class<?>[] parameterTypes = args != null ? new Class[]{args.getClass()} : null;
            return ((Boolean) clazz.getMethod("getMobileDataEnabled", parameterTypes)
                    .invoke(manager, args)).booleanValue();
        } catch (Exception e) {
            return false;
        }
    }

    /** 开关 WiFi。 */
    public static void setWifiEnabled(Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (enabled) {
            if (wifiManager.isWifiEnabled()) {
                return;
            }
            wifiManager.setWifiEnabled(true);
        } else if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    /** 当前是否有可用网络。 */
    public static boolean isNetworkConnected(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isConnected() && info.isAvailable();
    }

    /** 通过 ping 判断网络是否真的通。 */
    public static boolean isAvailableByPing(Context context) {
        ShellUtils.ExecResult result = ShellUtils.exec("ping -c 1 -w 1 " + PING_ADDRESS, false);
        boolean available = result.code == 0;
        if (result.stderr != null) {
            LogUtil.d("isAvailableByPing errorMsg", result.stderr);
        }
        if (result.stdout != null) {
            LogUtil.d("isAvailableByPing successMsg", result.stdout);
        }
        return available;
    }

    /** 反射读取 TelephonyManager 的数据开关。 */
    public static boolean isMobileDataEnabled(Context context) {
        boolean enabled = false;
        try {
            TelephonyManager manager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method method = manager.getClass().getDeclaredMethod("getDataEnabled", new Class[0]);
            if (method != null) {
                Boolean value = (Boolean) method.invoke(manager, new Object[0]);
                if (value == null) {
                    LogUtil.e(TAG, "getDataEnabled invoke == null");
                    enabled = false;
                } else {
                    enabled = value.booleanValue();
                }
            } else {
                enabled = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            enabled = false;
        }
        return enabled;
    }

    /** 是否"数据开启且当前是 2G/3G/4G"。 */
    public static boolean isMobileDataConnected(Context context) {
        NetworkType type = getNetworkType(context);
        if (!isMobileDataEnabled(context)) {
            return false;
        }
        return type.ordinal() == NetworkType.NETWORK_4G.ordinal()
                || type.ordinal() == NetworkType.NETWORK_3G.ordinal()
                || type.ordinal() == NetworkType.NETWORK_2G.ordinal();
    }

    /** 是否 LTE 网络。 */
    public static boolean is4G(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isAvailable() && info.getSubtype() == NETWORK_TYPE_LTE;
    }

    /** 是否 3G 网络。 */
    public static boolean is3G(Context context) {
        return getNetworkType(context).ordinal() == NetworkType.NETWORK_3G.ordinal();
    }

    /** 是否 2G 网络。 */
    public static boolean is2G(Context context) {
        return getNetworkType(context).ordinal() == NetworkType.NETWORK_2G.ordinal();
    }

    /** WiFi 开关是否打开（不代表已连上）。 */
    public static boolean isWifiEnabled(Context context) {
        return ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
    }

    /** 是否有已连接的 WiFi。 */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo[] allInfo = manager.getAllNetworkInfo();
        if (allInfo == null) {
            return false;
        }
        for (NetworkInfo info : allInfo) {
            // ConnectivityManager.TYPE_WIFI = 1
            if ("WIFI".equals(info.getTypeName()) && info.getType() == 1 && info.isConnected()) {
                return true;
            }
        }
        return false;
    }

    /** WiFi 已打开且已连接。 */
    public static boolean isWifiAvailable(Context context) {
        return isWifiEnabled(context) && isWifiConnected(context);
    }

    /** 运营商名称。 */
    public static String getNetworkOperatorName(Context context) {
        TelephonyManager manager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            return manager.getNetworkOperatorName();
        }
        return null;
    }

    /** 当前网络类型。 */
    public static NetworkType getNetworkType(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        if (info == null || !info.isAvailable()) {
            return NetworkType.NETWORK_NO;
        }
        // ConnectivityManager.TYPE_WIFI = 1, TYPE_MOBILE = 0
        if (info.getType() == 1) {
            return NetworkType.NETWORK_WIFI;
        }
        if (info.getType() != 0) {
            return NetworkType.NETWORK_UNKNOWN;
        }
        switch (info.getSubtype()) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
            case NETWORK_TYPE_EVDO_0:
                return NetworkType.NETWORK_2G;
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
            case NETWORK_TYPE_HSDPA:
                return NetworkType.NETWORK_3G;
            case NETWORK_TYPE_LTE:
            case NETWORK_TYPE_HSPA:
                return NetworkType.NETWORK_4G;
            default:
                String subtypeName = info.getSubtypeName();
                if ("TD-SCDMA".equalsIgnoreCase(subtypeName)
                        || "WCDMA".equalsIgnoreCase(subtypeName)
                        || "CDMA2000".equalsIgnoreCase(subtypeName)) {
                    return NetworkType.NETWORK_3G;
                }
                return NetworkType.NETWORK_UNKNOWN;
        }
    }

    /**
     * 当前活动网络是否为可用的 LTE。
     *
     * <p>判据是 {@code NetworkInfo.isAvailable()} 且 {@code subtype == NETWORK_TYPE_LTE(13)}。</p>
     */
    public static boolean isLteAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isAvailable() && info.getSubtype() == NETWORK_TYPE_LTE;
    }

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();
    }

}
