package com.xtc.utils.callback;

/**
 * 同时具备"单值成功"与"失败"能力的回调。
 */
public abstract class ValueCallback<T> implements ErrorCallback, SuccessCallback<T> {
}
