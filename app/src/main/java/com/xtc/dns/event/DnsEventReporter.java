package com.xtc.dns.event;

import android.content.Context;
import com.xtc.common.bigdata.BehaviorUtil;
import java.util.HashMap;

/**
 * HttpDns 请求的行为埋点（原厂 {@code com.xtc.dns.event.a}）。
 *
 * <p>每次真正发起 HttpDns 解析都会打一条 {@code apm_http_dns_req} 自定义事件，
 * 成功失败各一套字段。字段名是与数据平台约定死的，不能改。</p>
 *
 * <p>注意：{@code com.xtc.dns.client.event} 下有一份内容完全相同的实现，
 * 原厂就存在两份（各自由 client / server 侧引用），所以这里不做合并。</p>
 */
public class DnsEventReporter {

    /** 来源名：本地系统解析。 */
    public static final String SOURCE_LOCAL = "local";

    /** 来源名：腾讯 HttpDns。 */
    public static final String SOURCE_TENCENT = "tencent";

    /** 事件的 label，原厂固定传空串。 */
    private static final String LABEL = "";

    /** 事件 id。 */
    private static final String EVENT_HTTP_DNS_REQUEST = "apm_http_dns_req";

    /** 字段名：是否成功。 */
    private static final String KEY_SUCCESS = "success";

    /** 字段名：域名。 */
    private static final String KEY_HOST = "host";

    /** 字段名：解析出的 ip 串。 */
    private static final String KEY_IPS = "ips";

    /** 字段名：耗时（毫秒）。 */
    private static final String KEY_TIME_CONSUMED = "time_consumed";

    /** 字段名：来源（local / tencent）。 */
    private static final String KEY_SOURCE = "source";

    /**
     * 上报一次解析失败。
     *
     * @param context 上下文
     * @param source  来源名，见 {@link #SOURCE_LOCAL} / {@link #SOURCE_TENCENT}
     * @param host    域名
     */
    public static void reportFailure(Context context, String source, String host) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(KEY_SUCCESS, String.valueOf(false));
        params.put(KEY_HOST, host);
        params.put(KEY_SOURCE, source);
        BehaviorUtil.customEvent(context, EVENT_HTTP_DNS_REQUEST, LABEL, null, params);
    }

    /**
     * 上报一次解析成功。
     *
     * @param context      上下文
     * @param host         域名
     * @param ips          解析出的 ip 串
     * @param timeConsumed 耗时（毫秒）
     * @param source       来源名
     */
    public static void reportSuccess(Context context, String host, String ips, long timeConsumed, String source) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(KEY_SUCCESS, String.valueOf(true));
        params.put(KEY_HOST, host);
        params.put(KEY_IPS, ips);
        params.put(KEY_TIME_CONSUMED, String.valueOf(timeConsumed));
        params.put(KEY_SOURCE, source);
        BehaviorUtil.customEvent(context, EVENT_HTTP_DNS_REQUEST, LABEL, null, params);
    }
}
