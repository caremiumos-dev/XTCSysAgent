package com.xtc.utils.callback;

import java.util.List;

/**
 * 列表成功回调。
 */
public interface ListCallback<T> {
    /** 请求成功时回调，参数是结果列表。 */
    void onSuccess(List<T> list);
}
