package com.xtc.bigdata.collector.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.xtc.log.LogUtil;

/**
 * 应用信息读取（应用 id / 模块名 / 版本名）。
 *
 * <p>应用 id 不是包名，而是包名的 MD5 再转 16 进制 ——
 * 服务端不希望明文存包名。</p>
 */
public class AppUtils {

    private AppUtils() {
    }

    /**
     * 取应用 id（包名的 MD5，16 进制）。
     *
     * @param context 上下文
     * @return 应用 id
     * @throws Exception 摘要算法不可用
     */
    public static String getAppId(Context context) throws Exception {
        return new MD5Coder(16).encode(context.getPackageName().getBytes("UTF-8"));
    }

    /**
     * 取模块名（应用在桌面上显示的名字）。
     *
     * @param context 上下文
     * @return 模块名；取不到返回空串
     */
    public static String getModuleName(Context context) {
        String moduleName = "";
        try {
            moduleName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .applicationInfo.loadLabel(context.getPackageManager()).toString();
            return TextUtils.isEmpty(moduleName) ? "" : moduleName;
        } catch (Exception e) {
            LogUtil.e("AppUtils", e);
            return moduleName;
        }
    }

    /**
     * 取本应用版本名。
     *
     * @param context 上下文
     * @return 版本名；取不到返回 null
     */
    public static String getVersionName(Context context) {
        return getVersionName(context, context.getPackageName());
    }

    /**
     * 取指定包名的版本名。
     *
     * @param context     上下文
     * @param packageName 目标包名
     * @return 版本名；包不存在或拿不到 PackageInfo 时返回 null
     */
    public static String getVersionName(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getApplicationContext().getPackageManager()
                    .getPackageInfo(packageName, 0);
            if (packageInfo == null) {
                return null;
            }
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
