package com.xtc.im.core.common;

/**
 * IM Core 的日志 tag 工厂。所有 IM 相关日志都以 {@code IM-Core} 打头，
 * 这样 xlog 里按前缀就能把整个 IM 模块的日志捞出来。
 *
 * <p>原厂类名为单字母 {@code a}。</p>
 */
public class ImLogTag {

    /**
     * 模块级 tag。
     */
    public static String tag() {
        return "IM-Core";
    }

    /**
     * 子模块 tag，形如 {@code IM-Core-PushRequest}。
     *
     * @param name 子模块名
     */
    public static String tag(String name) {
        return "IM-Core-" + name;
    }
}
