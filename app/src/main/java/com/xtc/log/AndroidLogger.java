package com.xtc.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 输出到 logcat。是否真正打印取决于 {@link LogConfig} 里的控制台开关和保存级别。
 */
class AndroidLogger implements ILogger {

    @Nullable
    private LogConfig logConfig;

    /** 预留给文件落盘的包名，当前实现未使用。 */
    @Nullable
    private String packageName;

    AndroidLogger(@NonNull LogConfig logConfig) {
        this.logConfig = logConfig;
    }

    @Override
    public void log(Level level, @NonNull String tag, @NonNull String message) {
        if (this.logConfig != null
                && this.logConfig.isPrintConsole()
                && level.ordinal() >= this.logConfig.getSaveLevel().ordinal()) {
            level.consoleLog(tag, message);
        }
    }

    public void setLogConfig(@NonNull LogConfig logConfig) {
        this.logConfig = logConfig;
    }
}
