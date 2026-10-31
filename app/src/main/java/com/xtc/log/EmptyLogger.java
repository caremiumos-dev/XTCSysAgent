package com.xtc.log;

import android.support.annotation.NonNull;

/**
 * 空实现：日志开关关闭时使用，所有输出都被丢弃。
 */
public enum EmptyLogger implements ILogger {
    Instance;

    @Override
    public void log(Level level, @NonNull String tag, @NonNull String message) {
        // 故意留空：日志被完全关闭。
    }
}
