package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * adsp dump 采集服务。
 */
public interface IAdspDumpServe {
    /** 释放资源。 */
    void onDestroy();

    /** 按策略列表决定是否开始抓取 adsp dump。 */
    void startAdspDump(List<LogStrategy> strategies);
}
