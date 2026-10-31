package com.xtc.httplib.okhttp;

import android.support.annotation.NonNull;
import com.xtc.httplib.annotation.Timeout;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.Response;
import retrofit2.Invocation;

/**
 * 按服务方法上的 {@link Timeout} 注解为单次请求覆盖超时（拦截链最内层之前的第一个）。
 *
 * <p>{@code retrofit2.Invocation} 是 Retrofit 2.9 在发起调用时塞进 Request 的 tag，
 * 里面有被调用的 {@code Method}，于是能拿到方法上的注解。不是通过 Retrofit 发出的请求
 * （例如直接用 {@code HttpClient#newCall}）拿不到 tag，就原样放行。</p>
 *
 * <p>覆盖顺序与原厂一致：先处理 {@code all}，再用 {@code connect/read/write} 单独覆盖，
 * 所以"all + 某个单项"时以单项为准。</p>
 */
public class TimeoutInterceptor implements Interceptor {

    @Override
    @NonNull
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Invocation invocation = chain.request().tag(Invocation.class);
        if (invocation == null) {
            return chain.proceed(chain.request());
        }
        Timeout timeout = invocation.method().getAnnotation(Timeout.class);
        if (timeout == null) {
            return chain.proceed(chain.request());
        }
        if (timeout.all() >= 0) {
            chain = chain.withConnectTimeout(timeout.all(), TimeUnit.MILLISECONDS)
                    .withReadTimeout(timeout.all(), TimeUnit.MILLISECONDS)
                    .withWriteTimeout(timeout.all(), TimeUnit.MILLISECONDS);
        }
        if (timeout.connect() >= 0) {
            chain = chain.withConnectTimeout(timeout.connect(), TimeUnit.MILLISECONDS);
        }
        if (timeout.read() >= 0) {
            chain = chain.withReadTimeout(timeout.read(), TimeUnit.MILLISECONDS);
        }
        if (timeout.write() >= 0) {
            chain = chain.withWriteTimeout(timeout.write(), TimeUnit.MILLISECONDS);
        }
        return chain.proceed(chain.request());
    }
}
