package com.xtc.bigdata.monitor.anr;

import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.xtc.log.LogUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * 主线程卡顿看门狗（第二代实现）。
 *
 * <p>它比 {@code ANRWatchDog} 强的地方：可以同时监控多个 Handler 线程，
 * 可以在被监控线程上挂 {@link Monitor}（在目标线程里执行一段代码，
 * 用"这段代码能不能按时跑完"判断该线程是否卡住），
 * 并且能把<b>所有</b>线程的堆栈一次性 dump 出来。</p>
 *
 * <p>工作循环：{@code loopToCheck()} 里反复 {@code checkBlock(false)}，
 * 每轮把探测任务 {@code postAtFrontOfQueue} 到各被监控线程，
 * 然后 {@code wait(半个最小超时)}；醒来后按完成度分级：
 * 1=还在等、2=等过一半、3=超时，只有 3 才会真正 dump 堆栈。</p>
 *
 * <p>调试器判定：连着调试器时不做任何 dump —— 断点必然导致"卡顿"。</p>
 */
public class WatchDogs extends Thread {

    /** 完成度：已完成。 */
    static final int COMPLETED = 0;

    /** 完成度：还在等待。 */
    static final int WAITING = 1;

    /** 完成度：等过一半时间。 */
    static final int WAITED_HALF = 2;

    /** 完成度：已超时。 */
    static final int OVERDUE = 3;

    /** 默认卡顿阈值（毫秒）。 */
    static final long DEFAULT_TIMEOUT = 4000;

    /** 默认线程名。 */
    static final String DLThreadName = "WatchDogs_DeadLockMonitorThread";

    static final String TAG = "WatchDogs";

    /** 默认的卡顿回调（只打日志）。 */
    private static final BlockListener DEFAULT_BLOCK_LISTENER = new BlockListener() {
        @Override
        public void onBlocked(String stackTrace) {
            LogUtil.e(WatchDogs.TAG, "Block occurs ->\n" + stackTrace);
        }
    };

    /** 单例。 */
    static WatchDogs sWatchdog;

    /** 最小卡顿阈值。 */
    private long MIN_TIMEOUT;

    /** 卡顿回调。 */
    private BlockListener blockListener;

    /** 是否 dump 所有线程（false 只 dump 卡住的那个）。 */
    private boolean collectAllThreads;

    /** 退出标记（覆盖了 {@link Thread#isInterrupted()} 的语义）。 */
    public boolean isInterrupted;

    /** 死锁监控线程（保留字段，当前没有启动点）。 */
    private HandlerThread mDLThread;

    /** 所有被监控的 Handler 线程。 */
    private ArrayList<HandlerChecker> mHandlerCheckers;

    /** Monitor 专用检查器（保留字段，当前没有创建点）。 */
    private HandlerChecker mMonitorChecker;

    /** 卡顿回调。 */
    public interface BlockListener {

        /**
         * 发生卡顿。
         *
         * @param stackTrace 堆栈
         */
        void onBlocked(String stackTrace);
    }

    /** 可以在目标线程里执行的一段探测代码。 */
    public interface Monitor {

        /** 在目标线程里执行。 */
        void monitor();
    }

    /**
     * 单个 Handler 线程的探测器。
     *
     * <p>它是一条"哨兵任务"：投递到队首，如果目标线程在超时时间内没跑完，
     * 就说明该线程被前面的任务卡住了。</p>
     */
    public final class HandlerChecker implements Runnable {

        /** 当前正在执行的 Monitor。 */
        private Monitor mCurrentMonitor;

        /** 目标线程的 Handler。 */
        private final Handler mHandler;

        /** 线程名。 */
        private final String mName;

        /** 本轮投递时刻。 */
        private long mStartTime;

        /** 超时阈值。 */
        private final long mTimeout;

        /** 真实线程 id（首次执行时记录）。 */
        private int realTid = -1;

        /** 挂在该线程上的 Monitor 列表。 */
        private final ArrayList<Monitor> mMonitors = new ArrayList<Monitor>();

        /** 本轮是否已完成。 */
        private boolean mCompleted = true;

        HandlerChecker(Handler handler, String name, long timeout) {
            this.mHandler = handler;
            this.mName = name;
            this.mTimeout = timeout;
        }

        /**
         * 挂一个 Monitor。
         *
         * @param monitor 探测器
         */
        public void addMonitor(Monitor monitor) {
            this.mMonitors.add(monitor);
        }

        /**
         * 描述卡住的位置。
         *
         * @return 描述串
         */
        public String describeBlockedStateLocked() {
            return this.mCurrentMonitor == null
                    ? "Blocked in handler on " + this.mName + " (" + getThread().getName() + ") , realTid = " + this.realTid
                    : "Blocked in monitor " + this.mCurrentMonitor.getClass().getName() + " on " + this.mName
                    + " (" + getThread().getName() + ") , realTid = " + this.realTid;
        }

        /**
         * 计算完成度。
         *
         * @return {@link #COMPLETED}/{@link #WAITING}/{@link #WAITED_HALF}/{@link #OVERDUE}
         */
        public int getCompletionStateLocked() {
            if (this.mCompleted) {
                return COMPLETED;
            }
            long elapsed = SystemClock.uptimeMillis() - this.mStartTime;
            if (elapsed < this.mTimeout / 2) {
                return WAITING;
            }
            return elapsed < this.mTimeout ? WAITED_HALF : OVERDUE;
        }

        /** @return 线程名 */
        public String getName() {
            return this.mName;
        }

        /** @return 目标线程当前堆栈 */
        public StackTraceElement[] getStackTrace() {
            return this.mHandler.getLooper().getThread().getStackTrace();
        }

        /** @return 目标线程 */
        public Thread getThread() {
            return this.mHandler.getLooper().getThread();
        }

        /** @return 超时阈值 */
        public long getTimeout() {
            return this.mTimeout;
        }

        /**
         * 本轮是否已超时。
         *
         * @return 是否超时
         */
        public boolean isOverdueLocked() {
            return !this.mCompleted && SystemClock.uptimeMillis() > this.mStartTime + this.mTimeout;
        }

        /** 标记本轮完成。 */
        public void resetCompleted() {
            this.mCompleted = true;
        }

        @Override
        public void run() {
            if (this.realTid == -1) {
                this.realTid = Process.myTid();
            }
            int monitorCount = this.mMonitors.size();
            for (int index = 0; index < monitorCount; index++) {
                synchronized (WatchDogs.this) {
                    this.mCurrentMonitor = this.mMonitors.get(index);
                }
                this.mCurrentMonitor.monitor();
            }
            synchronized (WatchDogs.this) {
                this.mCompleted = true;
                this.mCurrentMonitor = null;
            }
        }

        /** 投递本轮探测任务（已在目标线程队列里则跳过）。 */
        public void scheduleCheckLocked() {
            if (this.mCompleted) {
                this.mCompleted = false;
                this.mCurrentMonitor = null;
                this.mStartTime = SystemClock.uptimeMillis();
                this.mHandler.postAtFrontOfQueue(this);
            }
        }

        @Override
        public String toString() {
            return "HandlerChecker{mName= " + this.mName + ", realTid= " + this.realTid
                    + ", mTimeout= " + this.mTimeout + ", mCompleted= " + this.mCompleted
                    + ", mStartTime= " + this.mStartTime
                    + ", nice= " + Process.getThreadPriority(this.realTid) + " }";
        }
    }

    private WatchDogs(int minTimeout) {
        super(TAG);
        this.MIN_TIMEOUT = DEFAULT_TIMEOUT;
        this.blockListener = DEFAULT_BLOCK_LISTENER;
        this.mHandlerCheckers = new ArrayList<HandlerChecker>();
        this.isInterrupted = false;
        this.collectAllThreads = false;
        // 只有落在 [200, 4000) 区间的自定义值才生效
        if (minTimeout >= 200 && minTimeout < DEFAULT_TIMEOUT) {
            this.MIN_TIMEOUT = minTimeout;
        }
        LogUtil.i(TAG, "create a WatchDogs = " + toString());
        this.mHandlerCheckers.add(new HandlerChecker(new Handler(Looper.getMainLooper()), "main thread", this.MIN_TIMEOUT));
    }

    /**
     * 一轮检测。
     *
     * @param reportHalfTime 是否在"等过一半"时也打日志
     */
    private void checkBlock(boolean reportHalfTime) {
        long checkInterval = getCheckInterval();
        synchronized (this) {
            for (int index = 0; index < this.mHandlerCheckers.size(); index++) {
                this.mHandlerCheckers.get(index).scheduleCheckLocked();
            }
            long startTime = SystemClock.uptimeMillis();
            char debuggerState = 0;
            for (long waitTime = checkInterval; waitTime > 0; waitTime = checkInterval - (SystemClock.uptimeMillis() - startTime)) {
                if (Debug.isDebuggerConnected()) {
                    debuggerState = WAITED_HALF;
                }
                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    LogUtil.i(TAG, "thread has bean interrupted , e = " + e);
                }
                if (Debug.isDebuggerConnected()) {
                    debuggerState = WAITED_HALF;
                }
            }
            int completionState = evaluateCheckerCompletionLocked();
            if (completionState == COMPLETED) {
                return;
            }
            if (completionState == WAITING) {
                return;
            }
            if (completionState == WAITED_HALF) {
                if (!reportHalfTime) {
                    LogUtil.w(TAG, "some threads have waited over half time !");
                }
                return;
            }
            if (Debug.isDebuggerConnected()) {
                debuggerState = WAITED_HALF;
            }
            // 两个条件等价（只要曾经连过调试器就忽略），原厂就是这么写的
            if (debuggerState >= WAITED_HALF || debuggerState > 0) {
                LogUtil.w(TAG, "Debugger connected or was connected , ignore !");
            } else {
                LogUtil.e(TAG, "now no debugger connected and block happen !");
                handleBlock(getBlockedCheckersLocked());
            }
        }
    }

    /**
     * 描述所有超时的检查器。
     *
     * @param blockedCheckers 超时的检查器
     * @return 描述串
     */
    private StringBuilder describeCheckersLocked(ArrayList<HandlerChecker> blockedCheckers) {
        StringBuilder builder = new StringBuilder(128);
        for (int index = 0; index < blockedCheckers.size(); index++) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(blockedCheckers.get(index).describeBlockedStateLocked());
        }
        return builder;
    }

    /**
     * 取所有检查器里最严重的完成度。
     *
     * @return 完成度
     */
    private int evaluateCheckerCompletionLocked() {
        int maxState = COMPLETED;
        for (int index = 0; index < this.mHandlerCheckers.size(); index++) {
            maxState = Math.max(maxState, this.mHandlerCheckers.get(index).getCompletionStateLocked());
        }
        return maxState;
    }

    /** 通知看门狗退出（由广播触发）。 */
    public static void exit() {
        LogUtil.d(TAG, "receive commond exit -------");
        if (sWatchdog != null) {
            sWatchdog.isInterrupted = true;
        }
    }

    /**
     * dump 所有 Java 线程的堆栈（主线程排最前）。
     *
     * @param builder         输出缓冲
     * @param blockedCheckers 超时的检查器（当前实现未使用）
     * @return 输出缓冲
     */
    private StringBuilder getAllThreadStackTraces(StringBuilder builder, ArrayList<HandlerChecker> blockedCheckers) {
        final Thread mainThread = Looper.getMainLooper().getThread();
        // 主线程固定排第一，其余按线程名倒序，保证多次 dump 的顺序稳定可 diff
        TreeMap<Thread, StackTraceElement[]> threadStacks = new TreeMap<Thread, StackTraceElement[]>(
                new Comparator<Thread>() {
                    @Override
                    public int compare(Thread first, Thread second) {
                        if (first == second) {
                            return 0;
                        }
                        if (first == mainThread) {
                            return -1;
                        }
                        if (second == mainThread) {
                            return 1;
                        }
                        return second.getName().compareTo(first.getName());
                    }
                });
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            threadStacks.put(entry.getKey(), entry.getValue());
        }
        if (!threadStacks.containsKey(mainThread)) {
            threadStacks.put(mainThread, mainThread.getStackTrace());
        }
        LogUtil.d(TAG, "current java thread count = " + threadStacks.size());
        for (Map.Entry<Thread, StackTraceElement[]> entry : threadStacks.entrySet()) {
            builder = getSingleThreadStateAndTrace(builder, entry.getKey(), entry.getValue());
        }
        return builder;
    }

    /**
     * 取本轮超时的检查器（取到后重置其完成标记）。
     *
     * @return 超时的检查器
     */
    private ArrayList<HandlerChecker> getBlockedCheckersLocked() {
        ArrayList<HandlerChecker> blockedCheckers = new ArrayList<HandlerChecker>();
        for (int index = 0; index < this.mHandlerCheckers.size(); index++) {
            HandlerChecker handlerChecker = this.mHandlerCheckers.get(index);
            if (handlerChecker.isOverdueLocked()) {
                blockedCheckers.add(handlerChecker);
                handlerChecker.resetCompleted();
            }
        }
        return blockedCheckers;
    }

    /**
     * 计算本轮的等待时长（最小超时的一半，且不超过任一检查器的超时）。
     *
     * @return 等待毫秒
     */
    private long getCheckInterval() {
        long interval = this.MIN_TIMEOUT / 2;
        for (int index = 0; index < this.mHandlerCheckers.size(); index++) {
            interval = Math.min(interval, this.mHandlerCheckers.get(index).getTimeout());
        }
        return interval;
    }

    /**
     * 取（或按需创建）看门狗单例。
     *
     * @param minTimeout 最小超时阈值
     * @return 单例
     */
    public static WatchDogs getInstance(int minTimeout) {
        if (sWatchdog == null) {
            synchronized (WatchDogs.class) {
                if (sWatchdog == null) {
                    sWatchdog = new WatchDogs(minTimeout);
                }
            }
        }
        return sWatchdog;
    }

    /**
     * 只 dump 卡住线程的堆栈。
     *
     * @param builder         输出缓冲
     * @param blockedCheckers 超时的检查器
     * @return 输出缓冲
     */
    private StringBuilder getJustBlockedStackTraces(StringBuilder builder, ArrayList<HandlerChecker> blockedCheckers) {
        for (int index = 0; index < blockedCheckers.size(); index++) {
            HandlerChecker handlerChecker = blockedCheckers.get(index);
            builder = getSingleThreadStateAndTrace(builder, handlerChecker.getThread(), handlerChecker.getStackTrace());
        }
        return builder;
    }

    /**
     * 追加单个线程的状态与堆栈。
     *
     * @param builder   输出缓冲
     * @param thread    线程
     * @param stackTrace 堆栈
     * @return 输出缓冲
     */
    private StringBuilder getSingleThreadStateAndTrace(StringBuilder builder, Thread thread, StackTraceElement[] stackTrace) {
        builder.append("Thread = " + thread.getName() + " (state = " + thread.getState() + ")\n");
        for (StackTraceElement stackTraceElement : stackTrace) {
            builder.append("\t\tat ").append(stackTraceElement.toString()).append("\n");
        }
        return builder;
    }

    /**
     * 组织并回调一次卡顿报告。
     *
     * @param blockedCheckers 超时的检查器
     */
    private void handleBlock(ArrayList<HandlerChecker> blockedCheckers) {
        StringBuilder builder = describeCheckersLocked(blockedCheckers);
        builder.append("\n");
        StringBuilder stackTraces = this.collectAllThreads
                ? getAllThreadStackTraces(builder, blockedCheckers)
                : getJustBlockedStackTraces(builder, blockedCheckers);
        if (this.blockListener != null) {
            this.blockListener.onBlocked(stackTraces.toString());
        }
    }

    /** 主循环：只要没被通知退出就一直检测。 */
    private void loopToCheck() {
        while (!isInterrupted()) {
            checkBlock(false);
        }
        release();
    }

    /** 释放资源并清空单例。 */
    private void release() {
        if (sWatchdog != null) {
            LogUtil.i(TAG, "release resources , sWatchdog = " + sWatchdog);
            if (this.mDLThread != null) {
                this.mDLThread.quit();
                this.mDLThread = null;
            }
            this.mHandlerCheckers.clear();
            this.mHandlerCheckers = null;
            this.mMonitorChecker = null;
            this.blockListener = null;
            sWatchdog = null;
        }
    }

    /**
     * 挂一个 Monitor（必须在 start 之前）。
     *
     * @param monitor 探测器
     * @return this
     */
    public WatchDogs addMonitor(Monitor monitor) {
        synchronized (this) {
            if (isAlive()) {
                throw new RuntimeException("Monitors can't be added once the Watchdog is running !!!");
            }
            if (this.mMonitorChecker != null) {
                this.mMonitorChecker.addMonitor(monitor);
            } else {
                LogUtil.w(TAG, "mMonitorChecker = null , pass");
            }
        }
        return this;
    }

    /**
     * 增加一个被监控的 Handler 线程（用默认超时）。
     *
     * @param handler 目标线程的 Handler
     * @return this
     */
    public WatchDogs addThread(Handler handler) {
        return addThread(handler, this.MIN_TIMEOUT);
    }

    /**
     * 增加一个被监控的 Handler 线程。
     *
     * @param handler 目标线程的 Handler
     * @param timeout 超时阈值（不会小于 MIN_TIMEOUT）
     * @return this
     */
    public WatchDogs addThread(Handler handler, long timeout) {
        synchronized (this) {
            if (isAlive()) {
                throw new RuntimeException("Threads can't be added once the Watchdog is running");
            }
            this.mHandlerCheckers.add(new HandlerChecker(handler, handler.getLooper().getThread().getName(),
                    Math.max(timeout, this.MIN_TIMEOUT)));
        }
        return this;
    }

    @Override
    public boolean isInterrupted() {
        return this.isInterrupted;
    }

    @Override
    public void run() {
        // 10 = THREAD_PRIORITY_BACKGROUND，降低自身对被测线程的干扰
        Process.setThreadPriority(10);
        LogUtil.i(TAG, "WatchDogs real run - " + toString());
        try {
            loopToCheck();
        } catch (Exception e) {
            LogUtil.e(TAG, "unknown Error , e = " + e);
            e.printStackTrace();
        }
    }

    /**
     * 设置卡顿回调。
     *
     * @param blockListener 回调（null 会被忽略）
     * @return this
     */
    public WatchDogs setBlockListener(BlockListener blockListener) {
        if (blockListener != null) {
            this.blockListener = blockListener;
        }
        return this;
    }

    /**
     * 设置是否 dump 全部线程。
     *
     * @param collectAllThread 是否全部 dump
     * @return this
     */
    public WatchDogs setCollectAllThread(boolean collectAllThread) {
        this.collectAllThreads = collectAllThread;
        return this;
    }

    @Override
    public synchronized void start() {
        if (isAlive()) {
            LogUtil.w(TAG, "dumplicate start , return !");
        } else {
            LogUtil.d(TAG, "real start !");
            super.start();
        }
    }

    @Override
    public String toString() {
        int tid = Process.myTid();
        return super.toString() + " , hashCode = " + hashCode() + " , (" + Process.myPid() + "-" + tid + ") , nice = "
                + Process.getThreadPriority(tid) + " , MIN_TIMEOUT = " + this.MIN_TIMEOUT;
    }
}
