package com.xtc.bigdata.monitor.frame;

import android.view.Choreographer;

import com.xtc.log.LogUtil;

/**
 * 帧率监控。
 *
 * <p>挂在 {@link Choreographer} 上逐帧回调：用"两帧之间的实际间隔"除以
 * "理论帧间隔"得到丢帧数，一旦丢帧超过半屏（{@code targetFrame / 2}）
 * 就打一条 {@code "Skipped N frames!"} —— 这条日志和系统 Choreographer
 * 自己打的那条格式一致，排查时可以一起 grep。</p>
 */
public class FPSFrameCallback implements Choreographer.FrameCallback {

    private static final String TAG = "FPSFrameCallback";

    /** 上一帧的时刻（纳秒）。 */
    private long mLastFrameTimeNanos;

    /** 目标帧率。 */
    private float mTargetFrame = 60.0f;

    /** 理论帧间隔（纳秒）。 */
    private long mFrameIntervalNanos = (long) (1.0E9f / this.mTargetFrame);

    /**
     * @param startFrameTimeNanos 起始帧时刻（纳秒），0 表示从第一次回调开始算
     */
    public FPSFrameCallback(long startFrameTimeNanos) {
        this.mLastFrameTimeNanos = 0L;
        this.mLastFrameTimeNanos = startFrameTimeNanos;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (this.mLastFrameTimeNanos == 0) {
            this.mLastFrameTimeNanos = frameTimeNanos;
        }
        long frameInterval = frameTimeNanos - this.mLastFrameTimeNanos;
        if (frameInterval >= this.mFrameIntervalNanos) {
            long skippedFrames = frameInterval / this.mFrameIntervalNanos;
            if (skippedFrames > this.mTargetFrame / 2.0f) {
                LogUtil.i(TAG, "Skipped " + skippedFrames + " frames!  The application may be doing too much work on its main thread !");
            }
            LogUtil.d(TAG, "current Frame = " + ((int) Math.max(this.mTargetFrame - skippedFrames, 0.0f)));
        }
        this.mLastFrameTimeNanos = frameTimeNanos;
        Choreographer.getInstance().postFrameCallback(this);
    }
}
