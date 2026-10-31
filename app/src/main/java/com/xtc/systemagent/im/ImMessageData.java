package com.xtc.systemagent.im;

/**
 * IMM 同步下来的一条消息记录（外层信封 + {@link ImMessage} 消息体）。
 */
public class ImMessageData {

    private Long dialogId;
    private Long accountId;
    private Long registId;
    private Integer msgType;
    private ImMessage message;
    private String msgId;
    private Long syncKey;
    private long createTime;

    public Long getDialogId() {
        return this.dialogId;
    }

    public void setDialogId(Long dialogId) {
        this.dialogId = dialogId;
    }

    public Long getAccountId() {
        return this.accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getRegistId() {
        return this.registId;
    }

    public void setRegistId(Long registId) {
        this.registId = registId;
    }

    public Integer getMsgType() {
        return this.msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public ImMessage getMessage() {
        return this.message;
    }

    public void setMessage(ImMessage message) {
        this.message = message;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Long getSyncKey() {
        return this.syncKey;
    }

    public void setSyncKey(Long syncKey) {
        this.syncKey = syncKey;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "ImMessageData{dialogId=" + this.dialogId
                + ", accountId=" + this.accountId
                + ", registId=" + this.registId
                + ", msgType=" + this.msgType
                + ", message=" + this.message
                + ", msgId='" + this.msgId
                + "', syncKey=" + this.syncKey
                + ", createTime=" + this.createTime + '}';
    }
}
