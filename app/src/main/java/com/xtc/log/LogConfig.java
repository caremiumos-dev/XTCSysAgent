package com.xtc.log;

import android.content.Context;
import android.support.annotation.NonNull;
import com.xtc.log.crash.CrashHandler;

/**
 * 日志模块的运行配置，通过 {@link Builder} 链式构造。
 */
public class LogConfig {

    private String appName;
    public Context context;
    private String filePrefix;
    private boolean isPrintConsole;
    private boolean isSaveLog;
    private String logPath;
    private String module;
    private String netPropertiesPath;
    /** 日志总开关，-1 表示尚未决定。 */
    public int sLogEnabled;
    private ILogger.Level saveLevel;

    /**
     * 配置构造器。默认：落盘开启、控制台关闭、保存级别 Debug。
     */
    public static class Builder {

        private String netConfigPath;
        private boolean isSaveLog = true;
        private boolean isPrintConsole = false;
        private String logPath = "";
        private String module = "";
        private String appName = "";
        private String filePrefix = "";
        private ILogger.Level saveLevel = ILogger.Level.Debug;

        Builder() {
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder isPrintConsole(boolean printConsole) {
            this.isPrintConsole = printConsole;
            return this;
        }

        public Builder logPath(String logPath) {
            this.logPath = logPath;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder netConfigPath(String netConfigPath) {
            this.netConfigPath = netConfigPath;
            return this;
        }

        public Builder saveLog(boolean saveLog) {
            this.isSaveLog = saveLog;
            return this;
        }

        public Builder setSaveLevel(ILogger.Level level) {
            this.saveLevel = level;
            return this;
        }

        /** 用当前配置构建一个 {@link LogConfig}。 */
        public void build(@NonNull BuildMode buildMode, Object... args) {
            buildMode.build(newInstance(), args);
        }

        LogConfig newInstance() {
            return new LogConfig(this.isSaveLog, this.isPrintConsole, this.logPath, this.module,
                    this.appName,
                    "".equals(this.filePrefix) ? this.appName : this.filePrefix,
                    this.saveLevel, this.netConfigPath);
        }

        /** 构建完之后要装配到哪种运行环境。 */
        public enum BuildMode {
            /** Android 环境：装配 logcat 输出并挂上崩溃处理器。 */
            Android {
                @Override
                public void build(LogConfig logConfig, Object... args) {
                    if (logConfig.isSaveLog()) {
                        logConfig.setPrintConsole(true);
                    }
                    AndroidLogger logger = new AndroidLogger(logConfig);
                    logger.setLogConfig(logConfig);
                    Log.setLogger(logger);
                    CrashHandler.Instance.init();
                }
            },
            /** 纯 Java 环境：输出到标准输出。 */
            Java {
                @Override
                public void build(LogConfig logConfig, Object... args) {
                    Log.setLogger(StandardOutLogger.Instance);
                }
            };

            public abstract void build(LogConfig logConfig, Object... args);
        }
    }

    private LogConfig(boolean saveLog, boolean printConsole, String logPath, String module,
                      String appName, String filePrefix, ILogger.Level level, String netPropertiesPath) {
        this.sLogEnabled = -1;
        this.isSaveLog = saveLog;
        this.isPrintConsole = printConsole;
        this.logPath = logPath;
        this.module = module;
        this.appName = appName;
        this.filePrefix = filePrefix;
        this.saveLevel = level;
        this.netPropertiesPath = netPropertiesPath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAppName() {
        return this.appName;
    }

    public String getFilePrefix() {
        return this.filePrefix;
    }

    public String getLogPath() {
        return this.logPath;
    }

    public String getModule() {
        return this.module;
    }

    public String getNetConfigPath() {
        return this.netPropertiesPath;
    }

    public ILogger.Level getSaveLevel() {
        return this.saveLevel;
    }

    public boolean isPrintConsole() {
        return this.isPrintConsole;
    }

    public boolean isSaveLog() {
        return this.isSaveLog;
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    void setPrintConsole(boolean printConsole) {
        this.isPrintConsole = printConsole;
    }

    public void setSaveLevel(ILogger.Level level) {
        this.saveLevel = level;
    }

    public void setSaveLog(boolean saveLog) {
        this.isSaveLog = saveLog;
    }
}
