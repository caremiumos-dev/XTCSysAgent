package com.xtc.utils.encode;

/**
 * 自实现的 Base64 编解码工具（标准字母表，+ / 结尾，= 补齐）。
 *
 * <p>原厂类名被混淆成 {@code b}。整个工程里凡是 "把二进制变成可打印串" 的场合
 * （AES 密文、RSA 密文、upload token）都用它，而不是 android.util.Base64——
 * 因为要能跑在纯 Java 侧、并且要求解码时对空白字符宽容、对非法字符直接判失败。
 * 行为细节（合法字符表、补齐校验、空白剔除）必须与原来逐字节一致，否则老数据解不出来。</p>
 */
public final class Base64Utils {

    /** 解码表覆盖的 ASCII 范围 0~127。 */
    private static final int ASCII_TABLE_SIZE = 128;
    /** Base64 字符表大小。 */
    private static final int ALPHABET_SIZE = 64;
    /** 3 字节 = 24 bit 编成 4 个字符。 */
    private static final int BITS_PER_GROUP = 24;
    /** 1 字节 = 8 bit。 */
    private static final int BITS_PER_BYTE = 8;
    /** 尾部剩 2 字节时按 16 bit 处理。 */
    private static final int BITS_REMAINDER_TWO_BYTES = 16;
    /** 一个 Base64 字符对应 6 bit，即 4 个字符一组。 */
    private static final int BITS_PER_CHAR = 4;
    /** byte 的符号位掩码，用来判断是否需要做无符号右移修正。 */
    private static final int SIGN_BIT_MASK = -128;
    /** 补齐字符。 */
    private static final char PADDING = '=';
    /** 原厂遗留的调试开关，未参与任何分支，保留占位。 */
    private static final boolean DEBUG = false;

    /** 解码表：字符 -> 6 bit 值，-1 表示不是合法 Base64 字符。 */
    private static final byte[] DECODE_TABLE = new byte[ASCII_TABLE_SIZE];
    /** 编码表：6 bit 值 -> 字符。 */
    private static final char[] ENCODE_TABLE = new char[ALPHABET_SIZE];

    static {
        // 先把整张解码表填 -1，再按 A-Z / a-z / 0-9 / + / 的顺序覆盖
        for (int i = 0; i < ASCII_TABLE_SIZE; i++) {
            DECODE_TABLE[i] = -1;
        }
        for (int i = 'Z'; i >= 'A'; i--) {
            DECODE_TABLE[i] = (byte) (i - 'A');
        }
        for (int i = 'z'; i >= 'a'; i--) {
            DECODE_TABLE[i] = (byte) ((i - 'a') + 26);
        }
        for (int i = '9'; i >= '0'; i--) {
            DECODE_TABLE[i] = (byte) ((i - '0') + 52);
        }
        DECODE_TABLE['+'] = 62;
        DECODE_TABLE['/'] = 63;

        // 编码表：0~25 -> A~Z
        for (int i = 0; i <= 25; i++) {
            ENCODE_TABLE[i] = (char) (i + 'A');
        }
        // 26~51 -> a~z
        int index = 26;
        int offset = 0;
        while (index <= 51) {
            ENCODE_TABLE[index] = (char) (offset + 'a');
            index++;
            offset++;
        }
        // 52~61 -> 0~9
        int digitIndex = 52;
        int digitOffset = 0;
        while (digitIndex <= 61) {
            ENCODE_TABLE[digitIndex] = (char) (digitOffset + '0');
            digitIndex++;
            digitOffset++;
        }
        ENCODE_TABLE[62] = '+';
        ENCODE_TABLE[63] = '/';
    }

    /**
     * 剔除空白字符并把有效字符前移压缩，返回压缩后的长度。
     *
     * <p>原厂是在传入的 char[] 上原地压缩的，调用方拿到的数组内容会变。</p>
     */
    private static int compactWhitespace(char[] chars) {
        int writePos = 0;
        if (chars != null) {
            int length = chars.length;
            for (int readPos = 0; readPos < length; readPos++) {
                if (!isWhitespace(chars[readPos])) {
                    chars[writePos] = chars[readPos];
                    writePos++;
                }
            }
        }
        return writePos;
    }

    /** 判断是否为空白字符（只有空格、回车、换行、制表符四种）。 */
    private static boolean isWhitespace(char value) {
        return value == ' ' || value == '\r' || value == '\n' || value == '\t';
    }

    /** 判断是否为补齐字符 '='。 */
    private static boolean isPadding(char value) {
        return value == '=';
    }

    /** 判断字符是否在编码表内（ASCII 范围内且解码表不为 -1）。 */
    private static boolean isValidChar(char value) {
        return value < ASCII_TABLE_SIZE && DECODE_TABLE[value] != -1;
    }

    /**
     * 模拟无符号右移。
     *
     * <p>原厂这段是老式写法：符号位为 0 时直接算术右移即可；符号位为 1 时算术右移会把
     * 高位补成 1，所以再异或一个掩码把补出来的位清掉（掩码分别是 0xC0 / 0xF0 / 0xFC）。
     * 这里保持同样的三元判断和掩码，保证任何输入下取出的 6 bit 都完全一致。</p>
     *
     * @param value 原始字节
     * @param shift 右移位数
     * @param mask  负数时需要异或的掩码
     */
    private static byte unsignedShiftRight(byte value, int shift, int mask) {
        if ((value & SIGN_BIT_MASK) == 0) {
            return (byte) (value >> shift);
        }
        return (byte) ((value >> shift) ^ mask);
    }

    /**
     * 编码：字节数组 -> Base64 字符串。
     *
     * @param data 待编码字节；为 null 时返回 null，长度为 0 时返回空串
     */
    public static String encode(byte[] data) {
        if (data == null) {
            return null;
        }
        int bitLength = data.length * BITS_PER_BYTE;
        if (bitLength == 0) {
            return "";
        }
        int remainder = bitLength % BITS_PER_GROUP;
        int groupCount = bitLength / BITS_PER_GROUP;
        // 不足一组也要占 4 个字符（尾部用 = 补齐）
        char[] out = new char[(remainder != 0 ? groupCount + 1 : groupCount) * 4];
        int inPos = 0;
        int outPos = 0;
        for (int i = 0; i < groupCount; i++) {
            byte first = data[inPos];
            byte second = data[inPos + 1];
            byte third = data[inPos + 2];
            inPos += 3;

            int secondLow4 = second & 15;
            int firstLow2 = first & 3;
            byte sixthOfFirst = unsignedShiftRight(first, 2, 0xC0);
            byte fourthOfSecond = unsignedShiftRight(second, 4, 0xF0);
            int sixthOfThird = unsignedShiftRight(third, 6, 0xFC);

            out[outPos] = ENCODE_TABLE[sixthOfFirst];
            outPos++;
            out[outPos] = ENCODE_TABLE[fourthOfSecond | (firstLow2 << 4)];
            outPos++;
            out[outPos] = ENCODE_TABLE[((byte) sixthOfThird) | (secondLow4 << 2)];
            outPos++;
            out[outPos] = ENCODE_TABLE[third & 63];
            outPos++;
        }
        if (remainder == BITS_PER_BYTE) {
            // 只剩 1 个字节：出 2 个字符 + 2 个 '='
            byte last = data[inPos];
            int low2 = last & 3;
            out[outPos] = ENCODE_TABLE[unsignedShiftRight(last, 2, 0xC0)];
            outPos++;
            out[outPos] = ENCODE_TABLE[low2 << 4];
            outPos++;
            out[outPos] = PADDING;
            outPos++;
            out[outPos] = PADDING;
        } else if (remainder == BITS_REMAINDER_TWO_BYTES) {
            // 剩 2 个字节：出 3 个字符 + 1 个 '='
            byte last = data[inPos];
            byte secondLast = data[inPos + 1];
            int low4 = secondLast & 15;
            int low2 = last & 3;
            byte sixthOfFirst = unsignedShiftRight(last, 2, 0xC0);
            byte fourthOfSecond = unsignedShiftRight(secondLast, 4, 0xF0);
            out[outPos] = ENCODE_TABLE[sixthOfFirst];
            outPos++;
            out[outPos] = ENCODE_TABLE[fourthOfSecond | (low2 << 4)];
            outPos++;
            out[outPos] = ENCODE_TABLE[low4 << 2];
            outPos++;
            out[outPos] = PADDING;
        }
        return new String(out);
    }

    /**
     * 解码：Base64 字符串 -> 字节数组。
     *
     * <p>规则与原厂一致：先剔除空白；长度不是 4 的倍数直接返回 null；
     * 出现了非法字符返回 null；补齐位的冗余比特必须为 0，否则也返回 null。</p>
     *
     * @param data Base64 字符串；为 null 时返回 null
     */
    public static byte[] decode(String data) {
        if (data == null) {
            return null;
        }
        char[] chars = data.toCharArray();
        int length = compactWhitespace(chars);
        if (length % 4 != 0) {
            return null;
        }
        int groupCount = length / 4;
        if (groupCount == 0) {
            return new byte[0];
        }
        byte[] out = new byte[groupCount * 3];
        int inPos = 0;
        int outPos = 0;
        // 先解前面的完整组，最后一组要单独处理补齐
        int fullGroups = groupCount - 1;
        for (int i = 0; i < fullGroups; i++) {
            char c0 = chars[inPos];
            if (!isValidChar(c0)) {
                return null;
            }
            char c1 = chars[inPos + 1];
            if (!isValidChar(c1)) {
                return null;
            }
            char c2 = chars[inPos + 2];
            if (!isValidChar(c2)) {
                return null;
            }
            char c3 = chars[inPos + 3];
            if (!isValidChar(c3)) {
                return null;
            }
            inPos += 4;

            byte v0 = DECODE_TABLE[c0];
            byte v1 = DECODE_TABLE[c1];
            byte v2 = DECODE_TABLE[c2];
            byte v3 = DECODE_TABLE[c3];
            out[outPos] = (byte) ((v0 << 2) | (v1 >> 4));
            outPos++;
            out[outPos] = (byte) (((v1 & 15) << 4) | ((v2 >> 2) & 15));
            outPos++;
            out[outPos] = (byte) ((v2 << 6) | v3);
            outPos++;
        }

        // 最后一组的前两个字符必须是有效字符
        char c0 = chars[inPos];
        if (!isValidChar(c0)) {
            return null;
        }
        char c1 = chars[inPos + 1];
        if (!isValidChar(c1)) {
            return null;
        }
        byte v0 = DECODE_TABLE[c0];
        byte v1 = DECODE_TABLE[c1];
        char c2 = chars[inPos + 2];
        char c3 = chars[inPos + 3];

        if (isValidChar(c2) && isValidChar(c3)) {
            // 完整的一组：出 3 个字节
            byte v2 = DECODE_TABLE[c2];
            byte v3 = DECODE_TABLE[c3];
            out[outPos] = (byte) ((v0 << 2) | (v1 >> 4));
            outPos++;
            out[outPos] = (byte) (((v1 & 15) << 4) | ((v2 >> 2) & 15));
            outPos++;
            out[outPos] = (byte) ((v2 << 6) | v3);
            return out;
        }
        if (isPadding(c2) && isPadding(c3)) {
            // 两个补齐符：只出 1 个字节，且第二个字符的低 4 位必须为 0
            if ((v1 & 15) != 0) {
                return null;
            }
            byte[] result = new byte[fullGroups * 3 + 1];
            System.arraycopy(out, 0, result, 0, fullGroups * 3);
            result[outPos] = (byte) ((v0 << 2) | (v1 >> 4));
            return result;
        }
        if (isPadding(c2) || !isPadding(c3)) {
            // c2 是 '=' 而 c3 不是、或者两个都不是 '='（非法字符）——都不合法
            return null;
        }
        // 只有一个补齐符：出 2 个字节，且第三个字符的低 2 位必须为 0
        byte v2 = DECODE_TABLE[c2];
        if ((v2 & 3) != 0) {
            return null;
        }
        byte[] result = new byte[fullGroups * 3 + 2];
        System.arraycopy(out, 0, result, 0, fullGroups * 3);
        result[outPos] = (byte) ((v0 << 2) | (v1 >> 4));
        result[outPos + 1] = (byte) (((v1 & 15) << 4) | ((v2 >> 2) & 15));
        return result;
    }
}
