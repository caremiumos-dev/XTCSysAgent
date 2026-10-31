package com.xtc.utils.encode;

import com.qiniu.android.common.Constants;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 加解密工具。
 *
 * <p>原厂类名被混淆成 {@code a}，是 HTTP 层（com.xtc.httplib.okhttp.HttpHelper）加密请求体
 * 和解密响应体时用的那一套：AES/ECB/PKCS5Padding，密文再走本包的 Base64。
 * 注意 key 是直接截取/补齐成 16 字节使用的，长度不是 16 时 SecretKeySpec 会抛异常，
 * 上层（HttpHelper.decodeHttpResponseBody）依赖这个异常走向 "解密失败返回原文" 的分支，
 * 所以不能替调用方补 key。</p>
 */
public class AesUtils {

    /** JCE 算法名。 */
    private static final String ALGORITHM = "AES";

    /**
     * 加密字符串：先按 {@link Constants#UTF_8} 取字节，再 AES 加密，最后 Base64。
     *
     * @param content 明文
     * @param key     密钥（原厂直接把 uuid 的前 16 位当 key 用）
     * @return Base64 密文；出错返回 null
     */
    public static String encrypt(String content, String key) {
        try {
            return Base64Utils.encode(encryptStringToBytes(content, key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 加密字节数组（不转换字符集），结果同样 Base64。
     *
     * @param data 待加密的原始字节（例如已经 gzip 过的请求体）
     * @param key  密钥
     * @return Base64 密文；出错返回 null
     */
    public static String encrypt(byte[] data, String key) {
        try {
            return Base64Utils.encode(encryptBytes(data, key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密 Base64 密文并还原成字符串。
     *
     * <p>还原时用的是平台默认字符集（原厂没有指定），这里保持不变。</p>
     *
     * @param content Base64 密文
     * @param key     密钥
     * @return 明文；出错返回 null
     */
    public static String decrypt(String content, String key) {
        try {
            return new String(decryptBytes(Base64Utils.decode(content), key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密 Base64 密文，直接返回明文字节（HttpHelper 解完还要再 gunzip，所以走这个入口）。
     *
     * @param content Base64 密文
     * @param key     密钥
     * @return 明文字节；出错返回 null
     */
    public static byte[] decryptToBytes(String content, String key) {
        try {
            return decryptBytes(Base64Utils.decode(content), key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 真正的 AES 加密：密钥按字面量 "UTF-8" 取字节，数据原样加。
     */
    private static byte[] encryptBytes(byte[] data, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    /**
     * 真正的 AES 解密：与 {@link #encryptBytes} 对称。
     */
    private static byte[] decryptBytes(byte[] data, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    /**
     * 字符串版本的加密：明文按 qiniu 的 {@link Constants#UTF_8}（"utf-8"）取字节。
     *
     * <p>和 {@link #encryptBytes} 只差字符集常量的写法，原厂就是两套并存的，
     * 合并会改变 "调用顺序/依赖的常量"，所以照原样分开。</p>
     */
    private static byte[] encryptStringToBytes(String content, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        byte[] plainBytes = content.getBytes(Constants.UTF_8);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plainBytes);
    }
}
