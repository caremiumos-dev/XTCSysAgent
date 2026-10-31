package com.xtc.bigdata.common.db.constant;

import android.provider.BaseColumns;

/**
 * 行为库的列名、表路径与 ContentProvider 契约。
 *
 * <p>这些字符串是<b>跨进程的二进制契约</b>：宿主进程按列名读 ContentValues，
 * 服务端按列名解析上报 JSON。少一个字符就读不到数据，所以一个字都不能改。</p>
 *
 * <p>注意 {@code COLUMN_EA_SESSIONID} 在 EventAttr 表里，而
 * {@code COLUMN_OA_CHANNELID}（渠道号）挂在 OtherAttr 表里 —— 这是原厂的
 * 分表方式，不是笔误。</p>
 */
public class Columns implements BaseColumns {

    /** 路径段：App 退出通知。 */
    public static final String APP_EXIT = "/app_exit";

    /** 路径段：App 启动通知。 */
    public static final String APP_LAUNCH = "/app_launch";

    /** 应用属性列：appId。 */
    public static final String COLUMN_AA_APPID = "appId";

    /** 应用属性列：应用版本。 */
    public static final String COLUMN_AA_APPVER = "appVer";

    /** 应用属性列：模块名。 */
    public static final String COLUMN_AA_MODULENAME = "moduleName";

    /** 应用属性列：包名。 */
    public static final String COLUMN_AA_PACKAGENAME = "packageName";

    /** 事件属性列：事件类型。 */
    public static final String COLUMN_EA_EVENTTYPE = "eventType";

    /** 事件属性列：功能点名。 */
    public static final String COLUMN_EA_FUNCTIONNAME = "functionName";

    /** 事件属性列：模块细节。 */
    public static final String COLUMN_EA_MODULEDETAIL = "moduleDetail";

    /** 事件属性列：页面名。 */
    public static final String COLUMN_EA_PAGE = "page";

    /** 事件属性列：会话 id。 */
    public static final String COLUMN_EA_SESSIONID = "sessionId";

    /** 事件属性列：触发时间。 */
    public static final String COLUMN_EA_TTIME = "trigTime";

    /** 事件属性列：触发值。 */
    public static final String COLUMN_EA_TVALUE = "trigValue";

    /** 机器属性列：内部型号（注意列名与常量名不同，列名是 innerModel）。 */
    public static final String COLUMN_INNER_MODEL = "innerModel";

    /** 机器属性列：品牌。 */
    public static final String COLUMN_MA_BRAND = "brand";

    /** 机器属性列：设备名。 */
    public static final String COLUMN_MA_DEVICE = "devName";

    /** 机器属性列：机器 id。 */
    public static final String COLUMN_MA_MID = "mId";

    /** 机器属性列：系统版本。 */
    public static final String COLUMN_MA_OSVERSION = "osVer";

    /** 其它属性列：渠道号。 */
    public static final String COLUMN_OA_CHANNELID = "channelId";

    /** 其它属性列：数据采集版本。 */
    public static final String COLUMN_OA_DAVER = "daVer";

    /** 其它属性列：扩展字段。 */
    public static final String COLUMN_OA_EXTEND = "extend";

    /** 其它属性列：扩展判定。 */
    public static final String COLUMN_OA_EXTENDJUDGMENT = "extendJudgment";

    /** 其它属性列：imei。 */
    public static final String COLUMN_OA_IMEI = "imei";

    /** 其它属性列：路由器 mac。 */
    public static final String COLUMN_OA_ROUTERMAC = "routerMac";

    /** 其它属性列：状态。 */
    public static final String COLUMN_OA_STATUS = "status";

    /** 机器属性列：是否 root。 */
    public static final String COLUMN_ROOTED = "rooted";

    /** 用户属性列：手机号。 */
    public static final String COLUMN_UA_PHONENUM = "phoneNum";

    /** 用户属性列：用户扩展。 */
    public static final String COLUMN_UA_USEREXTEND = "userExtend";

    /** 用户属性列：用户 id。 */
    public static final String COLUMN_UA_USERID = "userId";

    /** 用户属性列：用户名。 */
    public static final String COLUMN_UA_USERNAME = "userName";

    /** 目录型 MIME。 */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/hb.android.userbehaviors";

    /** 单条型 MIME。 */
    public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/hb.android.userbehavior";

    /** UriMatcher：目录。 */
    public static final int DIR = 1;

    /** 表路径段。 */
    public static final String DIR_PATH = "userbehavior";

    /** UriMatcher：单条。 */
    public static final int ITEM = 2;

    /** 单条路径模板。 */
    public static final String ITEM_PATH = "userbehavior/#";

    /** 路径段：按下 Home 键。 */
    public static final String PRESS_HOME_KEY = "/press_home_key";

    /** 路径段：查询数据。 */
    public static final String QUERY_DATA_KEY = "/query_data";

    /** 路径段：实时上报。 */
    public static final String REAL_TIME = "/real_time";

    /** 路径段：上报数据。 */
    public static final String REPORT_DATA_KEY = "/report_data";

    private Columns() {
    }
}
