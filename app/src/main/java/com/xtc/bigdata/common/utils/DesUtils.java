package com.xtc.bigdata.common.utils;

import android.text.TextUtils;

import com.xtc.log.LogUtil;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * DES 加解密。
 *
 * <p>密钥只取前 8 字节，不足补零 —— 这是 DES 的块长限制，也是原厂行为。
 * 密文用十六进制小写字符串表示，解密时按 2 字符一组还原。</p>
 *
 * <p>构造里如果 Cipher 初始化失败，只记日志不抛：这样即使某个 ROM 上没有
 * DES provider，调用方拿到的也只是加解密时的 NPE，而不是类加载失败。</p>
 */
public class DesUtils {

    private static final String TAG = "DesUtils";

    /** 默认密钥。 */
    private static final String strDefaultKey = "123456";

    /** 解密 Cipher，构造时初始化。 */
    private Cipher decryptCipher;

    /** 加密 Cipher，构造时初始化。 */
    private Cipher encryptCipher;

    /** 用默认密钥构造。 */
    public DesUtils() {
        this(strDefaultKey);
    }

    /**
     * 用指定密钥构造。
     *
     * @param key 密钥字符串
     */
    private DesUtils(String key) {
        this.encryptCipher = null;
        this.decryptCipher = null;
        try {
            Key secretKey = getKey(key.getBytes("UTF-8"));
            this.encryptCipher = Cipher.getInstance("DES");
            this.encryptCipher.init(1, secretKey);
            this.decryptCipher = Cipher.getInstance("DES");
            this.decryptCipher.init(2, secretKey);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
    }

    /**
     * 字节数组转十六进制字符串（小写、每字节补足两位）。
     *
     * @param data 字节数组
     * @return 十六进制串
     */
    private static String byteArr2HexStr(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (int value : data) {
            // byte 是有符号的，先归一到 0..255 再取十六进制
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
     * 解密一块数据。
     *
     * @param data 密文
     * @return 明文
     * @throws Exception Cipher 未初始化或密文非法
     */
    private byte[] decrypt(byte[] data) throws Exception {
        return this.decryptCipher.doFinal(data);
    }

    /**
     * 加密一块数据。
     *
     * @param data 明文
     * @return 密文
     * @throws Exception Cipher 未初始化
     */
    private byte[] encrypt(byte[] data) throws Exception {
        return this.encryptCipher.doFinal(data);
    }

    /**
     * 由任意长度密钥生成 8 字节 DES 密钥（不足补零，超长截断）。
     *
     * @param keyBytes 原始密钥
     * @return DES 密钥
     */
    private Key getKey(byte[] keyBytes) {
        byte[] keyData = new byte[8];
        for (int index = 0; index < keyBytes.length && index < keyData.length; index++) {
            keyData[index] = keyBytes[index];
        }
        return new SecretKeySpec(keyData, "DES");
    }

    /**
     * 十六进制字符串转字节数组。
     *
     * <p>任何一步失败都退化成"空数组"，让上层拿到空明文而不是异常 ——
     * 原厂在这里就是这样吞异常的。</p>
     *
     * @param hex 十六进制串
     * @return 字节数组；失败返回空数组
     * @throws UnsupportedEncodingException 理论上不会发生
     */
    private static byte[] hexStr2ByteArr(String hex) throws UnsupportedEncodingException {
        if (TextUtils.isEmpty(hex)) {
            return new byte[0];
        }
        byte[] bytes;
        try {
            bytes = hex.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtil.e(TAG, e);
            bytes = null;
        }
        if (bytes == null) {
            return new byte[0];
        }
        int length = bytes.length;
        byte[] result = new byte[length / 2];
        for (int index = 0; index < length; index += 2) {
            try {
                result[index / 2] = (byte) Integer.parseInt(new String(bytes, index, 2, "UTF-8"), 16);
            } catch (UnsupportedEncodingException e2) {
                LogUtil.e(TAG, e2);
            }
        }
        return result;
    }

    /**
     * 解密字符串。
     *
     * @param hex 十六进制密文
     * @return 明文
     * @throws Exception Cipher 未初始化或密文非法
     */
    public String decrypt(String hex) throws Exception {
        return new String(decrypt(hexStr2ByteArr(hex)), "UTF-8");
    }

    /**
     * 加密字符串。
     *
     * @param plain 明文
     * @return 十六进制密文
     * @throws Exception Cipher 未初始化
     */
    public String encrypt(String plain) throws Exception {
        return byteArr2HexStr(encrypt(plain.getBytes("UTF-8")));
    }
}
