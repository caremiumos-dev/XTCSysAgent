package com.xtc.systemagent.module.log.bean;

/**
 * Modem 日志的抓取与上传状态。
 */
public class ModemLogStatus {

    /** 最多重试上传的次数。 */
    public static final int MAX_RETRY_UPLOAD_NUM = 10;

    private int fetchingTime;
    private int currentFetchingTime;
    private int currentStatus = 0;
    private int currentRetryUploadNum = 0;
    private long currentRetryUploadTime = 0;
    private String zipFileName;

    /**
     * 状态机取值。
     */
    public interface Status {
        int IDLE = 0;
        int MONITORING = 1;
        int FETCHING = 2;
        int PRE_UPLOAD = 3;
        int UPLOAD_FAILURE = 4;
        int UPLOAD_SUCCESS = 5;
        int FETCH_LOG_FAILURE = 6;
    }

    public int getFetchingTime() {
        return this.fetchingTime;
    }

    public void setFetchingTime(int fetchingTime) {
        this.fetchingTime = fetchingTime;
    }

    public int getCurrentFetchingTime() {
        return this.currentFetchingTime;
    }

    public void setCurrentFetchingTime(int currentFetchingTime) {
        this.currentFetchingTime = currentFetchingTime;
    }

    public int getCurrentStatus() {
        return this.currentStatus;
    }

    public void setCurrentStatus(int currentStatus) {
        this.currentStatus = currentStatus;
    }

    public int getCurrentRetryUploadNum() {
        return this.currentRetryUploadNum;
    }

    public void setCurrentRetryUploadNum(int currentRetryUploadNum) {
        this.currentRetryUploadNum = currentRetryUploadNum;
    }

    public long getCurrentRetryUploadTime() {
        return this.currentRetryUploadTime;
    }

    public void setCurrentRetryUploadTime(long currentRetryUploadTime) {
        this.currentRetryUploadTime = currentRetryUploadTime;
    }

    public String getZipFileName() {
        return this.zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    @Override
    public String toString() {
        return "ModemLogStatus{fetchingTime=" + this.fetchingTime
                + ", currentFetchingTime=" + this.currentFetchingTime
                + ", currentStatus=" + this.currentStatus
                + ", currentRetryUploadNum=" + this.currentRetryUploadNum
                + ", currentRetryUploadTime=" + this.currentRetryUploadTime
                + ", zipFileName='" + this.zipFileName + "'}";
    }
}
