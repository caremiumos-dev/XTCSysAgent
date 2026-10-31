package com.xtc.dns.service.provider;

import android.text.TextUtils;
import com.xtc.dns.DnsLogTag;
import com.xtc.dns.info.HttpDnsResult;
import com.xtc.dns.service.DnsConfig;
import com.xtc.dns.http.HttpEngine;
import com.xtc.dns.http.HttpUrlConnectionEngine;
import com.xtc.dns.storage.db.DomainModel;
import com.xtc.dns.config.DesEncryptUtil;
import com.xtc.log.LogUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯 HttpDns 的 provider（原厂 {@code com.xtc.dns.service.provider.c}）。
 *
 * <p>协议：url = 前缀 + DES(域名) + {@code &id=<appId>&ttl=1}，
 * 响应体是一段 DES 密文，明文形如 {@code ip1;ip2,ttl}。所以这里要先解密再按
 * 逗号/分号拆包。</p>
 *
 * <p>解析响应时所有格式异常（数组越界、ttl 不是数字）都**静默**返回 null，
 * 不打日志——因为线上 DNS 服务偶尔会返回非预期内容，刷日志没有意义。
 * 这是原厂的选择，保持一致。</p>
 */
public class TencentDnsProvider implements DnsProvider {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("TencentHttpDnsProvider");

    /** 新记录的初始优先级。 */
    private static final int DEFAULT_PRIORITY = 0;

    /** 结果来源标记：0 表示腾讯 HttpDns。 */
    private static final int SOURCE_TENCENT = 0;

    /** 来源名，用于上报。 */
    private static final String SOURCE_NAME = "tencent";

    /** 协议的固定尾巴，{@code ttl=1} 表示要求服务端带上 ttl。 */
    private static final String REQUEST_TAIL_ID = "&id=";
    private static final String REQUEST_TAIL_TTL = "&ttl=1";

    /** HTTP 客户端（只在 DNS 解析阶段用，避免递归）。 */
    private final HttpEngine httpEngine = new HttpUrlConnectionEngine();

    /** 加密域名（腾讯协议要求）。 */
    private String encryptDomain(String domain) {
        return DesEncryptUtil.encrypt(domain);
    }

    /** 取 id 参数，每次都从配置读，保证配置热更新后立即生效。 */
    private String getAppId() {
        return DnsConfig.appId;
    }

    @Override
    public HttpDnsResult lookup(String domain) {
        String url = getBaseUrl() + encryptDomain(domain) + REQUEST_TAIL_ID + getAppId() + REQUEST_TAIL_TTL;
        LogUtil.i(TAG, "请求Dns开始，访问TencentHttpDns,domain = " + domain + ",url = " + url);
        String encryptedBody = this.httpEngine.get(url);
        if (TextUtils.isEmpty(encryptedBody)) {
            return null;
        }
        String plainBody = DesEncryptUtil.decrypt(encryptedBody);
        LogUtil.i(TAG, "请求Dns结束，访问TencentHttpDns,domain = " + domain + ",result = " + plainBody);
        if (TextUtils.isEmpty(plainBody)) {
            return null;
        }
        HttpDnsResult result = new HttpDnsResult();
        try {
            String[] sections = plainBody.split(",");
            String[] ipArray = sections[0].split(";");
            String ttl = sections[1];
            result.setIps(plainBody);
            result.setDomain(domain);
            result.setSource(SOURCE_TENCENT);
            long createTime = System.currentTimeMillis();
            List<DomainModel> models = new ArrayList<DomainModel>();
            for (String ip : ipArray) {
                DomainModel model = new DomainModel();
                model.setDomain(domain);
                model.setIp(ip);
                model.setPriority(DEFAULT_PRIORITY);
                model.setIpTtl(Long.valueOf(ttl).longValue());
                model.setDomainTtl(DnsConfig.dnsTtl);
                model.setCreateTime(createTime);
                models.add(model);
            }
            result.setModels(models);
        } catch (Exception e) {
            // 格式异常（没有逗号分段、ttl 不是数字等）→ 整份结果作废，静默返回 null。
            result = null;
        }
        return result;
    }

    @Override
    public boolean isAvailable() {
        return DnsConfig.tencentAvailable;
    }

    @Override
    public String getBaseUrl() {
        return DnsConfig.tencentRequestUrl;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }
}
