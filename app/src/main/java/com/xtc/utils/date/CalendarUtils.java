package com.xtc.utils.date;

import java.util.GregorianCalendar;

/**
 * 日历相关的取数工具。
 */
public class CalendarUtils {

    /**
     * 取指定年月的天数。
     *
     * <p>用 day-of-month = 0 的写法拿到"上个月最后一天"，也就是目标月的最后一天，
     * 因此闰年二月会自动返回 29。</p>
     *
     * @param year  年份（如 2024）
     * @param month 月份，从 0 开始（{@link java.util.Calendar} 的约定）
     */
    public static int getDaysOfMonth(int year, int month) {
        return new GregorianCalendar(year, month, 0).get(GregorianCalendar.DAY_OF_MONTH);
    }
}
