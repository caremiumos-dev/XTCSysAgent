package com.xtc.bigdata.collector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;

import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.log.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity 生命周期回调：页面时长、会话刷新、App 退出通知。
 *
 * <p>这里维护的 {@code activityMap} 不是"当前可见页面栈"，而是
 * "还活着（resumed）的页面 -> 最近一次 resumed 的时刻"。因为一个 Activity
 * 可能反复 resume/pause，用时间戳才能算出会话是否超时。</p>
 *
 * <p>所有回调都加了 {@code synchronized}：Activity 生命周期虽然都在主线程，
 * 但原厂这么写，保留以免并发语义变化。</p>
 */
@TargetApi(14)
public class AppLifeCycleCallback implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "BCLifecycleCallback";

    /** 页面 -> 最近一次 resumed 的 elapsedRealtime。 */
    private final Map<String, Long> activityMap = new HashMap<String, Long>();

    /** 构造时清空一次并打日志（原厂行为）。 */
    public AppLifeCycleCallback() {
        this.activityMap.clear();
        LogUtil.i(TAG, "初始化参数");
    }

    @Override
    public synchronized void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity != null) {
            String name = activity.getClass().getName();
            LogUtil.i(TAG, "==================onActivityCreated=====================" + name);
            if (!this.activityMap.containsKey(name)) {
                this.activityMap.put(name, Long.valueOf(SystemClock.elapsedRealtime()));
            }
        }
    }

    @Override
    public synchronized void onActivityDestroyed(Activity activity) {
        if (activity != null) {
            String name = activity.getClass().getName();
            LogUtil.i(TAG, "==================onActivityDestroyed=====================" + name);
            if (this.activityMap.containsKey(name)) {
                this.activityMap.remove(name);
            }
            if (this.activityMap.size() <= 0) {
                // 全部页面都销毁了：手机形态要通知宿主"应用退出"，并换一个会话
                if (Constants.deviceType.equals(Constants.PHONE)) {
                    ShareHelper.getInstance().appExitNotify();
                }
                SessionAgent.clean();
            }
        }
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        if (activity != null) {
            String name = activity.getClass().getName();
            LogUtil.i(TAG, "==================onActivityPaused=====================" + name);
            if (this.activityMap.containsKey(name)) {
                this.activityMap.remove(name);
            }
            if (ConfigAgent.getBehaviorConfig().openActivityDurationTrack) {
                BehaviorCollector.getInstance().pageEnd(name);
            }
        }
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        if (activity != null) {
            String name = activity.getClass().getName();
            LogUtil.i(TAG, "==================onActivityResumed=====================" + name);
            if (!this.activityMap.containsKey(name)) {
                this.activityMap.put(name, Long.valueOf(SystemClock.elapsedRealtime()));
            } else if (SystemClock.elapsedRealtime() - this.activityMap.get(name).longValue()
                    > ConfigAgent.getBehaviorConfig().sessionTimeout) {
                SessionAgent.refreshSessionId();
            }
            if (ConfigAgent.getBehaviorConfig().openActivityDurationTrack) {
                BehaviorCollector.getInstance().pageBegin(name);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle savedInstanceState) {
        LogUtil.i(TAG, "==================onActivitySaveInstanceState=====================" + activity.getClass().getName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        LogUtil.i(TAG, "==================onActivityStarted=====================" + activity.getClass().getName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        LogUtil.i(TAG, "==================onActivityStopped=====================" + activity.getClass().getName());
    }
}
