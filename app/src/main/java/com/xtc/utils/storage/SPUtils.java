package com.xtc.utils.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 全局 SharedPreferences 门面（单例）。
 *
 * <p>文件名叫什么？原厂直接用 {@code context.getPackageName()} 作为 SharedPreferences
 * 文件名，等于"整个 APK 共用一份偏好设置"，所以本类是进程内唯一的入口。</p>
 *
 * <p>所有写操作都走 {@code commit()} 同步落盘并返回 boolean（原厂刻意不用 apply()，
 * 因为有的调用方（开机流程）依赖写完即可读）。</p>
 */
public class SPUtils {

    /** 共享的偏好设置对象，构造时按包名取，之后不再变化。 */
    private static SharedPreferences sSharedPreferences;

    /** 双重检查锁单例；volatile 防止指令重排导致读到未初始化完的对象。 */
    private static volatile SPUtils sInstance;

    private SPUtils(Context context) {
        sSharedPreferences = context.getApplicationContext()
                .getSharedPreferences(context.getPackageName(), 0);
    }

    /**
     * 取单例。
     *
     * @param context 只在首次创建时使用，内部会取 applicationContext
     */
    public static SPUtils getInstance(Context context) {
        SPUtils instance = sInstance;
        if (instance == null) {
            synchronized (SPUtils.class) {
                instance = sInstance;
                if (instance == null) {
                    instance = new SPUtils(context);
                    sInstance = instance;
                }
            }
        }
        return instance;
    }

    /** 每次写入新建 Editor，避免共享 Editor 带来的并发问题。 */
    private SharedPreferences.Editor edit() {
        return sSharedPreferences.edit();
    }

    /** 清空整份偏好设置。 */
    public boolean clear() {
        return sSharedPreferences.edit().clear().commit();
    }

    /** key 是否存在。 */
    public boolean contains(String key) {
        return sSharedPreferences.contains(key);
    }

    public boolean putFloat(String key, float value) {
        return edit().putFloat(key, value).commit();
    }

    public boolean putInt(String key, int value) {
        return edit().putInt(key, value).commit();
    }

    public boolean putLong(String key, long value) {
        return edit().putLong(key, value).commit();
    }

    public boolean putString(String key, String value) {
        return edit().putString(key, value).commit();
    }

    public boolean putBoolean(String key, boolean value) {
        return edit().putBoolean(key, value).commit();
    }

    public float getFloat(String key, float defaultValue) {
        return sSharedPreferences.getFloat(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return sSharedPreferences.getInt(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return sSharedPreferences.getLong(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return sSharedPreferences.getString(key, defaultValue);
    }

    /** 删除单个 key。 */
    public boolean remove(String key) {
        return sSharedPreferences.edit().remove(key).commit();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sSharedPreferences.getBoolean(key, defaultValue);
    }
}
