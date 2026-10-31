package com.xtc.database.core;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.xtc.log.LogUtil;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * 单张表的建表/升级/降级/清空逻辑。
 *
 * <p>升级策略是"按列增量"：把老表改名成 {@code _temp}，按新结构重建，
 * 再把两表共有列的数据搬过来，最后删掉临时表。这样老数据不会丢。</p>
 *
 * @param <T> ORMLite 实体类型
 */
public class TableHandler<T> {

    private static final String TAG = "DatabaseHandler";

    private Class<T> entityClass;

    /** 表名。 */
    private final String tableName;

    public TableHandler(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = TableReflectUtils.getTableName(entityClass);
    }

    /** 拼出"新表里有、可以搬过来"的列清单。 */
    private String buildCopyColumns(List<String> oldColumns, List<String> newColumns) {
        StringBuilder builder = new StringBuilder("");
        if (oldColumns == null || newColumns == null) {
            return builder.toString();
        }
        int count = 0;
        Iterator<String> iterator = oldColumns.iterator();
        while (iterator.hasNext()) {
            String column = iterator.next();
            if (CollectionUtils.contains(column, newColumns)) {
                continue;
            }
            if (count == 0) {
                builder.append("`").append(column).append("`");
            } else {
                builder.append(", ").append("`").append(column).append("`");
            }
            count++;
        }
        return builder.toString();
    }

    /** 改名 + 重建 + 搬数据 + 删临时表，全程一个事务。 */
    private void upgradeByCopy(SQLiteDatabase database, ConnectionSource connectionSource,
                               String columns) throws SQLException {
        if (TextUtils.isEmpty(columns)) {
            LogUtil.d(TAG, "upgradeByCopy columns is null");
            return;
        }
        database.beginTransaction();
        String tempTable = this.tableName + "_temp";
        try {
            try {
                database.execSQL("ALTER TABLE " + this.tableName + " RENAME TO " + tempTable);
                try {
                    database.execSQL(
                            TableUtils.getCreateTableStatements(connectionSource, this.entityClass).get(0));
                } catch (Exception e) {
                    LogUtil.e(TAG, e);
                    TableUtils.createTable(connectionSource, this.entityClass);
                }
                database.execSQL("INSERT INTO " + this.tableName + " (" + columns
                        + ")  SELECT " + columns + " FROM " + tempTable);
                database.execSQL("DROP TABLE IF EXISTS " + tempTable);
                database.setTransactionSuccessful();
                database.endTransaction();
            } catch (Exception e) {
                LogUtil.e(TAG, e);
                throw new SQLException("upgrade database table struct fail");
            }
        } catch (Throwable throwable) {
            database.endTransaction();
            throw throwable;
        }
    }

    private void upgradeColumns(SQLiteDatabase database, ConnectionSource connectionSource,
                                List<ColumnStruct> oldColumns,
                                List<ColumnStruct> newColumns) throws SQLException {
        if (TableReflectUtils.hasChangedColumns(oldColumns, newColumns)) {
            LogUtil.d(TAG, "数据表已有字段的描述改变");
            recreate(connectionSource);
            return;
        }
        List<String> oldNames = TableReflectUtils.getColumnNames(oldColumns);
        List<String> newNames = TableReflectUtils.getColumnNames(newColumns);
        if (oldNames.equals(newNames)) {
            LogUtil.i(TAG, "表没有发生变化,不需要更新数据表");
        } else {
            LogUtil.d(TAG, "表发生了变化 tableName =" + this.tableName
                    + ",oldColumns = " + oldNames + ",newColumns =" + newNames);
            upgradeByCopy(database, connectionSource,
                    buildCopyColumns(oldNames, TableReflectUtils.getDifference(oldNames, newNames)));
        }
    }

    /** 删了再建。 */
    private void recreate(ConnectionSource connectionSource) throws SQLException {
        dropTable(connectionSource);
        createTable(connectionSource);
    }

    public String getTableName() {
        return this.tableName;
    }

    /** 升级单表：老库有、新结构也有 → 增量迁移；老库没有 → 建表；新结构没有 → 删表。 */
    protected void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource)
            throws SQLException {
        List<ColumnStruct> oldColumns =
                TableReflectUtils.getColumns(database, this.tableName);
        List<ColumnStruct> newColumns =
                TableReflectUtils.getColumns(connectionSource, this.entityClass);
        if (oldColumns.isEmpty() && newColumns.isEmpty()) {
            LogUtil.d(TAG, "数据表结构都为空！不是合法的数据库bean！！！");
            return;
        }
        if (oldColumns.isEmpty()) {
            LogUtil.d(TAG, "新增表");
            createTable(connectionSource);
        } else if (!newColumns.isEmpty()) {
            upgradeColumns(database, connectionSource, oldColumns, newColumns);
        } else {
            LogUtil.e(TAG, "删除表");
            dropTable(connectionSource);
        }
    }

    /** 带兜底的升级：出错就删表重建。 */
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) throws SQLException {
        try {
            onUpgrade(database, connectionSource);
        } catch (SQLException e) {
            LogUtil.e(TAG, e);
            recreate(connectionSource);
        }
    }

    /** 清空表数据。 */
    public void clearTable(ConnectionSource connectionSource) throws SQLException {
        TableUtils.clearTable(connectionSource, this.entityClass);
    }

    /** 降级：直接删表重建。 */
    public void onDowngrade(ConnectionSource connectionSource, int oldVersion, int newVersion)
            throws SQLException {
        recreate(connectionSource);
    }

    /** 建表。 */
    public void createTable(ConnectionSource connectionSource) throws SQLException {
        TableUtils.createTable(connectionSource, this.entityClass);
    }

    /** 删表。 */
    public void dropTable(ConnectionSource connectionSource) throws SQLException {
        TableUtils.dropTable(connectionSource, this.entityClass, true);
    }
}
