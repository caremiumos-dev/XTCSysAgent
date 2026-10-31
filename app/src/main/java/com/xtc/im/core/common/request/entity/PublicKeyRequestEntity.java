package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 公钥请求（命令 22）：只有一个请求号，服务端据此回公钥响应。
 */
@CommandValue(ImCommand.PUBLIC_KEY_REQUEST)
public class PublicKeyRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    @Override
    public String toString() {
        return "PublicKeyRequestEntity{RID=" + this.rid + '}';
    }
}
