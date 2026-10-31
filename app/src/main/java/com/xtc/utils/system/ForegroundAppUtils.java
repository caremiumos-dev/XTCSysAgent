package com.xtc.utils.system;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 前台应用查询工具。
 *
 * <p>原厂类名为单字母 {@code i}。Android 每个版本拿到"当前前台应用"的姿势都不同，
 * 原厂按版本分了三路，这里把分支照搬：</p>
 * <ul>
 *   <li>API &lt; 21：{@code getRunningTasks(1)} 取栈顶 Activity 的包名；</li>
 *   <li>API 21：{@code getRunningAppProcesses()} 里 importance == FOREGROUND 的进程名；</li>
 *   <li>API 22+：{@code UsageStatsManager} 查最近一天的使用记录，取 {@code lastTimeUsed} 最大的那条。</li>
 * </ul>
 *
 * <p>注意 API 22+ 的用法：{@code queryUsageStats(2, ...)} 里的 2 是
 * {@code INTERVAL_MONTHLY}（不是 DAILY，DAILY 是 0），原件就传 2，不能改 ——
 * 改成 DAILY 会改变返回集合，进而改变"谁在前台"的判断。</p>
 */
public class ForegroundAppUtils {

    /** 上一次查到的、{@code lastTimeUsed} 最大的使用记录，跨调用复用。 */
    private static UsageStats sLastUsedAppStats;

    /** 事件流方式查到的最后一个切前台包名，跨调用复用。 */
    private static String sLastEventPackageName;

    private ForegroundAppUtils() {
    }

    /**
     * 本应用是否在前台。
     *
     * <p>判据：存在 importance == FOREGROUND 的进程，且它的 pkgList 里含本应用包名。</p>
     */
    public static boolean isAppForeground(Context context) {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses =
                ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
        if (runningAppProcesses == null || runningAppProcesses.size() == 0) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (runningAppProcessInfo.importance == 100
                    && Arrays.asList(runningAppProcessInfo.pkgList).contains(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /** 指定包名是否就是当前前台应用。 */
    public static boolean isForeground(Context context, String packageName) {
        return TextUtils.equals(packageName, getForegroundPackage(context));
    }

    /** 当前前台应用包名，按系统版本走不同实现；取不到返回 null。 */
    public static String getForegroundPackage(Context context) {
        if (Build.VERSION.SDK_INT < 21) {
            return getForegroundPackageByTasks(context);
        }
        return Build.VERSION.SDK_INT < 22
                ? getForegroundPackageByProcess(context)
                : getForegroundPackageByUsageStats(context);
    }

    /**
     * API 22+：查最近一天的使用统计，取最后使用时间最大的那条的包名。
     *
     * <p>查询区间是"当前时间往前一天"；结果集为空返回 null。命中的记录会缓存到
     * {@link #sLastUsedAppStats} 用于下次比较，原件就是这种"只记录、不清空"的写法。</p>
     */
    @TargetApi(22)
    public static String getForegroundPackageByUsageStats(Context context) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        long endTime = calendar.getTimeInMillis();
        // 原件写死 5，即 Calendar.DAY_OF_MONTH。
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        List<UsageStats> usageStatsList =
                usageStatsManager.queryUsageStats(2, calendar.getTimeInMillis(), endTime);
        if (usageStatsList == null || usageStatsList.size() == 0) {
            return null;
        }
        for (UsageStats usageStats : usageStatsList) {
            if (sLastUsedAppStats == null || usageStats.getLastTimeUsed() > sLastUsedAppStats.getLastTimeUsed()) {
                sLastUsedAppStats = usageStats;
            }
        }
        return sLastUsedAppStats.getPackageName();
    }

    /** API &lt; 21：取任务栈顶 Activity 的包名。 */
    @TargetApi(21)
    private static String getForegroundPackageByTasks(Context context) {
        List<ActivityManager.RunningTaskInfo> runningTasks =
                ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1);
        if (runningTasks == null || runningTasks.size() == 0 || runningTasks.get(0) == null) {
            return null;
        }
        return runningTasks.get(0).topActivity.getPackageName();
    }

    /** API 21：取 importance == FOREGROUND 的进程名（注意是进程名，不是包名）。 */
    @TargetApi(22)
    private static String getForegroundPackageByProcess(Context context) {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses =
                ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
        if (runningAppProcesses == null || runningAppProcesses.size() == 0) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (runningAppProcessInfo.importance == 100) {
                return runningAppProcessInfo.processName;
            }
        }
        return null;
    }

    /**
     * API 22+：从最近 10 秒的使用事件流里找最后一个"切到前台"的包名。
     *
     * <p>原件里这个方法没有调用点（{@link #getForegroundPackage(Context)} 走的是 UsageStats 那条路），
     * 但作为备用实现保留；事件类型 1 即 {@code MOVE_TO_FOREGROUND}。</p>
     */
    @TargetApi(22)
    private static String getForegroundPackageByEvents(Context context) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        long now = System.currentTimeMillis();
        UsageEvents.Event event = new UsageEvents.Event();
        UsageEvents usageEvents = usageStatsManager.queryEvents(now - 10000, now);
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == 1) {
                sLastEventPackageName = event.getPackageName();
            }
        }
        return sLastEventPackageName;
    }
}
