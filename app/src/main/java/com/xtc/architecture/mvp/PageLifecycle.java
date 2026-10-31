package com.xtc.architecture.mvp;

/**
 * 页面级生命周期回调。
 *
 * <p>原厂接口名为单字母 {@code d}，方法名为 {@code i}/{@code j}/{@code k}，
 * 语义无法从字节码完全还原，按"进入页面 / 页面可见 / 离开页面"推断命名。</p>
 */
public interface PageLifecycle {
    /** 进入页面。 */
    void onPageEnter();

    /** 页面可见。 */
    void onPageVisible();

    /** 离开页面。 */
    void onPageLeave();
}
