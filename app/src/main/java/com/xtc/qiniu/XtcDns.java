package com.xtc.qiniu;

import android.content.Context;
import com.qiniu.android.http.Dns;
import com.xtc.dns.api.DnsStrategy;
import com.xtc.dns.client.HttpDnsManager;
import com.xtc.log.LogUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 把本工程的 HTTP DNS 接到七牛 SDK 上。
 *
 * <p>七牛 SDK 的 {@link Dns} 接口和 okhttp 的 {@code Dns} 是两套类型，
 * 所以这里做一层薄适配：{@link #lookup(String)} 直接委托给 {@link DnsStrategy}，
 * 同时把解析结果留在 {@link #mInetAddressList} 里，
 * 供上传失败时 {@link #clear()} 逐条上报坏 IP。</p>
 */
public class XtcDns implements Dns {

    private static final String TAG = "qiniu_upload_XtcDns";

    private Context mContext;
    private DnsStrategy mDnsStrategy;

    /** 最近一次解析结果，{@link #clear()} 用它上报坏 IP。 */
    private List<InetAddress> mInetAddressList;

    public XtcDns(Context context) {
        this.mContext = context;
        this.mDnsStrategy = new DnsStrategy(context);
    }

    /**
     * 把上一次解析出来的 IP 全部标为可疑：先清缓存，再上报。
     *
     * <p>七牛的域名经常被调度到不通的 IP 上，上传报 DNS 错误时调用这里，
     * 下次重新解析就能换一批。</p>
     */
    public void clear() {
        if (this.mInetAddressList == null || this.mInetAddressList.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.mInetAddressList.size(); i++) {
            InetAddress inetAddress = this.mInetAddressList.get(i);
            if (inetAddress == null) {
                continue;
            }
            LogUtil.d(TAG, "clear() called inetAddress = " + inetAddress);
            String hostAddress = inetAddress.getHostAddress();
            HttpDnsManager.getInstance(this.mContext).clearCache(hostAddress);
            HttpDnsManager.getInstance(this.mContext)
                    .reportInvalidIp(inetAddress.getHostName(), hostAddress);
        }
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        this.mInetAddressList = this.mDnsStrategy.lookup(hostname);
        LogUtil.d(TAG, "lookup() called with: mInetAddressList = [" + this.mInetAddressList + "]");
        return this.mInetAddressList;
    }
}
