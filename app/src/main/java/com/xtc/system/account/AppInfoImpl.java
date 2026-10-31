package com.xtc.system.account;

import android.content.Context;
import com.xtc.system.account.bean.HttpConfig;

/**
 * 应用信息（版本 / RSA 公钥 / 加密开关 / 灰度码 / registId）的进程内单例。
 *
 * <p>{@code com.xtc.httplib} 通过它把请求头需要的参数取出来。</p>
 */
public class AppInfoImpl {

    private static final String TAG = "AppInfoImpl";

    private static volatile AppInfoImpl instance;

    private OnInitListener listener;
    private HttpConfig httpConfig;
    private String version;
    private long registId;

    /** 初始化完成回调，参数表示是否成功读到 httpConfig。 */
    public interface OnInitListener {
        void onFinish(boolean success);
    }

    public AppInfoImpl(Context context, OnInitListener listener) {
        this.listener = listener;
        initRsaPublicKeyAndGrey(context);
        initVersion(context);
    }

    public static AppInfoImpl getDefaultInstance(Context context) {
        if (instance == null) {
            synchronized (AppInfoImpl.class) {
                if (instance == null) {
                    instance = new AppInfoImpl(context, null);
                }
            }
        }
        return instance;
    }

    private void initRegistId() {
        this.registId = SDCardUtil.getRegistId();
    }

    private void initRsaPublicKeyAndGrey(Context context) {
        this.httpConfig = InitServiceData.getAppInfo(context);
        if (this.listener != null) {
            this.listener.onFinish(this.httpConfig != null);
        }
    }

    /**
     * 版本号统一加 {@code W_} 前缀（手表版本约定）；取不到时置 null。
     */
    private void initVersion(Context context) {
        String watchVersion = new WatchDevice(context).getWatchVersion();
        if ("unkown".equals(watchVersion)) {
            this.version = null;
        } else {
            this.version = "W_" + watchVersion;
        }
    }

    public String getEncSwitch() {
        if (this.httpConfig != null) {
            return this.httpConfig.getEncSwitch();
        }
        return null;
    }

    public String getGrey() {
        if (this.httpConfig != null) {
            return this.httpConfig.getGrey();
        }
        return null;
    }

    public Long getRegistId() {
        if (this.registId == 0) {
            initRegistId();
        }
        return Long.valueOf(this.registId);
    }

    public String getRsaPublicKey() {
        if (this.httpConfig != null) {
            return this.httpConfig.getRsaPublicKey();
        }
        return null;
    }

    public String getVersion() {
        return this.version;
    }
}
