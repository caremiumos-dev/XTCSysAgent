package com.xtc.utils.storage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.media.session.PlaybackStateCompat;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 存储包内部的 IO 小工具（包级私有，不对外暴露）。
 *
 * <p>原厂把三类杂活放在了一起：创建其他包名的 Context、把字节数格式化成可读字符串、
 * 以及流/关闭动作的封装。之所以是包级私有，是因为它只服务于
 * {@link FileUtils} 与 {@link SdCardUtils}，属于实现细节。</p>
 */
class IOUtils {

    IOUtils() {
    }

    /**
     * 创建指定包名的 Context（用于跨进程/跨应用读对方的数据库）。
     *
     * @param flags 原厂固定传 2，即 {@link Context#CONTEXT_IGNORE_SECURITY}：
     *              忽略安全校验，允许本进程访问该包的私有资源
     * @throws PackageManager.NameNotFoundException 目标包未安装
     */
    public static Context createPackageContext(Context context, String packageName) throws PackageManager.NameNotFoundException {
        return context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
    }

    /**
     * 把字节数格式化成 "%.3f + 单位" 的字符串。
     *
     * <p>单位进到 B/KB/MB/GB 四档，阈值分别是 1024、1024*1024、1024^3；
     * 复数个 0 的细节（3 位小数、用默认 Locale）都与原厂一致，
     * 因为这类字符串会被写进日志被日志解析脚本消费。</p>
     *
     * @param byteCount 字节数，负数直接返回提示语（原厂行为，不做绝对值处理）
     */
    public static String formatSize(long byteCount) {
        if (byteCount < 0) {
            return "shouldn't be less than zero!";
        }
        if (byteCount < PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            return String.format(Locale.getDefault(), "%.3fB", (double) byteCount);
        }
        if (byteCount < PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
            return String.format(Locale.getDefault(), "%.3fKB", byteCount / 1024.0d);
        }
        return byteCount < 1073741824
                ? String.format(Locale.getDefault(), "%.3fMB", byteCount / 1048576.0d)
                : String.format(Locale.getDefault(), "%.3fGB", byteCount / 1.073741824E9d);
    }

    /**
     * 依次关闭一批流，忽略 null 元素。
     *
     * <p>注意原厂的"异常即中止"语义：任何一个 close 抛 IOException，
     * 后面的元素就不再关闭，只打印一次堆栈。调用方依赖这个行为的地方（比如
     * 先关文件流再关包装流）尚未出现问题，重写时保持原样。</p>
     */
    public static void closeIO(Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        try {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 把输入流整体读成 byte[]。
     *
     * <p>入参为 null 返回 null；其余情况交给 {@link #readBytes(InputStream)}。
     * 后者在读取失败时会返回 null，这里直接对它调用 toByteArray()，
     * 也就是说"读取失败会抛 NPE"——原厂就是这么写的，保留。</p>
     */
    public static byte[] toByteArray(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        return readBytes(inputStream).toByteArray();
    }

    /**
     * 把输入流读进内存缓冲，读完（或出错）都会关闭入参流。
     *
     * @return 读取成功返回缓冲；入参为 null 或读取出错返回 null
     */
    public static ByteArrayOutputStream readBytes(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        ByteArrayOutputStream output = null;
        try {
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true) {
                int read = inputStream.read(buffer, 0, 1024);
                if (read == -1) {
                    return output;
                }
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 原厂三个出口（成功/异常/异常抛出）都关一次入参流。
            closeIO(inputStream);
        }
    }
}
