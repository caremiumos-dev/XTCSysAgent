package com.xtc.im.core.common.response.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.response.entity.ResponseEntity;

/**
 * 第三方同步响应（命令 114）：增量同步下来的一条第三方消息。
 *
 * <p>原厂没有 toString，字段语义按 SyncResponseEntity 的 tag 序列（10..15）对齐推断：
 * appKey / alias / data / syncKey / msgId / createTime。</p>
 */
@CommandValue(ImCommand.THIRD_SYNC_RESPONSE)
public class ThirdSyncResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private String appKey;

    @TagValue(11)
    private String alias;

    @TagValue(12)
    private byte[] data;

    @TagValue(13)
    private long syncKey;

    @TagValue(14)
    private String msgId;

    @TagValue(15)
    private long createTime;

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

    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getSyncKey() {
        return this.syncKey;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
