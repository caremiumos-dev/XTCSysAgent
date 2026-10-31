package com.xtc.dns.http;

import android.text.TextUtils;
import com.xtc.log.LogUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 用 {@link HttpURLConnection} 实现的 GET（原厂 {@code com.xtc.dns.http.a}）。
 *
 * <p>之所以不用 okhttp：这个请求发生在 okhttp 的 DNS 解析阶段内部，
 * 用 okhttp 会形成"解析域名 → 请求 HttpDns → 又要解析域名"的递归。</p>
 *
 * <p>超时固定 5 秒、失败一律返回 null 且 {@code printStackTrace()}——
 * HttpDns 是加速器，失败必须静默降级到系统解析，不能把异常抛给调用方。</p>
 */
public class HttpUrlConnectionEngine implements HttpEngine {

    /** 连接超时（毫秒）。 */
    private static final int CONNECT_TIMEOUT = 5000;

    /** 读超时（毫秒）。 */
    private static final int READ_TIMEOUT = 5000;

    /** 打印请求头用的日志 tag，原厂就写成了这个"人名"字样，保持不变。 */
    private static final String HEADER_LOG_TAG = "HXQ-TAG";

    /** 请求方法。 */
    private static final String METHOD_GET = "GET";

    /** Host 请求头的键。 */
    private static final String HEADER_HOST = "host";

    @Override
    public String get(String url) {
        return get(url, "");
    }

    @Override
    public String get(String url, String host) {
        HashMap<String, String> headers = null;
        if (!TextUtils.isEmpty(host)) {
            headers = new HashMap<String, String>();
            headers.put(HEADER_HOST, host);
        }
        return get(url, headers);
    }

    /**
     * 发起 GET 并读取整个响应体。
     *
     * <p>流与连接的关闭都放在 finally 里，且**只有 reader 建起来了**才 disconnect：
     * 这是原厂的行为（reader 为 null 说明连流都没拿到，此时 connection 可能还没建立完整）。
     * 关闭时的 IOException 也只打印不外抛。</p>
     */
    @Override
    public String get(String url, HashMap<String, String> headers) {
        BufferedReader reader = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(METHOD_GET);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    LogUtil.i(HEADER_LOG_TAG, "" + entry.getKey() + "  -  " + entry.getValue());
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                connection.disconnect();
            }
        }
    }
}
