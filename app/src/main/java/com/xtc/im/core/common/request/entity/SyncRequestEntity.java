package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 同步请求（命令 12）：按 syncKey 增量拉取会话消息。
 *
 * <p>pageSize 是单次拉取上限；mode 已废弃（原厂标了 @Deprecated），保留字段兼容老服务端。</p>
 */
@CommandValue(ImCommand.SYNC_REQUEST)
public class SyncRequestEntity extends RequestEntity {

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

    @TagValue(14)
    private int pageSize;

    @Deprecated
    @TagValue(15)
    private int mode;

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

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Deprecated
    public int getMode() {
        return this.mode;
    }

    @Deprecated
    public void setMode(int mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "SyncRequestEntity{RID=" + this.rid + ", dialogId=" + this.dialogId
                + ", imAccountId=" + this.imAccountId + ", registId=" + this.registId
                + ", syncKey=" + this.syncKey + ", pageSize=" + this.pageSize
                + ", mode=" + this.mode + '}';
    }
}
