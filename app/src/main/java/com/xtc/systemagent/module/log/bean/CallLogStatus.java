package com.xtc.systemagent.module.log.bean;

/**
 * 通话日志（CallLog）的抓取与上传状态。
 */
public class CallLogStatus {

    /** 最多重试上传的次数。 */
    public static final int MAX_RETRY_UPLOAD_NUM = 10;

    private int fetchingTime;
    private int currentFetchingTime;
    private int currentStatus;
    private int currentRetryUploadNum = 0;
    private long currentRetryUploadTime = 0;
    private String zipFileName;

    /**
     * 状态机取值。
     */
    public interface Status {
        int DEFAULT = 0;
        int MONITORING = 1;
        int FETCHING = 2;
        int PRE_UPLOAD = 3;
        int UPLOAD_FAILURE = 4;
        int UPLOAD_SUCCESS = 5;
        int FETCH_LOG_FAILURE = 6;
    }

    public static int getMaxRetryUploadNum() {
        return MAX_RETRY_UPLOAD_NUM;
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
        // 原厂 toString 用的是 "CallFailureLogStatus" 前缀，属于对外文本，保持原样。
        return "CallFailureLogStatus{fetchingTime=" + this.fetchingTime
                + ", currentFetchingTime=" + this.currentFetchingTime
                + ", currentStatus=" + this.currentStatus
                + ", currentRetryUploadNum=" + this.currentRetryUploadNum
                + ", currentRetryUploadTime=" + this.currentRetryUploadTime
                + ", zipFileName='" + this.zipFileName + "'}";
    }
}
