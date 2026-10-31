package com.xtc.systemagent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.util.SecretCodeUtil;

/**
 * 拨号盘暗码接收器。
 *
 * <p>监听 {@code com.xtc.setting.SECRET_CODE}，scheme 必须是
 * {@code setting_secret_code}，host 作为暗码去 {@link SecretCodeUtil} 里查目标页面。
 * 例如 {@code *#*#0769873#*#*} 会命中 {@code LocalFetchSecretActivity}。</p>
 */
public class SecretCodeReceiver extends BroadcastReceiver {

    private static final String TAG = "SecretCodeReceiver";

    private static final String ACTION_SECRET_CODE = "com.xtc.setting.SECRET_CODE";

    private static final String SECRET_CODE_SCHEME = "setting_secret_code";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        Log.d(TAG, "action = " + action + "data = " + data);
        if (!ACTION_SECRET_CODE.equals(action) || data == null) {
            return;
        }
        String scheme = data.getScheme();
        String host = data.getHost();
        if (!SECRET_CODE_SCHEME.equals(scheme)) {
            return;
        }
        Class<?> targetActivity = SecretCodeUtil.getActivityFromSecretCode(host);
        if (targetActivity == null) {
            return;
        }
        Log.d(TAG, "match class = " + targetActivity.getName());
        Intent target = new Intent(context, targetActivity);
        // 原厂用的是 NOTIFICATION_FLAG_CUSTOM(0x10000000) 作为 flag，
        // 恰好等于 FLAG_ACTIVITY_NEW_TASK，这里保留原常量以维持字节码一致。
        target.addFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
        try {
            context.startActivity(target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
