package com.xtc.database.core;

import android.content.Context;
import android.text.TextUtils;
import com.j256.ormlite.logger.LocalLog;
import com.xtc.log.LogUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库 helper 的对外入口，按库名缓存实例。
 *
 * <p>版本号优先取宿主 APK 的 {@code BuildConfig.VERSION_CODE}；DEBUG 包允许
 * 调用方显式指定版本号覆盖。取不到时兜底为 1。</p>
 */
public class DatabaseHelper extends OrmLiteDatabaseHelper {

    private static final String TAG = "DatabaseHelper";

    /** 按库名缓存的元信息。 */
    private static volatile Map<String, DatabaseInfo> sDatabaseInfoMap = new HashMap<>();

    private DatabaseHelper(Context context, String databaseName, int version) {
        super(context, databaseName, null, version);
        // ORMLite 的日志会刷屏，强制降到 FATAL。
        System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, "FATAL");
    }

    /** 用默认版本号打开数据库。 */
    public static DatabaseHelper open(Context context, String databaseName) {
        return open(context, databaseName, -1);
    }

    /**
     * 打开数据库。
     *
     * @param version 显式版本号；-1 表示自动读取宿主 BuildConfig
     */
    public static synchronized DatabaseHelper open(Context context, String databaseName, int version) {
        if (TextUtils.isEmpty(databaseName)) {
            LogUtil.d(TAG, "database name is null");
            throw new IllegalStateException("database name is null");
        }
        DatabaseInfo info = sDatabaseInfoMap.get(databaseName);
        if (info == null || info.getHelper() == null) {
            LogUtil.d(TAG, "current instance is  null now,init here");
            return create(context, databaseName, version);
        }
        return info.getHelper();
    }

    private static DatabaseHelper create(Context context, String databaseName, int version) {
        LogUtil.i(TAG, "initDatabase dbName = " + databaseName + ",version " + version
                + ",ThreadId = " + Thread.currentThread().getName());
        DatabaseHelper helper;
        synchronized (DatabaseHelper.class) {
            DatabaseInfo info = sDatabaseInfoMap.get(databaseName);
            if (info == null) {
                info = new DatabaseInfo();
            }
            info.setDatabaseName(databaseName);

            int versionCode;
            boolean debuggable;
            try {
                Class<?> buildConfig = Class.forName(context.getPackageName() + ".BuildConfig");
                Field versionField = buildConfig.getDeclaredField("VERSION_CODE");
                versionField.setAccessible(true);
                versionCode = versionField.getInt(null);
                try {
                    Field debugField = buildConfig.getDeclaredField("DEBUG");
                    debugField.setAccessible(true);
                    debuggable = debugField.getBoolean(null);
                } catch (Exception e) {
                    LogUtil.e(TAG, e);
                    debuggable = false;
                }
            } catch (Exception e) {
                LogUtil.e(TAG, e);
                versionCode = -1;
                debuggable = false;
            }

            // DEBUG 包允许用调用方给的版本号覆盖。
            if (debuggable && version != -1) {
                versionCode = version;
            }
            if (versionCode == -1) {
                LogUtil.d(TAG, "something went wrong,need set version = 1");
                versionCode = 1;
            }
            LogUtil.i(TAG, "initDatabase dbVersion " + versionCode);
            info.setDatabaseVersion(versionCode);

            helper = new DatabaseHelper(context, databaseName, versionCode);
            info.setHelper(helper);
            sDatabaseInfoMap.put(databaseName, info);
        }
        return helper;
    }

    /** 登记一张表。 */
    public <T> void registerTable(Class<T> entityClass) {
        super.registerTable(entityClass);
    }

    /** 关闭指定库。 */
    public synchronized void close(String databaseName) {
        DatabaseInfo info = sDatabaseInfoMap.get(databaseName);
        if (info == null || info.getHelper() == null) {
            LogUtil.i(TAG, "current database helper is null");
            return;
        }
        info.getHelper().close();
    }
}
