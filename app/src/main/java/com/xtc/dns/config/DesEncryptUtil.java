package com.xtc.dns.config;

import com.qiniu.android.common.Constants;
import com.xtc.dns.DnsLogTag;
import com.xtc.log.LogUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 腾讯 HttpDns 请求参数的 DES 加解密（原厂 {@code com.xtc.dns.config.a}）。
 *
 * <p>腾讯的接口约定：域名要 DES 加密后做十六进制大写编码拼进 url，
 * 响应体同样是密文，需要再解一次。密钥/算法/编码方式都写死在协议里，不能动。</p>
 *
 * <p>注意本类用的是"裸 DES/ECB/PKCS5"，安全性只服务于接口约定，
 * 不要用工程里更"现代"的 {@code com.xtc.utils.encode.DesUtils} 替换——
 * 那是另一套密钥与分组模式，密文对不上。</p>
 */
public class DesEncryptUtil {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("DesEncryptUtil");

    /** 与腾讯约定的 DES 密钥。 */
    private static final String DES_KEY = "<Yb0AzXu";

    /** 算法名。 */
    private static final String DES_ALGORITHM = "DES";

    /** 变换名：ECB 模式 + PKCS5 填充。 */
    private static final String DES_TRANSFORMATION = "DES/ECB/PKCS5Padding";

    /** 十六进制字符表（小写，解码时用）。 */
    private static final String HEX_CHARS = "0123456789abcdef";

    /** 十六进制字符转半字节；非法字符会得到 -1，与原厂一致地不做校验。 */
    private static byte hexCharToByte(char c) {
        return (byte) HEX_CHARS.indexOf(c);
    }

    /**
     * 加密域名。
     *
     * @return 大写十六进制密文；失败返回 null（调用方会拼出坏 url，由服务端拒绝）
     */
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec key = new SecretKeySpec(DES_KEY.getBytes(Constants.UTF_8), DES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(DES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return toHexString(cipher.doFinal(plainText.getBytes(Constants.UTF_8)));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /**
     * 字节数组转大写十六进制串。每个字节不足两位时补一个 {@code 0}。
     *
     * @param bytes 待转换的字节；为 null 时返回空串
     */
    public static final String toHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer(bytes.length);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 255);
            if (hex.length() < 2) {
                buffer.append(0);
            }
            buffer.append(hex.toUpperCase());
        }
        return buffer.toString();
    }

    /**
     * 解密响应体。
     *
     * <p>注意这里用 {@code new String(byte[])}（平台默认字符集）而不是显式 UTF-8，
     * 原厂如此，必须保持——换字符集在某些机器上会解出乱码 IP。</p>
     *
     * @return 明文；失败返回 null
     */
    public static String decrypt(String cipherText) {
        try {
            SecretKeySpec key = new SecretKeySpec(DES_KEY.getBytes(Constants.UTF_8), DES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(DES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(hexStringToBytes(cipherText)));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /** 十六进制串转字节数组；长度按 {@code 串长/2} 向下取整。 */
    public static byte[] hexStringToBytes(String hexText) {
        int length = hexText.length() / 2;
        byte[] result = new byte[length];
        char[] chars = hexText.toCharArray();
        for (int i = 0; i < length; i++) {
            int offset = i * 2;
            result[i] = (byte) (hexCharToByte(chars[offset + 1]) | (hexCharToByte(chars[offset]) << 4));
        }
        return result;
    }
}
