package com.xtc.utils.encode;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * DES 加解密工具（DES/ECB/PKCS5Padding + 十六进制串）。
 *
 * <p>原厂类名被混淆成 {@code d}。这个类的 Cipher 在构造时就初始化好并缓存下来，
 * 属于 "一个实例一把钥匙" 的用法；因为 Cipher 不是线程安全的，原厂也没有加锁，
 * 这里保持同样的实例复用方式（调用方若要并发用需要自己 new 一个）。</p>
 */
public class DesUtils {

    /** 默认密钥，长度不足 8 字节时会被补 0。 */
    private static final String DEFAULT_KEY = "123456";

    /** 加密用的 Cipher，构造时初始化；初始化失败时为 null。 */
    private Cipher encryptCipher;
    /** 解密用的 Cipher，构造时初始化；初始化失败时为 null。 */
    private Cipher decryptCipher;

    public DesUtils() {
        this(DEFAULT_KEY);
    }

    private DesUtils(String key) {
        this.encryptCipher = null;
        this.decryptCipher = null;
        try {
            Key desKey = buildKey(key.getBytes("UTF-8"));
            this.encryptCipher = Cipher.getInstance("DES");
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, desKey);
            this.decryptCipher = Cipher.getInstance("DES");
            this.decryptCipher.init(Cipher.DECRYPT_MODE, desKey);
        } catch (Exception e) {
            // 初始化失败就把两个 Cipher 留成 null，后续加解密会抛 NPE——
            // 原厂就是这样把异常吞在这里的，不能改成抛出去
            e.printStackTrace();
        }
    }

    /**
     * 把字节数组转成小写十六进制串（不足两位的补一个 '0'）。
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (int value : bytes) {
            // 原厂用 while 累加把负 byte 抬到 0~255，等价于 & 0xFF，这里保留同样的循环
            while (value < 0) {
                value += 256;
            }
            if (value < 16) {
                builder.append("0");
            }
            builder.append(Integer.toString(value, 16));
        }
        return builder.toString();
    }

    /**
     * 十六进制串还原成字节数组。空串返回空数组；转不成字符集时也返回空数组。
     *
     * <p>注意这里只捕获 UnsupportedEncodingException，十六进制里出现非法字符时
     * Integer.parseInt 抛的 NumberFormatException 是往上抛的，原厂如此。</p>
     */
    private static byte[] hexStringToBytes(String hex) throws UnsupportedEncodingException {
        byte[] rawBytes;
        if (TextUtils.isEmpty(hex)) {
            return new byte[0];
        }
        try {
            rawBytes = hex.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            rawBytes = null;
        }
        if (rawBytes == null) {
            return new byte[0];
        }
        int length = rawBytes.length;
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            try {
                result[i / 2] = (byte) Integer.parseInt(new String(rawBytes, i, 2, "UTF-8"), 16);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 用缓存的加密 Cipher 加密。
     */
    private byte[] doEncrypt(byte[] data) throws Exception {
        return this.encryptCipher.doFinal(data);
    }

    /**
     * 用缓存的解密 Cipher 解密。
     */
    private byte[] doDecrypt(byte[] data) throws Exception {
        return this.decryptCipher.doFinal(data);
    }

    /**
     * 由任意字节数组构造 DES 密钥：只取前 8 字节，不足的补 0。
     */
    private Key buildKey(byte[] keyBytes) {
        byte[] key8 = new byte[8];
        for (int i = 0; i < keyBytes.length && i < key8.length; i++) {
            key8[i] = keyBytes[i];
        }
        return new SecretKeySpec(key8, "DES");
    }

    /**
     * 加密字符串，返回十六进制密文。
     */
    public String encrypt(String content) throws Exception {
        return toHexString(doEncrypt(content.getBytes("UTF-8")));
    }

    /**
     * 解密十六进制密文，返回 UTF-8 明文。
     */
    public String decrypt(String content) throws Exception {
        return new String(doDecrypt(hexStringToBytes(content)), "UTF-8");
    }
}
