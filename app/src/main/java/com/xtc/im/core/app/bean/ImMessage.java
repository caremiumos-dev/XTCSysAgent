package com.xtc.im.core.app.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 一条 IM 消息的内容体（对宿主暴露的最简形态）。
 *
 * <p>只有四个字段：内容、类型、手表 id、时间戳。真正上线传输的是它的加密字节，
 * 这个类用于跨进程把消息交给宿主时使用。</p>
 *
 * <p>字段名来自原厂 toString 的打印名，原厂类名已经是 {@code ImMessage}。</p>
 */
public class ImMessage implements Parcelable {

    public static final Parcelable.Creator<ImMessage> CREATOR =
            new Parcelable.Creator<ImMessage>() {
                @Override
                public ImMessage createFromParcel(Parcel parcel) {
                    return new ImMessage(parcel);
                }

                @Override
                public ImMessage[] newArray(int size) {
                    return new ImMessage[size];
                }
            };

    /** 消息内容。 */
    private String content;

    /** 消息类型。 */
    private Integer type;

    /** 手表 id。 */
    private String watchId;

    /** 时间戳（毫秒）。 */
    private Long timestamp;

    public ImMessage() {
    }

    protected ImMessage(Parcel parcel) {
        this.content = parcel.readString();
        this.type = Integer.valueOf(parcel.readInt());
        this.watchId = parcel.readString();
        this.timestamp = Long.valueOf(parcel.readLong());
    }

    public String getContent() {
        return this.content;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getType() {
        return this.type;
    }

    public void setWatchId(String watchId) {
        this.watchId = watchId;
    }

    public String getWatchId() {
        return this.watchId;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "ImMessage{content='" + this.content + "', type=" + this.type
                + ", watchId='" + this.watchId + "', timestamp=" + this.timestamp + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.content);
        // 这里不做空判：原厂就直接拆箱，字段没填会在写 Parcel 时 NPE，行为保持。
        parcel.writeInt(this.type.intValue());
        parcel.writeString(this.watchId);
        parcel.writeLong(this.timestamp.longValue());
    }
}
