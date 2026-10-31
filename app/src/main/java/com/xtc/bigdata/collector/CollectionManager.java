package com.xtc.bigdata.collector;

import android.os.SystemClock;

import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.encapsulation.BaseAttrManager;
import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import com.xtc.bigdata.collector.encapsulation.entity.event.AppLaunchEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.ClickEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CountEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomAttrEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.ExceptionEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.MonitorURLEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.PageEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SearchEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SystemInfoEvent;
import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.collector.encapsulation.interfaces.IEvent;
import com.xtc.bigdata.collector.interfaces.ICollector;
import com.xtc.bigdata.collector.utils.QueueUtils;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.error.ErrorCode;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.log.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 采集的调度层：所有事件最终都在这里被丢进 {@link QueueUtils}。
 *
 * <p>它做三件事：判开关（{@code usable}）、判上下文、判事件非空，
 * 任一不满足就只打日志不落库 —— 这就是"入库失败"各类错误码的出处。</p>
 *
 * <p>页面时长统计用 {@link SystemClock#elapsedRealtime()}（不受改系统时间影响），
 * key 是页面名，所以同名页面不能重入（重复 pageBegin 会被忽略）。</p>
 */
public class CollectionManager implements ICollector {

    /** pageEnd 不带功能名时使用的固定功能名。 */
    private static final String FUNCTION_NAME_APP_LIFE_CYCLE_ON_ACTIVITY_PAUSED = "AppLifeCycle_onActivityPaused";

    private static final String TAG = "CollectionManager";

    /** 单例。 */
    private static CollectionManager mInstance;

    /** 每个页面的进入时刻（elapsedRealtime）。 */
    private final Map<String, Long> pageBeginTimeMap = new HashMap<String, Long>();

    private CollectionManager() {
    }

    /**
     * 用现成的事件属性构造一个 {@link CustomAttrEvent}。
     *
     * <p>原厂保留的通用入口，当前包内没有调用点（各事件都有专属类）。</p>
     *
     * @param eventType   事件类型
     * @param unused      预留给业务方的附加串
     * @param eventAttr   事件属性
     * @param extendAttrs 扩展属性
     * @return 事件；{@code eventAttr} 为 null 时返回 null
     */
    private CustomAttrEvent createCustomAttrEvent(int eventType, String unused, EventAttr eventAttr, IAttr... extendAttrs) {
        if (isAttrEmpty(eventAttr)) {
            LogUtil.w(TAG, ErrorCode.CONTROL_NULL_POINTER_EVENT_ATTR);
            return null;
        }
        eventAttr.setEventType(eventType);
        CustomAttrEvent customAttrEvent = new CustomAttrEvent();
        customAttrEvent.eventAttr = eventAttr;
        customAttrEvent.addAttr(extendAttrs);
        return customAttrEvent;
    }

    /**
     * 取单例（双重检查）。
     *
     * @return 单例
     */
    public static CollectionManager getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        synchronized (CollectionManager.class) {
            if (mInstance == null) {
                mInstance = new CollectionManager();
            }
        }
        return mInstance;
    }

    /**
     * 把事件丢进落库队列（带三层校验）。
     *
     * @param event 事件
     */
    private void insert(IEvent event) {
        if (!ConfigAgent.getBehaviorConfig().usable) {
            if (Constants.isDebug) {
                LogUtil.i(TAG, "EnableUpload = false , 不需要 采集");
            }
        } else if (ContextUtils.isEmpty()) {
            LogUtil.w(TAG, ErrorCode.CONTROL_INIT_INSERT);
        } else if (event == null) {
            LogUtil.w(TAG, ErrorCode.CONTROL_NULL_POINTER_IEVENT);
        } else {
            QueueUtils.getInstance().add(event);
        }
    }

    /**
     * 属性是否为空。
     *
     * @param attr 属性
     * @return 是否为 null
     */
    private boolean isAttrEmpty(IAttr attr) {
        return attr == null;
    }

    @Override
    public void appLaunch() {
        insert(new AppLaunchEvent());
    }

    @Override
    public void clickEvent(ClickEvent clickEvent) {
        insert(clickEvent);
    }

    @Override
    public void countEvent(CountEvent countEvent) {
        insert(countEvent);
    }

    @Override
    public void customEvent(CustomEvent customEvent) {
        insert(customEvent);
    }

    /**
     * 直接入队任意事件。
     *
     * @param event 事件
     */
    public void event(IEvent event) {
        insert(event);
    }

    @Override
    public void exceptionEvent(ExceptionEvent exceptionEvent) {
        insert(exceptionEvent);
    }

    /** 原厂空实现：进程退出时没有额外动作。 */
    public void exit() {
    }

    @Override
    public String getBehaviorVersion() {
        return "bigdataClient_d1c2086";
    }

    /**
     * 取当前用户信息。
     *
     * @return 用户属性
     */
    public UserAttr getUserInfo() {
        return BaseAttrManager.getInstance().getUserAttr();
    }

    @Override
    public void initUserInfo(UserAttr userAttr) {
        BaseAttrManager.getInstance().setUserAttr(userAttr);
    }

    @Override
    public void monitorURLEvent(MonitorURLEvent monitorURLEvent) {
        insert(monitorURLEvent);
    }

    @Override
    public void pageBegin(String pageName) {
        if (this.pageBeginTimeMap.containsKey(pageName)) {
            return;
        }
        this.pageBeginTimeMap.put(pageName, Long.valueOf(SystemClock.elapsedRealtime()));
    }

    @Override
    public boolean pageEnd(String pageName) {
        return pageEnd(pageName, FUNCTION_NAME_APP_LIFE_CYCLE_ON_ACTIVITY_PAUSED, null, null);
    }

    @Override
    public boolean pageEnd(String pageName, String functionName, String moduleDetail) {
        return pageEnd(pageName, functionName, moduleDetail, null);
    }

    @Override
    public boolean pageEnd(String pageName, String functionName, String moduleDetail, Map<String, String> extend) {
        if (!this.pageBeginTimeMap.containsKey(pageName)) {
            LogUtil.w(TAG, ErrorCode.CONTROL_PAGE_ORDER);
            return false;
        }
        long duration = SystemClock.elapsedRealtime() - this.pageBeginTimeMap.remove(pageName).longValue();
        LogUtil.d(TAG, "界面[" + pageName + "]停留时间：" + duration + "ms");
        PageEvent pageEvent = new PageEvent();
        pageEvent.activityName = pageName;
        pageEvent.functionName = functionName;
        pageEvent.moduleDetail = moduleDetail;
        pageEvent.extend = extend;
        pageEvent.duaring = String.valueOf(duration);
        insert(pageEvent);
        return true;
    }

    @Override
    public void realTime2Upload() {
        ShareHelper.getInstance().realTimeNotify();
    }

    @Override
    public void searchEvent(SearchEvent searchEvent) {
        insert(searchEvent);
    }

    @Override
    public void systemInfoEvent(SystemInfoEvent systemInfoEvent) {
        insert(systemInfoEvent);
    }

    /** 原厂空实现：系统崩溃上报由 DropBox 扫描负责，这里不做事。 */
    @Override
    public void uploadSystemCrash() {
    }
}
