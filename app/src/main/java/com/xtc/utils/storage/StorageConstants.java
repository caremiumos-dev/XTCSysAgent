package com.xtc.utils.storage;

/**
 * 存储模块使用的目录名与文件名常量。
 *
 * <p>这些名字会直接拼进外部存储的路径里，改动会影响历史数据的可读性，
 * 因此必须与原厂保持一致。</p>
 */
public class StorageConstants {

    /** 通用数据目录。 */
    public static final String DIR_COMMON = "common";
    /** 七牛上传缓存目录。 */
    public static final String DIR_QINIU = "qiniu";
    /** 崩溃日志目录。 */
    public static final String DIR_CRASH = "crash";
    /** 运行日志目录。 */
    public static final String DIR_LOGS = "logs";
    /** 头像缓存目录。 */
    public static final String DIR_HEAD = "head";

    /** 常量落盘文件名。 */
    public static String FILE_CONSTANTS = "constants.txt";

    /** 应用配置目录名。 */
    public static final String DIR_APP_CONFIG = "AppConfig";
}
