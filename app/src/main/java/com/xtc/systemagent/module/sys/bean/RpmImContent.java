package com.xtc.systemagent.module.sys.bean;

/**
 * 设置 RPM 调试模式的 IMM 指令体。
 */
public class RpmImContent {

    private long id;
    private int rpm_debug_value;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRpm_debug_value() {
        return this.rpm_debug_value;
    }

    public void setRpm_debug_value(int rpmDebugValue) {
        this.rpm_debug_value = rpmDebugValue;
    }

    @Override
    public String toString() {
        return "RpmImContent{id=" + this.id + ", rpm_debug_value=" + this.rpm_debug_value + '}';
    }
}
