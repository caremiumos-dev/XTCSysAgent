package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 注测请求（命令 1）。
 *
 * <p>这是 IM 链路的第一个报文：把 App 与设备信息一次性登记上来，服务端据此分配 registId。
 * 字段全是宿主提供的信息（{@code DeviceInfoProvider} 就是它的取值接口），
 * 库本身不读任何系统属性，避免权限问题。</p>
 */
@CommandValue(ImCommand.REGIST_REQUEST)
public class RegistRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private int platform;

    @TagValue(11)
    private String appKey;

    @TagValue(12)
    private String pkgName;

    @TagValue(13)
    private String deviceId;

    @TagValue(14)
    private int sdkVersion;

    @TagValue(15)
    private String sysName;

    @TagValue(16)
    private String sysVersion;

    @TagValue(17)
    private String imei;

    @TagValue(18)
    private String imsi;

    @TagValue(19)
    private String mac;

    @TagValue(20)
    private String modelNumber;

    @TagValue(21)
    private String basebandVersion;

    @TagValue(22)
    private String buildNumber;

    @TagValue(23)
    private String resolution;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public int getPlatform() {
        return this.platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getSdkVersion() {
        return this.sdkVersion;
    }

    public void setSdkVersion(int sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getSysName() {
        return this.sysName;
    }

    public void setSysName(String sysName) {
        this.sysName = sysName;
    }

    public String getSysVersion() {
        return this.sysVersion;
    }

    public void setSysVersion(String sysVersion) {
        this.sysVersion = sysVersion;
    }

    public String getImei() {
        return this.imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getImsi() {
        return this.imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getModelNumber() {
        return this.modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getBasebandVersion() {
        return this.basebandVersion;
    }

    public void setBasebandVersion(String basebandVersion) {
        this.basebandVersion = basebandVersion;
    }

    public String getBuildNumber() {
        return this.buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getResolution() {
        return this.resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @Override
    public String toString() {
        return "RegistRequestEntity{RID=" + this.rid + ", platform=" + this.platform
                + ", appKey='" + this.appKey + "', pkgName='" + this.pkgName
                + "', deviceId='" + this.deviceId + "', sdkVerison=" + this.sdkVersion
                + ", sysName='" + this.sysName + "', sysVersion='" + this.sysVersion
                + "', imei='" + this.imei + "', imsi='" + this.imsi + "', mac='" + this.mac
                + "', modelNumber='" + this.modelNumber + "', basebandVersion='" + this.basebandVersion
                + "', buildNumber='" + this.buildNumber + "', resolution='" + this.resolution + "'}";
    }
}
