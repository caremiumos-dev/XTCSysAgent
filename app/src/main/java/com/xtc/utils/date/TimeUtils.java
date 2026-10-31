package com.xtc.utils.date;

import android.app.AlarmManager;
import android.content.Context;
import android.os.SystemClock;
import java.util.Calendar;
import java.util.Date;

/**
 * 开机时长与系统时间的换算工具。
 *
 * <p>手表上没有可靠的 RTC，系统时间可能在联网后被校准，因此这里保存一个
 * "开机时刻 + 当时墙上时间"的快照，用 {@link SystemClock#elapsedRealtime()}
 * 推算出当前时间，保证在系统时间被调整时依旧单调。</p>
 */
public class TimeUtils {

    /** 快照时刻的 elapsedRealtime。 */
    private static long sElapsedAtSync;
    /** 快照时刻的墙上时间。 */
    private static Date sWallClockAtSync;
    /** 已累计的时区偏移补偿，0 表示尚未计算。 */
    private static long sOffsetAccumulated = 0;

    private TimeUtils() {
    }

    /** 距离上次快照经过的毫秒数。 */
    public static long getElapsedSinceSync() {
        return SystemClock.elapsedRealtime() - sElapsedAtSync;
    }

    /** 重新采样一次系统时间快照。 */
    public static void sync(Context context) {
        takeSnapshot();
    }

    /**
     * 用给定的系统时间去设置时钟，并按已累计的偏移补偿。
     *
     * <p>原实现标注为废弃且当前没有任何调用点，保留原样以免行为差异。</p>
     */
    @Deprecated
    private static void applySystemTime(Context context, Long millis) {
        if (sOffsetAccumulated == 0) {
            sOffsetAccumulated = SystemClock.elapsedRealtime() - sElapsedAtSync;
        }
        // 超过 Integer.MAX_VALUE 秒的时间戳会被 AlarmManager 拒绝，直接跳过。
        if (millis == null || millis.longValue() / 1000 >= Integer.MAX_VALUE) {
            return;
        }
        sWallClockAtSync = new Date(millis.longValue() + (sOffsetAccumulated / 2));
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(millis.longValue());
    }

    /** 当前系统墙上时间。 */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /** 当前日历实例。 */
    public static Calendar calendar() {
        return Calendar.getInstance();
    }

    /**
     * 基于快照推算的当前时间。
     *
     * <p>原实现标注为废弃，保留以兼容既有调用。</p>
     */
    @Deprecated
    public static Date getCurrentDate() {
        if (sWallClockAtSync == null) {
            takeSnapshot();
        }
        return new Date(sWallClockAtSync.getTime() + getElapsedSinceSync());
    }

    /** 记录一次"开机时刻 / 墙上时间"快照。 */
    private static void takeSnapshot() {
        sElapsedAtSync = SystemClock.elapsedRealtime();
        sWallClockAtSync = new Date();
    }
}
