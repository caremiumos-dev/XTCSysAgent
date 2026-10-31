package com.xtc.utils.system;

import android.content.Context;
import java.io.File;

/**
 * 应用私有目录的清理工具。
 *
 * <p>原厂类名为单字母 {@code d}，只有一层薄封装：把所有删除动作都委托给包内的
 * {@link FileUtils}（原 {@code h}）和 {@link Context#deleteDatabase(String)}。
 * 返回值仅表示"删除动作是否全部成功"，失败既不抛异常也不重试。</p>
 *
 * <p>注意：与 {@code com.xtc.utils.storage.CleanUtils} 不是同一个类 —— 那个是通用存储库，
 * 本类只服务 system 包内"清数据"这条链路，方法集合与原件完全一致。</p>
 */
public class CleanUtils {

    private CleanUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /** 清空内部缓存目录 {@code /data/data/<pkg>/cache} 下的内容，保留目录本身。 */
    public static boolean cleanInternalCache(Context context) {
        return FileUtils.deleteAllInDir(context.getCacheDir());
    }

    /** 删除指定名字的数据库文件，等价于 {@link Context#deleteDatabase(String)}。 */
    public static boolean cleanDatabaseByName(Context context, String dbName) {
        return context.deleteDatabase(dbName);
    }

    /** 清空任意目录下的内容，保留目录本身。 */
    public static boolean cleanDirectory(File dir) {
        return FileUtils.deleteAllInDir(dir);
    }

    /** 按路径清空任意目录下的内容，保留目录本身。 */
    public static boolean cleanDirectory(String dirPath) {
        return FileUtils.deleteAllInDir(dirPath);
    }

    /** 清空内部文件目录 {@code /data/data/<pkg>/files} 下的内容。 */
    public static boolean cleanFiles(Context context) {
        return FileUtils.deleteAllInDir(context.getFilesDir());
    }

    /** 清空数据库目录 {@code /data/data/<pkg>/databases}。路径拼法与原件一致。 */
    public static boolean cleanDatabases(Context context) {
        return FileUtils.deleteAllInDir(context.getFilesDir().getParent() + File.separator + "databases");
    }

    /** 清空偏好设置目录 {@code /data/data/<pkg>/shared_prefs}。路径拼法与原件一致。 */
    public static boolean cleanSharedPreference(Context context) {
        return FileUtils.deleteAllInDir(context.getFilesDir().getParent() + File.separator + "shared_prefs");
    }

    /**
     * 清空外部缓存目录。
     *
     * <p>要求外部存储处于挂载状态才动手；未挂载时直接返回 false，不做任何删除 ——
     * 原件用的是"先判挂载再删"的短路与（{@code &&}），这里保持一致。</p>
     */
    public static boolean cleanExternalCache(Context context) {
        return FileUtils.isSDCardMounted() && FileUtils.deleteAllInDir(context.getExternalCacheDir());
    }
}
