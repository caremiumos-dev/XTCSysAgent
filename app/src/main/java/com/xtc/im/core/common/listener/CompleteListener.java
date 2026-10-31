package com.xtc.im.core.common.listener;

/**
 * 一次推送的“全部响应都收完了”回调。
 *
 * <p>多响应请求（{@code mutiResponse}）里，每条响应单独走
 * {@link PushResponseListener}，全部收完再走这里一次。</p>
 *
 * <p>原厂接口名为单字母 {@code c}。</p>
 */
public interface CompleteListener {

    /** 响应收完。 */
    void onComplete();
}
