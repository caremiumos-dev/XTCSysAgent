package com.xtc.bigdata.monitor.anr;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 周期性采样器的骨架。
 *
 * <p>采样跑在 {@link HandlerThreadFactory#getTimerThreadHandler()} 的
 * 独立线程上，用 {@link AtomicBoolean} 做启停闸门，避免重复 post 同一份
 * Runnable 造成采样频率翻倍。</p>
 *
 * @deprecated 原厂已标记废弃，新的实现是 {@code WatchDogs}。
 */
@Deprecated
abstract class AbstractSampler {

    /** 默认采样间隔（毫秒）。 */
    private static final int DEFAULT_SAMPLE_INTERVAL = 300;

    /** 首次采样的延迟（毫秒）。 */
    private static final long FIRST_SAMPLE_DELAY = 800L;

    /** 采样间隔。 */
    protected long mSampleInterval;

    /** 是否应该继续采样。 */
    protected AtomicBoolean mShouldSample = new AtomicBoolean(false);

    /** 采样任务本体。 */
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doSample();
            if (AbstractSampler.this.mShouldSample.get()) {
                HandlerThreadFactory.getTimerThreadHandler()
                        .postDelayed(AbstractSampler.this.mRunnable, AbstractSampler.this.mSampleInterval);
            }
        }
    };

    /**
     * @param sampleInterval 采样间隔，0 表示用默认值 300ms
     */
    public AbstractSampler(long sampleInterval) {
        this.mSampleInterval = 0 == sampleInterval ? DEFAULT_SAMPLE_INTERVAL : sampleInterval;
    }

    /** 执行一次采样。 */
    abstract void doSample();

    /** 开始采样（首次延迟 800ms，避开启动抖动）。 */
    public void start() {
        if (this.mShouldSample.get()) {
            return;
        }
        this.mShouldSample.set(true);
        HandlerThreadFactory.getTimerThreadHandler().removeCallbacks(this.mRunnable);
        HandlerThreadFactory.getTimerThreadHandler().postDelayed(this.mRunnable, FIRST_SAMPLE_DELAY);
    }

    /** 停止采样。 */
    public void stop() {
        if (this.mShouldSample.get()) {
            this.mShouldSample.set(false);
            HandlerThreadFactory.getTimerThreadHandler().removeCallbacks(this.mRunnable);
        }
    }
}
