package com.xtc.utils.system;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 计步相关工具：里程/卡路里换算，以及从 sysfs 读取计步模块的状态。
 */
public class StepCounterUtils {

    private static final String TAG = "life";

    private StepCounterUtils() {
    }

    /** 步数换算成里程（米），系数 0.75。 */
    public static int stepToDistance(int steps) {
        return (int) (((double) steps) * 0.75d);
    }

    /** 步数换算成卡路里，系数 55 * 1.036 / 1000。 */
    public static int stepToCalorie(int steps) {
        return (int) ((((double) (steps * 55)) * 1.036d) / 1000.0d);
    }

    /** 读 sysfs 单行内容，失败时返回 "-1"。 */
    private static String readLine(String path) {
        try {
            return new BufferedReader(new FileReader(path)).readLine();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return "-1";
        }
    }

    /** 是否需要强制刷新计步。 */
    public static synchronized boolean isStepForceRefresh() {
        return "1".equals(readLine("/sys/class/misc/m_step_c_misc/step_cforce_refresh"));
    }

    /** 活动模块 flush。 */
    public static synchronized String readActivityFlush() {
        return readLine("/sys/class/misc/m_act_misc/actflush");
    }

    /** 计步模块 flush。 */
    public static synchronized String readStepFlush() {
        return readLine("/sys/class/misc/m_step_c_misc/step_cflush");
    }

    /** 运行模块 flush。 */
    public static synchronized String readRunningModuleFlush() {
        return readLine("/sys/class/misc/m_rm_misc/running_moduleflush");
    }

    /** 卡路里模块 flush。 */
    public static synchronized String readCalorieFlush() {
        return readLine("/sys/class/misc/m_cc_misc/calorie_counterflush");
    }
}
