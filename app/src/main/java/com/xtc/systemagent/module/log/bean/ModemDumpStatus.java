package com.xtc.systemagent.module.log.bean;

/**
 * Modem dump 的上传状态。
 *
 * <p>状态对象会被序列化成 JSON 存进 SharedPreferences，字段名即保持原样。</p>
 */
public class ModemDumpStatus {

    /** 最多重试上传的次数。 */
    public static final int MAX_RETRY_UPLOAD_NUM = 5;

    private int currentRetryUploadNum = 0;

    public int getCurrentRetryUploadNum() {
        return this.currentRetryUploadNum;
    }

    public void setCurrentRetryUploadNum(int currentRetryUploadNum) {
        this.currentRetryUploadNum = currentRetryUploadNum;
    }

    @Override
    public String toString() {
        return "ModemDumpStatus{, currentRetryUploadNum=" + this.currentRetryUploadNum + '}';
    }
}
