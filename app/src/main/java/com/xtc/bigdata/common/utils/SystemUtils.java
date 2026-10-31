package com.xtc.bigdata.common.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.xtc.log.LogUtil;

import java.util.UUID;

/**
 * 系统信息采集（渠道号 / 路由器 mac / uuid）。
 */
public class SystemUtils {

    private static final String TAG = "SystemUtils";

    private SystemUtils() {
    }

    /**
     * 读 AndroidManifest 里的 {@code ChannleId} 元数据。
     *
     * <p>注意 meta-data 的键名原厂就拼成 {@code ChannleId}（少一个 n），
     * 改了就读不到渠道号。</p>
     *
     * @param context 上下文
     * @return 渠道号；没有元数据或取不到时返回空串
     */
    public static String getChannleId(Context context) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 128);
            return applicationInfo.metaData != null ? applicationInfo.metaData.getString("ChannleId") : "";
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e(TAG, e);
            return "";
        }
    }

    /**
     * 取路由器 mac（这里实际取的是本机 wifi/网卡 mac）。
     *
     * @param context 上下文
     * @return mac 地址
     */
    public static String getRouterMac(Context context) {
        return DeviceUtils.getMac(context);
    }

    /**
     * 生成去掉连字符的 uuid，作为会话 id 的随机部分。
     *
     * @return 32 位小写十六进制串
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
