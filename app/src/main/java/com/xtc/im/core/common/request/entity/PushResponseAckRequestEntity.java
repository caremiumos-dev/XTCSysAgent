package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 推送消息应答（命令 36）：告诉服务端这条推送已经落地，可以推进 syncKey。
 *
 * <p>tag=10 的 dialogId 已废弃（原厂标了 @Deprecated），新代码只用 imAccountId；
 * 字段和访问器都保留，因为老版本服务端仍然会读它。</p>
 */
@CommandValue(ImCommand.PUSH_RESPONSE_ACK)
public class PushResponseAckRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @Deprecated
    @TagValue(10)
    private long dialogId;

    @TagValue(11)
    private long imAccountId;

    @TagValue(12)
    private long registId;

    @TagValue(13)
    private long syncKey;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    @Deprecated
    public long getDialogId() {
        return this.dialogId;
    }

    @Deprecated
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

    public long getSyncKey() {
        return this.syncKey;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    @Override
    public String toString() {
        return "PushResponseAckRequestEntity{RID=" + this.rid + ", dialogId=" + this.dialogId
                + ", imAccountId=" + this.imAccountId + ", registId=" + this.registId
                + ", syncKey=" + this.syncKey + '}';
    }
}
