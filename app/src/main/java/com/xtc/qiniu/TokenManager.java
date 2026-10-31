package com.xtc.qiniu;

import android.content.Context;
import com.xtc.log.LogUtil;
import com.xtc.qiniu.bean.NetDownloadTokenParam;
import com.xtc.qiniu.bean.NetUploadToken;
import com.xtc.qiniu.net.ICloudHttpServiceProxy;
import com.xtc.utils.encode.JSONUtil;
import java.util.List;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * 七牛上传/下载 token 的获取与缓存。
 *
 * <p>token 由业务服务端签发，有效期短，所以每次上传前取一次、
 * 拿到后落 SharedPreferences 备查。所有请求都在 io 线程发起，
 * 回调里直接使用结果 —— 调用方自己负责切线程。</p>
 */
public class TokenManager {

    private static final String TAG = "TokenManager";

    private static ICloudHttpServiceProxy iCloudHttpServiceProxy;

    /** 下载 token 回调。 */
    public interface OnDownLoadTokenListener {
        void onFailure(String error);

        void onSuccess(List<String> tokens);
    }

    /** 上传 token 回调。 */
    public interface OnUpLoadTokenListener {
        void onFailure(String error);

        void onSuccess(String token);
    }

    private TokenManager() {
    }

    private static ICloudHttpServiceProxy getProxy(Context context) {
        if (iCloudHttpServiceProxy == null) {
            iCloudHttpServiceProxy = new ICloudHttpServiceProxy(context);
        }
        return iCloudHttpServiceProxy;
    }

    /** 取下载 token（8 参版本，带质量与格式）。 */
    public static void getDownloadToken(Context context, int fileType, List<String> keys,
                                        Integer longEdge, Integer shortEdge, int quality,
                                        String format,
                                        final OnDownLoadTokenListener listener) {
        NetDownloadTokenParam param = new NetDownloadTokenParam();
        param.setFileType(fileType);
        param.setKeys(keys);
        param.setLongEdge(longEdge);
        param.setShortEdge(shortEdge);
        param.setQuality(Integer.valueOf(quality));
        param.setFormat(format);
        LogUtil.i("getDownloadToken()_8args start downloading token");
        getProxy(context).getDownloadToken(param)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<List<String>>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.i(TAG, "getDownloadToken()_8args onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "getDownloadToken()_8args download failed: " + throwable);
                        if (listener != null) {
                            listener.onFailure(throwable.toString());
                        }
                    }

                    @Override
                    public void onNext(List<String> result) {
                        LogUtil.i(TAG, "getDownloadToken()_8args download success：" + result);
                        if (listener != null) {
                            listener.onSuccess(result);
                        }
                    }
                });
    }

    /** 取下载 token（6 参版本）。 */
    public static void getDownloadToken(Context context, int fileType, List<String> keys,
                                        Integer longEdge, Integer shortEdge,
                                        final OnDownLoadTokenListener listener) {
        NetDownloadTokenParam param = new NetDownloadTokenParam();
        param.setFileType(fileType);
        param.setKeys(keys);
        param.setLongEdge(longEdge);
        param.setShortEdge(shortEdge);
        LogUtil.i("getDownloadToken()_6args start downloading token");
        getProxy(context).getDownloadToken(param)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<List<String>>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.i(TAG, "getDownloadToken()_6args onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "getDownloadToken()_6args download failed：" + throwable);
                        if (listener != null) {
                            listener.onFailure(throwable.toString());
                        }
                    }

                    @Override
                    public void onNext(List<String> result) {
                        LogUtil.e(TAG, "getDownloadToken()_6args download success " + result);
                        if (listener != null) {
                            listener.onSuccess(result);
                        }
                    }
                });
    }

    /**
     * 取上传 token（3 参版本），成功后连同时间戳一起缓存。
     */
    public static void getUploadToken(final Context context, final int type,
                                      final OnUpLoadTokenListener listener) {
        LogUtil.i(TAG, "getUploadToken()_3args start uploading token");
        getProxy(context).getUploadToken(type)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.i(TAG, "getUploadToken()_3args onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "getUploadToken()_3args request failed " + throwable);
                        if (listener != null) {
                            listener.onFailure("The server is not responding.");
                        }
                    }

                    @Override
                    public void onNext(String token) {
                        LogUtil.i(TAG, "getUploadToken()_3args request success " + token);
                        NetUploadToken netUploadToken = new NetUploadToken();
                        netUploadToken.setToken(token);
                        netUploadToken.setTimestamp(Long.valueOf(System.currentTimeMillis()));
                        SharedTool.saveUploadToken(context, type, JSONUtil.toJSON(netUploadToken));
                        if (listener != null) {
                            listener.onSuccess(token);
                        }
                    }
                });
    }

    /** 取上传 token（4 参版本，带 key）。 */
    public static void getUploadToken(Context context, int type, String key,
                                      final OnUpLoadTokenListener listener) {
        LogUtil.i(TAG, "getUploadToken()_4args start uploading token");
        getProxy(context).getUploadToken(type, key)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.i(TAG, "getUploadToken() onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "getUploadToken()_4args request failed ：" + throwable);
                        if (listener != null) {
                            listener.onFailure("The server is not responding.");
                        }
                    }

                    @Override
                    public void onNext(String token) {
                        LogUtil.i(TAG, "getUploadToken()_4args request success " + token);
                        if (listener != null) {
                            listener.onSuccess(token);
                        }
                    }
                });
    }
}
