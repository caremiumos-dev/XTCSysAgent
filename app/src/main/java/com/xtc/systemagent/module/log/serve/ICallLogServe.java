package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.module.log.serve.impl.CallLogServeImpl;
import com.xtc.systemagent.net.bean.CallOtherParms;

/**
 * 通话日志服务：负责"查询其他手表通话日志"。
 */
public interface ICallLogServe {
    void openOtherWatchCallLog(CallOtherParms params, CallLogServeImpl.OtherCallBack callback);
}
