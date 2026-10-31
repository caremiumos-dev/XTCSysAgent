package com.xtc.systemagent.net.bean;

/**
 * 系统配置下发/回执参数。
 */
public class SysConfigCbParam {

    private String bindNum;
    private int pushId;
    private String configMsg;
    private String cbMsg;

    public String getBindNum() {
        return this.bindNum;
    }

    public void setBindNum(String bindNum) {
        this.bindNum = bindNum;
    }

    public int getPushId() {
        return this.pushId;
    }

    public void setPushId(int pushId) {
        this.pushId = pushId;
    }

    public String getConfigMsg() {
        return this.configMsg;
    }

    public void setConfigMsg(String configMsg) {
        this.configMsg = configMsg;
    }

    public String getCbMsg() {
        return this.cbMsg;
    }

    public void setCbMsg(String cbMsg) {
        this.cbMsg = cbMsg;
    }
}
