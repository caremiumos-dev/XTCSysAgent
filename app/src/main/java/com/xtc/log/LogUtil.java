package com.xtc.log;

import android.support.annotation.NonNull;
import com.tencent.mars.xlog.IXlog;

/**
 * 对 {@link Log} 的静态包装，并额外提供针对 xlog 实现的刷新/关闭/级别设置能力。
 */
public class LogUtil {

    /** 关闭 xlog 的文件写入。 */
    public static void close() {
        ILogger logger = Log.getLogger();
        if (logger instanceof IXlog) {
            ((IXlog) logger).appenderClose();
        }
    }

    /** 刷新 xlog 缓冲区。 */
    public static void flush() {
        ILogger logger = Log.getLogger();
        if (logger instanceof IXlog) {
            ((IXlog) logger).appenderFlush(false);
        }
    }

    /** 设置 xlog 的日志级别。 */
    public static void setLogLevel(ILogger.Level level) {
        ILogger logger = Log.getLogger();
        if (logger instanceof IXlog) {
            ((IXlog) logger).setLogLevel(level.ordinal());
        }
    }

    /** 已废弃：不带 tag 的输出被忽略。 */
    @Deprecated
    public static void d(String message) {
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
    }

    public static void d(String tag, String message, Throwable throwable) {
        Log.d(tag, message, throwable);
    }

    /** 已废弃：不带 tag 的输出被忽略。 */
    @Deprecated
    public static void e(String message) {
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }

    public static void e(String tag, Throwable throwable) {
        Log.e(tag, "", throwable);
    }

    /** 已废弃：直接输出异常被忽略。 */
    @Deprecated
    public static void e(Throwable throwable) {
    }

    /** 已废弃：不带 tag 的输出被忽略。 */
    @Deprecated
    public static void i(String message) {
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
    }

    public static void i(String tag, String message, Throwable throwable) {
        Log.i(tag, message, throwable);
    }

    /** 已废弃：不带 tag 的输出被忽略。 */
    @Deprecated
    public static void v(String message) {
    }

    public static void v(String tag, String message) {
        Log.v(tag, message);
    }

    public static void v(String tag, String message, Throwable throwable) {
        Log.v(tag, message, throwable);
    }

    /** 已废弃：不带 tag 的输出被忽略。 */
    @Deprecated
    public static void w(String message) {
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
    }

    public static void w(String tag, String message, Throwable throwable) {
        Log.w(tag, message, throwable);
    }

    public static void w(String tag, Throwable throwable) {
        Log.w(tag, throwable);
    }

    /** 已废弃：不带 tag 的输出被忽略。 */
    @Deprecated
    public static void wtf(String message) {
    }

    public static void wtf(String tag, String message) {
        Log.wtf(tag, message);
    }

    public static void wtf(String tag, String message, Throwable throwable) {
        Log.wtf(tag, message, throwable);
    }

    public static void wtf(@NonNull String tag, @NonNull Throwable throwable) {
        Log.wtf(tag, throwable);
    }
}
