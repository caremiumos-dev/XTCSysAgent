package com.tencent.mars.xlog;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * xlog 的日志接口。宿主可以用它替换掉默认的 logcat 输出实现，
 * 从而获得落盘、加密、压缩等能力。
 */
public interface IXlog {

    int APPENDER_MODE_ASYNC = 0;
    int APPENDER_MODE_SYNC = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({APPENDER_MODE_ASYNC, APPENDER_MODE_SYNC})
    @interface AppenderMode {
    }

    void appenderClose();

    void appenderFlush(boolean isSync);

    int getLogLevel();

    void log(int level, String tag, String filename, String functionName, int line,
             int pid, long tid, long mainTid, String message);

    void setLogLevel(int level);
}
