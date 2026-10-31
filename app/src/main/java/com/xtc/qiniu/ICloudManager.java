package com.xtc.qiniu;

import android.text.TextUtils;
import com.xtc.log.LogUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 云存储操作的抽象基类。
 *
 * <p>下载是通用的（HTTP GET），所以直接在基类里实现；上传各家协议不同，
 * 交给子类（七牛是 {@link QiNiuManager}）。</p>
 *
 * <p>进度回调的粒度是每次 {@code read} 一个 8KB 块就报一次，
 * 和大文件传输的实际体验一致。</p>
 */
public abstract class ICloudManager {

    private static final String TAG = "ICloudManager";

    /** 下载缓冲区大小。 */
    private static final int BUFFER_SIZE = 8192;

    /** HTTP 401：token 过期。 */
    private static final int HTTP_UNAUTHORIZED = 401;
    /** HTTP 200：成功。 */
    private static final int HTTP_OK = 200;

    /** 下载错误码。 */
    public interface ErrorCode {
        /** 下载失败（网络/服务端）。 */
        int DOWNLOAD_ERROR = 1;
        /** token 过期。 */
        int TOKEN_OUTDATE = 2;
        /** 数据为空。 */
        int EMPTY_DATA = 3;
        /** 连接失败。 */
        int CONNECT_FAIL = 4;
    }

    /** 下载回调。 */
    public interface OnDownLoadListener {
        void onError(int errorCode, String errorMsg);

        void onFinish(byte[] data);

        void onProgress(long current, long total);
    }

    /** 上传回调。 */
    public interface OnUpLoadListener {
        void onFailure(String key, int statusCode, String error);

        void onProgress(String key, double progress);

        void onSuccess(String key);
    }

    private final OkHttpClient client = new OkHttpClient();

    /** 统一的上传失败转发；listener 为空时只打日志。 */
    public void downloadFail(int errorCode, String errorMsg, OnDownLoadListener listener) {
        if (listener != null) {
            listener.onError(errorCode, errorMsg);
        } else {
            LogUtil.i(TAG, "listener is null");
        }
    }

    /** 取消下载。 */
    public abstract void cancle(String url);

    /** 下载成字节数组。 */
    public void downloadForBytes(String url, final OnDownLoadListener listener) {
        Call call = this.client.newCall(new Request.Builder().url(url).build());
        LogUtil.i(TAG, "downloadForBytes() starting download");
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(TAG, "downloadForBytes() download error：" + e);
                downloadFail(ErrorCode.DOWNLOAD_ERROR, "download error", listener);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                if (response.code() == HTTP_UNAUTHORIZED) {
                    LogUtil.e(TAG, "downloadForBytes() download error，response code is : 401");
                    downloadFail(ErrorCode.TOKEN_OUTDATE, "download token out of data!!!!", listener);
                    return;
                }
                if (response.code() != HTTP_OK) {
                    LogUtil.e(TAG, "downloadForBytes() download error，response code is ："
                            + response.code());
                    downloadFail(ErrorCode.DOWNLOAD_ERROR,
                            "download error,http state:" + response.code(), listener);
                    return;
                }
                if (body == null) {
                    downloadFail(ErrorCode.EMPTY_DATA, "download error,response body is null", listener);
                    return;
                }
                long total = body.contentLength();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = body.byteStream();
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    long current = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        current += read;
                        if (listener != null) {
                            listener.onProgress(current, total);
                        }
                    }
                    if (listener != null) {
                        listener.onFinish(out.toByteArray());
                    }
                } catch (IOException e) {
                    LogUtil.e(TAG, "downloadForBytes() read error：" + e);
                    downloadFail(ErrorCode.DOWNLOAD_ERROR, "download error", listener);
                } finally {
                    out.close();
                    in.close();
                }
            }
        });
    }

    /** 下载并落盘到 {@code dir/fileName}。 */
    public void downloadForFile(String url, String dir, String fileName,
                                final OnDownLoadListener listener) {
        Call call = this.client.newCall(new Request.Builder().url(url).build());
        LogUtil.i(TAG, "downloadForFile() starting download");
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(TAG, "downloadForFile() download error：" + e);
                downloadFail(ErrorCode.DOWNLOAD_ERROR, "download error", listener);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                if (response.code() == HTTP_UNAUTHORIZED) {
                    LogUtil.e(TAG, "downloadForFile() download error，response code is : 401");
                    downloadFail(ErrorCode.TOKEN_OUTDATE, "download token out of data!!!!", listener);
                    return;
                }
                if (response.code() != HTTP_OK) {
                    LogUtil.e(TAG, "downloadForFile() download error，response code is ："
                            + response.code());
                    downloadFail(ErrorCode.DOWNLOAD_ERROR,
                            "download error,http state:" + response.code(), listener);
                    return;
                }
                if (body == null) {
                    downloadFail(ErrorCode.EMPTY_DATA, "download error,response body is null", listener);
                    return;
                }
                File dirFile = new File(dir);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                long total = body.contentLength();
                File target = new File(dir, fileName);
                FileOutputStream out = new FileOutputStream(target);
                InputStream in = body.byteStream();
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    long current = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        current += read;
                        if (listener != null) {
                            listener.onProgress(current, total);
                        }
                    }
                    out.flush();
                } catch (IOException e) {
                    LogUtil.e(TAG, "downloadForFile() read error：" + e);
                    downloadFail(ErrorCode.DOWNLOAD_ERROR, "download error", listener);
                } finally {
                    out.close();
                    in.close();
                }
            }
        });
    }

    /** 上传字节数组。 */
    public abstract String uploadData(int type, String key, byte[] data, OnUpLoadListener listener);

    /** 上传文件。 */
    public abstract String uploadFile(int type, String key, File file, OnUpLoadListener listener);

    /** 上传文件（显式指定文件名与业务类型）。 */
    public abstract String uploadFile(String fileName, String key, String type,
                                      OnUpLoadListener listener);

    /** 上传文件（带封面）。 */
    public abstract String uploadFileByCover(int type, String key, File file,
                                             OnUpLoadListener listener);
}
