package com.xtc.im.transpond;

/**
 * HTTP 透传结果回调。
 *
 * <p>原厂接口名为单字母 {@code com.xtc.im.transpond.a}，按角色还原为
 * {@code TranspondCallback}。</p>
 */
public interface TranspondCallback {
    /** 透传结束。 */
    void onTranspondFinished();
}
