package com.xtc.dns.http;

import java.util.HashMap;

/**
 * 极简的 HTTP GET 抽象（原厂 {@code com.xtc.dns.http.b}）。
 *
 * <p>只有腾讯 HttpDns 的查询会用到它：一个 url、可选的 Host 头、可选的额外请求头，
 * 返回完整响应体（多行会被拼成一行）。抽成接口是为了让 provider 不绑死具体实现。</p>
 */
public interface HttpEngine {

    /** 不带任何额外请求头。 */
    String get(String url);

    /** 带一个 {@code Host} 请求头。 */
    String get(String url, String host);

    /** 带任意请求头。 */
    String get(String url, HashMap<String, String> headers);
}
