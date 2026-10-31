package com.xtc.dns.storage.db;

import android.content.Context;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.xtc.database.core.DaoBase;
import com.xtc.dns.DnsLogTag;
import com.xtc.log.LogUtil;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * {@link DomainModel} 的 DAO（原厂 {@code com.xtc.dns.storage.db.c}）。
 *
 * <p>继承工程里通用的 ORMLite DAO 基类 {@link DaoBase}，只补充本表特有的查询：
 * 按域名查、按 ip 查、按 domain 删旧再整批插入、批量改优先级。</p>
 *
 * <p>依赖基类提供的通用能力（接入时若基类命名不同，只需改本文件里的调用点）：
 * {@code getDao()}、{@code queryForFirst(Map)}、{@code deleteByColumn(String,Object)}、
 * {@code insertAll(List)}、{@code query(String orderBy,String where,Object,boolean)}、
 * {@code queryByColumn(String,Object)}、{@code updateByColumn(T,String,Object)}，
 * 以及 {@code clearTableWithCountCheck()}（原 {@code j.b()}：先数行数再删全部）。</p>
 */
public class DomainModelDao extends DaoBase<DomainModel> {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("DomainModelDao");

    /** 列名常量，避免与数据库里的物理列名写岔。 */
    private static final String COLUMN_DOMAIN = "domain";
    private static final String COLUMN_IP = "ip";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_CREATE_TIME = "create_time";

    public DomainModelDao(Context context) {
        super(context, DomainModel.class, DatabaseManager.DATABASE_NAME);
    }

    /**
     * 删除创建时间早于 {@code time} 的记录。
     *
     * @return 删除行数；SQL 失败返回 0（异常吞掉，只打日志）
     */
    public int deleteCreatedBefore(long time) {
        try {
            DeleteBuilder<DomainModel, Integer> deleteBuilder = getDao().deleteBuilder();
            deleteBuilder.where().lt(COLUMN_CREATE_TIME, Long.valueOf(time));
            return deleteBuilder.delete();
        } catch (SQLException e) {
            LogUtil.e(TAG, "deleteReportByTrigTime: ", e);
            return 0;
        }
    }

    /** 按域名 + ip 查唯一一条记录。 */
    public DomainModel findByHostAndIp(String host, String ip) {
        HashMap<String, Object> where = new HashMap<String, Object>();
        where.put(COLUMN_DOMAIN, host);
        where.put(COLUMN_IP, ip);
        return queryForFirst(where);
    }

    /**
     * 用新结果覆盖某域名的缓存：先删旧记录，再整批插入。
     *
     * <p>不比较新旧内容，直接删+插——因为 ttl/优先级/创建时间都要刷新，
     * 逐条 update 反而更容易出错。</p>
     */
    public void replaceByDomain(String host, List<DomainModel> models) {
        deleteByDomain(host);
        insertAll(models);
    }

    /** 按域名查，按优先级**降序**（priority 大的在前）。 */
    public List<DomainModel> queryByDomain(String host) {
        return query(COLUMN_PRIORITY, COLUMN_DOMAIN, host, false);
    }

    /** 按 ip 查（同一个 ip 可能被多个域名共用）。 */
    public List<DomainModel> queryByIp(String ip) {
        return queryByColumn(COLUMN_IP, ip);
    }

    /**
     * 批量改优先级，放在一个事务里提交。
     *
     * <p>用事务而不是逐条 update：降权往往一次影响多行，
     * 中途失败会留下"部分 ip 被降权"的不一致状态。</p>
     *
     * @return 事务是否成功；SQL 失败返回 false（异常吞掉，原厂不打印）
     */
    public boolean updateInTransaction(final List<DomainModel> models) {
        if (models == null || models.isEmpty()) {
            return false;
        }
        try {
            return new TransactionManager(getDao().getConnectionSource())
                    .<Boolean>callInTransaction(() -> {
                        for (DomainModel model : models) {
                            updateByColumn(model, COLUMN_IP, model.getIp());
                        }
                        return true;
                    }).booleanValue();
        } catch (SQLException e) {
            return false;
        }
    }

    /** 按域名删除（覆盖缓存前的清理动作）。 */
    public boolean deleteByDomain(String host) {
        return deleteByColumn(COLUMN_DOMAIN, host);
    }

    /**
     * 清空整张表。
     *
     * <p>对应基类的 {@code b()}（原 {@code j.b()}）：先数一次行数，为 0 就直接返回 true，
     * 否则删掉全部行。注意它和基类里另一个"直接 delete 全部"的重载不是同一个方法，
     * 这里必须用"先数再删"这个，否则空表时的返回值会不一样。</p>
     */
    public boolean clearAllRows() {
        return clearTableWithCountCheck();
    }
}
