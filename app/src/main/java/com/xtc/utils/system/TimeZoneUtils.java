package com.xtc.utils.system;

import android.text.TextUtils;
import java.util.TimeZone;

/**
 * 时区显示名。
 */
public final class TimeZoneUtils {

    private TimeZoneUtils() {
        throw new UnsupportedOperationException("You can't instantiate SystemTimeUtils");
    }

    /**
     * 优先用系统提供的显示名；如果显示名里有不可打印字符，则按 GMT 偏移自己拼。
     */
    public static String getTimeZoneDisplayName() {
        String displayName = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT);
        if (isPrintable(displayName)) {
            return displayName;
        }
        return formatGmtOffset(TimeZone.getDefault().getRawOffset());
    }

    /** 把毫秒偏移拼成 {@code GMT+08:00} 形式。 */
    private static String formatGmtOffset(int rawOffsetMillis) {
        int totalMinutes = rawOffsetMillis / 60000;
        char sign = '+';
        if (totalMinutes < 0) {
            sign = '-';
            totalMinutes = -totalMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        builder.append("GMT");
        builder.append(sign);
        appendPadded(builder, 2, totalMinutes / 60);
        builder.append(':');
        appendPadded(builder, 2, totalMinutes % 60);
        return builder.toString();
    }

    private static void appendPadded(StringBuilder builder, int width, int value) {
        String text = Integer.toString(value);
        for (int i = 0; i < width - text.length(); i++) {
            builder.append('0');
        }
        builder.append(text);
    }

    /** 是否全是可打印 ASCII（允许制表符）。 */
    private static boolean isPrintable(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if ((c <= 31 && c != '\t') || c >= 127) {
                return false;
            }
        }
        return true;
    }
}
