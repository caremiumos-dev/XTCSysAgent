package com.xtc.httplib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.text.TextUtils;
import com.xtc.httplib.bean.AppInfo;
import com.xtc.httplib.net.BaseUrlManager;
import com.xtc.httplib.okhttp.DefaultOkHttpClient;
import com.xtc.httplib.okhttp.MonitorInterceptor;
import com.xtc.httplib.okhttp.OnGetAppInfoListener;
import com.xtc.httplib.okhttp.PreprocessorInterceptor;
import com.xtc.log.LogUtil;
import com.xtc.system.account.bean.AppInfoBase;
import com.xtc.utils.system.LanguageUtils;
import com.xtc.utils.system.TimeZoneUtils;
import com.xtc.utils.system.constant.LanguageConstants;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * HTTP 库的全局入口（单例）。
 *
 * <p>负责：创建 OkHttp 客户端、监听 initService 下发的 RSA/加密/灰度参数、
 * 维护 Accept-Language 与 Watch-Time-Zone 两个会进请求头的值、
 * 以及把各类开关（监控、超时、协议、透传）转发到对应拦截器。</p>
 */
public class HttpManager {

    private static final String TAG = LogTag.tag("HttpManager");

    private static final String CONTENT_WATCHID_TYPE =
            "content://com.xtc.provider/BaseDataProvider/watchId/1";

    private static volatile HttpManager httpManager;

    protected Context context;
    private HttpClient httpClient;
    private OnGetAppInfoListener onGetAppInfoListener;
    private AppInfoReceiver appInfoReceiver;

    public String acceptLanguage;
    public String watchTimeZone;

    /**
     * 监听 initService 的广播，动态更新 RSA 公钥、加密开关、灰度码与 watchId。
     */
    private class AppInfoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i("receive app info update receiver,action:" + action);
            if (TextUtils.isEmpty(action)) {
                LogUtil.e("action is null");
                return;
            }
            if (!action.equals("com.xtc.initservice.action.NET_PARAM_UPDATE")) {
                if (action.equals("com.xtc.initservice.action.INIT_SUCCESS")) {
                    HttpManager.this.httpClient.setWatchId(getWatchIdByProvider(context));
                }
                return;
            }
            String rsa = intent.getStringExtra(AppInfoBase.KEY_RSA);
            String encSwitch = intent.getStringExtra(AppInfoBase.KEY_ENC_SWITCH);
            String grey = intent.getStringExtra(AppInfoBase.KEY_GREY);
            AppInfo appInfo = HttpManager.this.httpClient.getAppInfo();
            if (appInfo == null) {
                appInfo = new AppInfo();
                HttpManager.this.httpClient.setAppInfo(appInfo);
            }
            if (!TextUtils.isEmpty(rsa)) {
                appInfo.setRsaPublicKey(rsa);
                LogUtil.i("http set rsaPublicKey:" + rsa);
            }
            if (!TextUtils.isEmpty(encSwitch)) {
                appInfo.setEncSwitch(encSwitch);
                LogUtil.i("http set encSwitch:" + encSwitch);
            }
            if (TextUtils.isEmpty(grey)) {
                return;
            }
            appInfo.setGrey(grey);
            LogUtil.i("http set grey:" + grey);
        }

        void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.xtc.initservice.action.NET_PARAM_UPDATE");
            filter.addAction("com.xtc.initservice.action.INIT_SUCCESS");
            context.getApplicationContext().registerReceiver(this, filter);
        }

        void unregister(Context context) {
            context.getApplicationContext().unregisterReceiver(this);
        }
    }

    private HttpManager(Context context) {
        this.context = context.getApplicationContext();
        initHttpClient();
        this.appInfoReceiver = new AppInfoReceiver();
        this.appInfoReceiver.register(this.context);
    }

    public static HttpManager getInstance(Context context) {
        if (httpManager == null) {
            synchronized (HttpManager.class) {
                if (httpManager == null) {
                    httpManager = new HttpManager(context);
                }
            }
        }
        return httpManager;
    }

    /** 通过 ContentProvider 取 watchId。 */
    public static String getWatchIdByProvider(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_WATCHID_TYPE));
    }

    private void initHttpClient() {
        registerHttpBridge(this.context);
        this.httpClient = new DefaultOkHttpClient(this.context);
    }

    /**
     * 注册 IM 侧的 HTTP 透传桥接。
     *
     * <p>本 APK 里没有 {@code TranspondAdapterImpl}，所以这里必然走
     * ClassNotFoundException 分支 —— 原厂行为也是如此。</p>
     */
    private void registerHttpBridge(Context context) {
        try {
            com.xtc.im.transpond.TranspondManager.setAdapter(
                    (com.xtc.im.transpond.TranspondAdapter) Class
                            .forName("com.xtc.im.transpond.TranspondAdapterImpl")
                            .getConstructor(Context.class)
                            .newInstance(context));
            LogUtil.d(TAG, "register http bridge successful");
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
        } catch (InstantiationException e) {
            LogUtil.e(TAG, e);
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
        }
    }

    public void destroy() {
        this.appInfoReceiver.unregister(this.context);
    }

    /**
     * Accept-Language 请求头。泰语会被收敛成裸的 {@code th}，
     * 因为服务端只认这个形态。
     */
    public String getAcceptLanguage() {
        if (TextUtils.isEmpty(this.acceptLanguage)) {
            String language = LanguageUtils.getLanguage(this.context.getApplicationContext());
            if (language.startsWith(LanguageConstants.Language.TH)) {
                language = LanguageConstants.Language.TH;
            }
            this.acceptLanguage = language;
            LogUtil.i(TAG, "getAcceptLanguage is empty, reset acceptLanguage = " + this.acceptLanguage);
        }
        return this.acceptLanguage;
    }

    AppInfo getAppInfo() {
        if (this.onGetAppInfoListener != null) {
            return this.onGetAppInfoListener.getAppInfo();
        }
        LogUtil.e(TAG, "getAppInfo onGetAppInfoListener is null");
        return null;
    }

    public HttpClient getHttpClient() {
        return this.httpClient;
    }

    public String getWatchTimeZone() {
        if (TextUtils.isEmpty(this.watchTimeZone)) {
            this.watchTimeZone = TimeZoneUtils.getTimeZoneDisplayName();
            LogUtil.i(TAG, "getWatchTimeZone is empty, reset watchTimeZone = " + this.watchTimeZone);
        }
        return this.watchTimeZone;
    }

    public void setAcceptLanguage(String acceptLanguage) {
        LogUtil.i(TAG, "setAcceptLanguage acceptLanguage = " + acceptLanguage);
        this.acceptLanguage = acceptLanguage;
    }

    public void setAllTranspond(boolean allTranspond, List<String> urls) {
        LogUtil.i(TAG, "setAllTranspond allTranspond = " + allTranspond);
        PreprocessorInterceptor.allTranspond = allTranspond;
        if (urls == null || urls.size() == 0) {
            return;
        }
        if (allTranspond) {
            PreprocessorInterceptor.NO_NEED_TRANSPOND.addAll(urls);
        } else {
            PreprocessorInterceptor.NEED_TRANSPOND.addAll(urls);
        }
    }

    public void setBootLimitTime(long limitTime, List<String> urls) {
        LogUtil.i(TAG, "setBootLimitTime limitTime = " + limitTime);
        MonitorInterceptor.BOOT_LIMIT_TIME = limitTime;
        if (urls == null || urls.size() == 0) {
            return;
        }
        MonitorInterceptor.NECESSARY_BOOT_URLS.addAll(urls);
    }

    public void setNoNeedMonitor(List<String> urls) {
        LogUtil.i(TAG, "setNoNeedMonitor noNeedMonitor = " + urls);
        if (urls == null || urls.size() == 0) {
            return;
        }
        MonitorInterceptor.NO_NEED_MONITOR.addAll(urls);
    }

    public void setOnGetAppInfoListener(OnGetAppInfoListener listener) {
        this.onGetAppInfoListener = listener;
    }

    public void setOpenMonitor(boolean openMonitor) {
        LogUtil.i(TAG, "setOpenMonitor openMonitor = " + openMonitor);
        MonitorInterceptor.openMonitor = openMonitor;
    }

    public void setThirdHttpsDomain(List<String> domains) {
        LogUtil.i(TAG, "setThirdHttpsDomain thirdHttpsDomain = " + domains);
        DomainConfig.THIRD_HTTPS_DOMAIN_ARRAY.addAll(domains);
    }

    public void setTimeOut(int connectTimeout, int writeTimeout, int readTimeout) {
        LogUtil.i(TAG, "setTimeOut connectTimeout = " + connectTimeout
                + ",writeTimeout = " + writeTimeout + ",readTimeout = " + readTimeout);
        DefaultOkHttpClient.CONNECT_TIMEOUT = connectTimeout;
        DefaultOkHttpClient.WRITE_TIMEOUT = writeTimeout;
        DefaultOkHttpClient.READ_TIMEOUT = readTimeout;
    }

    public void setUseHttps(boolean useHttps) {
        LogUtil.i(TAG, "setUseHttps use = " + useHttps);
        if (useHttps) {
            BaseUrlManager.PROTOCOL_TYPE = ConfigOptions.ProtocolType.HTTPS;
        } else {
            BaseUrlManager.PROTOCOL_TYPE = ConfigOptions.ProtocolType.HTTP;
        }
    }

    public void setWatchTimeZone(String watchTimeZone) {
        LogUtil.i(TAG, "setWatchTimeZone watchTimeZone = " + watchTimeZone);
        this.watchTimeZone = watchTimeZone;
    }
}
