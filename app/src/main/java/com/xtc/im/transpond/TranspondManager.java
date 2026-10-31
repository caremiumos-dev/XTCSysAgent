package com.xtc.im.transpond;

/**
 * 透传适配器的注册表与转发入口。没有注册实现时一律返回 false。
 *
 * <p>原厂类名为单字母 {@code com.xtc.im.transpond.c}。</p>
 */
public class TranspondManager {

    private static TranspondAdapter sAdapter;

    private TranspondManager() {
    }

    public static void setAdapter(TranspondAdapter adapter) {
        sAdapter = adapter;
    }

    /** 转发一次透传请求；未注册实现时返回 false。 */
    public static boolean transpond(String url, int port, byte[] body, byte[] header,
                                    TranspondCallback callback) {
        if (sAdapter != null) {
            return sAdapter.transpond(url, port, body, header, callback);
        }
        return false;
    }
}
