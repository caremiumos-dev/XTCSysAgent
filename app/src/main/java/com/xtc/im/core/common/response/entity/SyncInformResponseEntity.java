package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 同步通知（命令 11）：服务端主动告诉客户端「有变更，来拉」。
 *
 * <p>只有会话定位和位点，没有请求号（不需要回执）。tag=10 的 dialogId 已废弃。</p>
 */
@CommandValue(ImCommand.SYNC_INFORM)
public class SyncInformResponseEntity extends ResponseEntity {

    @Deprecated
    @TagValue(10)
    private long dialogId;

    @TagValue(11)
    private long imAccountId;

    @TagValue(12)
    private long syncKey;

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

    public long getSyncKey() {
        return this.syncKey;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    @Override
    public String toString() {
        return "SyncInformResponseEntity{dialogId=" + this.dialogId + ", imAccountId="
                + this.imAccountId + ", syncKey=" + this.syncKey + '}';
    }
}
