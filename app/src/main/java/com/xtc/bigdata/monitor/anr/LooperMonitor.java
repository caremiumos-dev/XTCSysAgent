package com.xtc.bigdata.monitor.anr;

import android.os.Debug;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Printer;

import java.util.Iterator;

/**
 * 用 {@link Looper#setMessageLogging(Printer)} 检测主线程卡顿。
 *
 * <p>原理：每条消息开始打印时记下起点并启动堆栈采样，结束打印时算耗时；
 * 超过 {@code timeout} 就把这段时间窗口内的采样堆栈拼起来回调出去。</p>
 *
 * <p>{@code ignoreDebugger} 打开时，只要连着调试器就完全不统计 ——
 * 调试器断点会让每条消息都"超时"。</p>
 *
 * @deprecated 原厂已标记废弃。
 */
@Deprecated
public class LooperMonitor implements Printer {

    /** 卡顿回调。 */
    private BlockListener blockListener;

    /** 是否忽略调试器。 */
    private boolean ignoreDebugger;

    /** 被监控的 Looper。 */
    private Looper looper;

    /** 当前是否处在"消息开始"阶段。 */
    private boolean mPrintingStarted = false;

    /** 当前消息的开始时刻。 */
    private long mStartTimestamp = 0;

    /** 堆栈采样器。 */
    private StackSampler stackSampler;

    /** 判定卡顿的阈值。 */
    private long timeout;

    /** 卡顿回调接口。 */
    public interface BlockListener {

        /**
         * 发生卡顿。
         *
         * @param blockTime 卡顿时长（毫秒）
         * @param stack     这段时间内的采样堆栈
         */
        void onBlock(long blockTime, String stack);
    }

    /**
     * @param looper          被监控的 Looper
     * @param ignoreDebugger  是否忽略调试器
     * @param timeout         卡顿阈值（毫秒）
     * @param blockListener   回调
     */
    public LooperMonitor(Looper looper, boolean ignoreDebugger, long timeout, BlockListener blockListener) {
        this.ignoreDebugger = true;
        this.timeout = 5000L;
        this.looper = looper;
        this.ignoreDebugger = ignoreDebugger;
        this.timeout = timeout;
        this.blockListener = blockListener;
        this.stackSampler = new StackSampler(looper.getThread(), 1000L);
    }

    /**
     * 计算从消息开始到现在的耗时。
     *
     * @param now 当前 uptime
     * @return 耗时毫秒
     */
    private long getBlockTime(long now) {
        return now - this.mStartTimestamp;
    }

    /**
     * 是否算卡顿。
     *
     * @param now 当前 uptime
     * @return 是否超过阈值
     */
    private boolean isBlock(long now) {
        return getBlockTime(now) >= this.timeout;
    }

    /**
     * 回调卡顿（拼堆栈放后台线程做，避免卡顿现场再叠加耗时）。
     *
     * @param now 检测到卡顿的时刻
     */
    private void notifyBlock(final long now) {
        if (this.blockListener != null) {
            final long blockTime = getBlockTime(now);
            final long startTimestamp = this.mStartTimestamp;
            HandlerThreadFactory.getWriteLogThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    StringBuilder builder = new StringBuilder();
                    Iterator<String> iterator = LooperMonitor.this.stackSampler
                            .getThreadStackEntries(startTimestamp, now).iterator();
                    while (iterator.hasNext()) {
                        builder.append(iterator.next()).append("\r\n");
                    }
                    LooperMonitor.this.blockListener.onBlock(blockTime, builder.toString());
                }
            });
        }
    }

    @Override
    public void println(String message) {
        if (this.ignoreDebugger && Debug.isDebuggerConnected()) {
            return;
        }
        if (!this.mPrintingStarted) {
            // 消息开始
            this.mStartTimestamp = SystemClock.uptimeMillis();
            this.mPrintingStarted = true;
            this.stackSampler.start();
        } else {
            // 消息结束
            long now = SystemClock.uptimeMillis();
            this.mPrintingStarted = false;
            if (isBlock(now)) {
                notifyBlock(now);
            }
            this.stackSampler.stop();
        }
    }
}
