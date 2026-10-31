package com.xtc.utils.encode;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * 文件与容量相关的零碎工具。
 *
 * <p>原厂类名被混淆成 {@code h}，而且类本身是包级可见的（只有本包的 ZipUtils 在用），
 * 所以这里不放大成 public，避免把内部工具暴露给全工程。</p>
 *
 * <p>方法语义有几个反直觉的地方，都是原厂就这样的：{@link #createOrExistsDir(File)}
 * 的返回值表示 "是否需要报错"（目录存在但不是目录时返回 true），
 * {@link #makeDirs(File)} 在路径已是文件时反而去调 mkdirs。</p>
 */
class FileUtils {

    /** 1 字节。 */
    public static final int B = 1;
    /** 1 KB。 */
    public static final int KB = 1024;
    /** 1 MB。 */
    public static final int MB = 1048576;
    /** 1 GB。 */
    public static final int GB = 1073741824;

    FileUtils() {
    }

    /**
     * 路径串转 File；空串返回 null。
     */
    public static File getFileByPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return new File(filePath);
    }

    /**
     * 批量关闭流，逐个判空；关闭过程中的 IOException 只打印堆栈。
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
     * 建目录：如果路径已存在且不是普通文件（即已经是目录）则直接成功，
     * 否则交给 {@link File#mkdirs()}（父目录会一起建）。
     */
    public static boolean makeDirs(@NonNull File file) {
        if (!file.isFile() && file.exists()) {
            return true;
        }
        return file.mkdirs();
    }

    /**
     * 取路径里的文件名（最后一段）。没有分隔符时原样返回。
     */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastSeparator = filePath.lastIndexOf(File.separator);
        if (lastSeparator == -1) {
            return filePath;
        }
        return filePath.substring(lastSeparator + 1);
    }

    /**
     * 确保目录存在。
     *
     * @return true 表示 "出错"（file 为 null、或者路径存在但不是目录、或者 mkdirs 失败）。
     *         这个反着的返回值是原厂语义，调用方按 false 表示正常来用。
     */
    public static boolean createOrExistsDir(File file) {
        return file != null && (!file.exists() ? !file.mkdirs() : !file.isDirectory());
    }

    /**
     * 确保文件存在：已存在就判断是不是普通文件，不存在就先把父目录建出来再创建。
     *
     * @return 创建/存在成功返回 true
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
     * 路径串版本的 {@link #createOrExistsFile(File)}。
     */
    public static boolean createOrExistsFile(String filePath) {
        return createOrExistsFile(getFileByPath(filePath));
    }
}
