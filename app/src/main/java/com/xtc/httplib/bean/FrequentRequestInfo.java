package com.xtc.httplib.bean;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单个 URL 的"频繁请求"统计。
 *
 * <p>{@code MonitorInterceptor} 用它对同一路径的请求时间点做滑动窗口统计：
 * 窗口内请求次数超过阈值就认定刷接口。{@code requestTimes} 用写时复制列表，
 * 因为统计发生在网络线程、而清理可能在中控线程，读多写少且不能加锁。</p>
 *
 * <p>{@code isRequesting} 表示"上一次请求还没结束"，用于拦截并发重入。</p>
 */
public class FrequentRequestInfo {

    /** 该 URL 是否正处于请求中。 */
    private boolean isRequesting = false;
    /** 最近若干次请求的时间点（elapsedRealtime）。 */
    private List<Long> requestTimes = new CopyOnWriteArrayList<Long>();
    /** 被统计的 URL（已去掉 uuid 参数）。 */
    private String requestUrl;

    public List<Long> getRequestTimes() {
        return this.requestTimes;
    }

    public String getRequestUrl() {
        return this.requestUrl;
    }

    public boolean isRequesting() {
        return this.isRequesting;
    }

    public void setRequestTimes(List<Long> requestTimes) {
        this.requestTimes = requestTimes;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public void setRequesting(boolean requesting) {
        this.isRequesting = requesting;
    }

    @Override
    public String toString() {
        return "FrequentRequestInfo{requestUrl='" + this.requestUrl
                + "', isRequesting=" + this.isRequesting
                + ", requestTimes=" + this.requestTimes + '}';
    }
}
