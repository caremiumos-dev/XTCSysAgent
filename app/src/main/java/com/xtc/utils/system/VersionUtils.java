package com.xtc.utils.system;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

/**
 * 应用版本号相关工具。
 */
public class VersionUtils {

    private VersionUtils() {
    }

    /**
     * 按点分段比较两个版本号。
     *
     * @return 负数/0/正数 分别表示 {@code left} 小于/等于/大于 {@code right}
     */
    public static int compareVersion(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        for (int i = 0; i < leftParts.length; i++) {
            // 最后一段直接返回比较结果；原实现没有做长度对齐，保持原样。
            if (leftParts.length == i + 1) {
                return Integer.valueOf(leftParts[i]).compareTo(Integer.valueOf(rightParts[i]));
            }
            int result = Integer.valueOf(leftParts[i]).compareTo(Integer.valueOf(rightParts[i]));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /** 当前应用的版本名。 */
    public static String getVersionName(Context context) {
        return normalizeVersionName(getRawVersionName(context));
    }

    /**
     * 把版本名规整成三段式 {@code x.y.z}；不合法时返回 {@code 1.0.0}。
     */
    public static String normalizeVersionName(String versionName) {
        if (TextUtils.isEmpty(versionName)) {
            return "1.0.0";
        }
        if (!versionName.matches("[1-9]\\d*.[0-9]\\d*.[0-9]\\d*")
                && !versionName.matches("[1-9]\\d*.[0-9]\\d*.[0-9]\\d*..*")) {
            return "1.0.0";
        }
        String[] parts = versionName.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    /** 原始版本名（不做规整）。 */
    public static String getRawVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    /** 版本号。 */
    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            // PackageManager.GET_META_DATA = 16384
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 16384);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
