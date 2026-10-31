package com.xtc.bigdata.collector.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * 主线程 Handler。
 *
 * <p>类初始化时就绑定主 Looper —— 所以任何调用方都必须在主线程之外
 * 也能安全使用（{@code Looper.getMainLooper()} 本身是静态的）。</p>
 */
public class MainHandlerUtil {

    /** 主线程 Handler。 */
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    private MainHandlerUtil() {
    }

    /**
     * 立刻投递到主线程。
     *
     * @param runnable 任务
     */
    public static void post(Runnable runnable) {
        mainHandler.post(runnable);
    }

    /**
     * 延迟投递到主线程。
     *
     * @param runnable    任务
     * @param delayMillis 延迟毫秒
     */
    public static void postDelay(Runnable runnable, long delayMillis) {
        mainHandler.postDelayed(runnable, delayMillis);
    }
}
