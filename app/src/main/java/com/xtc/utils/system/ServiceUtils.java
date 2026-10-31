package com.xtc.utils.system;

import android.app.ActivityManager;
import android.content.Context;

/**
 * 服务运行状态查询。
 */
public final class ServiceUtils {

    private ServiceUtils() {
    }

    /** 指定 Service 类是否正在运行。 */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        // Integer.MAX_VALUE = 2147483647，即取全部运行中的服务。
        for (ActivityManager.RunningServiceInfo info
                : ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
