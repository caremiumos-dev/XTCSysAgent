package com.xtc.utils.storage;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 外部存储环境探测工具。
 *
 * <p>与 {@link SdCardUtils} 的分工：SdCardUtils 关心"容量和真实挂载点"，
 * 本类关心"这块存储到底是不是可插拔的物理卡、系统给的环境变量是什么、
 * 以及多存储设备场景下都有哪些候选根目录"。</p>
 *
 * <p>工具类禁止实例化。</p>
 */
public class StorageUtils {

    /** 日志 tag（原厂保留但未实际使用，按原值保留）。 */
    private static final String TAG = "StorageUtils";

    /** 路径分隔符正则，用于从绝对路径里切出最后一级（也就是多用户场景下的用户 id）。 */
    private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("/");

    private StorageUtils() {
    }

    /**
     * 是否"可插拔的物理外部存储"。
     *
     * <p>系统既没有把它做成模拟存储（emulated），又报告可移除（removable），
     * 才认定是真实的 SD 卡。内置的模拟存储会返回 false。</p>
     */
    public static boolean isExternalStorageRemovable() {
        return !Environment.isExternalStorageEmulated() && Environment.isExternalStorageRemovable();
    }

    /** 外部存储是否 mounted；state 为空视为不可用。 */
    public static boolean isExternalStorageMounted() {
        if (TextUtils.isEmpty(Environment.getExternalStorageState())) {
            return false;
        }
        return "mounted".equals(Environment.getExternalStorageState());
    }

    /** 外部存储根目录。 */
    public static File getExternalStorageDirectory() {
        return Environment.getExternalStorageDirectory();
    }

    /** 外部存储总容量（字节）；未挂载返回 -1。 */
    public static long getExternalStorageTotalBytes() {
        if (!isExternalStorageMounted()) {
            return -1L;
        }
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return ((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize());
    }

    /** 外部存储可用容量（字节，double）；未挂载返回 -1。 */
    public static double getExternalStorageUsableBytes() {
        if (isExternalStorageMounted()) {
            return FileUtils.getFsAvailableSpace(Environment.getExternalStorageDirectory());
        }
        return -1.0d;
    }

    /** data 分区根目录。 */
    public static File getDataDirectory() {
        return Environment.getDataDirectory();
    }

    /** data 分区可用容量（字节）。 */
    public static long getDataUsableBytes() {
        return FileUtils.getFsAvailableSpace(Environment.getDataDirectory());
    }

    /** 系统给的外部存储环境变量（可能为 null）。 */
    public static String getExternalStorageEnvPath() {
        return System.getenv("EXTERNAL_STORAGE");
    }

    /**
     * 枚举所有可能的外部存储根路径。
     *
     * <p>优先级：EMULATED_STORAGE_TARGET（模拟存储）→ EXTERNAL_STORAGE → 硬编码
     * "/storage/sdcard0"；此外 SECONDARY_STORAGE 里的每一项都会追加进来。
     * 模拟存储还要区分"路径里是否带了用户 id"（Android 4.2 起多用户会把
     * /storage/emulated/0 这种形式带出来），所以取绝对路径最后一段判断是不是纯数字。</p>
     *
     * <p>原厂此方法为 private 且无调用方，属于预留能力，保持原样。</p>
     */
    private static String[] getExternalStoragePaths() {
        HashSet<String> paths = new HashSet<String>();
        String externalStorage = System.getenv("EXTERNAL_STORAGE");
        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        String emulatedTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if (!TextUtils.isEmpty(emulatedTarget)) {
            String userId = "";
            if (Build.VERSION.SDK_INT >= 17) {
                String[] segments = PATH_SEPARATOR_PATTERN.split(Environment.getExternalStorageDirectory().getAbsolutePath());
                userId = segments[segments.length - 1];
                boolean isNumeric = false;
                try {
                    Integer.valueOf(userId);
                    isNumeric = true;
                } catch (NumberFormatException e) {
                    // 最后一段不是数字，说明系统没把用户 id 拼进路径，忽略即可
                }
                if (!isNumeric) {
                    userId = "";
                }
            }
            if (TextUtils.isEmpty(userId)) {
                paths.add(emulatedTarget);
            } else {
                paths.add(emulatedTarget + File.separator + userId);
            }
        } else if (TextUtils.isEmpty(externalStorage)) {
            paths.add("/storage/sdcard0");
        } else {
            paths.add(externalStorage);
        }
        if (!TextUtils.isEmpty(secondaryStorage)) {
            Collections.addAll(paths, secondaryStorage.split(File.pathSeparator));
        }
        return paths.toArray(new String[paths.size()]);
    }
}
