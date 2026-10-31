package com.xtc.systemagent.module.log.bean;

/**
 * tcpdump 抓包的状态。
 */
public class TcpDumpStatus {

    /** 最多重试上传的次数。 */
    public static final int MAX_RETRY_UPLOAD_NUM = 10;

    private int currentStatus = 0;
    private int currentRetryUploadNum = 0;
    private long currentRetryUploadTime = 0;
    /** 是否为独立触发的抓包（与通话联动触发时为 false）。 */
    private boolean isIndependent = true;
    private String zipFileName;

    /**
     * 状态机取值。
     */
    public interface Status {
        int IDLE = 0;
        int FETCHING = 1;
        int PRE_UPLOAD = 2;
        int UPLOAD_FAILURE = 3;
        int UPLOAD_SUCCESS = 4;
        int MODEM_FETCHING = 5;
        int MODEM_PRE_UPLOAD = 6;
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

    public boolean getIsIndependent() {
        return this.isIndependent;
    }

    public void setIsIndependent(boolean isIndependent) {
        this.isIndependent = isIndependent;
    }

    public String getZipFileName() {
        return this.zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    @Override
    public String toString() {
        return "TcpDumpStatus{currentStatus=" + this.currentStatus
                + ", currentRetryUploadNum=" + this.currentRetryUploadNum
                + ", currentRetryUploadTime=" + this.currentRetryUploadTime
                + ", isIndependent=" + this.isIndependent
                + ", zipFileName='" + this.zipFileName + "'}";
    }
}
