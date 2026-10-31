package com.xtc.system.account;

/**
 * 设备信息抽象。与 {@code com.xtc.httplib.bean.DeviceInfo} 是同一份契约的两处定义
 * （两个库各自持有），实现类为 {@link WatchDevice}。
 */
public interface Device {

    String BUILD_TYPE_DEBUG = "userdebug";
    String BUILD_TYPE_USER = "user";

    String getAndroidVersion();

    String getBindNumber();

    String getBuildType();

    String getChipId();

    String getMacAddr();

    String getSystemProperty(String key);

    String getWatchVersion();
}
