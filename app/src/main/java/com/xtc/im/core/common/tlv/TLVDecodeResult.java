package com.xtc.im.core.common.tlv;

import java.util.Arrays;
import java.util.List;

/**
 * 一个 TLV 节点解析出来的结果：帧类型、数据类型、tag、长度、值。
 *
 * <p>{@code dataType == 32} 时 {@code value} 不是 {@code byte[]} 而是
 * {@code List<TLVDecodeResult>}（容器节点，值里又嵌套了一串 TLV），
 * 所以取值方法都会先判类型再用，不能用就返回 0/null——协议里出现异常报文时
 * 宁可返回空值让上层跳过，也不要抛异常把整条链路搞断。</p>
 *
 * <p>原厂类名为单字母 {@code c}。</p>
 */
public class TLVDecodeResult {

    /** 帧类型（bit6）。 */
    private int frameType;

    /** 数据类型（bit5）：32 表示容器。 */
    private int dataType;

    /** tag 值。 */
    private int tagValue;

    /** 长度字段的值。 */
    private int length;

    /** 值：{@code byte[]} 或 {@code List<TLVDecodeResult>}。 */
    private Object value;

    /**
     * 值按整数读。
     */
    public int getValueAsInt() {
        if (this.value instanceof byte[]) {
            return (int) TLVNumberUtils.toLong((byte[]) this.value);
        }
        return 0;
    }

    /**
     * 在容器节点里按 tag 找一个子节点。
     */
    public TLVDecodeResult findChild(int tagValue) {
        if (this.dataType == 32) {
            for (TLVDecodeResult child : (List<TLVDecodeResult>) this.value) {
                if (child.getTagValue() == tagValue) {
                    return child;
                }
            }
        }
        return null;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * 值按 long 读。
     */
    public long getValueAsLong() {
        if (this.value instanceof byte[]) {
            return TLVNumberUtils.toLong((byte[]) this.value);
        }
        return 0L;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    /**
     * 值按 UTF-8 字符串读。
     */
    public String getValueAsString() {
        if (this.value instanceof byte[]) {
            return new String((byte[]) this.value);
        }
        return null;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getFrameType() {
        return this.frameType;
    }

    public void setTagValue(int tagValue) {
        this.tagValue = tagValue;
    }

    public int getDataType() {
        return this.dataType;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getTagValue() {
        return this.tagValue;
    }

    public int getLength() {
        return this.length;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        String valueText;
        if (this.dataType == 32) {
            valueText = this.value != null ? this.value.toString() : null;
        } else {
            valueText = Arrays.toString((byte[]) this.value);
        }
        return "TLVDecodeResult [frameType=" + this.frameType + ", dataType=" + this.dataType
                + ", tagValue=" + this.tagValue + ", length=" + this.length
                + ", value=" + valueText + "]";
    }
}
