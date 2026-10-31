package com.xtc.im.core.app.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 推给宿主的一条 IM 消息（跨进程载体）。
 *
 * <p>字段比协议实体多两个业务标记：{@code thirdSyncMsg}（是否第三方同步来的）、
 * {@code notify}（是否需要弹通知）。这两个标记决定了宿主收到后走哪条分支，
 * 所以必须跟着消息一起跨进程传。</p>
 *
 * <p>原厂类名已经是 {@code PushMessage}。</p>
 */
public class PushMessage implements Parcelable {

    public static final Parcelable.Creator<PushMessage> CREATOR =
            new Parcelable.Creator<PushMessage>() {
                @Override
                public PushMessage createFromParcel(Parcel parcel) {
                    return new PushMessage(parcel);
                }

                @Override
                public PushMessage[] newArray(int size) {
                    return new PushMessage[size];
                }
            };

    /** 是否第三方同步消息。 */
    private boolean thirdSyncMsg;

    /** 是否需要通知。 */
    private boolean notify;

    /** 会话 id。 */
    private long dialogId;

    /** 账号 id。 */
    private long imAccountId;

    /** 注测 id。 */
    private long registId;

    /** 消息类型。 */
    private int msgType;

    /** 消息体（加密字节）。 */
    private byte[] data;

    /** 同步位点。 */
    private long syncKey;

    /** 消息 id。 */
    private String msgId;

    /** 来源包名。 */
    private String pkgName;

    /** 第三方别名。 */
    private String alias;

    /** 创建时间。 */
    private long createTime;

    /** 内容类型。 */
    private int contentType;

    public PushMessage() {
    }

    protected PushMessage(Parcel parcel) {
        this.thirdSyncMsg = parcel.readByte() != 0;
        this.notify = parcel.readByte() != 0;
        this.dialogId = parcel.readLong();
        this.imAccountId = parcel.readLong();
        this.registId = parcel.readLong();
        this.msgType = parcel.readInt();
        this.data = parcel.createByteArray();
        this.syncKey = parcel.readLong();
        this.msgId = parcel.readString();
        this.pkgName = parcel.readString();
        this.alias = parcel.readString();
        this.createTime = parcel.readLong();
        this.contentType = parcel.readInt();
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public void setDialogId(long dialogId) {
        this.dialogId = dialogId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public void setThirdSyncMsg(boolean thirdSyncMsg) {
        this.thirdSyncMsg = thirdSyncMsg;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isThirdSyncMsg() {
        return this.thirdSyncMsg;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public void setImAccountId(long imAccountId) {
        this.imAccountId = imAccountId;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public boolean isNotify() {
        return this.notify;
    }

    public long getDialogId() {
        return this.dialogId;
    }

    public void setRegistId(long registId) {
        this.registId = registId;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public long getImAccountId() {
        return this.imAccountId;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    public long getRegistId() {
        return this.registId;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getMsgType() {
        return this.msgType;
    }

    public byte[] getData() {
        return this.data;
    }

    public long getSyncKey() {
        return this.syncKey;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public String getAlias() {
        return this.alias;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public int getContentType() {
        return this.contentType;
    }

    @Override
    public String toString() {
        return "PushMessage{syncKey=" + this.syncKey + ", contentType=" + this.contentType
                + ", msgType=" + this.msgType + ", msgId='" + this.msgId
                + "', createTime=" + this.createTime + ", thirdSyncMsg=" + this.thirdSyncMsg
                + ", notify=" + this.notify + ", dialogId=" + this.dialogId
                + ", imAccountId=" + this.imAccountId + ", registId=" + this.registId
                + ", pkgName='" + this.pkgName + "', alias='" + this.alias + "'}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByte((byte) (this.thirdSyncMsg ? 1 : 0));
        parcel.writeByte((byte) (this.notify ? 1 : 0));
        parcel.writeLong(this.dialogId);
        parcel.writeLong(this.imAccountId);
        parcel.writeLong(this.registId);
        parcel.writeInt(this.msgType);
        parcel.writeByteArray(this.data);
        parcel.writeLong(this.syncKey);
        parcel.writeString(this.msgId);
        parcel.writeString(this.pkgName);
        parcel.writeString(this.alias);
        parcel.writeLong(this.createTime);
        parcel.writeInt(this.contentType);
    }
}
