package com.xtc.bigdata.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.xtc.log.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Scanner;

/**
 * CPU / 内存占用的读取。
 *
 * <p>全部依赖 {@code /proc} 与隐藏 API：{@code /proc/stat} 取整机 jiffies，
 * {@code /proc/<pid>/stat} 取进程 jiffies，{@code /proc/<pid>/task/} 取线程明细，
 * 内存则走 {@code ActivityManager.getProcessMemoryInfo} 加反射取 USS。</p>
 *
 * <p>这些方法都是 {@code PerformanceMonitor} 的采样源，输出必须尽量稳定，
 * 因此读取失败时一律"返回上一次/零值"而不是抛异常。</p>
 */
public class ResourceUsedUtil {

    /** 读缓冲大小。 */
    private static final int BUFFER_SIZE = 1000;

    private static final String TAG = "ResourceUsedUtil";

    private ResourceUsedUtil() {
    }

    /**
     * 读整机 CPU 时间片。
     *
     * @return 长度为 2 的数组：[0]=总 jiffies，[1]=idle jiffies；读失败返回 {0,0}
     */
    public static long[] getCpuUsedArrayOfSystem() {
        long[] cpuStat = {0, 0};
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("/proc/stat"));
            scanner.next();
            long user = scanner.nextLong();
            long nice = scanner.nextLong();
            long system = scanner.nextLong();
            long idle = scanner.nextLong();
            cpuStat[0] = user + nice + system + idle + scanner.nextLong() + scanner.nextLong() + scanner.nextLong();
            cpuStat[1] = idle;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return cpuStat;
    }

    /**
     * 读单个进程的 CPU 时间片。
     *
     * @param pid 进程号
     * @return 进程 jiffies；读失败返回 0
     */
    private static long getCpuUsedOfProc(int pid) {
        long cpuTime = 0;
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("/proc/" + pid + "/stat"));
            // 前 13 个字段是进程状态与命令行，跳过后才是时间片
            for (int index = 0; scanner.hasNext() && index < 13; index++) {
                scanner.next();
            }
            cpuTime = scanner.nextLong() + scanner.nextLong() + scanner.nextLong() + scanner.nextLong();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return cpuTime;
    }

    /**
     * 读进程下所有线程的 CPU 时间片明细。
     *
     * <p>输出格式是 {@code "tid:utime+stime:comm,"} 的串接，线程名里的
     * 逗号、冒号、换行分别替换成 {@code "@@@"}、{@code "%%%"}、空 ——
     * 因为下游按逗号切分、按冒号取值，不转义就会串行。</p>
     *
     * @param pid 进程号
     * @return 明细串；进程没有 task 目录时返回空串
     */
    public static String getCpuUsedOfProcThreads(int pid) {
        String result = "";
        File taskDir = new File("/proc/" + pid + "/task");
        if (taskDir.exists()) {
            File[] threadFiles = taskDir.listFiles();
            LogUtil.d(TAG, "threadFiles 个数 = " + threadFiles.length);
            for (File threadFile : threadFiles) {
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(
                            new InputStreamReader(new FileInputStream(new File(threadFile, "stat"))), BUFFER_SIZE);
                    String line = bufferedReader.readLine();
                    if (line != null) {
                        int nameStart = 0;
                        int nameEnd = 0;
                        for (int index = 0; index < line.length(); index++) {
                            if (line.charAt(index) == '(' && nameStart == 0) {
                                nameStart = index;
                            }
                            if (line.charAt(index) == ')' && index > nameEnd) {
                                nameEnd = index;
                            }
                        }
                        // "tid (comm) state ..." —— 去掉右括号后的空格再按空格切
                        String tid = line.substring(0, nameStart - 1);
                        String comm = line.substring(nameStart + 1, nameEnd);
                        String[] fields = line.substring(nameEnd + 2, line.length()).split(" ");
                        if (fields.length >= 17) {
                            result = new StringBuffer(result)
                                    .append(tid).append(":")
                                    .append(Long.parseLong(fields[11]) + Long.parseLong(fields[12]))
                                    .append(":")
                                    .append(comm.replace(",", "@@@").replace(":", "%%%").replace("\n", ""))
                                    .append(",")
                                    .toString();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 读单个进程的内存占用。
     *
     * @param pid     进程号
     * @param context 上下文，为 null 时直接返回 {0,0}
     * @return 长度为 2 的数组：[0]=PSS(KB)，[1]=USS(KB)；失败时保持零值
     */
    public static int[] getMemoryUsedOfProc(int pid, Context context) {
        int[] memory = {0, 0};
        if (context == null) {
            return memory;
        }
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
            assert activityManager != null;
            Debug.MemoryInfo memoryInfo = activityManager.getProcessMemoryInfo(new int[]{pid})[0];
            memory[0] = memoryInfo.getTotalPss();
            // USS 是隐藏字段，只能反射拿
            Method declaredMethod = memoryInfo.getClass().getDeclaredMethod("getTotalUss", new Class[0]);
            declaredMethod.setAccessible(true);
            memory[1] = ((Integer) declaredMethod.invoke(memoryInfo, new Object[0])).intValue();
            return memory;
        } catch (Exception e) {
            e.printStackTrace();
            return memory;
        }
    }
}
