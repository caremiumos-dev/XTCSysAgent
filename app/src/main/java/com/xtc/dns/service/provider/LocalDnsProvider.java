package com.xtc.dns.service.provider;

import com.xtc.dns.DnsLogTag;
import com.xtc.dns.info.HttpDnsResult;
import com.xtc.dns.service.DnsConfig;
import com.xtc.dns.storage.db.DomainModel;
import com.xtc.log.LogUtil;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 用系统的 {@code InetAddress.getAllByName} 兜底的 provider（原厂 {@code com.xtc.dns.service.provider.b}）。
 *
 * <p>它永远可用，是整条链路的最后一道保险：腾讯接口不通时至少还能用运营商 DNS。</p>
 *
 * <p>写进缓存的 ttl 是两级：ip 级固定 60 秒（本地解析结果变化快），
 * 域名级用配置里的 {@code dnsTtl}。</p>
 */
public class LocalDnsProvider implements DnsProvider {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("LocalDnsProvider");

    /** 本地解析结果的 ip 级 ttl（秒）。 */
    private static final long LOCAL_IP_TTL = 60L;

    /** 新记录的初始优先级。 */
    private static final int DEFAULT_PRIORITY = 0;

    /** 结果来源标记：1 表示本地。 */
    private static final int SOURCE_LOCAL = 1;

    /** 来源名，用于上报。 */
    private static final String SOURCE_NAME = "local";

    @Override
    public HttpDnsResult lookup(String domain) {
        try {
            LogUtil.i(TAG, "请求Dns开始，LocalDns，domain = " + domain);
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            String[] ips = new String[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                ips[i] = addresses[i].getHostAddress();
            }
            if (ips.length > 0) {
                HttpDnsResult result = new HttpDnsResult();
                result.setDomain(domain);
                result.setSource(SOURCE_LOCAL);
                String ipString = "";
                long createTime = System.currentTimeMillis();
                List<DomainModel> models = new ArrayList<DomainModel>();
                for (int i = 0; i < ips.length; i++) {
                    String ip = ips[i];
                    // 最后一个 ip 后面不补逗号：原厂的拼接方式（用 length + (-1) 比较）。
                    ipString = i == ips.length + (-1) ? ipString + ip : ipString + ip + ",";
                    DomainModel model = new DomainModel();
                    model.setDomain(domain);
                    model.setIp(ip);
                    model.setPriority(DEFAULT_PRIORITY);
                    model.setIpTtl(LOCAL_IP_TTL);
                    model.setDomainTtl(DnsConfig.dnsTtl);
                    model.setCreateTime(createTime);
                    models.add(model);
                }
                result.setModels(models);
                result.setIps(ipString);
                LogUtil.i(TAG, "请求Dns结束，LocalDns，result  = " + result.toString());
                return result;
            }
        } catch (Exception e) {
            // 解析失败只记日志并返回 null，让上层继续走"没有缓存 → 系统解析"的路径。
            LogUtil.e(TAG, e);
        }
        return null;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getBaseUrl() {
        return null;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }
}
