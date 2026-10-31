package com.xtc.systemagent.net.bean;

import com.xtc.utils.encode.JSONUtil;

/**
 * 日志上传完成后的上报参数。
 *
 * <p>服务端靠 {@code strategyId + machineId + key} 三者定位这一次采集任务，
 * {@code collectStatus} 表示采集结果。</p>
 */
public class UploadNotifyParam {

    private Long id;
    private Integer strategyId;
    private String machineId;
    private String accountId;
    private String operatorId;
    private String appPackage;
    private Integer spaceType;
    private Integer collectStatus;
    private String key;
    private String remark;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStrategyId() {
        return this.strategyId;
    }

    public void setStrategyId(Integer strategyId) {
        this.strategyId = strategyId;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOperatorId() {
        return this.operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getAppPackage() {
        return this.appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public Integer getSpaceType() {
        return this.spaceType;
    }

    public void setSpaceType(Integer spaceType) {
        this.spaceType = spaceType;
    }

    public Integer getCollectStatus() {
        return this.collectStatus;
    }

    public void setCollectStatus(Integer collectStatus) {
        this.collectStatus = collectStatus;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return JSONUtil.toJSON(this);
    }
}
