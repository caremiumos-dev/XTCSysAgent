package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 同步结束应答（命令 15）：确认服务端下发的「同步结束」已经处理完。
 *
 * <p>注意访问器与字段的对应关系**不对称**：setImAccountId 走 tag=11，
 * setDialogId（已废弃）走 tag=10，而 getImAccountId 返回的也是 tag=11、getDialogId 返回 tag=10。
 * 原厂就是这样，改写时若「顺手对称化」会读错位点。</p>
 */
@CommandValue(ImCommand.SYNC_FINISH_ACK)
public class SyncFinishAckRequestEntity extends RequestEntity {

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

    public long getImAccountId() {
        return this.imAccountId;
    }

    public void setImAccountId(long imAccountId) {
        this.imAccountId = imAccountId;
    }

    @Deprecated
    public long getDialogId() {
        return this.dialogId;
    }

    @Deprecated
    public void setDialogId(long dialogId) {
        this.dialogId = dialogId;
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
        return "SyncFinishAckRequestEntity{RID=" + this.rid + ", dialogId=" + this.dialogId
                + ", imAccountId=" + this.imAccountId + ", registId=" + this.registId
                + ", syncKey=" + this.syncKey + '}';
    }
}
