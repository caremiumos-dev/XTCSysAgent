package com.xtc.systemagent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.im.ImMessage;
import com.xtc.systemagent.im.ImMessageData;
import com.xtc.systemagent.im.ImTypeConstants;
import com.xtc.systemagent.service.SysImService;
import com.xtc.utils.encode.JSONUtil;

/**
 * IM 数据接收器。
 *
 * <p>launcher 收到 IM 推送后转成 {@code com.xtc.systemagent.im.data} 广播发过来，
 * 这里按消息 type 分发：</p>
 * <ul>
 *   <li>33002 —— 安全转发，原样再用带权限的广播转发给安全模块；</li>
 *   <li>34001 —— 设置 RPM 模式；</li>
 *   <li>34002 —— 下发系统配置；</li>
 *   <li>34003 —— 删除系统文件。</li>
 * </ul>
 */
public class ImDataReceiver extends BroadcastReceiver {

    private static final String TAG = "Log_SysAgent_ImDataReceiver";

    private static final String IM_SYSTEM_AGENT_ACTION = "com.xtc.systemagent.im.data";

    private static final String FORWARD_SECURE_ACTION = "com.xtc.sys.secure.im";

    private static final String FORWARD_SECURE_PERM = "com.xtc.sys.secure.IM_FORWARD_PERM";

    private static final String FORWARD_SECURE_IM_TYPE = "imType";

    private static final String FORWARD_SECURE_IM_CONTENT = "imContent";

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_CONTENT = "content";
    private static final String EXTRA_ACTION_INT = "actionInt";

    /** 设置 RPM 模式。 */
    private static final int ACTION_SET_RPM_MODE = 1;
    /** 下发系统配置。 */
    private static final int ACTION_SET_SYS_CONFIG = 2;
    /** 删除系统文件。 */
    private static final int ACTION_DELETE_SYS_FILE = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (IM_SYSTEM_AGENT_ACTION.equals(action)) {
            dealImMsg(context, intent);
        } else {
            LogUtil.i(TAG, "handleMessage unknown action: " + action);
        }
    }

    private void dealImMsg(Context context, Intent intent) {
        ImMessageData messageData = (ImMessageData) JSONUtil.fromJSON(
                intent.getStringExtra(EXTRA_DATA), ImMessageData.class);
        if (messageData == null) {
            LogUtil.w(TAG, "imMessageData is null , return !");
            return;
        }
        ImMessage message = messageData.getMessage();
        LogUtil.i(TAG, "receiver the im messageData:" + messageData);
        if (message == null || message.getType() == null) {
            LogUtil.w(TAG, "imMessage:" + message);
            return;
        }
        LogUtil.i(TAG, "type = " + message.getType() + " , msg = " + message.getContent());
        switch (message.getType().intValue()) {
            case ImTypeConstants.TYPE_SECURE_FORWARD:
                dealForwardSecureIm(context, message.getType().intValue(), message.getContent());
                break;
            case ImTypeConstants.TYPE_SET_RPM_MODE:
                dealRpmContent(context, message.getContent());
                break;
            case ImTypeConstants.TYPE_SET_SYS_CONFIG:
                dealSysConfig(context, message.getContent());
                break;
            case ImTypeConstants.TYPE_DELETE_SYS_FILE:
                dealDeleteSysFile(context, message.getContent());
                break;
            default:
                LogUtil.i(TAG, "Ignore push type");
                break;
        }
    }

    /** 原样转发给安全模块，权限限定 {@code com.xtc.sys.secure.IM_FORWARD_PERM}。 */
    private void dealForwardSecureIm(Context context, int imType, String content) {
        Intent intent = new Intent();
        intent.setAction(FORWARD_SECURE_ACTION);
        intent.putExtra(FORWARD_SECURE_IM_TYPE, imType);
        intent.putExtra(FORWARD_SECURE_IM_CONTENT, content);
        context.sendBroadcast(intent, FORWARD_SECURE_PERM);
    }

    private void dealRpmContent(Context context, String content) {
        startSysImService(context, content, ACTION_SET_RPM_MODE);
    }

    private void dealSysConfig(Context context, String content) {
        startSysImService(context, content, ACTION_SET_SYS_CONFIG);
    }

    private void dealDeleteSysFile(Context context, String content) {
        startSysImService(context, content, ACTION_DELETE_SYS_FILE);
    }

    private void startSysImService(Context context, String content, int actionInt) {
        Intent intent = new Intent(context, SysImService.class);
        intent.putExtra(EXTRA_CONTENT, content);
        intent.putExtra(EXTRA_ACTION_INT, actionInt);
        context.startService(intent);
    }
}
