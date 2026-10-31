package com.xtc.im.transpond;

/**
 * HTTP 透传适配器：把 {@code com.xtc.httplib} 的请求交给 IM 长连接通道发出去。
 *
 * <p>原厂接口名为单字母 {@code com.xtc.im.transpond.b}。</p>
 */
public interface TranspondAdapter {
    /**
     * 发起一次透传。
     *
     * @return 是否成功交给通道
     */
    boolean transpond(String url, int port, byte[] body, byte[] header, TranspondCallback callback);
}
