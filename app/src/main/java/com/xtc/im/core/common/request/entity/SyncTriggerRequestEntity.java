package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 触发同步请求（命令 25）：用 accountToken 换一次服务端主动推同步。
 *
 * <p>和命令 12 的区别是「拉」变「推」：这里不指定位点，服务端会把该账号的变更推下来。</p>
 */
@CommandValue(ImCommand.SYNC_TRIGGER_REQUEST)
public class SyncTriggerRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long accountId;

    @TagValue(11)
    private String accountToken;

    @TagValue(12)
    private long registId;

    @TagValue(13)
    private String apnsType;

    @TagValue(14)
    private String apnsToken;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public long getAccountId() {
        return this.accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getAccountToken() {
        return this.accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }

    public long getRegistId() {
        return this.registId;
    }

    public void setRegistId(long registId) {
        this.registId = registId;
    }

    public String getApnsType() {
        return this.apnsType;
    }

    public void setApnsType(String apnsType) {
        this.apnsType = apnsType;
    }

    public String getApnsToken() {
        return this.apnsToken;
    }

    public void setApnsToken(String apnsToken) {
        this.apnsToken = apnsToken;
    }

    @Override
    public String toString() {
        return "SyncTriggerRequestEntity{RID=" + this.rid + ", accountId=" + this.accountId
                + ", accountToken='" + this.accountToken + "', registId=" + this.registId
                + ", apnsType='" + this.apnsType + "', apnsToken='" + this.apnsToken + "'}";
    }
}
