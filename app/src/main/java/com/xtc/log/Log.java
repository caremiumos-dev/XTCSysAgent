package com.xtc.log;

import android.support.annotation.NonNull;
import com.xtc.log.util.StackUtils;

/**
 * 全局日志入口。真正的输出目标由 {@link #setLogger(ILogger)} 注入。
 */
public class Log {

    @NonNull
    private static ILogger sLoggerImp = EmptyLogger.Instance;

    public static void d(@NonNull String tag, @NonNull String message) {
        sLoggerImp.log(ILogger.Level.Debug, tag, message);
    }

    public static void d(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Debug, tag, message + '\n' + StackUtils.getStackTraceString(throwable));
    }

    public static void e(@NonNull String tag, @NonNull String message) {
        sLoggerImp.log(ILogger.Level.Error, tag, message);
    }

    public static void e(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Error, tag, message + '\n' + StackUtils.getStackTraceString(throwable));
    }

    public static ILogger getLogger() {
        return sLoggerImp;
    }

    public static void i(@NonNull String tag, @NonNull String message) {
        sLoggerImp.log(ILogger.Level.Info, tag, message);
    }

    public static void i(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Info, tag, message + '\n' + StackUtils.getStackTraceString(throwable));
    }

    public static void setLogger(ILogger logger) {
        sLoggerImp = logger;
    }

    public static void v(@NonNull String tag, @NonNull String message) {
        sLoggerImp.log(ILogger.Level.Verbose, tag, message);
    }

    public static void v(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Verbose, tag, message + '\n' + StackUtils.getStackTraceString(throwable));
    }

    public static void w(@NonNull String tag, @NonNull String message) {
        sLoggerImp.log(ILogger.Level.Warning, tag, message);
    }

    public static void w(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Warning, tag, message + '\n' + StackUtils.getStackTraceString(throwable));
    }

    public static void w(@NonNull String tag, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Warning, tag, StackUtils.getStackTraceString(throwable));
    }

    public static void wtf(@NonNull String tag, @NonNull String message) {
        sLoggerImp.log(ILogger.Level.Assert, tag, message);
    }

    public static void wtf(@NonNull String tag, @NonNull String message, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Assert, tag, message + '\n' + StackUtils.getStackTraceString(throwable));
    }

    public static void wtf(@NonNull String tag, @NonNull Throwable throwable) {
        sLoggerImp.log(ILogger.Level.Assert, tag, StackUtils.getStackTraceString(throwable));
    }
}
