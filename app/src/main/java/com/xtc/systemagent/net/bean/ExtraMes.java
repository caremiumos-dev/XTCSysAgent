package com.xtc.systemagent.net.bean;

/**
 * 日志抓取策略里的"附加参数"。
 *
 * <p>默认值来自服务端协议约定（每十分钟 4 次、每小时 8 次、抓 3 次、
 * 每次 20 分钟），改动会直接影响抓取频率，必须保持原值。</p>
 */
public class ExtraMes {

    private int perTenMinutes = 4;
    private int perOneHour = 8;
    private int fetchingTime = 3;
    private boolean isImmediately = false;
    private int fetchingDuration = 20;
    private boolean isUnActive;
    private String callerWatchId;
    private String logType;
    private String grabType;

    public int getPerTenMinutes() {
        return this.perTenMinutes;
    }

    public void setPerTenMinutes(int perTenMinutes) {
        this.perTenMinutes = perTenMinutes;
    }

    public int getPerOneHour() {
        return this.perOneHour;
    }

    public void setPerOneHour(int perOneHour) {
        this.perOneHour = perOneHour;
    }

    public int getFetchingTime() {
        return this.fetchingTime;
    }

    public void setFetchingTime(int fetchingTime) {
        this.fetchingTime = fetchingTime;
    }

    public boolean getIsImmediately() {
        return this.isImmediately;
    }

    public void setIsImmediately(boolean isImmediately) {
        this.isImmediately = isImmediately;
    }

    public int getFetchingDuration() {
        return this.fetchingDuration;
    }

    public void setFetchingDuration(int fetchingDuration) {
        this.fetchingDuration = fetchingDuration;
    }

    public boolean isUnActive() {
        return this.isUnActive;
    }

    public void setIsUnActive(boolean isUnActive) {
        this.isUnActive = isUnActive;
    }

    public String getCallerWatchId() {
        return this.callerWatchId;
    }

    public void setCallerWatchId(String callerWatchId) {
        this.callerWatchId = callerWatchId;
    }

    public String getLogType() {
        return this.logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getGrabType() {
        return this.grabType;
    }

    public void setGrabType(String grabType) {
        this.grabType = grabType;
    }

    @Override
    public String toString() {
        return "ExtraMes{perTenMinutes=" + this.perTenMinutes
                + ", perOneHour=" + this.perOneHour
                + ", fetchingTime=" + this.fetchingTime
                + ", isImmediately=" + this.isImmediately
                + ", fetchingDuration=" + this.fetchingDuration
                + ", isUnActive=" + this.isUnActive
                + ", callerWatchId='" + this.callerWatchId
                + "', logType='" + this.logType
                + "', grabType=" + this.grabType + '}';
    }
}
