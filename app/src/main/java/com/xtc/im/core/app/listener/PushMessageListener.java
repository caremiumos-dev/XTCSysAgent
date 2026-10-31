package com.xtc.im.core.app.listener;

import com.xtc.im.core.app.bean.PushMessage;

/**
 * 推送消息回调：IM Core 收到服务端推下来的消息后回调给宿主。
 *
 * <p>原厂接口名为单字母 {@code a}。</p>
 */
public interface PushMessageListener {

    /**
     * 收到一条推送消息。
     */
    void onPushMessage(PushMessage pushMessage);
}
