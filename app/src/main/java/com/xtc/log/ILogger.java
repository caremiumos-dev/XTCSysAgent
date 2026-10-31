package com.xtc.log;

import android.support.annotation.NonNull;

/**
 * 日志输出接口。实现类决定日志最终落到哪里（控制台 / 文件 / 空实现）。
 */
public interface ILogger {

    /**
     * 日志级别。级别本身也负责"控制台输出"这一动作，因此每个枚举常量都带着
     * 一份对应的 {@link android.util.Log} 调用。
     */
    enum Level {
        Verbose("V") {
            @Override
            public void consoleLog(String tag, String message) {
                android.util.Log.v(tag, message);
            }
        },
        Debug("D") {
            @Override
            public void consoleLog(String tag, String message) {
                android.util.Log.d(tag, message);
            }
        },
        Info("I") {
            @Override
            public void consoleLog(String tag, String message) {
                android.util.Log.i(tag, message);
            }
        },
        Warning("W") {
            @Override
            public void consoleLog(String tag, String message) {
                android.util.Log.w(tag, message);
            }
        },
        Error("E") {
            @Override
            public void consoleLog(String tag, String message) {
                android.util.Log.e(tag, message);
            }
        },
        Assert("A") {
            @Override
            public void consoleLog(String tag, String message) {
                android.util.Log.wtf(tag, message);
            }
        },
        None("") {
            @Override
            public void consoleLog(String tag, String message) {
                // 关闭控制台输出时使用，什么都不做。
            }
        };

        /** 显示在日志头部的单字母级别名。 */
        public final String consoleName;

        Level(String consoleName) {
            this.consoleName = consoleName;
        }

        /**
         * 按下标还原级别。
         *
         * @throws NoSuchLevelException 下标越界时抛出
         */
        public static Level intToLevel(int index) throws NoSuchLevelException {
            try {
                return values()[index];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new NoSuchLevelException(index);
            }
        }

        /** 把消息打印到 logcat。 */
        public abstract void consoleLog(String tag, String message);

        /** 非法的级别下标。 */
        public static class NoSuchLevelException extends Exception {
            NoSuchLevelException(int index) {
                super("没有这种级别的日志等级：" + index);
            }
        }
    }

    /**
     * 输出一条日志。
     *
     * @param level   日志级别
     * @param tag     标签
     * @param message 正文
     */
    void log(Level level, @NonNull String tag, @NonNull String message);
}
