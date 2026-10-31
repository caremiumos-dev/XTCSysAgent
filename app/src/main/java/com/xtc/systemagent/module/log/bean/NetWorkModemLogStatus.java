package com.xtc.systemagent.module.log.bean;

/**
 * 网络侧 modem 日志的抓取与上传状态。
 */
public class NetWorkModemLogStatus {

    private int currentStatus;
    private String strategyTime;
    private String zipFileName;

    /**
     * 状态机取值。注意这里比 {@link ModemLogStatus.Status} 多一个 FETCHED。
     */
    public interface Status {
        int IDLE = 0;
        int MONITORING = 1;
        int FETCHING = 2;
        int FETCHED = 3;
        int PRE_UPLOAD = 4;
        int UPLOAD_FAILURE = 5;
        int UPLOAD_SUCCESS = 6;
        int FETCH_LOG_FAILURE = 7;
    }

    public NetWorkModemLogStatus() {
        this.currentStatus = Status.IDLE;
        this.strategyTime = "";
    }

    public NetWorkModemLogStatus(String strategyTime) {
        this.currentStatus = Status.IDLE;
        this.strategyTime = "";
        this.strategyTime = strategyTime;
    }

    public int getCurrentStatus() {
        return this.currentStatus;
    }

    public void setCurrentStatus(int currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getStrategyTime() {
        return this.strategyTime;
    }

    public void setStrategyTime(String strategyTime) {
        this.strategyTime = strategyTime;
    }

    public String getZipFileName() {
        return this.zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }
}
