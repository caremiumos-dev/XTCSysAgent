package com.xtc.httplib.bean;

/**
 * IM 长连接的域名配置（服务端下发配置里的 {@code imDomain} 节点）。
 *
 * <p>四个字段两两成对：正式环境一组、灰度环境一组；每组的"主域名"形如
 * {@code host:port}，"备用域名"是一个 JSON 数组字符串（原厂直接用
 * {@code JSONUtil.toJSON(String[])} 序列化出来的结果）。</p>
 */
public class ImDomain {

    /** 正式环境主域名，形如 host:port。 */
    private String imJoinDomain;
    /** 灰度环境主域名。 */
    private String imJoinGreyDomain;
    /** 正式环境备用域名列表（JSON 数组字符串）。 */
    private String imBackupDomain;
    /** 灰度环境备用域名列表（JSON 数组字符串）。 */
    private String imGreyBackupDomain;

    public String getImBackupDomain() {
        return this.imBackupDomain;
    }

    public String getImGreyBackupDomain() {
        return this.imGreyBackupDomain;
    }

    public String getImJoinDomain() {
        return this.imJoinDomain;
    }

    public String getImJoinGreyDomain() {
        return this.imJoinGreyDomain;
    }

    public void setImBackupDomain(String imBackupDomain) {
        this.imBackupDomain = imBackupDomain;
    }

    public void setImGreyBackupDomain(String imGreyBackupDomain) {
        this.imGreyBackupDomain = imGreyBackupDomain;
    }

    public void setImJoinDomain(String imJoinDomain) {
        this.imJoinDomain = imJoinDomain;
    }

    public void setImJoinGreyDomain(String imJoinGreyDomain) {
        this.imJoinGreyDomain = imJoinGreyDomain;
    }

    @Override
    public String toString() {
        return "ImDomain{imJoinDomain='" + this.imJoinDomain
                + "', imJoinGreyDomain='" + this.imJoinGreyDomain
                + "', imBackupDomain='" + this.imBackupDomain
                + "', imGreyBackupDomain='" + this.imGreyBackupDomain + "'}";
    }
}
