package com.xtc.httplib;

/**
 * HTTP 库的协议常量集合：请求头键名、监控阈值、错误码、协议前缀等。
 */
public interface ConfigOptions {

    /** 请求头键名。 */
    interface HeaderKey {
        String ACCEPT_LANGUAGE = "Accept-Language";
        String AUTH_ID = "authId";
        String BASE_REQUEST_PARAM = "Base-Request-Param";
        String CONTENT_ENCODING = "Content-Encoding";
        String CONTENT_TYPE = "Content-Type";
        String EEBBK_KEY = "Eebbk-Key";
        String EEBBK_SIGN = "Eebbk-Sign";
        String ENCRYPTED = "encrypted";
        String GREY = "Grey";
        String HOST = "Host";
        String IM_SDK_VERSION_CODE = "imSdkVersion";
        String MEDIA_TYPE = "application/json;charset=utf-8";
        String MODEL = "model";
        String PACKAGE_VERSION = "packageVersion";
        String PROGRAM = "program";
        String VERSION = "Version";
        String WATCH_TIME_ZONE = "Watch-Time-Zone";
    }

    /** 监控阈值。 */
    interface MonitorCondition {
        /** 单日最大流量：500MB。 */
        long DAY_REQUEST_MAX_TRAFFIC = 524288000L;
        /** 统计"频繁请求"的窗口内请求次数。 */
        int FREQUENT_REQUEST_COUNT = 5;
        /** 频繁请求统计窗口：60 秒。 */
        long FREQUENT_REQUEST_PERIOD = 60000;
        /** 请求超时：20 秒。 */
        int REQUESTING_TIME_OUT = 20000;
        /** 单次请求最大流量：10KB。 */
        long REQUEST_MAX_TRAFFIC = 10240;
    }

    /** 监控错误码。 */
    interface MonitorError {
        int MONITOR_ERROR_BOOT_LIMIT_TIME = 1001;
        int MONITOR_ERROR_FREQUENT_REQUEST = 1003;
        int MONITOR_ERROR_HEAVY_TRAFFIC = 1004;
        int MONITOR_ERROR_REQUESTING = 1002;
    }

    /** 协议前缀。 */
    interface ProtocolType {
        String HTTP = "http://";
        String HTTPS = "https://";
    }

    /** 响应体限制。 */
    interface ResponseCondition {
        /** 响应体最大 1MB。 */
        int MAX_BODY_SIZE = 1048576;
    }

    /** 透传条件。 */
    interface TranspondCondition {
        int INTERVAL_TIME = 600000;
        int TRANSPOND_TIME_OUT = 20000;
    }
}
