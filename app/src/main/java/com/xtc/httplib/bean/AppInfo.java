package com.xtc.httplib.bean;

/**
 * 宿主应用信息，由 {@code OnGetAppInfoListener} 提供给 HTTP 库，
 * 用于拼请求头（版本、灰度、加密开关）。
 */
public class AppInfo {

    private String version;
    private String grey;
    private String encSwitch;
    private String rsaPublicKey;

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGrey() {
        return this.grey;
    }

    public void setGrey(String grey) {
        this.grey = grey;
    }

    public String getEncSwitch() {
        return this.encSwitch;
    }

    public void setEncSwitch(String encSwitch) {
        this.encSwitch = encSwitch;
    }

    public String getRsaPublicKey() {
        return this.rsaPublicKey;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }
}
