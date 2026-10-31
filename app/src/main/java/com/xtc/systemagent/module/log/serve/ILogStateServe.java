package com.xtc.systemagent.module.log.serve;

import android.support.annotation.NonNull;
import com.xtc.systemagent.module.log.serve.impl.LogStateServeImpl;
import com.xtc.systemagent.net.bean.LogStateParams;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.UploadNotifyParam;

/**
 * 日志采集状态的上报服务。
 */
public interface ILogStateServe {
    /** 上报采集状态。 */
    void collectStateUpdate(@NonNull LogStateParams params);

    /** 上报采集状态，并在完成后回调。 */
    void collectStateUpdate(@NonNull LogStateParams params,
                            @NonNull LogStateServeImpl.UploadSuccessCallback callback);

    /** 上报错误信息。 */
    void notifyErrorInfo(@NonNull LogStrategy strategy, String errorInfo);

    /** 上报上传成功。 */
    void notifyUploadSuccess(@NonNull UploadNotifyParam param,
                             @NonNull LogStateServeImpl.UploadSuccessCallback callback);

    /** 上报上传成功（按文件名 + 策略 + 类型 + 数量）。 */
    void notifyUploadSuccess(@NonNull String fileName, @NonNull LogStrategy strategy,
                             int logType, int count,
                             @NonNull LogStateServeImpl.UploadSuccessCallback callback);
}
