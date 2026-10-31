package com.xtc.dns.info;

import com.xtc.dns.storage.db.DomainModel;
import java.util.List;

/**
 * 一次 HttpDns 解析的产物（原厂 {@code com.xtc.dns.info.b}）。
 *
 * <p>{@code LocalDnsProvider} 与 {@code TencentDnsProvider} 都返回它，
 * {@code DnsServiceImpl} 再把它交给 {@code DnsStorageServiceImpl#save} 落库。</p>
 *
 * <p>字段含义按调用点反推：{@code domain} 是查询的域名；{@code models} 是逐条 IP 记录；
 * {@code ips} 是"原样返回给上报的 ip 串"（腾讯分支是整包明文，本地分支是逗号拼串）；
 * {@code source} 是结果来源（0 腾讯 / 1 本地）。{@code code} 在包内没有任何写入点，
 * 按 HTTP 响应码推断命名，保留字段只为与序列化形态一致。</p>
 */
public class HttpDnsResult {

    /** 查询的域名。 */
    private String domain = "";

    /** 解析出的 IP 记录列表，未解析成功时为 null。 */
    private List<DomainModel> models = null;

    /** 响应码（包内无写入点）。 */
    private String code = "";

    /** 原样返回的 ip 串，用于行为上报的 {@code ips} 字段。 */
    private String ips;

    /** 结果来源：0 腾讯 HttpDns，1 本地系统解析。 */
    private int source;

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<DomainModel> getModels() {
        return this.models;
    }

    public void setModels(List<DomainModel> models) {
        this.models = models;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIps() {
        return this.ips;
    }

    public void setIps(String ips) {
        this.ips = ips;
    }

    public int getSource() {
        return this.source;
    }

    public void setSource(int source) {
        this.source = source;
    }
}
