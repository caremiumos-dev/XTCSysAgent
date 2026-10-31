package com.xtc.bigdata.common.utils;

import android.content.SharedPreferences;

/**
 * 采集库私有 SharedPreferences 的封装。
 *
 * <p>用的是固定的 {@code BigData_Collector} 文件，里面存的是设备基信息
 * （{@code base_attr}）、用户信息（{@code userInfo}）、上次崩溃指纹
 * （{@code crashInfo}）等"进程重启后还要用"的东西。</p>
 *
 * <p>所有写操作都走 {@code commit()} 而不是 {@code apply()}：
 * 崩溃采集是在进程即将被杀的路径上写的，必须同步落盘。</p>
 */
public class SharedPrefUtils {

    /** 私有 SP 文件名。 */
    private static final String PREF_NAME = "BigData_Collector";

    /** 单例。 */
    private static SharedPrefUtils instance = null;

    /** 编辑器，构造时创建后长期复用。 */
    private SharedPreferences.Editor editor;

    /** 底层 SharedPreferences。 */
    private SharedPreferences sharedPreferences;

    /** 构造时立刻取 SP 与 Editor —— 此时 {@code ContextUtils} 必须已就绪。 */
    protected SharedPrefUtils() {
        this.sharedPreferences = null;
        this.editor = null;
        this.sharedPreferences = ContextUtils.getContext().getSharedPreferences(PREF_NAME, 0);
        this.editor = this.sharedPreferences.edit();
    }

    /**
     * 取单例（双重检查）。
     *
     * @return 单例
     */
    public static SharedPrefUtils getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SharedPrefUtils.class) {
            if (instance == null) {
                instance = new SharedPrefUtils();
            }
        }
        return instance;
    }

    /** 清空整个 SP 文件。 */
    public void clear() {
        if (this.editor != null) {
            this.editor.clear();
            this.editor.commit();
        }
    }

    /**
     * 读 boolean。
     *
     * @param key          键
     * @param defaultValue SP 不可用时的默认值
     * @return 值
     */
    public boolean getKeyBooleanValue(String key, boolean defaultValue) {
        return this.sharedPreferences != null ? this.sharedPreferences.getBoolean(key, defaultValue) : defaultValue;
    }

    /**
     * 读 float。
     *
     * @param key          键
     * @param defaultValue SP 不可用时的默认值
     * @return 值
     */
    public float getKeyFloatValue(String key, float defaultValue) {
        return this.sharedPreferences != null ? this.sharedPreferences.getFloat(key, defaultValue) : defaultValue;
    }

    /**
     * 读 int。
     *
     * @param key          键
     * @param defaultValue SP 不可用时的默认值
     * @return 值
     */
    public int getKeyIntValue(String key, int defaultValue) {
        return this.sharedPreferences != null ? this.sharedPreferences.getInt(key, defaultValue) : defaultValue;
    }

    /**
     * 读 long。
     *
     * @param key          键
     * @param defaultValue SP 不可用时的默认值
     * @return 值
     */
    public long getKeyLongValue(String key, long defaultValue) {
        return this.sharedPreferences != null ? this.sharedPreferences.getLong(key, defaultValue) : defaultValue;
    }

    /**
     * 读 String。
     *
     * @param key          键
     * @param defaultValue SP 不可用时的默认值
     * @return 值
     */
    public String getKeyStringValue(String key, String defaultValue) {
        return this.sharedPreferences != null ? this.sharedPreferences.getString(key, defaultValue) : defaultValue;
    }

    /**
     * 删除一个键。
     *
     * @param key 键
     */
    public void remove(String key) {
        if (this.editor != null) {
            this.editor.remove(key);
            this.editor.commit();
        }
    }

    /**
     * 写 boolean。
     *
     * @param key   键
     * @param value 值
     */
    public void saveKeyBooleanValue(String key, boolean value) {
        if (this.editor != null) {
            this.editor.putBoolean(key, value);
            this.editor.commit();
        }
    }

    /**
     * 写 float。
     *
     * @param key   键
     * @param value 值
     */
    public void saveKeyFloatValue(String key, float value) {
        if (this.editor != null) {
            this.editor.putFloat(key, value);
            this.editor.commit();
        }
    }

    /**
     * 写 int。
     *
     * @param key   键
     * @param value 值
     */
    public void saveKeyIntValue(String key, int value) {
        if (this.editor != null) {
            this.editor.putInt(key, value);
            this.editor.commit();
        }
    }

    /**
     * 写 long。
     *
     * @param key   键
     * @param value 值
     */
    public void saveKeyLongValue(String key, long value) {
        if (this.editor != null) {
            this.editor.putLong(key, value);
            this.editor.commit();
        }
    }

    /**
     * 写 String。
     *
     * @param key   键
     * @param value 值
     */
    public void saveKeyStringValue(String key, String value) {
        if (this.editor != null) {
            this.editor.putString(key, value);
            this.editor.commit();
        }
    }
}
