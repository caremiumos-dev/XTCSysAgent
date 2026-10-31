package com.xtc.dns.storage.db;

import android.content.Context;
import com.xtc.database.core.DatabaseHelper;
import com.xtc.dns.DnsLogTag;
import com.xtc.log.LogUtil;

/**
 * HttpDns 数据库（{@code HttpDns.db}）的进程内单例（原厂 {@code com.xtc.dns.storage.db.a}）。
 *
 * <p>建表很慢，必须只做一次，所以用 {@code volatile} 字段 + 类锁的双检锁；
 * {@code init()} 用 {@code synchronized} 方法保证并发调用时也只会建一次表，
 * 第二次进来直接打日志返回。</p>
 */
public class DatabaseManager {

    /** 数据库文件名（包级可见，DAO 侧也用它）。 */
    static final String DATABASE_NAME = "HttpDns.db";

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("DatabaseManager");

    /** 单例实例。 */
    private static volatile DatabaseManager sInstance;

    /** ORMLite 的 helper。 */
    private final DatabaseHelper databaseHelper;

    /** 表是否已经建过。 */
    private volatile boolean initialized = false;

    private DatabaseManager(Context context) {
        this.databaseHelper = DatabaseHelper.open(context.getApplicationContext(), DATABASE_NAME);
    }

    /** 取单例；首次调用时创建 helper（但还没建表）。 */
    public static DatabaseManager getInstance(Context context) {
        DatabaseManager instance = sInstance;
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                instance = sInstance;
                if (instance == null) {
                    instance = new DatabaseManager(context);
                    sInstance = instance;
                }
            }
        }
        return instance;
    }

    /** 建表（幂等）。 */
    public synchronized void init() {
        if (this.initialized) {
            LogUtil.d(TAG, "database has inited , return !");
        } else {
            this.initialized = true;
            this.databaseHelper.registerTable(DomainModel.class);
            LogUtil.d(TAG, "database init completed");
        }
    }
}
