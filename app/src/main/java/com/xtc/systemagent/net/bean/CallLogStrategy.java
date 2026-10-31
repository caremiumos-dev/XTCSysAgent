package com.xtc.systemagent.net.bean;

/**
 * 通话日志抓取策略。服务端下发的 {@code timeFlag} 用于去抖，
 * 抓取时间与时长都有默认值（3 / 5，单位由使用方决定，原厂是分钟语义）。
 */
public class CallLogStrategy {

    private boolean isActive;
    private int fetchingTime = 3;
    private int fetchingDuration = 5;
    private String timeFlag;

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getFetchingTime() {
        return this.fetchingTime;
    }

    public void setFetchingTime(int fetchingTime) {
        this.fetchingTime = fetchingTime;
    }

    public int getFetchingDuration() {
        return this.fetchingDuration;
    }

    public void setFetchingDuration(int fetchingDuration) {
        this.fetchingDuration = fetchingDuration;
    }

    public String getTimeFlag() {
        return this.timeFlag;
    }

    public void setTimeFlag(String timeFlag) {
        this.timeFlag = timeFlag;
    }

    @Override
    public String toString() {
        return "CallLogStrategy{isActive=" + this.isActive
                + ", fetchingTime=" + this.fetchingTime
                + ", fetchingDuration=" + this.fetchingDuration
                + ", timeFlag='" + this.timeFlag + "'}";
    }
}
