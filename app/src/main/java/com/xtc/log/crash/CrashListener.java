package com.xtc.log.crash;

/**
 * 未捕获异常监听。业务侧可以在这里把崩溃上报到自己埋点系统。
 */
public interface CrashListener {
    void onUncaughtException(Thread thread, Throwable throwable);
}
