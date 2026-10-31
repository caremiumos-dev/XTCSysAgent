package com.xtc.bigdata.common.utils;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 存储空间与存储路径查询。
 *
 * <p>名字与 {@code com.xtc.utils.storage.StorageUtils} 重名，但两者是不同的类：
 * 这边是采集库私有的、值语义略有出入的实现，别看错。</p>
 *
 * <p>所有"取不到"的路径一律返回 -1（尺寸）或 null（路径），调用方必须当成
 * 不可用处理，不能当成 0 字节。</p>
 */
public class StorageUtils {

    /** 路径分隔符正则（用来切 {@code /storage/emulated/0}）。 */
    private static final Pattern DIR_SEPORATOR = Pattern.compile("/");

    private static final String TAG = "StorageUtils";

    private StorageUtils() {
    }

    /**
     * 取 data 分区可用空间。
     *
     * @return 字节数
     */
    public static long getDataAvailableSize() {
        return FileUtils.getFileAvaiableSize(Environment.getDataDirectory());
    }

    /**
     * 取 data 分区目录。
     *
     * @return 目录
     */
    public static File getDataFile() {
        return Environment.getDataDirectory();
    }

    /**
     * 取外部存储可用空间。
     *
     * @return 字节数；外部存储不可用时返回 -1
     */
    public static double getExternalStorageAvailableSize() {
        if (isExternalStroageAvailable()) {
            return FileUtils.getFileAvaiableSize(Environment.getExternalStorageDirectory());
        }
        return -1.0d;
    }

    /**
     * 取外部存储目录。
     *
     * @return 目录
     */
    public static File getExternalStorageFile() {
        return Environment.getExternalStorageDirectory();
    }

    /**
     * 取外部存储总空间。
     *
     * @return 字节数；外部存储不可用时返回 -1
     */
    public static long getExternalStorageTotalSize() {
        if (!isExternalStroageAvailable()) {
            return -1L;
        }
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return statFs.getBlockCountLong() * statFs.getBlockSizeLong();
    }

    /**
     * 取 SD 卡路径（直接读环境变量）。
     *
     * @return 路径；环境变量不存在时返回 null
     */
    public static String getSdCardPath() {
        return System.getenv("EXTERNAL_STORAGE");
    }

    /**
     * 枚举所有可能的存储根目录。
     *
     * <p>优先用 {@code EMULATED_STORAGE_TARGET}（多用户场景要拼上用户 id），
     * 其次 {@code EXTERNAL_STORAGE}，最后兜底 {@code /storage/sdcard0}；
     * {@code SECONDARY_STORAGE} 里的多路径再逐个补进去。</p>
     *
     * <p>原厂只在 {@link #getSdCardPath()} 之外保留了这段逻辑，
     * 当前包内没有调用点，属于预留能力。</p>
     *
     * @return 目录数组
     */
    private static String[] getStorageDirectories() {
        HashSet<String> paths = new HashSet<String>();
        String externalStorage = System.getenv("EXTERNAL_STORAGE");
        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        String emulatedTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if (!TextUtils.isEmpty(emulatedTarget)) {
            String userPartition;
            if (Build.VERSION.SDK_INT < 17) {
                userPartition = "";
            } else {
                String[] segments = DIR_SEPORATOR.split(Environment.getExternalStorageDirectory().getAbsolutePath());
                userPartition = segments[segments.length - 1];
                boolean isNumeric = false;
                try {
                    Integer.valueOf(userPartition);
                    isNumeric = true;
                } catch (NumberFormatException e) {
                    // 不是数字说明最后一个路径段不是用户 id，保持空串
                }
                if (!isNumeric) {
                    userPartition = "";
                }
            }
            if (TextUtils.isEmpty(userPartition)) {
                paths.add(emulatedTarget);
            } else {
                paths.add(emulatedTarget + File.separator + userPartition);
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

    /**
     * 外部存储是否可移除。
     *
     * @return 是否可移除
     */
    public static boolean isExternalStorageRemoveable() {
        return !Environment.isExternalStorageEmulated() && Environment.isExternalStorageRemovable();
    }

    /**
     * 外部存储是否挂载可用。
     *
     * <p>方法名里的 {@code Stroage} 是原厂拼写错误，调用点按这个签名编译，
     * 不要"顺手"改正。</p>
     *
     * @return 是否可用
     */
    public static boolean isExternalStroageAvailable() {
        if (TextUtils.isEmpty(Environment.getExternalStorageState())) {
            return false;
        }
        return "mounted".equals(Environment.getExternalStorageState());
    }
}
