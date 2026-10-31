package com.xtc.utils.callback;

/**
 * 同时具备"列表成功"与"失败"能力的回调。
 */
public abstract class ListResultCallback<T> implements ErrorCallback, ListCallback<T> {
}
