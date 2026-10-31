package com.xtc.dns.storage;

import com.xtc.dns.info.HttpDnsResult;
import com.xtc.dns.storage.db.DomainModel;
import java.util.List;

/**
 * HttpDns 的本地存储服务（原厂 {@code com.xtc.dns.storage.b}）。
 *
 * <p>只被 {@code DnsServiceImpl} 使用：它负责"内存缓存 + 数据库 + 过期清理"，
 * 上层的解析策略（先腾讯、再本地）与网络判断都不在这里。</p>
 */
public interface DnsStorageService {

    /** 查某域名的缓存记录；没有则返回 null。 */
    List<DomainModel> queryByDomain(String host);

    /** 清空内存缓存与数据库。 */
    void clearAll();

    /** 删除创建时间早于 {@code time} 的记录（毫秒时间戳）。 */
    void deleteExpired(long time);

    /** 保存一次解析结果（先按域名删旧记录，再整批插入）。 */
    void save(HttpDnsResult result);

    /** 把某个 ip 的权重减一（连接失败时调用）。 */
    void decreasePriority(String ip);
}
