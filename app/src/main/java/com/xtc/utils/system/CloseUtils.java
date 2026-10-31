package com.xtc.utils.system;

import java.io.Closeable;
import java.io.IOException;

/**
 * 资源关闭工具。两个方法行为差异只在"是否把异常打出来"。
 */
public class CloseUtils {

    private CloseUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /** 逐个关闭，异常打印堆栈。 */
    public static void closeQuietlyWithLog(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        try {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 逐个关闭，异常完全吞掉。 */
    public static void closeSilently(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        try {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (IOException e) {
            // 故意留空：静默关闭。
        }
    }
}
