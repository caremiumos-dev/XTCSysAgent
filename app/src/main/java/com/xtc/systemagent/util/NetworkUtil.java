package com.xtc.systemagent.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.lang.reflect.InvocationTargetException;

/**
 * 网络状态读取与开关控制。
 *
 * <p>这个类承担两类职责：</p>
 * <ol>
 *   <li>给采集日志打"网络标签"（WiFi SSID 或 {@code 运营商-2G/3G/4G}）；</li>
 *   <li>通过反射改网络模式 / 移动数据 / VoLTE。</li>
 * </ol>
 */
public class NetworkUtil {

    private static final String TAG = "NetworkUtil";

    /** 首选网络模式在 Settings.Global 里的键前缀（后面拼 subId）。 */
    private static final String PREFERRED_NETWORK_MODE = "preferred_network_mode";

    /** 飞行模式开关。 */
    private static final String SETTING_AIRPLANE_MODE_ON = "airplane_mode_on";

    private static final int NETWORK_MODE_GSM_ONLY = 1;
    private static final int NETWORK_MODE_CDMA_NO_EVDO = 5;
    private static final int NETWORK_MODE_TD_SCDMA_CDMA_EVDO_GSM_WCDMA = 21;
    private static final int NETWORK_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA = 22;

    /** {@code ConnectivityManager.TYPE_MOBILE}。 */
    private static final int TYPE_MOBILE = 0;
    /** {@code ConnectivityManager.TYPE_WIFI}。 */
    private static final int TYPE_WIFI = 1;

    /** 组网络标签的 SLA 阈值：超过 5 秒要告警。 */
    private static final long TAG_COST_WARN = 5000L;

    private NetworkUtil() {
    }

    /** 取当前网络模式，用默认 subId。 */
    public static int getCurrentNetworkMode(Context context) {
        return getCurrentNetworkMode(context, getDefaultSubscriptionId(context));
    }

    /**
     * 取指定 subId 的网络模式。
     *
     * <p>Settings 里没有记录时退回系统属性 {@code ro.telephony.default_network}。</p>
     */
    public static int getCurrentNetworkMode(Context context, int subId) {
        int mode = Settings.Global.getInt(context.getContentResolver(),
                PREFERRED_NETWORK_MODE + subId,
                SystemPropertiesUtils.getInt(SystemConstants.PROP_DEFAULT_NETWORK, 0));
        LogUtil.i(TAG, "currentPreferredNetworkMode：" + mode);
        return mode;
    }

    private static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager.from(context);
        int subId = SubscriptionManager.getDefaultSubscriptionId();
        LogUtil.d(TAG, "defaultSubscriptionId = " + subId);
        return subId;
    }

    /** 反射读移动数据开关。 */
    public static boolean getMobileDataEnabled(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Constants.PHONE);
        try {
            return ((Boolean) telephonyManager.getClass()
                    .getMethod("getDataEnabled", new Class[0])
                    .invoke(telephonyManager, new Object[0])).booleanValue();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
        return false;
    }

    /**
     * 生成网络标签。
     *
     * <p>WiFi 用 SSID，移动网络用 {@code 运营商-制式}，其它一律
     * {@code unknown network tag}。</p>
     */
    public static String getNetworkInfo(Context context) {
        String tag;
        long begin = SystemClock.elapsedRealtime();
        NetworkInfo activeNetworkInfo = ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable()) {
            tag = null;
        } else {
            int type = activeNetworkInfo.getType();
            if (type == TYPE_WIFI) {
                WifiInfo wifiInfo = ((WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
                tag = wifiInfo != null ? wifiInfo.getSSID() : null;
            } else if (type == TYPE_MOBILE) {
                tag = getSIMType(context) + "-" + getNetworkType(context);
            } else {
                tag = null;
            }
        }
        if (tag == null) {
            tag = "unknown network tag";
        }
        long cost = SystemClock.elapsedRealtime() - begin;
        LogUtil.i(TAG, "Network tag:" + tag + ", useTime:" + cost);
        if (cost > TAG_COST_WARN) {
            LogUtil.w(TAG, "getNetworkInfo cost too long : " + cost);
        }
        return tag;
    }

    /** 当前网络制式。 */
    public static String getNetworkType(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable()) {
            return "no network";
        }
        int type = activeNetworkInfo.getType();
        if (type == TYPE_WIFI) {
            return "WIFI";
        }
        if (type != TYPE_MOBILE) {
            return "unkonw network,type:" + type;
        }
        int networkType = ((TelephonyManager) context.getSystemService(Constants.PHONE))
                .getNetworkType();
        switch (networkType) {
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
                return "unkonw network,network type:" + networkType;
        }
    }

    /** 运营商名称；取不到名称时按 MCC/MNC 兜底。 */
    public static String getSIMType(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Constants.PHONE);
        String simOperator = telephonyManager.getSimOperator();
        String simOperatorName = telephonyManager.getSimOperatorName();
        String simType = "unknown sim type";
        if (!TextUtils.isEmpty(simOperatorName)) {
            simType = simOperatorName;
        } else if (simOperator != null) {
            if (simOperator.equals("46000") || simOperator.equals("46002")) {
                simType = "中国移动";
            } else if (simOperator.equals("46001")) {
                simType = "中国联通";
            } else if (simOperator.equals("46003")) {
                simType = "中国电信";
            } else {
                simType = simOperator;
            }
        }
        LogUtil.i(TAG, "getSIMType:" + simType);
        return simType;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), SETTING_AIRPLANE_MODE_ON, 0) != 0;
    }

    public static boolean isConnected(Context context) {
        NetworkInfo info = ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isMobileConnected(Context context) {
        NetworkInfo info = ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info != null && info.isConnected() && info.getType() == TYPE_MOBILE;
    }

    public static boolean isWifiConnected(Context context) {
        NetworkInfo info = ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info != null && info.isConnected() && info.getType() == TYPE_WIFI;
    }

    /** WiFi 网卡是否已连接（不看当前活跃网络）。 */
    public static boolean isWifiConnect(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getNetworkInfo(TYPE_WIFI).isConnected();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isAvailable();
    }

    /**
     * 是否开启了 VoLTE。
     *
     * <p>两个隐藏方法都是静态的，原厂用 {@code invoke(cls, context)} 调用 ——
     * 对静态方法而言第一个参数会被忽略，行为正确。这里保持同样的写法。</p>
     */
    public static boolean isVltEnabled(Context context) {
        boolean enabled;
        try {
            Class<?> imsManager = Class.forName("com.android.ims.ImsManager");
            boolean userEnabled = ((Boolean) imsManager
                    .getMethod("isEnhanced4gLteModeSettingEnabledByUser", Context.class)
                    .invoke(imsManager, context)).booleanValue();
            boolean ttyEnabled = ((Boolean) imsManager
                    .getMethod("isNonTtyOrTtyOnVolteEnabled", Context.class)
                    .invoke(imsManager, context)).booleanValue();
            LogUtil.i(TAG, "result1：" + userEnabled + "，result2：" + ttyEnabled);
            enabled = userEnabled && ttyEnabled;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            enabled = false;
        }
        LogUtil.i(TAG, "enh4glteMode：" + enabled);
        return enabled;
    }

    public static void setVltEnabled(Context context, boolean enabled) {
        if (isVltEnabled(context) == enabled) {
            return;
        }
        LogUtil.d(TAG, "setVltEnabled：" + enabled);
        try {
            Class<?> imsManager = Class.forName("com.android.ims.ImsManager");
            imsManager.getMethod("setEnhanced4gLteModeSetting", Context.class, Boolean.TYPE)
                    .invoke(imsManager, context, Boolean.valueOf(enabled));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
    }

    public static void setAirplaneModeOn(Context context, boolean enable) {
        LogUtil.d(TAG, "setAirplaneModeOn：" + enable);
        Settings.Global.putInt(context.getContentResolver(), SETTING_AIRPLANE_MODE_ON,
                enable ? 1 : 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", enable);
        context.sendBroadcast(intent);
    }

    /** 反射开关移动数据。 */
    public static void setMobileDataEnabled(Context context, boolean enabled) {
        LogUtil.d(TAG, "setMobileDataEnabled：" + enabled);
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Constants.PHONE);
        try {
            telephonyManager.getClass()
                    .getMethod("setDataEnabled", Boolean.TYPE)
                    .invoke(telephonyManager, Boolean.valueOf(enabled));
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
    }

    /**
     * 设置首选网络模式。
     *
     * @return 真的发生了切换返回 true；本来就是这个模式、或反射失败返回 false
     */
    public static boolean setPreferredNetworkType(Context context, int networkMode) {
        LogUtil.i(TAG, "networkMode=" + networkMode);
        int subId = getDefaultSubscriptionId(context);
        if (getCurrentNetworkMode(context, subId) == networkMode) {
            LogUtil.i(TAG, "不需要更改网络模式");
            return false;
        }
        try {
            TelephonyManager.class
                    .getMethod("setPreferredNetworkType", Integer.TYPE, Integer.TYPE)
                    .invoke((TelephonyManager) context.getSystemService(Constants.PHONE),
                            Integer.valueOf(subId), Integer.valueOf(networkMode));
            return true;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
        return false;
    }
}
