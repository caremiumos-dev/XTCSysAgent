package com.xtc.system.account.constant;

/**
 * 账号模块的其它杂项常量。
 */
public interface OtherConstants {
    /** 手表账号 Provider 的 authority。 */
    String WATCH_PROVIDER_AUTHORITY = "com.xtc.provider";
    /** 取 watchId 的 URI。 */
    String URI_WATCH_ID = "content://com.xtc.provider/BaseDataProvider/watchId/1";
    /** 设备型号在参数里的键。 */
    String KEY_DEVICE_MODEL = "model";
    /** 机型内部代号键。 */
    String KEY_INNER_MODEL = "innermodel";
}
