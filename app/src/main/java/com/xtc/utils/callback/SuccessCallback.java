package com.xtc.utils.callback;

/**
 * 单值成功回调。
 */
public interface SuccessCallback<T> {
    /** 请求成功时回调。 */
    void onSuccess(T value);
}
