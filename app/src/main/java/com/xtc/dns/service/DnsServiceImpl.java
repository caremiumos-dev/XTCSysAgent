package com.xtc.dns.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.xtc.dns.DnsLogTag;
import com.xtc.dns.info.HttpDnsResult;
import com.xtc.dns.event.DnsEventReporter;
import com.xtc.dns.service.provider.DnsProvider;
import com.xtc.dns.service.provider.LocalDnsProvider;
import com.xtc.dns.service.provider.TencentDnsProvider;
import com.xtc.dns.storage.DnsStorageServiceImpl;
import com.xtc.dns.storage.db.DomainModel;
import com.xtc.log.LogUtil;
import com.xtc.utils.storage.SPUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpDns 服务侧的实现：先在线查、再本地兜底、最后落库（原厂 {@code com.xtc.dns.service.b}）。
 *
 * <p>读路径（{@link #getDomainModel}）必须是**非阻塞**的——它被 ContentProvider 的
 * query 直接调用，调用方是别的进程，卡住会拖死对方的 DNS 解析。
 * 所以未命中时只是"起一个后台线程去刷新"，本次仍然返回 null。</p>
 *
 * <p>刷新请求做了两层节流：
 * <ol>
 *   <li>同域名去重：同一个域名上一次请求还没结束时不再发新的，
 *       但超过 {@link #NOTIFY_INTERVAL} 会补发一次（防线程被杀掉后卡死）；</li>
 *   <li>指数退避：本地解析连续失败会累加 {@code retryCount}，
 *       两次请求的间隔按 {@code 2^n} 毫秒放大，上限 2^10 毫秒。</li>
 * </ol></p>
 */
public class DnsServiceImpl implements DnsService {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("DnsServiceImpl");

    /** 累计访问 HttpDns 次数的 SharedPreferences 键。 */
    private static final String SP_HTTP_DNS_REQUEST_COUNT = "http_dns_request_count";

    /**
     * 同域名请求的补发间隔（毫秒）。
     *
     * <p>原实现引用 {@code com.xtc.bigdata.common.constants.Constants.NOTIFY_INTERVAL}
     * （值为 30000）。为一个常量跨包依赖整块统计 SDK 不值得，这里直接写等值常量。</p>
     */
    private static final long NOTIFY_INTERVAL = 30000L;

    /** 指数退避的指数上限。 */
    private static final int MAX_RETRY_COUNT = 10;

    /** 后台刷新线程的名字（原厂设在 run 里，便于排查线程栈）。 */
    private static final String REQUEST_THREAD_NAME = "Get Http Dns Data";

    /** 发起 DNS 查询用的系统服务名（原厂用字面量，等价于 Context.CONNECTIVITY_SERVICE）。 */
    private static final String CONNECTIVITY_SERVICE = "connectivity";

    /** 挂在"同域名请求去重"表里的一个待完成请求。 */
    private static class PendingRequest {

        /** 真正干活的任务。 */
        final Runnable task;

        /** 创建时间，用于判断是否需要补发。 */
        final long createTime = System.currentTimeMillis();

        PendingRequest(Runnable task) {
            this.task = task;
        }

        /** 新起一个线程执行任务（同一个任务可以被重复执行，即"补发"）。 */
        void start() {
            new Thread(this.task).start();
        }

        long getCreateTime() {
            return this.createTime;
        }
    }

    /** applicationContext。 */
    private final Context context;

    /** 正在进行中的域名 → 请求，用于同域名去重。 */
    private final ConcurrentHashMap<String, PendingRequest> pendingRequests =
            new ConcurrentHashMap<String, PendingRequest>();

    /** provider 列表，按优先级排列：腾讯在前，本地兜底。 */
    private final ArrayList<DnsProvider> providers = new ArrayList<DnsProvider>();

    /** 延迟获取的 ConnectivityManager。 */
    private ConnectivityManager connectivityManager;

    /** 上一次请求的时间戳（毫秒），为 0 表示还没请求过。 */
    private long lastRequestTime = 0;

    /** 本地解析连续失败的次数，指数退避的指数。 */
    private int retryCount = 0;

    /** 存储服务。 */
    private final DnsStorageServiceImpl storage;

    public DnsServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.providers.add(new TencentDnsProvider());
        this.providers.add(new LocalDnsProvider());
        // 注意这里传的是原始 context，不是上面取的 applicationContext（原厂如此）。
        this.storage = new DnsStorageServiceImpl(context);
    }

    /** 按"最大缓存时间"删掉库里过期的记录。 */
    private void deleteExpiredFromDatabase() {
        this.storage.deleteExpired(System.currentTimeMillis() - ((long) (DnsConfig.maxCacheTime * 1000)));
    }

    /**
     * 真正的刷新动作（在后台线程里跑）：解析 → 落库 → 累加访问计数。
     *
     * <p>{@code lastRequestTime} 在最前面就更新，这样即使后续解析失败，
     * 退避窗口也是从"这次尝试"开始算的。</p>
     */
    private void requestDnsData(String host) {
        this.lastRequestTime = System.currentTimeMillis();
        HttpDnsResult result = resolveByProviders(host);
        if (result == null) {
            return;
        }
        this.retryCount = 0;
        this.storage.save(result);
        int requestCount = SPUtils.getInstance(this.context).getInt(SP_HTTP_DNS_REQUEST_COUNT, 0) + 1;
        LogUtil.i(TAG, "访问HttpDns总次数：" + requestCount);
        SPUtils.getInstance(this.context).putInt(SP_HTTP_DNS_REQUEST_COUNT, requestCount);
    }

    /**
     * 发起一次异步刷新（同域名去重 + 超时补发）。
     *
     * @param host 域名
     */
    private void startRequest(final String host) {
        PendingRequest pending = this.pendingRequests.get(host);
        if (pending == null) {
            PendingRequest newPending = new PendingRequest(() -> {
                Thread.currentThread().setName(REQUEST_THREAD_NAME);
                requestDnsData(host);
                pendingRequests.remove(host);
            });
            this.pendingRequests.put(host, newPending);
            newPending.start();
        } else {
            LogUtil.w(TAG, "同一个域名请求还未结束，不发起HttpDns请求，domain = " + host);
            if (System.currentTimeMillis() - pending.getCreateTime() > NOTIFY_INTERVAL) {
                pending.start();
            }
        }
    }

    /**
     * 是否允许现在发起请求：距上次请求要超过 {@code 2^retryCount} 毫秒，且当前有网。
     *
     * <p>{@code retryCount} 在比较之后才被夹到 10，所以上限那个分支实际只在
     * 计数已经超限时兜一次底——原厂逻辑，保持原样。</p>
     */
    private boolean canRequest() {
        long sinceLastRequest = System.currentTimeMillis() - this.lastRequestTime;
        if (this.lastRequestTime != 0) {
            LogUtil.w(TAG, "距上次请求TencentHttpDns->" + sinceLastRequest + "ms,请求指数 updateCount = " + this.retryCount);
        }
        if (sinceLastRequest >= Math.pow(2.0d, this.retryCount)) {
            if (this.retryCount >= MAX_RETRY_COUNT) {
                this.retryCount = MAX_RETRY_COUNT;
            }
            if (isNetworkAvailable()) {
                return true;
            }
            LogUtil.w(TAG, "当前网络断开，不请求HttpDns");
        }
        return false;
    }

    /**
     * 依次询问各 provider，第一个给出结果的就采用。
     *
     * <p>整个循环加锁：{@code providers} 是普通 ArrayList，而且要保证
     * "谁失败、谁被计数"的判断不被并发打断。每次询问都会打一条行为埋点
     * （成功/失败各一条），失败的是本地解析才会累加退避指数。</p>
     */
    private HttpDnsResult resolveByProviders(String host) {
        synchronized (this.providers) {
            for (DnsProvider provider : this.providers) {
                if (provider.isAvailable()) {
                    long startTime = System.currentTimeMillis();
                    HttpDnsResult result = provider.lookup(host);
                    long timeConsumed = System.currentTimeMillis() - startTime;
                    if (result != null) {
                        DnsEventReporter.reportSuccess(this.context, host, result.getIps(), timeConsumed, provider.getSourceName());
                        return result;
                    }
                    DnsEventReporter.reportFailure(this.context, provider.getSourceName(), host);
                    if (provider instanceof LocalDnsProvider) {
                        this.retryCount++;
                        LogUtil.w(TAG, "LocalDns解析失败，增加指数，updateCount = " + this.retryCount);
                    }
                }
            }
            return null;
        }
    }

    /** 当前是否有可用网络。 */
    private boolean isNetworkAvailable() {
        if (this.connectivityManager == null) {
            this.connectivityManager = (ConnectivityManager) this.context.getSystemService(CONNECTIVITY_SERVICE);
        }
        if (this.connectivityManager != null) {
            NetworkInfo activeNetworkInfo = this.connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return activeNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    @Override
    public List<DomainModel> getDomainModel(String host) {
        List<DomainModel> models = this.storage.queryByDomain(host);
        if (models != null) {
            LogUtil.i(TAG, "getDomainModel, " + models.toString());
            return models;
        }
        if (canRequest()) {
            startRequest(host);
        }
        return null;
    }

    @Override
    public void checkIpOverTime() {
        new Thread(() -> deleteExpiredFromDatabase()).start();
    }

    @Override
    public void checkIpExpire(String host, String ip) {
        DomainModel model;
        if (isNetworkAvailable() && (model = this.storage.queryByHostAndIp(host, ip)) != null) {
            boolean expired = System.currentTimeMillis() - model.getCreateTime() > model.getDomainTtl() * 1000;
            LogUtil.w(TAG, "检测IP是否失效:" + expired + ",host=" + host + ",ip=" + ip);
            if (expired && canRequest()) {
                startRequest(host);
            }
        }
    }

    @Override
    public void clear() {
        this.storage.clearAll();
    }

    @Override
    public void decreasePriority(String ip) {
        if (isNetworkAvailable()) {
            this.storage.decreasePriority(ip);
        } else {
            LogUtil.i(TAG, "无网络，不降权ip, ip = " + ip);
        }
    }
}
