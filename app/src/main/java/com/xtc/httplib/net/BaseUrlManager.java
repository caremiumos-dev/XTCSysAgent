package com.xtc.httplib.net;

import android.content.Context;
import com.xtc.httplib.ConfigOptions;
import com.xtc.httplib.DomainConfig;
import com.xtc.httplib.HttpManager;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.bean.AppDomain;
import com.xtc.httplib.bean.AppInfo;
import com.xtc.httplib.bean.DomainInfo;
import com.xtc.httplib.bean.ImDomain;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.storage.FolderManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * 全局域名/协议/灰度管理中心。
 *
 * <p>域名不是硬编码的：ROM 会往配置文件里写一份服务端下发的 {@link DomainInfo}，
 * 本类在**第一次**取任意 URL 时读它，失败就退回 {@link #getDefaultDomain()} 的内置正式环境。
 * 之后所有 URL 都走缓存，不再读文件。</p>
 *
 * <p>配置文件有两个来源，取决于宿主包名：</p>
 * <ul>
 *   <li>{@code com.xtc.i3launcher}：读"上一次生效的配置"（{@code /cache/recovery/last_AppConfig}）；</li>
 *   <li>其它包（含本应用）：读"普通应用的配置路径"
 *       （{@code <SD>/xtc/<app>/common/AppConfig} 或恢复出厂备份目录，由 FolderManager 决定）。</li>
 * </ul>
 *
 * <p>灰度也会影响 IM 域名：AppInfo 里的 grey 大于 0 时使用灰度主域名 + 灰度备用列表。
 * 注意 {@code watchUrl} 与 {@code chatUrl} 取的是同一个 {@code watchDomain}，
 * 这是原厂行为，不是笔误。</p>
 *
 * <p>所有 {@code getXxxUrl} 都会拼成 {@code 协议 + 域名 + "/"} 的形态，
 * 协议由 {@link #PROTOCOL_TYPE} 决定，可在运行期由 {@code HttpManager#setUseHttps} 切换。</p>
 */
public class BaseUrlManager {

    private static final String TAG = LogTag.tag("BaseUrlManager");

    /** 协议前缀（{@code "http://"} 或 {@code "https://"}），可被宿主切换。 */
    public static String PROTOCOL_TYPE = ConfigOptions.ProtocolType.HTTPS;

    /** 当前配置是否正式环境（{@code domainId == "1"}）。 */
    private static boolean isReleaseEnv = true;

    /** URL 是否已经初始化过；初始化只做一次。 */
    private static boolean urlsHasInit = false;

    private static String watchUrl;
    private static String chatUrl;
    private static String locationUrl;
    private static String pointsUrl;
    private static String activityUrl;
    private static String sportUrl;
    private static String weatherUrl;
    private static String storeUrl;
    private static String thirdPartyUrl;
    private static String gameUrl;
    private static String themeUrl;
    private static String momentUrl;
    private static String gatewayUrl;

    /** IM 主域名（含端口），形如 {@code host:port}。 */
    private static String imUrl;
    /** IM 主机名（不含端口）。 */
    private static String imHost;
    /** IM 端口。 */
    private static int imPort;
    /** IM 备用地址列表，第 0 个就是主域名。 */
    private static String[] imBackupUrl;

    private BaseUrlManager() {
    }

    public static String getActivityUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(activityUrl);
    }

    public static String getChatUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(chatUrl);
    }

    /** 读取"上一次生效的配置"文件里的域名。 */
    public static DomainInfo getConfigDomain() {
        return getConfigDomain(FolderManager.getInstance().getLastAppConfigPath());
    }

    /**
     * 从指定路径读一行 JSON 并反序列化成 {@link DomainInfo}。
     *
     * <p>只读第一行；文件不存在、不是文件、或 IO 失败都返回 {@code null}
     * （并打印堆栈——原厂就是直接 {@code printStackTrace}，没有走日志框架）。
     * 注意这里**没有关闭** BufferedReader，原厂也没有关，保持原样以免影响
     * "文件句柄行为"这种极端可观测差异。</p>
     */
    private static DomainInfo getConfigDomain(String path) {
        DomainInfo domainInfo = null;
        try {
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                domainInfo = JSONUtil.fromJSON(new BufferedReader(new FileReader(path)).readLine(), DomainInfo.class);
                LogUtil.i(TAG, "getConfigDomain domainInfo = " + domainInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return domainInfo;
    }

    /** 读取"普通应用"配置文件里的域名。 */
    public static DomainInfo getConfigDomainOther() {
        return getConfigDomain(FolderManager.getInstance().getConfigFilePathForNormalApp());
    }

    /**
     * 内置的正式环境配置。配置文件缺失或字段不全时用它兜底。
     *
     * <p>IM 备用的两个数组由 {@code JSONUtil.toJSON} 序列化成串存进 {@link ImDomain}
     * （与和服务端下发配置的格式保持一致）。</p>
     */
    private static DomainInfo getDefaultDomain() {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setName(DomainConfig.AppFormalDomain.DNV_NAME);
        domainInfo.setDomainId("1");
        AppDomain appDomain = new AppDomain();
        appDomain.setWatchDomain("watch.okii.com");
        appDomain.setChatDomain("watch.okii.com");
        appDomain.setActivityDomain(DomainConfig.AppFormalDomain.ACTIVITY_DOMAIN);
        appDomain.setLocationDomain(DomainConfig.AppFormalDomain.LOCATION_DOMAIN);
        appDomain.setPointsDomain(DomainConfig.AppFormalDomain.POINTS_DOMAIN);
        appDomain.setThirdPartyDomain(DomainConfig.AppFormalDomain.THIRD_PARTY_DOMAIN);
        appDomain.setSportDomain("sport.watch.okii.com");
        appDomain.setWeatherDomain("sport.watch.okii.com");
        appDomain.setStoreDomain(DomainConfig.AppFormalDomain.STORE_DOMAIN);
        appDomain.setGameDomain(DomainConfig.AppFormalDomain.GAME_DOMAIN);
        appDomain.setThemeDomain(DomainConfig.AppFormalDomain.THEME_DOMAIN);
        appDomain.setMomentDomain(DomainConfig.AppFormalDomain.MOMENT_DOMAIN);
        appDomain.setGatewayDomain(DomainConfig.AppFormalDomain.GATEWAY_DOMAIN);
        domainInfo.setAppDomain(appDomain);
        ImDomain imDomain = new ImDomain();
        imDomain.setImJoinDomain(DomainConfig.ImDomain.FORMAL_DOMAIN);
        imDomain.setImBackupDomain(JSONUtil.toJSON(DomainConfig.ImDomain.FORMAL_STANDBY));
        imDomain.setImJoinGreyDomain(DomainConfig.ImDomain.GREY_DOMAIN);
        imDomain.setImGreyBackupDomain(JSONUtil.toJSON(DomainConfig.ImDomain.GRAY_STANDBY));
        domainInfo.setImDomain(imDomain);
        LogUtil.i(TAG, "getDefaultDomain domainInfo = " + domainInfo);
        return domainInfo;
    }

    public static String getDefaultUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(watchUrl);
    }

    public static String getGameUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(gameUrl);
    }

    public static String getGatewayUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(gatewayUrl);
    }

    public static String[] getImBackupUrl(Context context) {
        tryInitUrls(context);
        return imBackupUrl;
    }

    public static String getImHost(Context context) {
        tryInitUrls(context);
        return imHost;
    }

    public static int getImPort(Context context) {
        tryInitUrls(context);
        return imPort;
    }

    public static String getImUrl(Context context) {
        tryInitUrls(context);
        return imUrl;
    }

    public static String getLocationUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(locationUrl);
    }

    public static String getMomentUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(momentUrl);
    }

    public static String getPointsUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(pointsUrl);
    }

    /** 拼成 {@code 协议 + 域名 + "/"}。域名是 null 时会拼出 "null"，原厂如此。 */
    private static String getProtocolUrl(String url) {
        return PROTOCOL_TYPE + url + "/";
    }

    public static String getSportUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(sportUrl);
    }

    public static String getStoreUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(storeUrl);
    }

    public static String getThemeUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(themeUrl);
    }

    public static String getThirdPartyUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(thirdPartyUrl);
    }

    public static String getWeatherUrl(Context context) {
        tryInitUrls(context);
        return getProtocolUrl(weatherUrl);
    }

    /**
     * 是否灰度用户。
     *
     * <p>grey 是字符串形式的整数，{@code null} 视为非灰度；解析失败也视为非灰度
     * （但会打一条不带 tag 的错误日志——那个重载实际不输出内容，保留调用是为了
     * 与原厂的调用序列一致）。</p>
     */
    private static boolean isGrey(AppInfo appInfo) {
        if (appInfo.getGrey() == null) {
            return false;
        }
        try {
            return Integer.parseInt(appInfo.getGrey()) > 0;
        } catch (NumberFormatException e) {
            LogUtil.e(e);
            return false;
        }
    }

    /** 当前是否走 HTTPS。 */
    public static boolean isHttps() {
        return PROTOCOL_TYPE.equals(ConfigOptions.ProtocolType.HTTPS);
    }

    /** 当前配置是否正式环境。注意会触发一次 URL 初始化。 */
    public static boolean isReleaseEnv(Context context) {
        tryInitUrls(context);
        return isReleaseEnv;
    }

    /**
     * 解析 IM 主域名与备用列表。
     *
     * <p>主域名必须是 {@code host:port} 形态，否则只记一条错误日志、不改任何字段
     * （{@code imUrl/imHost/imPort} 保持上一次的值）。备用列表是 JSON 数组字符串，
     * 解析成功后把主域名插到第 0 位。</p>
     */
    private static void setImUrl(String imJoinDomain, String imBackupJson) {
        if (imJoinDomain.contains(":")) {
            String[] parts = imJoinDomain.split(":");
            imUrl = imJoinDomain;
            imHost = parts[0];
            imPort = Integer.parseInt(parts[1]);
        } else {
            LogUtil.e(TAG, "im format error!!");
        }
        List<String> backupList = JSONUtil.toCollection(imBackupJson, List.class, String.class);
        if (backupList != null) {
            backupList.add(0, imJoinDomain);
            imBackupUrl = backupList.toArray(new String[0]);
        }
    }

    /**
     * 首次取 URL 时初始化所有域名（同步方法，只会真正执行一次）。
     *
     * <p>顺序要点：先按包名选配置文件；配置缺字段就整体退回内置配置（不做字段级合并）；
     * 最后读 {@link AppInfo#getGrey()} 决定 IM 走正式还是灰度域名。</p>
     */
    private static synchronized void tryInitUrls(Context context) {
        if (urlsHasInit) {
            return;
        }
        DomainInfo configDomain = context.getPackageName().equals("com.xtc.i3launcher")
                ? getConfigDomain() : getConfigDomainOther();
        if (configDomain == null || configDomain.getAppDomain() == null || configDomain.getImDomain() == null) {
            configDomain = getDefaultDomain();
        }
        LogUtil.i(TAG, "tryInitUrls domainInfo = " + configDomain);
        AppDomain appDomain = configDomain.getAppDomain();
        isReleaseEnv = "1".equals(configDomain.getDomainId());
        watchUrl = appDomain.getWatchDomain();
        // 聊天与主域名共用同一个值（原厂行为）
        chatUrl = appDomain.getWatchDomain();
        sportUrl = appDomain.getSportDomain();
        activityUrl = appDomain.getActivityDomain();
        pointsUrl = appDomain.getPointsDomain();
        locationUrl = appDomain.getLocationDomain();
        thirdPartyUrl = appDomain.getThirdPartyDomain();
        weatherUrl = appDomain.getWeatherDomain();
        storeUrl = appDomain.getStoreDomain();
        gameUrl = appDomain.getGameDomain();
        themeUrl = appDomain.getThemeDomain();
        momentUrl = appDomain.getMomentDomain();
        gatewayUrl = appDomain.getGatewayDomain();
        ImDomain imDomain = configDomain.getImDomain();
        AppInfo appInfo = HttpManager.getInstance(context).getHttpClient().getAppInfo();
        if (appInfo == null || !isGrey(appInfo)) {
            setImUrl(imDomain.getImJoinDomain(), imDomain.getImBackupDomain());
        } else {
            setImUrl(imDomain.getImJoinGreyDomain(), imDomain.getImGreyBackupDomain());
        }
        urlsHasInit = true;
    }
}
