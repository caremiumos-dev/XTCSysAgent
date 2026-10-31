package com.xtc.utils.system;

/**
 * 全系统用到的 {@code SystemProperties} 键名常量。
 *
 * <p>原厂类名为单字母 {@code o}，字段是 {@code a}..{@code al} 的混淆名。
 * 这里按属性值语义还原成 {@code PROP_xxx}，值一个字都没改 ——
 * 这些字符串是与 ROM 的契约。</p>
 */
public class SystemConstants {

    /** 手表丢失状态。 */
    public static final String PROP_WATCH_LOSS_STATUS = "persist.sys.watchlossstatus";
    /** 当前表盘文件名。 */
    public static final String PROP_CURRENT_CLOCK_FILE = "persist.sys.currentclockfile";
    /** 充电是否可用。 */
    public static final String PROP_CHARGE_USABLE = "persist.sys.charge.usable";
    /** 解锁是否隐藏。 */
    public static final String PROP_UNLOCK_HIDDEN = "persist.sys.unlock.hidden";
    /** 蓝牙拒接来电。 */
    public static final String PROP_BT_REJECT_CALL = "persist.sys.bt.rejectcall";
    /** 体感开关状态。 */
    public static final String PROP_MOTION_SWITCH_STATE = "persist.sys.motion.switch.state";
    /** 是否发生过 dump。 */
    public static final String PROP_OCCUR_DUMP = "persist.sys.occurdump";
    /** 应用崩溃标记。 */
    public static final String PROP_APP_CRASH = "persist.sys.appcrash";
    /** 滑动清除。 */
    public static final String PROP_SWIPE_DISMISS = "persist.sys.SwipeDismiss";
    /** 通话结束。 */
    public static final String PROP_CALL_END = "persist.sys.call.end";
    /** 使用时长限制。 */
    public static final String PROP_DURE_LIMITS = "persist.sys.dure.limits";
    /** 待机显示。 */
    public static final String PROP_STANDBY_DISPLAY = "persist.sys.standby.display";
    /** 状态栏。 */
    public static final String PROP_STATUS_BAR = "persist.sys.status_bar";
    /** 省电模式。 */
    public static final String PROP_POWERSAVE_ENABLE = "persist.sys.powersave.enable";
    /** 关闭 WiFi。 */
    public static final String PROP_CLOSE_WIFI = "persist.sys.close_wifi";
    /** 关闭 GPS。 */
    public static final String PROP_CLOSE_GPS = "persist.sys.close_gps";
    /** GPS 采集 WiFi。 */
    public static final String PROP_GPS_COLLECT_WIFI = "persist.sys.gps_collect_wifi";
    /** 自动接听。 */
    public static final String PROP_AUTO_ANSWER = "persist.sys.autoanswer";
    /** 拒接陌生人来电。 */
    public static final String PROP_REFUSE_STRANGER_CALL = "persist.sys.refusestrangercall";
    /** 上报通话位置。 */
    public static final String PROP_REPORT_CALL_POSITION = "persist.sys.reportcallposition";
    /** 绑定状态。 */
    public static final String PROP_BIND_STATUS = "persist.sys.bindstatus";
    /** 上课禁用状态。 */
    public static final String PROP_CLASS_MODE_STATUS = "persist.sys.classmodestatus";
    /** 静音模式。 */
    public static final String PROP_QUIETNESS_MODE = "persist.sys.quietnessmode";
    /** 关闭 BLE。 */
    public static final String PROP_CLOSE_BLE = "persist.sys.close_ble";
    /** 蓝牙关机。 */
    public static final String PROP_BT_SHUTDOWN = "persist.sys.bt.shutdown";
    /** 蓝牙告警。 */
    public static final String PROP_BT_WARN = "persist.sys.bt.warn";
    /** WiFi 可见状态。 */
    public static final String PROP_WIFI_VISIBLE_STATUS = "persist.sys.wifi_visible_status";
    /** 开关机。 */
    public static final String PROP_POWER_ON_OFF = "persist.sys.power_on_off";
    /** 定位设置。 */
    public static final String PROP_SETTING_LOCATION = "persist.sys.setting_location";
    /** 省电设置。 */
    public static final String PROP_SETTING_SAVEPOWER = "persist.sys.setting_savepower";
    /** 移动数据控制。 */
    public static final String PROP_MOBILE_DATA_CONTROL = "persist.sys.mobliedata.control";
    /** 移动数据时间。 */
    public static final String PROP_MOBILE_DATA_TIME = "persist.sys.mobliedata.time";
    /** 未知来源安装。 */
    public static final String PROP_UNKNOWN_SOURCES = "persist.sys.unknownsources";
    /** WiFi 连接开关。 */
    public static final String PROP_WIFI_CONNECT_ENABLE = "persist.sys.wificonnect.enable";
    /** 蜂窝网络省电。 */
    public static final String PROP_CELLULAR_NETEASE = "persist.sys.cellular.netease";
    /** 摇一摇音乐。 */
    public static final String PROP_SHAKE_MUSIC = "persist.sys.shakemusic";
    /** IM 状态。 */
    public static final String PROP_IM_STATUS = "persist.sys.im.status";
    /** 蜂窝网络阈值。 */
    public static final String PROP_CELLULAR_THRESHOLD = "persist.sys.cellular.threshold";
    /** 微信更多表情。 */
    public static final String PROP_WEICHAT_MORE_EMOJI = "persist.sys.weichat.more.emoji";
    /** 运动 PK。 */
    public static final String PROP_SPORT_PK_TCT = "persist.sys.sport.pk.tct";
    /** modem 日志级别。 */
    public static final String PROP_LOG_TAG_MODEM = "log.tag.modem";
    /** 恢复出厂日志级别。 */
    public static final String PROP_LOG_TAG_FACTORYRESET = "log.tag.factoryreset";
    /** appdata 日志级别。 */
    public static final String PROP_LOG_TAG_APPDATA = "log.tag.appdata";
    /** btWifi 日志级别。 */
    public static final String PROP_LOG_TAG_BT_WIFI = "log.tag.btwifi";
    /** bugreport 日志级别。 */
    public static final String PROP_LOG_TAG_XTC_BUGREPORT = "log.tag.xtc_bugreport";
    /** 当前软件版本号。 */
    public static final String PROP_SOFT_VERSION = "ro.product.current.softversion";
    /** 产品型号。 */
    public static final String PROP_PRODUCT_MODEL = "ro.product.model";
    /** 产品内部型号。 */
    public static final String PROP_PRODUCT_INNER_MODEL = "ro.product.innermodel";
    /** 默认移动数据。 */
    public static final String PROP_DEFAULT_MOBILE_DATA = "ro.com.android.mobiledata";
    /** 手表颜色。 */
    public static final String PROP_WATCH_COLOR = "ro.xtcwatch.color";
    /** WiFi 自动连接禁用。 */
    public static final String PROP_WIFI_AUTOCONNECT_DISABLE = "wifi.autoconnect.disable";
    /** WiFi 休眠。 */
    public static final String PROP_WIFI_IS_SLEEP = "wifi.is.sleep";
    /** IM 初始化开关。 */
    public static final String PROP_IM_INIT_ENABLE = "sys.im.init.enable";
    /** xtc hacker 服务。 */
    public static final String PROP_XTC_HACKER = "persist.service.xtc_hacker";

    /** LOG_HACKER_XTC_BUG_REPORT 的开关属性。原厂字段名为 {@code S}。 */
    public static final String PROP_LOG_HACKER_XTC_BUG_REPORT = "log.tag.xtc_bugreport";
    /** GSM 信号强度。 */
    public static final String PROP_GSM_SIGNAL_STRENGTH = "gsm.signal.strength";
    /** 基带版本。 */
    public static final String PROP_GSM_BASEBAND = "gsm.version.baseband";
    /** 绑定号（暗码校验用）。 */
    public static final String PROP_BIND_NUMBER = "ro.boot.bindnumber";
    /** 默认网络类型。 */
    public static final String PROP_DEFAULT_NETWORK = "ro.telephony.default_network";
    /** 编译时间。 */
    public static final String PROP_BUILD_DATE_UTC = "ro.build.date.utc";
    /** 内网服务器。 */
    public static final String PROP_SERVER_INNER = "persist.sys.serverinner";
    /** 地区。 */
    public static final String PROP_LOCALE_REGION = "ro.product.locale.region";
    /** 语言。 */
    public static final String PROP_LOCALE = "ro.product.locale";
    /** 硬件平台（包内可见，不对外）。 */
    static final String PROP_HARDWARE = "ro.hardware";
    /** 设置项 keyId。 */
    public static final String PROP_SETTING_KEY_ID = "persist.sys.setting.keyid";

    private SystemConstants() {
    }

    /**
     * 硬件代号常量。
     *
     * <p>原厂这里是 {@code o.a} 与 {@code o.a.a}（因为重名被反编译重命名成
     * {@code InterfaceC0026a}）。按用途还原为两层：{@link Hardware} 与
     * {@link Project}。</p>
     */
    public interface Hardware {
        /** 硬件代号。取值就是屏幕模组代号。 */
        interface Id {
            String DI01 = "DI01";
            String F2 = "F2";
            String F3 = "F3";
            String F4 = "F4";
            String GLI17 = "GLI17";
            String IA = "IA";
            String IB = "IB";
            String I12 = "I12";
            String I13 = "I13";
            String I13C = "I13C";
            String I16 = "I16";
            String I17 = "I17";
            String I18 = "I18";
            String I2C = "I2C";
            String I3 = "I3";
            String I6 = "I6";
            String I8 = "I8";
            String IDI13 = "IDI13";
            String IDI6C = "IDI6C";
            String Y01 = "Y01";
            String Y02 = "Y02";
            String Y1A = "Y1A";
            String UNKNOWN = "";
        }
    }

    /** 项目代号。 */
    public interface Project {
        String XTC_Z3 = "XTC_Z3";
        String XTC_Z2Y = "XTC_Z2y";
        String XTC_Z5 = "XTC_Z5";
        String XTC_V1 = "XTC V1";
    }
}
