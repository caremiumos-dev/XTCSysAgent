package com.xtc.httplib;

/**
 * HTTP 库的日志 tag 生成器。所有日志都以 {@code HTTP-} 开头，便于过滤。
 */
public class LogTag {

    private LogTag() {
    }

    public static String tag() {
        return "HTTP";
    }

    public static String tag(String module) {
        return "HTTP-" + module;
    }
}
