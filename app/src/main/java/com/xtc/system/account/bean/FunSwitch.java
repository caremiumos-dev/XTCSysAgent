package com.xtc.system.account.bean;

/**
 * 功能开关。
 */
public class FunSwitch {

    private int switchId;
    private int switchStatus;

    public int getSwitchId() {
        return this.switchId;
    }

    public void setSwitchId(int switchId) {
        this.switchId = switchId;
    }

    public int getSwitchStatus() {
        return this.switchStatus;
    }

    public void setSwitchStatus(int switchStatus) {
        this.switchStatus = switchStatus;
    }

    @Override
    public String toString() {
        return "FunSwitch{switchId=" + this.switchId
                + ", switchStatus=" + this.switchStatus + '}';
    }
}
