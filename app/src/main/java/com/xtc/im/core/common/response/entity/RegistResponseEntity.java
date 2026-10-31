package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 注测响应（命令 2）：下发 registId 与 registToken，后续登录要用。
 */
@CommandValue(ImCommand.REGIST_RESPONSE)
public class RegistResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private long registId;

    @TagValue(11)
    private String registToken;

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

    public long getRegistId() {
        return this.registId;
    }

    public void setRegistId(long registId) {
        this.registId = registId;
    }

    public String getRegistToken() {
        return this.registToken;
    }

    public void setRegistToken(String registToken) {
        this.registToken = registToken;
    }

    @Override
    public String toString() {
        return "RegistResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "', registId=" + this.registId
                + ", registToken='" + this.registToken + "'}";
    }
}
