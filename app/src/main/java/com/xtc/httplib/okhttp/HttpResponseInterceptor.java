package com.xtc.httplib.okhttp;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.httplib.ConfigOptions;
import com.xtc.httplib.HttpClient;
import com.xtc.httplib.LogTag;
import com.xtc.log.LogUtil;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 入站解密拦截器：把服务端的加密响应体还原成明文，再交给 Retrofit 解析。
 *
 * <p>做法是"整段读出来再重建 Response"：{@code peekBody} 只窥探不消费，
 * 用它拿到原始字节；然后造一个新 Response，body 换成明文字符串。
 * 所以下游看到的 {@code Content-Type} 统一是
 * {@link ConfigOptions.HeaderKey#MEDIA_TYPE}（application/json;charset=utf-8），
 * 即使服务端返回的是别的类型。</p>
 *
 * <p>判断依据是响应头 {@code encrypted}（值里包含该字样即认为已加密）。</p>
 *
 * <p>注意这里**没有**捕获解密异常：解密链路内部已经把异常吞成"返回原串"，
 * 所以走到这里的 body 一定可用。</p>
 */
public class HttpResponseInterceptor extends BaseInterceptor {

    private static final String TAG = LogTag.tag("HttpResponseInterceptor");

    /** 窥探响应体用的上限：1MB（原厂引用的是 PlaybackStateCompat 的 1024*1024 常量）。 */
    private static final long PEEK_BODY_BYTES = 1048576L;

    private HttpClient httpClient;

    HttpResponseInterceptor(Context context, HttpClient httpClient) {
        super(context);
        this.httpClient = httpClient;
    }

    /**
     * 解密响应体。
     *
     * @param response 原始响应
     * @param eebbkKey 本次会话的 eebbkKey，为空表示服务端未加密
     */
    private Response decryptResponse(Response response, String eebbkKey) throws IOException {
        String body = response.peekBody(PEEK_BODY_BYTES).string();
        if (TextUtils.isEmpty(body) || TextUtils.isEmpty(eebbkKey)) {
            LogUtil.w(TAG, "result or eebbkKey is null");
        } else {
            String encrypted = response.header(ConfigOptions.HeaderKey.ENCRYPTED);
            if (encrypted != null && encrypted.contains(ConfigOptions.HeaderKey.ENCRYPTED)) {
                body = HttpHelper.decodeHttpResponseBody(eebbkKey, body);
            }
        }
        Response.Builder builder = response.newBuilder();
        builder.body(ResponseBody.create(MediaType.parse(ConfigOptions.HeaderKey.MEDIA_TYPE), body));
        Response decryptedResponse = builder.build();
        LogUtil.d(TAG, "decryptResponse: header : Conf-Updates = "
                + decryptedResponse.header(ConfUpdateInterceptor.HEADER_CONF_UPDATES));
        LogUtil.d(TAG, "decryptResponse: header : Conf-Updates-V = "
                + decryptedResponse.header(ConfUpdateInterceptor.HEADER_CONF_UPDATES_VERSION));
        LogUtil.i(TAG, "decryptResponse http response:" + body);
        return decryptedResponse;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        return response != null
                ? decryptResponse(response, HttpHelper.getEebbkKey(this.httpClient))
                : response;
    }
}
