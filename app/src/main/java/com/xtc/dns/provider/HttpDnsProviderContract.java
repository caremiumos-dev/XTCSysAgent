package com.xtc.dns.provider;

/**
 * HttpDns ContentProvider 对外暴露的 Uri 与列名契约
 * （原厂 {@code com.xtc.dns.provider.a}）。
 *
 * <p>这里的字符串是**跨进程协议**：{@code com.xtc.dns.client.contract.DnsResolver}
 * 按这里的路径拼 Uri，本类的 {@code getType} 按同样的路径分发动作，
 * 两侧必须逐字一致。</p>
 *
 * <p>注意本类的路径**带前导斜杠**，因为 {@code getType} 拿到的是
 * {@code Uri#getPath()}（以 {@code /} 开头）；而客户端侧拼 Uri 用的是
 * {@code appendPath}，所以那边不带斜杠。两个包各有一套常量，不能合并。</p>
 */
public interface HttpDnsProviderContract {

    /** authority 模板，参数是宿主包名；实际值是 {@code <包名>.com.xtc.httpdns.provider}。 */
    String AUTHORITY_FORMAT = "%s.com.xtc.httpdns.provider";

    /** UriMatcher 里注册的查询路径（带通配，匹配 {@code DomainModel/<域名>}）。 */
    String MATCH_PATH_DOMAIN_MODEL = "DomainModel/*";

    /** 动作：上报某 ip 不可用，参数 host/ip。 */
    String PATH_CHECK_IP_EXPIRE = "/checkIpExpire";

    /** 动作：检查过期缓存，可选带一个 ip。 */
    String PATH_CHECK_IP_OVER_TIME = "/checkIpOverTime";

    /** 动作：降低某 ip 的优先级。 */
    String PATH_DECREASE_IP_PRIORITY = "/decreaseIpPriority";

    /** 动作：清空缓存。 */
    String PATH_CLEAR = "/clear";

    /** 开关相关的键。 */
    interface Switch {

        /** provider 里记录 HttpDns 开关的键（原厂预留，未被读取）。 */
        String IS_HTTP_DNS_ENABLE = "isHttpDnsEnable";
    }

    /** 游标列名：与 query 返回的 MatrixCursor 列一一对应，顺序无关但名字必须一致。 */
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
