package com.xtc.bigdata.common.error;

/**
 * 错误码的"位段"定义。
 *
 * <p>错误码是 8 位十进制字符串，拼法是
 * {@code 项目(02) + 模块(2位) + 异常类型(2位) + 序号(2位)}。
 * 这里只放每一段的取值，具体的码值在 {@link ErrorCode} 里。</p>
 *
 * <p>类与构造器是包级可见的 —— 它只给同包的 {@link ErrorCode} 继承用。</p>
 */
class ErrorCodeStandard {

    /** 数据异常段。 */
    protected static final String DATA_EXCEPTION = "05";

    /** 非法参数异常段。 */
    protected static final String ILLEGAL_ARGUMENT_EXCEPTION = "03";

    /** 初始化异常段。 */
    protected static final String INIT_EXCEPTION = "01";

    /** 模块：缓存。 */
    protected static final String MODULE_CACHE = "02";

    /** 模块：控制。 */
    protected static final String MODULE_CONTROL = "01";

    /** 模块：数据库。 */
    protected static final String MODULE_DB = "03";

    /** 模块：上报。 */
    protected static final String MODULE_REPORT = "04";

    /** 网络异常段。 */
    protected static final String NET_EXCEPTION = "04";

    /** 空指针异常段。 */
    protected static final String NULL_POINTER_EXCEPTION = "06";

    /** 操作异常段。 */
    protected static final String OPERATION_EXCEPTION = "02";

    /** 其它异常段。 */
    protected static final String OTHER_EXCEPTION = "ff";

    /** 项目号：行为采集。 */
    protected static final String PROJECT_BEHAVIOR = "02";

    ErrorCodeStandard() {
    }
}
