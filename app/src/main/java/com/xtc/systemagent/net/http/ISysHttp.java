package com.xtc.systemagent.net.http;

import com.xtc.httplib.bean.NetBaseResult;
import com.xtc.systemagent.net.bean.DelFileFeedParam;
import com.xtc.systemagent.net.bean.RpmStateParam;
import com.xtc.systemagent.net.bean.SysConfigCbParam;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

/**
 * 系统能力相关的服务端接口（删文件、RPM、产线配置）。
 */
public interface ISysHttp {

    /** 上报"删除文件"指令的回执。 */
    @POST("/watch-func/push/platform/cleanWatchCallback")
    Observable<NetBaseResult<String>> feedDelFileState(@Body DelFileFeedParam param);

    /** 上报 RPM 调试模式状态。 */
    @POST("/watch-func/rpmUp/up")
    Observable<NetBaseResult<String>> feedRpmState(@Body RpmStateParam param);

    /** 上报产线配置下发回执。 */
    @POST("/watch-func/productionLineUp/up")
    Observable<NetBaseResult<String>> feedSysConfigState(@Body SysConfigCbParam param);
}
