package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.ExtraMes;
import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * modem 日志采集服务。
 */
public interface IModemLogServe {
    /** 取消当前的 modem 日志抓取。 */
    void cancelModemLog();

    /** 按策略列表取消。 */
    void cancelModemLog(List<LogStrategy> strategies);

    /** 补抓上一次的 modem 日志。 */
    void checkLastModemLog(boolean immediately);

    /** 通话结束：停止抓取。 */
    void dealCallIdle();

    /** 通话开始：准备抓取。 */
    void dealCallRinging();

    /** 立即抓取（本地触发）。 */
    void fetchingModemLog(ExtraMes extraMes);

    /** 按策略列表抓取。 */
    void fetchingModemLog(List<LogStrategy> strategies);

    /** 释放资源。 */
    void onDestroy();

    /** 按单条策略开始抓取。 */
    void startModemLog(LogStrategy strategy, int fetchingTime);
}
