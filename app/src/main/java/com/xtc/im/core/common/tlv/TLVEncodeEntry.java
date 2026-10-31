package com.xtc.im.core.common.tlv;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * "这组 tag/类型/值 → 这个编码结果"的注册项。
 *
 * <p>编码比解码更值得缓存：同一条消息会被重发多次，重组 TLV 字节是纯 CPU 开销。</p>
 *
 * <p>原厂类名为单字母 {@code h}。</p>
 */
public class TLVEncodeEntry {

    /** 帧类型。 */
    private int frameType;

    /** 数据类型。 */
    private int dataType;

    /** tag 值。 */
    private int tagValue;

    /** 值字节。 */
    private byte[] value;

    /** 对应的编码结果。 */
    private TLVEncodeResult encodeResult;

    /** 命中次数。 */
    private AtomicInteger hitCount = new AtomicInteger(0);

    public TLVEncodeEntry() {
    }

    public TLVEncodeEntry(int frameType, int dataType, int tagValue, byte[] value,
                          TLVEncodeResult encodeResult) {
        this.frameType = frameType;
        this.dataType = dataType;
        this.tagValue = tagValue;
        this.value = value;
        this.encodeResult = encodeResult;
    }

    public int getHitCount() {
        return this.hitCount.get();
    }

    /**
     * 尝试命中：四个维度（帧类型、数据类型、tag、值）全部一致才算命中。
     */
    public TLVEncodeResult match(int frameType, int dataType, int tagValue, byte[] value) {
        if (frameType != this.frameType || dataType != this.dataType || tagValue != this.tagValue
                || !Arrays.equals(value, this.value)) {
            return null;
        }
        this.hitCount.incrementAndGet();
        return this.encodeResult;
    }
}
