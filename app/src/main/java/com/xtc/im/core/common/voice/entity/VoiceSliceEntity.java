package com.xtc.im.core.common.voice.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImEntity;
import java.util.Arrays;

/**
 * 一段语音里的一片。
 *
 * <p>字段分三组：定位（groupId / index）、内容（voc / vocTime / isFin / extra）、
 * 以及三个坐标系的位置信息（gd 高德 / bd 百度 / gg 谷歌）——语音消息要带上发送位置，
 * 三种坐标系同时带是为了兼容不同地图 SDK 的接收方。</p>
 *
 * <p>原厂类名为单字母 {@code a}，toString 里已经写着 {@code VoiceSliceEntity}。</p>
 */
@CommandValue(0)
public class VoiceSliceEntity extends ImEntity {

    @TagValue(10)
    private String groupId;

    @TagValue(11)
    private int index;

    @TagValue(12)
    private byte[] voc;

    @TagValue(13)
    private int vocTime;

    @TagValue(14)
    private int fin;

    @TagValue(15)
    private String gdLatitude;

    @TagValue(16)
    private String gdLongitude;

    @TagValue(17)
    private int gdRadius;

    @TagValue(18)
    private String bdLatitude;

    @TagValue(19)
    private String bdLongitude;

    @TagValue(20)
    private int bdRadius;

    @TagValue(21)
    private String ggLatitude;

    @TagValue(22)
    private String ggLongitude;

    @TagValue(23)
    private int ggRadius;

    @TagValue(24)
    private String extra;

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public byte[] getVoc() {
        return this.voc;
    }

    public void setVoc(byte[] voc) {
        this.voc = voc;
    }

    public int getVocTime() {
        return this.vocTime;
    }

    public void setVocTime(int vocTime) {
        this.vocTime = vocTime;
    }

    public int isFin() {
        return this.fin;
    }

    public void setFin(int fin) {
        this.fin = fin;
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

    public String getExtra() {
        return this.extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    /**
     * 切片类型：{@code isFin} 的取值。
     *
     * <p>1 = 中间片，2 = 末片（末片的 extra 里带资源键）。
     * 原厂内部接口名是单字母 {@code a}。</p>
     */
    public interface SliceType {

        /** 中间片。 */
        int TYPE_MIDDLE = 1;

        /** 末片。 */
        int TYPE_LAST = 2;
    }

    @Override
    public String toString() {
        return "VoiceSliceEntity{groupId='" + this.groupId + "', index=" + this.index
                + ", voc=" + Arrays.toString(this.voc) + ", vocTime=" + this.vocTime
                + ", isFin=" + this.fin + ", gdLatitude='" + this.gdLatitude
                + "', gdLongitude='" + this.gdLongitude + "', gdRadius=" + this.gdRadius
                + ", bdLatitude='" + this.bdLatitude + "', bdLongitude='" + this.bdLongitude
                + "', bdRadius=" + this.bdRadius + ", ggLatitude='" + this.ggLatitude
                + "', ggLongitude='" + this.ggLongitude + "', ggRadius=" + this.ggRadius
                + ", extra='" + this.extra + "'}";
    }
}
