package com.xtc.systemagent.net;

import android.content.Context;
import com.xtc.httplib.net.HttpRxJavaCallback;
import com.xtc.httplib.net.HttpServiceProxy;
import com.xtc.systemagent.net.bean.DelFileFeedParam;
import com.xtc.systemagent.net.bean.RpmStateParam;
import com.xtc.systemagent.net.bean.SysConfigCbParam;
import com.xtc.systemagent.net.http.ISysHttp;
import rx.Observable;

/**
 * 系统配置类接口的 Retrofit 代理。
 *
 * <p>与 {@link LogHttpProxy} 不同，这里的 baseUrl 由调用方传入。</p>
 */
public class SysHttpProxy extends HttpServiceProxy {

    private final ISysHttp sysHttp;

    public SysHttpProxy(Context context, String baseUrl) {
        super(context);
        this.sysHttp = (ISysHttp) this.httpClient.request(baseUrl, ISysHttp.class);
    }

    /** 上报"删除文件"反馈状态。 */
    public Observable<String> feedDelFileState(DelFileFeedParam param) {
        return this.sysHttp.feedDelFileState(param).map(new HttpRxJavaCallback<String>());
    }

    /** 上报 RPM 状态。 */
    public Observable<String> feedRpmState(RpmStateParam param) {
        return this.sysHttp.feedRpmState(param).map(new HttpRxJavaCallback<String>());
    }

    /** 上报系统配置状态。 */
    public Observable<String> feedSysConfigState(SysConfigCbParam param) {
        return this.sysHttp.feedSysConfigState(param).map(new HttpRxJavaCallback<String>());
    }
}
