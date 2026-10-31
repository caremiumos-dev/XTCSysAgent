package com.xtc.httplib.net;

import android.content.Context;
import com.xtc.httplib.HttpClient;
import com.xtc.httplib.HttpManager;

/**
 * 所有 Retrofit 服务代理的基类：持有 ApplicationContext 和全局 HttpClient。
 */
public abstract class HttpServiceProxy {

    protected Context context;
    protected HttpClient httpClient;

    public HttpServiceProxy(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext == null ? context : applicationContext;
        this.httpClient = HttpManager.getInstance(context).getHttpClient();
    }
}
