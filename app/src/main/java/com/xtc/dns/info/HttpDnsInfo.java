package com.xtc.dns.info;

/**
 * 一次 HttpDns 配置下发的快照（原厂 {@code com.xtc.dns.info.a}）。
 *
 * <p>四项配置分别对应服务端 web 配置里的 {@code cacheTime} / {@code enableHttpDns} /
 * {@code httpDnsIp} / {@code dnsTtl}。{@code DnsConfig#apply} 读它、
 * {@code DnsConfig#loadFromConfigFile} 写它。</p>
 *
 * <p>字段刻意用包装类型而不是基本类型：原厂用 {@code null} 表示"这一项没有下发"，
 * 而调用方（{@code DnsConfig#apply}）是直接 {@code intValue()} 的，
 * 所以传 null 会 NPE——这属于原厂行为，保持一致，不做"防御性修正"。</p>
 */
public class HttpDnsInfo {

    /** 最大缓存时间（秒）。 */
    private Integer cacheTime;

    /** HttpDns 开关：1 开，0 关。 */
    private Integer dnsSwitch;

    /** 腾讯 HttpDns 的服务 ip。 */
    private String ip;

    /** 缓存 ttl（秒）。 */
    private Integer ttl;

    public Integer getCacheTime() {
        return this.cacheTime;
    }

    public void setCacheTime(Integer cacheTime) {
        this.cacheTime = cacheTime;
    }

    public Integer getDnsSwitch() {
        return this.dnsSwitch;
    }

    public void setDnsSwitch(Integer dnsSwitch) {
        this.dnsSwitch = dnsSwitch;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getTtl() {
        return this.ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    /** 仅用于日志排错，格式与原厂逐字节一致。 */
    @Override
    public String toString() {
        return "HttpDnsInfo{cacheTime=" + this.cacheTime + ", dnsSwitch=" + this.dnsSwitch
                + ", ip='" + this.ip + "', ttl=" + this.ttl + '}';
    }
}
