package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 通用错误响应（命令 1000）：服务端用这个命令回任何无法归类的失败。
 *
 * <p>它带 rid，所以能对上是哪条请求出的错——这是排障时最有用的一条报文。</p>
 */
@CommandValue(ImCommand.ERROR)
public class ErrorResponseEntity extends ResponseEntity {

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
        return "ErrorResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "'}";
    }
}
