package com.xtc.systemagent;

/**
 * 与 aapt 为应用包自动生成的常量容器同名、同结构。
 *
 * <p>原厂 APK 里 {@code com.xtc.systemagent.Manifest$permission} 保存了本应用
 * 自定义的两条权限名（<b>不是</b> {@code android.Manifest}）。这些名字是与
 * 系统 ROM 里 {@code privapp-permissions} / 签名权限白名单的契约，
 * 一个字符都不能改。</p>
 */
public final class Manifest {

    private Manifest() {
    }

    /** 本应用声明与使用的自定义权限名。 */
    public static final class permission {

        /** 转发 IM 消息用的系统私有权限（由 {@code com.xtc.sys.secure} 持有）。 */
        public static final String IM_FORWARD = "com.xtc.sys.secure.IM_FORWARD";

        /** 读取 DNS 服务数据用的本应用权限。 */
        public static final String READ_DNS = "com.xtc.systemagent.permission.READ_DNS";

        private permission() {
        }
    }
}
