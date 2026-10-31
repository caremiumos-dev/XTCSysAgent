package com.xtc.utils.system;

import android.os.Environment;
import android.text.TextUtils;
import java.io.File;

/**
 * 本包内部使用的文件小工具。
 *
 * <p>原厂类名为单字母 {@code h}，且是<b>包内可见</b>（非 public）的辅助类，只服务于同包的
 * 应用信息（原 {@code a}）、清理（原 {@code d}）、设备信息（原 {@code f}）、
 * Intent 构造（原 {@code g}）四个类。这里保持原厂的"包内可见"约定，避免被其它包误用。</p>
 *
 * <p>注意：本类与原厂的 {@code com.xtc.utils.storage.FileUtils} 不是同一个类 ——
 * 那个是完整版文件库（IO、拷贝、大小、压缩等），本类只提供
 * "判断存在 / 取扩展名 / 递归删除 / 空串判断" 这几件事，与原件一一对应。</p>
 */
class FileUtils {

    FileUtils() {
    }

    /** 路径字符串转 {@link File}；空串返回 null（原件的 {@code a(String)}）。 */
    protected static File toFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return new File(filePath);
    }

    /** 外部存储是否已挂载。原件就是拿 {@code "mounted"} 直接比较，这里保持同一字符串。 */
    public static boolean isSDCardMounted() {
        return "mounted".equals(Environment.getExternalStorageState());
    }

    /** 文件是否存在；{@code null} 视为不存在（原件的 {@code a(File)}）。 */
    protected static boolean isFileExists(File file) {
        return file != null && file.exists();
    }

    /**
     * 取扩展名（不含点）。
     *
     * <p>没有点、或最后一个路径分隔符位于最后一个点之后（说明这个点属于目录名）
     * 时返回空串；入参为空串时原样返回。</p>
     */
    protected static String getFileExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastDotIndex = filePath.lastIndexOf(46);
        return (lastDotIndex == -1 || filePath.lastIndexOf(File.separator) >= lastDotIndex)
                ? "" : filePath.substring(lastDotIndex + 1);
    }

    /**
     * 删除目录下的全部内容，保留目录本身（原件的 {@code b(File)}）。
     *
     * <p>目录不存在直接返回 true；路径存在但不是目录返回 false；
     * 任意一个子项删不掉就立刻返回 false，不再继续。</p>
     */
    protected static boolean deleteAllInDir(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists()) {
            return true;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        if (files != null && files.length != 0) {
            for (File file : files) {
                if (file.isFile()) {
                    if (!deleteFile(file)) {
                        return false;
                    }
                } else if (file.isDirectory() && !deleteDir(file)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 删除单个文件；文件本来就不存在也算成功（原件的 {@code c(File)}）。 */
    protected static boolean deleteFile(File file) {
        return file != null && (!file.exists() || (file.isFile() && file.delete()));
    }

    /** 按路径删除目录及其内容（原件的 {@code c(String)}，内部走的是删目录而非删文件）。 */
    protected static boolean deleteDir(String dirPath) {
        return deleteDir(toFile(dirPath));
    }

    /**
     * 删除目录及其全部内容（原件的 {@code d(File)}）。
     *
     * <p>先递归清空子项，最后再删目录自身，因此返回值包含"目录本身是否删掉"。</p>
     */
    protected static boolean deleteDir(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists()) {
            return true;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        if (files != null && files.length != 0) {
            for (File file : files) {
                if (file.isFile()) {
                    if (!deleteFile(file)) {
                        return false;
                    }
                } else if (file.isDirectory() && !deleteDir(file)) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /** 按路径清空目录下的内容，保留目录本身（原件的 {@code d(String)}）。 */
    public static boolean deleteAllInDir(String dirPath) {
        return deleteAllInDir(toFile(dirPath));
    }

    /** 字符串是否为空（原件的 {@code e(String)}）。 */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
