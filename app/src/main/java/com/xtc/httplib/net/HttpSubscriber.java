package com.xtc.httplib.net;

/**
 * 在 {@link BaseSubscriber} 的基础上增加 {@link #onHttpError(Throwable)} 钩子，
 * 让业务侧只需要关心"HTTP 失败"这一种情况。
 */
public class HttpSubscriber<T> extends BaseSubscriber<T> {

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCompleted() {
        super.onCompleted();
    }

    @Override
    public final void onError(Throwable throwable) {
        super.onError(throwable);
        onHttpError(throwable);
    }

    /** 请求失败。默认空实现，由子类覆写。 */
    public void onHttpError(Throwable throwable) {
    }

    @Override
    public void onNext(T value) {
    }
}
