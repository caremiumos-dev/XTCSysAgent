package com.xtc.dns.api;

import android.content.Context;
import android.support.annotation.NonNull;
import com.xtc.dns.client.DomainInfo;
import com.xtc.dns.client.HttpDnsManager;
import com.xtc.dns.client.event.DnsEventReporter;
import com.xtc.log.LogUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Dns;
import okhttp3.EventListener;
import okhttp3.Protocol;

/**
 * okhttp 的 DNS 策略：先问本机 HttpDns 缓存，拿不到再回落到系统解析
 * （原厂 {@code com.xtc.dns.api.b}）。
 *
 * <p>它同时实现 {@link Dns} 与 {@link EventListener.Factory}：
 * 前者负责"域名 → IP 列表"，后者负责在连接失败时把坏 IP 从缓存里摘掉并上报，
 * 两件事配合起来才能让 HttpDns 的缓存自我纠错。</p>
 *
 * <p>注意 {@link #lookup} 是**热路径**，会被 okhttp 的每个连接调用；
 * 命中缓存时走内存 map，未命中时先发起一次后台刷新（不阻塞），再用系统解析兜底。</p>
 */
public class DnsStrategy implements Dns, EventListener.Factory {

    /** 日志 tag（原厂用 {@code apiTag("DnsStrategy")} 拼出来的）。 */
    private static final String TAG = ApiLogTag.apiTag("DnsStrategy");

    /**
     * 借用的 ContentProvider 所属包名。
     *
     * <p>HttpDns 的数据是"谁提供谁生效"：本模块自己不一定注册了 provider，
     * 因此固定去 launcher 的 provider 取数据，取不到才回落到本包。</p>
     */
    private static final String PROVIDER_PACKAGE = "com.xtc.i3launcher";

    /** 上下文，构造后不再变化。 */
    private final Context context;

    /** 连接失败监听器；okhttp 每次建连都会通过 {@link #create(Call)} 取它。 */
    private final EventListener connectFailedListener;

    /** 连接失败监听器：清掉这个 IP 的缓存，并把它标记给 service 端做降权。 */
    private final class ConnectFailedListener extends EventListener {

        @Override
        public void connectFailed(@NonNull Call call, @NonNull InetSocketAddress inetSocketAddress,
                                  @NonNull Proxy proxy, Protocol protocol, @NonNull IOException e) {
            String host = call.request().url().host();
            String hostAddress = inetSocketAddress.getAddress().getHostAddress();
            HttpDnsManager.getInstance(DnsStrategy.this.context).clearCache(hostAddress);
            HttpDnsManager.getInstance(DnsStrategy.this.context).reportInvalidIp(host, hostAddress);
        }
    }

    /**
     * 构造时就把 provider 绑定到 launcher 的包名上——这一步只做一次，
     * 后续所有查询都走同一个 {@code HttpDnsManager} 单例。
     */
    public DnsStrategy(Context context) {
        this.connectFailedListener = new ConnectFailedListener();
        this.context = context;
        HttpDnsManager.getInstance(context).initDnsResolver(PROVIDER_PACKAGE);
    }

    /** 每个 Call 复用同一个监听器实例（原厂如此，避免高频建连时反复 new）。 */
    @NonNull
    @Override
    public EventListener create(@NonNull Call call) {
        return this.connectFailedListener;
    }

    /**
     * 域名解析。
     *
     * <p>顺序严格保持原厂：缓存命中 → 直接返回；未命中 → 用系统解析，
     * 成功就记一条成功埋点（{@code "local"}），失败就记一条失败埋点并把异常原样抛出。
     * 注意抛出的必须是同一个异常实例，okhttp 依赖它做重试判定。</p>
     */
    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        List<DomainInfo> domainInfoList = HttpDnsManager.getInstance(this.context).getDomainInfo(hostname);
        LogUtil.i(TAG, "hostname = " + hostname + "---domainInfo = " + domainInfoList);
        if (domainInfoList != null && !domainInfoList.isEmpty()) {
            ArrayList<InetAddress> addresses = new ArrayList<InetAddress>(domainInfoList.size());
            for (DomainInfo domainInfo : domainInfoList) {
                addresses.add(InetAddress.getByName(domainInfo.getIp()));
            }
            return addresses;
        }
        try {
            long startTime = System.currentTimeMillis();
            List<InetAddress> localAddresses = Dns.SYSTEM.lookup(hostname);
            long timeConsumed = System.currentTimeMillis() - startTime;
            if (!localAddresses.isEmpty()) {
                // 第三个参数传的是 getHostName()，这是原厂写法（上报里当前不消费该值），保持一致。
                DnsEventReporter.reportSuccess(this.context, hostname,
                        localAddresses.get(0).getHostName(), timeConsumed, "local");
            }
            return localAddresses;
        } catch (UnknownHostException e) {
            DnsEventReporter.reportFailure(this.context, "local", hostname);
            throw e;
        }
    }
}
