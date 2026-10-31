package com.xtc.systemagent.bigData.bean;

/**
 * 通话失败事件的数据体。
 *
 * <p>字段名就是上报到大数据的键名，全部带下划线，不能改成驼峰，
 * 因此 getter/setter 也保留了同样的下划线形态。</p>
 */
public class CallFailureValue {

    private String id;
    private String other_number;
    private String voice_network_type;
    private String self_dial_mode;
    private String call_fail_start_time;
    private String call_fail_cause_code;
    private String call_fail_vendor_cause;
    private String self_cell_id;
    private String netWorkType;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOther_number() {
        return this.other_number;
    }

    public void setOther_number(String otherNumber) {
        this.other_number = otherNumber;
    }

    public String getVoice_network_type() {
        return this.voice_network_type;
    }

    public void setVoice_network_type(String voiceNetworkType) {
        this.voice_network_type = voiceNetworkType;
    }

    public String getSelf_dial_mode() {
        return this.self_dial_mode;
    }

    public void setSelf_dial_mode(String selfDialMode) {
        this.self_dial_mode = selfDialMode;
    }

    public String getCall_fail_start_time() {
        return this.call_fail_start_time;
    }

    public void setCall_fail_start_time(String callFailStartTime) {
        this.call_fail_start_time = callFailStartTime;
    }

    public String getCall_fail_cause_code() {
        return this.call_fail_cause_code;
    }

    public void setCall_fail_cause_code(String callFailCauseCode) {
        this.call_fail_cause_code = callFailCauseCode;
    }

    public String getCall_fail_vendor_cause() {
        return this.call_fail_vendor_cause;
    }

    public void setCall_fail_vendor_cause(String callFailVendorCause) {
        this.call_fail_vendor_cause = callFailVendorCause;
    }

    public String getSelf_cell_id() {
        return this.self_cell_id;
    }

    public void setSelf_cell_id(String selfCellId) {
        this.self_cell_id = selfCellId;
    }

    public String getNetWorkType() {
        return this.netWorkType;
    }

    public void setNetWorkType(String netWorkType) {
        this.netWorkType = netWorkType;
    }

    @Override
    public String toString() {
        return "CallFailureValue{id='" + this.id
                + "', other_number='" + this.other_number
                + "', voice_network_type='" + this.voice_network_type
                + "', self_dial_mode='" + this.self_dial_mode
                + "', call_fail_start_time='" + this.call_fail_start_time
                + "', call_fail_cause_code='" + this.call_fail_cause_code
                + "', call_fail_vendor_cause='" + this.call_fail_vendor_cause
                + "', self_cell_id='" + this.self_cell_id + "'}";
    }
}
