package com.xtc.httplib.util;

import com.xtc.httplib.LogTag;
import com.xtc.log.LogUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 固定密钥的 DES 工具（DES/ECB/PKCS5Padding，密文用十六进制串表示）。
 *
 * <p>密钥是写死的字符串 {@code "<Yb0AzXu"}，属于"仅供服务端弱校验"的用途，
 * 不能换成随机密钥。它和 HTTP 层用的是两套完全不同的算法（那边是 AES + RSA 协商），
 * 不要互相替代。</p>
 *
 * <p>十六进制表示是**大写**且不足两位补 0（{@link #bytesToHexString}），
 * 而 {@link #hexStringToByte} 的解析表是小写的 {@code "0123456789abcdef"}——
 * 也就是说加密产物是给服务端用的，回解时只接受小写输入。这对不对称行为是原厂就有的，
 * 改动会让历史密文解不开。</p>
 */
public class DesEncryptUtil {

    private static final String TAG = LogTag.tag("DesEncryptUtil");

    /** 固定密钥，DES 只用前 8 字节，长度刚好 8。 */
    private static final String encKey = "<Yb0AzXu";

    /** 原厂引用的七牛 SDK 字符集常量，值即 "utf-8"。 */
    private static final String CHARSET_UTF8 = "utf-8";

    /**
     * 字节数组转大写十六进制串。
     *
     * @param data 原始字节，为 null 时返回空串
     */
    public static String bytesToHexString(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer(data.length);
        for (byte value : data) {
            // & 255 把负数抬到 0~255
            String hexString = Integer.toHexString(value & 255);
            if (hexString.length() < 2) {
                buffer.append(0);
            }
            buffer.append(hexString.toUpperCase());
        }
        return buffer.toString();
    }

    /**
     * 解密十六进制密文。
     *
     * <p>明文字节直接 {@code new String(...)}，用平台默认字符集——原厂没有指定 UTF-8，
     * 在手表 ROM 上默认就是 UTF-8，但这里不能"顺手修正"，否则会改变极端场景的行为。</p>
     *
     * @return 明文；任何异常都记日志并返回 {@code null}
     */
    public static String decrypt(String content) {
        try {
            SecretKeySpec key = new SecretKeySpec(encKey.getBytes(CHARSET_UTF8), "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(hexStringToByte(content)));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /**
     * 加密成大写十六进制密文。
     *
     * @return 密文；任何异常都记日志并返回 {@code null}
     */
    public static String encrypt(String content) {
        try {
            SecretKeySpec key = new SecretKeySpec(encKey.getBytes(CHARSET_UTF8), "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return bytesToHexString(cipher.doFinal(content.getBytes(CHARSET_UTF8)));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /**
     * 十六进制串还原成字节数组。
     *
     * <p>只按下表解析小写字母；遇到非法字符时 {@link #toByte(char)} 返回 -1，
     * 拼出来的字节是脏数据（原厂不做校验）。入参长度必须是偶数，
     * 奇数长度会丢掉最后一个字符。</p>
     */
    public static byte[] hexStringToByte(String hex) {
        int length = hex.length() / 2;
        byte[] result = new byte[length];
        char[] chars = hex.toCharArray();
        for (int index = 0; index < length; index++) {
            int charIndex = index * 2;
            // 高 4 位在前：先取 chars[2i] 左移 4 位，再或上 chars[2i+1]
            result[index] = (byte) (toByte(chars[charIndex + 1]) | (toByte(chars[charIndex]) << 4));
        }
        return result;
    }

    /** 单个十六进制字符转数值；不是合法字符时返回 -1（原厂如此，不做校验）。 */
    private static byte toByte(char value) {
        return (byte) "0123456789abcdef".indexOf(value);
    }
}
