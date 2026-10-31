package com.xtc.utils.system;

import android.annotation.TargetApi;
import com.xtc.log.LogUtil;
import java.lang.reflect.InvocationTargetException;

/**
 * 通过反射访问 {@code android.os.SystemProperties}（隐藏 API）。
 *
 * <p>原厂类名为单字母 {@code p}，方法名也是 {@code a}/{@code b}/{@code c}/{@code d}
 * 四套重载。这里按"读/写"与参数类型还原成 {@code getXxx}/{@code setXxx}，
 * 行为（包括每个 catch 分支打印什么日志）完全保留。</p>
 *
 * <p>注意：带 {@code @Deprecated} 的重载在字节码里走的是
 * {@code invoke(Class, ...)}（静态方法的 receiver 传 Class），
 * 其余走 {@code invoke(null, ...)}。两者对 SystemProperties 都等价，
 * 但为了保持调用形态一致，这里原样保留。</p>
 */
public class SystemPropertiesUtils {

    private static final String TAG = "SystemPropertyUtil";

    /** 原厂遗留的"无效 long 值"。 */
    @Deprecated
    public static final long INVALID_LONG = -1;

    private SystemPropertiesUtils() {
    }

    // ------------------------------------------------------------------
    // 写
    // ------------------------------------------------------------------

    /** 写入 int 属性。 */
    public static void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    /** 写入 long 属性（已废弃的重载）。 */
    @Deprecated
    @TargetApi(19)
    public static void setLongLegacy(String key, long value) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            clazz.getMethod("set", String.class, String.class).invoke(clazz, key, String.valueOf(value));
            LogUtil.i(key + "：" + value);
        } catch (ClassNotFoundException e) {
            LogUtil.e(e);
        } catch (IllegalAccessException e) {
            LogUtil.e(e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(e);
        } catch (InvocationTargetException e) {
            LogUtil.e(e);
        }
    }

    /** 写入字符串属性。 */
    public static void set(String key, String value) {
        try {
            Class.forName("android.os.SystemProperties")
                    .getMethod("set", String.class, String.class)
                    .invoke(null, key, value);
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while setting system property: " + e);
            e.printStackTrace();
        }
    }

    /** 写入 boolean 属性（已废弃的重载）。 */
    @Deprecated
    @TargetApi(19)
    public static void setBooleanLegacy(String key, boolean value) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            clazz.getMethod("set", String.class, String.class).invoke(clazz, key, String.valueOf(value));
            LogUtil.i(key + "：" + value);
        } catch (ClassNotFoundException e) {
            LogUtil.e(e);
        } catch (IllegalAccessException e) {
            LogUtil.e(e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(e);
        } catch (InvocationTargetException e) {
            LogUtil.e(e);
        }
    }

    /** 写入 long 属性。 */
    public static void setLong(String key, long value) {
        set(key, String.valueOf(value));
    }

    /** 写入 boolean 属性。 */
    public static void setBoolean(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    // ------------------------------------------------------------------
    // 读
    // ------------------------------------------------------------------

    /** 读 int 属性。 */
    public static int getInt(String key, int defaultValue) {
        try {
            Integer value = (Integer) Class.forName("android.os.SystemProperties")
                    .getMethod("getInt", String.class, Integer.TYPE)
                    .invoke(null, key, Integer.valueOf(defaultValue));
            return value == null ? defaultValue : value.intValue();
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while getting system property: " + e);
            e.printStackTrace();
            return defaultValue;
        }
    }

    /** 读 long 属性（已废弃的重载）。 */
    @Deprecated
    @TargetApi(19)
    public static long getLongLegacy(String key, long defaultValue) {
        long value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            value = ((Long) clazz.getMethod("getLong", String.class, Long.TYPE)
                    .invoke(clazz, key, Long.valueOf(defaultValue))).longValue();
        } catch (ClassNotFoundException e) {
            LogUtil.e(e);
        } catch (IllegalAccessException e) {
            LogUtil.e(e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(e);
        } catch (InvocationTargetException e) {
            LogUtil.e(e);
        }
        LogUtil.i(key + "：" + value);
        return value;
    }

    /** 读字符串属性。 */
    public static String get(String key, String defaultValue) {
        try {
            return (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, defaultValue);
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while getting system property: " + e);
            e.printStackTrace();
            return defaultValue;
        }
    }

    /** 读 boolean 属性（已废弃的重载）。 */
    @Deprecated
    @TargetApi(19)
    public static boolean getBooleanLegacy(String key, boolean defaultValue) {
        boolean value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            value = ((Boolean) clazz.getMethod("getBoolean", String.class, Boolean.TYPE)
                    .invoke(clazz, key, Boolean.valueOf(defaultValue))).booleanValue();
        } catch (ClassNotFoundException e) {
            LogUtil.e(e);
        } catch (IllegalAccessException e) {
            LogUtil.e(e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(e);
        } catch (InvocationTargetException e) {
            LogUtil.e(e);
        }
        LogUtil.i(key + "：" + value);
        return value;
    }

    /** 读 long 属性。 */
    public static long getLong(String key, long defaultValue) {
        try {
            Long value = (Long) Class.forName("android.os.SystemProperties")
                    .getMethod("getLong", String.class, Long.TYPE)
                    .invoke(null, key, Long.valueOf(defaultValue));
            return value == null ? defaultValue : value.longValue();
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while getting system property: " + e);
            e.printStackTrace();
            return defaultValue;
        }
    }

    /** 读 boolean 属性。 */
    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            Boolean value = (Boolean) Class.forName("android.os.SystemProperties")
                    .getMethod("getBoolean", String.class, Boolean.TYPE)
                    .invoke(null, key, Boolean.valueOf(defaultValue));
            return value == null ? defaultValue : value.booleanValue();
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while getting system property: " + e);
            e.printStackTrace();
            return defaultValue;
        }
    }
}
