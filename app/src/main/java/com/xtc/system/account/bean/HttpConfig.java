package com.xtc.system.account.bean;

/**
 * initService 下发的网络配置。
 */
public class HttpConfig {

    private String rsaPublicKey;
    private String encSwitch;
    private String grey;

    public HttpConfig() {
    }

    /**
     * 构造器参数的顺序是 (grey, encSwitch, rsaPublicKey) ——
     * 和字段声明顺序不一致，调用时容易写错，注意对照。
     */
    public HttpConfig(String grey, String encSwitch, String rsaPublicKey) {
        this.grey = grey;
        this.encSwitch = encSwitch;
        this.rsaPublicKey = rsaPublicKey;
    }

    public String getRsaPublicKey() {
        return this.rsaPublicKey;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public String getEncSwitch() {
        return this.encSwitch;
    }

    public void setEncSwitch(String encSwitch) {
        this.encSwitch = encSwitch;
    }

    public String getGrey() {
        return this.grey;
    }

    public void setGrey(String grey) {
        this.grey = grey;
    }

    @Override
    public String toString() {
        return "HttpConfig{encSwitch='" + this.encSwitch
                + "', grey='" + this.grey
                + "', rsaPublicKey='" + this.rsaPublicKey + "'}";
    }
}
