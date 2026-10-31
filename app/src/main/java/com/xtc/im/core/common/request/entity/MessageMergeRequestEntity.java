package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 消息合并请求（命令 29）：把一个会话里多条消息合并成一次上行，省流量。
 *
 * <p>消息体在 tag=10 的字节数组里，已经是拼好的多消息块，本类不展开。</p>
 */
@CommandValue(ImCommand.MESSAGE_MERGE_REQUEST)
public class MessageMergeRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private byte[] data;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MessageMergeRequestEntity{RID=" + this.rid + '}';
    }
}
