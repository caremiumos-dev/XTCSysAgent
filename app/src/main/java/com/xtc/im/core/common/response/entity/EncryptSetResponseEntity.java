package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 加密设置响应（命令 34）：只有公共的 rid/code/desc 三个字段。
 *
 * <p>状态码 5200 是加密协商完成，见 {@code PushResponse.isEncryptStatus5200()}。</p>
 */
@CommandValue(ImCommand.ENCRYPT_SET_RESPONSE)
public class EncryptSetResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

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

    @Override
    public String toString() {
        return "EncryptSetResponseEntity{RID=" + this.rid + ", code=" + this.code + ", desc='" + this.desc + "'}";
    }
}
