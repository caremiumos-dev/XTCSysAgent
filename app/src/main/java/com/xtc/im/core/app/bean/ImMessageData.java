package com.xtc.im.core.app.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 同步下来的一条消息记录：外层信封（会话/账号/位点）+ 内层消息体 {@link ImMessage}。
 *
 * <p>注意 {@link #getCreateTime()} 与 {@link #getContentType()} 返回的是**基本类型**，
 * 其余 getter 返回装箱类型——原厂如此（它们在 getter 里直接拆箱），
 * 所以对象字段为 null 时这两个方法会 NPE，调用前必须先走一遍反序列化。</p>
 *
 * <p>原厂类名已经是 {@code ImMessageData}。</p>
 */
public class ImMessageData implements Parcelable {

    public static final Parcelable.Creator<ImMessageData> CREATOR =
            new Parcelable.Creator<ImMessageData>() {
                @Override
                public ImMessageData createFromParcel(Parcel parcel) {
                    return new ImMessageData(parcel);
                }

                @Override
                public ImMessageData[] newArray(int size) {
                    return new ImMessageData[size];
                }
            };

    /** 会话 id。 */
    private Long dialogId;

    /** 账号 id。 */
    private Long accountId;

    /** 注测 id。 */
    private Long registId;

    /** 消息类型。 */
    private Integer msgType;

    /** 消息体。 */
    private ImMessage message;

    /** 消息 id。 */
    private String msgId;

    /** 同步位点。 */
    private Long syncKey;

    /** 创建时间。 */
    private Long createTime;

    /** 内容类型。 */
    private Integer contentType;

    public ImMessageData() {
    }

    protected ImMessageData(Parcel parcel) {
        this.dialogId = Long.valueOf(parcel.readLong());
        this.accountId = Long.valueOf(parcel.readLong());
        this.registId = Long.valueOf(parcel.readLong());
        this.msgType = Integer.valueOf(parcel.readInt());
        this.message = (ImMessage) parcel.readParcelable(ImMessage.class.getClassLoader());
        this.msgId = parcel.readString();
        this.syncKey = Long.valueOf(parcel.readLong());
        this.createTime = Long.valueOf(parcel.readLong());
        this.contentType = Integer.valueOf(parcel.readInt());
    }

    public Long getDialogId() {
        return this.dialogId;
    }

    public void setContentType(int contentType) {
        this.contentType = Integer.valueOf(contentType);
    }

    public void setCreateTime(long createTime) {
        this.createTime = Long.valueOf(createTime);
    }

    public void setMessage(ImMessage message) {
        this.message = message;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public void setDialogId(Long dialogId) {
        this.dialogId = dialogId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Long getAccountId() {
        return this.accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getRegistId() {
        return this.registId;
    }

    public void setRegistId(Long registId) {
        this.registId = registId;
    }

    public Integer getMsgType() {
        return this.msgType;
    }

    public void setSyncKey(Long syncKey) {
        this.syncKey = syncKey;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ImMessage getMessage() {
        return this.message;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public Long getSyncKey() {
        return this.syncKey;
    }

    public long getCreateTime() {
        return this.createTime.longValue();
    }

    public int getContentType() {
        return this.contentType.intValue();
    }

    @Override
    public String toString() {
        return "ImMessageData{dialogId=" + this.dialogId + ", accountId=" + this.accountId
                + ", registId=" + this.registId + ", msgType=" + this.msgType
                + ", message=" + this.message + ", msgId='" + this.msgId
                + "', syncKey=" + this.syncKey + ", createTime=" + this.createTime
                + ", contentType=" + this.contentType + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(this.dialogId.longValue());
        parcel.writeLong(this.accountId.longValue());
        parcel.writeLong(this.registId.longValue());
        parcel.writeInt(this.msgType.intValue());
        parcel.writeParcelable(this.message, flags);
        parcel.writeString(this.msgId);
        parcel.writeLong(this.syncKey.longValue());
        parcel.writeLong(this.createTime.longValue());
        parcel.writeInt(this.contentType.intValue());
    }
}
