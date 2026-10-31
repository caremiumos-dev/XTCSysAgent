package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import java.util.Arrays;

/**
 * 加密设置请求（命令 33）：协商对称加密的算法与密钥。
 *
 * <p>encryptKey 是裸字节，toString 里用 Arrays.toString 打出来是为了排障时能直接比对密钥。</p>
 */
@CommandValue(ImCommand.ENCRYPT_SET_REQUEST)
public class EncryptSetRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private int encryptType;

    @TagValue(11)
    private byte[] encryptKey;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public int getEncryptType() {
        return this.encryptType;
    }

    public void setEncryptType(int encryptType) {
        this.encryptType = encryptType;
    }

    public byte[] getEncryptKey() {
        return this.encryptKey;
    }

    public void setEncryptKey(byte[] encryptKey) {
        this.encryptKey = encryptKey;
    }

    @Override
    public String toString() {
        return "EncryptSetRequestEntity{RID=" + this.rid + ", encryptType=" + this.encryptType
                + ", encryptKey=" + Arrays.toString(this.encryptKey) + '}';
    }
}
