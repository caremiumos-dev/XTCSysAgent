package com.xtc.dns.client.contract;

/**
 * 客户端访问 HttpDns ContentProvider 的契约（原厂 {@code com.xtc.dns.client.contract.a}）。
 *
 * <p>这里定义的路径段与列名是**跨进程协议**：本类（读取方）与
 * {@code com.xtc.dns.provider.HttpDnsContentProvider}（提供方）必须逐字一致，
 * 所以两侧都引用常量，避免手写字符串写岔。</p>
 *
 * <p>注意本类里的路径段**不带前导斜杠**，而 provider 侧匹配用的路径**带**前导斜杠，
 * 这是原厂就有的区别（一个走 {@code Uri.Builder#appendPath}，一个走
 * {@code Uri#getPath} 的前缀比较），不能统一。</p>
 */
public interface DnsProviderContract {

    /** ContentProvider authority 的拼装模板，参数是宿主包名。 */
    String AUTHORITY_FORMAT = "%s.com.xtc.httpdns.provider";

    /** 查询某域名缓存的路径段。 */
    String PATH_DOMAIN_MODEL = "DomainModel";

    /** 通知"某个 ip 已经不可用"的路径段。 */
    String PATH_CHECK_IP_EXPIRE = "checkIpExpire";

    /** 通知"检查过期缓存"的路径段。 */
    String PATH_CHECK_IP_OVER_TIME = "checkIpOverTime";

    /** 降低某 ip 权重的路径段。 */
    String PATH_DECREASE_IP_PRIORITY = "decreaseIpPriority";

    /** 清空缓存的路径段。 */
    String PATH_CLEAR = "clear";

    /** 开关相关的键。 */
    interface Switch {

        /** provider 里记录 HttpDns 开关的键。 */
        String IS_HTTP_DNS_ENABLE = "isHttpDnsEnable";
    }

    /** provider 返回游标的列名。 */
    interface Column {

        /** 主键。 */
        String ID = "id";

        /** 域名。 */
        String DOMAIN = "domain";

        /** 记录创建时间。 */
        String CREATE_TIME = "createTime";

        /** 域名级 ttl（秒）。 */
        String DOMAIN_TTL = "domainTTL";

        /** ip。 */
        String IP = "ip";

        /** ip 级 ttl（秒）。 */
        String IP_TTL = "ipTTL";

        /** 优先级。 */
        String PRIORITY = "priority";

        /** 来源标记。 */
        String SP = "sp";
    }
}
