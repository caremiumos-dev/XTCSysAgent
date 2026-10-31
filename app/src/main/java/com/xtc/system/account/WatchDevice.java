package com.xtc.system.account;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.SystemConstants;

/**
 * 手表设备的只读信息，每个字段都懒加载并缓存。
 *
 * <p>读取系统属性走反射 {@code android.os.SystemProperties.get(String)} ——
 * 这个类不是公开 API，只能反射。读不到时统一返回 {@link #UNKOWN}（原厂拼写，保留）。</p>
 */
public class WatchDevice implements Device {

    private static final String TAG = "WatchDevice";

    /** 读不到时的占位值。原厂拼写就是 "unkown"，不能改成 unknown。 */
    public static final String UNKOWN = "unkown";

    private String androidVersion;
    private String bindNumber;
    private String chipId;
    private String macAddr;
    private String watchVersion;
    private Context context;

    public WatchDevice(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getAndroidVersion() {
        if (!TextUtils.isEmpty(this.androidVersion)) {
            return this.androidVersion;
        }
        this.androidVersion = getSystemProperty("ro.build.version.release");
        if (TextUtils.isEmpty(this.androidVersion)) {
            this.androidVersion = UNKOWN;
        }
        return this.androidVersion;
    }

    @Override
    public String getBindNumber() {
        if (!TextUtils.isEmpty(this.bindNumber)) {
            return this.bindNumber;
        }
        this.bindNumber = getSystemProperty(SystemConstants.PROP_BIND_NUMBER);
        if (TextUtils.isEmpty(this.bindNumber)) {
            this.bindNumber = UNKOWN;
        }
        return this.bindNumber;
    }

    @Override
    public String getBuildType() {
        return getSystemProperty("ro.build.type");
    }

    @Override
    public String getChipId() {
        if (!TextUtils.isEmpty(this.chipId)) {
            return this.chipId;
        }
        this.chipId = getSystemProperty("ro.boot.xtc.chipid");
        if (TextUtils.isEmpty(this.chipId)) {
            this.chipId = UNKOWN;
        }
        return this.chipId;
    }

    @Override
    public String getMacAddr() {
        if (!TextUtils.isEmpty(this.macAddr)) {
            return this.macAddr;
        }
        this.macAddr = UNKOWN;
        WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return this.macAddr;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return this.macAddr;
        }
        this.macAddr = wifiInfo.getMacAddress();
        if (TextUtils.isEmpty(this.macAddr)) {
            this.macAddr = UNKOWN;
        }
        return this.macAddr;
    }

    /** 反射读系统属性。失败只打日志、返回 null。 */
    @Override
    public String getSystemProperty(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return (String) cls.getMethod("get", String.class).invoke(cls, key);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    @Override
    public String getWatchVersion() {
        if (!TextUtils.isEmpty(this.watchVersion)) {
            return this.watchVersion;
        }
        this.watchVersion = getSystemProperty(SystemConstants.PROP_SOFT_VERSION);
        if (TextUtils.isEmpty(this.watchVersion)) {
            this.watchVersion = UNKOWN;
        }
        return this.watchVersion;
    }
}
