package com.xtc.utils.storage;

import android.database.sqlite.SQLiteDatabase;
import com.xtc.log.LogUtil;

/**
 * SQLite 数据库健康检查。
 *
 * <p>只有一个能力：把 {@link SQLiteDatabase#isDatabaseIntegrityOk()} 包一层，
 * 顺手吞掉"库文件损坏/无法打开"时抛出的异常，避免巡检流程因为单个库坏掉而整体崩掉。</p>
 */
public class DatabaseUtil {

    /** 日志 tag，与原厂保持一致，便于线上按 tag 过滤。 */
    private static final String TAG = "DataBaseUtil";

    /**
     * 检查数据库完整性。
     *
     * @param db 已经打开的数据库
     * @return 完整返回 true；不完整或检查过程抛异常返回 false
     */
    public static boolean isDatabaseIntegrityOk(SQLiteDatabase db) {
        try {
            return db.isDatabaseIntegrityOk();
        } catch (Exception e) {
            // 原厂在这里直接吞掉异常并返回 false，不做重试也不向上抛，保持一致。
            LogUtil.e(TAG, "isDatabaseIntegrityOk error=", e);
            return false;
        }
    }
}
