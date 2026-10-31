package com.xtc.httplib.bean;

/**
 * 设备信息抽象，用于拼请求头。实现在 {@code WatchInfo}。
 */
public interface DeviceInfo {

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
