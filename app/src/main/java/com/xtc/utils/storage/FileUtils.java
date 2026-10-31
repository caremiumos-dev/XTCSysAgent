package com.xtc.utils.storage;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;
import com.xtc.log.LogUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;

/**
 * 文件操作总入口。
 *
 * <p>原厂这个类承担了整个存储层的"文件系统原语"：存在性判断、目录/文件创建、
 * 递归删除、递归复制（含"复制后删源"的移动语义）、按后缀/正则/文件名筛选、
 * 文本与字节读写、字符集嗅探、行数统计、Uri 转真实路径、assets 读取，
 * 以及一个基于反射的 XML → JavaBean/List 反序列化。</p>
 *
 * <p>设计约定（理解本类行为的前提）：</p>
 * <ul>
 *   <li>"写入"类方法在文件不存在时会先建父目录再建文件，成功返回 true；</li>
 *   <li>所有 IO 异常都被就地 printStackTrace 吞掉、降级成 false/null，不向上抛
 *       （唯一例外是 {@link #writeFileFromBytes(File, byte[], boolean)}，
 *       它按原厂签名会抛 IOException）；</li>
 *   <li>流的关闭统一走 {@link IOUtils#closeIO(java.io.Closeable...)}，
 *       异常吞没位置与原厂保持一致；</li>
 *   <li>接收 {@code File} 与 {@code String} 的重载成对出现，语义完全相同，
 *       只是方便调用方直接传路径；</li>
 *   <li>本类是工具类，禁止实例化。</li>
 * </ul>
 */
public class FileUtils {

    /** 1 字节。 */
    public static final int BYTE = 1;

    /** 1KB（1024 字节）。 */
    public static final int KB = 1024;

    /** 1MB（1024 × 1024 字节）。 */
    public static final int MB = 1048576;

    /** 1GB（1024 × 1024 × 1024 字节）。 */
    public static final int GB = 1073741824;

    /** 日志 tag，原厂固定为 FileUtil（注意没有 s），保持原样便于线上日志对照。 */
    private static final String TAG = "FileUtil";

    private FileUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    // ------------------------------------------------------------------
    // 基础：路径 → File、存在性、类型判断
    // ------------------------------------------------------------------

    /** 路径转 File；路径为空返回 null（不抛异常）。 */
    public static File getFileByPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return new File(filePath);
    }

    /** 文件/目录是否存在。 */
    public static boolean isFileExists(File file) {
        return file != null && file.exists();
    }

    /** 路径是否存在。 */
    public static boolean isFileExists(String filePath) {
        return isFileExists(getFileByPath(filePath));
    }

    /** 是否是已存在的目录。 */
    public static boolean isDir(File dir) {
        return isFileExists(dir) && dir.isDirectory();
    }

    /** 路径是否是已存在的目录。 */
    public static boolean isDir(String dirPath) {
        return isDir(getFileByPath(dirPath));
    }

    /** 是否是已存在的文件。 */
    public static boolean isFile(File file) {
        return isFileExists(file) && file.isFile();
    }

    /** 路径是否是已存在的文件。 */
    public static boolean isFile(String filePath) {
        return isFile(getFileByPath(filePath));
    }

    /** 外部存储根路径；外部存储目录不存在时返回空串。 */
    public static String getExternalStoragePath() {
        return Environment.getExternalStorageDirectory().exists() ? Environment.getExternalStorageDirectory().getPath() : "";
    }

    // ------------------------------------------------------------------
    // 目录 / 文件创建
    // ------------------------------------------------------------------

    /**
     * 目录存在就返回"是否是目录"，不存在则尝试 mkdirs。
     *
     * <p>这是全类所有"写文件前先备好父目录"动作的基石；
     * 与原厂反编译结果相比，这里的判断方向被 JADX 弄反过一次，
     * 已按 smali 还原为"成功即 true"。</p>
     */
    public static boolean createOrExistsDir(File dir) {
        return dir != null && (dir.exists() ? dir.isDirectory() : dir.mkdirs());
    }

    /** 按路径创建目录（存在则判断类型）。 */
    public static boolean createOrExistsDir(String dirPath) {
        return createOrExistsDir(getFileByPath(dirPath));
    }

    /** 文件存在就返回"是否是文件"，不存在则先备好父目录再 createNewFile。 */
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

    /** 按路径创建文件（存在则判断类型）。 */
    public static boolean createOrExistsFile(String filePath) {
        return createOrExistsFile(getFileByPath(filePath));
    }

    /**
     * 删掉同名旧文件后重建一个空文件。
     *
     * <p>与 {@link #createOrExistsFile(File)} 的差别：不清旧文件的内容，
     * 这里保证拿到的一定是一个"新的空文件"。</p>
     */
    public static boolean createFileByDeleteOldFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.exists() && file.isFile() && !file.delete()) {
            return false;
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

    /** 按路径删旧建新。 */
    public static boolean createFileByDeleteOldFile(String filePath) {
        return createFileByDeleteOldFile(getFileByPath(filePath));
    }

    // ------------------------------------------------------------------
    // 删除
    // ------------------------------------------------------------------

    /** 删除单个文件；文件本来就不存在也算成功。 */
    public static boolean deleteFile(File file) {
        return file != null && (!file.exists() || (file.isFile() && file.delete()));
    }

    /** 按路径删除文件。 */
    public static boolean deleteFile(String filePath) {
        return deleteFile(getFileByPath(filePath));
    }

    /** 递归删除目录（连同目录自身）。 */
    public static boolean deleteDir(File dir) {
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

    /** 按路径递归删除目录。 */
    public static boolean deleteDir(String dirPath) {
        return deleteDir(getFileByPath(dirPath));
    }

    /**
     * 只清空目录内容，保留目录本身。
     *
     * <p>与 {@link #deleteDir(File)} 的差别就是最后不收尾那一下 dir.delete()；
     * 清理缓存场景必须用这个，否则会把 cache 目录本身删掉导致后续写入失败。</p>
     */
    public static boolean deleteAllInDir(File dir) {
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

    /** 按路径清空目录内容。 */
    public static boolean deleteAllInDir(String dirPath) {
        return deleteAllInDir(getFileByPath(dirPath));
    }
    // ------------------------------------------------------------------
    // 目录遍历与筛选
    // ------------------------------------------------------------------

    /**
     * 递归列出目录下所有文件/子目录（深度优先，先父后子）。
     *
     * @return 目录不存在或不是目录返回 null；空目录返回空集合
     */
    public static List<File> listFilesInDir(File dir) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return files;
        }
        for (File file : listFiles) {
            files.add(file);
            if (file.isDirectory()) {
                files.addAll(listFilesInDir(file));
            }
        }
        return files;
    }

    /** 按路径递归列出目录内容。 */
    public static List<File> listFilesInDir(String dirPath) {
        return listFilesInDir(getFileByPath(dirPath));
    }

    /**
     * 列出目录内容，{@code isRecursive} 决定是否递归。
     *
     * <p>注意非递归分支用的是 {@code Collections.addAll(files, dir.listFiles())}，
     * 目录不可读时 listFiles() 返回 null 会在这里抛 NPE——原厂如此，保留。</p>
     */
    public static List<File> listFilesInDir(File dir, boolean isRecursive) {
        if (isRecursive) {
            return listFilesInDir(dir);
        }
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        Collections.addAll(files, dir.listFiles());
        return files;
    }

    /** 按路径列出目录内容（可递归）。 */
    public static List<File> listFilesInDir(String dirPath, boolean isRecursive) {
        return listFilesInDir(getFileByPath(dirPath), isRecursive);
    }

    /**
     * 按后缀筛选（不区分大小写），递归。
     *
     * <p>实现上用的是 endsWith，所以传 ".mp4" 与传 "mp4" 都能匹配到同一批文件。</p>
     */
    public static List<File> listFilesBySuffixInDir(File dir, String suffix) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return files;
        }
        for (File file : listFiles) {
            if (file.getName().toUpperCase().endsWith(suffix.toUpperCase())) {
                files.add(file);
            }
            if (file.isDirectory()) {
                files.addAll(listFilesBySuffixInDir(file, suffix));
            }
        }
        return files;
    }

    /** 按后缀筛选，{@code isRecursive} 为 false 时只看当前层。 */
    public static List<File> listFilesBySuffixInDir(File dir, String suffix, boolean isRecursive) {
        if (isRecursive) {
            return listFilesBySuffixInDir(dir, suffix);
        }
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return files;
        }
        for (File file : listFiles) {
            if (file.getName().toUpperCase().endsWith(suffix.toUpperCase())) {
                files.add(file);
            }
        }
        return files;
    }

    /** 按路径按后缀筛选（递归）。 */
    public static List<File> listFilesBySuffixInDir(String dirPath, String suffix) {
        return listFilesBySuffixInDir(getFileByPath(dirPath), suffix);
    }

    /** 按路径按后缀筛选（可递归）。 */
    public static List<File> listFilesBySuffixInDir(String dirPath, String suffix, boolean isRecursive) {
        return listFilesBySuffixInDir(getFileByPath(dirPath), suffix, isRecursive);
    }

    /**
     * 用 {@link FilenameFilter} 筛选，递归。
     *
     * <p>回调里传的是 {@code file.getParentFile()} 而不是 dir，这样过滤器
     * 拿到的父目录永远是当前子文件真实的父目录（递归时逐层变化）。</p>
     */
    public static List<File> listFilesInDirWithFilter(File dir, FilenameFilter filter) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return files;
        }
        for (File file : listFiles) {
            if (filter.accept(file.getParentFile(), file.getName())) {
                files.add(file);
            }
            if (file.isDirectory()) {
                files.addAll(listFilesInDirWithFilter(file, filter));
            }
        }
        return files;
    }

    /** 用 FilenameFilter 筛选，{@code isRecursive} 为 false 时只看当前层。 */
    public static List<File> listFilesInDirWithFilter(File dir, FilenameFilter filter, boolean isRecursive) {
        if (isRecursive) {
            return listFilesInDirWithFilter(dir, filter);
        }
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return files;
        }
        for (File file : listFiles) {
            if (filter.accept(file.getParentFile(), file.getName())) {
                files.add(file);
            }
        }
        return files;
    }

    /** 按路径用 FilenameFilter 筛选（递归）。 */
    public static List<File> listFilesInDirWithFilter(String dirPath, FilenameFilter filter) {
        return listFilesInDirWithFilter(getFileByPath(dirPath), filter);
    }

    /** 按路径用 FilenameFilter 筛选（可递归）。 */
    public static List<File> listFilesInDirWithFilter(String dirPath, FilenameFilter filter, boolean isRecursive) {
        return listFilesInDirWithFilter(getFileByPath(dirPath), filter, isRecursive);
    }

    /**
     * 按"文件名完全相同"筛选（不区分大小写），递归。
     *
     * <p>与按后缀筛选的区别：这里比对的是整个文件名，不做前缀匹配。</p>
     */
    public static List<File> listFilesInDirWithName(File dir, String fileName) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return files;
        }
        for (File file : listFiles) {
            if (file.getName().toUpperCase().equals(fileName.toUpperCase())) {
                files.add(file);
            }
            if (file.isDirectory()) {
                files.addAll(listFilesInDirWithName(file, fileName));
            }
        }
        return files;
    }

    /** 按路径按文件名筛选（递归）。 */
    public static List<File> listFilesInDirWithName(String dirPath, String fileName) {
        return listFilesInDirWithName(getFileByPath(dirPath), fileName);
    }

    // ------------------------------------------------------------------
    // 写文件
    // ------------------------------------------------------------------

    /**
     * 把输入流写进文件。
     *
     * @param append 是否追加（false 时覆盖）
     * @return 写入失败（含目标文件无法创建）返回 false，不抛异常
     */
    public static boolean writeFileFromStream(File file, InputStream inputStream, boolean append) {
        if (file == null || inputStream == null || !createOrExistsFile(file)) {
            return false;
        }
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file, append));
            byte[] buffer = new byte[1024];
            while (true) {
                int read = inputStream.read(buffer, 0, 1024);
                if (read == -1) {
                    return true;
                }
                outputStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            // 原厂在成功/失败/异常三条路上都关一次入参与出参，位置保持一致
            IOUtils.closeIO(inputStream, outputStream);
        }
    }

    /** 按路径把输入流写进文件。 */
    public static boolean writeFileFromStream(String filePath, InputStream inputStream, boolean append) {
        return writeFileFromStream(getFileByPath(filePath), inputStream, append);
    }

    /**
     * 把字节数组写进文件。
     *
     * <p>这是本类唯一声明受检异常的写方法：原厂在 dex 里显式声明了 IOException，
     * 因为 FileOutputStream 的构造/写入异常没有被就地捕获。</p>
     */
    public static boolean writeFileFromBytes(File file, byte[] bytes, boolean append) throws IOException {
        if (file == null || bytes == null || !createOrExistsFile(file)) {
            return false;
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file, append);
            outputStream.write(bytes);
            outputStream.flush();
            return true;
        } finally {
            IOUtils.closeIO(outputStream);
        }
    }

    /**
     * 把字符串写进文件（平台默认字符集）。
     *
     * @param append 是否追加
     */
    public static boolean writeFileFromString(File file, String content, boolean append) {
        if (file == null || content == null || !createOrExistsFile(file)) {
            return false;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file, append));
            writer.write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            IOUtils.closeIO(writer);
        }
    }

    /** 按路径把字符串写进文件。 */
    public static boolean writeFileFromString(String filePath, String content, boolean append) {
        return writeFileFromString(getFileByPath(filePath), content, append);
    }
    // ------------------------------------------------------------------
    // 读文件
    // ------------------------------------------------------------------

    /**
     * 按行读取文件，只取 {@code [startLine, endLine]} 闭区间内的行（行号从 1 开始）。
     *
     * @param charset 字符集名，空则以平台默认字符集读
     * @return 参数非法返回 null；读失败返回 null（已打印堆栈）
     */
    public static List<String> readFileToList(File file, int startLine, int endLine, String charset) {
        if (file == null) {
            return null;
        }
        if (startLine > endLine) {
            return null;
        }
        List<String> lines = null;
        BufferedReader reader = null;
        try {
            lines = new ArrayList<String>();
            reader = TextUtils.isEmpty(charset)
                    ? new BufferedReader(new FileReader(file))
                    : new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            int lineNumber = 1;
            while (true) {
                String line = reader.readLine();
                if (line == null || lineNumber > endLine) {
                    break;
                }
                if (startLine <= lineNumber && lineNumber <= endLine) {
                    lines.add(line);
                }
                lineNumber++;
            }
            return lines;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeIO(reader);
        }
    }

    /** 按路径按行读取指定行区间。 */
    public static List<String> readFileToList(String filePath, int startLine, int endLine, String charset) {
        return readFileToList(getFileByPath(filePath), startLine, endLine, charset);
    }

    /**
     * 读出整个文件的所有行。
     *
     * <p>上界取 {@link Integer#MAX_VALUE} 当作"无限大"。</p>
     */
    public static List<String> readFileToList(File file, String charset) {
        return readFileToList(file, 0, Integer.MAX_VALUE, charset);
    }

    /** 按路径读出整个文件的所有行。 */
    public static List<String> readFileToList(String filePath, String charset) {
        return readFileToList(getFileByPath(filePath), charset);
    }

    /**
     * 把整个文件读成一个字符串。
     *
     * <p>每行后面补 "\r\n"（Windows 换行），最后再把末尾那 2 个字符删掉，
     * 所以原始换行风格（LF）会被统一成 CRLF——原厂行为，调用方（读配置文件）
     * 依赖这种"逐行 + CRLF"的格式。</p>
     *
     * @param charset 字符集名，空则以平台默认字符集读
     * @return 空文件返回空串；读取失败返回 null
     */
    public static String readFileToString(File file, String charset) {
        if (file == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = TextUtils.isEmpty(charset)
                    ? new BufferedReader(new InputStreamReader(new FileInputStream(file)))
                    : new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                builder.append(line).append("\r\n");
            }
            if (builder.length() >= 2) {
                return builder.delete(builder.length() - 2, builder.length()).toString();
            }
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeIO(reader);
        }
    }

    /** 按路径把整个文件读成字符串。 */
    public static String readFileToString(String filePath, String charset) {
        return readFileToString(getFileByPath(filePath), charset);
    }

    /** 把文件读成字节数组；文件不存在或读失败返回 null。 */
    public static byte[] readFileToBytes(File file) {
        if (file == null) {
            return null;
        }
        try {
            return IOUtils.toByteArray(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 按路径把文件读成字节数组。 */
    public static byte[] readFileToBytes(String filePath) {
        return readFileToBytes(getFileByPath(filePath));
    }

    /**
     * 嗅探文本文件的字符集。
     *
     * <p>只看文件头两个字节（BOM）：</p>
     * <ul>
     *   <li>0xEF 0xBB → UTF-8；</li>
     *   <li>0xFE 0xFF → UTF-16BE；</li>
     *   <li>0xFF 0xFE → Unicode（即 UTF-16LE）；</li>
     *   <li>其它一律当 GBK（国内设备的历史默认值）。</li>
     * </ul>
     *
     * @return 嗅探不到（文件读不了）返回 GBK
     */
    public static String detectCharset(File file) {
        int head = 0;
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            head = (inputStream.read() << 8) + inputStream.read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeIO(inputStream);
        }
        switch (head) {
            case 61371:
                return "UTF-8";
            case 65279:
                return "UTF-16BE";
            case 65534:
                return "Unicode";
            default:
                return "GBK";
        }
    }

    /** 按路径嗅探字符集。 */
    public static String detectCharset(String filePath) {
        return detectCharset(getFileByPath(filePath));
    }

    /**
     * 统计文件行数。
     *
     * <p>实现是"数 0x0A 的个数再 +1"，因此最后一行没有换行符也会被算上一行；
     * 空文件的返回值是 1（原厂如此，调用方（日志切分）依赖这个约定）。</p>
     */
    public static int countLines(File file) {
        int lines = 1;
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1024];
            while (true) {
                int read = inputStream.read(buffer, 0, 1024);
                if (read == -1) {
                    break;
                }
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == '\n') {
                        lines++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeIO(inputStream);
        }
        return lines;
    }

    /** 按路径统计行数。 */
    public static int countLines(String filePath) {
        return countLines(getFileByPath(filePath));
    }

    /** 文件大小，格式化成 "1.234MB" 这样的可读字符串；文件不存在返回空串。 */
    public static String getFileSize(File file) {
        return !isFileExists(file) ? "" : IOUtils.formatSize(file.length());
    }

    /** 按路径取文件大小。 */
    public static String getFileSize(String filePath) {
        return getFileSize(getFileByPath(filePath));
    }

    // ------------------------------------------------------------------
    // 路径拆解
    // ------------------------------------------------------------------

    /** 目录名（带结尾分隔符）；没有分隔符返回空串。 */
    public static String getDirName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastSep = filePath.lastIndexOf(File.separator);
        return lastSep == -1 ? "" : filePath.substring(0, lastSep + 1);
    }

    /** 取文件所在目录。 */
    public static String getDirName(File file) {
        if (file == null) {
            return null;
        }
        return getDirName(file.getPath());
    }

    /** 文件名（含扩展名）；没有分隔符时原样返回。 */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastSep = filePath.lastIndexOf(File.separator);
        return lastSep == -1 ? filePath : filePath.substring(lastSep + 1);
    }

    /** 取文件名（含扩展名）。 */
    public static String getFileName(File file) {
        if (file == null) {
            return null;
        }
        return getFileName(file.getPath());
    }

    /** 文件名（不含扩展名）。 */
    public static String getFileNameNoExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastDot = filePath.lastIndexOf('.');
        int lastSep = filePath.lastIndexOf(File.separator);
        if (lastSep == -1) {
            return lastDot != -1 ? filePath.substring(0, lastDot) : filePath;
        }
        return (lastDot == -1 || lastSep > lastDot) ? filePath.substring(lastSep + 1) : filePath.substring(lastSep + 1, lastDot);
    }

    /** 取文件名（不含扩展名）。 */
    public static String getFileNameNoExtension(File file) {
        if (file == null) {
            return null;
        }
        return getFileNameNoExtension(file.getPath());
    }

    /** 扩展名（不含点）；没有扩展名或点是目录名的一部分时返回空串。 */
    public static String getFileExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastDot = filePath.lastIndexOf('.');
        return (lastDot == -1 || filePath.lastIndexOf(File.separator) >= lastDot) ? "" : filePath.substring(lastDot + 1);
    }

    /** 取扩展名。 */
    public static String getFileExtension(File file) {
        if (file == null) {
            return null;
        }
        return getFileExtension(file.getPath());
    }

    /** 文件系统可用空间（字节）。 */
    public static long getFsAvailableSpace(@NonNull File file) {
        return file.getUsableSpace();
    }

    /** 按路径取文件系统可用空间。 */
    public static long getFsAvailableSpace(String filePath) {
        return getFsAvailableSpace(new File(filePath));
    }

    // ------------------------------------------------------------------
    // 容量格式化
    // ------------------------------------------------------------------

    /**
     * 用 DecimalFormat("#.00") 把字节数格式化成 B/K/M/G 的短格式。
     *
     * <p>与 {@link #formatSize(long)} 的区别：小数位固定 2 位（"#.00" 对整数部分为 0
     * 的值会输出 ".00" 这种形式），单位用单字母。两份实现都被业务直接调用，
     * 输出格式不能合并。</p>
     */
    public static String formatSizeShort(long byteCount) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        if (byteCount < PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            return decimalFormat.format(byteCount) + "B";
        }
        if (byteCount < PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
            return decimalFormat.format(byteCount / 1024.0d) + "K";
        }
        return byteCount < 1073741824
                ? decimalFormat.format(byteCount / 1048576.0d) + "M"
                : decimalFormat.format(byteCount / 1.073741824E9d) + "G";
    }

    /**
     * 用 "%.3f" 把字节数格式化成 B/KB/MB/GB。
     *
     * <p>与 {@link IOUtils#formatSize(long)} 是同一份实现的另一份拷贝（原厂就重复了），
     * 两个入口都有人调用，所以都保留。</p>
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
     * 外部存储总容量（系统 Formatter 的人类可读格式）。
     *
     * <p>原厂是私有实例方法，而本类构造函数直接抛异常，因此这几段是死代码；
     * 保留是为了"一个方法都不少"。</p>
     */
    private String getSdCardTotalSize(Context context) {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return Formatter.formatFileSize(context, ((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize()));
    }

    /** 外部存储可用容量（人类可读格式）。原厂死代码，保留。 */
    private String getSdCardAvailableSize(Context context) {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return Formatter.formatFileSize(context, ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize()));
    }

    /** data 分区总容量（人类可读格式）。原厂死代码，保留。 */
    private String getDataTotalSize(Context context) {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return Formatter.formatFileSize(context, ((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize()));
    }

    /** data 分区可用容量（人类可读格式）。原厂死代码，保留。 */
    private String getDataAvailableSize(Context context) {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return Formatter.formatFileSize(context, ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize()));
    }
    // ------------------------------------------------------------------
    // 复制 / 移动
    // ------------------------------------------------------------------

    /**
     * 复制目录（递归）。
     *
     * @return 只要有一层复制失败就返回 false（已复制的部分不回滚）
     */
    public static boolean copyDir(File srcDir, File destDir) {
        return copyDir(srcDir, destDir, false);
    }

    /** 按路径复制目录。 */
    public static boolean copyDir(String srcDirPath, String destDirPath) {
        return copyDir(getFileByPath(srcDirPath), getFileByPath(destDirPath));
    }

    /**
     * 移动目录：复制成功后再删源目录。
     *
     * <p>失败时源目录仍在、目标目录可能是半成品——原厂没有事务性保证，保持原样。</p>
     */
    public static boolean moveDir(File srcDir, File destDir) {
        return copyDir(srcDir, destDir, true);
    }

    /** 按路径移动目录。 */
    public static boolean moveDir(String srcDirPath, String destDirPath) {
        return moveDir(getFileByPath(srcDirPath), getFileByPath(destDirPath));
    }

    /**
     * 复制目录的核心实现。
     *
     * @param deleteSource 复制完成后是否删除源目录（移动语义）
     */
    private static boolean copyDir(File srcDir, File destDir, boolean deleteSource) {
        if (srcDir == null || destDir == null) {
            return false;
        }
        // 两个路径都补上分隔符再比较，避免 "/a/b" 与 "/a/bc" 这种前缀误判
        String srcPath = srcDir.getPath() + File.separator;
        String destPath = destDir.getPath() + File.separator;
        if (destPath.contains(srcPath) || !srcDir.exists() || !srcDir.isDirectory() || !createOrExistsDir(destDir)) {
            return false;
        }
        for (File srcFile : srcDir.listFiles()) {
            File destFile = new File(destPath + srcFile.getName());
            if (srcFile.isFile()) {
                if (!copyFile(srcFile, destFile, deleteSource)) {
                    return false;
                }
            } else if (srcFile.isDirectory() && !copyDir(srcFile, destFile, deleteSource)) {
                return false;
            }
        }
        return !deleteSource || deleteDir(srcDir);
    }

    /**
     * 按路径复制目录的核心实现（字符串版）。
     *
     * <p>原厂保留了这层"路径 → File"的私有转发，目前没有调用方，
     * 属于历史遗留的对称 API，一并还原。</p>
     */
    private static boolean copyDir(String srcDirPath, String destDirPath, boolean deleteSource) {
        return copyDir(getFileByPath(srcDirPath), getFileByPath(destDirPath), deleteSource);
    }

    /** 复制单个文件（目标已存在则直接失败，不覆盖）。 */
    public static boolean copyFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, false);
    }

    /** 按路径复制单个文件。 */
    public static boolean copyFile(String srcFilePath, String destFilePath) {
        return copyFile(getFileByPath(srcFilePath), getFileByPath(destFilePath));
    }

    /**
     * 移动单个文件：复制成功后再删源文件。
     *
     * <p>注意"目标已存在"时这里会直接失败而不覆盖，调用方需要自己先删。</p>
     */
    public static boolean moveFile(File srcFile, File destFile) {
        return copyFile(srcFile, destFile, true);
    }

    /** 按路径移动单个文件。 */
    public static boolean moveFile(String srcFilePath, String destFilePath) {
        return moveFile(getFileByPath(srcFilePath), getFileByPath(destFilePath));
    }

    /** 复制单个文件的核心实现。 */
    private static boolean copyFile(File srcFile, File destFile, boolean deleteSource) {
        if (srcFile == null || destFile == null || !srcFile.exists() || !srcFile.isFile()) {
            return false;
        }
        if ((destFile.exists() && destFile.isFile()) || !createOrExistsDir(destFile.getParentFile())) {
            return false;
        }
        try {
            if (writeFileFromStream(destFile, new FileInputStream(srcFile), false)) {
                return !deleteSource || deleteFile(srcFile);
            }
            return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 按路径复制单个文件的核心实现（字符串版），同样是对称 API 的还原。 */
    private static boolean copyFile(String srcFilePath, String destFilePath, boolean deleteSource) {
        return copyFile(getFileByPath(srcFilePath), getFileByPath(destFilePath), deleteSource);
    }

    /** 把输入流原样搬运到输出流，两端都会被关闭。 */
    public static void copyStream(@NonNull InputStream inputStream, @NonNull OutputStream outputStream) throws IOException {
        BufferedOutputStream bufferedOutputStream = null;
        try {
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            byte[] buffer = new byte[1024];
            while (true) {
                int read = inputStream.read(buffer, 0, 1024);
                if (read == -1) {
                    bufferedOutputStream.flush();
                    return;
                }
                bufferedOutputStream.write(buffer, 0, read);
            }
        } finally {
            IOUtils.closeIO(inputStream);
            IOUtils.closeIO(bufferedOutputStream);
        }
    }

    /**
     * 把 assets 下的文件（或整个目录）复制到目标路径。
     *
     * <p>实现上先 {@code assets.list(path)} 判断是目录还是文件：
     * 目录则递归（目标按同名子路径展开），文件则按 1KB 缓冲拷贝。
     * 任何异常（包括目标不可写）都只打印堆栈，不抛给调用方。</p>
     */
    public static void copyAssetsToDir(Context context, String assetPath, String destPath) {
        try {
            String[] children = context.getAssets().list(assetPath);
            if (children.length > 0) {
                new File(destPath).mkdirs();
                for (String child : children) {
                    copyAssetsToDir(context, assetPath + "/" + child, destPath + "/" + child);
                }
                return;
            }
            InputStream inputStream = context.getAssets().open(assetPath);
            FileOutputStream outputStream = new FileOutputStream(new File(destPath));
            byte[] buffer = new byte[1024];
            while (true) {
                int read = inputStream.read(buffer);
                if (read == -1) {
                    outputStream.flush();
                    inputStream.close();
                    outputStream.close();
                    return;
                }
                outputStream.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 把 assets 里的文本文件读成字符串（按平台默认字符集解码）。
     *
     * <p>读成功后还会把内容打到 android.util.Log（tag 同样是 FileUtil）；
     * 打日志的 IOException 被单独 catch 一次，不影响返回值。</p>
     *
     * @return 读取失败返回 null
     */
    public static String readAssetsAsString(Context context, String assetPath) {
        String content = null;
        try {
            InputStream inputStream = context.getAssets().open(assetPath);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            content = new String(buffer);
            Log.d(TAG, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    // ------------------------------------------------------------------
    // Uri → 真实路径
    // ------------------------------------------------------------------

    /**
     * 把 Android 各种 Uri（document / content / file）转换成文件系统路径。
     *
     * <p>分派顺序：</p>
     * <ol>
     *   <li>API &gt; 19 且是 SAF 的 document Uri：按 authority 三种情况分别处理
     *       （media → 按 image/video/audio 换 MediaStore Uri 再查 _data；
     *       downloads → 拼 public_downloads Uri 再查；externalstorage → 直接拆 documentId）；</li>
     *   <li>scheme 为空或 "file" → 直接取 uri.getPath()；</li>
     *   <li>scheme 为 "content" → 查 _data 列；</li>
     *   <li>其它 scheme 一律返回 null。</li>
     * </ol>
     *
     * <p>注意 media 分支里如果 documentId 前缀既不是 image/video/audio，
     * 原厂会把 null 传给查询方法（会抛 NPE）——这是既有行为，不额外兜底。</p>
     */
    public static String getFilePathFromUri(Context context, @NonNull Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        LogUtil.d(TAG, "Authority: " + uri.getAuthority() + ", Fragment: " + uri.getFragment() + ", Port: " + uri.getPort()
                + ", Query: " + uri.getQuery() + ", Scheme: " + uri.getScheme() + ", Host: " + uri.getHost()
                + ", Segments: " + uri.getPathSegments().toString());
        if (Build.VERSION.SDK_INT > 19 && DocumentsContract.isDocumentUri(context, uri)) {
            String authority = uri.getAuthority();
            String documentId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(authority)) {
                String prefix = documentId.split(":")[0];
                Uri contentUri;
                if ("image".equals(prefix)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(prefix)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else {
                    contentUri = "audio".equals(prefix) ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : null;
                }
                return queryFilePath(context, contentUri, null, null);
            }
            if ("com.android.providers.downloads.documents".equals(authority)) {
                return queryFilePath(context,
                        ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(documentId)),
                        null, null);
            }
            if ("com.android.externalstorage.documents".equals(authority)) {
                String[] parts = documentId.split(":");
                // 原厂两个分支写的是同一个表达式（primary 与否都用外部存储根 + 第二段），此处照抄
                return "primary".equalsIgnoreCase(parts[0])
                        ? Environment.getExternalStorageDirectory() + "/" + parts[1]
                        : Environment.getExternalStorageDirectory() + "/" + parts[1];
            }
        }
        String scheme = uri.getScheme();
        if (scheme != null && !"file".equals(scheme)) {
            if (!"content".equals(scheme)) {
                return null;
            }
            Cursor cursor = null;
            String path = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex("_data");
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                return path;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return uri.getPath();
    }

    /**
     * 按 {@code _data} 列查真实路径。
     *
     * <p>查不到（游标为空/没有行）返回 null；无论成败都关闭游标。</p>
     */
    private static String queryFilePath(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{"_data"}, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ------------------------------------------------------------------
    // XML → Bean（反射）
    // ------------------------------------------------------------------

    /**
     * 把 XML 反序列化成一个 JavaBean。
     *
     * <p>约定：根标签的属性写进 bean 自身的同名字段；子标签名与字段名（忽略大小写）
     * 对应，子标签的文本写进该字段；若字段类型是 {@code java.util.List}，
     * 则用泛型实参 new 出元素对象，把该子标签的属性写进元素，遇到同名结束标签时
     * 把元素塞进 list。</p>
     *
     * <p>异常处理（与原厂一致）：解析过程中任何异常都只打印堆栈并返回"已经构造了一半"
     * 的对象；连 setInput 都失败时返回 null。</p>
     */
    public static Object xmlToObject(InputStream inputStream, Class<?> cls) {
        XmlPullParser parser = Xml.newPullParser();
        int eventType;
        try {
            parser.setInput(inputStream, "UTF-8");
            eventType = parser.getEventType();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Object result = null;
        // 当前正在收集的 List 元素对象、它在 bean 里对应的字段名、以及那个 List 本身
        Object listItem = null;
        String currentName = null;
        ArrayList<Object> itemList = null;
        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        result = cls.newInstance();
                        break;
                    case XmlPullParser.START_TAG: {
                        String tagName = parser.getName();
                        Field[] fields;
                        if (listItem == null) {
                            assert result != null;
                            fields = result.getClass().getDeclaredFields();
                            int attributeCount = parser.getAttributeCount();
                            for (int i = 0; i < attributeCount; i++) {
                                setFieldValue(result, parser.getAttributeName(i), parser.getAttributeValue(i));
                            }
                        } else {
                            fields = listItem.getClass().getDeclaredFields();
                        }
                        for (Field field : fields) {
                            if (!field.getName().equalsIgnoreCase(tagName)) {
                                continue;
                            }
                            if ("java.util.List".equals(field.getType().getName())) {
                                Type genericType = field.getGenericType();
                                if (!(genericType instanceof ParameterizedType)) {
                                    // 字段没有泛型实参时原厂直接放弃这个标签
                                    break;
                                }
                                Object element = ((Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0]).newInstance();
                                String fieldName = field.getName();
                                int attributeCount = parser.getAttributeCount();
                                for (int i = 0; i < attributeCount; i++) {
                                    setFieldValue(element, parser.getAttributeName(i), parser.getAttributeValue(i));
                                }
                                if (itemList == null) {
                                    itemList = new ArrayList<Object>();
                                    field.setAccessible(true);
                                    field.set(result, itemList);
                                }
                                listItem = element;
                                currentName = fieldName;
                            } else if (listItem != null) {
                                setFieldValue(listItem, tagName, parser.nextText());
                            } else {
                                setFieldValue(result, tagName, parser.nextText());
                            }
                            break;
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG:
                        if (listItem != null && currentName.equalsIgnoreCase(parser.getName())) {
                            itemList.add(listItem);
                            currentName = null;
                            listItem = null;
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 把 XML 反序列化成 JavaBean 列表。
     *
     * <p>与 {@link #xmlToObject(InputStream, Class)} 的区别：这里以 {@code rootTag} 为
     * "一条记录"的标签，每遇到同名开始标签就 new 一个对象并填属性，
     * 遇到同名结束标签就把它收进列表；子标签文本同样按字段名写入。</p>
     *
     * <p>异常处理与前一个方法一致：解析异常打印堆栈并返回已经收集到的列表
     * （可能为 null）；setInput 失败返回 null。</p>
     */
    public static List<Object> xmlToList(InputStream inputStream, Class<?> cls, String rootTag) {
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<Object> list = null;
        Object current = null;
        int eventType;
        try {
            parser.setInput(inputStream, "UTF-8");
            eventType = parser.getEventType();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        list = new ArrayList<Object>();
                        break;
                    case XmlPullParser.START_TAG: {
                        String tagName = parser.getName();
                        if (rootTag.equals(tagName)) {
                            current = cls.newInstance();
                            int attributeCount = parser.getAttributeCount();
                            for (int i = 0; i < attributeCount; i++) {
                                setFieldValue(current, parser.getAttributeName(i), parser.getAttributeValue(i));
                            }
                        } else if (current != null) {
                            setFieldValue(current, tagName, parser.nextText());
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG:
                        if (rootTag.equals(parser.getName())) {
                            assert list != null;
                            list.add(current);
                            current = null;
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 按字段名把字符串写进对象（反射，忽略大小写）。
     *
     * <p>支持 String/int/float/double/long/short/boolean 七种基本类型的解析，
     * 其余类型（含包装类型）一律按 String 直接 set——也就是说给 Integer 字段传 "1"
     * 会抛 IllegalArgumentException，被外层吞掉。原厂如此，不做增强。</p>
     *
     * <p>命中字段后没有 break，会继续扫描后续字段（同名字段不会重复，幂等）。</p>
     */
    private static void setFieldValue(Object target, String fieldName, String fieldValue) {
        try {
            Field[] fields = target.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getName().equalsIgnoreCase(fieldName)) {
                    fields[i].setAccessible(true);
                    Class<?> type = fields[i].getType();
                    if (type == String.class) {
                        fields[i].set(target, fieldValue);
                    } else if (type == Integer.TYPE) {
                        fields[i].set(target, Integer.valueOf(Integer.parseInt(fieldValue)));
                    } else if (type == Float.TYPE) {
                        fields[i].set(target, Float.valueOf(Float.parseFloat(fieldValue)));
                    } else if (type == Double.TYPE) {
                        fields[i].set(target, Double.valueOf(Double.parseDouble(fieldValue)));
                    } else if (type == Long.TYPE) {
                        fields[i].set(target, Long.valueOf(Long.parseLong(fieldValue)));
                    } else if (type == Short.TYPE) {
                        fields[i].set(target, Short.valueOf(Short.parseShort(fieldValue)));
                    } else if (type == Boolean.TYPE) {
                        fields[i].set(target, Boolean.valueOf(Boolean.parseBoolean(fieldValue)));
                    } else {
                        fields[i].set(target, fieldValue);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
