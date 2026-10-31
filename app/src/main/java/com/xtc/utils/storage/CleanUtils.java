package com.xtc.utils.storage;

import android.content.Context;
import java.io.File;

/**
 * 缓存/数据库/偏好设置的清理工具。
 *
 * <p>原厂把「清理」拆成按分区的一组静态方法：内部缓存、内部文件、数据库目录、
 * shared_prefs 目录、外部缓存。所有方法都直接委托给 {@link FileUtils} 的目录删除能力，
 * 返回值仅表示"删除动作是否全部成功"，失败不抛异常，由调用方自行忽略。</p>
 *
 * <p>本类是工具类，禁止实例化，构造即抛异常，保持与原厂一致。</p>
 */
public class CleanUtils {

    private CleanUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /** 清空内部缓存目录 /data/data/&lt;pkg&gt;/cache 下的内容（保留目录本身）。 */
    public static boolean cleanInternalCache(Context context) {
        return FileUtils.deleteAllInDir(context.getCacheDir());
    }

    /** 删除指定名字的数据库文件，等价于 {@link Context#deleteDatabase(String)}。 */
    public static boolean cleanDatabaseByName(Context context, String dbName) {
        return context.deleteDatabase(dbName);
    }

    /** 清空任意目录下的内容（保留目录本身）。 */
    public static boolean cleanDirectory(File dir) {
        return FileUtils.deleteAllInDir(dir);
    }

    /** 按路径清空任意目录下的内容（保留目录本身）。 */
    public static boolean cleanDirectory(String dirPath) {
        return FileUtils.deleteAllInDir(dirPath);
    }

    /** 清空内部文件目录 /data/data/&lt;pkg&gt;/files 下的内容。 */
    public static boolean cleanFiles(Context context) {
        return FileUtils.deleteAllInDir(context.getFilesDir());
    }

    /** 清空数据库目录 /data/data/&lt;pkg&gt;/databases 下的内容。 */
    public static boolean cleanDatabases(Context context) {
        return FileUtils.deleteAllInDir(context.getFilesDir().getParent() + File.separator + "databases");
    }

    /** 清空偏好设置目录 /data/data/&lt;pkg&gt;/shared_prefs 下的内容。 */
    public static boolean cleanSharedPreference(Context context) {
        return FileUtils.deleteAllInDir(context.getFilesDir().getParent() + File.separator + "shared_prefs");
    }

    /**
     * 清空外部缓存目录。
     *
     * <p>要求外部存储处于挂载可用状态才动手；SD 卡不可用时直接返回 false，
     * 不做任何删除——原厂如此处理，避免在未挂载时误判。</p>
     */
    public static boolean cleanExternalCache(Context context) {
        return SdCardUtils.isSdCardAvailable() && FileUtils.deleteAllInDir(context.getExternalCacheDir());
    }
}
