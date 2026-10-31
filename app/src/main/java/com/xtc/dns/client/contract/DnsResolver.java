package com.xtc.dns.client.contract;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.xtc.dns.client.ClientLogTag;
import com.xtc.dns.client.DomainInfo;
import com.xtc.dns.client.util.InetAddressValidator;
import com.xtc.log.LogUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HttpDns ContentProvider 的客户端代理（原厂 {@code com.xtc.dns.client.contract.b}）。
 *
 * <p>所有"写"操作都借 {@link ContentResolver#getType} 触发：原厂没有用
 * insert/update，而是把动作编码进 URI 路径，provider 在 {@code getType} 里执行，
 * 这样调用方不需要拿返回值。改造成 insert/update 会改变 provider 的对外协议，
 * 所以这里原样保留。</p>
 */
public class DnsResolver {

    /** 日志 tag。 */
    private static final String TAG = ClientLogTag.clientTag("DnsResolver");

    /** provider 的 base uri（只含 scheme + authority）。 */
    private final Uri baseUri;

    /** 用来发 getType 的 ContentResolver。 */
    private final ContentResolver contentResolver;

    /** ip 格式校验器，单例。 */
    private final InetAddressValidator inetAddressValidator = InetAddressValidator.getInstance();

    public DnsResolver(Context context, String authority) {
        this.contentResolver = context.getContentResolver();
        this.baseUri = new Uri.Builder().scheme("content").authority(authority).build();
    }

    /** 通知 provider 检查一遍过期缓存。 */
    public void checkIpOverTime() {
        this.contentResolver.getType(this.baseUri.buildUpon()
                .appendPath(DnsProviderContract.PATH_CHECK_IP_OVER_TIME).build());
    }

    /** 通知 provider 检查指定 ip 的过期情况。 */
    public void checkIpOverTime(String ip) {
        this.contentResolver.getType(this.baseUri.buildUpon()
                .appendPath(DnsProviderContract.PATH_CHECK_IP_OVER_TIME)
                .appendPath(ip).build());
    }

    /** 上报一个不可达的 ip，provider 会据此判断是否需要重新解析。 */
    public void checkIpExpire(String host, String ip) {
        this.contentResolver.getType(this.baseUri.buildUpon()
                .appendPath(DnsProviderContract.PATH_CHECK_IP_EXPIRE)
                .appendPath(host)
                .appendPath(ip).build());
    }

    /** 要求 provider 清空缓存。 */
    public void clear() {
        this.contentResolver.getType(this.baseUri.buildUpon()
                .appendPath(DnsProviderContract.PATH_CLEAR).build());
    }

    /**
     * 查询某域名的 HttpDns 缓存。
     *
     * <p>传进来的如果本身就是 ip，直接原样包装返回（不再问 provider）。
     * 查询异常一律吞掉并返回 null——HttpDns 只是"加速器"，
     * 任何失败都必须让上层能继续走系统解析，所以这里不允许抛异常。</p>
     */
    @Nullable
    public List<DomainInfo> getDomainInfo(String host) {
        if (TextUtils.isEmpty(host)) {
            return null;
        }
        if (this.inetAddressValidator.isValid(host)) {
            LogUtil.i(TAG, "已经是ip请求，返回原url");
            return Collections.singletonList(new DomainInfo(host, host));
        }
        Uri queryUri = this.baseUri.buildUpon()
                .appendPath(DnsProviderContract.PATH_DOMAIN_MODEL)
                .appendPath(host).build();
        List<DomainInfo> result = new ArrayList<DomainInfo>();
        Cursor cursor = null;
        try {
            cursor = this.contentResolver.query(queryUri, null, null, null, null);
            if (cursor == null) {
                // 注意：这条分支直接返回 null，不走下面的"没有缓存"日志（原厂如此）。
                LogUtil.d(TAG, "cursor == null");
                return null;
            }
            if (cursor.moveToNext()) {
                String ip = cursor.getString(cursor.getColumnIndex(DnsProviderContract.Column.IP));
                LogUtil.d(TAG, "ip: " + ip);
                result.add(new DomainInfo(host, ip));
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (result.isEmpty()) {
            LogUtil.i(TAG, "没有HttpDns缓存");
        }
        return result.isEmpty() ? null : result;
    }
}
