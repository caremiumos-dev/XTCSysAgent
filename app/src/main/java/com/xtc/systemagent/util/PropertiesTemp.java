package com.xtc.systemagent.util;

import com.xtc.log.LogUtil;

/**
 * 通过反射访问 {@code android.os.SystemProperties}（隐藏 API）。
 *
 * <p>目前只服务于"通话失败日志抓取时长"这一个属性。</p>
 */
public class PropertiesTemp {

    private static final String TAG = "Log_PropertiesTemp";
    /** 通话失败日志的抓取时长（分钟）。 */
    private static final String PROPERTY_CALL_FAILURE_TIME = "persist.sys.call.failure.time";

    private static int getIntegerSystemProperties(String key, int defaultValue) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            return ((Integer) clazz.getMethod("getInt", String.class, Integer.TYPE)
                    .invoke(clazz, key, Integer.valueOf(defaultValue))).intValue();
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return defaultValue;
        }
    }

    private static void setSystemProperties(String key, int value) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            clazz.getMethod("set", String.class, String.class)
                    .invoke(clazz, key, String.valueOf(value));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
    }

    /** 写入抓取时长，并立即读回打日志确认生效。 */
    public static void setCallLogFetchDuration(int duration) {
        setSystemProperties(PROPERTY_CALL_FAILURE_TIME, duration);
        LogUtil.i(TAG, "setCallLogFetchDuration = " + duration
                + " , after get = " + getIntegerSystemProperties(PROPERTY_CALL_FAILURE_TIME, -1));
    }
}
