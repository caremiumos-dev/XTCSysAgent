package com.xtc.bigdata.collector;

import android.text.TextUtils;

import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.utils.SystemUtils;
import com.xtc.log.LogUtil;

/**
 * 会话 id 管理。
 *
 * <p>会话 id 的形态是 {@code "<开机时间戳>_<uuid>"}，每个事件都会带上它，
 * 服务端据此把一次使用过程串起来。</p>
 *
 * <p>刷新时机由 {@code AppLifeCycleCallback} 决定：Activity 重新可见且
 * 距上次可见超过 {@code sessionTimeout} 时刷新，于是"离开手表很久再回来"
 * 会被切成两个会话。</p>
 */
public class SessionAgent {

    private static final String TAG = "SessionAgent";

    /** 当前会话 id。 */
    private static String mSessionId;

    private SessionAgent() {
    }

    /** 清空会话 id（下一次取用时重新生成）。 */
    public static void clean() {
        if (Constants.isDebug) {
            LogUtil.i("life", "清除sessionId");
        }
        mSessionId = null;
    }

    /**
     * 取会话 id（为空则立刻生成）。
     *
     * @return 会话 id
     */
    public static String getSessionId() {
        if (TextUtils.isEmpty(mSessionId)) {
            refreshSessionId();
        }
        return mSessionId;
    }

    /** 重新生成会话 id。 */
    public static void refreshSessionId() {
        if (Constants.isDebug) {
            LogUtil.v(TAG, "refresh session Id");
        }
        mSessionId = System.currentTimeMillis() + "_" + SystemUtils.getUUID();
    }
}
