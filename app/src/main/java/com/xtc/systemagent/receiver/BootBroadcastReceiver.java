package com.xtc.systemagent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.xtc.log.LogUtil;

/**
 * 开机广播接收器。
 *
 * <p>本应用自身没有界面，开机后由系统按 {@code targetPackage} 拉起进程即可，
 * 所以这里只记一条日志，不额外做跳转。</p>
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.i(TAG, "接到 开机广播 , 目的 --> 拉起 APP");
    }
}
