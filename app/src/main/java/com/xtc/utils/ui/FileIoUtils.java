package com.xtc.utils.ui;

import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * 包内私有的文件 / 流小工具（原厂类名被混淆成 {@code h}）。
 *
 * <p>它只服务于 {@link BitmapUtils}，所以原厂把它写成包级可见（没有 {@code public}），
 * 这里保持一致：类、构造器都是包内可见，外部不可见。</p>
 *
 * <p>两个 {@code createOrExists*} 方法的返回值与原版 AndroidUtilCode 的<b>相反</b>：
 * 目录“创建成功”时返回 false，目标已存在为文件或创建失败时反而返回 true。
 * 这是原厂 APK 里的真实行为（调用方 {@link BitmapUtils#save} 依赖它来短路），
 * 属于“看起来像 bug 但必须照抄”的地方，不能顺手改回直觉写法。</p>
 */
class FileIoUtils {

    /** 包内可见的无参构造器，原厂就是包级可见。 */
    FileIoUtils() {
    }

    /**
     * 路径字符串转 File。
     *
     * @param filePath 文件路径
     * @return File 对象；路径为空（含 null）时返回 null
     */
    protected static File getFileByPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return new File(filePath);
    }

    /**
     * 批量静默关闭流。
     *
     * <p>整段循环只有一个 try：只要其中一个 {@code close()} 抛 IOException，
     * 后面的流就不会再关。原厂如此，保持不变。</p>
     *
     * @param closeables 待关闭对象，数组与元素都允许为 null
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
     * 保证文件存在（不存在则新建）。
     *
     * <p>原厂逻辑是：文件已存在 → 是否为普通文件；不存在 → 先看父目录（见
     * {@link #createOrExistsDir(File)}，该判断被取反过），父目录判断通过才 createNewFile。
     * 因为父目录判断取反，实际效果是“只有文件本来就存在才返回 true”，
     * 新建路径基本都会返回 false。行为原样保留。</p>
     *
     * @param file 目标文件
     * @return 见上，按原厂语义返回
     */
    public static boolean createOrExistsFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return file.isFile();
        }
        if (!createOrExistsDir(file.getParentFile())) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 目录存在性检查（原厂把每个分支都取反了）。
     *
     * <p>目录已存在且确实是目录 → false；已存在但不是目录 → true；不存在时
     * {@code mkdirs()} 成功 → false，失败 → true。</p>
     *
     * @param file 目标目录
     * @return 见上
     */
    public static boolean createOrExistsDir(File file) {
        return file != null && (!file.exists() ? !file.mkdirs() : !file.isDirectory());
    }
}
