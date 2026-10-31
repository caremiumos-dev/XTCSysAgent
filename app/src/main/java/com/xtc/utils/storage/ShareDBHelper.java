package com.xtc.utils.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.xtc.log.LogUtil;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 直接持有一个 SQLiteDatabase 连接的小型 DAO 助手。
 *
 * <p>用于跨进程/跨包读取别的 APK 的数据库（守护进程读手表应用的表），
 * 所以它不走 SQLiteOpenHelper，而是拿到 db 文件的绝对路径后用
 * {@link SQLiteDatabase#openDatabase(String, android.database.sqlite.SQLiteDatabase.CursorFactory, int)}
 * 只读打开。</p>
 *
 * <p>三种用法：</p>
 * <ul>
 *   <li>{@link #open(String)} / {@link #open(String, String)}：按路径或"包名 + 库名"打开；</li>
 *   <li>{@link #query(String, CursorCallback)}：查一批数据，回调里自己取列，回调结束即关连接；</li>
 *   <li>{@link #parseBean(Cursor, Object)}：把当前行按字段名反射塞进一个 JavaBean。</li>
 * </ul>
 *
 * <p>注意：{@code insert/update/delete/execSql} 四个方法体完全相同，都只是把整条 SQL
 * 丢给 execSQL 并关闭连接；原厂就是这样的一组薄封装，命名按传统 CRUD 语义推断。</p>
 */
public class ShareDBHelper {

    /** 日志 tag，与原厂保持一致。 */
    private static final String TAG = "ShareDBHelper";

    /** 当前持有的数据库连接；打开失败时为 null。 */
    private SQLiteDatabase mDB;

    /**
     * 游标回调。原厂接口名就叫 callBack，这里保留方法名不动（只改接口名）。
     */
    public interface CursorCallback {
        void callBack(Cursor cursor);
    }

    private ShareDBHelper(String dbPath) {
        this.mDB = null;
        this.mDB = openDatabase(dbPath);
    }

    private ShareDBHelper(String packageName, String dbName) {
        this.mDB = null;
        this.mDB = openDatabase(String.format("/data/data/%s/databases/%s", packageName, dbName));
    }

    /** 按数据库文件绝对路径打开。 */
    public static synchronized ShareDBHelper open(String dbPath) {
        return new ShareDBHelper(dbPath);
    }

    /** 按"包名 + 数据库名"拼出 /data/data/&lt;pkg&gt;/databases/&lt;db&gt; 再打开。 */
    public static synchronized ShareDBHelper open(String packageName, String dbName) {
        return new ShareDBHelper(packageName, dbName);
    }

    /**
     * 用某个包的 Context 打开它自己的数据库。
     *
     * <p>配合 {@code createPackageContext} 使用：拿到的 Context 的
     * {@code getDatabasePath()} 指向目标包的数据目录，所以这里直接按绝对路径打开。</p>
     */
    public static synchronized ShareDBHelper open(Context context, String dbName) {
        return new ShareDBHelper(context.getDatabasePath(dbName).getAbsolutePath());
    }

    /** 统一的时间格式（与数据库里存的字符串格式一致）。 */
    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (date != null) {
            return formatter.format(date);
        }
        return null;
    }

    /**
     * 按字段名找对应的 getter（形如 getXxx，且不以 set 开头）。
     *
     * <p>比较方式是"方法名与字段名都转小写后 endsWith"，因此大小写不敏感，
     * 但也会命中 {@code getSomeXxx} 这种前缀方法——原厂就这么写的。
     * 原厂此方法目前没有调用方（保留的反射能力），照原样重建。</p>
     */
    private Method findGetter(Class<?> cls, Field field) {
        Locale locale = Locale.getDefault();
        try {
            Method[] methods = cls.getDeclaredMethods();
            String fieldName = field.getName().toLowerCase(locale);
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase(locale);
                if (!methodName.startsWith("set") && methodName.endsWith(fieldName) && methodName.startsWith("get")) {
                    return method;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 按字段名找对应的 setter（形如 setXxx，且不以 get 开头）。
     *
     * <p>serialVersionUID 是编译期常量，不能也不需要赋值，直接跳过。
     * 找不到就返回 null，由调用方跳过该字段。</p>
     */
    private Method findSetter(Class<?> cls, Field field) {
        Method result;
        Locale locale = Locale.getDefault();
        try {
            Method[] methods = cls.getDeclaredMethods();
            String fieldName = field.getName().toLowerCase(locale);
            if ("serialversionuid".equals(fieldName)) {
                return null;
            }
            String setterName = "set" + fieldName;
            int length = methods.length;
            for (int i = 0; i < length; i++) {
                result = methods[i];
                String methodName = result.getName().toLowerCase(locale);
                if (!methodName.startsWith("get") && methodName.equals(setterName)) {
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result = null;
        return result;
    }

    /**
     * 打开数据库文件。
     *
     * <p>文件不存在直接返回 null（不创建）；打开失败（权限、格式、被占用）
     * 只打印堆栈并返回 null——上层据此走"数据库不可用"分支。</p>
     */
    private SQLiteDatabase openDatabase(String dbPath) {
        if (!new File(dbPath).exists()) {
            return null;
        }
        try {
            return SQLiteDatabase.openDatabase(dbPath, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 把数据库里的时间字符串按 "yyyy-MM-dd HH:mm:ss" 解析；解析失败返回 null。 */
    private Date parseDate(String value) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (value != null) {
            try {
                return formatter.parse(value);
            } catch (Exception e) {
                // 格式不合法按"没有值"处理，异常被吞掉
            }
        }
        return null;
    }

    /**
     * 在一个事务里执行一条 SQL，然后无条件关闭连接（单次使用，用完即弃）。
     *
     * <p>细节：</p>
     * <ul>
     *   <li>{@code "".equals(sql) && sql == null} 这个条件永远不成立，是原厂遗留的
     *       防御性写法，照原样保留，不"顺手修好"；</li>
     *   <li>finally 里 {@code endTransaction/close} 没有判空——但因为整段 try 被包在
     *       {@code mDB != null} 分支里，所以不会 NPE，结构也要原样保持；</li>
     *   <li>所有异常都被 catch 吞掉并 printStackTrace，返回 false 而不是抛出。</li>
     * </ul>
     */
    private boolean executeSql(String sql) {
        Boolean success = false;
        if (this.mDB != null) {
            try {
                if ("".equals(sql) && sql == null) {
                    return success.booleanValue();
                }
                this.mDB.beginTransaction();
                this.mDB.execSQL(sql);
                this.mDB.setTransactionSuccessful();
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.mDB.endTransaction();
                this.mDB.close();
            }
        }
        return success.booleanValue();
    }

    /**
     * 把游标的当前行按"字段名 == 列名"反射写入 bean。
     *
     * <p>支持 String / int / float / long / Date 四种类型以及"其它一律当 String"的兜底。
     * 列值为 NULL 时：String 与 Date 写入 null，而数值类型会走原厂的自动拆箱路径
     * 抛 NullPointerException，最终被外层统一包成 RuntimeException——
     * 这是原厂既有行为（调用方的实体字段都非空，所以线上没暴露）。</p>
     *
     * @param cursor 已定位到目标行的游标
     * @param bean   待填充对象
     * @return 传入的 bean
     * @throws RuntimeException 反射失败或数值列为 NULL 时
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T parseBean(Cursor cursor, T bean) {
        try {
            Class<?> cls = bean.getClass();
            for (Field field : cls.getDeclaredFields()) {
                int columnIndex = cursor.getColumnIndex(field.getName());
                if (columnIndex < 0) {
                    continue;
                }
                Method setter = findSetter(cls, field);
                if (setter == null) {
                    continue;
                }
                String value = cursor.getString(columnIndex) + "";
                if (cursor.isNull(columnIndex)) {
                    value = null;
                }
                Class<?> type = field.getType();
                if (type == String.class) {
                    setter.invoke(bean, value);
                } else if (type == Integer.TYPE || type == Integer.class) {
                    setter.invoke(bean, value == null ? (Integer) null : Integer.parseInt(value));
                } else if (type == Float.TYPE || type == Float.class) {
                    setter.invoke(bean, value == null ? (Float) null : Float.parseFloat(value));
                } else if (type == Long.TYPE || type == Long.class) {
                    setter.invoke(bean, value == null ? (Long) null : Long.parseLong(value));
                } else if (type == Date.class) {
                    setter.invoke(bean, value == null ? (Date) null : parseDate(value));
                } else {
                    setter.invoke(bean, value);
                }
            }
            return bean;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** 关闭连接并置空，避免重复关闭。 */
    public void close() {
        if (this.mDB != null) {
            this.mDB.close();
            this.mDB = null;
        }
    }

    /**
     * 执行一次查询并在回调里交付游标，回调返回后关闭连接。
     *
     * <p>数据库不可用时只打一条 error 日志然后返回，回调不会被调用。</p>
     */
    public void query(String sql, CursorCallback callback) {
        if (this.mDB == null) {
            LogUtil.e(TAG, "mDB is null!!!");
            return;
        }
        Cursor cursor = this.mDB.rawQuery(sql, null);
        LogUtil.d(TAG, "getCursorData start");
        callback.callBack(cursor);
        close();
    }

    /** 执行 INSERT 语句（整条 SQL 由调用方拼好）。 */
    public boolean insert(String sql) {
        return executeSql(sql);
    }

    /** 执行 UPDATE 语句。 */
    public boolean update(String sql) {
        return executeSql(sql);
    }

    /** 执行 DELETE 语句。 */
    public boolean delete(String sql) {
        return executeSql(sql);
    }

    /** 执行任意 SQL（建表、ALTER 等）。 */
    public boolean execSql(String sql) {
        return executeSql(sql);
    }

    /**
     * 直接查询并返回游标，连接不关闭，由调用方自行关闭。
     *
     * <p>与 {@link #query(String, CursorCallback)} 的区别就在这里：
     * 后者会关连接，前者不会——调用方（比如 MotionData）需要自己管理。</p>
     */
    public Cursor rawQuery(String sql) {
        if (this.mDB != null) {
            return this.mDB.rawQuery(sql, null);
        }
        return null;
    }
}
