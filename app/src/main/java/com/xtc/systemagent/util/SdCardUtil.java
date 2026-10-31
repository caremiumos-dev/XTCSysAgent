package com.xtc.systemagent.util;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;
import com.xtc.utils.storage.StorageConstants;

import java.io.File;

/**
 * SD 卡（外部存储）相关工具类：负责各类日志目录的拼装、可用空间判断和目录大小统计。
 *
 * <p>原厂实现里所有路径都以外部存储根目录为基准，本类只是把这些约定集中在一处，
 * 供 {@code Constants} 等常量接口在类初始化时取用。</p>
 */
public class SdCardUtil {

    /** 系统日志目录下每个文件夹的公共前缀。 */
    public static final String LOG_FOLDER_PREFIX = "Folder_";

    private static final String TAG = "SdCardUtil";

    /** 小天才定制数据分区根目录。 */
    private static final String XTC_DATA_PATH = "/xtcdata";

    /**
     * 普通空间检查：外部存储是否可用、剩余空间是否满足最小值。
     *
     * @param minSpace 需要的最小剩余字节数
     * @return 空串表示检查通过，否则返回带错误码的说明文字
     */
    public static String execNormalCheck(long minSpace) {
        if (!isAvailable()) {
            return "22-sd卡 不可用";
        }
        long availableSize = getAvailableSize();
        Log.d(TAG, "availSdSize = " + availableSize + " , minSpace = " + minSpace);
        if (availableSize < minSpace) {
            return "23-sd卡 可用空间 不足 , 当前大小为 = " + availableSize;
        }
        return "";
    }

    /**
     * 剩余空间占比检查。
     *
     * @param percent 需要的最小剩余百分比（0~100）
     * @return 剩余空间占比大于 {@code percent} 时返回 true
     */
    public static boolean execSpaceCheck(int percent) {
        if (!isAvailable()) {
            Log.d(TAG, "sd 卡不可用");
            return false;
        }
        long availableSize = getAvailableSize();
        long totalSize = getTotalSize();
        Log.d(TAG, "totalSize: " + totalSize + " ,availSdSize: " + availableSize);
        return ((float) availableSize) / ((float) totalSize) * 100.0f > ((float) percent);
    }

    /** adsp ramdump 目录。 */
    public static String getAdspDumpFilePath() {
        return getSdCardPath() + File.separator + "adspramdump" + File.separator;
    }

    /** adsp dump 打包目录。 */
    public static String getAdspDumpZipFilePath() {
        return getSdCardPath() + File.separator + "adsp_dump_zip" + File.separator;
    }

    /** 应用日志根目录。 */
    public static String getAppLogDir() {
        return getSdCardPath() + File.separator + "xtc" + File.separator + "ibwatch"
                + File.separator + StorageConstants.DIR_LOGS + File.separator;
    }

    /** 外部存储当前可用字节数。 */
    public static long getAvailableSize() {
        StatFs statFs = new StatFs(getSdCardPath());
        return statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
    }

    private static String getDataSlogDir() {
        return "data" + File.separator + "slog" + File.separator;
    }

    /** dropbox 日志目录。 */
    public static String getDropboxLogDir() {
        return getSdCardPath() + "/bigdata/dropbox/";
    }

    /** 指定日期的 dropbox 日志子目录。 */
    public static String getDropboxOneDayDir(String day) {
        return getDropboxLogDir() + day + File.separator;
    }

    /** dropbox 日志压缩包完整路径。 */
    public static String getDropboxZipDir() {
        return getDropboxLogDir() + "dropbox_log.zip";
    }

    /** mcu dump 目录。 */
    public static String getMcuDumpFilePath() {
        return "/data" + File.separator + "vendor" + File.separator + "gps"
                + File.separator + "mcudump" + File.separator;
    }

    /** 上一次（备份）modem 日志目录。 */
    public static String getModemBackupLogFilePath() {
        return getSdCardPath() + File.separator + "modem_logs_pre" + File.separator;
    }

    /** modem ramdump 目录。 */
    public static String getModemDumpFilePath() {
        return getSdCardPath() + File.separator + "mdramdump" + File.separator;
    }

    /** modem dump 打包目录。 */
    public static String getModemDumpZipFilePath() {
        return getSdCardPath() + File.separator + "modem_dump_zip" + File.separator;
    }

    /**
     * modem 日志目录。Android 5.0 以下硬件把它放在 /data/modem_log。
     */
    public static String getModemLogFilePath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getSdCardPath() + File.separator + "diag_logs" + File.separator;
        }
        return "/data" + File.separator + "modem_log" + File.separator;
    }

    /** modem 日志打包目录。 */
    public static String getModemLogZipFilePath() {
        return getSdCardPath() + File.separator + "modem_logs_zip" + File.separator;
    }

    /** 网络侧 modem 日志打包目录。 */
    public static String getNertWorkModemLogZipFilePath() {
        return getSdCardPath() + File.separator + "netmodem_logs_zip" + File.separator;
    }

    /** 指定应用名的日志目录。 */
    public static String getOneAppLogDir(String appName) {
        return getAppLogDir() + appName + File.separator;
    }

    /** 日志收集根目录。 */
    public static String getSdCardLogDir() {
        return getSdCardPath() + File.separator + "Logs_Collector" + File.separator;
    }

    /** 日志收集子目录。 */
    public static String getSdCardLogDir(String subDir) {
        if (TextUtils.isEmpty(subDir)) {
            return getSdCardLogDir();
        }
        return getSdCardLogDir() + subDir + File.separator;
    }

    private static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * 依次探测 /xtcdata/Logs_Collector、data/slog、/xtcdata/slog，
     * 都不存在时退回外部存储上的 Logs_Collector。
     */
    public static String getSystemLogDir() {
        String sdCardLogDir = getSdCardLogDir();
        String xtcDataLogDir = getXtcDataLogDir();
        String dataSlogDir = getDataSlogDir();
        String xtcDataSlogDir = getXtcDataSlogDir();
        File file = new File(xtcDataLogDir);
        File file2 = new File(dataSlogDir);
        File file3 = new File(xtcDataSlogDir);
        if (file.exists() && file.isDirectory()) {
            return xtcDataLogDir;
        }
        if (file2.exists() && file2.isDirectory()) {
            return dataSlogDir;
        }
        if (file3.exists() && file3.isDirectory()) {
            return xtcDataSlogDir;
        }
        return sdCardLogDir;
    }

    /** 系统日志目录下各文件夹的公共前缀拼装结果。 */
    public static String getSystemLogDirPrefix() {
        return getSystemLogDir() + LOG_FOLDER_PREFIX;
    }

    /** tcpdump 抓包目录。 */
    public static String getTcpDumpFilePath() {
        return getSdCardPath() + File.separator + "tcpdump" + File.separator;
    }

    /** tcpdump 打包目录。 */
    public static String getTcpDumpZipFilePath() {
        return getSdCardPath() + File.separator + "tcp_dump_zip" + File.separator;
    }

    /** 外部存储总字节数。 */
    public static long getTotalSize() {
        StatFs statFs = new StatFs(getSdCardPath());
        return statFs.getBlockSizeLong() * statFs.getBlockCountLong();
    }

    /** 递归统计目录（或单个文件）占用的字节数。 */
    public static long getTotalSizeOfFilesInDir(File file) {
        if (file.isFile()) {
            return file.length();
        }
        File[] children = file.listFiles();
        if (children == null) {
            Log.d(TAG, "children is null");
            return 0L;
        }
        long total = 0L;
        for (File child : children) {
            total += getTotalSizeOfFilesInDir(child);
            Log.d(TAG, "total: " + total);
        }
        return total;
    }

    /** wifi 日志目录。 */
    public static String getWifiLogFilePath() {
        return getSdCardPath() + File.separator + "wlan_logs" + File.separator;
    }

    /** wifi 日志打包目录。 */
    public static String getWifiLogZipFilePath() {
        return getSdCardPath() + File.separator + "wifi_log_zip" + File.separator;
    }

    private static String getXtcDataLogDir() {
        return getXtcDataPath() + File.separator + "Logs_Collector" + File.separator;
    }

    private static String getXtcDataPath() {
        return XTC_DATA_PATH;
    }

    private static String getXtcDataSlogDir() {
        return getXtcDataPath() + File.separator + "slog" + File.separator;
    }

    /** 外部存储是否处于 mounted 状态。 */
    public static boolean isAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}
