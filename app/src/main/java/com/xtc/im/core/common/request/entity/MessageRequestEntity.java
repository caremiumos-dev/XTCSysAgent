package com.xtc.im.core.common.request.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 查 / 发消息请求（命令 9）。
 *
 * <p>会话内消息的主通道：dialogId + imAccountId + registId 三元组定位会话，
 * msgType 区分消息种类，真正的消息体放在 tag=14 的字节数组里（已加密）。</p>
 *
 * <p>needResponse 为 0 时服务端不回响应，用来发「不需要确认」的消息。</p>
 */
@CommandValue(ImCommand.MESSAGE_REQUEST)
public class MessageRequestEntity extends RequestEntity implements Parcelable {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int needResponse;

    @TagValue(10)
    private long dialogId;

    @TagValue(11)
    private long imAccountId;

    @TagValue(12)
    private long registId;

    @TagValue(13)
    private int msgType;

    @TagValue(14)
    private byte[] data;

    @TagValue(15)
    private String msgId;

    @TagValue(18)
    private int contentType;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public int getNeedResponse() {
        return this.needResponse;
    }

    public void setNeedResponse(int needResponse) {
        this.needResponse = needResponse;
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

    public int getMsgType() {
        return this.msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getContentType() {
        return this.contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public static final Parcelable.Creator<MessageRequestEntity> CREATOR =
            new Parcelable.Creator<MessageRequestEntity>() {
                @Override
                public MessageRequestEntity createFromParcel(Parcel parcel) {
                    return new MessageRequestEntity(parcel);
                }

                @Override
                public MessageRequestEntity[] newArray(int size) {
                    return new MessageRequestEntity[size];
                }
            };

    public MessageRequestEntity() {
    }

    /**
     * 从 Parcel 还原。
     *
     * <p>注意 readByteArray(this.data) 传的是还没初始化的 null：原厂就这么写，
     * 只有报文里 data 为 null 时才不炸。这条路径（反序列化请求实体）实际不会被走到，
     * 所以没人发现，保留原样。</p>
     */
    protected MessageRequestEntity(Parcel parcel) {
        this.rid = parcel.readInt();
        this.needResponse = parcel.readInt();
        this.dialogId = parcel.readLong();
        this.imAccountId = parcel.readLong();
        this.registId = parcel.readLong();
        this.msgType = parcel.readInt();
        parcel.readByteArray(this.data);
        this.msgId = parcel.readString();
        this.contentType = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.rid);
        parcel.writeInt(this.needResponse);
        parcel.writeLong(this.dialogId);
        parcel.writeLong(this.imAccountId);
        parcel.writeLong(this.registId);
        parcel.writeInt(this.msgType);
        parcel.writeByteArray(this.data);
        parcel.writeString(this.msgId);
        parcel.writeInt(this.contentType);
    }

    @Override
    public String toString() {
        return "MessageRequestEntity{RID=" + this.rid + ", needResponse=" + this.needResponse
                + ", dialogId=" + this.dialogId + ", imAccountId=" + this.imAccountId
                + ", registId=" + this.registId + ", msgType=" + this.msgType
                + ", msgId='" + this.msgId + "', contentType=" + this.contentType + '}';
    }
}
