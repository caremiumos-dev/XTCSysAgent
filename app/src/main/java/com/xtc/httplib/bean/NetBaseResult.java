package com.xtc.httplib.bean;

/**
 * 服务端统一响应信封。
 *
 * @param <T> 业务数据类型
 */
public class NetBaseResult<T> {

    /** 业务成功码。 */
    public static final String SUCCESS = "000001";

    private String code;
    private String desc;
    private T data;
    private NetPushError netPushError;
    private String serverGreyCode;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public NetPushError getNetPushError() {
        return this.netPushError;
    }

    public void setNetPushError(NetPushError netPushError) {
        this.netPushError = netPushError;
    }

    public String getServerGreyCode() {
        return this.serverGreyCode;
    }

    public void setServerGreyCode(String serverGreyCode) {
        this.serverGreyCode = serverGreyCode;
    }

    @Override
    public String toString() {
        return "NetBaseResult{code='" + this.code
                + "', desc='" + this.desc
                + "', data=" + this.data
                + ", netPushError=" + this.netPushError
                + ", serverGreyCode='" + this.serverGreyCode + "'}";
    }
}
