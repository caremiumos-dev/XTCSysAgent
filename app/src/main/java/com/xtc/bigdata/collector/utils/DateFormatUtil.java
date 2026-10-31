package com.xtc.bigdata.collector.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期格式化（按需建 {@link SimpleDateFormat}，不做缓存）。
 *
 * <p>刻意不缓存 formatter：本类只在异常/DropBox 采集这种低频路径上用，
 * 而 {@code SimpleDateFormat} 不是线程安全的，缓存了反而要加锁。</p>
 */
public class DateFormatUtil {

    /** 完整时间格式。 */
    public static final String FORMAT_1 = "yyyy-MM-dd HH:mm:ss";

    /** 纯日期格式（用于按天建目录）。 */
    public static final String FORMAT_2 = "yyyyMMdd";

    private DateFormatUtil() {
    }

    /**
     * 用默认格式格式化当前时间。
     *
     * @return 形如 {@code 2024-01-01 10:00:00}
     */
    public static String format() {
        return format(FORMAT_1);
    }

    /**
     * 用指定格式格式化当前时间。
     *
     * @param pattern 格式
     * @return 格式化结果
     */
    public static String format(String pattern) {
        return format(pattern, new Date());
    }

    /**
     * 用指定格式格式化时间戳。
     *
     * @param pattern     格式
     * @param timestamp   毫秒时间戳
     * @return 格式化结果
     */
    public static String format(String pattern, long timestamp) {
        return format(pattern, new Date(timestamp));
    }

    /**
     * 用指定格式格式化时间。
     *
     * @param pattern 格式
     * @param date    时间
     * @return 格式化结果
     */
    public static String format(String pattern, Date date) {
        return new SimpleDateFormat(pattern).format(date);
    }
}
