package com.xtc.qiniu;

import android.content.Context;
import android.text.TextUtils;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.KeyGenerator;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.FileRecorder;
import com.xtc.log.LogUtil;
import com.xtc.qiniu.bean.NetUploadToken;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.encode.UuidUtils;
import com.xtc.utils.storage.FolderManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 七牛云的上传实现。
 *
 * <p>上传前需要 token：优先用本地缓存里的，没有或已失效才去服务端取。
 * 每次上传会分配一个随机 requestId 放进 {@code cancelMap}，
 * 取消时把对应项置 true，{@link UpCancellationSignal} 就会让 SDK 中断传输。</p>
 *
 * <p>上传失败且错误信息里含 "dns" 时，会清掉本地 DNS 缓存，让下次重新解析 ——
 * 七牛域名经常被调度到坏 IP 上。</p>
 */
public class QiNiuManager extends ICloudManager {

    private static final String TAG = "qiniu_upload_QiNiuManager";

    /** 断点续传记录文件名前缀。 */
    private static final String QINIU_UPLOAD_RECORD_FILE = "qiniu_upload_record";

    private static volatile QiNiuManager instance;

    private Context context;
    private XtcDns mXtcDns;
    private UploadManager uploadManager = getUploadManager();

    /** requestId → 是否已取消。 */
    private Map<String, Boolean> cancelMap = new HashMap<>();

    private QiNiuManager(Context context) {
        this.context = context;
    }

    public static QiNiuManager getInstance(Context context) {
        QiNiuManager manager = instance;
        if (manager == null) {
            synchronized (QiNiuManager.class) {
                manager = instance;
                if (manager == null) {
                    manager = new QiNiuManager(context);
                    instance = manager;
                }
            }
        }
        return manager;
    }

    /**
     * 创建上传器：带断点续传记录、自定义 key 生成器、以及本工程的 DNS 实现。
     *
     * <p>key 生成器把 key 和"反转后的绝对路径"拼在一起，这样同一个文件在不同路径下
     * 不会互相覆盖续传记录。</p>
     */
    public UploadManager getUploadManager() {
        // 先确保工作目录存在，否则 FileRecorder 会拿不到可写目录。
        FolderManager.getInstance().getCommonDir();
        FileRecorder fileRecorder;
        try {
            File recordFile = File.createTempFile(QINIU_UPLOAD_RECORD_FILE, ".tmp");
            String parent = recordFile.getParent();
            LogUtil.d(TAG, "HXQ-qiniu, create record file = " + recordFile.getAbsolutePath());
            fileRecorder = new FileRecorder(parent);
        } catch (IOException e) {
            e.printStackTrace();
            fileRecorder = null;
        }
        KeyGenerator keyGenerator = new KeyGenerator() {
            @Override
            public String gen(String key, File file) {
                return key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
            }
        };
        this.mXtcDns = new XtcDns(this.context);
        return new UploadManager(new Configuration.Builder()
                .recorder(fileRecorder, keyGenerator)
                .dns(this.mXtcDns)
                .build());
    }

    /** 上传失败且错误信息提到 dns 时，清掉本地 DNS 缓存。 */
    private void checkNeedClearDns(ResponseInfo responseInfo) {
        if (responseInfo == null) {
            QiNiuUploadBehavior.uploadFail(-1, "null");
            return;
        }
        LogUtil.d(TAG, "checkNeedClearDns");
        QiNiuUploadBehavior.uploadFail(responseInfo.statusCode, responseInfo.error);
        if (TextUtils.isEmpty(responseInfo.error) || this.mXtcDns == null
                || !responseInfo.error.toLowerCase().contains("dns")) {
            return;
        }
        this.mXtcDns.clear();
    }

    /** 统一的完成回调处理。 */
    public void handlerComplete(ICloudManager.OnUpLoadListener listener, String key,
                                ResponseInfo responseInfo, JSONObject response) {
        LogUtil.d(TAG, "complete() called with: key = [" + key + "], respInfo = ["
                + responseInfo + "], response = [" + response + "]");
        if (listener == null) {
            LogUtil.d(TAG, "onUpLoadListener is null");
            return;
        }
        if (responseInfo == null) {
            LogUtil.w(TAG, "uploadFile() respInfo is null,Qiniu server not response");
            listener.onFailure(key, -1, "七牛服务器未响应");
            checkNeedClearDns(null);
            return;
        }
        if (response == null) {
            LogUtil.w(TAG, "uploadFile() response is null");
        } else {
            try {
                key = response.getString("key");
                LogUtil.d(TAG, "response key = " + key);
            } catch (JSONException e) {
                LogUtil.e(TAG, e);
            }
        }
        if (responseInfo.isOK()) {
            LogUtil.i(TAG, "upload Success");
            listener.onSuccess(key);
        } else {
            LogUtil.w(TAG, key + "uploadFile() upload failed ,status:" + responseInfo.statusCode
                    + ",error info is :" + responseInfo.error);
            listener.onFailure(key, responseInfo.statusCode, responseInfo.error);
            checkNeedClearDns(responseInfo);
        }
    }

    /** 文件上传（带 token 缓存）。 */
    private void upLoad(int type, final String key, final File file,
                        final UpCompletionHandler completionHandler,
                        final UploadOptions uploadOptions) {
        if (file == null || !file.exists()) {
            return;
        }
        NetUploadToken token = (NetUploadToken) JSONUtil.fromJSON(
                SharedTool.getUploadToken(this.context, type), NetUploadToken.class);
        if (token == null || token.getToken() == null) {
            TokenManager.getUploadToken(this.context, type, new TokenManager.OnUpLoadTokenListener() {
                @Override
                public void onFailure(String error) {
                    LogUtil.w(TAG, "upLoad()_File get token failed: " + error);
                    completionHandler.complete(key, null, null);
                }

                @Override
                public void onSuccess(String tokenValue) {
                    LogUtil.i(TAG, "upLoad()_File get token success: " + tokenValue
                            + " file length is :" + file.length());
                    uploadManager.put(file, key, tokenValue, completionHandler, uploadOptions);
                }
            });
        } else {
            this.uploadManager.put(file, key, token.getToken(), completionHandler, uploadOptions);
        }
    }

    /** 字节数组上传（带 token 缓存）。 */
    private void upLoad(int type, final String key, final byte[] data,
                        final UpCompletionHandler completionHandler,
                        final UploadOptions uploadOptions) {
        if (data == null) {
            return;
        }
        NetUploadToken token = (NetUploadToken) JSONUtil.fromJSON(
                SharedTool.getUploadToken(this.context, type), NetUploadToken.class);
        if (token == null || token.getToken() == null) {
            TokenManager.getUploadToken(this.context, type, new TokenManager.OnUpLoadTokenListener() {
                @Override
                public void onFailure(String error) {
                    LogUtil.i(TAG, "upLoad()_byte[] get token failed: " + error);
                }

                @Override
                public void onSuccess(String tokenValue) {
                    LogUtil.i(TAG, "upLoad()_byte[] get token success: " + tokenValue
                            + "data length is :" + data.length);
                    uploadManager.put(data, key, tokenValue, completionHandler, uploadOptions);
                }
            });
        } else {
            this.uploadManager.put(data, key, token.getToken(), completionHandler, uploadOptions);
        }
    }

    /** 带封面的上传：token 需要绑定 key，所以走 4 参版本。 */
    private void upLoadByCover(int type, final String key, final File file,
                               final UpCompletionHandler completionHandler,
                               final UploadOptions uploadOptions) {
        if (file == null || !file.exists()) {
            LogUtil.w(TAG, "upLoadByCover() file is not exist.");
            completionHandler.complete(key, null, null);
            return;
        }
        TokenManager.getUploadToken(this.context, type, key, new TokenManager.OnUpLoadTokenListener() {
            @Override
            public void onFailure(String error) {
                LogUtil.w(TAG, "upLoadByCover() get token failed :" + error);
                completionHandler.complete(key, null, null);
            }

            @Override
            public void onSuccess(String tokenValue) {
                LogUtil.i(TAG, "upLoadByCover() get token success : " + tokenValue
                        + "file length is :" + file.length());
                uploadManager.put(file, key, tokenValue, completionHandler, uploadOptions);
            }
        });
    }

    @Override
    public void cancle(String requestId) {
        if (this.cancelMap.containsKey(requestId)) {
            this.cancelMap.put(requestId, true);
        }
        this.cancelMap.remove(requestId);
    }

    /** 清掉所有已取消的条目。 */
    public void clearCancel() {
        ArrayList<String> cancelled = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : this.cancelMap.entrySet()) {
            if (entry.getValue().booleanValue()) {
                cancelled.add(entry.getKey());
            } else {
                LogUtil.i(TAG, "entry.getValue() is false");
            }
        }
        Iterator<String> iterator = cancelled.iterator();
        while (iterator.hasNext()) {
            this.cancelMap.remove(iterator.next());
        }
        cancelled.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.cancelMap.clear();
    }

    /** 进度回调：key 不为空才算有效进度。 */
    private UpProgressHandler newProgressHandler(final ICloudManager.OnUpLoadListener listener) {
        return new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (listener != null && !TextUtils.isEmpty(key)) {
                    listener.onProgress(key, percent);
                }
            }
        };
    }

    /** 取消信号：requestId 不在 map 里、或已被置 true 就中断。 */
    private UpCancellationSignal newCancellationSignal(final String requestId) {
        return new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return QiNiuManager.this.cancelMap.get(requestId) == null
                        || QiNiuManager.this.cancelMap.get(requestId).booleanValue();
            }
        };
    }

    @Override
    public String uploadData(int type, String key, byte[] data,
                             final ICloudManager.OnUpLoadListener listener) {
        final String requestId = UuidUtils.uuid();
        this.cancelMap.put(requestId, false);
        upLoad(type, key, data, new UpCompletionHandler() {
            @Override
            public void complete(String completeKey, ResponseInfo responseInfo, JSONObject response) {
                QiNiuManager.this.cancelMap.remove(requestId);
                handlerComplete(listener, completeKey, responseInfo, response);
            }
        }, new UploadOptions(null, null, false, newProgressHandler(listener),
                newCancellationSignal(requestId)));
        return requestId;
    }

    @Override
    public String uploadFile(int type, String key, File file,
                            final ICloudManager.OnUpLoadListener listener) {
        final String requestId = UuidUtils.uuid();
        this.cancelMap.put(requestId, false);
        upLoad(type, key, file, new UpCompletionHandler() {
            @Override
            public void complete(String completeKey, ResponseInfo responseInfo, JSONObject response) {
                QiNiuManager.this.cancelMap.remove(requestId);
                handlerComplete(listener, completeKey, responseInfo, response);
            }
        }, new UploadOptions(null, null, false, newProgressHandler(listener),
                newCancellationSignal(requestId)));
        return requestId;
    }

    /**
     * 直接按"本地路径 + key + token"上传。
     *
     * <p>三个参数都先校验，任一为空就直接回调失败并返回空串。</p>
     */
    @Override
    public String uploadFile(String filePath, String key, String token,
                            final ICloudManager.OnUpLoadListener listener) {
        if (listener == null) {
            LogUtil.d(TAG, "onUpLoadListener is null");
            return "";
        }
        if (TextUtils.isEmpty(filePath)) {
            LogUtil.d(TAG, "filePath is empty");
            listener.onFailure(key, -1, "filePath is empty");
            return "";
        }
        if (!new File(filePath).exists()) {
            LogUtil.d(TAG, "file is not exists！filePath:" + filePath);
            listener.onFailure(key, -1, "file is not exists！filePath:" + filePath);
            return "";
        }
        if (TextUtils.isEmpty(key)) {
            LogUtil.d(TAG, "key is empty");
            listener.onFailure(key, -1, "key is empty");
            return "";
        }
        if (TextUtils.isEmpty(token)) {
            LogUtil.d(TAG, "token is empty");
            listener.onFailure(key, -1, "token is empty");
            return "";
        }
        final String requestId = UuidUtils.uuid();
        this.cancelMap.put(requestId, false);
        this.uploadManager.put(filePath, key, token, new UpCompletionHandler() {
            @Override
            public void complete(String completeKey, ResponseInfo responseInfo, JSONObject response) {
                QiNiuManager.this.cancelMap.remove(requestId);
                handlerComplete(listener, completeKey, responseInfo, response);
            }
        }, new UploadOptions(null, null, false, new UpProgressHandler() {
            @Override
            public void progress(String progressKey, double percent) {
                LogUtil.i(TAG, "progress: " + percent);
                if (listener == null) {
                    LogUtil.d(TAG, "onUpLoadListener is null");
                } else {
                    listener.onProgress(progressKey, percent);
                }
            }
        }, newCancellationSignal(requestId)));
        return requestId;
    }

    @Override
    public String uploadFileByCover(int type, String key, File file,
                                   final ICloudManager.OnUpLoadListener listener) {
        final String requestId = UuidUtils.uuid();
        this.cancelMap.put(requestId, false);
        upLoadByCover(type, key, file, new UpCompletionHandler() {
            @Override
            public void complete(String completeKey, ResponseInfo responseInfo, JSONObject response) {
                QiNiuManager.this.cancelMap.remove(requestId);
                handlerComplete(listener, completeKey, responseInfo, response);
            }
        }, new UploadOptions(null, null, false, newProgressHandler(listener),
                newCancellationSignal(requestId)));
        return requestId;
    }
}
