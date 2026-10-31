package com.xtc.systemagent.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具：字符串解析与"是否早于 N 天前"判断。
 */
public class DateUtil {

    private static final String TAG = "DateUtil";

    /**
     * 按指定格式解析日期字符串。
     *
     * @return 解析失败或入参为空时返回 null
     */
    @Nullable
    public static Date String2Date(String text, String pattern) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(pattern)) {
            return null;
        }
        try {
            return new SimpleDateFormat(pattern, Locale.getDefault()).parse(text);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断 {@code date} 是否早于 {@code reference} 之后 {@code days} 天的那一刻。
     *
     * @param date      待判断的时间
     * @param reference 基准时间（通常是"现在"）
     * @param days      偏移天数，可以是负数
     */
    public static boolean beforeDate(Date date, Date reference, int days) {
        if (date == null || reference == null) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(reference);
        // Calendar.DAY_OF_MONTH = 5
        calendar.add(Calendar.DAY_OF_MONTH, days);
        LogUtil.d(TAG, "beforeDate time: " + date + " ,now: " + reference
                + " ,nTime: " + calendar.getTime());
        return date.before(calendar.getTime());
    }
}
