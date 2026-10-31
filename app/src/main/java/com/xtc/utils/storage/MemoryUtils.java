package com.xtc.utils.storage;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 内存信息读取工具。
 *
 * <p>两个入口：{@link #getTotalMemory()} 直接读 {@code /proc/meminfo} 的 MemTotal，
 * 得到物理内存总量（Linux 只暴露 KB，所以乘 1024 换成字节）；
 * {@link #getAvailableMemory(Context)} 走 ActivityManager 取当前可用内存。</p>
 *
 * <p>注意：原厂没有把构造函数私有化，本类仍然可以被实例化，这里保留该行为。</p>
 */
public class MemoryUtils {

    /** 日志/单位换算用不到常量类，PlaybackStateCompat 的 1024 即原厂使用的乘数。 */

    /**
     * 读取系统物理内存总量（字节）。
     *
     * <p>解析方式很原始：取 /proc/meminfo 的第一行 "MemTotal:  xxxx kB"，
     * 截取到 MemTotal: 之后的部分，用正则去掉所有非数字字符再 parse。
     * 任何一步失败（文件不存在、权限不足、解析异常）都只打印堆栈并返回 0，
     * 不做兜底估算——保持原厂行为。</p>
     *
     * @return 内存总字节数；读取失败返回 0
     */
    public static long getTotalMemory() {
        long total = 0L;
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader("/proc/meminfo");
            reader = new BufferedReader(fileReader, 2048);
            String line = reader.readLine();
            if (!TextUtils.isEmpty(line)) {
                // 原厂用 PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID(==1024) 当单位换算因子。
                total = ((long) Integer.parseInt(line.substring(line.indexOf("MemTotal:")).replaceAll("\\D+", "")))
                        * PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 原厂手写的两段 close，且两个 close 在同一个 try 里：
            // 前一个抛 IOException 时后一个就不会执行了，这里连同异常吞没位置一起保留。
            try {
                if (reader != null) {
                    reader.close();
                }
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return total;
    }

    /**
     * 取当前进程的可用内存（字节）。
     *
     * <p>用的是系统服务的全局可用内存，而不是本进程的 PSS，原厂即如此。</p>
     */
    public static long getAvailableMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }
}
