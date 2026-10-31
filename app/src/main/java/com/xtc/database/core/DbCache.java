package com.xtc.database.core;

import android.text.TextUtils;
import android.util.LruCache;

import com.xtc.log.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 表数据的二级缓存。
 *
 * <p>键是"表名"，值是这一行数据的 列名 → 字符串值 映射。用 {@link LruCache} 按
 * <b>每张表缓存的列数</b>（{@link #sizeOf} 返回 {@code map.size()}）来计权，
 * 总容量 100，而不是按条目数计。这样一张宽表会更快被淘汰。</p>
 *
 * <p>注意：原厂实现里 {@link #put(String, String, Object)} 只负责"占位" ——
 * 它把表对应的 map 取出来再原样放回去，并不真的写入列值（列值由调用方
 * 直接改 map 或走另一条路径）。这里保留该行为，不做"修正"。</p>
 */
public class DbCache {

    private static final String TAG = "DbCache";

    /** LruCache 的容量（按列数计权）。 */
    private static final int MAX_SIZE = 100;

    private static DbCache instance;

    private LruCache<String, Map<String, String>> cache;

    private DbCache() {
    }

    public static DbCache getInstance() {
        if (instance == null) {
            synchronized (DbCache.class) {
                if (instance == null) {
                    instance = new DbCache();
                }
            }
        }
        return instance;
    }

    /**
     * 判断本次操作是否可以跳过缓存。
     *
     * @return true 表示缓存还没建，或 key 为空
     */
    private boolean isUnavailable(String key) {
        if (this.cache == null) {
            return true;
        }
        return TextUtils.isEmpty(key);
    }

    /** 懒创建 LruCache（计权按列的个数）。 */
    private synchronized void ensureCache() {
        this.cache = new LruCache<String, Map<String, String>>(MAX_SIZE) {
            @Override
            protected int sizeOf(String tableName, Map<String, String> row) {
                return row.size();
            }
        };
    }

    /**
     * 读一列。
     *
     * @param tableName 表名
     * @param column    列名
     * @return 缓存里的值；没有则 null
     */
    public String get(String tableName, String column) {
        if (isUnavailable(tableName) || isUnavailable(column)) {
            LogUtil.i(TAG, "mLruCache is empty");
            return null;
        }
        Map<String, String> row = this.cache.get(tableName);
        if (row != null && !row.isEmpty()) {
            return row.get(column);
        }
        LogUtil.i(TAG, "tableCache is empty");
        return null;
    }

    /** 淘汰整张表的缓存。 */
    public void removeTable(String tableName) {
        if (isUnavailable(tableName)) {
            return;
        }
        this.cache.remove(tableName);
    }

    /**
     * 登记一次写入。
     *
     * <p>保留原厂行为：只把该表的 map 重新 put 回去（用于刷新 LRU 顺序与计权），
     * 不写入具体列值。</p>
     */
    public <T> void put(String tableName, String column, T value) {
        if (this.cache == null) {
            ensureCache();
        }
        if (isUnavailable(tableName) || isUnavailable(column) || value == null) {
            LogUtil.i(TAG, "value is null");
            return;
        }
        Map<String, String> row = this.cache.remove(tableName);
        if (row == null) {
            row = new HashMap<>();
        }
        this.cache.put(tableName, row);
    }

    /** 清空全部缓存并触发一次 GC。 */
    public void clear() {
        if (this.cache != null && this.cache.size() > 0) {
            this.cache.evictAll();
        }
        System.gc();
    }

    /** 删除某张表里的一列。 */
    public void remove(String tableName, String column) {
        Map<String, String> row;
        if (isUnavailable(tableName) || isUnavailable(column)
                || (row = this.cache.get(tableName)) == null) {
            return;
        }
        row.remove(column);
    }
}
