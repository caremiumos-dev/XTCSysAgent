package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 全量设置请求（命令 18）：把一次完整的 HTTP 请求（url/方法/头/体）交给 IM 通道。
 *
 * <p>三个 setter 返回 this 是原厂为了链式拼装，返回值形态保留原样。</p>
 */
@CommandValue(ImCommand.ALL_SET_REQUEST)
public class AllSetRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long dialogId;

    @TagValue(11)
    private long imAccountId;

    @TagValue(12)
    private long registId;

    @TagValue(13)
    private String url;

    @TagValue(14)
    private int method;

    @TagValue(15)
    private byte[] requestHeader;

    @TagValue(16)
    private byte[] requestBody;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public long getDialogId() {
        return this.dialogId;
    }

    public void setDialogId(long dialogId) {
        this.dialogId = dialogId;
    }

    public long getImAccountId() {
        return this.imAccountId;
    }

    public void setImAccountId(long imAccountId) {
        this.imAccountId = imAccountId;
    }

    public long getRegistId() {
        return this.registId;
    }

    public void setRegistId(long registId) {
        this.registId = registId;
    }

    public String getUrl() {
        return this.url;
    }

    public AllSetRequestEntity setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getMethod() {
        return this.method;
    }

    public AllSetRequestEntity setMethod(int method) {
        this.method = method;
        return this;
    }

    public byte[] getRequestHeader() {
        return this.requestHeader;
    }

    public AllSetRequestEntity setRequestHeader(byte[] requestHeader) {
        this.requestHeader = requestHeader;
        return this;
    }

    public byte[] getRequestBody() {
        return this.requestBody;
    }

    public void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public String toString() {
        return "AllSetRequestEntity{RID=" + this.rid + ", dialogId=" + this.dialogId
                + ", imAccountId=" + this.imAccountId + ", registId=" + this.registId
                + ", url='" + this.url + "', method=" + this.method + '}';
    }
}
