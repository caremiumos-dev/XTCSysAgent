package com.xtc.bigdata.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.log.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * 存储与内存容量的读取与格式化。
 *
 * <p>崩溃事件里的 {@code diskTotal/diskUsage/sdTotal/sdUsage/memTotal/memUsage}
 * 六个字段全部出自本类，所以这里的单位换算必须与原厂一致：
 * 容量统一用 {@link #convertSizeUnit(long)} 转成 {@code "x.xx GB/MB/KB"}，
 * 使用率用 {@link #availablepercent(long, long)} 转成 {@code "xx%"}。</p>
 */
public class StoreUtils {

    /** 1GB 的字节数。 */
    private static final int GB = 1073741824;

    /** 1KB 的字节数。 */
    private static final int KB = 1024;

    /** 1MB 的字节数。 */
    private static final long MB = 1048576;

    private static final String TAG = "StoreUtils";

    private StoreUtils() {
    }

    /**
     * 计算可用占比字符串。
     *
     * @param available 可用字节数
     * @param total     总字节数
     * @return 形如 {@code "42%"}；总量非法（<=0）时返回 null
     */
    public static String availablepercent(long available, long total) {
        if (total <= 0) {
            return null;
        }
        return ((100 * available) / total) + "%";
    }

    /**
     * 确保某个文件路径的父目录存在。
     *
     * <p>即使 mkdirs 失败也返回 true —— 这里只是"尽力准备目录"，
     * 真正的写失败由后续 IO 操作暴露。原厂行为如此。</p>
     *
     * @param filePath 文件路径
     * @return 父目录存在（或已创建）返回 true；路径非法返回 false
     */
    public static boolean checkFileDirExisted(String filePath) {
        String parentDir = getParentDir(filePath);
        if (parentDir == null) {
            return false;
        }
        File dir = new File(parentDir);
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                LogUtil.d(TAG, "create folder " + parentDir + " failed");
            }
            return true;
        } catch (SecurityException e) {
            LogUtil.e(TAG, e);
            return false;
        }
    }

    /**
     * 两个时间戳是否相差超过给定秒数。
     *
     * @param firstTime  时间一
     * @param secondTime 时间二
     * @param seconds    阈值（秒）
     * @return 超过阈值返回 true
     */
    public static boolean compare2Sec(long firstTime, long secondTime, int seconds) {
        Log.v(TAG, (Math.abs(firstTime - secondTime) / 1000) + "");
        return Math.abs(firstTime - secondTime) / 1000 > ((long) seconds);
    }

    /**
     * 把字节数格式化成带单位的可读串。
     *
     * <p>阈值是 {@code 1KB -> 1MB -> 1GB}；注意第二个分支里
     * {@code bytes >= 1073741824} 的判断永远不成立（上面已经拦掉了），
     * 保留它是为了与原厂的判断序列完全一致。</p>
     *
     * @param bytes 字节数
     * @return 形如 {@code "12.34 MB"}
     */
    public static String convertSizeUnit(long bytes) {
        if (bytes >= GB) {
            return String.format(Locale.CHINA, "%.02f GB", Float.valueOf(bytes / 1.0737418E9f));
        }
        return (bytes < MB || bytes >= GB)
                ? String.format(Locale.CHINA, "%.02f KB", Float.valueOf(bytes / 1024.0f))
                : String.format(Locale.CHINA, "%.02f MB", Float.valueOf(bytes / 1048576.0f));
    }

    /**
     * 递归删除目录。
     *
     * @param dirPath 目录路径
     * @return 是否删除成功
     */
    private static boolean deleteDirectory(String dirPath) {
        if (TextUtils.isEmpty(dirPath)) {
            return false;
        }
        return FileUtils.deleteDir(new File(dirPath));
    }

    /**
     * 删除单个文件。
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    private static boolean deleteFile(String filePath) {
        return FileUtils.deleteFile(filePath);
    }

    /**
     * 删除文件或目录（自动区分）。
     *
     * @param path 路径
     * @return 是否删除成功；不存在返回 false
     */
    public static boolean deleteFolder(String path) {
        if (path == null) {
            return false;
        }
        File file = new File(path);
        if (file.exists()) {
            return file.isFile() ? deleteFile(path) : deleteDirectory(path);
        }
        return false;
    }

    /**
     * 外部存储是否挂载。
     *
     * @return 是否已挂载
     */
    private static boolean externalStoreAvailable() {
        String state = Environment.getExternalStorageState();
        return state != null && state.equals("mounted");
    }

    /**
     * data 分区可用空间。
     *
     * @return 字节数
     */
    public static long getExternalStoreAvailableSize() {
        return StorageUtils.getDataAvailableSize();
    }

    /**
     * 外部存储总空间。
     *
     * @return 字节数
     */
    public static long getExternalStoreTotalSize() {
        return StorageUtils.getExternalStorageTotalSize();
    }

    /**
     * 递归统计文件或目录大小。
     *
     * @param path 路径
     * @return 字节数；路径为空或不是目录时返回 0（单个文件返回其长度）
     */
    public static long getFileSize(String path) {
        long size = 0;
        if (path == null) {
            return 0L;
        }
        File file = new File(path);
        if (!file.isDirectory()) {
            return file.length();
        }
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        for (File child : children) {
            size += child.isDirectory() ? getFileSize(child.getAbsolutePath()) : child.length();
        }
        return size;
    }

    /**
     * 内部存储（/system）可用空间。
     *
     * @return 字节数
     */
    public static long getInternalStoreAvailableSize() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getPath());
        return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
    }

    /**
     * 内部存储（/system）总空间。
     *
     * @return 字节数
     */
    public static long getInternalStoreTotalSize() {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getPath());
        return statFs.getBlockCountLong() * statFs.getBlockSizeLong();
    }

    /**
     * 当前进程可用内存。
     *
     * @param context 上下文
     * @return 字节数；ActivityManager 拿不到时返回 0
     */
    public static long getMemoryAvailable(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager == null) {
            LogUtil.e(TAG, "error , ActivityManager = null !");
            return 0L;
        }
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    /**
     * 总内存（读 {@code /proc/meminfo} 的 MemTotal，单位换算成字节）。
     *
     * <p>关闭流的失败只记日志，因为此时结果已经拿到了；
     * 读取失败则返回 0，让上报侧看到"未知"而不是脏数据。</p>
     *
     * @return 字节数；读取失败返回 0
     */
    public static long getMemoryTotalSize() {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader("/proc/meminfo");
            bufferedReader = new BufferedReader(fileReader, 2048);
            String line = bufferedReader.readLine();
            if (!TextUtils.isEmpty(line)) {
                // 行内容形如 "MemTotal:  524288 kB"，去掉非数字后乘 1KB
                return ((long) Integer.parseInt(line.substring(line.indexOf("MemTotal:")).replaceAll("\\D+", ""))) * 1048576L;
            }
            return 0L;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return 0L;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }
    }

    /**
     * 取父目录（含末尾斜杠之前的部分）。
     *
     * @param path 路径
     * @return 父目录；没有分隔符或出错返回 null
     */
    private static String getParentDir(String path) {
        if (path == null) {
            return null;
        }
        try {
            int lastSeparator = path.lastIndexOf("/");
            if (lastSeparator > -1) {
                return path.substring(0, lastSeparator);
            }
            return null;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }
}
