package com.xtc.log;

import android.support.annotation.NonNull;

/**
 * 需要把调用位置信息（文件、类、方法、行号、线程）一并输出的日志接口。
 */
public interface IStackLogger extends ILogger {

    /** 一条待输出的日志，把级别、位置信息和正文打包在一起。 */
    class LogDatum {
        public final Level _logLevel;
        public final StackInfo _stackInfo;
        public final String _tag;
        public final String _message;

        public LogDatum(Level level, StackInfo stackInfo, String tag, String message) {
            this._logLevel = level;
            this._stackInfo = stackInfo;
            this._tag = tag;
            this._message = message;
        }
    }

    /** 调用位置信息。 */
    class StackInfo {
        public final String _fileName;
        public final String _className;
        public final String _methodName;
        public final int _lineNumber;
        public final int _pid;
        public final long _tid;
        public final long _mainTid;

        public StackInfo(String fileName, String className, String methodName, int lineNumber,
                         int pid, long tid, long mainTid) {
            this._fileName = fileName;
            this._className = className;
            this._methodName = methodName;
            this._lineNumber = lineNumber;
            this._pid = pid;
            this._tid = tid;
            this._mainTid = mainTid;
        }
    }

    /**
     * 输出一条带调用位置信息的日志。
     *
     * @param level     日志级别
     * @param stackInfo 调用位置
     * @param tag       标签
     * @param message   正文
     */
    void log(Level level, @NonNull StackInfo stackInfo, @NonNull String tag, @NonNull String message);
}
