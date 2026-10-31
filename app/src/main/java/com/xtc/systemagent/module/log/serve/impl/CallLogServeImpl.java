package com.xtc.systemagent.module.log.serve.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import com.xtc.httplib.net.HttpSubscriber;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.log.serve.ICallLogServe;
import com.xtc.systemagent.net.LogHttpProxy;
import com.xtc.systemagent.net.bean.CallOtherParms;
import rx.schedulers.Schedulers;

/**
 * 通话日志服务的实现：请求服务端把"其他手表"的通话日志推过来。
 */
public class CallLogServeImpl implements ICallLogServe {

    private static final String TAG = "Log_CallLogServeImpl";

    /** 请求结果回调。 */
    public interface OtherCallBack {
        void onFailure();

        void onSuccess();
    }

    private final LogHttpProxy httpProxy;

    public CallLogServeImpl(Context context) {
        this.httpProxy = new LogHttpProxy(context);
    }

    @Override
    public void openOtherWatchCallLog(@NonNull CallOtherParms params, final OtherCallBack callback) {
        LogUtil.i(TAG, "openOtherWatchCallLog , params = " + params);
        this.httpProxy.openOtherWatchCallLog(params)
                .subscribeOn(Schedulers.io())
                .subscribe(new HttpSubscriber<Object>() {
                    @Override
                    public void onHttpError(Throwable throwable) {
                        super.onHttpError(throwable);
                        LogUtil.i(TAG, "openOtherWatchCallLog onHttpError = " + throwable);
                        if (callback != null) {
                            callback.onFailure();
                        }
                    }

                    @Override
                    public void onNext(Object result) {
                        super.onNext(result);
                        LogUtil.i(TAG, "openOtherWatchCallLog onNext result = " + result);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }
                });
    }
}
