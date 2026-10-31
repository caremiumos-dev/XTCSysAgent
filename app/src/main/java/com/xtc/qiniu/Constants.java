package com.xtc.qiniu;

/**
 * 七牛上传相关常量。
 */
public interface Constants {

    /** 上传日志的 tag 前缀。 */
    String QINIU_TAG = "qiniu_upload_";

    interface ICloud {
        /** 上传 token 在 SharedPreferences 里的键前缀（后面拼业务类型）。 */
        String UPLOAD_TOKEN = "upload_token_";
    }
}
