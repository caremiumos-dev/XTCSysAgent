package com.xtc.httplib.util;

import com.xtc.httplib.LogTag;
import com.xtc.log.LogUtil;
import java.lang.reflect.InvocationTargetException;

/**
 * {@code android.os.SystemProperties} 的反射封装（get/getInt/getBoolean/set）。
 *
 * <p>为什么不用 {@code com.xtc.utils.system.SystemPropertiesUtils}：本类属于 HTTP 库的
 * 私有实现，**全部方法都是 private 且当前没有调用方**，是原厂给库内部预留的能力
 * （这样 HTTP 库可以独立于 utils 包发布）。为了保持文件/方法结构与原厂一致，
 * 这里照原样保留，不要合并、也不要删成空类。</p>
 *
 * <p>约定（与原厂一致，且与另一处 {@code WatchInfo#getSystemProperty} 不同）：</p>
 * <ul>
 *   <li>字符串读取失败返回**空串**；</li>
 *   <li>整数读取失败返回**入参默认值**；</li>
 *   <li>布尔读取失败返回 **false**（不是入参默认值）。</li>
 * </ul>
 * <p>三类不一致是有意保留的，调用方已按此约定编写。</p>
 */
public class SystemProperties {

    private static final String TAG = LogTag.tag("SystemProperties");

    /** 反射目标类名。 */
    private static final String CLASS_SYSTEM_PROPERTIES = "android.os.SystemProperties";

    private SystemProperties() {
    }

    private static boolean getBooleanSystemProperties(String key, boolean defaultValue) {
        try {
            Class<?> clazz = Class.forName(CLASS_SYSTEM_PROPERTIES);
            return ((Boolean) clazz.getMethod("getBoolean", String.class, Boolean.TYPE)
                    .invoke(clazz, key, Boolean.valueOf(defaultValue))).booleanValue();
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
            return false;
        }
    }

    private static int getIntegerSystemProperties(String key, int defaultValue) {
        try {
            Class<?> clazz = Class.forName(CLASS_SYSTEM_PROPERTIES);
            return ((Integer) clazz.getMethod("getInt", String.class, Integer.TYPE)
                    .invoke(clazz, key, Integer.valueOf(defaultValue))).intValue();
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
            return defaultValue;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
            return defaultValue;
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
            return defaultValue;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
            return defaultValue;
        }
    }

    private static String getStringSystemProperties(String key) {
        try {
            Class<?> clazz = Class.forName(CLASS_SYSTEM_PROPERTIES);
            return (String) clazz.getMethod("get", String.class).invoke(clazz, key);
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
            return "";
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
            return "";
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
            return "";
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
            return "";
        }
    }

    private static void setSystemProperties(String key, int value) {
        try {
            Class<?> clazz = Class.forName(CLASS_SYSTEM_PROPERTIES);
            clazz.getMethod("set", String.class, String.class).invoke(clazz, key, String.valueOf(value));
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
    }

    private static void setSystemProperties(String key, String value) {
        LogUtil.d(TAG, "setSystemProperties:{" + key + "," + value + "}");
        try {
            Class<?> clazz = Class.forName(CLASS_SYSTEM_PROPERTIES);
            clazz.getMethod("set", String.class, String.class).invoke(clazz, key, value);
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
    }

    private static void setSystemProperties(String key, boolean value) {
        try {
            Class<?> clazz = Class.forName(CLASS_SYSTEM_PROPERTIES);
            clazz.getMethod("set", String.class, String.class).invoke(clazz, key, String.valueOf(value));
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
    }
}
