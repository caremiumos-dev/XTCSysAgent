package com.xtc.system.account.bean;

import android.net.Uri;

/**
 * initService（系统初始化服务）ContentProvider 的契约。
 *
 * <p>其它系统应用通过这个 Provider 读取 RSA 公钥、加密开关、灰度码等参数。</p>
 */
public class AppInfoBase {

    public static final String AUTHORITY = "com.xtc.initservice";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xtc.initservice";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xtc.initservice";
    public static final Uri CONTENT_URI = Uri.parse("content://com.xtc.initservice/item");
    public static final String DEFAULT_SORT_ORDER = "id asc";

    public static final int ITEM = 1;
    public static final int ITEM_ID = 2;

    public static final String KEY_ID = "id";
    public static final String KEY_TAG = "tag";
    public static final String KEY_RSA = "rsaPublicKey";
    public static final String KEY_ENC_SWITCH = "encSwitch";
    public static final String KEY_GREY = "grey";
}
