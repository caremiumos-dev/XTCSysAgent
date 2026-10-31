package com.xtc.systemagent.net.bean;

import java.util.List;

/**
 * 服务端下发的一条"日志采集策略"。
 *
 * <p>字段名即服务端 JSON 的键名，不能改。部分 getter 返回包装类型是原厂
 * 为了可以直接判空，保持这个形态可以让调用点行为完全一致。</p>
 */
public class LogStrategy {

    private Integer id;
    private String userId;
    private Long logId;
    private String accountId;
    private String appPackage;
    private int enableStatus;
    private int fileLimit;
    private int logLevel;
    private int uploadDays;
    private int uploadMode;
    private int uploadNet;
    private int batteryStatus;
    private List<Long> dates;
    private ExtraMes extra;
    private CallLogStrategy callExtra;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getLogId() {
        return this.logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAppPackage() {
        return this.appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public Integer getEnableStatus() {
        return Integer.valueOf(this.enableStatus);
    }

    public void setEnableStatus(int enableStatus) {
        this.enableStatus = enableStatus;
    }

    public Integer getFileLimit() {
        return Integer.valueOf(this.fileLimit);
    }

    public void setFileLimit(int fileLimit) {
        this.fileLimit = fileLimit;
    }

    public Integer getLogLevel() {
        return Integer.valueOf(this.logLevel);
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getUploadDays() {
        return this.uploadDays;
    }

    public void setUploadDays(int uploadDays) {
        this.uploadDays = uploadDays;
    }

    public int getUploadMode() {
        return this.uploadMode;
    }

    public void setUploadMode(int uploadMode) {
        this.uploadMode = uploadMode;
    }

    public int getUploadNet() {
        return this.uploadNet;
    }

    public void setUploadNet(int uploadNet) {
        this.uploadNet = uploadNet;
    }

    public int getBatteryStatus() {
        return this.batteryStatus;
    }

    public void setBatteryStatus(int batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public List<Long> getDates() {
        return this.dates;
    }

    public void setDates(List<Long> dates) {
        this.dates = dates;
    }

    public ExtraMes getExtra() {
        return this.extra;
    }

    public void setExtra(ExtraMes extra) {
        this.extra = extra;
    }

    public CallLogStrategy getCallExtra() {
        return this.callExtra;
    }

    public void setCallExtra(CallLogStrategy callLogStrategy) {
        this.callExtra = callLogStrategy;
    }

    @Override
    public String toString() {
        return "LogStrategy{id=" + this.id
                + ", userId='" + this.userId + "'"
                + ", logId=" + this.logId
                + ", accountId='" + this.accountId + "'"
                + ", appPackage='" + this.appPackage + "'"
                + ", enableStatus=" + this.enableStatus
                + ", fileLimit=" + this.fileLimit
                + ", logLevel=" + this.logLevel
                + ", uploadDays=" + this.uploadDays
                + ", uploadMode=" + this.uploadMode
                + ", uploadNet=" + this.uploadNet
                + ", batteryStatus=" + this.batteryStatus
                + ", dates=" + this.dates
                + ", extra=" + this.extra
                + ", callExtra=" + this.callExtra + '}';
    }
}
