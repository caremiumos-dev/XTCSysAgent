package com.xtc.utils.callback;

/**
 * 失败回调。
 *
 * <p>原厂包名 {@code com.xtc.a.a.a} 已被混淆，接口同样只剩单字母名，
 * 这里按它在调用链里的角色还原为 {@code ErrorCallback}。</p>
 */
public interface ErrorCallback {
    /** 请求失败时回调，参数是失败原因。 */
    void onError(Exception e);
}
