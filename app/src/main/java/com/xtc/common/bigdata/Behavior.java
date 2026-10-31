package com.xtc.common.bigdata;

import android.content.Context;
import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import java.util.HashMap;
import java.util.Map;

/**
 * 埋点行为的抽象。
 *
 * <p>目前只有 {@link BbkBehavior} 一个实现（步步高自有通道），
 * 保留这层抽象是为了以后能接友盟等第三方通道。</p>
 */
public abstract class Behavior {

    /** 点击事件。 */
    public abstract void clickEvent(Context context, String functionName, String moduleDetail,
                                    HashMap<String, String> extend);

    /** 计数事件。 */
    public abstract void countEvent(Context context, String functionName, String moduleDetail,
                                    String trigValue, HashMap<String, String> extend);

    /** 自定义事件（带子类型与触发值）。 */
    public abstract void customEvent(Context context, String functionName, String moduleDetail,
                                     String trigValue, HashMap<String, String> extend);

    /** 页面结束。 */
    public abstract void onPageEnd(String pageName, String fromPage, String toPage, Map extend);

    /** 页面开始。 */
    public abstract void onPageStart(String pageName);

    public abstract void onPause(Context context);

    public abstract void onResume(Context context);

    /** 搜索事件。 */
    public abstract void searchEvent(Context context, String functionName, String keyword,
                                     String resultCount, String searchType);

    public abstract void setDebugMode(boolean debug);

    public abstract void setSessionTimeout(long timeout);

    public abstract void userSingIn(Context context, UserAttr userAttr);

    public abstract void userSingOut(Context context);
}
