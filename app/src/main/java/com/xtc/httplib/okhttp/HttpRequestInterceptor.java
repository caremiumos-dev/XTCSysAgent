package com.xtc.httplib.okhttp;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.httplib.ConfigOptions;
import com.xtc.httplib.HttpClient;
import com.xtc.httplib.HttpManager;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.bean.AppInfo;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.DeviceUtils;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 出站加密拦截器：补齐请求头、加密请求体，最后才真正发出去。
 *
 * <p>它在链上的位置很关键——排在 {@link MonitorInterceptor} 之后、{@link ConfUpdateInterceptor}
 * 之前，所以监控看到的是"业务原始请求"，而服务端收到的是加密后的请求。</p>
 *
 * <p>发请求前会把 URL 补一个 {@code uuid} 参数（见 {@link BaseInterceptor#getRedirectURL}），
 * 这是服务端判重的手段。</p>
 *
 * <p>{@code multipart/form-data} 的请求**不加密**：表单里有文件流，加密会破坏
 * 分片格式；同样的判断在 {@link MonitorInterceptor} 里用来跳过流量统计。</p>
 */
public class HttpRequestInterceptor extends BaseInterceptor {

    private static final String TAG = LogTag.tag("HttpRequestInterceptor");

    /**
     * 当前应用的 versionCode。
     *
     * <p>是 static 且由构造函数赋值（不是常量），{@link HttpHelper} 拼
     * {@code packageVersion} 头时直接读它。取不到包信息时为 {@code -1}。</p>
     */
    public static int appVersion;

    private HttpClient httpClient;

    HttpRequestInterceptor(Context context, HttpClient httpClient) {
        super(context);
        appVersion = initAppVersion(context);
        this.httpClient = httpClient;
    }

    /**
     * 只补缺失的头。
     *
     * <p>与 {@link HttpHelper} 里的同名方法差别：这里多打一行"名字+已有值"的日志，
     * 因为 OkHttp 链路上的请求头可能被业务方预设过，排查时必须有据可查。</p>
     */
    private void addHeader(Request request, Request.Builder builder, String name, String value) {
        String tempValue = request.headers().get(name);
        LogUtil.i(TAG, "addHeader name:" + name + ",tempValue:" + tempValue);
        if (tempValue == null) {
            builder.header(name, value);
        }
    }

    /**
     * 一次性补齐所有业务头。
     *
     * <p>三类头的设置方式不同，不能混：</p>
     * <ul>
     *   <li>基础头（Content-Type / model / 版本）用 {@code header()} 覆盖；</li>
     *   <li>签名与基础参数用 {@code header()} 强制覆盖（必须与本次请求体匹配）；</li>
     *   <li>加密相关与语言/时区用 {@code addHeader()} 追加，避免覆盖上游同名头。</li>
     * </ul>
     *
     * <p>整个方法体被 try/catch 包住：任何取包名、取属性失败都不该让请求发不出去。</p>
     */
    private void addHeader(Request request, Request.Builder builder, String sign, String baseRequestParam,
                           String eebbkKey, boolean encrypt) {
        try {
            addHeader(request, builder, "Content-Type", ConfigOptions.HeaderKey.MEDIA_TYPE);
            addHeader(request, builder, "model", DeviceUtils.getDeviceModel());
            addHeader(request, builder, ConfigOptions.HeaderKey.IM_SDK_VERSION_CODE, String.valueOf(102));
            addHeader(request, builder, ConfigOptions.HeaderKey.PACKAGE_VERSION, String.valueOf(appVersion));
            builder.header(ConfigOptions.HeaderKey.EEBBK_SIGN, sign);
            builder.header(ConfigOptions.HeaderKey.BASE_REQUEST_PARAM, baseRequestParam);
            AppInfo appInfo = this.httpClient.getAppInfo();
            if (appInfo != null) {
                String version = appInfo.getVersion();
                if (!TextUtils.isEmpty(version)) {
                    builder.header(ConfigOptions.HeaderKey.VERSION, version);
                }
                String grey = appInfo.getGrey();
                if (!TextUtils.isEmpty(grey)) {
                    builder.header(ConfigOptions.HeaderKey.GREY, grey);
                }
            }
            if (!TextUtils.isEmpty(eebbkKey) && encrypt) {
                builder.addHeader(ConfigOptions.HeaderKey.EEBBK_KEY, eebbkKey);
                builder.addHeader(ConfigOptions.HeaderKey.ENCRYPTED, ConfigOptions.HeaderKey.ENCRYPTED);
                builder.addHeader(ConfigOptions.HeaderKey.CONTENT_ENCODING, "gzip");
            }
            if (!TextUtils.isEmpty(HttpManager.getInstance(this.context).getAcceptLanguage())) {
                builder.addHeader(ConfigOptions.HeaderKey.ACCEPT_LANGUAGE,
                        HttpManager.getInstance(this.context).getAcceptLanguage());
            }
            if (TextUtils.isEmpty(HttpManager.getInstance(this.context).getWatchTimeZone())) {
                return;
            }
            builder.addHeader(ConfigOptions.HeaderKey.WATCH_TIME_ZONE,
                    HttpManager.getInstance(this.context).getWatchTimeZone());
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
    }

    /**
     * 替换请求体（必要时加密），并把 method 重新设回去。
     *
     * <p>必须显式调用 {@code builder.method(method, body)}：{@code Request.Builder} 的
     * GET/HEAD 分支会丢弃 body，重新设置一次才能让"带体的 GET"这种非常规用法保持原样。</p>
     */
    private void dealBody(Request request, Request.Builder builder, String body, String eebbkKey, boolean encrypt) {
        RequestBody requestBody = request.body();
        if (!TextUtils.isEmpty(body) && !TextUtils.isEmpty(eebbkKey) && encrypt) {
            String encryptedBody = HttpHelper.generateEncryptBody(body, eebbkKey, ENCRYPT_KEY);
            if (!TextUtils.isEmpty(encryptedBody)) {
                requestBody = RequestBody.create(requestBody.contentType(), encryptedBody);
            }
        }
        builder.method(request.method(), requestBody);
    }

    /**
     * 补 uuid 参数并执行请求。
     *
     * <p>异常被吞掉后**不**继续抛：原厂的意图是"发不出去也要给调用方一个 Response"，
     * 于是兜底造一个 code=1005 的响应（{@code 1005} 是私有错误码），
     * body 用 {@link #EMPTY_BODY}。注意这个兜底发生在 finally 里，
     * 所以"proceed 返回 null"与"proceed 抛异常"两条路径都会走到。</p>
     */
    private Response doHttpDns(Interceptor.Chain chain, Request.Builder builder) throws IOException {
        Response response = null;
        try {
            builder.url(getRedirectURL(builder.build().url().toString(), false));
            response = chain.proceed(builder.build());
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        } finally {
            if (response == null) {
                response = new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(1005)
                        .message("Response is Null! Contact the server developer.")
                        .body(EMPTY_BODY)
                        .sentRequestAtMillis(-1L)
                        .receivedResponseAtMillis(System.currentTimeMillis())
                        .build();
            }
        }
        return response;
    }

    /**
     * 由原始请求构造"加密并补齐头"的新请求。
     *
     * <p>顺序不能改：先读 body 用来算签名，再补头（签名本身是头的一部分），
     * 最后才换 body。反过来的话签名会和实际发出的内容对不上。</p>
     */
    private Request.Builder encryptRequest(Interceptor.Chain chain) {
        String eebbkKey = HttpHelper.getEebbkKey(this.httpClient);
        boolean encrypt = HttpHelper.isEncrypt(this.httpClient);
        Request request = chain.request();
        String url = request.url().toString();
        String method = request.method();
        String bodyStr = HttpHelper.getRequestBodyString(request.body());
        LogUtil.i(TAG, "encryptRequest url:" + url + ", method:" + method
                + ", isEncrypt:" + encrypt + ",bodyStr:" + bodyStr);
        if (request.body() != null && request.body().contentType() != null
                && request.body().contentType().toString().contains("multipart/form-data")) {
            LogUtil.i(TAG, "encryptRequest contentType:" + request.body().contentType());
            encrypt = false;
        }
        String baseRequestParamJson = HttpHelper.buildBaseRequestParamJson(this.httpClient);
        String requestParam = (TextUtils.isEmpty(eebbkKey) || !encrypt)
                ? baseRequestParamJson
                : HttpHelper.generateEncryptRequestParam(baseRequestParamJson, ENCRYPT_KEY);
        String sign = HttpHelper.generateSign(ENCRYPT_KEY, url, baseRequestParamJson, bodyStr);
        Request.Builder builder = request.newBuilder();
        addHeader(request, builder, sign, requestParam, eebbkKey, encrypt);
        LogUtil.i(TAG, "encryptRequest header = " + builder.build().headers().toString());
        dealBody(request, builder, bodyStr, eebbkKey, encrypt);
        return builder;
    }

    /**
     * 读本应用的 versionCode。取不到（包还没装好 / 被卸载）时返回 {@code -1}，
     * 并把这个异常本身记进日志——和 {@code packageVersion} 头里的 -1 对得上。
     */
    private static int initAppVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            LogUtil.e(TAG, "get app version error = " + e);
            return -1;
        }
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        return doHttpDns(chain, encryptRequest(chain));
    }
}
