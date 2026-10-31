package com.xtc.systemagent.net.bean;

/**
 * RPM 调试模式状态参数。
 */
public class RpmStateParam {

    private String bindNum;
    private String rpmMode;

    public String getBindNum() {
        return this.bindNum;
    }

    public void setBindNum(String bindNum) {
        this.bindNum = bindNum;
    }

    public String getRpmMode() {
        return this.rpmMode;
    }

    public void setRpmMode(String rpmMode) {
        this.rpmMode = rpmMode;
    }
}
