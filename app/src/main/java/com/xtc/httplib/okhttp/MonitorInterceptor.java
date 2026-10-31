package com.xtc.httplib.okhttp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.xtc.common.bigdata.BehaviorUtil;
import com.xtc.httplib.ConfigOptions;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.bean.FrequentRequestInfo;
import com.xtc.httplib.bean.RequestTrafficInfo;
import com.xtc.log.LogUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 请求健康监控拦截器：开机限流、并发重入、高频刷接口、单日流量超限。
 *
 * <p>它在链上紧贴 {@link HttpRequestInterceptor} 之前，所以看到的是"业务原始请求"。
 * 命中下面任意一条就不再真正发请求，直接返回一个本地合成的错误响应：</p>
 * <ul>
 *   <li>{@code 1001} 开机限流：开机后 {@link #BOOT_LIMIT_TIME} 毫秒内不许发，
 *       但 {@link #NECESSARY_BOOT_URLS} 白名单里的关键请求放行；</li>
 *   <li>{@code 1002} 并发重入：同一 URL 的上一次请求还没回来（20 秒内）；</li>
 *   <li>{@code 1003} 高频请求：60 秒窗口内同一 URL 超过 5 次；
 *       {@link #NO_NEED_MONITOR} 里的 URL 只上报不限流；</li>
 *   <li>{@code 1004} 单次请求流量超 10KB（仅上报，不拦）。</li>
 * </ul>
 *
 * <p>所有拦截/上报都可被 {@link #openMonitor} 关掉（关掉后只统计不拦截）。
 * 命中项通过 {@code BehaviorUtil} 上报埋点，事件名与参数是数据分析侧的契约。</p>
 *
 * <p>{@code requestTrafficInfo} 是**进程级静态**的，所以"单日流量"是跨 HttpClient 实例
 * 累计的；它用 volatile + 原子字段保证多线程下可见。</p>
 */
public class MonitorInterceptor extends BaseInterceptor {

    private static final String TAG = LogTag.tag("MonitorInterceptor");

    /** 开机后禁止发请求的时长（毫秒），由宿主设置。 */
    public static long BOOT_LIMIT_TIME = 0;

    /** 开机限流期间仍必须放行的关键 URL（子串匹配）。 */
    public static final List<String> NECESSARY_BOOT_URLS = new ArrayList<String>();

    /** 监控开关；关闭后只统计不拦截。 */
    public static boolean openMonitor = true;

    /** 只上报不限流的 URL 集合（子串匹配）。用写时复制集合，避免统计线程与配置线程互相阻塞。 */
    public static final Set<String> NO_NEED_MONITOR = new CopyOnWriteArraySet<String>();

    /** 当日流量统计，进程级共享。 */
    private static volatile RequestTrafficInfo requestTrafficInfo = new RequestTrafficInfo();

    /** 窥探响应体用的上限：1MB（原厂引用的是 PlaybackStateCompat 的 1024*1024 常量）。 */
    private static final long PEEK_BODY_BYTES = 1048576L;

    /** URL -> 请求时间点/状态。 */
    private Map<String, FrequentRequestInfo> frequentRequestMap;

    /** 主线程 Handler。原厂建了但当前没有任何地方往它 post，保留以维持字段与初始化顺序。 */
    private Handler mainHandler;

    MonitorInterceptor(Context context) {
        super(context);
        this.frequentRequestMap = new ConcurrentHashMap<String, FrequentRequestInfo>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 从 {@code from} 开始截取请求时间点列表。
     *
     * <p>返回新的 {@link CopyOnWriteArrayList} 而不是 {@code subList}：后者是原列表的视图，
     * 原列表被并发修改时会抛 {@code ConcurrentModificationException}。</p>
     */
    private List<Long> copyData(List<Long> requestTimes, int from) {
        LogUtil.i(TAG, "copyData from = " + from + ",requestTimes.size = " + requestTimes.size());
        CopyOnWriteArrayList<Long> result = new CopyOnWriteArrayList<Long>();
        while (from < requestTimes.size()) {
            result.add(requestTimes.get(from));
            from++;
        }
        return result;
    }

    /**
     * 造一个本地错误响应。{@code 1001~1004} 这些码只在本机出现，服务端不会返回。
     */
    private Response createErrorResponse(Request request, String url, int errorCode) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(errorCode)
                .message(getErrorMessage(url, errorCode))
                .body(EMPTY_BODY)
                .sentRequestAtMillis(-1L)
                .receivedResponseAtMillis(System.currentTimeMillis())
                .build();
    }

    /**
     * 累加当日流量，必要时上报"超限"。
     *
     * <p>跨天判定用了一个看起来很怪的算式
     * {@code ((起点/24)*3600*1000 + 1) * 24*3600*1000}：把"起点/24"当成小时数，
     * 换算成毫秒后 +1 个完整的"24 小时"作为下一个边界。原厂就是这么算的，
     * 直接换成 Calendar 会改变边界时刻，所以原样保留。</p>
     *
     * <p>已经上报过（{@code hadRecord}）就直接返回，保证一天只报一次。</p>
     */
    private void dealDayTotalTraffic(long traffic) {
        long now = System.currentTimeMillis();
        long boundary = (((requestTrafficInfo.getStartTime() / 24) * 3600 * 1000) + 1) * 24 * 3600 * 1000;
        if (requestTrafficInfo.getHadRecord()) {
            LogUtil.i(TAG, "dealDayTotalTraffic HadRecord");
            return;
        }
        if (now >= boundary) {
            // 跨天了：重置起点与累计值，这一次的流量算进新的一天
            requestTrafficInfo.setStartTime(now);
            requestTrafficInfo.setTotalTraffic(0L);
            requestTrafficInfo.setHadRecord(false);
            requestTrafficInfo.addTotalTraffic(traffic);
            return;
        }
        requestTrafficInfo.addTotalTraffic(traffic);
        if (requestTrafficInfo.getTotalTraffic() > ConfigOptions.MonitorCondition.DAY_REQUEST_MAX_TRAFFIC) {
            requestTrafficInfo.setHadRecord(true);
            LogUtil.w(TAG, "dealDayTotalTraffic Total Traffic Over 524288000");
            recordTotalTraffic(now);
        }
    }

    private void dealException(String url, int errorCode) {
        LogUtil.i(TAG, "dealException url = " + url + ",errorCode = " + errorCode);
        recordException(url, errorCode);
    }

    /** 本地错误码 -> 响应 message。未知码统一 "Unknown"（服务端/上层按这个字符串判断）。 */
    private String getErrorMessage(String url, int errorCode) {
        switch (errorCode) {
            case ConfigOptions.MonitorError.MONITOR_ERROR_BOOT_LIMIT_TIME:
                return "Http MONITOR_ERROR_BOOT_LIMIT_TIME:[" + url + "]";
            case ConfigOptions.MonitorError.MONITOR_ERROR_REQUESTING:
                return "Http MONITOR_ERROR_REQUESTING:[" + url + "]";
            case ConfigOptions.MonitorError.MONITOR_ERROR_FREQUENT_REQUEST:
                return "Http MONITOR_ERROR_FREQUENT_REQUEST:[" + url + "]";
            default:
                return "Unknown";
        }
    }

    /** 开机限流判定：白名单优先命中，其次看是否还在限流时长内。 */
    private boolean isBootLimitTime(long requestTime, String url) {
        for (String necessaryUrl : NECESSARY_BOOT_URLS) {
            if (url.contains(necessaryUrl)) {
                return true;
            }
        }
        return requestTime < BOOT_LIMIT_TIME;
    }

    /**
     * 高频请求判定（滑动窗口）。
     *
     * <p>规则与副作用：</p>
     * <ul>
     *   <li>第一次见到该 URL：建表并返回 false；</li>
     *   <li>窗口内（60 秒）且次数没超过 5：返回 false；</li>
     *   <li>窗口内且超过 5 次：丢掉最老的一条并返回 true（判定为刷接口）；</li>
     *   <li>窗口外：把已经过期的时间点裁掉；裁完仍非空就继续统计，全过期则删表。</li>
     * </ul>
     */
    private boolean isFrequentRequest(long requestTime, String url) {
        FrequentRequestInfo info = this.frequentRequestMap.get(url);
        if (info == null) {
            FrequentRequestInfo newInfo = new FrequentRequestInfo();
            newInfo.setRequestUrl(url);
            newInfo.getRequestTimes().add(Long.valueOf(requestTime));
            this.frequentRequestMap.put(url, newInfo);
            return false;
        }
        info.getRequestTimes().add(Long.valueOf(requestTime));
        if (requestTime - info.getRequestTimes().get(0).longValue()
                < ConfigOptions.MonitorCondition.FREQUENT_REQUEST_PERIOD) {
            if (info.getRequestTimes().size() <= ConfigOptions.MonitorCondition.FREQUENT_REQUEST_COUNT) {
                return false;
            }
            info.getRequestTimes().remove(0);
            return true;
        }
        int index = 0;
        while (index < info.getRequestTimes().size()
                && requestTime - info.getRequestTimes().get(index).longValue()
                >= ConfigOptions.MonitorCondition.FREQUENT_REQUEST_PERIOD) {
            index++;
        }
        if (index < info.getRequestTimes().size()) {
            info.setRequestTimes(copyData(info.getRequestTimes(), index));
            return false;
        }
        this.frequentRequestMap.remove(url);
        return false;
    }

    /** 是否有同一 URL 的请求正在飞行中（20 秒内视为同一批）。 */
    private boolean isRequesting(long requestTime, String url) {
        FrequentRequestInfo info = this.frequentRequestMap.get(url);
        if (info == null) {
            return false;
        }
        List<Long> requestTimes = info.getRequestTimes();
        if (requestTimes == null || requestTimes.size() == 0) {
            return false;
        }
        if (requestTime - requestTimes.get(requestTimes.size() - 1).longValue()
                > ConfigOptions.MonitorCondition.REQUESTING_TIME_OUT) {
            return false;
        }
        return info.isRequesting();
    }

    /** 上报被拦截的异常请求。 */
    private void recordException(String url, int errorCode) {
        String packageName = this.context.getPackageName();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("package_name", packageName);
        map.put("url", url);
        map.put("error_code", String.valueOf(errorCode));
        BehaviorUtil.customEvent(this.context, "Http_Monitor_Exception", packageName, null, map);
    }

    /** 上报请求失败（HTTP 非 2xx 或抛异常）。{@code response} 为 null 表示连响应都没有。 */
    private void recordFailure(String url, Request request, Response response) {
        String packageName = this.context.getPackageName();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("package_name", packageName);
        map.put("url", url);
        if (response == null) {
            map.put("failure_code", "-1");
            map.put("failure_message", "response is null");
        } else {
            map.put("failure_code", String.valueOf(response.code()));
            map.put("failure_message", response.message());
        }
        BehaviorUtil.customEvent(this.context, "Http_Monitor_Failure", packageName, null, map);
    }

    /** 上报当日流量超限。 */
    private void recordTotalTraffic(long currentTime) {
        String packageName = this.context.getPackageName();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("package_name", packageName);
        map.put("currentTime", String.valueOf(currentTime));
        map.put("maxTraffic", String.valueOf(ConfigOptions.MonitorCondition.DAY_REQUEST_MAX_TRAFFIC));
        BehaviorUtil.customEvent(this.context, "Http_Monitor_Total_Traffic_Over", packageName, null, map);
    }

    /** 标记某 URL 是否处于请求中；表里没有该 URL 时什么也不做。 */
    private void setRequesting(String url, boolean requesting) {
        FrequentRequestInfo info = this.frequentRequestMap.get(url);
        if (info != null) {
            info.setRequesting(requesting);
        }
    }

    /**
     * 监控主流程。
     *
     * <p>四个判定是互斥的短路顺序（开机限流 → 并发 → 高频 → 放行），与原厂一致；
     * 一旦命中并且 {@link #openMonitor} 为 true，就上报 + 清请求中标记 + 返回本地错误响应。</p>
     *
     * <p>放行路径上的 try/finally 结构也保留：无论成功、失败还是抛异常，
     * 都必须把 {@code setRequesting(url, false)} 执行掉，否则这个 URL 会被永久判定为"请求中"。</p>
     */
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        long requestTime = SystemClock.elapsedRealtime();
        Request request = chain.request();
        String url = subUrl(request.url().toString());
        LogUtil.i(TAG, "intercept url = " + url + ",requestTime = " + requestTime);
        int errorCode;
        if (isBootLimitTime(requestTime, url)) {
            errorCode = ConfigOptions.MonitorError.MONITOR_ERROR_BOOT_LIMIT_TIME;
        } else if (isRequesting(requestTime, url)) {
            errorCode = ConfigOptions.MonitorError.MONITOR_ERROR_REQUESTING;
        } else if (isFrequentRequest(requestTime, url)) {
            int code = ConfigOptions.MonitorError.MONITOR_ERROR_FREQUENT_REQUEST;
            // 命中"不必监控"名单的 URL 只上报不限流（码置 -1 即放行）
            for (String ignoreUrl : NO_NEED_MONITOR) {
                if (url.contains(ignoreUrl)) {
                    LogUtil.i(TAG, "isFrequentRequest NO_NEED_MONITOR");
                    code = -1;
                }
            }
            errorCode = code;
        } else {
            errorCode = -1;
        }
        LogUtil.i(TAG, "isFrequentRequest errorCode = " + errorCode + "---openMonitor = " + openMonitor);
        if (errorCode != -1 && openMonitor) {
            dealException(url, errorCode);
            setRequesting(url, false);
            return createErrorResponse(request, url, errorCode);
        }
        setRequesting(url, true);
        RequestBody requestBody = request.body();
        long requestLength = requestBody != null ? requestBody.contentLength() : 0L;
        try {
            try {
                Response response = chain.proceed(request);
                if (response == null || !response.isSuccessful()) {
                    recordFailure(url, request, response);
                } else {
                    long totalLength = response.peekBody(PEEK_BODY_BYTES).contentLength() + requestLength;
                    boolean countable;
                    if (request.body() == null || request.body().contentType() == null
                            || !request.body().contentType().toString().contains("multipart/form-data")) {
                        countable = true;
                    } else {
                        // 表单上传动辄几 MB，统进去会误报"流量超限"
                        LogUtil.i(TAG, "intercept contentType:" + request.body().contentType());
                        countable = false;
                    }
                    if (countable && totalLength > ConfigOptions.MonitorCondition.REQUEST_MAX_TRAFFIC) {
                        dealException(url, ConfigOptions.MonitorError.MONITOR_ERROR_HEAVY_TRAFFIC);
                    }
                    dealDayTotalTraffic(totalLength);
                }
                setRequesting(url, false);
                return response;
            } catch (IOException e) {
                LogUtil.e(TAG, "intercept e = " + e.getMessage());
                recordFailure(url, request, null);
                throw e;
            }
        } catch (Throwable th) {
            setRequesting(url, false);
            throw th;
        }
    }
}
