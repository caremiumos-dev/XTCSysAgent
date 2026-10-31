package com.xtc.qiniu.bean;

/**
 * 上传 token 的本地缓存结构（token + 获取时间戳）。
 */
public class NetUploadToken {

    private String token;
    private Long timestamp;

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NetUploadToken{token='" + this.token + "', timestamp=" + this.timestamp + '}';
    }
}
