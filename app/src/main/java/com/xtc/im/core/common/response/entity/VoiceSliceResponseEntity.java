package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 语音切片（命令 24）：语音消息按片下发，客户端按 groupId 攒齐后拼成整段。
 *
 * <p>拼装逻辑在 {@code SliceReceiver} / {@code SliceReceiverManager} 里。</p>
 */
@CommandValue(ImCommand.VOICE_SLICE)
public class VoiceSliceResponseEntity extends ResponseEntity {

    @TagValue(10)
    private long dialogId;

    @TagValue(11)
    private long imAccountId;

    @TagValue(12)
    private long registId;

    @TagValue(13)
    private byte[] data;

    @TagValue(14)
    private long receiverId;

    @TagValue(15)
    private int contentType;

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

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getReceiverId() {
        return this.receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public int getContentType() {
        return this.contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return "VoiceSliceResponseEntity{dialogId=" + this.dialogId + ", imAccountId="
                + this.imAccountId + ", registId=" + this.registId
                + ", reciverId=" + this.receiverId + ", contentType=" + this.contentType + '}';
    }
}
