package com.xtc.log.util;

import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;
import com.xtc.log.ILogger;
import com.xtc.log.IStackLogger;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

/**
 * 栈与调用位置相关的工具方法。
 */
public final class StackUtils {

    private StackUtils() {
        throw new AssertionError();
    }

    /** 把调用位置拼进消息体后打印到控制台。 */
    public static void consoleLog(ILogger.Level level, IStackLogger.StackInfo stackInfo,
                                  String tag, String message) {
        level.consoleLog(tag, generateConsoleMessage(stackInfo, message));
    }

    /**
     * 生成 "{短类名.方法名-行号} 正文" 形式的控制台消息。
     */
    public static String generateConsoleMessage(IStackLogger.StackInfo stackInfo, String message) {
        String className = stackInfo._className;
        return "{" + className.substring(className.lastIndexOf('.') + 1) + "."
                + stackInfo._methodName + "-" + stackInfo._lineNumber + "} " + message;
    }

    /** 取当前线程栈的指定层，越界时退化为栈底。 */
    @NonNull
    private static StackTraceElement getStackTraceOfPosition(int position) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int length = stackTrace.length;
        return length > position ? stackTrace[position] : stackTrace[length - 1];
    }

    /**
     * 异常堆栈字符串。{@link UnknownHostException} 被视为无意义的网络抖动，
     * 直接返回空串以免污染日志。
     */
    public static String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownHostException) {
                return "";
            }
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }

    /** 以当前进程和主线程为基准构造调用位置信息。 */
    public static IStackLogger.StackInfo makeAndroidStackInfo(int position) {
        return makeStackInfo(position, Process.myPid(), Looper.getMainLooper().getThread().getId());
    }

    /** 构造指定栈深度的调用位置信息。 */
    @NonNull
    public static IStackLogger.StackInfo makeStackInfo(int position, int pid, long mainTid) {
        StackTraceElement element = getStackTraceOfPosition(position);
        return new IStackLogger.StackInfo(element.getFileName(), element.getClassName(),
                element.getMethodName(), element.getLineNumber(), pid,
                Thread.currentThread().getId(), mainTid);
    }
}
