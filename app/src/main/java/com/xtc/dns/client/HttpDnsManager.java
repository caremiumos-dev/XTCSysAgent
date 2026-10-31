package com.xtc.dns.client;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import com.xtc.dns.client.contract.DnsProviderContract;
import com.xtc.dns.client.contract.DnsResolver;
import com.xtc.log.LogUtil;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpDns 客户端门面：进程内单例，负责"选 provider + 内存缓存 + 转发写操作"
 * （原厂 {@code com.xtc.dns.client.c}）。
 *
 * <p>两层缓存：这里的内存 map（按域名缓存整份解析结果）和 provider 侧的数据库。
 * 命中内存就直接返回，避免每次都跨进程查 Cursor。</p>
 *
 * <p>单例用"静态实例 + 私有锁对象"的双检锁，锁对象是独立字段而不是 {@code Class}，
 * 与原厂一致；构造失败（context 为 null）时抛 RuntimeException 而不是静默降级，
 * 因为后续每一步都依赖它。</p>
 */
public class HttpDnsManager {

    /** 日志 tag。 */
    private static final String TAG = ClientLogTag.clientTag("HttpDnsManager");

    /** 单例实例。 */
    private static HttpDnsManager sInstance = null;

    /** 单例的锁对象。 */
    private static final Object LOCK = new Object();

    /** applicationContext。 */
    private final Context context;

    /** 内存缓存：域名 → 解析结果。 */
    private final Map<String, List<DomainInfo>> domainCache = new ConcurrentHashMap<String, List<DomainInfo>>();

    /** 当前使用的 provider 代理，构造时确定；volatile 保证跨线程可见。 */
    private volatile DnsResolver dnsResolver;

    private HttpDnsManager(Context context) {
        if (context == null) {
            throw new RuntimeException("HttpDnsManager init; context can not be null!!!");
        }
        this.context = context.getApplicationContext();
        initDnsResolver(this.context.getPackageName());
        checkIpOverTime();
    }

    /** 取单例。 */
    public static HttpDnsManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new HttpDnsManager(context);
                }
            }
        }
        return sInstance;
    }

    /** 让 provider 清一遍过期缓存。 */
    public void checkIpOverTime() {
        this.dnsResolver.checkIpOverTime();
    }

    /**
     * 绑定 provider：优先用传入包名自己的 provider，没有就回落到本包。
     *
     * <p>回落是必要的——系统应用之间会互相借用 HttpDns，借不到时必须用自己注册的那个，
     * 否则所有查询都会失败。</p>
     *
     * @param packageName 期望提供 HttpDns 数据的包名
     */
    public void initDnsResolver(String packageName) {
        PackageManager packageManager = this.context.getPackageManager();
        String authority = String.format(Locale.ROOT, DnsProviderContract.AUTHORITY_FORMAT, packageName);
        if (packageManager.resolveContentProvider(authority, 0) != null) {
            LogUtil.i(TAG, "Set DnsResolver: " + packageName);
            this.dnsResolver = new DnsResolver(this.context, authority);
        } else {
            LogUtil.w(TAG, "Can not resolve provider in application: " + packageName);
            this.dnsResolver = new DnsResolver(this.context, String.format(Locale.ROOT,
                    DnsProviderContract.AUTHORITY_FORMAT, this.context.getPackageName()));
        }
    }

    /**
     * 某个 ip 连不上：整份内存缓存作废（可能已经换网络了），
     * 再让 provider 检查一遍过期缓存。
     *
     * @param ip 连不上的 ip
     */
    public void clearCache(String ip) {
        this.domainCache.clear();
        this.dnsResolver.checkIpOverTime(ip);
    }

    /**
     * 上报一个不可达的 ip：先摘掉该域名的内存缓存，再让 provider 判断是否要重新解析。
     *
     * @param host 域名
     * @param ip   不可达的 ip
     */
    public void reportInvalidIp(String host, String ip) {
        this.domainCache.remove(host);
        this.dnsResolver.checkIpExpire(host, ip);
    }

    /**
     * 查域名的解析结果：先内存，再 provider；provider 有结果就回填内存。
     *
     * @param host 域名
     * @return 解析结果；没有缓存时返回 null
     */
    @Nullable
    public List<DomainInfo> getDomainInfo(String host) {
        List<DomainInfo> cached = this.domainCache.get(host);
        if (cached == null || cached.isEmpty()) {
            cached = this.dnsResolver.getDomainInfo(host);
            if (cached != null) {
                this.domainCache.put(host, cached);
            }
        } else {
            LogUtil.i(TAG, "cache hint: " + cached.size());
        }
        return cached;
    }

    /** 清空全部缓存（内存交给 provider 一起清）。 */
    public void clearAll() {
        this.dnsResolver.clear();
    }
}
