package com.xtc.systemagent.net.http;

import com.xtc.httplib.bean.NetBaseResult;
import com.xtc.systemagent.net.bean.CallOtherParms;
import com.xtc.systemagent.net.bean.LogStateParams;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.LogStrategyParam;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import java.util.List;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import rx.Observable;

/**
 * 日志采集相关的服务端接口。
 */
public interface ILogHttp {

    /** 上报采集状态。 */
    @PUT("/log/collect/state/update")
    Observable<NetBaseResult<String>> collectStateUpdate(@Body LogStateParams params);

    /** 拉取采集策略。 */
    @POST("/log/collect/strategy/get")
    Observable<NetBaseResult<List<LogStrategy>>> getCollectStrategy(@Body LogStrategyParam param);

    /** 上报日志上传完成。 */
    @PUT("/log/collect/add")
    Observable<NetBaseResult<Object>> notifyUploadSuccess(@Body UploadNotifyParam param);

    /** 通知服务端查询其他手表的通话日志。 */
    @POST("/log/collect/notify/add")
    Observable<NetBaseResult<Object>> openOtherWatchCallLog(@Body CallOtherParms params);
}
