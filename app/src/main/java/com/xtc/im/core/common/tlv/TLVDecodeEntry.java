package com.xtc.im.core.common.tlv;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * "这段原始字节 → 这个解码结果"的注册项。
 *
 * <p>IM 会反复解同一类报文（心跳、同步应答），把首次解析结果按原始字节缓存起来，
 * 下次字节完全相同就直接命中，省掉一次遍历解析。{@code hitCount} 只用于统计，
 * 不影响命中判定。</p>
 *
 * <p>原厂类名为单字母 {@code e}。</p>
 */
public class TLVDecodeEntry {

    /** 注册时的原始字节。 */
    private byte[] rawData;

    /** 对应的解码结果（多条报文可共用同一结果对象）。 */
    private TLVDecodeResult decodeResult;

    /** 命中次数。 */
    private AtomicInteger hitCount = new AtomicInteger(0);

    public TLVDecodeEntry() {
    }

    public TLVDecodeEntry(byte[] rawData, TLVDecodeResult decodeResult) {
        this.rawData = rawData;
        this.decodeResult = decodeResult;
    }

    public int getHitCount() {
        return this.hitCount.get();
    }

    /**
     * 尝试命中：字节完全一致才算命中，命中计数 +1。
     *
     * @return 命中时返回缓存的解码结果，否则 null
     */
    public TLVDecodeResult match(byte[] rawData) {
        if (rawData == null || rawData.length <= 0 || !Arrays.equals(rawData, this.rawData)) {
            return null;
        }
        this.hitCount.incrementAndGet();
        return this.decodeResult;
    }
}
