package com.xtc.systemagent.module.sys.bean;

/**
 * 系统配置读写的 IMM 指令体。
 */
public class SysConfigImContent {

    private long id;
    private String configMsg;
    private int configAction;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getConfigMsg() {
        return this.configMsg;
    }

    public void setConfigMsg(String configMsg) {
        this.configMsg = configMsg;
    }

    public int getConfigAction() {
        return this.configAction;
    }

    public void setConfigAction(int configAction) {
        this.configAction = configAction;
    }

    @Override
    public String toString() {
        return "SysConfigImContent{id=" + this.id
                + ", configMsg='" + this.configMsg
                + "', configAction=" + this.configAction + '}';
    }
}
