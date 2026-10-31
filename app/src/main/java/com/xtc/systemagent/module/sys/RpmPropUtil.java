package com.xtc.systemagent.module.sys;

import com.xtc.log.LogUtil;
import com.xtc.utils.system.SystemPropertiesUtils;

/**
 * RPM（资源功耗管理）调试开关的读写。
 *
 * <p>两个属性键都是系统级约定：{@code ro.boot.xtc.rpmmode} 决定 RPM 模式，
 * {@code log.tag.rpmdebug} 是调试值。</p>
 */
public class RpmPropUtil {

    private static final String TAG = "Log_RpmPropUtil";

    /** 是否打开 RPM 调试日志。 */
    private static final String PRO_RPM_DEBUG_VALUE = "log.tag.rpmdebug";

    /** RPM 模式。 */
    private static final String PRO_RPM_MODE = "ro.boot.xtc.rpmmode";

    private RpmPropUtil() {
    }

    /** 读取 RPM 调试值，未设置时为 -1。 */
    public static int getRpmDebugValue() {
        return SystemPropertiesUtils.getInt(PRO_RPM_DEBUG_VALUE, -1);
    }

    /** 读取 RPM 模式，未设置时为 {@code no_set_rpm}。 */
    public static String getRpmMode() {
        return SystemPropertiesUtils.get(PRO_RPM_MODE, "no_set_rpm");
    }

    /** 设置 RPM 调试值。 */
    public static void setRpmDebugValue(int value) {
        SystemPropertiesUtils.set(PRO_RPM_DEBUG_VALUE, value);
        LogUtil.d(TAG, "im set log.tag.rpmdebug:" + value);
    }
}
