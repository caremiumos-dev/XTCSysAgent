package com.xtc.httplib.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * 全局主线程 / 后台 Handler。
 *
 * <p>后台线程的优先级用 {@code Process.THREAD_PRIORITY_BACKGROUND}(10)，
 * 与原件一致。</p>
 */
public class HandlerUtil {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Handler backgroundHandler;

    static {
        HandlerThread handlerThread = new HandlerThread("background_thread", 10);
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    private HandlerUtil() {
    }

    public static void runOnBackground(Runnable runnable) {
        backgroundHandler.post(runnable);
    }

    public static void runOnBackgroundDelay(Runnable runnable, long delayMillis) {
        backgroundHandler.postDelayed(runnable, delayMillis);
    }

    public static void runOnUIThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public static void runOnUIThreadDelay(Runnable runnable, long delayMillis) {
        mainHandler.postDelayed(runnable, delayMillis);
    }
}
