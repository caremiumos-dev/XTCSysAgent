package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * WiFi 日志采集服务。
 */
public interface IWifiLogServe {
    /** 结束采集。 */
    void endWifiLog();

    /** 按策略开始采集。 */
    void startWifiLog(List<LogStrategy> strategies);
}
