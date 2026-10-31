package com.xtc.bigdata.common.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.log.LogUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 采集库的文件工具。
 *
 * <p>与 {@code com.xtc.utils.storage.FileUtils} 是两份独立实现：这一份带
 * {@code BigData} 私有目录约定（{@link #getBigDataDirPath()}）与
 * {@link Uri} 转真实路径的能力。</p>
 *
 * <p>设计上有两条贯穿全类的规则，改动前请想清楚：</p>
 * <ul>
 *   <li>写文件时的关闭动作全部放在 finally / 显式 close 里，<b>异常照样往外抛</b>
 *       —— 上层靠这个判断落盘是否成功；</li>
 *   <li>读文件失败返回 null / 空串，不抛 —— 读不到数据不值得让调用方崩。</li>
 * </ul>
 */
public class FileUtils {

    /** 单位：字节。 */
    public static final int BYTE = 1;

    /** 单位：GB。 */
    public static final int GB = 1073741824;

    /** 单位：KB。 */
    public static final int KB = 1024;

    /** 单位：MB。 */
    public static final int MB = 1048576;

    private static final String TAG = "FileUtils";

    /** 统一 UTF-8。 */
    private static final String UTF_8 = "UTF-8";

    /** 读缓冲大小。 */
    private static final int BUFFER_SIZE = 1024;

    private FileUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 字节数转自适应单位。
     *
     * @param size 字节数
     * @return 形如 {@code "1.234MB"}；负数返回提示串
     */
    public static String byte2FitSize(long size) {
        if (size < 0) {
            return "shouldn't be less than zero!";
        }
        if (size < 1048576) {
            return String.format(Locale.getDefault(), "%.3fB", Double.valueOf(size));
        }
        if (size < 2097152) {
            return String.format(Locale.getDefault(), "%.3fKB", Double.valueOf(size / 1024.0d));
        }
        return size < 1073741824
                ? String.format(Locale.getDefault(), "%.3fMB", Double.valueOf(size / 1048576.0d))
                : String.format(Locale.getDefault(), "%.3fGB", Double.valueOf(size / 1.073741824E9d));
    }

    /**
     * 关闭流，失败只在调试模式下提示。
     *
     * @param closeable 待关闭对象
     */
    public static void closeIO(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            if (Constants.isDebug) {
                LogUtil.w(TAG, e + ":关闭io失败, 这不科学 - -");
            }
        }
    }

    /**
     * 拷贝或移动整个目录。
     *
     * @param source 源目录
     * @param target 目标目录
     * @param move   true 表示移动（成功后删源）
     * @return 是否成功
     */
    private static boolean copyOrMoveDir(File source, File target, boolean move) {
        if (source == null || target == null) {
            return false;
        }
        String sourcePath = source.getPath() + File.separator;
        String targetPath = target.getPath() + File.separator;
        // 目标在源内部会无限递归，必须提前拦掉
        if (targetPath.contains(sourcePath) || !source.exists() || !source.isDirectory() || !createDirOrExists(target)) {
            return false;
        }
        for (File child : source.listFiles()) {
            File childTarget = new File(targetPath + child.getName());
            if (child.isFile()) {
                if (!copyOrMoveFile(child, childTarget, move)) {
                    return false;
                }
            } else if (child.isDirectory() && !copyOrMoveDir(child, childTarget, move)) {
                return false;
            }
        }
        return !move || deleteDir(source);
    }

    /**
     * 拷贝或移动单个文件。
     *
     * @param source 源文件
     * @param target 目标文件
     * @param move   true 表示移动（成功后删源）
     * @return 是否成功
     */
    private static boolean copyOrMoveFile(File source, File target, boolean move) {
        if (source == null || target == null || !source.exists() || !source.isFile()) {
            return false;
        }
        if ((target.exists() && target.isFile()) || !createDirOrExists(target.getParentFile())) {
            return false;
        }
        try {
            if (writeFile(target, (InputStream) new FileInputStream(source), false)) {
                return !move || deleteFile(source);
            }
            return false;
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return false;
        }
    }

    /**
     * 确保目录存在（不存在则创建）。
     *
     * @param dir 目录
     * @return 存在或是有效目录返回 true；入参为 null 或创建失败返回 false
     */
    public static boolean createDirOrExists(File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.exists()) {
            return dir.isDirectory();
        }
        return dir.mkdirs();
    }

    /**
     * 确保目录存在（字符串重载）。
     *
     * @param dirPath 目录路径
     * @return 是否可用
     */
    public static boolean createDirOrExists(String dirPath) {
        return createDirOrExists(getFileByPath(dirPath));
    }

    /**
     * 先删旧文件再创建新文件。
     *
     * @param file 目标文件
     * @return 是否创建成功
     */
    public static boolean createFileByDeleteOld(File file) {
        if (file == null) {
            return false;
        }
        if ((file.exists() && file.isFile() && !file.delete()) || !createDirOrExists(file.getParentFile())) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            if (!Constants.isDebug) {
                return false;
            }
            LogUtil.w(TAG, e + ":创建文件失败");
            return false;
        }
    }

    /**
     * 先删旧文件再创建新文件（字符串重载）。
     *
     * @param filePath 目标文件路径
     * @return 是否创建成功
     */
    public static boolean createFileByDeleteOld(String filePath) {
        return createFileByDeleteOld(getFileByPath(filePath));
    }

    /**
     * 文件不存在则创建，存在则判断是不是文件。
     *
     * @param file 目标文件
     * @return 可用返回 true
     */
    public static boolean createFileOrExists(File file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return file.isFile();
        }
        if (!createDirOrExists(file.getParentFile())) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            if (!Constants.isDebug) {
                return false;
            }
            LogUtil.w(TAG, e + ":创建文件失败");
            return false;
        }
    }

    /**
     * 文件不存在则创建（字符串重载）。
     *
     * @param filePath 目标文件路径
     * @return 是否可用
     */
    public static boolean createFileOrExists(String filePath) {
        return createFileOrExists(getFileByPath(filePath));
    }

    /**
     * 递归删除目录。
     *
     * @param dir 目录
     * @return 是否删除成功；不存在返回 true
     */
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
        File[] children = dir.listFiles();
        if (children != null && children.length != 0) {
            for (File child : children) {
                if (child.isFile()) {
                    if (!deleteFile(child)) {
                        return false;
                    }
                } else if (child.isDirectory() && !deleteDir(child)) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * 删除单个文件。
     *
     * @param file 文件
     * @return 是否删除成功；不存在返回 true
     */
    public static boolean deleteFile(File file) {
        return file != null && (!file.exists() || (file.isFile() && file.delete()));
    }

    /**
     * 删除单个文件（字符串重载）。
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        return deleteFile(getFileByPath(filePath));
    }

    /**
     * 确保 SD 卡根路径已经写进配置。
     *
     * <p>只在配置为空<b>且</b>外置存储已挂载时才写入，避免把空路径固化下来。</p>
     */
    public static void ensureSdCardPath() {
        if (TextUtils.isEmpty(ConfigAgent.getBehaviorConfig().sdcardRootPath)
                && "mounted".equals(Environment.getExternalStorageState())) {
            ConfigAgent.getBehaviorConfig().sdcardRootPath = Environment.getExternalStorageDirectory().getPath();
        }
    }

    /**
     * 取采集库私有目录（{@code <sdcardRoot>/BigData}）。
     *
     * @return 目录路径
     */
    public static String getBigDataDirPath() {
        ensureSdCardPath();
        return ConfigAgent.getBehaviorConfig().sdcardRootPath + DeviceInfo.BIG_DATA_DIR;
    }

    /**
     * 从 content Uri 里查 {@code _data} 列（真实路径）。
     *
     * @param context       上下文
     * @param uri           内容 Uri
     * @param selection     条件
     * @param selectionArgs 条件参数
     * @return 真实路径；查不到返回 null
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
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

    /**
     * 取目录名（带末尾分隔符）。
     *
     * @param file 文件
     * @return 目录名；入参为 null 返回 null
     */
    public static String getDirName(File file) {
        if (file == null) {
            return null;
        }
        return getDirName(file.getAbsolutePath());
    }

    /**
     * 取目录名（带末尾分隔符）。
     *
     * @param path 绝对路径
     * @return 目录名；没有分隔符返回空串
     */
    private static String getDirName(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        int lastSeparator = path.lastIndexOf(File.separator);
        return lastSeparator == -1 ? "" : path.substring(0, lastSeparator + 1);
    }

    /**
     * 取文件所在分区可用空间。
     *
     * @param file 文件
     * @return 字节数
     */
    public static long getFileAvaiableSize(File file) {
        return file.getUsableSpace();
    }

    /**
     * 取文件所在分区可用空间（字符串重载）。
     *
     * @param filePath 文件路径
     * @return 字节数
     */
    public static long getFileAvaiableSize(String filePath) {
        return getFileAvaiableSize(new File(filePath));
    }

    /**
     * 路径转 File，空白路径返回 null。
     *
     * @param filePath 路径
     * @return File 或 null
     */
    static File getFileByPath(String filePath) {
        if (StringUtils.isSpace(filePath)) {
            return null;
        }
        return new File(filePath);
    }

    /**
     * 取文件扩展名（不含点）。
     *
     * @param file 文件
     * @return 扩展名；入参为 null 返回 null
     */
    public static String getFileExtension(File file) {
        if (file == null) {
            return null;
        }
        return getFileExtension(file.getAbsolutePath());
    }

    /**
     * 取文件扩展名（不含点）。
     *
     * @param filePath 路径
     * @return 扩展名；无扩展名返回空串
     */
    private static String getFileExtension(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }
        int lastDot = filePath.lastIndexOf(46);
        return (lastDot == -1 || filePath.lastIndexOf(File.separator) >= lastDot)
                ? "" : filePath.substring(lastDot + 1);
    }

    /**
     * 取文件名（带扩展名）。
     *
     * @param file 文件
     * @return 文件名；入参为 null 返回 null
     */
    public static String getFileName(File file) {
        if (file == null) {
            return null;
        }
        return getFileName(file.getAbsolutePath());
    }

    /**
     * 取文件名（带扩展名）。
     *
     * @param filePath 路径
     * @return 文件名；没有分隔符时返回原串
     */
    private static String getFileName(String filePath) {
        int lastSeparator;
        return (StringUtils.isEmpty(filePath) || (lastSeparator = filePath.lastIndexOf(File.separator)) == -1)
                ? filePath : filePath.substring(lastSeparator + 1);
    }

    /**
     * 取文件大小。
     *
     * @param file 文件
     * @return 字节数；文件不存在返回 -1
     */
    public static long getFileSize(File file) {
        if (isFileExists(file)) {
            return file.length();
        }
        return -1L;
    }

    /**
     * 取文件大小（字符串重载）。
     *
     * @param filePath 文件路径
     * @return 字节数；文件不存在返回 -1
     */
    public static long getFileSize(String filePath) {
        return getFileSize(getFileByPath(filePath));
    }

    /**
     * 取上传用目录（外置私有目录，取不到退内部 files）。
     *
     * @param context    上下文
     * @param uploadDir  子目录名
     * @return 目录
     */
    public static File getUploadDir(Context context, String uploadDir) {
        File externalFilesDir = context.getExternalFilesDir(uploadDir);
        if (externalFilesDir == null || !externalFilesDir.exists()) {
            return context.getFilesDir();
        }
        return TextUtils.isEmpty(externalFilesDir.getAbsolutePath()) ? context.getFilesDir() : externalFilesDir;
    }

    /**
     * 把 content/file 协议的 Uri 转成真实文件路径。
     *
     * <p>Android 4.4 起 {@code DocumentsProvider} 会给出
     * {@code content://com.android.*.documents/...} 形态的 Uri，
     * 需要按 authority 分别翻译；更早的系统走 {@code _data} 列。</p>
     *
     * @param context 上下文
     * @param uri     原始 Uri
     * @return 真实路径；无法解析返回 null
     */
    public static String getUriReallyPath(Context context, Uri uri) {
        String realPath = null;
        if (context == null || uri == null) {
            return null;
        }
        LogUtil.d(TAG, "Authority: " + uri.getAuthority() + ", Fragment: " + uri.getFragment()
                + ", Port: " + uri.getPort() + ", Query: " + uri.getQuery() + ", Scheme: " + uri.getScheme()
                + ", Host: " + uri.getHost() + ", Segments: " + uri.getPathSegments().toString());
        if (Build.VERSION.SDK_INT > 19 && DocumentsContract.isDocumentUri(context, uri)) {
            String authority = uri.getAuthority();
            String documentId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(authority)) {
                String mediaType = documentId.split(":")[0];
                Uri mediaUri;
                if ("image".equals(mediaType)) {
                    mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(mediaType)) {
                    mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else {
                    mediaUri = "audio".equals(mediaType) ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : null;
                }
                return getDataColumn(context, mediaUri, null, null);
            }
            if ("com.android.providers.downloads.documents".equals(authority)) {
                return getDataColumn(context,
                        ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(documentId)),
                        null, null);
            }
            if ("com.android.externalstorage.documents".equals(authority)) {
                String[] documentParts = documentId.split(":");
                // 两个分支拼出来的路径是一样的 —— 原厂就是这么写的，保留
                return "primary".equalsIgnoreCase(documentParts[0])
                        ? Environment.getExternalStorageDirectory() + "/" + documentParts[1]
                        : Environment.getExternalStorageDirectory() + "/" + documentParts[1];
            }
        }
        String scheme = uri.getScheme();
        if (scheme != null && !"file".equals(scheme)) {
            if (!"content".equals(scheme)) {
                return null;
            }
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataIndex = cursor.getColumnIndex("_data");
                    if (dataIndex > -1) {
                        realPath = cursor.getString(dataIndex);
                    }
                }
                return realPath;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return uri.getPath();
    }

    /**
     * 老名字的兼容方法（原厂拼写错误 {@code Realy}）。
     *
     * @param context 上下文
     * @param uri     原始 Uri
     * @return 真实路径
     * @deprecated 用 {@link #getUriReallyPath(Context, Uri)}
     */
    @Deprecated
    public static String getUriRealyPath(Context context, Uri uri) {
        return getUriReallyPath(context, uri);
    }

    /**
     * 是否目录。
     *
     * @param file 文件
     * @return 是否目录
     */
    public static boolean isDir(File file) {
        return isFileExists(file) && file.isDirectory();
    }

    /**
     * 是否目录（字符串重载）。
     *
     * @param filePath 路径
     * @return 是否目录
     */
    public static boolean isDir(String filePath) {
        return isDir(getFileByPath(filePath));
    }

    /**
     * 是否文件。
     *
     * @param file 文件
     * @return 是否文件
     */
    public static boolean isFile(File file) {
        return isFileExists(file) && file.isFile();
    }

    /**
     * 是否文件（字符串重载）。
     *
     * @param filePath 路径
     * @return 是否文件
     */
    public static boolean isFile(String filePath) {
        return isFile(getFileByPath(filePath));
    }

    /**
     * 路径是否存在。
     *
     * @param file 文件
     * @return 是否存在
     */
    public static boolean isFileExists(File file) {
        return file != null && file.exists();
    }

    /**
     * 路径是否存在（字符串重载）。
     *
     * @param filePath 路径
     * @return 是否存在
     */
    public static boolean isFileExists(String filePath) {
        return isFileExists(getFileByPath(filePath));
    }

    /**
     * 递归列出目录下所有文件。
     *
     * @param dir 目录
     * @return 文件列表（含子目录里的）；目录不可用返回 null
     */
    public static List<File> listFilesInDir(File dir) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        for (File child : dir.listFiles()) {
            files.add(child);
            if (child.isDirectory()) {
                files.addAll(listFilesInDir(child));
            }
        }
        return files;
    }

    /**
     * 列出目录下文件，可选是否递归。
     *
     * @param dir        目录
     * @param recursive  是否递归
     * @return 文件列表；目录不可用返回 null
     */
    public static List<File> listFilesInDir(File dir, boolean recursive) {
        if (recursive) {
            return listFilesInDir(dir);
        }
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        Collections.addAll(files, dir.listFiles());
        return files;
    }

    /**
     * 按过滤器递归列出文件。
     *
     * @param dir    目录
     * @param filter 文件名过滤器
     * @return 文件列表；目录不可用返回 null
     */
    public static List<File> listFilesInDirWithFilter(File dir, FilenameFilter filter) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        for (File child : dir.listFiles()) {
            if (filter.accept(child.getParentFile(), child.getName())) {
                files.add(child);
            }
            if (child.isDirectory()) {
                files.addAll(listFilesInDirWithFilter(child, filter));
            }
        }
        return files;
    }

    /**
     * 按过滤器列出文件，可选是否递归。
     *
     * @param dir       目录
     * @param filter    文件名过滤器
     * @param recursive 是否递归
     * @return 文件列表；目录不可用返回 null
     */
    public static List<File> listFilesInDirWithFilter(File dir, FilenameFilter filter, boolean recursive) {
        if (recursive) {
            return listFilesInDirWithFilter(dir, filter);
        }
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        for (File child : dir.listFiles()) {
            if (filter.accept(child.getParentFile(), child.getName())) {
                files.add(child);
            }
        }
        return files;
    }

    /**
     * 按后缀名递归列出文件（忽略大小写）。
     *
     * @param dir      目录
     * @param suffix   后缀
     * @return 文件列表；目录不可用返回 null
     */
    public static List<File> listFilesInDirWithFilter(File dir, String suffix) {
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        for (File child : dir.listFiles()) {
            if (child.getName().toUpperCase().endsWith(suffix.toUpperCase())) {
                files.add(child);
            }
            if (child.isDirectory()) {
                files.addAll(listFilesInDirWithFilter(child, suffix));
            }
        }
        return files;
    }

    /**
     * 按后缀名列出文件，可选是否递归。
     *
     * @param dir       目录
     * @param suffix    后缀
     * @param recursive 是否递归
     * @return 文件列表；目录不可用返回 null
     */
    public static List<File> listFilesInDirWithFilter(File dir, String suffix, boolean recursive) {
        if (recursive) {
            return listFilesInDirWithFilter(dir, suffix);
        }
        if (dir == null || !isDir(dir)) {
            return null;
        }
        ArrayList<File> files = new ArrayList<File>();
        File[] children = dir.listFiles();
        assert children != null;
        for (File child : children) {
            if (child.getName().toUpperCase().endsWith(suffix.toUpperCase())) {
                files.add(child);
            }
        }
        return files;
    }

    /**
     * 读文件全部字节。
     *
     * @param file 文件
     * @return 字节数组
     * @throws FileNotFoundException 文件不存在
     */
    public static byte[] readFile2Bytes(File file) throws FileNotFoundException {
        return readFile2Bytes(new FileInputStream(file));
    }

    /**
     * 读流全部字节。
     *
     * @param inputStream 输入流
     * @return 字节数组；读失败返回 null
     */
    public static byte[] readFile2Bytes(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            transfer(inputStream, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            if (Constants.isDebug) {
                LogUtil.w(TAG, e + ":读取数据失败, 不科学 - -");
            }
            return null;
        }
    }

    /**
     * 读文件全部字节（字符串重载）。
     *
     * @param filePath 文件路径
     * @return 字节数组
     * @throws FileNotFoundException 文件不存在
     */
    public static byte[] readFile2Bytes(String filePath) throws FileNotFoundException {
        return readFile2Bytes(getFileByPath(filePath));
    }

    /**
     * 读文本文件（UTF-8）。
     *
     * @param filePath 文件路径
     * @return 文本；文件不存在或读失败返回 null
     */
    public static String readFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int readCount = bufferedInputStream.read(buffer);
                if (readCount == -1) {
                    return new String(byteArrayOutputStream.toByteArray(), UTF_8);
                }
                byteArrayOutputStream.write(buffer, 0, readCount);
            }
        } catch (IOException e) {
            if (Constants.isDebug) {
                LogUtil.e(TAG, e);
            }
            return null;
        } finally {
            CloseableUtils.closeAllQuietly(byteArrayOutputStream, bufferedInputStream, fileInputStream);
        }
    }

    /**
     * 把内容写到指定目录下的文件（目录不存在则建）。
     *
     * <p>写成功会打一条 {@code "save file to sdcard successful:"} 日志 ——
     * 线上排查"文件到底存不存在"就靠它。</p>
     *
     * @param dirPath  目录
     * @param content  内容
     * @param fileName 文件名
     */
    public static void saveFile(String dirPath, String content, String fileName) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dirPath, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LogUtil.e(TAG, e);
            }
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
            LogUtil.i(TAG, "save file to sdcard successful:" + file.getAbsolutePath());
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }
    }

    /**
     * 把内容覆盖写入指定路径。
     *
     * @param content  内容
     * @param filePath 目标路径
     */
    public static void saveToFile(String content, String filePath) {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, UTF_8);
            outputStreamWriter.write(content);
            outputStreamWriter.flush();
            fileOutputStream.flush();
        } catch (Exception e) {
            if (Constants.isDebug) {
                LogUtil.e(TAG, e);
            }
        } finally {
            CloseableUtils.closeAllQuietly(fileOutputStream, outputStreamWriter);
        }
    }

    /**
     * 把输入流全部搬到输出流。
     *
     * <p>结束时（无论成功失败）都会关闭入参与出参 —— 这是调用方约定的
     * "一次性管道"语义，别在外面重复 close 也别指望还能复用流。</p>
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 读写失败
     */
    public static void transfer(InputStream inputStream, OutputStream outputStream) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int readCount = inputStream.read(buffer, 0, BUFFER_SIZE);
                if (readCount == -1) {
                    bufferedOutputStream.flush();
                    return;
                }
                bufferedOutputStream.write(buffer, 0, readCount);
            }
        } finally {
            closeIO(inputStream);
            closeIO(bufferedOutputStream);
        }
    }

    /**
     * 用输入流写文件。
     *
     * @param file        目标文件
     * @param inputStream 数据来源
     * @param append      是否追加
     * @return 是否成功
     * @throws IOException 读写失败
     */
    public static boolean writeFile(File file, InputStream inputStream, boolean append) throws IOException {
        if (file == null || inputStream == null || !createFileOrExists(file)) {
            return false;
        }
        transfer(inputStream, new FileOutputStream(file, append));
        return true;
    }

    /**
     * 用字符串写文件。
     *
     * @param file    目标文件
     * @param content 内容
     * @param append  是否追加
     * @return 是否成功
     * @throws IOException 写失败
     */
    public static boolean writeFile(File file, String content, boolean append) throws IOException {
        if (file == null || content == null || !createFileOrExists(file)) {
            return false;
        }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file, append);
            fileWriter.write(content);
            fileWriter.flush();
            return true;
        } finally {
            closeIO(fileWriter);
        }
    }

    /**
     * 用字节数组写文件。
     *
     * @param file    目标文件
     * @param content 内容
     * @param append  是否追加
     * @return 是否成功
     * @throws IOException 写失败
     */
    public static boolean writeFile(File file, byte[] content, boolean append) throws IOException {
        if (file == null || content == null || !createFileOrExists(file)) {
            return false;
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, append);
            fileOutputStream.write(content);
            fileOutputStream.flush();
            return true;
        } finally {
            closeIO(fileOutputStream);
        }
    }
}
