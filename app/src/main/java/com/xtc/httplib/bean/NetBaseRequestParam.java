package com.xtc.httplib.bean;

/**
 * 每个请求都会带的"基础参数"。
 *
 * <p>它被序列化成 JSON 后放进 {@code Base-Request-Param} 请求头；开启加密时，
 * 这个 JSON 串会先做一次 AES 再放进去。服务端靠它识别设备与登录态，
 * 因此字段名（JSON key）一个都不能改。</p>
 *
 * <p>字段顺序即 toString 的输出顺序，保持与线上日志一致。</p>
 */
public class NetBaseRequestParam {

    private String appId;
    private String token;
    private String timestamp;
    private String machineId;
    private String mac;
    private String accountId;
    private String deviceId;
    private String program;
    private String imFlag;
    private Long registId;

    /** 应用 id 取值。 */
    public interface AppId {
        /** 手表端固定为 2。 */
        String WATCH = "2";
    }

    /** 是否经由 IM 通道发出。 */
    public interface ImFlag {
        /** 不走 IM。 */
        String No = "";
        /** 走 IM。 */
        String Yes = "1";
    }

    /** 程序标识。 */
    public interface Program {
        /** 手表端。 */
        String Watch = "watch";
    }

    public String getAccountId() {
        return this.accountId;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getImFlag() {
        return this.imFlag;
    }

    public String getMac() {
        return this.mac;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public String getProgram() {
        return this.program;
    }

    public Long getRegistId() {
        return this.registId;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getToken() {
        return this.token;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setImFlag(String imFlag) {
        this.imFlag = imFlag;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public void setRegistId(Long registId) {
        this.registId = registId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "NetBaseRequestParam{appId='" + this.appId
                + "', token='" + this.token
                + "', timestamp='" + this.timestamp
                + "', machineId='" + this.machineId
                + "', mac='" + this.mac
                + "', accountId='" + this.accountId
                + "', deviceId='" + this.deviceId
                + "', program='" + this.program
                + "', imFlag='" + this.imFlag
                + "', registId=" + this.registId + '}';
    }
}
