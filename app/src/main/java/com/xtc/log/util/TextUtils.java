package com.xtc.log.util;

/**
 * 极简文本工具，避免日志模块依赖 android.text.TextUtils。
 */
public final class TextUtils {

    private TextUtils() {
        throw new AssertionError();
    }

    public static boolean isEmpty(CharSequence text) {
        return text == null || text.length() == 0;
    }
}
