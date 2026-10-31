package com.xtc.bigdata.common.constants;

/**
 * 事件类型编号与它们的中文名。
 *
 * <p>编号是写入数据库 {@code eventType} 列的值，也是上报协议里的类型字段，
 * <b>绝对不能重排</b>：8 是计次、7 是自定义、9 是异常，中间没有连续性可言。</p>
 *
 * <p>{@link #getEventNameByEventType(int)} 里 11/12/13 落到 {@code default}
 * 分支返回空串，这是原厂行为（11 是 DropBox 异常，故意不给名字）。</p>
 */
public class EType {

    /** 功能点：URL 监控。 */
    public static final String FUNCTION_MONITOR_URL = "URL监控";

    /** 事件名：界面进入。 */
    public static final String NAME_ACTIVITY_IN = "界面进入事件类型";

    /** 事件名：界面退出。 */
    public static final String NAME_ACTIVITY_OUT = "界面退出事件类型";

    /** 事件名：应用异常（写进 functionName 的固定值）。 */
    public static final String NAME_APP_EXCEPTION = "应用异常";

    /** 事件名：APP 调出前台。 */
    public static final String NAME_APP_LAUNCH = "APP调出前台事件";

    /** 事件名：APP 退出。 */
    public static final String NAME_APP_OUT = "APP退出事件";

    /** 事件名：计数事件。 */
    public static final String NAME_CLICK = "计次事件";

    /** 事件名：计数事件（TYPE_COUNT）。 */
    public static final String NAME_COUNT = "计数事件";

    /** 事件名：自定义事件。 */
    public static final String NAME_CUSTOM = "自定义事件";

    /** 事件名：使用时长。 */
    public static final String NAME_DURATION = "使用时长事件";

    /** 事件名：异常信息。 */
    public static final String NAME_EXCEPTION = "异常信息";

    /** 事件名：功能点结束。 */
    public static final String NAME_FUNC_END = "功能点结束事件";

    /** 事件名：URL 监控事件。 */
    public static final String NAME_MONITOR_URL = "URL监控事件";

    /** 事件名：页面切出。 */
    public static final String NAME_PAGE = "页面切出事件";

    /** 事件名：搜索事件。 */
    public static final String NAME_SEARCH = "搜索事件";

    /** 事件名：系统信息事件。 */
    public static final String NAME_SYSTEM_INFO = "系统信息事件";

    /** 事件名：系统异常。 */
    public static final String NAME_SYS_EXCEPTION = "系统异常";

    /** 类型：界面进入。 */
    public static final int TYPE_ACTIVITY_IN = 1;

    /** 类型：界面退出。 */
    public static final int TYPE_ACTIVITY_OUT = 2;

    /** 类型：APP 进入前台。 */
    public static final int TYPE_APP_IN = 3;

    /** 类型：APP 退出。 */
    public static final int TYPE_APP_OUT = 4;

    /** 类型：计次。 */
    public static final int TYPE_CLICK = 5;

    /** 类型：计数。 */
    public static final int TYPE_COUNT = 8;

    /** 类型：自定义。 */
    public static final int TYPE_CUSTOM = 7;

    /** 类型：DropBox 异常。 */
    public static final int TYPE_DROPBOX_Exception = 11;

    /** 类型：异常。 */
    public static final int TYPE_EXCEPTION = 9;

    /** 类型：URL 监控。 */
    public static final int TYPE_MONITOR_URL = 14;

    /** 类型：搜索。 */
    public static final int TYPE_SEARCH = 6;

    /** 类型：系统信息。 */
    public static final int TYPE_SYSTEM_INFO = 10;

    private EType() {
    }

    /**
     * 按事件类型取中文名。
     *
     * @param eventType 事件类型编号
     * @return 中文名；未知类型返回空串
     */
    public static String getEventNameByEventType(int eventType) {
        switch (eventType) {
            case TYPE_ACTIVITY_IN:
                return NAME_ACTIVITY_IN;
            case TYPE_ACTIVITY_OUT:
                return NAME_ACTIVITY_OUT;
            case TYPE_APP_IN:
                return NAME_APP_LAUNCH;
            case TYPE_APP_OUT:
                return NAME_APP_OUT;
            case TYPE_CLICK:
                return NAME_CLICK;
            case TYPE_SEARCH:
                return NAME_SEARCH;
            case TYPE_CUSTOM:
                return NAME_CUSTOM;
            case TYPE_COUNT:
                return NAME_COUNT;
            case TYPE_EXCEPTION:
                return NAME_EXCEPTION;
            case TYPE_SYSTEM_INFO:
                return NAME_SYSTEM_INFO;
            case TYPE_MONITOR_URL:
                return NAME_MONITOR_URL;
            case TYPE_DROPBOX_Exception:
            default:
                return "";
        }
    }
}
