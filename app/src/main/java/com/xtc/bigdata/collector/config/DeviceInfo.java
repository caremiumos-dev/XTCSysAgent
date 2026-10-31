package com.xtc.bigdata.collector.config;

/**
 * 采集库在磁盘/SharedPreferences 上使用的键名常量。
 *
 * <p>这些字符串是与"上一版本遗留数据"以及"服务端下发配置"的契约：
 * 改了就读不到老的 base_attr / userInfo，用户会被当成新设备。</p>
 */
public class DeviceInfo {

    /** 设备基信息落盘文件名。 */
    public static final String BASE_ATTR_FILE_PATH = "BaseAttrInfo.txt";

    /** 采集库在 SD 卡上的私有目录名。 */
    public static final String BIG_DATA_DIR = "/BigData";

    /** 采集到的 watchId（SP 键）。 */
    public static final String COLLECT_WATCH_ID = "collect_watchId";

    /** 崩溃指纹（SP 键），用于 5 秒内同类崩溃去重。 */
    public static final String CRASH_INFO = "crashInfo";

    /** 手机侧渠道号（SP 键）。 */
    public static final String PHONE_CHANNEL_ID = "phone_channelId";

    /** 用户信息（SP 键）。 */
    public static final String USERINFO = "userInfo";

    /** 手表绑定号（SP 键）。 */
    public static final String WATCH_BIND_NUMBER = "watch_bindNumber";

    /** 手表配置文件名。 */
    public static final String WATCH_CONFIG_PATH = "/config.txt";

    /** 手表系统版本（SP 键）。 */
    public static final String WATCH_OS_VER = "watch_osVer";

    private DeviceInfo() {
    }
}
