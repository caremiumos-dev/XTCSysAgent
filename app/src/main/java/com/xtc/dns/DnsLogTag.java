package com.xtc.dns;

/**
 * DNS 模块的日志 tag 工厂（原厂 {@code com.xtc.dns.event}）。
 *
 * <p>整个 DNS 模块的 tag 都是 {@code HttpDnsServer} 加上子模块后缀，集中在这里拼装，
 * 方便在 logcat 里按前缀过滤整条 DNS 链路。</p>
 *
 * <p>注意后缀是用短横线连接的。{@code com.xtc.dns.api} 和 {@code com.xtc.dns.client}
 * 里还各有一份同族工厂，但它们是"直接拼接、没有短横线"的写法——这是原厂就存在的不一致，
 * 不能顺手"修正"，否则日志 tag 就和线上日志对不上了。</p>
 */
public class DnsLogTag {

    /** 根 tag：没有子模块后缀时使用。 */
    public static String serverTag() {
        return "HttpDnsServer";
    }

    /**
     * 带子模块后缀的 tag。
     *
     * @param suffix 子模块名，例如 {@code DnsConfig}
     */
    public static String serverTag(String suffix) {
        return "HttpDnsServer-" + suffix;
    }
}
