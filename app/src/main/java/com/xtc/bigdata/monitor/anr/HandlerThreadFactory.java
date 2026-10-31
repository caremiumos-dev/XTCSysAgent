package com.xtc.bigdata.monitor.anr;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * 采样/写日志两条后台线程的工厂。
 *
 * <p>两个线程在类加载时就起来并常驻 —— 采样要求延迟稳定，
 * 临时建线程会引入不可控的启动开销。</p>
 *
 * @deprecated 原厂已标记废弃。
 */
@Deprecated
final class HandlerThreadFactory {

    /** 采样线程。 */
    private static HandlerThreadWrapper sLoopThread = new HandlerThreadWrapper("loop");

    /** 写日志线程。 */
    private static HandlerThreadWrapper sWriteLogThread = new HandlerThreadWrapper("writer");

    /** 带 Handler 的线程包装。 */
    private static class HandlerThreadWrapper {

        /** 该线程的 Handler。 */
        private Handler handler;

        /**
         * 起线程并建 Handler。
         *
         * @param name 线程名后缀
         */
        public HandlerThreadWrapper(String name) {
            this.handler = null;
            HandlerThread handlerThread = new HandlerThread("ANR-Tracer-" + name);
            handlerThread.start();
            this.handler = new Handler(handlerThread.getLooper());
        }

        /** @return 该线程的 Handler */
        public Handler getHandler() {
            return this.handler;
        }
    }

    private HandlerThreadFactory() {
        throw new InstantiationError("Must not instantiate this class");
    }

    /** @return 采样线程的 Handler */
    public static Handler getTimerThreadHandler() {
        return sLoopThread.getHandler();
    }

    /** @return 写日志线程的 Handler */
    public static Handler getWriteLogThreadHandler() {
        return sWriteLogThread.getHandler();
    }
}
