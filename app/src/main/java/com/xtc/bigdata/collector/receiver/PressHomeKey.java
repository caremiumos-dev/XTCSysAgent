package com.xtc.bigdata.collector.receiver;

import android.content.IntentFilter;

import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.error.ErrorCode;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.log.LogUtil;

/**
 * Home 键监听的注册/注销门面。
 *
 * <p>只监听一次：收到 Home 键就上报，然后立刻注销 ——
 * 因为"按 Home 退出"这个动作每次应用退出只会发生一次，
 * 常驻监听没有任何意义，反而会随宿主进程一直占着广播。</p>
 */
public class PressHomeKey {

    private static final String TAG = PressHomeKey.class.getName();

    /** 接收者实例（只创建一个，反复注册/注销同一个）。 */
    private final HomePressReceiver homePressReceiver;

    /** 当前是否已注册。 */
    private boolean isRegister;

    /** 单例持有者。 */
    private static class InstanceHolder {

        /** 唯一实例。 */
        private static final PressHomeKey mInstance = new PressHomeKey();

        private InstanceHolder() {
        }
    }

    private PressHomeKey() {
        this.isRegister = false;
        this.homePressReceiver = new HomePressReceiver(new HomePressReceiver.HomePressListener() {
            @Override
            public void onHomePressed() {
                if (Constants.isDebug) {
                    LogUtil.v(PressHomeKey.TAG, "按下了Home键");
                }
                ShareHelper.getInstance().pressHomeKeyNotify();
                unRegisterHomePress();
            }
        });
    }

    /**
     * 取单例。
     *
     * @return 单例
     */
    public static PressHomeKey getInstance() {
        return InstanceHolder.mInstance;
    }

    /** 注册 Home 键监听（已注册则直接返回）。 */
    public void registerHomePress() {
        if (ContextUtils.isEmpty()) {
            LogUtil.w(TAG, ErrorCode.REPORT_HOME_KEY_REG);
            return;
        }
        try {
            if (this.isRegister) {
                return;
            }
            ContextUtils.getContext().registerReceiver(this.homePressReceiver,
                    new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
            this.isRegister = true;
        } catch (Exception e) {
            LogUtil.w(TAG, e.toString());
            LogUtil.w(TAG, ErrorCode.REPORT_HOME_KEY_REG);
        }
    }

    /** 注销 Home 键监听（未注册则什么都不做）。 */
    public void unRegisterHomePress() {
        if (ContextUtils.isEmpty()) {
            LogUtil.w(TAG, ErrorCode.REPORT_HOME_KEY_UNREG);
            return;
        }
        try {
            if (this.isRegister) {
                ContextUtils.getContext().unregisterReceiver(this.homePressReceiver);
                this.isRegister = false;
            }
        } catch (Exception e) {
            LogUtil.w(TAG, e.toString());
            LogUtil.w(TAG, ErrorCode.REPORT_HOME_KEY_UNREG);
        }
    }
}
