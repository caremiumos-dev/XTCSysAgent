package com.xtc.log.crash;

import android.support.annotation.Nullable;
import com.xtc.log.LogUtil;

/**
 * 全局未捕获异常处理器。先把日志落盘，再回调业务监听，最后交回系统默认处理器，
 * 保持原有的进程崩溃流程。
 */
public enum CrashHandler implements Thread.UncaughtExceptionHandler {
    Instance;

    private static final String LogTag = "Fatal";

    @Nullable
    private static CrashListener mCrashListener;

    @Nullable
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static CrashHandler getInstance() {
        return Instance;
    }

    public static void setCrashListener(CrashListener crashListener) {
        mCrashListener = crashListener;
    }

    /** 记录当前默认处理器并把自己注册为全局处理器。 */
    public void init() {
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    private boolean saveAndPrintLog(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        LogUtil.e(LogTag, throwable);
        LogUtil.close();
        return true;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        saveAndPrintLog(throwable);
        if (mCrashListener != null) {
            mCrashListener.onUncaughtException(thread, throwable);
        }
        // 原实现在此处带有 assert 语义：未注册默认处理器时直接抛断言错误。
        if (this.mDefaultHandler == null) {
            throw new AssertionError();
        }
        this.mDefaultHandler.uncaughtException(thread, throwable);
    }
}
