package com.xtc.dns.storage;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.dns.DnsLogTag;
import com.xtc.dns.info.HttpDnsResult;
import com.xtc.dns.storage.db.DomainModel;
import com.xtc.dns.storage.db.DomainModelDao;
import com.xtc.log.LogUtil;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DnsStorageService} 的实现（原厂 {@code com.xtc.dns.storage.db}）。
 *
 * <p>两级存储：内存 {@link ConcurrentHashMap}（域名 → 整份记录列表）挡在前面，
 * ORMLite 数据库在後面。读路径先内存后库，并把库里的结果回填内存；
 * 写路径先落库再回填内存，保证"库里有的内存里也有"。</p>
 *
 * <p>为什么内存缓存存"整份列表"而不是单条？因为 {@code priority} 是列表内相对顺序，
 * 拆开存会让排序失真。</p>
 */
public class DnsStorageServiceImpl implements DnsStorageService {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("DnsStorageServiceImpl");

    /** 内存缓存的初始容量（原厂写死 8）。 */
    private static final int CACHE_INITIAL_CAPACITY = 8;

    /** 内存缓存的装载因子（原厂写死 32.0f，明显偏大，但那是为了少扩容，保持一致）。 */
    private static final float CACHE_LOAD_FACTOR = 32.0f;

    /** 内存缓存：域名 → 该域名的全部记录。 */
    private final ConcurrentHashMap<String, List<DomainModel>> memoryCache =
            new ConcurrentHashMap<String, List<DomainModel>>(CACHE_INITIAL_CAPACITY, CACHE_LOAD_FACTOR);

    /** 数据库访问对象。 */
    private final DomainModelDao domainModelDao;

    public DnsStorageServiceImpl(Context context) {
        this.domainModelDao = new DomainModelDao(context);
    }

    /** 把一份记录放进内存缓存（以第一条的域名作为键）。 */
    private void cache(List<DomainModel> models) {
        if (models == null || models.isEmpty()) {
            return;
        }
        this.memoryCache.put(models.get(0).getDomain(), models);
    }

    /** 清空内存缓存。 */
    private void clearMemoryCache() {
        this.memoryCache.clear();
    }

    /** 只摘掉某个域名的内存缓存。 */
    private void removeCache(String host) {
        if (TextUtils.isEmpty(host)) {
            return;
        }
        this.memoryCache.remove(host);
    }

    /** 按域名 + ip 查单条记录（判断某个 ip 是否已过期时用）。 */
    public DomainModel queryByHostAndIp(String host, String ip) {
        return this.domainModelDao.findByHostAndIp(host, ip);
    }

    @Override
    public List<DomainModel> queryByDomain(String host) {
        if (TextUtils.isEmpty(host)) {
            LogUtil.w(TAG, "host is null");
            return null;
        }
        List<DomainModel> cached = this.memoryCache.get(host);
        if (cached != null) {
            return cached;
        }
        List<DomainModel> fromDatabase = this.domainModelDao.queryByDomain(host);
        cache(fromDatabase);
        if (fromDatabase == null || fromDatabase.isEmpty()) {
            return null;
        }
        return fromDatabase;
    }

    @Override
    public void clearAll() {
        LogUtil.i(TAG, "清除所有dns缓存");
        clearMemoryCache();
        this.domainModelDao.clearAllRows();
    }

    @Override
    public void deleteExpired(long time) {
        this.domainModelDao.deleteCreatedBefore(time);
    }

    @Override
    public void save(HttpDnsResult result) {
        List<DomainModel> models = result.getModels();
        if (models == null || models.size() <= 0) {
            return;
        }
        this.domainModelDao.replaceByDomain(result.getDomain(), models);
        cache(models);
        LogUtil.i(TAG, "设置ip缓存成功!，domain = " + models.get(0).getDomain());
    }

    @Override
    public void decreasePriority(String ip) {
        if (TextUtils.isEmpty(ip)) {
            LogUtil.i(TAG, "decrease ip is invalid");
            return;
        }
        List<DomainModel> models = this.domainModelDao.queryByIp(ip);
        if (models != null) {
            // 注意日志在循环体内：库里可能有多个域名共用同一个 ip，每条都会打一行（原厂如此）。
            for (DomainModel model : models) {
                model.setPriority(model.getPriority() - 1);
                this.domainModelDao.updateInTransaction(models);
                removeCache(model.getDomain());
                LogUtil.i(TAG, "ip不可用，降低权值, ip = " + ip);
            }
        }
    }
}
