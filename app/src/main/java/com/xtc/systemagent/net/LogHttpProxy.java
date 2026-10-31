package com.xtc.systemagent.net;

import android.content.Context;
import com.xtc.httplib.net.BaseUrlManager;
import com.xtc.httplib.net.HttpRxJavaCallback;
import com.xtc.httplib.net.HttpServiceProxy;
import com.xtc.systemagent.net.bean.CallOtherParms;
import com.xtc.systemagent.net.bean.LogStateParams;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.LogStrategyParam;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import com.xtc.systemagent.net.http.ILogHttp;
import java.util.List;
import rx.Observable;

/**
 * 日志采集接口的 Retrofit 代理。
 *
 * <p>每个方法都套一层 {@link HttpRxJavaCallback}，把统一响应信封
 * {@code NetBaseResult} 拆成业务数据；失败时抛
 * {@code WatchHttpResultException}。</p>
 */
public class LogHttpProxy extends HttpServiceProxy {

    private final ILogHttp logHttp;

    public LogHttpProxy(Context context) {
        super(context);
        this.logHttp = (ILogHttp) this.httpClient.request(
                BaseUrlManager.getDefaultUrl(context), ILogHttp.class);
    }

    /** 上报采集状态。 */
    public Observable<String> collectStateUpdate(LogStateParams params) {
        return this.logHttp.collectStateUpdate(params).map(new HttpRxJavaCallback<String>());
    }

    /** 拉取采集策略。 */
    public Observable<List<LogStrategy>> getCollectStrategy(LogStrategyParam param) {
        return this.logHttp.getCollectStrategy(param)
                .map(new HttpRxJavaCallback<List<LogStrategy>>());
    }

    /** 上报日志上传完成。 */
    public Observable<Object> notifyUploadSuccess(UploadNotifyParam param) {
        return this.logHttp.notifyUploadSuccess(param).map(new HttpRxJavaCallback<Object>());
    }

    /** 通知服务端查询其他手表的通话日志。 */
    public Observable<Object> openOtherWatchCallLog(CallOtherParms params) {
        return this.logHttp.openOtherWatchCallLog(params).map(new HttpRxJavaCallback<Object>());
    }
}
