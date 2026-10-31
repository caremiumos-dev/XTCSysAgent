package com.xtc.utils.system.constant;

/**
 * 语言 + 地区的组合工具。
 */
public class LanguageRegionUtils {

    private LanguageRegionUtils() {
    }

    /** 拼成 {@code language_region}，例如 {@code zh_CN}。 */
    public static String combine(String language, String region) {
        return language + "_" + region;
    }
}
