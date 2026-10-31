package com.xtc.dns.client.event;

import android.content.Context;
import com.xtc.common.bigdata.BehaviorUtil;
import java.util.HashMap;

/**
 * HttpDns 请求的行为埋点，客户端侧副本（原厂 {@code com.xtc.dns.client.event.a}）。
 *
 * <p>与 {@code com.xtc.dns.event.DnsEventReporter} 内容完全一致：原厂在 client 与 server
 * 两个包里各留了一份，各自被本方代码引用，这里保持两份、不合并，
 * 以免跨包改动影响到另一侧的调用点。</p>
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
     * @param source  来源名
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
