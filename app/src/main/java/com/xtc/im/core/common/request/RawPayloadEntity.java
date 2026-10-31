package com.xtc.im.core.common.request;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;

/**
 * 命令 32 的报文实体：只有一个 tag=10 的裸字节数组。
 *
 * <p>这个命令在协议里没有专属语义（注册表里也只有一个类名），能确定的只有
 * "载荷是一段二进制"。原厂类名与字段名都已混淆，按形态命名为
 * {@code RawPayloadEntity}。</p>
 */
@CommandValue(ImCommand.RAW_PAYLOAD)
public class RawPayloadEntity extends ImEntity {

    @TagValue(10)
    private byte[] payload;

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getPayload() {
        return this.payload;
    }
}
