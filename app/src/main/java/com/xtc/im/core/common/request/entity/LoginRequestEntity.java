package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 登录请求（命令 3）。
 *
 * <p>带注测阶段拿到的 registId + registToken 换取登录态，同时上报推送通道信息。
 * 两个 {@code setApnsXxx} 故意返回 this：原厂用链式调用一次性把推送信息串起来赋值。</p>
 */
@CommandValue(ImCommand.LOGIN_REQUEST)
public class LoginRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long registId;

    @TagValue(11)
    private String registToken;

    @TagValue(12)
    private long sdkVersion;

    @TagValue(15)
    private long apnsType;

    @TagValue(16)
    private String apnsToken;

    @TagValue(17)
    private String deviceToken;

    @TagValue(18)
    private String imSdkVersionName;

    @TagValue(19)
    private int pushType;

    @TagValue(20)
    private int platform;

    @TagValue(21)
    private long timestamp;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public long getRegistId() {
        return this.registId;
    }

    public void setRegistId(long registId) {
        this.registId = registId;
    }

    public String getRegistToken() {
        return this.registToken;
    }

    public void setRegistToken(String registToken) {
        this.registToken = registToken;
    }

    public long getSdkVersion() {
        return this.sdkVersion;
    }

    public void setSdkVersion(long sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public long getApnsType() {
        return this.apnsType;
    }

    public LoginRequestEntity setApnsType(long apnsType) {
        this.apnsType = apnsType;
        return this;
    }

    public String getApnsToken() {
        return this.apnsToken;
    }

    public LoginRequestEntity setApnsToken(String apnsToken) {
        this.apnsToken = apnsToken;
        return this;
    }

    public String getDeviceToken() {
        return this.deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getImSdkVersionName() {
        return this.imSdkVersionName;
    }

    public void setImSdkVersionName(String imSdkVersionName) {
        this.imSdkVersionName = imSdkVersionName;
    }

    public int getPushType() {
        return this.pushType;
    }

    public void setPushType(int pushType) {
        this.pushType = pushType;
    }

    public int getPlatform() {
        return this.platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "LoginRequestEntity{RID=" + this.rid + ", registId=" + this.registId
                + ", registToken='" + this.registToken + "', sdkVersion=" + this.sdkVersion
                + ", apnsType=" + this.apnsType + ", apnsToken='" + this.apnsToken
                + "', deviceToken='" + this.deviceToken + "', imSdkVersionName='"
                + this.imSdkVersionName + "', pushType=" + this.pushType
                + ", platform=" + this.platform + ", timestamp=" + this.timestamp + '}';
    }
}
