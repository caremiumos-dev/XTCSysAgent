package com.xtc.dns.service;

import android.text.TextUtils;
import com.xtc.dns.DnsLogTag;
import com.xtc.dns.info.HttpDnsInfo;
import com.xtc.dns.config.HttpDnsConfigFile;
import com.xtc.httplib.ConfigOptions;
import com.xtc.log.LogUtil;
import java.util.Properties;

/**
 * HttpDns 的全部可调参数（原厂 {@code com.xtc.dns.service.provider}）。
 *
 * <p>参数的来源有三处，优先级从高到低：服务端 web 配置（{@link #apply}）、
 * sdcard 上的本地配置文件（{@link #loadFromConfigFile}）、类里的默认值。
 * 两个入口都是"读文件/写文件"或"读配置/写文件"的成对操作，且都放在后台线程里做，
 * 因为 sdcard 读写可能卡几百毫秒。</p>
 *
 * <p>注意所有字段都是 {@code public static}：provider 侧（另一个进程也会读同一份代码）
 * 直接读字段，没有 getter 封装。这是原厂的形态，改成 getter 反而会影响
 * {@code DnsServiceImpl} 等处对 {@code dnsTtl} 的即时读取语义。</p>
 */
public class DnsConfig {

    /** 结果来源：本地系统解析。 */
    public static final int SOURCE_LOCAL = 1;

    /** 结果来源：腾讯 HttpDns。 */
    public static final int SOURCE_TENCENT = 0;

    /** 通用开关常量（原字段 j，包内无引用点，按"开启"语义推断）。 */
    public static final int SWITCH_ENABLE = 1;

    /** 日志 tag。 */
    private static final String TAG = DnsLogTag.serverTag("DnsConfig");

    /** 腾讯 HttpDns 服务 ip。 */
    public static String httpDnsIp = "119.29.29.29";

    /** 腾讯 HttpDns 的 id 参数（服务端分配的应用标识）。 */
    public static String appId = "196";

    /** 最大缓存时间（秒）：默认 3 天，超过就从库里删掉。 */
    public static int maxCacheTime = 259200;

    /** 缓存 ttl（秒）：写进每条记录的 domain_ttl。 */
    public static int dnsTtl = 480;

    /** HttpDns 总开关；provider 查询时先看它。 */
    public static boolean httpDnsEnable = false;

    /**
     * 腾讯 HttpDns 的查询前缀（{@code http://ip/d?dn=}）。
     *
     * <p>注意它只在类初始化时按默认 ip 拼一次；后续 {@code httpDnsIp} 被配置改掉时
     * **不会**跟着变——原厂就这个行为，改了就与线上一致的数据对不上。</p>
     */
    public static String tencentRequestUrl = ConfigOptions.ProtocolType.HTTP + httpDnsIp + "/d?dn=";

    /** 腾讯 HttpDns 是否可用（{@code TencentDnsProvider#isAvailable} 直接返回它）。 */
    public static boolean tencentAvailable = true;

    /**
     * 从 sdcard 的配置文件加载参数。
     *
     * <p>整体在后台线程执行；文件不存在时什么都不做（保留默认值）。
     * 注意 {@code enableHttpDns} 缺省为 {@code "0"}，即"没有配置就是关闭"。</p>
     */
    public static void loadFromConfigFile() {
        new Thread(() -> {
            Properties properties = HttpDnsConfigFile.load();
            if (properties != null) {
                setHttpDnsEnable(properties.getProperty("enableHttpDns", "0").equals("1"));
                String ip = properties.getProperty("httpDnsIp");
                if (!TextUtils.isEmpty(ip)) {
                    setHttpDnsIp(ip);
                }
                String cacheTime = properties.getProperty("cacheTime");
                if (!TextUtils.isEmpty(cacheTime)) {
                    setMaxCacheTime(Integer.parseInt(cacheTime));
                }
                String ttl = properties.getProperty("dnsTtl");
                if (TextUtils.isEmpty(ttl)) {
                    return;
                }
                setDnsTtl(Integer.parseInt(ttl));
            }
        }).start();
    }

    /**
     * 应用一份服务端下发的配置：先更新内存里的四个字段，再把它们写回本地配置文件。
     *
     * <p>写文件放在后台线程，且复用"读出来的 Properties"——读失败时用空对象兜底，
     * 这样写回去的文件里就只有这四个键（原厂如此）。</p>
     *
     * @param info 下发的配置；为 null 时直接返回
     */
    public static void apply(final HttpDnsInfo info) {
        if (info == null) {
            return;
        }
        setHttpDnsEnable(info.getDnsSwitch().intValue() == 1);
        setHttpDnsIp(info.getIp());
        setDnsTtl(info.getTtl().intValue());
        setMaxCacheTime(info.getCacheTime().intValue());
        new Thread(() -> {
            Properties properties = HttpDnsConfigFile.load();
            if (properties == null) {
                properties = new Properties();
            }
            properties.put("enableHttpDns", String.valueOf(info.getDnsSwitch()));
            properties.put("httpDnsIp", info.getIp());
            properties.put("cacheTime", String.valueOf(info.getCacheTime()));
            properties.put("dnsTtl", String.valueOf(info.getTtl()));
            HttpDnsConfigFile.save(properties);
        }).start();
    }

    /** 设置腾讯 HttpDns 的 ip。 */
    private static void setHttpDnsIp(String ip) {
        LogUtil.d(TAG, "设置腾讯HttpDns ip:" + ip);
        httpDnsIp = ip;
    }

    /** 设置 HttpDns 总开关。 */
    private static void setHttpDnsEnable(boolean enable) {
        LogUtil.d(TAG, "设置HttpDns开关：" + enable);
        httpDnsEnable = enable;
    }

    /** 设置最大缓存时间（秒）。 */
    private static void setMaxCacheTime(int cacheTime) {
        LogUtil.d(TAG, "设置最大缓存时间：" + cacheTime);
        maxCacheTime = cacheTime;
    }

    /** 设置缓存 ttl（秒）。 */
    private static void setDnsTtl(int ttl) {
        LogUtil.d(TAG, "设置缓存时间ttl:" + ttl);
        dnsTtl = ttl;
    }
}
