package com.xtc.httplib;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.httplib.bean.AppInfo;
import com.xtc.httplib.bean.DeviceInfo;
import com.xtc.httplib.bean.WatchInfo;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Request;

/**
 * HTTP 客户端的抽象。真实实现是 {@code DefaultOkHttpClient}，
 * 这里只放公共状态（AppInfo / DeviceInfo / watchId / registId）与空实现骨架。
 */
public abstract class HttpClient {

    private static final String TAG = LogTag.tag("HttpClient");

    protected Context context;
    private DeviceInfo deviceInfo;
    private AppInfo appInfo;
    private String watchId;
    private long registId;

    public HttpClient(Context context) {
        this.context = context.getApplicationContext();
        this.deviceInfo = new WatchInfo(context);
    }

    /** 允许子类往 OkHttp 客户端里塞自定义头。 */
    public void addHeadToOkHttpClient(Map<String, String> headers) {
    }

    public AppInfo getAppInfo() {
        if (this.appInfo == null) {
            this.appInfo = HttpManager.getInstance(this.context).getAppInfo();
        }
        return this.appInfo;
    }

    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    public long getRegistId() {
        return this.registId;
    }

    public String getWatchId() {
        if (TextUtils.isEmpty(this.watchId)) {
            this.watchId = HttpManager.getWatchIdByProvider(this.context);
        }
        return this.watchId;
    }

    public Call newCall(Request request) {
        return null;
    }

    public <T> T request(String baseUrl, Class<T> service) {
        return null;
    }

    public <T> T requestSync(String baseUrl, Class<T> service) {
        return null;
    }

    public <T> T requestThird(String baseUrl, Class<T> service) {
        return null;
    }

    public <T> T requestThirdSync(String baseUrl, Class<T> service) {
        return null;
    }

    HttpClient setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
        return this;
    }

    public HttpClient setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
    }

    public HttpClient setRegistId(long registId) {
        this.registId = registId;
        return this;
    }

    HttpClient setWatchId(String watchId) {
        this.watchId = watchId;
        return this;
    }
}
