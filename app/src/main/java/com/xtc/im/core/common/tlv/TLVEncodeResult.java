package com.xtc.im.core.common.tlv;

import java.util.Arrays;

/**
 * 一次 TLV 编码的结果：tag / length / value 三段字节及其长度。
 *
 * <p>拆开存是为了让上层可以只取其中一段（例如拼帧时只关心 value 段），
 * {@link #toBytes()} 再把三段拼回完整报文。</p>
 *
 * <p>原厂类名为单字母 {@code f}。</p>
 */
public class TLVEncodeResult {

    /** tag 段长度。 */
    private int tagSize;

    /** length 段长度。 */
    private int lengthSize;

    /** value 段长度。 */
    private int valueSize;

    /** tag 段字节。 */
    private byte[] tagBytes;

    /** length 段字节。 */
    private byte[] lengthBytes;

    /** value 段字节。 */
    private byte[] valueBytes;

    public void setTagSize(int tagSize) {
        this.tagSize = tagSize;
    }

    public void setTagBytes(byte[] tagBytes) {
        this.tagBytes = tagBytes;
    }

    /**
     * 拼成完整报文。value 段允许为 null（长度为 0 的 TLV）。
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[this.tagSize + this.lengthSize + this.valueSize];
        System.arraycopy(this.tagBytes, 0, bytes, 0, this.tagSize);
        System.arraycopy(this.lengthBytes, 0, bytes, this.tagSize, this.lengthSize);
        if (this.valueBytes != null) {
            System.arraycopy(this.valueBytes, 0, bytes, this.tagSize + this.lengthSize, this.valueSize);
        }
        return bytes;
    }

    public int getTagSize() {
        return this.tagSize;
    }

    public void setLengthSize(int lengthSize) {
        this.lengthSize = lengthSize;
    }

    public void setLengthBytes(byte[] lengthBytes) {
        this.lengthBytes = lengthBytes;
    }

    public int getLengthSize() {
        return this.lengthSize;
    }

    public void setValueSize(int valueSize) {
        this.valueSize = valueSize;
    }

    public void setValueBytes(byte[] valueBytes) {
        this.valueBytes = valueBytes;
    }

    public int getValueSize() {
        return this.valueSize;
    }

    public byte[] getTagBytes() {
        return this.tagBytes;
    }

    public byte[] getLengthBytes() {
        return this.lengthBytes;
    }

    public byte[] getValueBytes() {
        return this.valueBytes;
    }

    @Override
    public String toString() {
        return "TLVEncodeResult [tagSize=" + this.tagSize + ", lengthSize=" + this.lengthSize
                + ", valueSize=" + this.valueSize + ", tagBytes=" + Arrays.toString(this.tagBytes)
                + ", lengthBytes=" + Arrays.toString(this.lengthBytes)
                + ", valueBytes=" + Arrays.toString(this.valueBytes) + "]";
    }
}
