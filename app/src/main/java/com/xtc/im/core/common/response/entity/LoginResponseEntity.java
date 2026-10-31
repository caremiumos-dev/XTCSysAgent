package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 登录响应（命令 4）：下发对称密钥与服务器时间。
 *
 * <p>serverTime 用来校正设备时钟——IM 的消息排序依赖服务端时间，
 * 手表本地时间不准时靠它兜底。</p>
 */
@CommandValue(ImCommand.LOGIN_RESPONSE)
public class LoginResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private String encryptKey;

    @TagValue(11)
    private int encryptType;

    @TagValue(12)
    private long serverTime;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getEncryptKey() {
        return this.encryptKey;
    }

    public void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    public int getEncryptType() {
        return this.encryptType;
    }

    public void setEncryptType(int encryptType) {
        this.encryptType = encryptType;
    }

    public long getServerTime() {
        return this.serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    @Override
    public String toString() {
        return "LoginResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "', encryptKey='" + this.encryptKey
                + "', encryptType=" + this.encryptType + ", serverTime=" + this.serverTime + '}';
    }
}
