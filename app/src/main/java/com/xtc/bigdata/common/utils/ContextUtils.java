package com.xtc.bigdata.common.utils;

import android.app.Application;

import com.xtc.bigdata.common.error.ErrorCode;
import com.xtc.log.LogUtil;

/**
 * 全局 Application 持有者。
 *
 * <p>采集库大量静态方法拿不到 Context，只能靠这里保存的 Application。
 * 之所以存 {@link Application} 而不是 {@code Context}，是为了从类型上
 * 杜绝把 Activity 传进来造成泄漏。</p>
 *
 * <p>{@link #setContext(Application)} 收到 null 时<b>只打日志、不覆盖</b>已有值，
 * 这条规则被 {@code BehaviorCollector} 的初始化流程依赖。</p>
 */
public class ContextUtils {

    /** 全局 Application，可能为 null（未初始化或已清理）。 */
    private static Application mContext = null;

    /** 日志 tag 用全限定类名，方便按包过滤。 */
    private static final String TAG = ContextUtils.class.getName();

    private ContextUtils() {
    }

    /** 清空持有的 Application（进程退出或测试收尾时调用）。 */
    public static void clean() {
        mContext = null;
    }

    /**
     * 取全局 Application。
     *
     * @return 可能为 null
     */
    public static Application getContext() {
        return mContext;
    }

    /**
     * 判断是否还没有 Application。
     *
     * @return true 表示未初始化
     */
    public static boolean isEmpty() {
        return getContext() == null;
    }

    /**
     * 设置全局 Application。
     *
     * @param application 宿主 Application；为 null 时只报警，不改动原值
     */
    public static void setContext(Application application) {
        if (application == null) {
            LogUtil.w(TAG, ErrorCode.CONTROL_NULL_POINTER_APPLICATION);
        } else {
            mContext = application;
        }
    }
}
