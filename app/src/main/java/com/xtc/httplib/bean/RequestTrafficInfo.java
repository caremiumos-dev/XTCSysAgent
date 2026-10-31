package com.xtc.httplib.bean;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 当日流量统计（进程级单例，由 {@code MonitorInterceptor} 持有）。
 *
 * <p>统计口径是"请求体 + 响应体"的字节数，超过阈值后只上报一次，
 * 用 {@code hadRecord} 去重。三个字段都是原子类型，因为网络线程并发累加。</p>
 *
 * <p><b>注意两处与原厂一致的反直觉实现</b>：
 * {@link #setStartTime(long)} 与 {@link #setTotalTraffic(long)} 内部都是
 * {@code addAndGet} 而不是 {@code set}（原厂笔误）。{@code setStartTime(now)} 因此是
 * "在旧值上加 now"，并不真的把起点设为 now；调用方只看结果，行为必须保持一致。</p>
 */
public class RequestTrafficInfo {

    /** 统计起点（墙钟时间）。 */
    private AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    /** 累计流量（字节）。 */
    private AtomicLong totalTraffic = new AtomicLong(0);
    /** 当日超限是否已上报。 */
    private AtomicBoolean hadRecord = new AtomicBoolean(false);

    public void addTotalTraffic(long traffic) {
        this.totalTraffic.addAndGet(traffic);
    }

    public boolean getHadRecord() {
        return this.hadRecord.get();
    }

    public long getStartTime() {
        return this.startTime.get();
    }

    public long getTotalTraffic() {
        return this.totalTraffic.get();
    }

    public void setHadRecord(boolean hadRecord) {
        this.hadRecord.getAndSet(hadRecord);
    }

    /** 原厂用的是 {@code addAndGet}，不是 {@code set}，保留。 */
    public void setStartTime(long startTime) {
        this.startTime.addAndGet(startTime);
    }

    /** 原厂用的是 {@code addAndGet}，不是 {@code set}，保留。 */
    public void setTotalTraffic(long totalTraffic) {
        this.totalTraffic.addAndGet(totalTraffic);
    }

    @Override
    public String toString() {
        return "RequestTrafficInfo{startTime=" + this.startTime
                + ", totalTraffic=" + this.totalTraffic
                + ", hadRecord=" + this.hadRecord + '}';
    }
}
