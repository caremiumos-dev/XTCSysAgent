package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 心跳请求（命令 7）。没有任何字段。
 *
 * <p>心跳是最省字节的报文：一个空值 TLV 就够了，编码由
 * {@code ImEntity.encode()} 里对命令 7 的特判完成。</p>
 */
@CommandValue(ImCommand.HEART_BEAT_REQUEST)
public class HeartBeatRequestEntity extends RequestEntity {

    @Override
    public String toString() {
        return "HeartBeatRequestEntity{Command=" + getCommand() + "}";
    }
}
