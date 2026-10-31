package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * modem dump 采集服务。
 */
public interface IModemDumpServe {
    /** 释放资源。 */
    void onDestroy();

    /** 按策略决定是否开始抓取 modem dump。 */
    void startModemDump(List<LogStrategy> strategies);
}
