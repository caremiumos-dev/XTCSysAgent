package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * tcpdump 抓包服务。
 */
public interface ITcpDumpServe {
    /** 取消当前抓包。 */
    void cancelTcpDump();

    /** 按策略列表取消。 */
    void cancelTcpDump(List<LogStrategy> strategies);

    /** 补抓上一次的抓包。 */
    void checkLastTcpDump();

    /** 通话结束。 */
    void dealCallIdle();

    /** 通话开始。 */
    void dealCallRinging();

    /** 释放资源。 */
    void onDestroy();

    /** 本地触发的抓包。 */
    void startLocalTcpDump(LogStrategy strategy, boolean independent);

    /** 按策略列表开始抓包。 */
    void startTcpDump(List<LogStrategy> strategies, boolean independent);

    /** 本地 modem 日志抓取完成后的回调。 */
    void whenLocalModemLogDone();

    /** modem 日志准备上传时的回调。 */
    void whenModemLogPrepareToUpload();

    /** modem 日志上传成功后的回调。 */
    void whenUploadModemLogSucceed(boolean success);
}
