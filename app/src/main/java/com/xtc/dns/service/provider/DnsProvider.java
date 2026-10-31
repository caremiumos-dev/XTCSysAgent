package com.xtc.dns.service.provider;

import com.xtc.dns.info.HttpDnsResult;

/**
 * 一种 HttpDns 数据来源（原厂 {@code com.xtc.dns.service.provider.a}）。
 *
 * <p>实现有两个：{@code TencentDnsProvider}（在线查腾讯接口）和
 * {@code LocalDnsProvider}（用系统解析兜底）。{@code DnsServiceImpl} 按注册顺序
 * 依次询问，第一个返回非 null 的结果即采用。</p>
 */
public interface DnsProvider {

    /**
     * 解析域名。
     *
     * @return 解析结果；失败返回 null（由上层继续问下一个 provider）
     */
    HttpDnsResult lookup(String domain);

    /** 当前是否可以询问该 provider。 */
    boolean isAvailable();

    /** 该 provider 的请求前缀（只有腾讯需要，本地返回 null）。 */
    String getBaseUrl();

    /** 来源名，用于行为上报的 {@code source} 字段。 */
    String getSourceName();
}
