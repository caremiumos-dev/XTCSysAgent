package com.xtc.common.bigdata;

import android.content.Context;
import com.xtc.bigdata.collector.BehaviorCollector;
import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import com.xtc.bigdata.collector.encapsulation.entity.event.ClickEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CountEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SearchEvent;
import com.xtc.log.LogUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * 步步高自有埋点通道的实现。
 *
 * <p>只是把参数搬进对应的事件实体，再交给 {@link BehaviorCollector} 去落库/上报。
 * 每个方法的 context 都做了判空 —— 埋点不该因为调用方传 null 而崩。</p>
 */
public class BbkBehavior extends Behavior {

    private static final String TAG = "BbkBehavior";

    @Override
    public void clickEvent(Context context, String functionName, String moduleDetail,
                           HashMap<String, String> extend) {
        if (context == null) {
            LogUtil.e(TAG, "context is null");
            return;
        }
        ClickEvent event = new ClickEvent();
        event.activity = context.getClass().getName();
        event.functionName = functionName;
        event.moduleDetail = moduleDetail;
        event.setExtend(extend);
        BehaviorCollector.getInstance().clickEvent(event);
    }

    @Override
    public void countEvent(Context context, String functionName, String moduleDetail,
                           String trigValue, HashMap<String, String> extend) {
        if (context == null) {
            LogUtil.e(TAG, "context is null");
            return;
        }
        CountEvent event = new CountEvent();
        event.functionName = functionName;
        event.moduleDetail = moduleDetail;
        event.trigValue = trigValue;
        event.activity = context.getClass().getName();
        event.setExtend(extend);
        BehaviorCollector.getInstance().countEvent(event);
    }

    @Override
    public void customEvent(Context context, String functionName, String moduleDetail,
                            String trigValue, HashMap<String, String> extend) {
        if (context == null) {
            LogUtil.e(TAG, "context is null");
            return;
        }
        CustomEvent event = new CustomEvent();
        event.functionName = functionName;
        event.moduleDetail = moduleDetail;
        event.trigValue = trigValue;
        event.activity = context.getClass().getName();
        event.setExtend(extend);
        BehaviorCollector.getInstance().customEvent(event);
    }

    @Override
    public void onPageEnd(String pageName, String fromPage, String toPage, Map extend) {
        BehaviorCollector.getInstance().pageEnd(pageName, fromPage, toPage, extend);
    }

    @Override
    public void onPageStart(String pageName) {
        BehaviorCollector.getInstance().pageBegin(pageName);
    }

    @Override
    public void onPause(Context context) {
        if (context == null) {
            LogUtil.e(TAG, "context is null");
        } else {
            LogUtil.i(TAG, context.getClass().getSimpleName());
        }
    }

    @Override
    public void onResume(Context context) {
        if (context == null) {
            LogUtil.e(TAG, "context is null");
        } else {
            LogUtil.i(TAG, context.getClass().getSimpleName());
        }
    }

    @Override
    public void searchEvent(Context context, String functionName, String keyword,
                            String resultCount, String searchType) {
        if (context == null) {
            LogUtil.e(TAG, "context is null");
            return;
        }
        SearchEvent event = new SearchEvent();
        event.functionName = functionName;
        event.moduleDetail = keyword;
        // 原厂字段名就拼成 keyWrod，改不了（服务端按此字段取值）。
        event.keyWrod = keyword;
        event.resultCount = resultCount;
        event.activity = context.getClass().getName();
        BehaviorCollector.getInstance().searchEvent(event);
    }

    /** 原厂空实现。 */
    @Override
    public void setDebugMode(boolean debug) {
    }

    @Override
    public void setSessionTimeout(long timeout) {
        ConfigAgent.getBehaviorConfig().sessionTimeout = timeout;
    }

    @Override
    public void userSingIn(Context context, UserAttr userAttr) {
        if (context == null || userAttr == null) {
            return;
        }
        BehaviorCollector.getInstance().initUserInfo(userAttr);
    }

    /** 原厂空实现。 */
    @Override
    public void userSingOut(Context context) {
    }
}
