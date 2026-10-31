package com.xtc.bigdata.common.error;

import java.util.HashMap;
import java.util.Map;

/**
 * 采集库的错误码表。
 *
 * <p>键是 8 位数字码，值是可以直接写进日志的中文说明 —— 日志里只打码，
 * 排查时再回这张表反查，既省字节又不会把内部文案泄露给外部。</p>
 *
 * <p>{@link #getErrorCodes()} 是懒加载的：第一次调用才建表，
 * 之后返回<b>同一个</b> HashMap 实例。这一点很关键 ——
 * {@code BehaviorCollector.getErrorCodes()} 把它直接交给调用方，
 * 如果每次新建 map，调用方缓存的引用会立刻失效。</p>
 */
public class ErrorCode extends ErrorCodeStandard {

    /** 缓存事件为空，无法上报。 */
    public static final String CACHE_EVENT_NULL_POINTER = "02020601";

    /** 记录功能点时传了非法参数。 */
    public static final String CONTROL_ILLEGAL_ARGUMENT_RECORD_FUNCTION = "02010301";

    /** 崩溃模块初始化失败。 */
    public static final String CONTROL_INIT_CRASH = "02010102";

    /** 初始化异常，无法执行上报。 */
    public static final String CONTROL_INIT_DOREPORT = "02040102";

    /** 入库前未初始化。 */
    public static final String CONTROL_INIT_INSERT = "02010101";

    /** 数据库 provider 初始化异常。 */
    public static final String CONTROL_INIT_PROVIDER = "02030101";

    /** 上报模块初始化失败。 */
    public static final String CONTROL_INIT_REPORT = "02040101";

    /** Application context 为空。 */
    public static final String CONTROL_NULL_POINTER_APPLICATION = "02010606";

    /** BehaviorConfig 为空。 */
    public static final String CONTROL_NULL_POINTER_BEHAVIOR_CONFIG = "02010605";

    /** 缓存配置为空。 */
    public static final String CONTROL_NULL_POINTER_CACHE_CONFIG = "02010601";

    /** EventAttr 为空。 */
    public static final String CONTROL_NULL_POINTER_EVENT_ATTR = "02010602";

    /** 事件本身为空。 */
    public static final String CONTROL_NULL_POINTER_IEVENT = "02010603";

    /** functionName 为空。 */
    public static final String CONTROL_NULL_POINTER_PAGE_FUNCTIONNAME = "02010604";

    /** functionName 已存在。 */
    public static final String CONTROL_PAGE_FUNCTIONNAME_MULTIDEX = "02010203";

    /** pageEnd 早于 pageBegin。 */
    public static final String CONTROL_PAGE_ORDER = "02010201";

    /** recordFunctionEnd 早于 recordFunctionBegin。 */
    public static final String CONTROL_RECORD_FUNCTION_ORDER = "02010202";

    /** bfc-uploadsdk 异常。 */
    public static final String REPORT_BFC_UPLOAD_INIT_EXCEPTION = "02040105";

    /** 创建上传文件失败。 */
    public static final String REPORT_CREATE_FILE_ERROR = "0204ff05";

    /** 创建压缩包失败。 */
    public static final String REPORT_CREATE_ZIP_ERROR = "0204ff06";

    /** 注册 Home 键监听失败。 */
    public static final String REPORT_HOME_KEY_REG = "02040103";

    /** 注销 Home 键监听失败。 */
    public static final String REPORT_HOME_KEY_UNREG = "02040104";

    /** 非定时模式却收到定时上报消息。 */
    public static final String REPORT_ILLEGAL_MODE_START_FIXED_TIME = "0204ff01";

    /** 非推送模式却收到推送上报消息。 */
    public static final String REPORT_ILLEGAL_MODE_START_PUSH = "0204ff02";

    /** 磁盘剩余空间不足。 */
    public static final String REPORT_INSUFFICIENT_REMAINING_SPACE = "0204ff08";

    /** 没有网络。 */
    public static final String REPORT_NO_NET = "02040401";

    /** 当前非 wifi，只在 wifi 下上报。 */
    public static final String REPORT_ONLY_WIFI = "02040402";

    /** 上报参数里的 context 为空。 */
    public static final String REPORT_TASK_CONTEXT_NULL = "02040601";

    /** 该上报模式尚未开启。 */
    public static final String REPORT_THE_MODE_ALREADY_CLOSED = "0204ff03";

    /** 不支持的上报数据类型。 */
    public static final String REPORT_UNKNOW_FILE_TYPE = "0204ff07";

    /** 压缩包不存在。 */
    public static final String REPORT_ZIP_NOT_EXISTS = "02040603";

    /** 压缩包为空。 */
    public static final String REPORT_ZIP_NULL = "02040602";

    /** 懒加载的错误码表。 */
    private static Map<String, String> mErrCodes;

    private ErrorCode() {
    }

    /**
     * 取错误码表（首次调用时建表）。
     *
     * @return 错误码 -> 中文说明
     */
    public static synchronized Map<String, String> getErrorCodes() {
        if (mErrCodes != null) {
            return mErrCodes;
        }
        mErrCodes = new HashMap<String, String>();
        mErrCodes.put(CONTROL_INIT_INSERT, "数据入库失败，请先调用BehaviorCollector.getInstance().init()进行初始化");
        mErrCodes.put(CONTROL_INIT_CRASH, "异常捕获模块初始化失败");
        mErrCodes.put(CONTROL_PAGE_ORDER, "在调用 pawo qgeEnd() 前必须要先调用 pageBegin()");
        mErrCodes.put(CONTROL_RECORD_FUNCTION_ORDER, "在调用 recordFunctionEnd() 前必须要先调用 recordFunctionBegin()");
        mErrCodes.put(CONTROL_PAGE_FUNCTIONNAME_MULTIDEX, "数据入库失败，该functionName已经存在");
        mErrCodes.put(CONTROL_ILLEGAL_ARGUMENT_RECORD_FUNCTION, "记录的时间有问题，计算得到使用时长小于0");
        mErrCodes.put(CONTROL_NULL_POINTER_CACHE_CONFIG, "缓存配置设置失败，cacheConfig不能为空！");
        mErrCodes.put(CONTROL_NULL_POINTER_EVENT_ATTR, "EventAttr参数不能为空");
        mErrCodes.put(CONTROL_NULL_POINTER_IEVENT, "数据入库失败，数据为空");
        mErrCodes.put(CONTROL_NULL_POINTER_PAGE_FUNCTIONNAME, "functionName不能为空");
        mErrCodes.put(CONTROL_NULL_POINTER_BEHAVIOR_CONFIG, "配置信息设置失败，behaviorConfig不能为空");
        mErrCodes.put(CONTROL_NULL_POINTER_APPLICATION, "Application context不能设置为空");
        mErrCodes.put(CACHE_EVENT_NULL_POINTER, "数据数据为空，无法缓存上报");
        mErrCodes.put(CONTROL_INIT_PROVIDER, "数据库provider初始化异常");
        mErrCodes.put(CONTROL_INIT_REPORT, "上报模块初始化失败");
        mErrCodes.put(CONTROL_INIT_DOREPORT, "初始化异常，无法执行上报操作");
        mErrCodes.put(REPORT_HOME_KEY_REG, "注册监听home键失败");
        mErrCodes.put(REPORT_HOME_KEY_UNREG, "注销home键监听失败");
        mErrCodes.put(REPORT_BFC_UPLOAD_INIT_EXCEPTION, "数据上报失败,bfc-uploadsdk异常,数据可能丢失");
        mErrCodes.put(REPORT_NO_NET, "没有网络，无法执行上报操作");
        mErrCodes.put(REPORT_ONLY_WIFI, "当前非wifi网络状态，仅在wifi下上报");
        mErrCodes.put(REPORT_TASK_CONTEXT_NULL, "上传参数有问题!! context = null");
        mErrCodes.put(REPORT_ZIP_NULL, "上传参数有问题!! zipFile = null");
        mErrCodes.put(REPORT_ZIP_NOT_EXISTS, "上传参数有问题!! zipFile文件不存在:");
        mErrCodes.put(REPORT_ILLEGAL_MODE_START_FIXED_TIME, "收到定时上报消息，但当前非定时上报模式，不做处理");
        mErrCodes.put(REPORT_ILLEGAL_MODE_START_PUSH, "收到推送上报消息，但当前非推送上报模式，不做处理");
        mErrCodes.put(REPORT_THE_MODE_ALREADY_CLOSED, "此上报模式尚未开启");
        mErrCodes.put(REPORT_CREATE_FILE_ERROR, "创建上传文件失败");
        mErrCodes.put(REPORT_CREATE_ZIP_ERROR, "创建上传文件压缩失败");
        mErrCodes.put(REPORT_UNKNOW_FILE_TYPE, "上报行为采集数据不支持类型,\u3000taskType:");
        mErrCodes.put(REPORT_INSUFFICIENT_REMAINING_SPACE, "磁盘剩余空间不足,上报失败");
        return mErrCodes;
    }

    /**
     * 原厂恒返回 {@code null} 的占位方法 —— 保留是为了不改变已有的调用点。
     *
     * @return 永远是 null
     */
    public static String logErrorCode() {
        return null;
    }
}
