package com.xtc.httplib.okhttp;

import android.content.Context;
import com.xtc.httplib.LogTag;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.UuidUtils;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 所有拦截器的基类：保存 Context、提供 uuid 参数注入与"去掉 uuid 后的 url"。
 *
 * <p>为什么请求要带 {@code uuid} 参数：OkHttp 的链式重试/重定向会把同一个 URL
 * 发多次，服务端靠 uuid 识别"这是不是同一个请求的重复到达"。所以拦截器在真正发出前
 * 补 {@code uuid}，统计（{@link MonitorInterceptor#subUrl(String)}）时再把它剥掉，
 * 这样同一路径的请求才能被归到一组。</p>
 *
 * <p>{@link #ENCRYPT_KEY} 是**进程级一次性随机**的 AES 密钥（uuid 前 16 位）：
 * 它与服务端通过 RSA 交换（见 {@link HttpHelper#getEebbkKey}），所以每次冷启动都不同。
 * 写成 static final 在类初始化时算一次，正是这个语义。</p>
 */
public abstract class BaseInterceptor implements Interceptor {

    private static final String TAG = LogTag.tag("BaseInterceptor");

    /** 请求体加密用的 AES 密钥，取随机 uuid 的前 16 位。 */
    static final String ENCRYPT_KEY = UuidUtils.uuid().substring(0, 16);

    /**
     * 空响应体。
     *
     * <p>监控拦截器拦下请求时要用它造一个"没有 body"的 Response；
     * 必须是单例——每次 new 会让调用方拿到不同的对象，且 {@link #source()}
     * 返回的 Buffer 只能被读一次，复用同一个空 Buffer 正好满足"永远读不到内容"。</p>
     */
    static final ResponseBody EMPTY_BODY = new EmptyResponseBody();

    /** 应用上下文，由子类构造函数注入（可能为 null）。 */
    protected Context context;

    public BaseInterceptor() {
    }

    BaseInterceptor(Context context) {
        if (context == null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext != null ? applicationContext : context;
    }

    /**
     * 给 URL 追加 uuid 参数。
     *
     * @param url          原始 URL
     * @param keepOriginal 为 true 时原样返回（有些链路上游已经加过了）
     */
    String getRedirectURL(String url, boolean keepOriginal) {
        if (keepOriginal) {
            return url;
        }
        // 已有 query 就用 & 追加，否则用 ? 开头
        return url.contains("?") ? url + "&uuid=" + UuidUtils.uuid() : url + "?uuid=" + UuidUtils.uuid();
    }

    /**
     * 基类不做事，直接返回 null（抽象占位）。子类都会覆写。
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        return null;
    }

    /**
     * 剥掉 {@code uuid} 参数，得到用于统计/日志的"纯路径"。
     *
     * <p>两种形态都要处理：{@code ?uuid=} 与 {@code &uuid=}。
     * 找到就直接截断到参数之前，找不到则原样返回。</p>
     */
    protected String subUrl(String url) {
        int index = url.indexOf("?uuid=");
        if (index == -1) {
            index = url.indexOf("&uuid=");
        }
        if (index != -1) {
            url = url.substring(0, index);
        }
        LogUtil.i(TAG, "http request sub url:" + url);
        return url;
    }

    /**
     * 空响应体实现（原来是 {@code BaseInterceptor$1} 匿名类，按用途还原成命名内部类）。
     */
    private static final class EmptyResponseBody extends ResponseBody {

        /**
         * 显式给一个包级构造器。
         *
         * <p>不写的话就是 private 构造器，javac 为了允许外部类 new 它，会额外生成一个
         * 带 {@code Outer$1} 哑参数的合成构造器和对应的合成类——多一个 class 文件，
         * 与原厂类集不一致。写成包级可见就没有这个副作用。</p>
         */
        EmptyResponseBody() {
        }

        @Override
        public long contentLength() {
            return 0L;
        }

        @Override
        public MediaType contentType() {
            return null;
        }

        @Override
        public BufferedSource source() {
            return new Buffer();
        }
    }
}
