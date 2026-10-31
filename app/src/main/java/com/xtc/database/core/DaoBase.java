package com.xtc.database.core;

import android.content.Context;
import android.text.TextUtils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.xtc.log.LogUtil;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 所有 DAO 的基类，包住 ormlite 的样板代码。
 *
 * <p>子类只需要在构造器里给出「实体类型 + 数据库名」，其余增删改查都由这里提供。
 * 表在第一次构造时创建（{@code CREATE TABLE IF NOT EXISTS}），
 * 所以调用方不需要关心建表时机。</p>
 *
 * <p>所有方法都把 {@link SQLException} 吞掉并打日志，返回"空"结果 ——
 * 这是原厂的行为，业务层不需要处理数据库异常。</p>
 *
 * @param <T> 实体类型
 */
public abstract class DaoBase<T> {

    private static final String TAG = "DaoBase";

    /** 实体类型。 */
    protected final Class<T> entityClass;

    /** 数据库名。 */
    protected final String databaseName;

    protected final Context context;

    private DatabaseHelper databaseHelper;

    private Dao<T, Integer> dao;

    protected DaoBase(Context context, Class<T> entityClass, String databaseName) {
        this.context = context == null ? null : context.getApplicationContext();
        this.entityClass = entityClass;
        this.databaseName = databaseName;
        open();
    }

    /** 打开（必要时创建）表并拿到 Dao。 */
    private void open() {
        try {
            this.databaseHelper = DatabaseHelper.open(this.context, this.databaseName);
            this.databaseHelper.registerTable(this.entityClass);
            TableUtils.createTableIfNotExists(
                    this.databaseHelper.getConnectionSource(), this.entityClass);
            //noinspection unchecked
            this.dao = this.databaseHelper.getDao(this.entityClass);
        } catch (SQLException e) {
            LogUtil.e(TAG, "open() failed, entity = " + this.entityClass + " ,ex = " + e);
        }
    }

    /** 底层 ormlite 的 Dao。 */
    public Dao<T, Integer> getDao() {
        return this.dao;
    }

    /** 表名（类名小写，和 {@code @DatabaseTable} 的约定一致）。 */
    public String getTableName() {
        return this.entityClass.getSimpleName().toLowerCase();
    }

    /** 插入一条。 */
    public int insert(T entity) {
        try {
            return this.dao.create(entity);
        } catch (SQLException e) {
            LogUtil.e(TAG, "insert() failed, ex = " + e);
            return 0;
        }
    }

    /** 批量插入（逐条，保留 id 回填）。 */
    public int insertAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        int count = 0;
        try {
            for (T entity : entities) {
                this.dao.create(entity);
                count++;
            }
        } catch (SQLException e) {
            LogUtil.e(TAG, "insertAll() failed, ex = " + e);
        }
        return count;
    }

    /** 插入或替换（按主键）。 */
    public int insertOrUpdate(T entity) {
        try {
            return this.dao.createOrUpdate(entity).getNumLinesChanged();
        } catch (SQLException e) {
            LogUtil.e(TAG, "insertOrUpdate() failed, ex = " + e);
            return 0;
        }
    }

    /** 按主键更新整条。 */
    public int update(T entity) {
        try {
            return this.dao.update(entity);
        } catch (SQLException e) {
            LogUtil.e(TAG, "update() failed, ex = " + e);
            return 0;
        }
    }

    /**
     * 按某个字段定位一行，再把 {@code entity} 里的非空字段合并进去写回。
     *
     * <p>这是原厂 {@code j.a(T, String, Object)} 的等价实现，语义容易被名字误导，这里写清楚：</p>
     * <ol>
     *   <li>先按 {@code column = value} 查出**已存在的那一行**（查不到就打日志返回 false）；</li>
     *   <li>把入参 {@code entity} 里**所有非 null 且不是主键**的字段，反射拷到查出来的那一行上；</li>
     *   <li>再按主键 update 这一行。</li>
     * </ol>
     *
     * <p>所以它是"局部更新"：{@code entity} 里没赋值的字段不会把库里已有的值清空。
     * 注意 {@code column}/{@code value} 只用来**定位**，不会把值写进 {@code column} 这一列。</p>
     *
     * @param entity 携带新值的实体（只用其中的非空字段）
     * @param column 定位用的列名
     * @param value  定位用的列值
     * @return 是否真的改动了行
     */
    public boolean updateByColumn(T entity, String column, Object value) {
        T existing = queryForFirst(column, value);
        if (existing == null) {
            LogUtil.e(TAG, "no find this data in database:" + entity);
            return false;
        }
        try {
            mergeNonNullFields(existing, entity);
            return this.dao.update(existing) > 0;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (SQLException e) {
            LogUtil.e(TAG, e);
            return false;
        }
    }

    /**
     * 把 {@code source} 的非空字段合并到 {@code target} 上（跳过主键字段）。
     *
     * <p>对应原厂私有方法 {@code j.a(T, Object)}。三个细节必须照抄：</p>
     * <ul>
     *   <li>主键字段名由 {@link #getIdFieldName(Object)} 从 {@code source} 的注解里算出来，命中就跳过；</li>
     *   <li>值为 null 的字段跳过（这正是"局部更新"的语义来源）；</li>
     *   <li>目标类上没有同名字段时，原厂那句 <code>declaredField != null</code> 判断其实是死代码 ——
     *       因为 {@link Class#getDeclaredField(String)} 找不到会直接抛 {@link NoSuchFieldException}，
     *       由调用方统一捕获。这里不做"修正"，保持抛出。</li>
     * </ul>
     */
    private void mergeNonNullFields(T target, Object source)
            throws IllegalAccessException, NoSuchFieldException {
        Field[] sourceFields = source.getClass().getDeclaredFields();
        Class<?> targetClass = target.getClass();
        String idFieldName = getIdFieldName(source);
        for (Field sourceField : sourceFields) {
            sourceField.setAccessible(true);
            if (idFieldName.equals(sourceField.getName())) {
                continue;
            }
            Object newValue = sourceField.get(source);
            if (newValue == null) {
                continue;
            }
            Field targetField = targetClass.getDeclaredField(sourceField.getName());
            targetField.setAccessible(true);
            targetField.set(target, newValue);
        }
    }

    /**
     * 找出实体的主键字段名。
     *
     * <p>对应原厂私有方法 {@code j.f(Object)}：遍历 {@code @DatabaseField} 注解，
     * 第一个满足 {@code id()} / {@code generatedId()} / 非空 {@code generatedIdSequence()}
     * 的字段就是主键；一个都没有则返回空串。</p>
     */
    private String getIdFieldName(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            DatabaseField annotation = field.getAnnotation(DatabaseField.class);
            if (annotation == null) {
                continue;
            }
            if (annotation.id()
                    || annotation.generatedId()
                    || !TextUtils.isEmpty(annotation.generatedIdSequence())) {
                return field.getName();
            }
        }
        return "";
    }

    /** 把 {@code column = whereValue} 的行里 {@code targetColumn} 改成 {@code value}。 */
    public int updateColumnBy(String targetColumn, Object value, String whereColumn,
                              Object whereValue) {
        try {
            UpdateBuilder<T, Integer> updateBuilder = this.dao.updateBuilder();
            updateBuilder.updateColumnValue(targetColumn, value);
            updateBuilder.where().eq(whereColumn, whereValue);
            return updateBuilder.update();
        } catch (SQLException e) {
            LogUtil.e(TAG, "updateColumnBy() failed, ex = " + e);
            return 0;
        }
    }

    /** 全表。 */
    public List<T> queryAll() {
        try {
            return this.dao.queryForAll();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryAll() failed, ex = " + e);
            return new ArrayList<>();
        }
    }

    /** 按单个字段查。 */
    public List<T> queryByColumn(String column, Object value) {
        try {
            return this.dao.queryForEq(column, value);
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryByColumn() failed, ex = " + e);
            return new ArrayList<>();
        }
    }

    /**
     * 按单个字段查，可指定排序。
     *
     * @param orderByColumn 排序列
     * @param whereColumn   条件列
     * @param value         条件值
     * @param ascending     是否升序
     */
    public List<T> query(String orderByColumn, String whereColumn, Object value,
                         boolean ascending) {
        try {
            QueryBuilder<T, Integer> queryBuilder = this.dao.queryBuilder();
            queryBuilder.orderBy(orderByColumn, ascending);
            if (whereColumn != null) {
                queryBuilder.where().eq(whereColumn, value);
            }
            return queryBuilder.query();
        } catch (SQLException e) {
            LogUtil.e(TAG, "query() failed, ex = " + e);
            return new ArrayList<>();
        }
    }

    /**
     * 按单个字段取第一条。
     *
     * <p>对应原厂 {@code j.e(String, Object)}：{@code where().eq(column, value)} 再
     * {@code queryForFirst()}，异常时打日志返回 null。</p>
     */
    public T queryForFirst(String column, Object value) {
        try {
            QueryBuilder<T, Integer> queryBuilder = this.dao.queryBuilder();
            queryBuilder.where().eq(column, value);
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /** 多个等值条件取第一条。 */
    public T queryForFirst(Map<String, Object> conditions) {
        try {
            QueryBuilder<T, Integer> queryBuilder = this.dao.queryBuilder();
            if (conditions != null && !conditions.isEmpty()) {
                Where<T, Integer> where = queryBuilder.where();
                int index = 0;
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    where.eq(entry.getKey(), entry.getValue());
                    index++;
                    if (index < conditions.size()) {
                        where.and();
                    }
                }
                where.prepare();
            }
            queryBuilder.limit(1L);
            List<T> result = queryBuilder.query();
            return result.isEmpty() ? null : result.get(0);
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryForFirst() failed, ex = " + e);
            return null;
        }
    }

    /** 按主键查。 */
    public T queryById(Integer id) {
        try {
            return this.dao.queryForId(id);
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryById() failed, ex = " + e);
            return null;
        }
    }

    /** 行数。 */
    public long count() {
        try {
            return this.dao.countOf();
        } catch (SQLException e) {
            LogUtil.e(TAG, "count() failed, ex = " + e);
            return 0L;
        }
    }

    /** 按单个字段删。 */
    public boolean deleteByColumn(String column, Object value) {
        try {
            DeleteBuilder<T, Integer> deleteBuilder = this.dao.deleteBuilder();
            deleteBuilder.where().eq(column, value);
            return deleteBuilder.delete() > 0;
        } catch (SQLException e) {
            LogUtil.e(TAG, "deleteByColumn() failed, ex = " + e);
            return false;
        }
    }

    /** 按主键删。 */
    public int deleteById(Integer id) {
        try {
            return this.dao.deleteById(id);
        } catch (SQLException e) {
            LogUtil.e(TAG, "deleteById() failed, ex = " + e);
            return 0;
        }
    }

    /** 删一条。 */
    public int delete(T entity) {
        try {
            return this.dao.delete(entity);
        } catch (SQLException e) {
            LogUtil.e(TAG, "delete() failed, ex = " + e);
            return 0;
        }
    }

    /** 清空表。 */
    public int clearTable() {
        try {
            return TableUtils.clearTable(getDao().getConnectionSource(), this.entityClass);
        } catch (SQLException e) {
            LogUtil.e(TAG, "clearTable() failed, ex = " + e);
            return 0;
        }
    }

    /**
     * 先数行数，非空才清表。
     *
     * <p>原厂的 {@code j.b()}：空表时直接返回 0，避免无意义地走一次 DDL 级别操作。</p>
     */
    public boolean clearTableWithCountCheck() {
        long rowCount = count();
        if (rowCount <= 0) {
            LogUtil.d(TAG, "clearTableWithCountCheck() table is empty, skip");
            return false;
        }
        LogUtil.d(TAG, "clearTableWithCountCheck() clear " + rowCount + " rows");
        return clearTable() > 0;
    }
}
