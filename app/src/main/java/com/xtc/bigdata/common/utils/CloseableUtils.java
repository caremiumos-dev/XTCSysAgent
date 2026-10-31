package com.xtc.bigdata.common.utils;

import com.xtc.bigdata.common.constants.Constants;
import com.xtc.log.LogUtil;

import java.io.Closeable;
import java.io.IOException;

/**
 * 关闭流的静默封装。
 *
 * <p>关闭失败只在调试开关打开时记日志 —— 关流失败几乎无法处理，
 * 而丢日志会掩盖真正的业务异常。</p>
 */
public class CloseableUtils {

    private static final String TAG = "CloseableUtils";

    private CloseableUtils() {
    }

    /**
     * 批量关闭，忽略单个失败。
     *
     * @param closeables 待关闭对象，可含 null
     * @param <T>        统一泛型（实际都按 Closeable 处理）
     */
    public static <T extends Closeable> void closeAllQuietly(T... closeables) {
        for (T closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    if (Constants.isDebug) {
                        LogUtil.e(TAG, e);
                    }
                }
            }
        }
    }

    /**
     * 关闭单个对象，忽略失败。
     *
     * @param closeable 待关闭对象，可为 null
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (Constants.isDebug) {
                    LogUtil.e(TAG, e);
                }
            }
        }
    }
}
