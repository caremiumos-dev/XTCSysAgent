package com.xtc.im.core.common.voice.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImEntity;

/**
 * 语音消息的描述体：不含语音数据本身，只描述它放在哪、多长、带什么位置信息。
 *
 * <p>语音数据走切片通道（{@code VoiceSliceResponseEntity} + {@code SliceReceiver} 拼装），
 * 这个实体负责「先告诉对方有这么一段语音」，所以它带 storeAddr（存储地址）、
 * resourceKey（资源键）和 deadline（过期时间）。</p>
 *
 * <p>位置信息同样是 gd / bd / gg 三套坐标。原厂类名就是 {@code VoiceDescEntity}。</p>
 */
@CommandValue(0)
public class VoiceDescEntity extends ImEntity implements Parcelable {

    @TagValue(10)
    private int vocTime;

    @TagValue(11)
    private String storeAddr;

    @TagValue(12)
    private String groupId;

    @TagValue(13)
    private int lastIndex;

    @TagValue(14)
    private String gdLatitude;

    @TagValue(15)
    private String gdLongitude;

    @TagValue(16)
    private int gdRadius;

    @TagValue(17)
    private String bdLatitude;

    @TagValue(18)
    private String bdLongitude;

    @TagValue(19)
    private int bdRadius;

    @TagValue(20)
    private String ggLatitude;

    @TagValue(21)
    private String ggLongitude;

    @TagValue(22)
    private int ggRadius;

    @TagValue(23)
    private String resourceKey;

    @TagValue(24)
    private long deadline;

    @TagValue(25)
    private String extra;

    public int getVocTime() {
        return this.vocTime;
    }

    public void setVocTime(int vocTime) {
        this.vocTime = vocTime;
    }

    public String getStoreAddr() {
        return this.storeAddr;
    }

    public void setStoreAddr(String storeAddr) {
        this.storeAddr = storeAddr;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getLastIndex() {
        return this.lastIndex;
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

    public String getGdLatitude() {
        return this.gdLatitude;
    }

    public void setGdLatitude(String gdLatitude) {
        this.gdLatitude = gdLatitude;
    }

    public String getGdLongitude() {
        return this.gdLongitude;
    }

    public void setGdLongitude(String gdLongitude) {
        this.gdLongitude = gdLongitude;
    }

    public int getGdRadius() {
        return this.gdRadius;
    }

    public void setGdRadius(int gdRadius) {
        this.gdRadius = gdRadius;
    }

    public String getBdLatitude() {
        return this.bdLatitude;
    }

    public void setBdLatitude(String bdLatitude) {
        this.bdLatitude = bdLatitude;
    }

    public String getBdLongitude() {
        return this.bdLongitude;
    }

    public void setBdLongitude(String bdLongitude) {
        this.bdLongitude = bdLongitude;
    }

    public int getBdRadius() {
        return this.bdRadius;
    }

    public void setBdRadius(int bdRadius) {
        this.bdRadius = bdRadius;
    }

    public String getGgLatitude() {
        return this.ggLatitude;
    }

    public void setGgLatitude(String ggLatitude) {
        this.ggLatitude = ggLatitude;
    }

    public String getGgLongitude() {
        return this.ggLongitude;
    }

    public void setGgLongitude(String ggLongitude) {
        this.ggLongitude = ggLongitude;
    }

    public int getGgRadius() {
        return this.ggRadius;
    }

    public void setGgRadius(int ggRadius) {
        this.ggRadius = ggRadius;
    }

    public String getResourceKey() {
        return this.resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public long getDeadline() {
        return this.deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public String getExtra() {
        return this.extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public static final Parcelable.Creator<VoiceDescEntity> CREATOR =
            new Parcelable.Creator<VoiceDescEntity>() {
                @Override
                public VoiceDescEntity createFromParcel(Parcel parcel) {
                    return new VoiceDescEntity(parcel);
                }

                @Override
                public VoiceDescEntity[] newArray(int size) {
                    return new VoiceDescEntity[size];
                }
            };

    public VoiceDescEntity() {
    }

    protected VoiceDescEntity(Parcel parcel) {
        this.vocTime = parcel.readInt();
        this.storeAddr = parcel.readString();
        this.groupId = parcel.readString();
        this.lastIndex = parcel.readInt();
        this.gdLatitude = parcel.readString();
        this.gdLongitude = parcel.readString();
        this.gdRadius = parcel.readInt();
        this.bdLatitude = parcel.readString();
        this.bdLongitude = parcel.readString();
        this.bdRadius = parcel.readInt();
        this.ggLatitude = parcel.readString();
        this.ggLongitude = parcel.readString();
        this.ggRadius = parcel.readInt();
        this.resourceKey = parcel.readString();
        this.deadline = parcel.readLong();
        this.extra = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.vocTime);
        parcel.writeString(this.storeAddr);
        parcel.writeString(this.groupId);
        parcel.writeInt(this.lastIndex);
        parcel.writeString(this.gdLatitude);
        parcel.writeString(this.gdLongitude);
        parcel.writeInt(this.gdRadius);
        parcel.writeString(this.bdLatitude);
        parcel.writeString(this.bdLongitude);
        parcel.writeInt(this.bdRadius);
        parcel.writeString(this.ggLatitude);
        parcel.writeString(this.ggLongitude);
        parcel.writeInt(this.ggRadius);
        parcel.writeString(this.resourceKey);
        parcel.writeLong(this.deadline);
        parcel.writeString(this.extra);
    }

    @Override
    public String toString() {
        return "VoiceDescEntity{vocTime=" + this.vocTime + ", storeAddr='" + this.storeAddr
                + "', groupId='" + this.groupId + "', lastIndex=" + this.lastIndex
                + ", gdLatitude='" + this.gdLatitude + "', gdLongitude='" + this.gdLongitude
                + "', gdRadius=" + this.gdRadius + ", bdLatitude='" + this.bdLatitude
                + "', bdLongitude='" + this.bdLongitude + "', bdRadius=" + this.bdRadius
                + ", ggLatitude='" + this.ggLatitude + "', ggLongitude='" + this.ggLongitude
                + "', ggRadius=" + this.ggRadius + ", resoureKey='" + this.resourceKey
                + "', deadline=" + this.deadline + ", extra='" + this.extra + "'}";
    }
}
