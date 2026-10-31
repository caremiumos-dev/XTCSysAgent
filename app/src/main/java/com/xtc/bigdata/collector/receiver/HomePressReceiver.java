package com.xtc.bigdata.collector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Home 键广播接收者。
 *
 * <p>系统在按 Home 键时会发 {@code CLOSE_SYSTEM_DIALOGS}，其中
 * {@code reason == "homekey"} 才是 Home 键（长按最近任务等也是这个 action）。</p>
 *
 * <p>它的构造参数是 listener 而不是空构造 —— 说明它是<b>动态注册</b>的，
 * 不可能出现在 Manifest 里。</p>
 */
public class HomePressReceiver extends BroadcastReceiver {

    /** 系统 reason 字段里的 Home 键值。 */
    private static final String SYSTEM_HOME_KEY = "homekey";

    /** 系统广播里的 reason 字段名。 */
    private static final String SYSTEM_REASON = "reason";

    /** 回调。 */
    private HomePressListener mListener;

    /** Home 键回调。 */
    public interface HomePressListener {

        /** Home 键被按下。 */
        void onHomePressed();
    }

    /**
     * 构造。
     *
     * @param homePressListener 回调
     */
    public HomePressReceiver(HomePressListener homePressListener) {
        this.mListener = null;
        this.mListener = homePressListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String reason;
        if (!"android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())
                || (reason = intent.getStringExtra(SYSTEM_REASON)) == null
                || !reason.equals(SYSTEM_HOME_KEY) || this.mListener == null) {
            return;
        }
        this.mListener.onHomePressed();
    }
}
