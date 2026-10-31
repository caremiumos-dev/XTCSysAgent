package com.xtc.bigdata.monitor.anr;

import android.annotation.TargetApi;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.xtc.log.LogUtil;

import java.lang.reflect.Method;

/**
 * 独立的 ANR 看门狗线程（Builder 方式配置）。
 *
 * <p>原理：每 {@code timeout} 毫秒往主线程队列<b>队首</b>塞一个空任务，
 * 然后 {@code wait} 同样时长。如果任务没执行完（{@code isBlocked()}），
 * 说明主线程被卡住了，此时抓主线程堆栈 + CPU 信息回调出去。</p>
 *
 * <p>它和 {@code WatchDogs} 是两代实现：这个只抓主线程，功能更简单，
 * 也被标了 {@code @Deprecated}。</p>
 *
 * @deprecated 原厂已标记废弃，替代者是 {@code WatchDogs}。
 */
@Deprecated
public class ANRWatchDog extends Thread {

    private static final String TAG = "ANRWatchDog";

    /** 主线程探测器。 */
    private AnrChecker anrChecker;

    /** 卡顿回调。 */
    private AnrListener anrListener;

    /** 反射出来的 {@code ProcessCpuTracker} 实例。 */
    private Object cpuTrackerObj;

    /** 是否忽略调试器。 */
    private boolean ignoreDebugger;

    /** 主线程 Handler。 */
    private Handler mainHandler;

    /** 卡顿阈值（毫秒）。 */
    private int timeout;

    /** 反射出来的 {@code update()} 方法。 */
    private Method updateMethod;

    /** 往主线程塞空任务并判断是否按时完成。 */
    private class AnrChecker implements Runnable {

        /** 任务完成时刻。 */
        private long executeTime;

        /** 任务是否已完成。 */
        private boolean mCompleted;

        /** 任务投递时刻。 */
        private long mStartTime;

        /**
         * 主线程是否被卡住。
         *
         * @return 未完成、或首尾耗时超过 5 秒
         */
        boolean isBlocked() {
            return !this.mCompleted || this.executeTime - this.mStartTime >= 5000;
        }

        @Override
        public void run() {
            synchronized (ANRWatchDog.this) {
                this.mCompleted = true;
                this.executeTime = SystemClock.uptimeMillis();
            }
        }

        /** 投递一次探测任务。 */
        void schedule() {
            this.mCompleted = false;
            this.mStartTime = SystemClock.uptimeMillis();
            ANRWatchDog.this.mainHandler.postAtFrontOfQueue(this);
        }
    }

    /** 卡顿回调。 */
    public interface AnrListener {

        /**
         * 发生卡顿。
         *
         * @param stackTrace 主线程堆栈
         */
        void onAnrHappened(String stackTrace);
    }

    /** 构造器。 */
    public static class Builder {

        /** 回调。 */
        AnrListener anrListener;

        /** 是否忽略调试器。 */
        boolean ignoreDebugger;

        /** 卡顿阈值。 */
        int timeout;

        /**
         * @param anrListener 回调
         * @return this
         */
        public Builder anrListener(AnrListener anrListener) {
            this.anrListener = anrListener;
            return this;
        }

        /** @return 构建好的看门狗 */
        public ANRWatchDog build() {
            return new ANRWatchDog(this);
        }

        /**
         * @param ignoreDebugger 是否忽略调试器
         * @return this
         */
        public Builder ignoreDebugger(boolean ignoreDebugger) {
            this.ignoreDebugger = ignoreDebugger;
            return this;
        }

        /**
         * @param timeout 卡顿阈值毫秒
         * @return this
         */
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
    }

    private ANRWatchDog(Builder builder) {
        super("ANR-WatchDog-Thread");
        this.timeout = 5000;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.anrChecker = new AnrChecker();
        this.timeout = builder.timeout;
        this.ignoreDebugger = builder.ignoreDebugger;
        this.anrListener = builder.anrListener;
    }

    /**
     * 反射取系统 CPU 明细。
     *
     * @return CPU 明细文本；失败返回空串
     */
    private String getCpuInfo() {
        try {
            if (this.cpuTrackerObj == null) {
                this.cpuTrackerObj = Class.forName("com.android.internal.os.ProcessCpuTracker")
                        .getConstructor(Boolean.TYPE).newInstance(false);
                this.cpuTrackerObj.getClass().getMethod("init", new Class[0]).invoke(this.cpuTrackerObj, new Object[0]);
                this.updateMethod = this.cpuTrackerObj.getClass().getMethod("update", new Class[0]);
            } else {
                this.updateMethod.invoke(this.cpuTrackerObj, new Object[0]);
            }
            synchronized (ANRWatchDog.class) {
                this.cpuTrackerObj.wait(500L);
            }
            this.updateMethod.invoke(this.cpuTrackerObj, new Object[0]);
            return (String) this.cpuTrackerObj.getClass().getMethod("printCurrentState", Long.TYPE)
                    .invoke(this.cpuTrackerObj, Long.valueOf(SystemClock.uptimeMillis()));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return "";
        }
    }

    /**
     * 抓主线程堆栈。
     *
     * @return 堆栈文本（最多的 100KB）
     */
    private String getStackTraceInfo() {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement stackTraceElement : Looper.getMainLooper().getThread().getStackTrace()) {
            builder.append(stackTraceElement.toString()).append("\r\n");
        }
        return builder.substring(0, builder.length() < 102400 ? builder.length() : 102400);
    }

    @Override
    @TargetApi(16)
    public void run() {
        LogUtil.d(TAG, "start watch anr...");
        Process.setThreadPriority(10);
        while (!isInterrupted()) {
            synchronized (this) {
                this.anrChecker.schedule();
                long waitTime = this.timeout;
                long startTime = SystemClock.uptimeMillis();
                while (waitTime > 0) {
                    try {
                        wait(waitTime);
                    } catch (InterruptedException e) {
                        LogUtil.w(TAG, e.toString());
                    }
                    waitTime = ((long) this.timeout) - (SystemClock.uptimeMillis() - startTime);
                }
                if (this.anrChecker.isBlocked()) {
                    if (this.ignoreDebugger || !Debug.isDebuggerConnected()) {
                        String cpuInfo = getCpuInfo();
                        String stackTraceInfo = getStackTraceInfo();
                        LogUtil.w(TAG, "cpuInfo:" + cpuInfo);
                        LogUtil.w(TAG, "stackTraceInfo:" + stackTraceInfo);
                        if (this.anrListener != null) {
                            this.anrListener.onAnrHappened(stackTraceInfo);
                        }
                    } else {
                        LogUtil.w(TAG, "device is debugging...");
                    }
                }
            }
        }
        LogUtil.w(TAG, getName() + " thread is interrupted");
    }
}
