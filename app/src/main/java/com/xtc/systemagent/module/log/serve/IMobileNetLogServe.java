package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * 移动网络（modem 侧网络日志）采集服务。
 */
public interface IMobileNetLogServe {
    /** 补抓上一次还没结束的网络日志。 */
    void checkLastMoblieNetWorkLog();

    /** 结束采集。 */
    void endMobileNetLog();

    /** 按策略开始采集。 */
    void startMobileNetLog(List<LogStrategy> strategies);
}
