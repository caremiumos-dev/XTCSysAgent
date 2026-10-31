package com.xtc.database.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.j256.ormlite.android.AndroidDatabaseConnection;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.xtc.log.LogUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ORMLite 的 {@link OrmLiteSqliteOpenHelper} 封装。
 *
 * <p>做了三件事：</p>
 * <ol>
 *   <li>把每张表的升级/创建动作委托给 {@link TableHandler}，实现"按表增量升级"；</li>
 *   <li>缓存 {@link Dao}，避免每次 {@code getDao} 都走一遍反射；</li>
 *   <li>覆写 {@code onDowngrade}（系统默认是删库重建，这里改成逐表处理）。</li>
 * </ol>
 */
public class OrmLiteDatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = "OrmLiteDatabaseHelper";

    /** 已登记的表处理器。 */
    List<TableHandler> tableHandlers;

    /** Dao 缓存，键是实体类的简单类名。 */
    Map<String, Dao> daoCache;

    public OrmLiteDatabaseHelper(Context context, String databaseName,
                                 SQLiteDatabase.CursorFactory cursorFactory, int version) {
        super(context, databaseName, cursorFactory, version);
        this.daoCache = new HashMap<>();
    }

    /** 清空所有已登记表的数据。 */
    public void clearAllTable() {
        try {
            Iterator<TableHandler> iterator = this.tableHandlers.iterator();
            while (iterator.hasNext()) {
                iterator.next().clearTable(this.connectionSource);
            }
        } catch (SQLException e) {
            LogUtil.e("clear all table fail", e);
        }
    }

    /** 降级：逐表重建。 */
    public void onDowngradeTables(ConnectionSource connectionSource, int oldVersion, int newVersion) {
        LogUtil.i(TAG, "数据库降级了 oldVersion = " + oldVersion + " newVersion = " + newVersion);
        try {
            Iterator<TableHandler> iterator = this.tableHandlers.iterator();
            while (iterator.hasNext()) {
                iterator.next().onDowngrade(connectionSource, oldVersion, newVersion);
            }
        } catch (SQLException e) {
            LogUtil.e("database downgrade fail", e);
        }
    }

    /** 表是否还没登记过（按表名判重）。 */
    public boolean isNewTable(TableHandler handler) {
        if (this.tableHandlers == null || handler == null) {
            return false;
        }
        String tableName = handler.getTableName();
        Iterator<TableHandler> iterator = this.tableHandlers.iterator();
        while (iterator.hasNext()) {
            if (tableName.equals(iterator.next().getTableName())) {
                return false;
            }
        }
        return true;
    }

    /** 登记一张表，重复登记会被忽略。 */
    public <T> void registerTable(Class<T> entityClass) {
        if (this.tableHandlers == null) {
            this.tableHandlers = new ArrayList<>();
        }
        TableHandler<T> handler = new TableHandler<>(entityClass);
        if (isNewTable(handler)) {
            this.tableHandlers.add(handler);
        }
    }

    /**
     * 关闭数据库并清空 Dao 缓存。
     *
     * <p>注意原实现在循环体里只做了 {@code getValue()}（没有使用），
     * 实际效果等价于 {@code clear()}；这里直接 clear，语义相同。</p>
     */
    @Override
    public void close() {
        super.close();
        synchronized (this) {
            this.daoCache.clear();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized Dao getDao(Class entityClass) {
        Dao dao;
        String simpleName = entityClass.getSimpleName();
        if (this.daoCache.containsKey(simpleName)) {
            dao = this.daoCache.get(simpleName);
        } else {
            try {
                dao = super.getDao(entityClass);
                this.daoCache.put(simpleName, dao);
            } catch (SQLException e) {
                LogUtil.e("database operate fail", e);
                dao = null;
            }
        }
        return dao;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            Iterator<TableHandler> iterator = this.tableHandlers.iterator();
            while (iterator.hasNext()) {
                iterator.next().createTable(connectionSource);
            }
        } catch (SQLException e) {
            LogUtil.e("database create fail", e);
        }
    }

    /**
     * 降级处理。
     *
     * <p>照搬 ORMLite 官方 {@code OrmLiteSqliteOpenHelper} 的写法：先把当前
     * {@link SQLiteDatabase} 包成 {@link AndroidDatabaseConnection} 并登记为
     * "特殊连接"，让升级逻辑能在同一个事务里跑，最后再清理。</p>
     */
    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        ConnectionSource connectionSource = getConnectionSource();
        // ormlite 5.x 的 getSpecialConnection 需要一个连接标识参数，传 null 表示"默认连接"。
        DatabaseConnection specialConnection = connectionSource.getSpecialConnection(null);
        boolean clearSpecial;
        DatabaseConnection connection;
        if (specialConnection == null) {
            AndroidDatabaseConnection androidConnection =
                    new AndroidDatabaseConnection(database, true, this.cancelQueriesEnabled);
            try {
                connectionSource.saveSpecialConnection(androidConnection);
                connection = androidConnection;
                clearSpecial = true;
            } catch (SQLException e) {
                throw new IllegalStateException("Could not save special connection", e);
            }
        } else {
            connection = specialConnection;
            clearSpecial = false;
        }
        try {
            onDowngradeTables(connectionSource, oldVersion, newVersion);
        } finally {
            if (clearSpecial) {
                connectionSource.clearSpecialConnection(connection);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        LogUtil.i(TAG, "数据库升级了 oldVersion = " + oldVersion + " newVersion = " + newVersion);
        try {
            Iterator<TableHandler> iterator = this.tableHandlers.iterator();
            while (iterator.hasNext()) {
                iterator.next().onUpgrade(database, connectionSource);
            }
        } catch (SQLException e) {
            LogUtil.e("database upgrade fail", e);
        }
    }
}
