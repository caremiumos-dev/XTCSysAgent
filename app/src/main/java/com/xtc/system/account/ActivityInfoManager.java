package com.xtc.system.account;

import android.app.Activity;
import com.xtc.log.LogUtil;
import java.util.LinkedList;

/**
 * 维护一个 Activity 栈（只关心"最上面是谁"）。
 *
 * <p>埋点需要知道当前页面名，但 systemagent 是常驻进程、Activity 少，
 * 所以用一个极简的 LinkedList 当栈，不做任何状态同步。</p>
 *
 * <p>注意 {@link #destroy(Activity)} 只在栈顶匹配时才出栈，否则只打错误日志 ——
 * 这样乱序的 destroy 不会把栈搞乱。</p>
 */
public class ActivityInfoManager {

    private static final String TAG = "ActivityInfoManager";

    private static ActivityInfoManager activityInfoManager;

    private LinkedList<ActivityInfo> activityInfoQueue = new LinkedList<>();

    /** 栈里的一项，目前只存类名。 */
    public class ActivityInfo {

        private String name;

        public ActivityInfo(String name) {
            this.name = name;
        }
    }

    private ActivityInfoManager() {
    }

    public static ActivityInfoManager getInstance() {
        if (activityInfoManager == null) {
            activityInfoManager = new ActivityInfoManager();
        }
        return activityInfoManager;
    }

    private ActivityInfo createActivityInfo(Activity activity) {
        return new ActivityInfo(activity.getClass().getSimpleName());
    }

    /** Activity.onCreate 时压栈。 */
    public synchronized void create(Activity activity) {
        ActivityInfo info = createActivityInfo(activity);
        this.activityInfoQueue.push(info);
        LogUtil.i(TAG, "create [" + info.name + "] success.");
    }

    /** Activity.onDestroy 时出栈；只有栈顶匹配才允许出栈。 */
    public synchronized void destroy(Activity activity) {
        String simpleName = activity.getClass().getSimpleName();
        ActivityInfo top = this.activityInfoQueue.peek();
        if (top != null && top.name.equals(simpleName)) {
            LogUtil.i(TAG, "destroy [" + this.activityInfoQueue.pop().name + "] success.");
            return;
        }
        LogUtil.e(TAG, "destroy [" + simpleName + "] fail,activityName:" + simpleName
                + ",activityInfoQueue.peek():" + (top == null ? "null" : top.name));
    }

    /** 栈顶 Activity 的类名；栈空返回 null。 */
    public synchronized String getTop() {
        ActivityInfo top = this.activityInfoQueue.peek();
        if (top == null) {
            LogUtil.e(TAG, "top activity is null.");
            return null;
        }
        LogUtil.i(TAG, "top activity is [" + top.name + "]");
        return top.name;
    }

    /** 是不是栈顶。 */
    public synchronized boolean isTop(Activity activity) {
        String simpleName = activity.getClass().getSimpleName();
        ActivityInfo top = this.activityInfoQueue.peek();
        if (top == null) {
            LogUtil.e(TAG, "top activity is null.");
            return false;
        }
        if (simpleName.equals(top.name)) {
            LogUtil.i(TAG, "top activity is [" + top.name + "]");
            return true;
        }
        return false;
    }

    /** 空实现（原厂就没做）。 */
    public synchronized void pause(Activity activity) {
    }

    /** 空实现（原厂就没做）。 */
    public synchronized void resume(Activity activity) {
    }
}
