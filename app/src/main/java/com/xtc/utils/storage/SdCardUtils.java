package com.xtc.utils.storage;

import android.annotation.TargetApi;
import android.os.Environment;
import android.os.StatFs;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * SD 卡/外部存储信息工具。
 *
 * <p>原厂这一层是"存储路径 + 容量"的查询库：判断对外部存储是否可用、
 * 给出外部存储与 data 分区的根路径、从 {@code /proc/mounts} 里反查真实挂载点，
 * 以及通过 {@link StatFs} 汇总一张容量信息表。</p>
 *
 * <p>工具类禁止实例化，构造即抛异常。</p>
 */
public class SdCardUtils {

    /** 外部存储不可用时统一返回的提示语（注意原厂用的是英文 + 感叹号）。 */
    private static final String MSG_SDCARD_UNABLE = "sdcard unable!";

    private SdCardUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /** 外部存储是否处于 mounted 状态。 */
    public static boolean isSdCardAvailable() {
        return "mounted".equals(Environment.getExternalStorageState());
    }

    /**
     * 数据分区根路径。
     *
     * <p>SD 卡不可用时返回提示语而不是路径，调用方需要自己判空/判等——
     * 原厂所有"路径"方法都是这个约定。</p>
     */
    public static String getDataDirectoryPath() {
        return !isSdCardAvailable() ? MSG_SDCARD_UNABLE : Environment.getDataDirectory().getPath() + File.separator;
    }

    /** 外部存储根路径（带结尾分隔符）。 */
    public static String getSdCardPath() {
        return !isSdCardAvailable() ? MSG_SDCARD_UNABLE : Environment.getExternalStorageDirectory().getPath() + File.separator;
    }

    /**
     * 从 {@code /proc/mounts} 反查外部存储的真实挂载点。
     *
     * <p>思路：读 mount 表，找同时包含 "sdcard" 与 ".android_secure" 的那一行，
     * 第二个字段就是挂载点，去掉 "/.android_secure" 后缀再补分隔符即为 SD 卡根目录。
     * 这是为了应付部分机型 {@code Environment.getExternalStorageDirectory()}
     * 返回的路径与实际挂载点不一致的老问题。</p>
     *
     * <p>失败路径原厂都给定了固定返回值：命令退出码为 1 时返回 " 命令执行失败"
     * （注意有个前导空格），读不到匹配行时回退到系统 API 给出的目录。</p>
     */
    public static String querySdCardPath() {
        if (!isSdCardAvailable()) {
            return MSG_SDCARD_UNABLE;
        }
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        try {
            Process process = runtime.exec("cat /proc/mounts");
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains("sdcard") && line.contains(".android_secure")) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 5) {
                        return parts[1].replace("/.android_secure", "") + File.separator;
                    }
                }
                // 原厂把 waitFor 放在循环里：每读一行都判一次进程状态，行为保持一致。
                if (process.waitFor() != 0 && process.exitValue() == 1) {
                    return " 命令执行失败";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeIO(reader);
        }
        return Environment.getExternalStorageDirectory().getPath() + File.separator;
    }

    /**
     * 外部存储可用空间，格式化成 "x.xxxGB" 之类的字符串。
     *
     * <p>需要 API 18（{@code getBlockSizeLong} 等），原厂直接标了 TargetApi(18)。</p>
     */
    @TargetApi(18)
    public static String getSdCardAvailableSize() {
        if (!isSdCardAvailable()) {
            return MSG_SDCARD_UNABLE;
        }
        StatFs statFs = new StatFs(getSdCardPath());
        return IOUtils.formatSize(statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong());
    }

    /**
     * 汇总外部存储的容量信息。
     *
     * <p>SD 卡不可用时返回的对象 {@code exists} 为 false、其余字段为 0，
     * 调用方只需判断 exists 即可。</p>
     */
    @TargetApi(18)
    public static SdCardInfo getSdCardInfo() {
        SdCardInfo info = new SdCardInfo();
        if ("mounted".equals(Environment.getExternalStorageState())) {
            info.exists = true;
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            info.totalBlocks = statFs.getBlockCountLong();
            info.blockByteSize = statFs.getBlockSizeLong();
            info.availableBlocks = statFs.getAvailableBlocksLong();
            info.availableBytes = statFs.getAvailableBytes();
            info.freeBlocks = statFs.getFreeBlocksLong();
            info.freeBytes = statFs.getFreeBytes();
            info.totalBytes = statFs.getTotalBytes();
        }
        return info;
    }

    /**
     * 外部存储的容量快照。
     *
     * <p>原来是个混淆成 {@code i$a} 的私有静态内部类，按用途还原成 SdCardInfo。
     * toString 的输出格式被日志/上报直接消费，字段顺序与文案都保持原样。</p>
     */
    private static class SdCardInfo {

        /** 是否存在可用的外部存储。 */
        boolean exists;
        /** 总块数。 */
        long totalBlocks;
        /** 空闲块数。 */
        long freeBlocks;
        /** 可用块数（已扣除系统保留）。 */
        long availableBlocks;
        /** 每块字节数。 */
        long blockByteSize;
        /** 总字节数。 */
        long totalBytes;
        /** 空闲字节数。 */
        long freeBytes;
        /** 可用字节数。 */
        long availableBytes;

        private SdCardInfo() {
        }

        @Override
        public String toString() {
            return "SDCardInfo{isExist=" + this.exists + ", totalBlocks=" + this.totalBlocks + ", freeBlocks=" + this.freeBlocks + ", availableBlocks=" + this.availableBlocks + ", blockByteSize=" + this.blockByteSize + ", totalBytes=" + this.totalBytes + ", freeBytes=" + this.freeBytes + ", availableBytes=" + this.availableBytes + '}';
        }
    }
}
