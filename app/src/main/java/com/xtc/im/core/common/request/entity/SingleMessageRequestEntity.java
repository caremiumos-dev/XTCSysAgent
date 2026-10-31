package com.xtc.im.core.common.request.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 单条消息发送请求（命令 40）。
 *
 * <p>与命令 9 的区别是收件人：这里是 receiverId / accountId（点对点），
 * 而不是 dialogId（会话）。服务端处理完仍然回命令 10 的消息响应。</p>
 */
@CommandValue(ImCommand.SINGLE_MESSAGE_REQUEST)
public class SingleMessageRequestEntity extends RequestEntity implements Parcelable {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int needResponse;

    @TagValue(10)
    private long receiverId;

    @TagValue(11)
    private long accountId;

    @TagValue(12)
    private long registId;

    @TagValue(13)
    private int msgType;

    @TagValue(14)
    private byte[] data;

    @TagValue(15)
    private String msgId;

    @TagValue(16)
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

    public long getReceiverId() {
        return this.receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public long getAccountId() {
        return this.accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
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

    public static final Parcelable.Creator<SingleMessageRequestEntity> CREATOR =
            new Parcelable.Creator<SingleMessageRequestEntity>() {
                @Override
                public SingleMessageRequestEntity createFromParcel(Parcel parcel) {
                    return new SingleMessageRequestEntity(parcel);
                }

                @Override
                public SingleMessageRequestEntity[] newArray(int size) {
                    return new SingleMessageRequestEntity[size];
                }
            };

    public SingleMessageRequestEntity() {
    }

    /** 从 Parcel 还原；data 的 null 问题见 MessageRequestEntity 的说明。 */
    protected SingleMessageRequestEntity(Parcel parcel) {
        this.rid = parcel.readInt();
        this.needResponse = parcel.readInt();
        this.receiverId = parcel.readLong();
        this.accountId = parcel.readLong();
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
        parcel.writeLong(this.receiverId);
        parcel.writeLong(this.accountId);
        parcel.writeLong(this.registId);
        parcel.writeInt(this.msgType);
        parcel.writeByteArray(this.data);
        parcel.writeString(this.msgId);
        parcel.writeInt(this.contentType);
    }

    @Override
    public String toString() {
        return "SingleMessageRequestEntity{RID=" + this.rid + ", needResponse=" + this.needResponse
                + ", receiverId=" + this.receiverId + ", accountId=" + this.accountId
                + ", registId=" + this.registId + ", msgType=" + this.msgType
                + ", msgId='" + this.msgId + "', contentType=" + this.contentType + '}';
    }
}
