package com.xtc.im.core.common.tlv;

/**
 * TLV 数值与字节的互转。协议里所有整数都是**大端**、且按有效位宽取最短表示：
 * 能放 1 字节就绝不写 4 字节，这是给弱网省流量的关键约定。
 *
 * <p>原厂类名为单字母 {@code k}。</p>
 */
public class TLVNumberUtils {

    /**
     * 大端字节数组转 long。只接受 1/2/4/8 字节，其它长度直接抛异常。
     */
    public static long toLong(byte[] value) {
        int length = value.length;
        if (length == 1) {
            return (byte) (value[0] & 255);
        }
        if (length == 2) {
            return (short) (((value[0] & 255) << 8) | ((value[1] & 255) << 0));
        }
        if (length == 4) {
            return ((value[0] & 255) << 24) | ((value[1] & 255) << 16)
                    | ((value[2] & 255) << 8) | ((value[3] & 255) << 0);
        }
        if (length != 8) {
            throw new IllegalArgumentException("the length of byte array is uncorrected.");
        }
        return (((long) (value[0] & 255)) << 56) | (((long) (value[1] & 255)) << 48)
                | (((long) (value[2] & 255)) << 40) | (((long) (value[3] & 255)) << 32)
                | (((long) (value[4] & 255)) << 24) | (((long) (value[5] & 255)) << 16)
                | (((long) (value[6] & 255)) << 8) | (((long) (value[7] & 255)) << 0);
    }

    /**
     * long 转大端字节数组，按能容纳的最短位宽输出。
     *
     * <p>最后一个分支的判据是"值和自己的 longValue 相等"，恒为真，
     * 等价于"前面都不匹配就用 8 字节"——原厂如此，保留这个写法以免位宽判断顺序变化。</p>
     */
    public static byte[] toBytes(long value) {
        Long boxed = Long.valueOf(value);
        if (boxed.longValue() == boxed.byteValue()) {
            return toBytes(value, 1);
        }
        if (boxed.longValue() == boxed.shortValue()) {
            return toBytes(value, 2);
        }
        if (boxed.longValue() == boxed.intValue()) {
            return toBytes(value, 4);
        }
        if (boxed.longValue() == boxed.longValue()) {
            return toBytes(value, 8);
        }
        throw new IllegalArgumentException("the value [" + value + "] is too large.");
    }

    /** 定长大端输出。 */
    private static byte[] toBytes(long value, int byteCount) {
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            bytes[i] = (byte) (value >>> (((byteCount - i) - 1) * 8));
        }
        return bytes;
    }
}
