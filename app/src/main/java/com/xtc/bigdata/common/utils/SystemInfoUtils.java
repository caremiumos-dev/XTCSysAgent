package com.xtc.bigdata.common.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.DisplayMetrics;

import com.xtc.log.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * 系统信息读取（CPU、ROM、RAM、屏幕、root 状态）。
 *
 * <p>这些值会被拼进 {@code AppLaunchEvent} 的 extend JSON，服务端按
 * {@code cpuInfo / memoryInfo / deviceCapacity / screenResolution / screenSize}
 * 取值，所以格式（含小数点位数）不能变。</p>
 */
public class SystemInfoUtils {

    private static final String TAG = "SystemInfoUtils";

    private SystemInfoUtils() {
    }

    /**
     * 保留一位小数（四舍五入）。
     *
     * @param value 原值
     * @return 一位小数的值
     */
    private static double formatDouble(double value) {
        return new BigDecimal(value).setScale(1, 4).doubleValue();
    }

    /**
     * Android 版本号。
     *
     * @return 版本串
     */
    public static String getAndroidVersion() {
        return String.valueOf(Build.VERSION.RELEASE);
    }

    /**
     * data 分区可用空间（已格式化）。
     *
     * @param context 上下文
     * @return 形如 {@code "1.2 GB"}
     */
    public static String getAvailableRomSize(Context context) {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return Formatter.formatFileSize(context, statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
    }

    /**
     * CPU 描述（取 {@code /proc/cpuinfo} 前两行的关键字段）。
     *
     * @return 形如 {@code "ARMv7 Processor rev 3 1234 "}
     */
    public static String getCpuInfo() {
        String[] parts = {"", ""};
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"), 8192);
            String[] firstLineFields = reader.readLine().split("\\s+");
            for (int index = 2; index < firstLineFields.length; index++) {
                parts[0] = parts[0] + firstLineFields[index] + " ";
            }
            parts[1] = parts[1] + reader.readLine().split("\\s+")[2];
            reader.close();
        } catch (IOException e) {
            // 读不到 cpuinfo 时返回半成品串，与原厂一致（不抛也不记日志）
        }
        return parts[0] + " " + parts[1];
    }

    /**
     * CPU 型号（从 {@code /proc/cpuinfo} 里找带 {@code Hardware} 的那一行）。
     *
     * @return 冒号后的部分；读到底都没找到返回 null
     */
    public static String getCpuName() {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader("/proc/cpuinfo");
            bufferedReader = new BufferedReader(fileReader);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    return null;
                }
                if (line.contains("Hardware")) {
                    return line.split(":")[1];
                }
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // 关流失败无法处理，直接忽略
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    // 同上
                }
            }
        }
    }

    /**
     * ROM 总空间（已格式化）。
     *
     * @param context 上下文
     * @return 形如 {@code "8.00 GB"}
     */
    public static String getRomTotalSize(Context context) {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return Formatter.formatFileSize(context, statFs.getBlockCountLong() * statFs.getBlockSizeLong());
    }

    /**
     * ROM 版本号。
     *
     * <p>优先取 {@code ro.modversion}，没有再用 {@code ro.build.display.id}；
     * 两者都没有则返回空串。</p>
     *
     * @return 版本串
     */
    public static String getRomVersion() {
        String version = "";
        try {
            String modVersion = getSystemProperty("ro.modversion");
            String displayId = getSystemProperty("ro.build.display.id");
            if (modVersion != null && !modVersion.equals("")) {
                version = modVersion;
            }
            return (displayId == null || displayId.equals("")) ? version : displayId;
        } catch (Throwable th) {
            return version;
        }
    }

    /**
     * 屏幕物理尺寸（英寸），按勾股定理从像素与 dpi 反推。
     *
     * @param context 上下文
     * @return 英寸数；计算失败返回 0
     */
    public static double getScreenInch(Context context) {
        try {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int widthPixels = displayMetrics.widthPixels;
            int heightPixels = displayMetrics.heightPixels;
            return formatDouble(Math.sqrt(((heightPixels / displayMetrics.ydpi) * (heightPixels / displayMetrics.ydpi))
                    + ((widthPixels / displayMetrics.xdpi) * (widthPixels / displayMetrics.xdpi))));
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0d;
        }
    }

    /**
     * 屏幕分辨率，形如 {@code "320*320"}。
     *
     * @param context 上下文
     * @return 分辨率串
     */
    public static String getScreenResolutionRatio(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels + "*" + displayMetrics.heightPixels;
    }

    /**
     * 机型。
     *
     * @return {@link Build#MODEL}
     */
    public static String getSystemModel() {
        return Build.MODEL;
    }

    /**
     * 读系统属性（反射 {@code android.os.SystemProperties}）。
     *
     * @param key 属性名
     * @return 属性值；失败返回 null
     */
    public static String getSystemProperty(String key) {
        try {
            return Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 总 RAM，向上取整成 GB。
     *
     * <p>单位后缀写死 {@code "GB"}，即使实际是 MB 级内存也一样 ——
     * 服务端按这个格式解析。</p>
     *
     * @param context 上下文
     * @return 形如 {@code "2GB"}
     */
    public static String getTotalRam(Context context) {
        String memTotal = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"), 8192);
            memTotal = reader.readLine().split("\\s+")[1];
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (memTotal != null
                ? (int) Math.ceil(new Float(Float.valueOf(memTotal).floatValue() / 1048576.0f).doubleValue())
                : 0) + "GB";
    }

    /**
     * 是否 root。
     *
     * <p>只看 su 二进制是否存在 —— 不执行 su，避免触发权限弹窗。</p>
     *
     * @return 是否 root
     */
    public static boolean isRoot() {
        try {
            return new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
        } catch (Exception e) {
            LogUtil.e(TAG, "isRoot error = " + e);
            e.printStackTrace();
            return false;
        }
    }
}
