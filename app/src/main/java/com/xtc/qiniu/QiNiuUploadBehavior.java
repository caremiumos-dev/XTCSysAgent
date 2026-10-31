package com.xtc.qiniu;

import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.common.bigdata.BehaviorUtil;
import com.xtc.httplib.util.HandlerUtil;
import com.xtc.log.LogUtil;
import java.util.HashMap;

/**
 * 七牛上传失败的埋点。
 *
 * <p>埋点必须在后台线程跑（SDK 内部会写 SharedPreferences、起线程），
 * 所以先 {@link HandlerUtil#runOnBackground(Runnable)} 再上报。</p>
 */
public class QiNiuUploadBehavior {

    private static final String TAG = "QiNiuUploadBehavior";

    /** 埋点名："微信场景下七牛上传失败"。 */
    private static final String WEICHAT_QINIU_UPLOAD_FAIL = "weichat_qiniu_upload_fail";

    private static final String STATUS_CODE = "statusCode";
    private static final String ERROR = "error";

    private QiNiuUploadBehavior() {
    }

    /**
     * 上报一次上传失败。
     *
     * @param statusCode 七牛返回的 HTTP 状态码，-1 表示没有响应
     * @param error      七牛返回的错误描述，可能是 null
     */
    public static void uploadFail(final int statusCode, final String error) {
        LogUtil.d(TAG, "uploadFail() called with: statusCode = [" + statusCode
                + "], error = [" + error + "]");
        HandlerUtil.runOnBackground(new Runnable() {
            @Override
            public void run() {
                HashMap<String, String> extend = new HashMap<>();
                extend.put(STATUS_CODE, String.valueOf(statusCode));
                extend.put(ERROR, error);
                BehaviorUtil.customEvent(ContextUtils.getContext(),
                        WEICHAT_QINIU_UPLOAD_FAIL, extend);
            }
        });
    }
}
