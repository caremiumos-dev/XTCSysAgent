package com.xtc.bigdata.collector;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.xtc.bigdata.collector.config.BehaviorConfig;
import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.collector.config.FilterInfoManager;
import com.xtc.bigdata.collector.encapsulation.entity.BaseAttr;
import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import com.xtc.bigdata.collector.encapsulation.entity.event.ClickEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CountEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.ExceptionEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.MonitorURLEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SearchEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SystemInfoEvent;
import com.xtc.bigdata.collector.encapsulation.interfaces.IEvent;
import com.xtc.bigdata.collector.exception.CrashHandler;
import com.xtc.bigdata.collector.init.InitManager;
import com.xtc.bigdata.collector.interfaces.ICollector;
import com.xtc.bigdata.collector.utils.MD5Coder;
import com.xtc.bigdata.collector.utils.QueueUtils;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.error.ErrorCode;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.UriUtils;
import com.xtc.bigdata.monitor.anr.ANRException;
import com.xtc.log.LogUtil;

import java.util.Map;

/**
 * 行为采集 SDK 的门面（业务方唯一需要直接接触的类）。
 *
 * <p>典型用法：</p>
 * <pre>
 * BehaviorUtil.init(new BehaviorCollector.Builder(application)
 *         .setHostAppId("com.xtc.i3launcher")
 *         .setDeviceType("watch")
 *         .build());
 * </pre>
 *
 * <p>本类自身只做两件事：初始化判定（{@link #isCollectInit}）与转发。
 * 真正的组装在 {@link CollectionManager}，落库在 {@code ShareHelper}。</p>
 *
 * <p>另外还有一个"自动采集点击"的能力：{@link #dealAutoCollect(MotionEvent, Activity)}
 * 在宿主 {@code dispatchTouchEvent} 里被调用，用 down/up 两次命中的 View
 * 是否同一个来判断"这是一次点击"，然后把 View 树路径（或它的 MD5）当功能名上报。</p>
 */
public final class BehaviorCollector implements ICollector {

    private static final String TAG = BehaviorCollector.class.getName();

    /** 采集数据落库用的 provider 目录 Uri。 */
    private Uri CONTENT_URI;

    /** 按下时命中的 View 节点，用于和抬起时的节点比对。 */
    private ClickViewNode actionDownView;

    /** 是否初始化完成。 */
    private boolean isCollectInit;

    /**
     * 自动采集点击时用来描述"命中的是哪个 View"的游标节点。
     *
     * <p>{@link #viewTree} 是路径串（例如 {@code "DecorView.LinearLayout[0].TextView[2]"}），
     * {@link #level} 是当前递归深度，{@link #filterLevelCount} 是"从第几层开始
     * 才把类名拼进路径"的阈值 —— 前几层是各页面都一样的外壳，拼进去只会让
     * 功能名失去区分度。</p>
     */
    private static class ClickViewNode {

        /** 当前节点对应的 View。 */
        public View view;

        /** 路径串。 */
        public String viewTree;

        /** 当前递归深度。 */
        public int level = 0;

        /** 超过这个深度才把类名拼进路径。 */
        public int filterLevelCount = 3;

        /**
         * 构造。
         *
         * @param view     起始 View
         * @param viewTree 起始路径串
         */
        public ClickViewNode(View view, String viewTree) {
            this.view = view;
            this.viewTree = viewTree;
        }
    }

    /**
     * 配置构造器。
     *
     * <p>所有 setter 都直接改 {@link BehaviorConfig} 的字段并返回 {@code this}，
     * 最后 {@link #build()} 把配置交出去 —— 也就是说 {@code build()} 之后再新建
     * 一个 Builder 改的是同一份全局配置。</p>
     */
    public static final class Builder {

        /** 正在编辑的配置（就是全局那份）。 */
        private final BehaviorConfig mBuildConfig;

        /**
         * 构造：顺手把 Application、包名记到 {@link Constants} 与 {@code ContextUtils}。
         *
         * @param application 宿主 Application
         */
        public Builder(Application application) {
            ContextUtils.setContext(application);
            Constants.setPackageName(application.getPackageName());
            this.mBuildConfig = ConfigAgent.getBehaviorConfig();
        }

        /**
         * 交出配置。
         *
         * @return 全局配置实例
         */
        public BehaviorConfig build() {
            return this.mBuildConfig;
        }

        /**
         * 采集总开关。
         *
         * @param enable 是否采集
         * @return this
         */
        public Builder enable(boolean enable) {
            this.mBuildConfig.usable = enable;
            return this;
        }

        /**
         * 是否接管全局崩溃。
         *
         * @param enable 是否接管
         * @return this
         */
        public Builder enableCrash(boolean enable) {
            this.mBuildConfig.crashUsable = enable;
            return this;
        }

        /**
         * 崩溃时是否弹 Toast。
         *
         * @param enable 是否弹
         * @return this
         */
        public Builder enableToastCrash(boolean enable) {
            this.mBuildConfig.crashToastUsable = enable;
            return this;
        }

        /**
         * 是否上报系统崩溃（DropBox）。
         *
         * @param enable 是否上报
         * @return this
         */
        public Builder enableUploadSysCrash(boolean enable) {
            this.mBuildConfig.isUploadSysLog = enable;
            return this;
        }

        /**
         * 是否按 Activity 生命周期统计页面时长。
         *
         * @param enable 是否统计
         * @return this
         */
        public Builder openActivityDurationTrack(boolean enable) {
            this.mBuildConfig.openActivityDurationTrack = enable;
            return this;
        }

        /**
         * 会话超时时间。
         *
         * @param timeoutMillis 超时毫秒
         * @return this
         */
        public Builder sessionTimeout(long timeoutMillis) {
            this.mBuildConfig.sessionTimeout = timeoutMillis;
            return this;
        }

        /**
         * 是否自动采集点击。
         *
         * @param enable 是否开启
         * @return this
         */
        public Builder setAutoCollectEvent(boolean enable) {
            this.mBuildConfig.isAutoCollectEvent = enable;
            return this;
        }

        /**
         * 调试开关（决定大量日志是否输出）。
         *
         * @param debug 是否调试
         * @return this
         */
        public Builder setDebugMode(boolean debug) {
            Constants.setDebug(debug);
            return this;
        }

        /**
         * 设备类型（{@link Constants#PHONE} 或 {@link Constants#WATCH}）。
         *
         * @param deviceType 设备类型
         * @return this
         */
        public Builder setDeviceType(String deviceType) {
            Constants.deviceType = deviceType;
            return this;
        }

        /**
         * 宿主应用 id（用于拼 provider authority）。
         *
         * @param hostAppId 宿主应用 id
         * @return this
         */
        public Builder setHostAppId(String hostAppId) {
            Constants.setHostAppId(hostAppId);
            return this;
        }

        /**
         * 自动采集点击时是否 Toast 提示功能名。
         *
         * @param enable 是否提示
         * @return this
         */
        public Builder setToastAutoCollectEvent(boolean enable) {
            this.mBuildConfig.isToastAutoCollectEvent = enable;
            return this;
        }
    }

    /** 单例持有者。 */
    private static class InstanceHolder {

        /** 唯一实例。 */
        private static final BehaviorCollector mInstance = new BehaviorCollector();

        private InstanceHolder() {
        }
    }

    private BehaviorCollector() {
        this.CONTENT_URI = null;
        this.isCollectInit = false;
    }

    /**
     * 把 watchdog 抓到的 ANR 堆栈当异常事件上报。
     *
     * <p>当前没有调用点（{@code WatchDogs} 的回调走的是自己的 listener），
     * 保留是为了不改变类的形状。</p>
     *
     * @param stackTrace ANR 堆栈
     */
    private void collectANRException(String stackTrace) {
        LogUtil.e(TAG, new ANRException(stackTrace));
        ExceptionEvent exceptionEvent = new ExceptionEvent();
        exceptionEvent.functionName = "WATCHDOG-ANR-ERROR-" + Constants.PACKAGE_NAME;
        exceptionEvent.reason = "WATCHDOG-ANR-ERROR-" + Constants.PACKAGE_NAME;
        exceptionEvent.stack = stackTrace;
        CollectionManager.getInstance().exceptionEvent(exceptionEvent);
    }

    /**
     * 找出触摸点命中的 View 节点。
     *
     * @param motionEvent 触摸事件
     * @param activity    当前 Activity，可为 null
     * @return 命中节点；没有 Activity 或没命中返回 null
     */
    private ClickViewNode findClickView(MotionEvent motionEvent, Activity activity) {
        if (activity != null) {
            return searchClickView(new ClickViewNode(activity.getWindow().getDecorView(), activity.getClass().getName()),
                    motionEvent, 0);
        }
        return null;
    }

    /**
     * 取单例。
     *
     * @return 单例
     */
    public static BehaviorCollector getInstance() {
        return InstanceHolder.mInstance;
    }

    /**
     * 用 Application 完成初始化。
     *
     * @param application 宿主 Application；为 null 时只打日志不初始化
     */
    private void init(Application application) {
        if (application == null) {
            LogUtil.e(TAG, "BFC DA init fail , Application can not be null !!!");
            return;
        }
        initMemoryMonitor();
        this.CONTENT_URI = UriUtils.getContentUri(application);
        InitManager.init();
        this.isCollectInit = true;
    }

    /** 原厂空实现：内存监控预留入口。 */
    private void initAnrMonitor() {
    }

    /** 原厂空实现：内存监控预留入口。 */
    private void initMemoryMonitor() {
    }

    /**
     * 判断触摸点是否落在 View 内（以屏幕坐标算，不是相对父容器）。
     *
     * @param view        View
     * @param motionEvent 触摸事件
     * @return 是否命中（不可见或 GONE 直接 false）
     */
    private boolean isInView(View view, MotionEvent motionEvent) {
        if (view.getVisibility() == 4 || view.getVisibility() == 8) {
            return false;
        }
        int rawX = (int) motionEvent.getRawX();
        int rawY = (int) motionEvent.getRawY();
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return rawX > viewX && rawX < viewX + view.getWidth() && rawY > viewY && rawY < viewY + view.getHeight();
    }

    /**
     * 自顶向下递归找命中的 View 节点（从最后一个子 View 往前找，
     * 也就是优先命中最上层绘制的那个）。
     *
     * @param node       当前游标节点
     * @param motionEvent 触摸事件
     * @param childIndex  当前节点在父容器里的下标
     * @return 命中节点；没有命中返回 null
     */
    private ClickViewNode searchClickView(ClickViewNode node, MotionEvent motionEvent, int childIndex) {
        ClickViewNode foundNode = null;
        View view = node.view;
        if (view == null || !isInView(view, motionEvent) || view.getVisibility() != 0) {
            return null;
        }
        node.level++;
        // 第 2 层如果不是 LinearLayout，说明这层是业务自己的容器，把它也算进有效层级
        if (node.level == 2 && !"LinearLayout".equals(view.getClass().getSimpleName())) {
            node.filterLevelCount++;
        }
        if (node.level > node.filterLevelCount) {
            node.viewTree += "." + view.getClass().getSimpleName() + "[" + childIndex + "]";
        }
        if (view.getTag() != null) {
            String tag = view.getTag().toString();
            // 只有 bigdata_ 前缀的 tag 才是"要采集"的标记，bigdata_ignore 是要排除的
            if (!tag.startsWith("bigdata_") || "bigdata_ignore".equals(tag)) {
                return null;
            }
            return node;
        }
        if (!(view instanceof ViewGroup)) {
            return node;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        for (int index = viewGroup.getChildCount() - 1; index >= 0; index--) {
            node.view = viewGroup.getChildAt(index);
            foundNode = searchClickView(node, motionEvent, index);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return foundNode;
    }

    @Override
    public void appLaunch() {
        if (this.isCollectInit) {
            CollectionManager.getInstance().appLaunch();
        }
    }

    @Override
    public void clickEvent(ClickEvent clickEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().clickEvent(clickEvent);
        }
    }

    @Override
    public void countEvent(CountEvent countEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().countEvent(countEvent);
        }
    }

    @Override
    public void customEvent(CustomEvent customEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().customEvent(customEvent);
        }
    }

    /**
     * 自动采集点击（在宿主 {@code dispatchTouchEvent} 里调用）。
     *
     * <p>判定逻辑：ACTION_DOWN 记下命中节点，ACTION_UP 再算一次，
     * 两次命中的是<b>同一个 View 实例</b>才认为是一次点击 ——
     * 这样滑动、长按拖动都不会被误采。</p>
     *
     * @param motionEvent 触摸事件
     * @param activity    当前 Activity
     */
    public void dealAutoCollect(MotionEvent motionEvent, Activity activity) {
        if (!ContextUtils.isEmpty() && ConfigAgent.getBehaviorConfig().isAutoCollectEvent) {
            if (motionEvent.getAction() != 1) {
                if (motionEvent.getAction() == 0) {
                    this.actionDownView = findClickView(motionEvent, activity);
                    return;
                }
                return;
            }
            if (this.actionDownView != null) {
                ClickViewNode upNode = findClickView(motionEvent, activity);
                if (upNode != null && this.actionDownView.view == upNode.view) {
                    ClickEvent autoClickEvent = new ClickEvent();
                    try {
                        autoClickEvent.activity = activity.getClass().getName();
                        // 路径太长不适合当功能名，取 32 进制 MD5
                        autoClickEvent.functionName = new MD5Coder(32).encode(upNode.viewTree.getBytes("utf-8"));
                        autoClickEvent.moduleDetail = Constants.AUTO_COLLECT;
                    } catch (Exception e) {
                        if (Constants.isDebug) {
                            LogUtil.i(TAG, "获取方法名失败");
                        }
                        autoClickEvent.functionName = upNode.viewTree;
                    }
                    clickEvent(autoClickEvent);
                    if (ConfigAgent.getBehaviorConfig().isToastAutoCollectEvent) {
                        Toast.makeText(ContextUtils.getContext(), autoClickEvent.functionName, 0).show();
                    }
                }
                this.actionDownView = null;
            }
        }
    }

    /**
     * 直接上报任意事件。
     *
     * @param event 事件
     */
    public void event(IEvent event) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().event(event);
        }
    }

    @Override
    public void exceptionEvent(ExceptionEvent exceptionEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().exceptionEvent(exceptionEvent);
        }
    }

    @Override
    public String getBehaviorVersion() {
        return "bigdataClient_d1c2086";
    }

    /**
     * 取当前配置。
     *
     * @return 全局配置
     */
    public BehaviorConfig getConfig() {
        return ConfigAgent.getBehaviorConfig();
    }

    /**
     * 取数据落库 Uri。
     *
     * @return Uri；未初始化时为 null
     */
    public Uri getContentUri() {
        return this.CONTENT_URI;
    }

    /**
     * 取错误码表（键是码，值是中文说明）。
     *
     * @return 错误码表
     */
    public Map<String, String> getErrorCodes() {
        return ErrorCode.getErrorCodes();
    }

    /**
     * 取当前用户信息。
     *
     * @return 用户属性
     */
    public UserAttr getUserInfo() {
        return CollectionManager.getInstance().getUserInfo();
    }

    /**
     * 用指定配置初始化。
     *
     * @param behaviorConfig 配置
     */
    public void init(BehaviorConfig behaviorConfig) {
        ConfigAgent.setBehaviorConfig(behaviorConfig);
        init(ContextUtils.getContext());
    }

    @Override
    public void initUserInfo(UserAttr userAttr) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().initUserInfo(userAttr);
        }
    }

    /**
     * 是否已初始化完成。
     *
     * @return 是否可采集
     */
    public boolean isCollectInit() {
        return this.isCollectInit;
    }

    /** 从磁盘加载功能点过滤配置（丢到后台线程，避免卡主线程）。 */
    public void loadFilterFunctions() {
        QueueUtils.getInstance().post(new Runnable() {
            @Override
            public void run() {
                ConfigAgent.getBehaviorConfig().collectFilterConfig
                        .setFilterFunctionItemMap(FilterInfoManager.readFilterFunctions());
            }
        });
    }

    /**
     * 原厂恒返回 {@code null} 的占位方法。
     *
     * @return 永远是 null
     */
    public String logErrorCode() {
        return ErrorCode.logErrorCode();
    }

    @Override
    public void monitorURLEvent(MonitorURLEvent monitorURLEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().monitorURLEvent(monitorURLEvent);
        }
    }

    /**
     * 宿主自己捕获到崩溃时调用（不走全局 handler 的场景）。
     *
     * <p>只有"初始化完成"<b>且</b>"SDK 没有自己注册全局崩溃处理"时才上报 ——
     * 否则会和 {@code CrashHandler.uncaughtException} 重复上报同一份堆栈。</p>
     *
     * @param throwable 崩溃
     */
    public void onCrash(Throwable throwable) {
        if (this.isCollectInit && !ConfigAgent.getBehaviorConfig().crashUsable) {
            CrashHandler.getInstance().handleExec(throwable);
        }
    }

    /** 进程退出前的收尾。 */
    public void onKillProcess() {
        CollectionManager.getInstance().exit();
    }

    @Override
    public void pageBegin(String pageName) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().pageBegin(pageName);
        }
    }

    @Override
    public boolean pageEnd(String pageName) {
        if (this.isCollectInit) {
            return CollectionManager.getInstance().pageEnd(pageName, null, null, null);
        }
        return false;
    }

    @Override
    public boolean pageEnd(String pageName, String functionName, String moduleDetail) {
        if (this.isCollectInit) {
            return CollectionManager.getInstance().pageEnd(pageName, functionName, moduleDetail, null);
        }
        return false;
    }

    @Override
    public boolean pageEnd(String pageName, String functionName, String moduleDetail, Map<String, String> extend) {
        if (this.isCollectInit) {
            return CollectionManager.getInstance().pageEnd(pageName, functionName, moduleDetail, extend);
        }
        return false;
    }

    @Override
    public void realTime2Upload() {
        CollectionManager.getInstance().realTime2Upload();
    }

    /**
     * 保存功能点过滤配置（后台线程写盘）。
     *
     * @param configJson 配置 JSON
     */
    public void saveFilterFunctionName(final String configJson) {
        QueueUtils.getInstance().post(new Runnable() {
            @Override
            public void run() {
                FilterInfoManager.saveFilterFunctions(configJson);
            }
        });
    }

    /**
     * 保存手表下发的配置串（后台线程写盘）。
     *
     * @param config 形如 {@code "mId|osVer"} 的配置
     */
    public void saveWatchConfig(final String config) {
        QueueUtils.getInstance().post(new Runnable() {
            @Override
            public void run() {
                BaseAttr.saveConfig(DeviceInfo.WATCH_CONFIG_PATH, config);
            }
        });
    }

    @Override
    public void searchEvent(SearchEvent searchEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().searchEvent(searchEvent);
        }
    }

    /**
     * 直接替换配置（不触发重新初始化）。
     *
     * @param behaviorConfig 配置
     */
    public void setConfig(BehaviorConfig behaviorConfig) {
        ConfigAgent.setBehaviorConfig(behaviorConfig);
    }

    @Override
    public void systemInfoEvent(SystemInfoEvent systemInfoEvent) {
        if (this.isCollectInit) {
            CollectionManager.getInstance().systemInfoEvent(systemInfoEvent);
        }
    }

    @Override
    public void uploadSystemCrash() {
        CollectionManager.getInstance().uploadSystemCrash();
    }
}
