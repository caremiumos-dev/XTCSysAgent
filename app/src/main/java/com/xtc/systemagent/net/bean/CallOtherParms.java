package com.xtc.systemagent.net.bean;

/**
 * 查询"其他通话日志"时的附加参数。
 */
public class CallOtherParms {

    private String accountId;
    private Long id;
    private String callerWatchId;
    private String logType;

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "CallOtherParms{accountId='" + this.accountId
                + "', id=" + this.id
                + ", callerWatchId='" + this.callerWatchId
                + "', logType='" + this.logType + "'}";
    }
}
