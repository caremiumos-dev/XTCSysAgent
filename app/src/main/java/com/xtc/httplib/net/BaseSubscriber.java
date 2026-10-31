package com.xtc.httplib.net;

import com.xtc.log.LogUtil;
import rx.Subscriber;

/**
 * 统一打点的 Subscriber 基类：记录请求开始/结束时间并打印耗时。
 */
public class BaseSubscriber<T> extends Subscriber<T> {

    protected long startTime;
    protected long completeTime;

    @Override
    public void onStart() {
        LogUtil.d("BaseSubscriber.onStart on thread:" + Thread.currentThread().getName());
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onCompleted() {
        LogUtil.d("BaseSubscriber.onCompleted on thread:" + Thread.currentThread().getName());
        this.completeTime = System.currentTimeMillis();
        LogUtil.i("http spent time[" + (this.completeTime - this.startTime) + "]ms");
    }

    @Override
    public void onError(Throwable throwable) {
        LogUtil.e("BaseSubscriber.onError on thread:" + Thread.currentThread().getName());
        this.completeTime = System.currentTimeMillis();
        LogUtil.e("http spent time[" + (this.completeTime - this.startTime) + "]ms");
    }

    @Override
    public void onNext(T value) {
    }
}
