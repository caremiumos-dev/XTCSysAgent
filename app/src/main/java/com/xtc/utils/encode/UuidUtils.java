package com.xtc.utils.encode;

import java.util.UUID;

/**
 * UUID 生成工具。
 *
 * <p>原厂类名被混淆成 {@code k}。生成的串去掉了 4 个连字符，长度 32，
 * 被大量用作上传/请求的 uuid 参数（BaseInterceptor、QiNiuManager 等），
 * 格式不能变——服务端按 32 位十六进制校验。</p>
 */
public class UuidUtils {

    /**
     * 生成 32 位、无连字符的随机 UUID。
     */
    public static String uuid() {
        String value = UUID.randomUUID().toString();
        // 36 位标准 UUID 的连字符位置固定在 8/13/18/23，逐段拼接即可去掉它们
        return value.substring(0, 8) + value.substring(9, 13) + value.substring(14, 18)
                + value.substring(19, 23) + value.substring(24);
    }
}
