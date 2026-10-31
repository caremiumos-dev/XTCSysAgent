package com.xtc.bigdata.common.constants;

/**
 * 采集库的全局常量与运行期开关。
 *
 * <p>这里的静态字段是全库共享的"配置单例"，宿主在
 * {@code BehaviorCollector.Builder} 里写入，库内各处读取。</p>
 *
 * <p>注意 {@link #deviceType} 的默认值是 {@code "watch"}，
 * 而 {@link #isDebug} 的默认值是 {@code true} —— 两者都是原厂默认值，
 * 不要"顺手"改成更安全的值，否则日志量与分支行为都会变。</p>
 */
public class Constants {

    /** 自动采集点击事件时写进 moduleDetail 的固定文案。 */
    public static final String AUTO_COLLECT = "自动采集点击事件";

    /** 数据触发时间的格式（本地时间，毫秒精度）。 */
    public static final String DATA_TRIGGER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /** 定时上报的闹钟 action。 */
    public static final String FIXTIME_TRIGER_ACTION = "com.xtc.bigdata.action.fixtimetriger";

    /** 宿主应用 id（一般等于宿主包名），由 Builder 写入。 */
    public static String HOST_APP_ID = null;

    /** 采集模块状态：初始化中。 */
    public static final int INIT = 0;

    /** 上报线程池最大线程数。 */
    public static final int MAX_UPLOAD_THREAD = 10;

    /** 会话超时 / 上报间隔，单位毫秒。 */
    public static final long NOTIFY_INTERVAL = 30000;

    /** 宿主包名，由 Builder 写入。 */
    public static String PACKAGE_NAME = null;

    /** 设备类型：手机。 */
    public static final String PHONE = "phone";

    /** 上传状态：上传中。 */
    public static final int UPLOADING = 1;

    /** 上传状态：失败。 */
    public static final int UPLOAD_FAIL = 2;

    /** 单次上报最大记录数。 */
    public static final int UPLOAD_MAX_RECORDS = 100;

    /** 上传状态：成功。 */
    public static final int UPLOAD_SUCCESS = 3;

    /** 版本名，与 {@code BuildConfig.VERSION_NAME} 同值。 */
    public static final String VERSION_NAME = "bigdataClient_d1c2086";

    /** 设备类型：手表。 */
    public static final String WATCH = "watch";

    /** 当前设备类型，默认手表。 */
    public static String deviceType = "watch";

    /** 运行期调试开关，默认开启（原厂值）。 */
    public static boolean isDebug = true;

    /**
     * 设置运行期调试开关。
     *
     * @param debug 是否输出调试日志
     */
    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    /**
     * 设置宿主应用 id。
     *
     * @param hostAppId 宿主应用 id
     */
    public static void setHostAppId(String hostAppId) {
        HOST_APP_ID = hostAppId;
    }

    /**
     * 设置宿主包名。
     *
     * @param packageName 宿主包名
     */
    public static void setPackageName(String packageName) {
        PACKAGE_NAME = packageName;
    }
}
