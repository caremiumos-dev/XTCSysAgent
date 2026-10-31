package com.xtc.database.core;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import com.xtc.log.LogUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 表结构与列信息的解析、比较工具。
 *
 * <p>核心思路：不依赖 ORMLite 的私有 API，而是把
 * {@code CREATE TABLE} 语句原文解析成 {@link ColumnStruct} 列表，
 * 再比较"老表结构"和"新结构"的差异，从而生成增量升级语句。</p>
 */
public class TableReflectUtils {

    private static final String TAG = "DatabaseUtil";

    /** 列定义之间的分隔符（{@code CREATE TABLE} 语句里用的就是空格）。 */
    private static final String COLUMN_SEPARATOR = " ";

    private TableReflectUtils() {
    }

    /** 按列名查列描述。 */
    public static ColumnStruct findColumn(String columnName, List<ColumnStruct> columns) {
        if (columns == null || columnName == null) {
            return null;
        }
        for (ColumnStruct column : columns) {
            if (column != null && columnName.equals(column.getColumnName())) {
                return column;
            }
        }
        return null;
    }

    /**
     * 取实体类对应的表名：优先 {@code @DatabaseTable(tableName=...)}，取不到则退化成"类名小写"。
     *
     * <p>原厂还尝试过 JPA 的 {@code @Entity(name=...)}（走 ormlite 的
     * {@code JavaxPersistence.getEntityName}）。ormlite-core 5.1 已经剥离了 JPA 支持，
     * 该入口不存在了；本工程所有实体都用 {@code @DatabaseTable} 标注，
     * 因此这条退化路径在实践中不会被走到，行为不变。</p>
     */
    public static <T> String getTableName(Class<T> entityClass) {
        DatabaseTable annotation = entityClass.getAnnotation(DatabaseTable.class);
        if (annotation != null && annotation.tableName() != null && annotation.tableName().length() > 0) {
            return annotation.tableName();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    /** 从已有数据库里读出某张表的列结构。 */
    public static List<ColumnStruct> getColumns(SQLiteDatabase database, String tableName) {
        List<ColumnStruct> columns = new ArrayList<>();
        try {
            Cursor cursor = database.rawQuery(
                    "select * from sqlite_master where type = ? AND name = ?",
                    new String[]{"table", tableName});
            if (cursor != null) {
                cursor.moveToFirst();
                int sqlIndex = cursor.getColumnIndex("sql");
                if (-1 == sqlIndex || cursor.getCount() <= 0) {
                    LogUtil.i(TAG, "不存在旧表");
                } else {
                    columns = parseColumns(cursor.getString(sqlIndex));
                }
                cursor.close();
            } else {
                LogUtil.i(TAG, "数据库操作失败");
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return columns;
    }

    /** 从 ORMLite 生成出来的建表语句里读出列结构。 */
    public static <T> List<ColumnStruct> getColumns(ConnectionSource connectionSource, Class<T> entityClass) {
        try {
            return parseColumns(TableUtils.getCreateTableStatements(connectionSource, entityClass).get(0));
        } catch (SQLException e) {
            LogUtil.e(TAG, e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析 {@code CREATE TABLE xxx (...)} 语句里的列定义。
     *
     * <p>列定义有三种形态：</p>
     * <ul>
     *   <li>{@code `col` TYPE ...} —— 普通列；</li>
     *   <li>{@code `col` ... , `col2` ...} —— 联合唯一约束；</li>
     *   <li>其它 —— 跳过。</li>
     * </ul>
     */
    public static List<ColumnStruct> parseColumns(String createTableSql) {
        List<ColumnStruct> columns = new ArrayList<>();
        String body = createTableSql.substring(
                createTableSql.indexOf("(") + 1, createTableSql.length() - 1);
        for (String rawColumn : body.split(", ")) {
            String column = rawColumn;
            if (column.contains("(") || column.contains(")")) {
                column = column.replace("(", "").replace(")", "");
            }
            String trimmed = column.trim();
            if (trimmed.startsWith("`")) {
                String[] parts = trimmed.split("` ");
                columns.add(new ColumnStruct(parts[0].replace("`", ""), parts[1]));
            } else if (trimmed.contains(",")) {
                String[] parts = trimmed.split(" `")[1].replace("`", "").split(",");
                for (String part : parts) {
                    appendColumn(columns, part, "UniqueCombo");
                }
            } else {
                String[] parts = trimmed.split(" `");
                appendColumn(columns, parts[1].replace("`", ""), parts[0]);
            }
        }
        return columns;
    }

    /** 取出所有列名，顺序与入参一致。 */
    public static List<String> getColumnNames(List<ColumnStruct> columns) {
        List<String> names = new ArrayList<>();
        if (columns == null) {
            return names;
        }
        Iterator<ColumnStruct> iterator = columns.iterator();
        while (iterator.hasNext()) {
            names.add(iterator.next().getColumnName());
        }
        return names;
    }

    /** 追加（或合并）一条列定义。已存在同名列时把约束拼上去。 */
    private static void appendColumn(List<ColumnStruct> columns, String columnName, String columnLimit) {
        if (columns == null || columns.isEmpty()) {
            LogUtil.e(TAG, "list is null.");
            return;
        }
        if (TextUtils.isEmpty(columnName) || TextUtils.isEmpty(columnLimit)) {
            LogUtil.e(TAG, "columnName is null or limit is null.");
            return;
        }
        int size = columns.size();
        for (int i = 0; i < size; i++) {
            ColumnStruct column = columns.get(i);
            if (columnName.equals(column.getColumnName())) {
                column.setColumnLimit(column.getColumnLimit() + COLUMN_SEPARATOR + columnLimit);
                return;
            }
        }
        columns.add(new ColumnStruct(columnName, columnLimit));
    }

    /** 两列是否"同名但定义不同"。 */
    private static boolean isColumnDifferent(ColumnStruct oldColumn, ColumnStruct newColumn) {
        if (oldColumn == null || newColumn == null) {
            return false;
        }
        String oldName = oldColumn.getColumnName();
        String oldLimit = oldColumn.getColumnLimit();
        if (oldName == null || !oldName.equals(newColumn.getColumnName())) {
            return false;
        }
        String newLimit = newColumn.getColumnLimit();
        if (oldLimit == null && newLimit == null) {
            return false;
        }
        return oldLimit == null || newLimit == null || !oldLimit.equals(newLimit);
    }

    /** {@code columns} 里是否存在与 {@code column} 同名但定义不同的列。 */
    private static boolean hasDifferentColumn(List<ColumnStruct> columns, ColumnStruct column) {
        if (column == null || TextUtils.isEmpty(column.getColumnName())) {
            return false;
        }
        Iterator<ColumnStruct> iterator = columns.iterator();
        while (iterator.hasNext()) {
            if (isColumnDifferent(column, iterator.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 两组列结构之间是否存在"同名列定义不同"。
     *
     * <p>为真表示表结构发生了不兼容变化，必须删表重建，不能做增量迁移。</p>
     */
    public static boolean hasChangedColumns(List<ColumnStruct> oldColumns, List<ColumnStruct> newColumns) {
        if (oldColumns == null || newColumns == null) {
            return false;
        }
        Iterator<ColumnStruct> iterator = oldColumns.iterator();
        while (iterator.hasNext()) {
            if (hasDifferentColumn(newColumns, iterator.next())) {
                return true;
            }
        }
        return false;
    }

    /** 求差集：{@code oldColumns} 里有、{@code newColumns} 里没有的列名。 */
    public static List<String> getDifference(List<String> oldColumns, List<String> newColumns) {
        return removeAll(oldColumns, newColumns);
    }

    private static List<String> removeAll(List<String> source, List<String> toRemove) {
        List<String> result = new ArrayList<>();
        if (source == null || toRemove == null) {
            return result;
        }
        for (String item : source) {
            boolean found = false;
            Iterator<String> iterator = toRemove.iterator();
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                String candidate = iterator.next();
                if (candidate != null && candidate.equals(item)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(item);
            }
        }
        return result;
    }
}
