package com.xtc.qiniu;

import android.content.Context;
import com.xtc.utils.storage.SPUtils;

/**
 * 上传 token 的本地缓存。
 *
 * <p>token 是服务端下发的短期凭证，缓存起来可以避免每次上传都去请求。</p>
 */
public class SharedTool {

    private SharedTool() {
    }

    /** 读取指定业务类型的上传 token。 */
    public static String getUploadToken(Context context, int type) {
        return SPUtils.getInstance(context)
                .getString(Constants.ICloud.UPLOAD_TOKEN + type, "");
    }

    /** 保存指定业务类型的上传 token。 */
    public static void saveUploadToken(Context context, int type, String token) {
        SPUtils.getInstance(context)
                .putString(Constants.ICloud.UPLOAD_TOKEN + type, token);
    }
}
