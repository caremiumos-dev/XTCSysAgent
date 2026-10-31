package com.xtc.utils.callback;

import java.util.List;

/**
 * 回调的安全分发器：所有入口都先判空，避免调用方反复写 null 检查。
 */
public class CallbackInvoker {

    /** 分发一个失败回调。 */
    public static void postError(ErrorCallback callback, String message) {
        if (callback == null) {
            return;
        }
        callback.onError(new Exception(message));
    }

    /** 分发一个完成回调。 */
    public static void postComplete(CompleteCallback callback) {
        if (callback == null) {
            return;
        }
        callback.onComplete();
    }

    /** 分发一个单值成功回调。 */
    public static <T> void postSuccess(SuccessCallback<T> callback, T value) {
        if (callback == null) {
            return;
        }
        callback.onSuccess(value);
    }

    /** 分发一个列表成功回调。 */
    public static <T> void postList(ListCallback<T> callback, List<T> list) {
        if (callback == null) {
            return;
        }
        callback.onSuccess(list);
    }
}
