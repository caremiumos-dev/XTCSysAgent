package com.xtc.utils.encode;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * ZIP 压缩/解压工具。
 *
 * <p>原厂类名被混淆成 {@code l}。采集日志时把一堆文件打成 zip 再上传（LogCollectServiceImpl、
 * WifiLogServeImpl 等），解包时按文件名关键字过滤（注意不是密码），这些行为细节必须保留：</p>
 * <ul>
 *   <li>压缩目录时，空目录也会被写进一个以 '/' 结尾的条目；</li>
 *   <li>可以给每个条目写 comment，解包侧靠 {@link #getFilesComment(File)} 读回来；</li>
 *   <li>解包遇到创建失败会返回 null（而不是抛异常），调用方据此判断失败；</li>
 *   <li>解包时跳过不匹配的条目，但匹配到的目录也会返回在结果列表里。</li>
 * </ul>
 *
 * <p>所有方法的异常声明都是 IOException（与原厂签名一致），调用方不必 catch Throwable。</p>
 */
public class ZipUtils {

    private ZipUtils() {
        // 纯工具类，原厂用一句带感叹号的文案阻止实例化，保持原样
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 列出 zip 里所有条目的名字。
     */
    public static List<String> getFilesPath(File zipFile) throws IOException {
        if (zipFile == null) {
            return null;
        }
        List<String> paths = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = getEntries(zipFile);
        while (entries.hasMoreElements()) {
            paths.add(entries.nextElement().getName());
        }
        return paths;
    }

    /**
     * 列出 zip 里所有条目的名字（路径串版本）。
     */
    public static List<String> getFilesPath(String zipFilePath) throws IOException {
        return getFilesPath(FileUtils.getFileByPath(zipFilePath));
    }

    /**
     * 列出 zip 里所有条目的注释。
     */
    public static List<String> getFilesComment(File zipFile) throws IOException {
        if (zipFile == null) {
            return null;
        }
        List<String> comments = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = getEntries(zipFile);
        while (entries.hasMoreElements()) {
            comments.add(entries.nextElement().getComment());
        }
        return comments;
    }

    /**
     * 列出 zip 里所有条目的注释（路径串版本）。
     */
    public static List<String> getFilesComment(String zipFilePath) throws IOException {
        return getFilesComment(FileUtils.getFileByPath(zipFilePath));
    }

    /**
     * 压缩单个文件/目录。
     */
    public static boolean zipFile(File srcFile, File zipFile) throws IOException {
        return zipFile(srcFile, zipFile, null);
    }

    /**
     * 压缩单个文件/目录，并给所有条目带上 comment。
     *
     * @return 压缩是否成功；入参有空时返回 false
     */
    public static boolean zipFile(File srcFile, File zipFile, String comment) throws IOException {
        if (srcFile == null || zipFile == null) {
            return false;
        }
        // 流在这里声明成 null，是为了让 finally 在构造失败时也能安全关闭
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            return zipFile(srcFile, "", zos, comment);
        } finally {
            if (zos != null) {
                FileUtils.closeIO(zos);
            }
        }
    }

    /**
     * 压缩单个文件/目录（路径串版本）。
     */
    public static boolean zipFile(String srcFilePath, String zipFilePath) throws IOException {
        return zipFile(srcFilePath, zipFilePath, null);
    }

    /**
     * 压缩单个文件/目录（路径串版本，带 comment）。
     */
    public static boolean zipFile(String srcFilePath, String zipFilePath, String comment) throws IOException {
        return zipFile(FileUtils.getFileByPath(srcFilePath), FileUtils.getFileByPath(zipFilePath), comment);
    }

    /**
     * 压缩多个文件。
     */
    public static boolean zipFiles(Collection<File> srcFiles, File zipFile) throws IOException {
        return zipFiles(srcFiles, zipFile, null);
    }

    /**
     * 压缩多个文件，并给所有条目带上 comment。
     *
     * <p>注意 finally 里先 {@code finish()} 再关闭：finish 会把中央目录写完整，
     * 少了它生成的 zip 在某些解压工具里打不开。</p>
     */
    public static boolean zipFiles(Collection<File> srcFiles, File zipFile, String comment) throws IOException {
        if (srcFiles == null || zipFile == null) {
            return false;
        }
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            Iterator<File> iterator = srcFiles.iterator();
            while (iterator.hasNext()) {
                if (!zipFile(iterator.next(), "", zos, comment)) {
                    return false;
                }
            }
            return true;
        } finally {
            if (zos != null) {
                zos.finish();
                FileUtils.closeIO(zos);
            }
        }
    }

    /**
     * 压缩多个文件（路径串版本的输出文件）。
     */
    public static boolean zipFiles(Collection<File> srcFiles, String zipFilePath) throws IOException {
        return zipFiles(srcFiles, zipFilePath, null);
    }

    /**
     * 压缩多个文件（路径串版本，带 comment）。
     */
    public static boolean zipFiles(Collection<File> srcFiles, String zipFilePath, String comment) throws IOException {
        return zipFiles(srcFiles, FileUtils.getFileByPath(zipFilePath), comment);
    }

    /**
     * 递归地把一个文件/目录写进 ZipOutputStream。
     *
     * <p>rootPath 是当前条目名前缀：根目录传空串，子层用 "父前缀 + 分隔符 + 父目录名"。
     * 源文件不存在时直接算成功（原厂行为，方便压缩 "可能不存在的日志文件"）。</p>
     *
     * @return 是否全部写入成功
     */
    private static boolean zipFile(File srcFile, String rootPath, ZipOutputStream zos, String comment) throws IOException {
        if (!srcFile.exists()) {
            return true;
        }
        String entryName = rootPath
                + (TextUtils.isEmpty(rootPath.trim()) ? "" : File.separator)
                + srcFile.getName();
        if (srcFile.isDirectory()) {
            File[] children = srcFile.listFiles();
            if (children == null || children.length <= 0) {
                // 空目录也要落一个条目，否则解包后目录结构会丢
                ZipEntry entry = new ZipEntry(entryName + '/');
                if (!TextUtils.isEmpty(comment)) {
                    entry.setComment(comment);
                }
                zos.putNextEntry(entry);
                zos.closeEntry();
            } else {
                for (File child : children) {
                    if (!zipFile(child, entryName, zos, comment)) {
                        return false;
                    }
                }
            }
        } else {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(srcFile));
                ZipEntry entry = new ZipEntry(entryName);
                if (!TextUtils.isEmpty(comment)) {
                    entry.setComment(comment);
                }
                zos.putNextEntry(entry);
                byte[] buffer = new byte[1024];
                while (true) {
                    int length = bis.read(buffer, 0, 1024);
                    if (length == -1) {
                        break;
                    }
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            } finally {
                FileUtils.closeIO(bis);
            }
        }
        return true;
    }

    /**
     * 解压。
     */
    public static boolean unzipFile(File zipFile, File destDir) throws IOException {
        return unzipFile(zipFile, destDir, null) != null;
    }

    /**
     * 解压。
     */
    public static boolean unzipFile(String zipFilePath, String destDirPath) throws IOException {
        return unzipFile(FileUtils.getFileByPath(zipFilePath), FileUtils.getFileByPath(destDirPath));
    }

    /**
     * 解压，只解出文件名包含 filterName 的条目（大小写不敏感）。
     *
     * @param filterName 文件名关键字；为空表示全部解出
     * @return 解出来的文件/目录列表；入参为空或中途创建失败返回 null
     */
    public static List<File> unzipFile(File zipFile, File destDir, String filterName) throws IOException {
        if (zipFile == null || destDir == null) {
            return null;
        }
        List<File> files = new ArrayList<>();
        ZipFile zip = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            // 过滤时只看文件名（不含目录），并且两个方向都转小写再比较
            if (TextUtils.isEmpty(filterName)
                    || FileUtils.getFileName(name).toLowerCase().contains(filterName.toLowerCase())) {
                File destFile = new File(destDir + File.separator + name);
                files.add(destFile);
                if (entry.isDirectory()) {
                    if (!FileUtils.createOrExistsDir(destFile)) {
                        return null;
                    }
                } else {
                    if (!FileUtils.createOrExistsFile(destFile)) {
                        return null;
                    }
                    BufferedInputStream bis = null;
                    BufferedOutputStream bos = null;
                    try {
                        bis = new BufferedInputStream(zip.getInputStream(entry));
                        bos = new BufferedOutputStream(new FileOutputStream(destFile));
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, length);
                        }
                    } finally {
                        FileUtils.closeIO(bis, bos);
                    }
                }
            }
        }
        return files;
    }

    /**
     * 解压（路径串版本）。
     */
    public static List<File> unzipFile(String zipFilePath, String destDirPath, String filterName) throws IOException {
        return unzipFile(FileUtils.getFileByPath(zipFilePath), FileUtils.getFileByPath(destDirPath), filterName);
    }

    /**
     * 批量解压到同一个目录，任一失败立即返回 false。
     */
    public static boolean unzipFiles(Collection<File> zipFiles, File destDir) throws IOException {
        if (zipFiles == null || destDir == null) {
            return false;
        }
        for (File zipFile : zipFiles) {
            if (!unzipFile(zipFile, destDir)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 批量解压（目标目录用路径串）。
     */
    public static boolean unzipFiles(Collection<File> zipFiles, String destDirPath) throws IOException {
        return unzipFiles(zipFiles, FileUtils.getFileByPath(destDirPath));
    }

    /**
     * 打开 zip 并返回条目枚举。
     *
     * <p>原厂直接返回 {@code new ZipFile(file).entries()}，ZipFile 句柄没有关闭
     * （靠 GC 回收），这里保持同样的写法。</p>
     */
    public static Enumeration<? extends ZipEntry> getEntries(File zipFile) throws IOException {
        if (zipFile == null) {
            return null;
        }
        return new ZipFile(zipFile).entries();
    }

    /**
     * 打开 zip 并返回条目枚举（路径串版本）。
     */
    public static Enumeration<? extends ZipEntry> getEntries(String zipFilePath) throws IOException {
        return getEntries(FileUtils.getFileByPath(zipFilePath));
    }
}
