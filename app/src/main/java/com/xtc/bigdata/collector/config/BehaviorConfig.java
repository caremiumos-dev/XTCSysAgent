package com.xtc.bigdata.collector.config;

import com.xtc.bigdata.common.constants.Constants;

/**
 * 采集行为配置。
 *
 * <p>全部字段都是 {@code public}，因为它是宿主与采集库之间的"参数包"：
 * 宿主用 {@code BehaviorCollector.Builder} 逐个赋值，库内各处直接读字段。
 * 这种设计省掉了 getter/setter 的样板，但意味着任何一处都能改它，
 * 所以字段默认值必须与原厂一致。</p>
 *
 * <p>默认值要点：{@link #usable} 默认 true（不配置也能采集），
 * {@link #crashUsable} 默认 false（不接管崩溃），
 * {@link #isUploadSysLog} 默认 true（会走系统崩溃上报）。</p>
 */
public class BehaviorConfig {

    /** 采集总开关。 */
    public boolean usable = true;

    /** 是否按 Activity 生命周期自动统计使用时长。 */
    public boolean openActivityDurationTrack = false;

    /** 是否自动采集点击（View 树热区）。 */
    public boolean isAutoCollectEvent = false;

    /** 自动采集点击时是否用 Toast 提示采集到的 functionName。 */
    public boolean isToastAutoCollectEvent = false;

    /** 会话超时时间，单位毫秒。 */
    public long sessionTimeout = Constants.NOTIFY_INTERVAL;

    /** 是否注册全局崩溃捕获。 */
    public boolean crashUsable = false;

    /** 崩溃时是否弹 Toast 提示。 */
    public boolean crashToastUsable = false;

    /** 功能点过滤配置（哪些 functionName 要采集）。 */
    public final CollectFilterConfig collectFilterConfig = new CollectFilterConfig();

    /** 是否上报系统日志（DropBox 崩溃）。 */
    public boolean isUploadSysLog = true;

    /** SD 卡根路径，空串表示"还没确定"，由 FileUtils 惰性填充。 */
    public String sdcardRootPath = "";
}
