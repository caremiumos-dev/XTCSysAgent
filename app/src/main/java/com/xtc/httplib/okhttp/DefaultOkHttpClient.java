package com.xtc.httplib.okhttp;

import android.content.Context;
import android.os.Process;
import com.xtc.dns.api.DnsStrategy;
import com.xtc.httplib.HttpClient;
import com.xtc.httplib.LogTag;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

/**
 * {@link HttpClient} 的 OkHttp + Retrofit 实现，是整个网络库的骨架。
 *
 * <p>它维护两套 OkHttpClient：</p>
 * <ul>
 *   <li>{@code okHttpClient}：自有业务用，拦截链完整
 *       （超时 → 监控 → 预处理/透传 → 加密 → 配置更新 → 解密 → 重试）；</li>
 *   <li>{@code okHttpClientThird}：第三方业务用，只带监控与重试——
 *       第三方的 URL 不适用自家的加解密与透传协议。</li>
 * </ul>
 * <p>另有两套 {@code web} 客户端（{@link #getWebOkHttpClient()}），构建方式相同，
 * 但懒加载且独立：给 WebView/下载这类"要单独调超时"的场景用。</p>
 *
 * <p>Retrofit 实例按 {@code baseUrl + 是否同步} 缓存。为什么要区分同步：异步接口
 * 用 {@code RxJavaCallAdapterFactory.createWithScheduler(自定义线程池)}，
 * 让 Rx 的订阅调度走统一的 {@code http-thread} 线程池（便于统一降优先级）；
 * 同步接口不需要调度器，用默认工厂即可。</p>
 *
 * <p>DNS 与连接事件都由 {@link DnsStrategy} 接管：它既是 OkHttp 的 {@code Dns}，
 * 又是 {@code EventListener.Factory}，负责 HTTPDNS 与"连接失败时上报/降级"。
 * 同一个实例同时挂到两处是必要的——内部要按 host 记录连接失败次数，
 * 换成两个实例就统计不到了。</p>
 */
public class DefaultOkHttpClient extends HttpClient {

    private static final String TAG = LogTag.tag("DefaultOkHttpClient");

    /** 连接超时（毫秒），可被 {@code HttpManager#setTimeOut} 覆盖。 */
    public static int CONNECT_TIMEOUT = 15000;

    /** 写超时（毫秒），可被 {@code HttpManager#setTimeOut} 覆盖。 */
    public static int WRITE_TIMEOUT = 15000;

    /** 读超时（毫秒），可被 {@code HttpManager#setTimeOut} 覆盖。 */
    public static int READ_TIMEOUT = 15000;

    /** HTTPDNS 策略 + 连接事件监听。 */
    private DnsStrategy dnsStrategy;

    /** Rx 订阅用的线程池。 */
    private Executor httpExecutor;

    /** 自有业务的客户端。 */
    private OkHttpClient okHttpClient;

    /** 第三方业务的客户端。 */
    private OkHttpClient okHttpClientThird;

    /** WebView/下载用的客户端，懒加载。 */
    private OkHttpClient webOkHttpClient;

    /** WebView/下载用的第三方客户端，懒加载。 */
    private OkHttpClient webOkHttpClientThird;

    /** {@code baseUrl + 是否同步} -> Retrofit 实例。 */
    private final Map<String, Retrofit> urlToRetrofitCache;

    public DefaultOkHttpClient(Context context) {
        super(context);
        this.urlToRetrofitCache = new HashMap<String, Retrofit>();
        initDns();
        initOkHttpClient();
        initOkHttpClientThird();
        initExecutor();
    }

    /**
     * 自有业务的拦截链。
     *
     * <p>**顺序即语义**，改动会直接影响行为：</p>
     * <ol>
     *   <li>{@link TimeoutInterceptor}：按注解改超时，必须最先（后面的 proceed 才会用新值）；</li>
     *   <li>{@link MonitorInterceptor}：看原始请求，命中就直接短路；</li>
     *   <li>{@link PreprocessorInterceptor}：能走 IM 就不发网络；</li>
     *   <li>{@link HttpRequestInterceptor}：加密与补头，真正发包；</li>
     *   <li>{@link ConfUpdateInterceptor}：读配置更新头；</li>
     *   <li>{@link HttpResponseInterceptor}：解密响应体；</li>
     *   <li>{@link RetryInterceptor}：末端占位。</li>
     * </ol>
     */
    private OkHttpClient.Builder createOkHttpClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.dns(this.dnsStrategy);
        builder.eventListenerFactory(this.dnsStrategy);
        builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.addInterceptor(new TimeoutInterceptor());
        builder.addInterceptor(new MonitorInterceptor(this.context));
        builder.addInterceptor(new PreprocessorInterceptor(this.context));
        builder.addInterceptor(new HttpRequestInterceptor(this.context, this));
        builder.addInterceptor(new ConfUpdateInterceptor(this.context));
        builder.addInterceptor(new HttpResponseInterceptor(this.context, this));
        builder.addInterceptor(new RetryInterceptor(this.context));
        return builder;
    }

    /** 第三方业务的拦截链：只留监控与重试，不做加解密/透传。 */
    private OkHttpClient.Builder createOkHttpClientThirdBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.dns(this.dnsStrategy);
        builder.eventListenerFactory(this.dnsStrategy);
        builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.addInterceptor(new MonitorInterceptor(this.context));
        builder.addInterceptor(new RetryInterceptor(this.context));
        return builder;
    }

    /** 建一个 Retrofit（不带缓存）。原厂保留了它，但当前所有入口都走 {@link #getOrCreateRetrofit}。 */
    private Retrofit createRetrofit(String baseUrl, OkHttpClient okHttpClient, boolean sync) {
        return new Retrofit.Builder()
                .addCallAdapterFactory(getCallAdapterFactory(sync))
                .addConverterFactory(GsonConverterFactory.create(JSONUtil.createGsonBuilder().create()))
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build();
    }

    /**
     * 选 CallAdapter 工厂。
     *
     * <p>注意参数含义：{@code true} 表示"同步调用"，此时**不需要**调度器；
     * {@code false}（异步/Rx）才挂上自定义线程池。</p>
     */
    private CallAdapter.Factory getCallAdapterFactory(boolean sync) {
        return sync
                ? RxJavaCallAdapterFactory.create()
                : RxJavaCallAdapterFactory.createWithScheduler(Schedulers.from(this.httpExecutor));
    }

    /**
     * 取（必要时创建）Retrofit。
     *
     * <p>缓存 key 是 {@code baseUrl + sync}（字符串拼接，不是复合对象），
     * 所以 {@code http://a/} + {@code true} 的 key 是 {@code "http://a/true"}——
     * 与原厂一致。缓存用 {@code synchronized} 保护，因为 Retrofit 创建较慢，
     * 并发重复创建会让拦截链出现多份实例。</p>
     */
    private Retrofit getOrCreateRetrofit(String baseUrl, OkHttpClient okHttpClient, boolean sync) {
        Retrofit retrofit;
        synchronized (this.urlToRetrofitCache) {
            retrofit = this.urlToRetrofitCache.get(baseUrl + sync);
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .addCallAdapterFactory(getCallAdapterFactory(sync))
                        .addConverterFactory(GsonConverterFactory.create(JSONUtil.createGsonBuilder().create()))
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .build();
                this.urlToRetrofitCache.put(baseUrl + sync, retrofit);
            } else {
                LogUtil.i(TAG, "get retrofit from cache");
            }
        }
        return retrofit;
    }

    private void initDns() {
        this.dnsStrategy = new DnsStrategy(this.context);
    }

    /**
     * 建 Rx 用的线程池。
     *
     * <p>每个线程名都叫 {@code http-thread}（便于在 systrace/日志里区分），
     * 并把优先级降到后台（10）：网络回调里通常还要做解析/落库，
     * 不该和 UI 抢 CPU。</p>
     */
    private void initExecutor() {
        this.httpExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable runnable) {
                return new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(10);
                        runnable.run();
                    }
                }, "http-thread");
            }
        });
    }

    private void initOkHttpClient() {
        this.okHttpClient = createOkHttpClientBuilder().build();
    }

    private void initOkHttpClientThird() {
        this.okHttpClientThird = createOkHttpClientThirdBuilder().build();
    }

    private void initWebOkHttpClient() {
        this.webOkHttpClient = createOkHttpClientBuilder().build();
    }

    private void initWebOkHttpClientThird() {
        this.webOkHttpClientThird = createOkHttpClientThirdBuilder().build();
    }

    /**
     * 给所有自有业务请求追加固定请求头。
     *
     * <p>实现方式是把一个拦截器**追加到链上**再重建客户端（OkHttp 的配置是不可变的），
     * 所以这个方法每调一次，链上就多一层——业务侧应该只在初始化时调一次。</p>
     *
     * <p>用 {@code addHeader}（可重复）而不是 {@code header}（覆盖）：
     * 调用方要的是"追加"，同名多值在 HTTP 里是合法的。</p>
     */
    @Override
    public void addHeadToOkHttpClient(final Map<String, String> headers) {
        OkHttpClient.Builder builder = this.okHttpClient.newBuilder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Request.Builder requestBuilder = chain.request().newBuilder();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
                return chain.proceed(requestBuilder.build());
            }
        });
        this.okHttpClient = builder.build();
    }

    /** WebView/下载用的客户端（懒加载 + 同步，避免并发建出多个）。 */
    public synchronized OkHttpClient getWebOkHttpClient() {
        if (this.webOkHttpClient == null) {
            initWebOkHttpClient();
        }
        return this.webOkHttpClient;
    }

    /** WebView/下载用的第三方客户端（懒加载 + 同步）。 */
    public synchronized OkHttpClient getWebOkHttpClientThird() {
        if (this.webOkHttpClientThird == null) {
            initWebOkHttpClientThird();
        }
        return this.webOkHttpClientThird;
    }

    @Override
    public Call newCall(Request request) {
        return this.okHttpClient.newCall(request);
    }

    @Override
    public <T> T request(String baseUrl, Class<T> service) {
        LogUtil.i(TAG, "request baseUrl = " + baseUrl);
        return getOrCreateRetrofit(baseUrl, this.okHttpClient, false).create(service);
    }

    @Override
    public <T> T requestSync(String baseUrl, Class<T> service) {
        LogUtil.i(TAG, "requestSync baseUrl = " + baseUrl);
        return getOrCreateRetrofit(baseUrl, this.okHttpClient, true).create(service);
    }

    @Override
    public <T> T requestThird(String baseUrl, Class<T> service) {
        LogUtil.i(TAG, "requestThird baseUrl = " + baseUrl);
        return getOrCreateRetrofit(baseUrl, this.okHttpClientThird, false).create(service);
    }

    @Override
    public <T> T requestThirdSync(String baseUrl, Class<T> service) {
        LogUtil.i(TAG, "requestThirdSync baseUrl = " + baseUrl);
        return getOrCreateRetrofit(baseUrl, this.okHttpClientThird, true).create(service);
    }
}
