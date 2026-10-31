package com.xtc.utils.system;

import android.content.Context;
import android.text.TextUtils;
import java.util.Locale;

/**
 * 系统语言读取。
 */
public final class LanguageUtils {

    private LanguageUtils() {
        throw new UnsupportedOperationException("You can't instantiate SystemLanguageUtils");
    }

    /**
     * 取当前语言，形如 {@code zh-CN}；没有国家码时只返回语言，如 {@code zh}。
     */
    public static String getLanguage(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        String country = locale.getCountry();
        if (TextUtils.isEmpty(country)) {
            return language;
        }
        return language + "-" + country;
    }
}
