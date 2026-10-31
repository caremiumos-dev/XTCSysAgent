package com.xtc.utils.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期格式化 / 解析工具。全部基于 {@link SimpleDateFormat} 的紧绑定模式，
 * 调用点自己传入 pattern 字符串。
 */
public class DateUtils {

    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_YEAR_MONTH_DAY = "yyyyMMdd";
    public static final String PATTERN_MONTH_DAY = "MM-dd";
    public static final String PATTERN_TIME = "HH:mm:ss";
    public static final String PATTERN_HOUR_MINUTE = "HH:mm";
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    /**
     * 按给定 pattern 解析时间字符串。
     *
     * @return 毫秒时间戳；解析失败时返回 0（原实现如此，不抛异常）
     */
    public static long parse(String text, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(text).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /** 当前时间的 {@code yyyy-MM-dd HH:mm:ss} 文本。 */
    public static String formatDateTime() {
        return format(PATTERN_DATETIME);
    }

    /** 把时间戳格式化成 {@code MM-dd}。 */
    public static String formatMonthDay(long millis) {
        return format(PATTERN_MONTH_DAY, millis);
    }

    /** 按 pattern 格式化当前时间。 */
    public static String format(String pattern) {
        return format(pattern, new Date());
    }

    /** 按 pattern 格式化指定时间戳。 */
    public static String format(String pattern, long millis) {
        return format(pattern, new Date(millis));
    }

    /** 按 pattern 格式化指定时间戳（包装类型重载，避免调用点拆箱）。 */
    public static String format(String pattern, Long millis) {
        return format(pattern, millis.longValue());
    }

    /** 按 pattern 格式化指定时间。 */
    public static String format(String pattern, Date date) {
        return new SimpleDateFormat(pattern).format(date);
    }
}
