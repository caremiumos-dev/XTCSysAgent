package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 消息合并响应（命令 41）。
 *
 * <p>字段 tag 与基类一样（2=状态码、3=描述），但原厂给它另起了一套访问器
 * （getRespCode/getRespDesc）且**不覆写基类**，所以基类的 getCode 在这条响应上恒为 0。
 * 这个坑原样保留：调用方必须用 getRespCode 才拿得到值。</p>
 */
@CommandValue(ImCommand.MESSAGE_MERGE_RESPONSE)
public class MessageMergeResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int respCode;

    @TagValue(3)
    private String respDesc;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public int getRespCode() {
        return this.respCode;
    }

    public void setRespCode(int respCode) {
        this.respCode = respCode;
    }

    public String getRespDesc() {
        return this.respDesc;
    }

    public void setRespDesc(String respDesc) {
        this.respDesc = respDesc;
    }

    @Override
    public String toString() {
        return "MessageMergeResponseEntity{RID=" + this.rid + ", respCode=" + this.respCode
                + ", respDesc='" + this.respDesc + "'}";
    }
}
