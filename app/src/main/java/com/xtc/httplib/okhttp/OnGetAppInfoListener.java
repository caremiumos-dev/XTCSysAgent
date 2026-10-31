package com.xtc.httplib.okhttp;

import com.xtc.httplib.bean.AppInfo;

/**
 * 宿主应用通过它把版本号、灰度码、加密开关、RSA 公钥交给 HTTP 库。
 */
public interface OnGetAppInfoListener {
    AppInfo getAppInfo();
}
