package com.xtc.dns.service;

import com.xtc.dns.storage.db.DomainModel;
import java.util.List;

/**
 * HttpDns 的服务侧能力（原厂 {@code com.xtc.dns.service.c}）。
 *
 * <p>这些方法全部由 {@code HttpDnsContentProvider} 通过 {@code getType} 的 URI 路径触发，
 * 也就是说**它们是跨进程调用的入口**：调用方（client 侧）是 fire-and-forget，
 * 所以这里的返回值和异常都不能指望被看到，实现里必须自己把异常吞掉。</p>
 */
public interface DnsService {

    /**
     * 查某域名的缓存；未命中时会顺带触发一次后台刷新（不阻塞）。
     *
     * @return 缓存记录；没有则返回 null
     */
    List<DomainModel> getDomainModel(String host);

    /** 检查并清理过期缓存。 */
    void checkIpOverTime();

    /**
     * 上报一个不可用的 ip：若该记录已过期则触发重解析。
     *
     * @param host 域名
     * @param ip   不可用的 ip
     */
    void checkIpExpire(String host, String ip);

    /** 清空缓存。 */
    void clear();

    /** 降权某个 ip（连接失败时调用）。 */
    void decreasePriority(String ip);
}
