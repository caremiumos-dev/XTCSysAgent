package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * 通话失败（volte）日志采集服务。
 */
public interface ICallFailureServer {
    /** 补抓上一次还没处理完的通话日志。 */
    void checkLastCallLog();

    /** 释放资源。 */
    void onDestroy();

    /** 处理一批通话日志策略。 */
    void startDealCallLog(List<LogStrategy> strategies, boolean immediately);

    /** 抓取单个通话日志。 */
    void startFetchCallLog(LogStrategy strategy, int fetchingTime, boolean immediately);
}
