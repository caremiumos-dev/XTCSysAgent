package com.xtc.bigdata.collector.utils;

import android.util.Log;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 摘要转指定进制的字符串。
 *
 * <p>两个固定用法：{@code new MD5Coder(16)} 打应用 id（16 进制），
 * {@code new MD5Coder(32)} 打点击热区路径（32 进制）——
 * 进制不同得到的串长不同，服务端按长度区分用途。</p>
 */
public class MD5Coder {

    private static final String KEY_MD5 = "MD5";

    private static final String TAG = "MD5Coder";

    /** 输出进制。 */
    private int radix;

    /** 默认 36 进制。 */
    public MD5Coder() {
        this.radix = 36;
    }

    /**
     * 指定输出进制。
     *
     * @param radix 进制
     */
    public MD5Coder(int radix) {
        this.radix = 36;
        this.radix = radix;
    }

    /**
     * 算 MD5 摘要。
     *
     * @param data 原始数据
     * @return 16 字节摘要；算法不存在时返回 null
     */
    private byte[] getMD5(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(KEY_MD5);
            messageDigest.update(data);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e + "");
            return null;
        }
    }

    /**
     * 把摘要转成指定进制的字符串（取绝对值，避免负数带 '-'）。
     *
     * @param data 原始数据
     * @return 摘要串
     * @throws Exception 摘要算法不可用时会 NPE，这里按原厂签名声明异常
     */
    public String encode(byte[] data) throws Exception {
        return new BigInteger(getMD5(data)).abs().toString(this.radix);
    }
}
