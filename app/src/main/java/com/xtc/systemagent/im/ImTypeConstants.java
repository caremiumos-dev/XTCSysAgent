package com.xtc.systemagent.im;

/**
 * IMM 消息类型常量，取值与服务端约定的 {@code SYNC_MSG_TYPE} 一致。
 */
public class ImTypeConstants {
    /** 设置 RPM 调试模式。 */
    public static final int TYPE_SET_RPM_MODE = 34001;
    /** 读写系统配置。 */
    public static final int TYPE_SET_SYS_CONFIG = 34002;
    /** 删除手表上的文件。 */
    public static final int TYPE_DELETE_SYS_FILE = 34003;
    /** 安全转发。 */
    public static final int TYPE_SECURE_FORWARD = 33002;
}
