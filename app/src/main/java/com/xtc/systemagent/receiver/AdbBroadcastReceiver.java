package com.xtc.systemagent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.xtc.log.LogUtil;

/**
 * 调试用的"启动电池监控"广播接收器。
 *
 * <p>正式包里这个功能是关掉的：收到广播只打一条错误日志就返回，
 * 不会真的启动监控。保留它是为了让原厂的 adb 调试流程不报
 * "receiver not found"。</p>
 */
public class AdbBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "Log_AdbBroadcastReceiver";

    /** 启动电池监控。 */
    private static final String ACTION_START_BATTERY_MONITOR =
            "com.xtc.i3launcher.ACTION.START_BATTERY_MONITOR";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.d(TAG, "action = " + action);
        if (ACTION_START_BATTERY_MONITOR.equals(action)) {
            LogUtil.e(TAG, "AdbBroadcastReceiver not debug , return !");
        }
    }
}
