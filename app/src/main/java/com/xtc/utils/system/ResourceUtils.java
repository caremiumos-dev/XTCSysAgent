package com.xtc.utils.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.io.InputStream;

/**
 * 跨包资源读取工具（附带两个裸 SQLite 快捷方法）。
 *
 * <p>原厂类名为单字母 {@code j}，内部回调接口是 {@code j.a}。命名依据：主要用途是
 * "拿另一个 App 的 Resources，再按 id 名取颜色 / 尺寸 / 图 / 字符串 / 字符串数组 / raw"，
 * 所以叫 {@link ResourceUtils}；末尾三个方法是对某个具体 .db 文件做 execSQL / rawQuery，
 * 不需要打开整个 ORM，原件就把它们塞在同一个工具类里，这里保留同样布局。</p>
 *
 * <p>包不存在时统一抛 {@link PackageManager.NameNotFoundException}（原件不吞），
 * 由调用方决定怎么处理。</p>
 */
public class ResourceUtils {

    private ResourceUtils() {
    }

    /**
     * 资源查询回调。
     *
     * <p>原厂是 {@code j.a}，方法 {@code a(Cursor)}。Cursor 由本类打开、也在本类里随
     * 数据库一起关闭，回调内只负责读数据，不要自己关。</p>
     */
    public interface CursorHandler {
        /** 处理查询结果。 */
        void handle(Cursor cursor);
    }

    /** 取颜色值（{@code color} 类型资源）。 */
    public static int getColor(Context context, String packageName, String colorName)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).getColor(
                getIdentifier(context, packageName, colorName, "color"));
    }

    /**
     * 按资源名 + 资源类型取资源 id。
     *
     * <p>第四个参数就是 {@code Resources.getIdentifier} 的 defType，
     * 原件传的是 {@code "color"} / {@code "dimen"} / {@code "drawable"} /
     * {@code "string"} / {@code "array"} / {@code "raw"}，原样保留。</p>
     */
    public static int getIdentifier(Context context, String packageName, String resourceName, String defType)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).getIdentifier(resourceName, defType, packageName);
    }

    /**
     * 拿到目标包的 Context。
     *
     * <p>flag 固定用 {@link Context#CONTEXT_IGNORE_SECURITY}（数值 2，原件直接写死 2），
     * 语义就是"忽略目标包与自己的签名/权限差异也要读到资源"。</p>
     */
    public static Context getPackageContext(Context context, String packageName)
            throws PackageManager.NameNotFoundException {
        return context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
    }

    /** 打开一个已存在的数据库文件；文件不存在返回 null。flag 0 即"只读打开"。 */
    private static SQLiteDatabase openDatabase(String dbPath) {
        if (new File(dbPath).exists()) {
            return SQLiteDatabase.openDatabase(dbPath, null, 0);
        }
        return null;
    }

    /** 对指定数据库文件执行一条非查询 SQL；数据库不存在时什么都不做。 */
    public static void executeSql(String dbPath, String sql) {
        SQLiteDatabase database = openDatabase(dbPath);
        if (database != null) {
            database.execSQL(sql);
            database.close();
        }
    }

    /** 对指定数据库文件执行查询，把 Cursor 交给回调；数据库不存在时回调不会被调用。 */
    public static void querySql(String dbPath, String sql, CursorHandler handler) {
        SQLiteDatabase database = openDatabase(dbPath);
        if (database != null) {
            handler.handle(database.rawQuery(sql, null));
            database.close();
        }
    }

    /** 取尺寸值（{@code dimen} 类型资源，返回像素）。 */
    public static float getDimension(Context context, String packageName, String dimenName)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).getDimension(
                getIdentifier(context, packageName, dimenName, "dimen"));
    }

    /** 取目标包的 Resources。 */
    public static Resources getResources(Context context, String packageName)
            throws PackageManager.NameNotFoundException {
        return getPackageContext(context, packageName).getResources();
    }

    /** 取图片（{@code drawable} 类型资源）。 */
    public static Drawable getDrawable(Context context, String packageName, String drawableName)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).getDrawable(
                getIdentifier(context, packageName, drawableName, "drawable"));
    }

    /** 取字符串（{@code string} 类型资源）。 */
    public static String getString(Context context, String packageName, String stringName)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).getString(
                getIdentifier(context, packageName, stringName, "string"));
    }

    /** 取字符串数组（{@code array} 类型资源）。 */
    public static String[] getStringArray(Context context, String packageName, String arrayName)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).getStringArray(
                getIdentifier(context, packageName, arrayName, "array"));
    }

    /** 打开 raw 资源（{@code raw} 类型资源）成输入流。 */
    public static InputStream openRawResource(Context context, String packageName, String rawName)
            throws PackageManager.NameNotFoundException {
        return getResources(context, packageName).openRawResource(
                getIdentifier(context, packageName, rawName, "raw"));
    }
}
