package com.xtc.systemagent.module.log.serve.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.xtc.architecture.mvp.BasePresenter;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.log.serve.ILogStateServe;
import com.xtc.systemagent.net.LogHttpProxy;
import com.xtc.systemagent.net.bean.LogStateParams;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.lang.ref.SoftReference;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * 采集状态上报的实现。
 *
 * <p>用 {@link SoftReference} 缓存单例：内存紧张时允许被回收，
 * 下次调用再重建，避免长期占内存。</p>
 */
public class LogStateServeImpl extends BasePresenter<String> implements ILogStateServe {

    private static final String TAG = "Log_LogStateServeImpl";

    private static volatile SoftReference<LogStateServeImpl> logStateServeSoftRef;

    /** 上报结果回调。 */
    public interface UploadSuccessCallback {
        void onFailure();

        void onSuccess();
    }

    private final LogHttpProxy httpProxy;

    /** {@link #notifyErrorInfo} 用的空回调，忽略结果。 */
    private UploadSuccessCallback NotifyErrorInfoCallBack = new UploadSuccessCallback() {
        @Override
        public void onFailure() {
        }

        @Override
        public void onSuccess() {
        }
    };

    private LogStateServeImpl(Context context) {
        super(context);
        this.httpProxy = new LogHttpProxy(this.mContext);
    }

    public static synchronized LogStateServeImpl getInstance(Context context) {
        if (logStateServeSoftRef == null) {
            logStateServeSoftRef = new SoftReference<>(new LogStateServeImpl(context));
        }
        LogStateServeImpl instance = logStateServeSoftRef.get();
        if (instance == null) {
            instance = new LogStateServeImpl(context);
            logStateServeSoftRef = new SoftReference<>(instance);
        }
        return instance;
    }

    @Override
    public void collectStateUpdate(@NonNull LogStateParams params) {
        LogUtil.d(TAG, "collectStateUpdate params = " + params);
        this.httpProxy.collectStateUpdate(params)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "collectStateUpdate error = " + throwable);
                    }

                    @Override
                    public void onNext(String result) {
                        LogUtil.d(TAG, "collectStateUpdate result " + result);
                    }
                });
    }

    @Override
    public void collectStateUpdate(@NonNull LogStateParams params,
                                   @NonNull final UploadSuccessCallback callback) {
        LogUtil.d(TAG, "collectStateUpdate params = " + params);
        this.httpProxy.collectStateUpdate(params)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "collectStateUpdate error = " + throwable);
                        callback.onFailure();
                    }

                    @Override
                    public void onNext(String result) {
                        LogUtil.d(TAG, "collectStateUpdate result " + result);
                        callback.onSuccess();
                    }
                });
    }

    @Override
    public void notifyErrorInfo(@NonNull LogStrategy strategy, String errorInfo) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            LogUtil.e(TAG, "bindNumber is null !");
            return;
        }
        UploadNotifyParam param = new UploadNotifyParam();
        param.setAccountId(bindNumber);
        param.setKey("");
        param.setAppPackage(strategy.getAppPackage());
        param.setId(strategy.getLogId());
        param.setStrategyId(strategy.getId());
        param.setOperatorId(strategy.getUserId());
        param.setMachineId(SharedTool.getMachineId(this.mContext));
        param.setRemark(errorInfo);
        // 2 = 采集失败
        param.setCollectStatus(Integer.valueOf(2));
        notifyUploadSuccess(param, this.NotifyErrorInfoCallBack);
    }

    @Override
    public void notifyUploadSuccess(@NonNull UploadNotifyParam param,
                                    @NonNull final UploadSuccessCallback callback) {
        LogUtil.d(TAG, "notifyUploadSuccess , params = " + param);
        this.httpProxy.notifyUploadSuccess(param)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "notifyUploadSuccess " + throwable);
                        callback.onFailure();
                    }

                    @Override
                    public void onNext(Object result) {
                        LogUtil.d(TAG, "notifyUploadSuccess result " + result);
                        callback.onSuccess();
                    }
                });
    }

    @Override
    public void notifyUploadSuccess(@NonNull String fileName, @NonNull LogStrategy strategy,
                                    int logType, int count,
                                    @NonNull UploadSuccessCallback callback) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            LogUtil.e(TAG, "bindNumber is null !");
            return;
        }
        UploadNotifyParam param = new UploadNotifyParam();
        param.setAccountId(bindNumber);
        param.setKey(fileName);
        param.setSpaceType(Integer.valueOf(count));
        param.setAppPackage(strategy.getAppPackage());
        param.setId(strategy.getLogId());
        param.setStrategyId(strategy.getId());
        param.setOperatorId(strategy.getUserId());
        param.setMachineId(SharedTool.getMachineId(this.mContext));
        param.setCollectStatus(Integer.valueOf(logType));
        notifyUploadSuccess(param, callback);
    }
}
