package com.xtc.utils.system;

import android.content.Context;

/**
 * 相机能力查询。手表基本都没摄像头，业务侧靠它兜底。
 */
public class CameraUtils {

    private CameraUtils() {
    }

    /** 设备是否声明了相机硬件特性。 */
    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.camera");
    }
}
