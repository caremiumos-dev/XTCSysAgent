package com.xtc.bigdata.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;

import com.xtc.log.LogUtil;

import java.util.List;

/**
 * 进程信息工具。
 */
public class ProcessUtils {

    private static final String TAG = "ProcessUtils";

    private ProcessUtils() {
    }

    /**
     * 取当前进程名。
     *
     * <p>用于判断"当前是不是宿主主进程" —— 只有主进程才做基信息采集、
     * 才发 appLaunch 事件。取不到时返回 null，调用方必须自己判空。</p>
     *
     * @param context 上下文
     * @return 进程名；取不到返回 null
     */
    public static String getCurProcessName(Context context) {
        int pid = Process.myPid();
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        if (activityManager == null) {
            LogUtil.e(TAG, "error , ActivityManager = null !");
            return null;
        }
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        if (runningProcesses == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return null;
    }
}
