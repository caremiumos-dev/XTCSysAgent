package com.xtc.im.core.common.tlv;

import com.xtc.im.core.common.ImLogTag;
import com.xtc.log.LogUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * 可链式调用的 TLV 构造器：一次 {@code put} 写一个 tag，最后 {@code toBytes()} 拿报文。
 *
 * <p>容器节点（{@code DATA_TYPE_CONTAINER}）的值必须是**另一个 TLVObject 的字节**，
 * 所以嵌套 put 时会先递归把自己的缓冲区序列化出来再当成值写入。</p>
 *
 * <p>原厂类名为单字母 {@code j}，日志 tag 就是 {@code TLVObject}。</p>
 */
public class TLVObject {

    static final String TAG = ImLogTag.tag("TLVObject");

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     * 写一个容器节点：值为空/长度为 0 时整体跳过（空容器没有任何意义）。
     */
    private void putNested(int tagValue, TLVObject nested) {
        if (nested == null || nested.size() <= 0) {
            return;
        }
        try {
            this.buffer.write(TLVEncoder.encode(TLVEncoder.FRAME_TYPE_NORMAL,
                    TLVEncoder.DATA_TYPE_CONTAINER, tagValue, nested.toBytes()).toBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 写一个裸值节点。
     */
    private void putValue(int tagValue, byte[] value) throws IOException {
        this.buffer.write(TLVEncoder.encode(TLVEncoder.FRAME_TYPE_NORMAL,
                TLVEncoder.DATA_TYPE_RAW, tagValue, value).toBytes());
    }

    /** 已写入的字节数。 */
    public int size() {
        return this.buffer.size();
    }

    public TLVObject put(int tagValue, long value) throws IOException {
        putValue(tagValue, TLVNumberUtils.toBytes(value));
        return this;
    }

    public TLVObject put(int tagValue, TLVObject nested) {
        putNested(tagValue, nested);
        return this;
    }

    public TLVObject put(int tagValue, String value) throws IOException {
        if (value != null) {
            putValue(tagValue, value.getBytes());
        } else {
            putValue(tagValue, (byte[]) null);
        }
        return this;
    }

    public TLVObject put(int tagValue, byte[] value) throws IOException {
        putValue(tagValue, value);
        return this;
    }

    public byte[] toBytes() {
        return this.buffer.toByteArray();
    }

    /**
     * 整个报文按二进制字符串输出（调试用）。
     *
     * <p>用 {@code BigInteger} 转二进制是因为它会把前导 0 吃掉，得到最短表示——
     * 原厂就用它做日志，换成别的写法会打出不同的字符串。</p>
     */
    public String toBinaryString() {
        return new BigInteger(1, this.buffer.toByteArray()).toString(2);
    }

    @Override
    public String toString() {
        try {
            return TLVCodec.decode(this.buffer.toByteArray()).toString();
        } catch (Throwable throwable) {
            LogUtil.e(TAG, throwable);
            return null;
        }
    }
}
