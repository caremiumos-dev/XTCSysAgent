package com.xtc.dns.api;

/**
 * API 子模块的日志 tag 工厂（原厂 {@code com.xtc.dns.api.c}）。
 *
 * <p>与 {@code com.xtc.dns.DnsLogTag} 的差别只在拼接方式：这里后缀是**直接拼接**，
 * 中间没有短横线（例如 {@code apiTag("DnsStrategy")} 得到 {@code HttpDnsApiDnsStrategy}）。
 * 原厂就是如此，必须保持，否则日志 tag 与线上不一致。</p>
 */
public class ApiLogTag {

    /** 根 tag。 */
    public static String apiTag() {
        return "HttpDnsApi";
    }

    /** 带后缀的 tag（无分隔符）。 */
    public static String apiTag(String suffix) {
        return apiTag() + suffix;
    }
}
