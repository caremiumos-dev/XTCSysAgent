package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 业务账号绑定响应（命令 6）：下发 imAccountId 与 accountToken。
 */
@CommandValue(ImCommand.ACCOUNT_RESPONSE)
public class AccountResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private long imAccountId;

    @TagValue(11)
    private String accountToken;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getImAccountId() {
        return this.imAccountId;
    }

    public void setImAccountId(long imAccountId) {
        this.imAccountId = imAccountId;
    }

    public String getAccountToken() {
        return this.accountToken;
    }

    public void setAccountToken(String accountToken) {
        this.accountToken = accountToken;
    }

    @Override
    public String toString() {
        return "AccountResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "', imAccountId=" + this.imAccountId
                + ", accountToken='" + this.accountToken + "'}";
    }
}
