package com.xtc.bigdata.collector.config;

import com.xtc.bigdata.common.error.ErrorCode;
import com.xtc.log.LogUtil;

/**
 * {@link BehaviorConfig} 的持有者。
 *
 * <p>全库唯一一份配置实例，惰性创建。{@link #setBehaviorConfig} 拿到 null 时
 * <b>不覆盖</b>已有配置，只打一条告警 —— 因为宿主可能确实没配，
 * 这时用默认值继续跑比直接崩掉合理。</p>
 */
public class ConfigAgent {

    private static final String TAG = ConfigAgent.class.getName();

    /** 全局唯一配置。 */
    private static BehaviorConfig mConfig;

    private ConfigAgent() {
    }

    /**
     * 取全局配置（首次调用时创建默认配置）。
     *
     * @return 全局配置，永不为 null
     */
    public static BehaviorConfig getBehaviorConfig() {
        if (mConfig == null) {
            mConfig = new BehaviorConfig();
        }
        return mConfig;
    }

    /**
     * 持久化配置。
     *
     * <p>原厂就是空实现 —— 采集配置不落盘，每次启动由宿主重新下发。
     * 保留空方法是为了让 {@link #setBehaviorConfig} 的调用序列保持不变。</p>
     */
    public static void saveConfig() {
    }

    /**
     * 替换全局配置。
     *
     * @param behaviorConfig 新配置；为 null 时忽略
     */
    public static void setBehaviorConfig(BehaviorConfig behaviorConfig) {
        if (behaviorConfig == null) {
            LogUtil.w(TAG, ErrorCode.CONTROL_NULL_POINTER_BEHAVIOR_CONFIG);
        } else {
            mConfig = behaviorConfig;
            saveConfig();
        }
    }
}
