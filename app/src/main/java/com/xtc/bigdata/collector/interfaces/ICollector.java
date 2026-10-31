package com.xtc.bigdata.collector.interfaces;

import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import com.xtc.bigdata.collector.encapsulation.entity.event.ClickEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CountEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.ExceptionEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.MonitorURLEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SearchEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SystemInfoEvent;

import java.util.Map;

/**
 * 采集器对外暴露的能力。
 *
 * <p>{@code BehaviorCollector} 与 {@code CollectionManager} 都实现它 ——
 * 前者是"给业务方用的门面"（带初始化判断），后者是"真正干活的那层"。</p>
 */
public interface ICollector {

    /** 上报一次 App 启动事件。 */
    void appLaunch();

    /**
     * 上报点击事件。
     *
     * @param clickEvent 事件
     */
    void clickEvent(ClickEvent clickEvent);

    /**
     * 上报计数事件。
     *
     * @param countEvent 事件
     */
    void countEvent(CountEvent countEvent);

    /**
     * 上报自定义事件。
     *
     * @param customEvent 事件
     */
    void customEvent(CustomEvent customEvent);

    /**
     * 上报异常事件。
     *
     * @param exceptionEvent 事件
     */
    void exceptionEvent(ExceptionEvent exceptionEvent);

    /**
     * 取采集库版本号。
     *
     * @return 版本号
     */
    String getBehaviorVersion();

    /**
     * 初始化用户信息。
     *
     * @param userAttr 用户属性
     */
    void initUserInfo(UserAttr userAttr);

    /**
     * 上报 URL 监控事件。
     *
     * @param monitorURLEvent 事件
     */
    void monitorURLEvent(MonitorURLEvent monitorURLEvent);

    /**
     * 记录页面进入。
     *
     * @param pageName 页面名
     */
    void pageBegin(String pageName);

    /**
     * 记录页面切出（不带功能名）。
     *
     * @param pageName 页面名
     * @return 是否有对应的进入记录
     */
    boolean pageEnd(String pageName);

    /**
     * 记录页面切出。
     *
     * @param pageName     页面名
     * @param functionName 功能点名
     * @param moduleDetail 模块细节
     * @return 是否有对应的进入记录
     */
    boolean pageEnd(String pageName, String functionName, String moduleDetail);

    /**
     * 记录页面切出（带扩展）。
     *
     * @param pageName     页面名
     * @param functionName 功能点名
     * @param moduleDetail 模块细节
     * @param extend       扩展
     * @return 是否有对应的进入记录
     */
    boolean pageEnd(String pageName, String functionName, String moduleDetail, Map<String, String> extend);

    /** 触发一次实时上报。 */
    void realTime2Upload();

    /**
     * 上报搜索事件。
     *
     * @param searchEvent 事件
     */
    void searchEvent(SearchEvent searchEvent);

    /**
     * 上报系统信息事件。
     *
     * @param systemInfoEvent 事件
     */
    void systemInfoEvent(SystemInfoEvent systemInfoEvent);

    /** 上报系统崩溃日志。 */
    void uploadSystemCrash();
}
