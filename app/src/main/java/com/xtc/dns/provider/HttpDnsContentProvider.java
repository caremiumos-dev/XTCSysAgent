package com.xtc.dns.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import com.xtc.dns.DnsLogTag;
import com.xtc.dns.service.DnsConfig;
import com.xtc.dns.service.DnsService;
import com.xtc.dns.service.DnsServiceImpl;
import com.xtc.dns.storage.db.DatabaseManager;
import com.xtc.dns.storage.db.DomainModel;
import com.xtc.log.LogUtil;
import java.util.List;
import java.util.Locale;

/**
 * HttpDns 的 ContentProvider（原厂同名类，Manifest 里以
 * {@code android:exported="true"} 注册，authority 为
 * {@code <包名>.com.xtc.httpdns.provider}）。
 *
 * <p>它对外只提供两件事：
 * <ol>
 *   <li>{@link #query} —— 按 {@code DomainModel/<域名>} 返回该域名的缓存行；</li>
 *   <li>{@link #getType} —— 把"写动作"编码进 Uri 路径，返回 null。
 *       原厂没有用 insert/update，因为调用方（别的进程）只关心副作用、不关心结果，
 *       用 getType 可以省掉一次 Binder 回包的数据。</li>
 * </ol></p>
 *
 * <p>因为 {@code exported=true}，任何应用都能访问它，所以这里刻意不做"调用方鉴权"
 * ——这是原厂的产品设计（HttpDns 本来就是给同厂商应用共享的），不是漏洞。</p>
 */
public class HttpDnsContentProvider extends ContentProvider {

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("HttpDnsContentProvider");

    /** UriMatcher 里 {@code DomainModel/*} 对应的编号。 */
    private static final int MATCH_DOMAIN_MODEL = 1;

    /** 返回游标的列，顺序即 MatrixCursor 的列顺序，不能调换（客户端按下标取值）。 */
    private static final String[] COLUMNS = {
            HttpDnsProviderContract.Column.ID,
            HttpDnsProviderContract.Column.CREATE_TIME,
            HttpDnsProviderContract.Column.DOMAIN,
            HttpDnsProviderContract.Column.DOMAIN_TTL,
            HttpDnsProviderContract.Column.IP,
            HttpDnsProviderContract.Column.IP_TTL,
            HttpDnsProviderContract.Column.PRIORITY,
            HttpDnsProviderContract.Column.SP};

    /** 服务实现，onCreate 里注入；包级可见与原厂一致。 */
    DnsService dnsService;

    /** Uri 匹配器，{@link UriMatcher#NO_MATCH} 即 -1（与原厂构造值一致）。 */
    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    @Override
    public boolean onCreate() {
        LogUtil.i(TAG, "onCreate");
        Context context = getContext();
        // 1. 先把本地配置文件里的 HttpDns 参数读进来（异步）；
        // 2. 建表；3. 组装服务实现。
        DnsConfig.loadFromConfigFile();
        DatabaseManager.getInstance(context).init();
        this.dnsService = new DnsServiceImpl(context);
        // provider 自己注册的 authority 用宿主包名拼，和客户端侧拼法一致。
        this.uriMatcher.addURI(
                String.format(Locale.ROOT, HttpDnsProviderContract.AUTHORITY_FORMAT, context.getPackageName()),
                HttpDnsProviderContract.MATCH_PATH_DOMAIN_MODEL,
                MATCH_DOMAIN_MODEL);
        return true;
    }

    /**
     * 把写动作分发出去。
     *
     * <p>路径第一段决定动作，第二段起是参数。返回值固定为 null——
     * {@code ContentResolver#getType} 的调用方不关心类型，只借用这次跨进程调用。</p>
     */
    @Override
    public String getType(Uri uri) {
        String path = uri.getPath();
        LogUtil.d(TAG, "getType,path: " + path);
        if (!TextUtils.isEmpty(path)) {
            // 取第一个 "/" 之后的下一段作为动作名（没有第二段时整条路径就是动作名）。
            String type = path.indexOf("/", 1) != -1 ? path.substring(0, path.indexOf("/", 1)) : path;
            LogUtil.i(TAG, "getType,type: " + type);
            switch (type) {
                case HttpDnsProviderContract.PATH_DECREASE_IP_PRIORITY:
                    String ip = path.substring(path.indexOf("/", 1) + 1);
                    LogUtil.i(TAG, "decreaseIp: " + ip);
                    this.dnsService.decreasePriority(ip);
                    break;
                case HttpDnsProviderContract.PATH_CHECK_IP_EXPIRE:
                    String param = path.substring(path.indexOf("/", 1) + 1);
                    LogUtil.i(TAG, "param: " + param);
                    this.dnsService.checkIpExpire(param.substring(0, param.indexOf("/")),
                            param.substring(param.indexOf("/") + 1));
                    break;
                case HttpDnsProviderContract.PATH_CHECK_IP_OVER_TIME:
                    this.dnsService.checkIpOverTime();
                    break;
                case HttpDnsProviderContract.PATH_CLEAR:
                    this.dnsService.clear();
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    /**
     * 查询某域名的 HttpDns 缓存。
     *
     * <p>只有 {@code DomainModel/*} 这一种 Uri 会被受理，且 HttpDns 总开关关闭时
     * 直接返回 null（让客户端回落到系统解析）。返回值永远是 MatrixCursor，
     * 没有数据时就是空游标而不是 null——客户端用 {@code moveToNext()} 判断。</p>
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // API 19 起才能拿到调用方包名，低版本传空串（原厂如此），只用于日志。
        LogUtil.d(TAG, "uri: " + uri.toString() + ", packageName:"
                + (Build.VERSION.SDK_INT >= 19 ? getCallingPackage() : ""));
        int match = this.uriMatcher.match(uri);
        LogUtil.d(TAG, "query type:" + match);
        if (match != MATCH_DOMAIN_MODEL) {
            return null;
        }
        if (!DnsConfig.httpDnsEnable) {
            LogUtil.i(TAG, "httpdns disable");
            return null;
        }
        // 每次给游标一份新的列数组，避免调用方拿到内部数组后改坏后续查询（与原厂一致）。
        MatrixCursor cursor = new MatrixCursor(COLUMNS.clone());
        List<DomainModel> models = this.dnsService.getDomainModel(uri.getLastPathSegment());
        if (models != null) {
            for (DomainModel model : models) {
                cursor.addRow(new Object[]{
                        Long.valueOf(model.getId()),
                        Long.valueOf(model.getCreateTime()),
                        model.getDomain(),
                        Long.valueOf(model.getDomainTtl()),
                        model.getIp(),
                        Long.valueOf(model.getIpTtl()),
                        Integer.valueOf(model.getPriority()),
                        model.getSp()});
            }
        }
        return cursor;
    }

    /** 不支持插入（写动作全部走 {@link #getType}）。 */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LogUtil.d(TAG, "insert");
        return null;
    }

    /** 不支持删除（写动作全部走 {@link #getType}）。 */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LogUtil.d(TAG, "delete");
        return 0;
    }

    /** 不支持更新（写动作全部走 {@link #getType}）。 */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LogUtil.d(TAG, "update");
        return 0;
    }
}
