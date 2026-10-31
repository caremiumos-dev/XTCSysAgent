package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * HTTP 透传请求（命令 20）：把一次 HTTP 请求塞进 IM 通道，弱网时也能发出去。
 *
 * <p>两个 byte[] 字段的**位置**是确定的（tag=12 在前、tag=13 在后），但哪个是
 * 头 JSON、哪个是加密后的 body 取决于桥接实现：原厂调用方（PreprocessorInterceptor）
 * 传给透传通道的顺序是「头 JSON, 加密 body」，所以这里按位置命名 body(12) / header(13)，
 * 与 {@code com.xtc.im.transpond.TranspondAdapter.transpond} 的形参名保持同一套叫法。
 * 语义有歧义的地方一律以位置为准。</p>
 */
@CommandValue(ImCommand.TRANSPOND_REQUEST)
public class TranspondRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private String url;

    @TagValue(11)
    private int method;

    @TagValue(12)
    private byte[] body;

    @TagValue(13)
    private byte[] header;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMethod() {
        return this.method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getHeader() {
        return this.header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return "TranspondRequestEntity{RID=" + this.rid + ", url='" + this.url
                + "', method=" + this.method + '}';
    }
}
