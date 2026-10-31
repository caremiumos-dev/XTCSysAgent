package com.xtc.dns.client;

/**
 * 从 ContentProvider 取回来的一条"域名 → IP"记录（原厂 {@code com.xtc.dns.client.contract}）。
 *
 * <p>provider 侧其实返回整行 {@code DomainModel}，但客户端只用得到 host 与 ip，
 * 所以落地成这个轻量结构；{@code startTime/stopTime/code} 是原厂预留的埋点字段，
 * 目前只有构造时写入的 startTime 有值。</p>
 */
public class DomainInfo {

    /** 请求的 url（原厂预留，当前无写入点）。 */
    private String url;

    /** 域名。 */
    private String host;

    /** 解析出的 ip。 */
    private String ip;

    /** 响应体（原厂预留）。 */
    private String data = null;

    /** 创建时间戳（毫秒），构造时取当前时间。 */
    private String startTime = String.valueOf(System.currentTimeMillis());

    /** 结束时间戳（原厂预留）。 */
    private String stopTime = null;

    /** 业务码（原厂预留）。 */
    private String code = null;

    public DomainInfo(String host, String ip) {
        this.host = host;
        this.ip = ip;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStopTime() {
        return this.stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /** 日志用，格式与原厂逐字节一致（字段顺序不能变）。 */
    @Override
    public String toString() {
        return "DomainInfo{url='" + this.url + "', host='" + this.host + "', ip='" + this.ip
                + "', data='" + this.data + "', startTime='" + this.startTime
                + "', stopTime='" + this.stopTime + "', code='" + this.code + "'}";
    }
}
