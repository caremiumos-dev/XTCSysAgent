package com.xtc.systemagent.net.bean;

/**
 * 拉取日志策略时的查询参数。
 */
public class LogStrategyParam {

    private String accountId;
    private String appPackage;

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

    @Override
    public String toString() {
        return "LogStrategyParam{accountId='" + this.accountId
                + "', appPackage='" + this.appPackage + "'}";
    }
}
