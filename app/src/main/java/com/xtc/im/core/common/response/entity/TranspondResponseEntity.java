package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * HTTP 透传响应（命令 21）：把 IM 通道拿回来的 HTTP 响应交还给请求方。
 *
 * <p>tag=10 响应体、tag=11 响应头、tag=12 HTTP 状态码。
 * 上层（PreprocessorInterceptor）拿不到状态码时按 200 处理，所以这里的 statusCode 只用于排障。</p>
 */
@CommandValue(ImCommand.TRANSPOND_RESPONSE)
public class TranspondResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private byte[] responseBody;

    @TagValue(11)
    private byte[] responseHeader;

    @TagValue(12)
    private int statusCode;

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

    public byte[] getResponseBody() {
        return this.responseBody;
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    public byte[] getResponseHeader() {
        return this.responseHeader;
    }

    public void setResponseHeader(byte[] responseHeader) {
        this.responseHeader = responseHeader;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return "TranspondResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "', statusCode=" + this.statusCode + '}';
    }
}
