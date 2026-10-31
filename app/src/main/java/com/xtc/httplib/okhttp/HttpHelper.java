package com.xtc.httplib.okhttp;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.httplib.ConfigOptions;
import com.xtc.httplib.HttpClient;
import com.xtc.httplib.HttpManager;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.bean.AppInfo;
import com.xtc.httplib.bean.DeviceInfo;
import com.xtc.httplib.bean.NetBaseRequestParam;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.AesUtils;
import com.xtc.utils.encode.GzipUtils;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.encode.Md5Utils;
import com.xtc.utils.encode.RsaUtils;
import com.xtc.utils.system.DeviceUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * HTTP 请求/响应的加解密、签名与请求头拼装工具。
 *
 * <p>这里集中了整套"安全传输"协议，四个关键概念：</p>
 * <ul>
 *   <li>{@code ENCRYPT_KEY}（{@link BaseInterceptor#ENCRYPT_KEY}）：进程内随机 AES 密钥；</li>
 *   <li>{@code eebbkKey}：用内置 RSA 公钥把 ENCRYPT_KEY 加密后得到的串，
 *       放在 {@code Eebbk-Key} 头里告诉服务端本次会话用哪把 AES 密钥；</li>
 *   <li>{@code Base-Request-Param}：设备/登录基础参数 JSON，加密开启时先 AES 一次；</li>
 *   <li>{@code Eebbk-Sign}：url + 基础参数（明文）+ 请求体 + 密钥 的 MD5，用于防篡改。</li>
 * </ul>
 *
 * <p>请求体的加密链路是：明文 → gzip → AES → Base64；响应体正好相反。
 * 所有"失败"都返回 {@code null} 或原值，绝不抛异常给网络层——
 * 加密是增强能力，不能因为它把请求打死。</p>
 *
 * <p>注意日志 tag 沿用 {@code "DefaultOkHttpClient"}：原厂就是复用了这个 tag，
 * 改成 "HttpHelper" 会让线上日志检索规则失效。</p>
 */
public class HttpHelper {

    private static final String TAG = LogTag.tag("DefaultOkHttpClient");

    /** 请求体/参数统一使用的字符集名（原厂引用的是七牛 SDK 的常量，值即 "utf-8"）。 */
    private static final String CHARSET_UTF8 = "utf-8";

    /**
     * 只在请求头里没有该字段时才补上；已有值说明上游（业务方/其它拦截器）已经指定，
     * 以已有值为准，这里只打一条日志。
     */
    private static void addHeader(Request request, Map<String, String> headers, String name, String value) {
        String tempValue = request.headers().get(name);
        if (tempValue == null) {
            headers.put(name, value);
        } else {
            LogUtil.i(TAG, "addHeader tempValue:" + tempValue);
        }
    }

    /**
     * 拼 {@code Base-Request-Param} 的明文 JSON。
     *
     * <p>时间戳格式固定 {@code yyyy-MM-dd HH:mm:ss}（本地时区），服务端按同格式解析。
     * {@code imFlag="1"} 表示"本请求可能走 IM 透传通道"，保证服务端能关联到长连接会话。</p>
     */
    protected static String buildBaseRequestParamJson(HttpClient httpClient) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        NetBaseRequestParam param = new NetBaseRequestParam();
        param.setImFlag(NetBaseRequestParam.ImFlag.Yes);
        param.setAppId(NetBaseRequestParam.AppId.WATCH);
        param.setProgram(NetBaseRequestParam.Program.Watch);
        DeviceInfo deviceInfo = httpClient.getDeviceInfo();
        param.setTimestamp(timestamp);
        param.setDeviceId(deviceInfo.getBindNumber());
        param.setToken(deviceInfo.getChipId());
        param.setMac(deviceInfo.getMacAddr());
        param.setAccountId(httpClient.getWatchId());
        param.setRegistId(Long.valueOf(httpClient.getRegistId()));
        return JSONUtil.toJSON(param);
    }

    /**
     * 响应体解密：去掉 JSON 转义出来的引号 → Base64 解码（在 {@link AesUtils} 内部）→ AES 解密 → gunzip。
     *
     * <p>先 {@code replace("\"", "")} 是因为有些入口把密文又包了一层 JSON 字符串。
     * 解不出来时返回**处理过的原串**，让调用方自己当普通 JSON 解析，避免丢数据。</p>
     */
    private static String decodeAndUncompressBodyBase64(String body) {
        if (!isEmptyString(body)) {
            body = body.replace("\"", "");
        }
        byte[] decompressed = GzipUtils.decompress(AesUtils.decryptToBytes(body, BaseInterceptor.ENCRYPT_KEY));
        return decompressed != null ? new String(decompressed) : body;
    }

    /**
     * 按"是否拿到了 eebbkKey"决定要不要解密响应体。
     *
     * <p>没有 key 说明服务端这次没加密（或者还没协商成功），响应体就是明文。</p>
     */
    protected static String decodeHttpResponseBody(String eebbkKey, String body) {
        return isEmptyString(eebbkKey) ? body : decodeAndUncompressBodyBase64(body);
    }

    /**
     * 解密一次完整响应（供 IM 透传通道复用：那条链路不经过 OkHttp 拦截器，
     * 需要自己拿 header 判断有没有加密）。
     *
     * @param headers 响应头（Map 形态），为 null 视为未加密
     * @param body    响应体
     */
    public static String decodeHttpResult(Context context, Map<String, String> headers, String body) {
        String eebbkKey = getEebbkKey(HttpManager.getInstance(context).getHttpClient());
        if (isEmptyString(body) || isEmptyString(eebbkKey)) {
            LogUtil.e(TAG, "decodeHttpResult:eebbkKey is null or http response body is nll.");
        } else if (headers != null) {
            String encrypted = Headers.of(headers).get(ConfigOptions.HeaderKey.ENCRYPTED);
            if (encrypted != null && encrypted.contains(ConfigOptions.HeaderKey.ENCRYPTED)) {
                body = decodeHttpResponseBody(eebbkKey, body);
            }
        }
        LogUtil.i(TAG, "decodeHttpResult:" + body);
        return body;
    }

    /**
     * 加密一个请求体（供 IM 透传通道使用）。
     *
     * <p>只有"有 key 且服务端要求加密"时才动，否则原样返回。</p>
     */
    public static String encryptBody(Context context, String body) {
        HttpClient httpClient = HttpManager.getInstance(context).getHttpClient();
        String eebbkKey = getEebbkKey(httpClient);
        boolean encrypt = isEncrypt(httpClient);
        if (!isEmptyString(body) && !isEmptyString(eebbkKey) && encrypt) {
            body = generateEncryptBody(body, eebbkKey, BaseInterceptor.ENCRYPT_KEY);
        }
        LogUtil.i(TAG, "encrypt body:" + body);
        return body;
    }

    /**
     * 请求体加密：gzip 后 AES，再 Base64。
     *
     * <p>没有 eebbkKey 时记一条 warn 并返回明文——服务端拿到 {@code Eebbk-Key} 为空
     * 就知道是明文，两边是对齐的。</p>
     */
    protected static String generateEncryptBody(String body, String eebbkKey, String aesKey) {
        if (!isEmptyString(eebbkKey)) {
            return AesUtils.encrypt(GzipUtils.compress(body, CHARSET_UTF8), aesKey);
        }
        LogUtil.w(TAG, "eebbkKey is null,eebbkKey:" + eebbkKey);
        return body;
    }

    /** 加密 {@code Base-Request-Param}（AES + Base64，不 gzip）。 */
    protected static String generateEncryptRequestParam(String baseRequestParamJson, String aesKey) {
        LogUtil.d(TAG, "generateEncryptRequestParam");
        return AesUtils.encrypt(baseRequestParamJson, aesKey);
    }

    /**
     * 给 IM 透传通道拼请求头。
     *
     * <p>与 OkHttp 链路（{@link HttpRequestInterceptor}）的差别：这里**不覆盖**已有头，
     * 只补缺失的头，因为透传的 Request 可能来自任意来源。签名用的是 byte[] 请求体，
     * 避免"二进制内容转字符串再取字节"造成的损坏。</p>
     */
    public static Map<String, String> generateHeaderMapWithBodyBytes(Context context, Request request,
                                                                    String url, byte[] bodyBytes) {
        HttpClient httpClient = HttpManager.getInstance(context).getHttpClient();
        Map<String, String> headers = new HashMap<String, String>();
        addHeader(request, headers, "Content-Type", ConfigOptions.HeaderKey.MEDIA_TYPE);
        String baseRequestParamJson = buildBaseRequestParamJson(httpClient);
        String eebbkKey = getEebbkKey(httpClient);
        boolean encrypt = isEncrypt(httpClient);
        String requestParam = (isEmptyString(eebbkKey) || !encrypt)
                ? baseRequestParamJson
                : generateEncryptRequestParam(baseRequestParamJson, BaseInterceptor.ENCRYPT_KEY);
        headers.put(ConfigOptions.HeaderKey.EEBBK_SIGN,
                generateSignWithBodyBytes(BaseInterceptor.ENCRYPT_KEY, url, baseRequestParamJson, bodyBytes));
        headers.put(ConfigOptions.HeaderKey.BASE_REQUEST_PARAM, requestParam);
        addHeader(request, headers, "model", DeviceUtils.getDeviceModel());
        addHeader(request, headers, ConfigOptions.HeaderKey.IM_SDK_VERSION_CODE, String.valueOf(102));
        addHeader(request, headers, ConfigOptions.HeaderKey.PACKAGE_VERSION,
                String.valueOf(HttpRequestInterceptor.appVersion));
        AppInfo appInfo = httpClient.getAppInfo();
        if (appInfo != null) {
            String version = appInfo.getVersion();
            if (!isEmptyString(version)) {
                headers.put(ConfigOptions.HeaderKey.VERSION, version);
            }
            String grey = appInfo.getGrey();
            if (!isEmptyString(grey)) {
                headers.put(ConfigOptions.HeaderKey.GREY, grey);
            }
        }
        if (!isEmptyString(eebbkKey) && encrypt) {
            headers.put(ConfigOptions.HeaderKey.EEBBK_KEY, eebbkKey);
            headers.put(ConfigOptions.HeaderKey.ENCRYPTED, ConfigOptions.HeaderKey.ENCRYPTED);
            headers.put(ConfigOptions.HeaderKey.CONTENT_ENCODING, "gzip");
        }
        if (!TextUtils.isEmpty(HttpManager.getInstance(context).getAcceptLanguage())) {
            headers.put(ConfigOptions.HeaderKey.ACCEPT_LANGUAGE,
                    HttpManager.getInstance(context).getAcceptLanguage());
        }
        if (!TextUtils.isEmpty(HttpManager.getInstance(context).getWatchTimeZone())) {
            headers.put(ConfigOptions.HeaderKey.WATCH_TIME_ZONE,
                    HttpManager.getInstance(context).getWatchTimeZone());
        }
        LogUtil.i(TAG, "generateHeaderMap:" + headers);
        return headers;
    }

    /** 字符串版签名：把请求体按 utf-8 取字节后转调字节版。 */
    protected static String generateSign(String signKey, String url, String baseRequestParamJson, String body) {
        byte[] bodyBytes = null;
        if (body != null && !"".equals(body)) {
            bodyBytes = body.getBytes(Charset.forName(CHARSET_UTF8));
        }
        return generateSignWithBodyBytes(signKey, url, baseRequestParamJson, bodyBytes);
    }

    /**
     * 计算签名：MD5(url + baseRequestParam + body + key)。
     *
     * <p>用 {@link ByteArrayOutputStream} 顺序拼接而不是字符串相加，
     * 是因为 body 可能是二进制（gzip 后的密文）。拼接**顺序**是与服务端的约定，
     * 密钥放在最后一段。</p>
     */
    protected static String generateSignWithBodyBytes(String signKey, String url,
                                                      String baseRequestParamJson, byte[] bodyBytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Charset charset = Charset.forName(CHARSET_UTF8);
        byte[] urlBytes = url.getBytes(charset);
        outputStream.write(urlBytes, 0, urlBytes.length);
        byte[] paramBytes = baseRequestParamJson.getBytes(charset);
        outputStream.write(paramBytes, 0, paramBytes.length);
        if (bodyBytes != null && bodyBytes.length > 0) {
            outputStream.write(bodyBytes, 0, bodyBytes.length);
        }
        byte[] keyBytes = signKey.getBytes(charset);
        outputStream.write(keyBytes, 0, keyBytes.length);
        try {
            outputStream.close();
        } catch (IOException e) {
            // 关内存流理论上不会失败；原厂也只记日志，不改变签名结果
            LogUtil.e(e);
        }
        return Md5Utils.md5(outputStream.toByteArray());
    }

    /**
     * 取本次会话的 {@code eebbkKey}：用 RSA 公钥加密进程内的 AES 密钥。
     *
     * <p>三层判空：AppInfo 为空、RSA 公钥为空、加密失败（返回 null），
     * 任何一种都返回 null，调用方据此降级成明文传输。</p>
     */
    protected static String getEebbkKey(HttpClient httpClient) {
        AppInfo appInfo = httpClient.getAppInfo();
        if (appInfo == null) {
            LogUtil.w(TAG, "getEebbkKey AppInfo is null");
            return null;
        }
        String rsaPublicKey = appInfo.getRsaPublicKey();
        if (isEmptyString(rsaPublicKey)) {
            return null;
        }
        return RsaUtils.encryptByPublicKey(BaseInterceptor.ENCRYPT_KEY, rsaPublicKey);
    }

    /**
     * 把请求体读成字符串（用于签名与日志）。
     *
     * <p>注意 OkHttp 的 RequestBody 只能写一次，所以这里写进一个独立 Buffer 读取，
     * 不影响真正发送时 OkHttp 自己再写一次（{@link RequestBody#writeTo} 是可重复调用的）。</p>
     */
    protected static String getRequestBodyString(RequestBody requestBody) {
        if (requestBody == null) {
            return null;
        }
        try {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            return buffer.readUtf8();
        } catch (IOException e) {
            LogUtil.e(e);
            return null;
        }
    }

    /** 空串判断。原厂刻意不用 {@code TextUtils}，因为这里也接受非 String 的 CharSequence。 */
    private static boolean isEmptyString(CharSequence value) {
        return value == null || value.length() == 0;
    }

    /**
     * 服务端是否要求加密。
     *
     * <p>AppInfo 为空时**默认要求加密**（返回 true）：拿不到配置说明初始化还没完成，
     * 这时按最安全的假设走；真正发出时会因为 eebbkKey 为空而退化成明文。</p>
     */
    protected static boolean isEncrypt(HttpClient httpClient) {
        AppInfo appInfo = httpClient.getAppInfo();
        if (appInfo == null) {
            LogUtil.w(TAG, "isEncrypt AppInfo is null");
            return true;
        }
        if ("1".equals(appInfo.getEncSwitch())) {
            LogUtil.i(TAG, "isEncrypt http encrypt");
            return true;
        }
        LogUtil.i(TAG, "isEncrypt http no encrypt");
        return false;
    }
}
