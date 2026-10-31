package com.xtc.systemagent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.log.bean.ImPushLog;
import com.xtc.systemagent.service.LogStrategyService;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;

/**
 * launcher 转发的推送指令接收器。
 *
 * <p>指令类型：</p>
 * <ul>
 *   <li>1 —— 采集日志（content 是策略 JSON）</li>
 *   <li>3 —— 日志开关（content 是 {@link ImPushLog} JSON）</li>
 *   <li>4 —— 同步日志策略（无 content）</li>
 * </ul>
 */
public class PushDataReceiver extends BroadcastReceiver {

    private static final String TAG = "Log_PushDataReceiver";

    private static final String EXTRA_CONTENT = "content";
    private static final String EXTRA_ACTION_INT = "actionInt";

    /** 采集日志。 */
    private static final int ACTION_COLLECTION_LOG = 1;
    /** 日志开关。 */
    private static final int ACTION_LOG_SWITCH = 3;
    /** 同步日志策略。 */
    private static final int ACTION_LOG_STRATEGY_INIT = 4;

    /** 打开 XTC_HACKER 日志。 */
    private static final int SWITCH_TYPE_XTC_HACKER = 100;
    /** 打开 LOG_HACKER_XTC_BUG_REPORT 日志。 */
    private static final int SWITCH_TYPE_LOG_HACKER_XTC_BUG_REPORT = 101;

    /** XTC_HACKER 默认打开的 tag 组合。 */
    private static final String DEFAULT_XTC_HACKER_TAGS = "enable,kernel,crash";

    @Override
    public void onReceive(Context context, Intent intent) {
        int actionInt = intent.getIntExtra(EXTRA_ACTION_INT, -1);
        String content = intent.getStringExtra(EXTRA_CONTENT);
        LogUtil.i(TAG, "接到 launcher转发的指令 : 1 收集日志, 3 日志开关, 4 同步日志策略 。actionInt = "
                + actionInt + " , content = " + content);
        if (ACTION_LOG_STRATEGY_INIT != actionInt && TextUtils.isEmpty(content)) {
            LogUtil.e(TAG, "error , content is null , return !");
        }
        switch (actionInt) {
            case ACTION_COLLECTION_LOG:
                dealCollectionLog(context, content, actionInt);
                break;
            case ACTION_LOG_SWITCH:
                dealLogSwitch(context, content);
                break;
            case ACTION_LOG_STRATEGY_INIT:
                dealLogStrategyInit(context, actionInt);
                break;
            default:
                LogUtil.w(TAG, "unknown actionInt = " + actionInt);
                break;
        }
    }

    private void dealCollectionLog(Context context, String content, int actionInt) {
        LogUtil.d(TAG, "dealCollectionLog , content = " + content);
        startLogService(context, content, actionInt);
    }

    private void dealLogStrategyInit(Context context, int actionInt) {
        LogUtil.d(TAG, "dealLogStrategyInit -->");
        startLogService(context, null, actionInt);
    }

    /**
     * 处理日志开关。
     *
     * <p>100 = XTC_HACKER，101 = LOG_HACKER_XTC_BUG_REPORT。
     * 100 的 content 为空时用默认 tag 组合。</p>
     */
    private void dealLogSwitch(Context context, String content) {
        ImPushLog imPushLog = (ImPushLog) JSONUtil.fromJSON(content, ImPushLog.class);
        LogUtil.i(TAG, "dealLogSwitch , content = " + content
                + "\ntransfer to ImPushLog = " + imPushLog);
        if (imPushLog == null) {
            LogUtil.e(TAG, "imPushLog = null ,return !");
            return;
        }
        String switchValue = imPushLog.getContent();
        if (SWITCH_TYPE_XTC_HACKER == imPushLog.getType().intValue()) {
            if (TextUtils.isEmpty(switchValue)) {
                SystemPropertiesUtils.set(SystemConstants.PROP_XTC_HACKER, DEFAULT_XTC_HACKER_TAGS);
            } else {
                SystemPropertiesUtils.set(SystemConstants.PROP_XTC_HACKER, switchValue);
            }
        } else if (SWITCH_TYPE_LOG_HACKER_XTC_BUG_REPORT == imPushLog.getType().intValue()) {
            SystemPropertiesUtils.set(SystemConstants.PROP_LOG_HACKER_XTC_BUG_REPORT, switchValue);
        }
        LogUtil.d(TAG, "dealLogSwitch 触发日志开关 type = " + imPushLog.getType()
                + "(100:XTC_HACKER, 101:LOG_HACKER_XTC_BUG_REPORT)，开关值: " + switchValue);
    }

    /** 原厂此处为空实现，保留。 */
    private void dealUploadFile(Context context, String content, int actionInt) {
    }

    private void startLogService(Context context, String content, int actionInt) {
        Intent intent = new Intent(context, LogStrategyService.class);
        intent.putExtra(EXTRA_CONTENT, content);
        intent.putExtra(EXTRA_ACTION_INT, actionInt);
        context.startService(intent);
    }
}
