package com.xtc.system.account.constant;

/**
 * 通知栏条目的 flag 位。
 *
 * <p>高 8 位（{@link #NOTIFICATION_FLAG_MASK}）被系统用来存放这些标记，
 * 低 24 位留给通知本身。数值是与 SystemUI 的约定，不能改。</p>
 */
public interface NotificationFlag {
    /** 开机阶段就要显示。 */
    int NOTIFICATION_FLAG_BOOT_USE = 536870912;
    /** 自定义通知。 */
    int NOTIFICATION_FLAG_CUSTOM = 268435456;
    /** 作为头部（常驻）。 */
    int NOTIFICATION_FLAG_HEADER = 134217728;
    /** 高 8 位掩码。 */
    int NOTIFICATION_FLAG_MASK = -16777216;
    /** 音乐类通知。 */
    int NOTIFICATION_FLAG_MUSIC = 16777216;
    /** 不在主界面显示。 */
    int NOTIFICATION_FLAG_NOT_IN_MAIN = 67108864;
    /** 实时表盘。 */
    int NOTIFICATION_FLAG_REAL_TIME_CLOCK = 33554432;
    /** 启动后不删除。 */
    int NOTIFICATION_FLAG_START_NOT_DELETE = 1073741824;
}
