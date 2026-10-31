package com.xtc.im.core.common.tlv;

import android.support.v4.media.TransportMediator;

/**
 * TLV 编码器：把 tag / length / value 三段拼成字节。
 *
 * <p>tag 段和 length 段都用变长编码（bit7 续位），小于 128 的值只占 1 字节，
 * 这样一条普通的协议报文头只有 2 个字节。{@code KEYCODE_MEDIA_PAUSE}（127）
 * 在这里被当掩码用（原厂引用的是 support 库里这个常量），换成字面量也是 127，
 * 但保持引用形式以免与其它 127 的用途混淆。</p>
 *
 * <p>原厂类名为单字母 {@code g}。</p>
 */
public class TLVEncoder {

    /** 帧类型：普通帧。 */
    public static final int FRAME_TYPE_NORMAL = 0;

    /** 帧类型：嵌套帧（tag 段 bit6）。 */
    public static final int FRAME_TYPE_NESTED = 64;

    /** 数据类型：裸值。 */
    public static final int DATA_TYPE_RAW = 0;

    /** 数据类型：容器（tag 段 bit5），值里再套一串 TLV。 */
    public static final int DATA_TYPE_CONTAINER = 32;

    /**
     * 多字节 tag 需要几个额外字节（{@code ceil(log128(1 + tagValue))}）。
     */
    private static double tagByteCount(double tagValue) {
        if (tagValue < 31.0d) {
            throw new IllegalArgumentException("the tag value must not less than 31.");
        }
        return Math.ceil(log(1.0d + tagValue, 128.0d));
    }

    /**
     * 换底公式求对数。
     */
    public static double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }

    public static TLVEncodeResult encode(int frameType, int dataType, int tagValue, long value) {
        return encode(frameType, dataType, tagValue, TLVNumberUtils.toBytes(value));
    }

    public static TLVEncodeResult encode(int frameType, int dataType, int tagValue, String value) {
        return value != null ? encode(frameType, dataType, tagValue, value.getBytes())
                : encode(frameType, dataType, tagValue, (byte[]) null);
    }

    /**
     * 组装一条 TLV。
     *
     * @param frameType 帧类型
     * @param dataType  数据类型
     * @param tagValue  tag
     * @param value     值，null 视为长度 0
     */
    public static TLVEncodeResult encode(int frameType, int dataType, int tagValue, byte[] value) {
        byte[] tagBytes = packTag(frameType, dataType, tagValue);
        byte[] lengthBytes = encodeLength(value == null ? 0 : value.length);
        TLVEncodeResult result = new TLVEncodeResult();
        result.setTagBytes(tagBytes);
        result.setTagSize(tagBytes.length);
        result.setLengthBytes(lengthBytes);
        result.setLengthSize(lengthBytes.length);
        result.setValueBytes(value);
        result.setValueSize(value != null ? value.length : 0);
        return result;
    }

    /**
     * 编码 length 段。
     */
    public static byte[] encodeLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("the length must not less than 0.");
        }
        if (length < 128) {
            return new byte[]{(byte) (length & TransportMediator.KEYCODE_MEDIA_PAUSE)};
        }
        int groupCount = (int) lengthByteCount(length);
        return packLengthBytes(packLengthWithGroups(0, groupCount, length), groupCount);
    }

    /**
     * 把已打包的 tag 整数摊成字节（多留 1 字节给首字节）。
     */
    private static byte[] packTagBytes(int value, int extraBytes) {
        byte[] bytes = new byte[extraBytes + 1];
        int length = bytes.length;
        bytes[0] = (byte) ((value >> (extraBytes * 8)) & 255);
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((value >> ((extraBytes - i) * 8)) & 255);
        }
        return bytes;
    }

    /**
     * 编码 tag 段。
     *
     * <p>tag 值 < 31 时只用首字节的低 5 位；否则首字节低 5 位置 1 表示"还有后续"，
     * 后续字节每 7 位一组，续位标记在 bit7。</p>
     */
    public static byte[] packTag(int frameType, int dataType, int tagValue) {
        int packed = frameType | dataType | tagValue;
        int extraBytes = 0;
        if (tagValue >= 31) {
            extraBytes = (int) tagByteCount(tagValue);
            packed = packTagWithContinuation(((frameType | dataType) | 128) << (extraBytes * 8),
                    extraBytes, tagValue);
        }
        return packTagBytes(packed, extraBytes);
    }

    /**
     * 多字节长度需要几个额外字节。
     */
    private static double lengthByteCount(int length) {
        return Math.ceil(log(length + 1, 128.0d));
    }

    /**
     * 把长度的 7 位分组顺序填进整数的高位部分。
     */
    private static int packLengthWithGroups(int prefix, int groupCount, int length) {
        int packed = prefix;
        for (int i = groupCount - 1; i > 0; i--) {
            packed |= (((length >> (i * 7)) & TransportMediator.KEYCODE_MEDIA_PAUSE) | 128) << (i * 8);
        }
        return (length & TransportMediator.KEYCODE_MEDIA_PAUSE) | packed;
    }

    /**
     * 把已打包的长度整数摊成字节。
     */
    private static byte[] packLengthBytes(int value, int byteCount) {
        byte[] bytes = new byte[byteCount];
        int length = bytes.length;
        bytes[0] = (byte) ((value >> ((byteCount - 1) * 8)) & 255);
        for (int i = 1; i < length; i++) {
            bytes[i] = (byte) ((value >> (((byteCount - i) - 1) * 8)) & 255);
        }
        return bytes;
    }

    /**
     * 从高位往低位把 tag 的 7 位分组填进整数，续位标记打在每组的 bit7。
     */
    private static int packTagWithContinuation(int prefix, int groupCount, int tagValue) {
        int packed = prefix;
        for (int i = 0; i < groupCount - 1; i++) {
            packed |= (((tagValue >> (i * 7)) & TransportMediator.KEYCODE_MEDIA_PAUSE) | 128)
                    << (((groupCount - 1) - i) * 8);
        }
        return ((tagValue >> ((groupCount - 1) * 7)) & TransportMediator.KEYCODE_MEDIA_PAUSE) | packed;
    }
}
