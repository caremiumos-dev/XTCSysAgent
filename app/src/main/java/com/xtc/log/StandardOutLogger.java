package com.xtc.log;

import android.support.annotation.NonNull;
import com.xtc.log.util.StackUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 输出到标准输出，用于纯 Java 环境（单元测试、后台工具）调试。
 */
public enum StandardOutLogger implements IStackLogger {
    Instance;

    /** makeStackInfo 到真实调用者之间的栈深度偏移。 */
    private static final int STACK_POSITION = 6;

    public String generateLogHeader(Level level) {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.CHINA)
                .format(Calendar.getInstance(Locale.CHINA).getTime())
                + " " + Thread.currentThread().getId() + " " + level.consoleName + "/";
    }

    @Override
    public void log(Level level, @NonNull StackInfo stackInfo, @NonNull String tag, @NonNull String message) {
        System.out.println(generateLogHeader(level) + tag + ": "
                + StackUtils.generateConsoleMessage(stackInfo, message));
    }

    @Override
    public void log(Level level, @NonNull String tag, @NonNull String message) {
        log(level, StackUtils.makeStackInfo(STACK_POSITION, 0, 0L), tag, message);
    }
}
