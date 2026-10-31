package com.xtc.utils.encode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP 压缩/解压工具。
 *
 * <p>原厂类名被混淆成 {@code e}。HTTP 层在加密前先 gzip 请求体、解密后再 gunzip 响应体，
 * 就是走这里。两个方法都有 "出错也返回已经读到的内容" 的行为：流操作抛 IOException 时
 * 只打印堆栈，然后把 ByteArrayOutputStream 里已有的字节返回（可能是半截数据），
 * 上层据此走 "解压失败当原文" 的兜底逻辑，这点必须保持一致。</p>
 */
public class GzipUtils {

    /**
     * 把字符串按指定字符集编码后 gzip 压缩。
     *
     * <p>没有 throws：内部把所有 IOException（含 UnsupportedEncodingException）都吞掉了，
     * 原厂也是如此。</p>
     *
     * @param content     待压缩的字符串
     * @param charsetName 取字节用的字符集名，例如 "UTF-8"
     * @return 压缩后的字节；入参为 null 或空串时返回 null
     */
    public static byte[] compress(String content, String charsetName) {
        byte[] result = null;
        if (content != null && content.length() != 0) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipStream = null;
            try {
                gzipStream = new GZIPOutputStream(byteStream);
                gzipStream.write(content.getBytes(charsetName));
                gzipStream.close();
                result = byteStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                // 出错时仍然返回已经写进去的内容（原厂行为）
                result = byteStream.toByteArray();
            } finally {
                try {
                    byteStream.close();
                    if (gzipStream != null) {
                        gzipStream.close();
                    }
                } catch (IOException e) {
                    // 关闭阶段的异常被完全吞掉，原厂这里连日志都没有
                }
            }
        }
        return result;
    }

    /**
     * gzip 解压。
     *
     * @param data 压缩数据
     * @return 解压后的字节；入参为 null 或空数组时返回 null
     */
    public static byte[] decompress(byte[] data) {
        byte[] result = null;
        if (data != null && data.length != 0) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GZIPInputStream gzipStream = null;
            try {
                gzipStream = new GZIPInputStream(new ByteArrayInputStream(data));
                // 缓冲区长度 256 是原厂写死的，不影响结果，保留
                byte[] buffer = new byte[256];
                int length;
                while ((length = gzipStream.read(buffer)) >= 0) {
                    byteStream.write(buffer, 0, length);
                }
                gzipStream.close();
                result = byteStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                // 与压缩方向一致：失败时返回已经解出来的部分
                result = byteStream.toByteArray();
            } finally {
                try {
                    byteStream.close();
                    if (gzipStream != null) {
                        gzipStream.close();
                    }
                } catch (IOException e) {
                    // 关闭异常吞掉，保持原厂行为
                }
            }
        }
        return result;
    }
}
