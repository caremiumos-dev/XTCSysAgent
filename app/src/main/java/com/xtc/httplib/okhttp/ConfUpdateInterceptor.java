package com.xtc.httplib.okhttp;

import android.content.Context;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.confupdate.ConfUpdatesInfoManager;
import com.xtc.httplib.util.HandlerUtil;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 配置变更拦截器：读响应头里的 {@code Conf-Updates} / {@code Conf-Updates-V}，
 * 解析后决定要不要通知业务侧重新拉配置。
 *
 * <p>为什么要放到后台线程：解析 + 广播不该占用 OkHttp 的 IO 线程，
 * 而 {@link ConfUpdatesInfoManager#updateConf} 里可能触发广播（跨进程 IPC）。</p>
 *
 * <p>返回值原样透传——它只"顺手"看一眼响应头，不改变响应内容，
 * 也不关心 HTTP 状态码（失败响应同样可能带更新头）。</p>
 */
public class ConfUpdateInterceptor extends BaseInterceptor {

    /** 配置更新位串（二进制字符串）所在的响应头。 */
    public static final String HEADER_CONF_UPDATES = "Conf-Updates";

    /** 配置项版本号列表 + 延迟分钟数所在的响应头。 */
    public static final String HEADER_CONF_UPDATES_VERSION = "Conf-Updates-V";

    private static final String TAG = LogTag.tag("ConfUpdateInterceptor");

    ConfUpdateInterceptor(Context context) {
        super(context);
    }

    /**
     * 把解析与派发丢到后台线程。
     *
     * <p>{@code response} 必须作为 final 捕获：OkHttp 的 Response 在链走后仍可读头，
     * 但不能依赖它一直有效，所以这里只读 head。</p>
     */
    private void dealConfigurationUpdate(final Response response) throws IOException {
        HandlerUtil.runOnBackground(new Runnable() {
            @Override
            public void run() {
                ConfUpdatesInfoManager.getInstance(ConfUpdateInterceptor.this.context)
                        .updateConf(response.header(HEADER_CONF_UPDATES),
                                response.header(HEADER_CONF_UPDATES_VERSION));
                ConfUpdatesInfoManager.getInstance(ConfUpdateInterceptor.this.context)
                        .dispathUpdatesToClient();
            }
        });
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        if (response != null) {
            dealConfigurationUpdate(response);
        }
        return response;
    }
}
