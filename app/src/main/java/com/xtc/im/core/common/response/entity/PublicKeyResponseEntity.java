package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 公钥响应（命令 23）：下发服务端公钥相关的三段二进制。
 *
 * <p>三个 byte[] 字段没有 toString 线索，按协议形态推断为 publicKey / encryptKey / encryptSign，
 * tag 顺序（10/11/12）与原厂一致。</p>
 */
@CommandValue(ImCommand.PUBLIC_KEY_RESPONSE)
public class PublicKeyResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private byte[] publicKey;

    @TagValue(11)
    private byte[] encryptKey;

    @TagValue(12)
    private byte[] encryptSign;

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

    public byte[] getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getEncryptKey() {
        return this.encryptKey;
    }

    public void setEncryptKey(byte[] encryptKey) {
        this.encryptKey = encryptKey;
    }

    public byte[] getEncryptSign() {
        return this.encryptSign;
    }

    public void setEncryptSign(byte[] encryptSign) {
        this.encryptSign = encryptSign;
    }

    @Override
    public String toString() {
        return "PublicKeyResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "'}";
    }
}
