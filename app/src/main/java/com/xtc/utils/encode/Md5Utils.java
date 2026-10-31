package com.xtc.utils.encode;

import com.qiniu.android.common.Constants;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * MD5 摘要工具。
 *
 * <p>原厂类名被混淆成 {@code g}。这里的 MD5 被当成请求签名用（见 HttpHelper
 * 的 generateSignWithBodyBytes），输出是大写十六进制、不带分隔符，
 * 与 {@code java.security.MessageDigest} 的默认小写输出不同，改动会导致服务端验签失败。</p>
 */
public class Md5Utils {

    /**
     * 对字符串按 UTF-8 取摘要。
     *
     * <p>注意原厂用的是 qiniu 的 {@link Constants#UTF_8}（值 "utf-8"），
     * 与标准的 "UTF-8" 只是大小写不同，编码结果一致，但保持原样以便对拍。</p>
     */
    public static final String md5(String text) {
        return md5(text.getBytes(Charset.forName(Constants.UTF_8)));
    }

    /**
     * 对字节数组取 MD5，返回 32 位大写十六进制串；出错时返回 null。
     */
    public static final String md5(byte[] data) {
        // 十六进制字符表是原厂写死在方法里的局部数组，这里保持同一位置，
        // 避免引入新的静态字段影响类初始化顺序。
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data);
            byte[] digest = messageDigest.digest();
            char[] hexChars = new char[digest.length * 2];
            int pos = 0;
            for (byte value : digest) {
                // 高 4 位：先无符号右移再取低 4 位；低 4 位直接掩码
                hexChars[pos++] = hexDigits[(value >>> 4) & 15];
                hexChars[pos++] = hexDigits[value & 15];
            }
            return new String(hexChars);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
