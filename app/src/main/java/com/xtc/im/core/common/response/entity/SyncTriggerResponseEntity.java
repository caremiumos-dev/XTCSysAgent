package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 触发同步响应（命令 26）：只有公共的 rid/code/desc 三个字段，真正的数据在随后推下来的同步命令里。
 */
@CommandValue(ImCommand.SYNC_TRIGGER_RESPONSE)
public class SyncTriggerResponseEntity extends ResponseEntity {

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
        return "SyncTriggerResponseEntity{RID=" + this.rid + ", code=" + this.code + ", desc='" + this.desc + "'}";
    }
}
