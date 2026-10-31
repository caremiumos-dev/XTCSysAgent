package com.xtc.dns.client;

/**
 * client 子模块的日志 tag 工厂（原厂 {@code com.xtc.dns.client.d}）。
 *
 * <p>与 {@code com.xtc.dns.DnsLogTag} 一样用短横线连接后缀，但前缀是
 * {@code HttpDnsClient}，用于把"客户端侧（读缓存）"和"服务侧（写缓存）"的日志分开。</p>
 */
public class ClientLogTag {

    /** 根 tag。 */
    public static String clientTag() {
        return "HttpDnsClient";
    }

    /** 带后缀的 tag。 */
    public static String clientTag(String suffix) {
        return "HttpDnsClient-" + suffix;
    }
}
