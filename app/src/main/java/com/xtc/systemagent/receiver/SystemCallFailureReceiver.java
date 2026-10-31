package com.xtc.systemagent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.bigData.BigdataUtil;

/**
 * 通话失败上报接收器。
 *
 * <p>通话应用把失败原因打包成一份 JSON 放进 {@code map} extra，
 * 这里原样转给埋点。</p>
 */
public class SystemCallFailureReceiver extends BroadcastReceiver {

    private static final String TAG = "Log_SystemCallFailureReceiver";

    /** 上报通话失败。 */
    private static final String ACTION_REPORT_CALL_FAIL = "com.xtc.largedata.REPORT_CALL_FAIL_ACTION";

    /** 失败信息在 extra 里的键。 */
    private static final String EXTRA_MAP = "map";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.i(TAG, "onReceive , action = " + action);
        if (ACTION_REPORT_CALL_FAIL.equals(action)) {
            String contentMap = intent.getStringExtra(EXTRA_MAP);
            LogUtil.i(TAG, "contentMap = " + contentMap);
            if (TextUtils.isEmpty(contentMap)) {
                LogUtil.w(TAG, "contentMap = null , return !");
            } else {
                BigdataUtil.callFailed(context, contentMap);
            }
        }
    }
}
