package com.xtc.httplib.okhttp;

import android.content.Context;
import com.xtc.httplib.LogTag;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 重试拦截器（拦截链最末端）。
 *
 * <p>线上版本里它只是原样放行：真正的"重试"由 OkHttp 自己的
 * {@code retryOnConnectionFailure} 与上层业务负责。保留这个空壳是因为</p>
 * <ul>
 *   <li>拦截器的**顺序**参与行为（日志顺序、Context 传递）；</li>
 *   <li>原厂升级时可能在这个位置插入逻辑，删除会让后续差异变大。</li>
 * </ul>
 */
public class RetryInterceptor extends BaseInterceptor {

    private static final String TAG = LogTag.tag("RetryInterceptor");

    RetryInterceptor(Context context) {
        super(context);
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        return chain.proceed(chain.request());
    }
}
