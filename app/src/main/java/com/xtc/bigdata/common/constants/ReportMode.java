package com.xtc.bigdata.common.constants;

/**
 * 上报模式编号。
 *
 * <p>这六个数字会被写进上报请求的 {@code mode} 字段，服务端按编号分流。
 * 数值不连续（0/1/2/3/4/5/6 里 5 是退出上报、6 是启动上报）是原厂设计。</p>
 */
public class ReportMode {

    /** 退出时上报。 */
    public static final int MODE_EXIT = 5;

    /** 定时上报。 */
    public static final int MODE_FIXTIME = 0;

    /** 启动时上报。 */
    public static final int MODE_LAUNCH = 6;

    /** 周期上报。 */
    public static final int MODE_PERIOD = 1;

    /** 推送触发上报。 */
    public static final int MODE_PUSH = 2;

    /** 数量触发上报。 */
    public static final int MODE_QUANTITY = 3;

    /** 实时上报。 */
    public static final int MODE_REAL_TIME = 4;

    private ReportMode() {
    }
}
