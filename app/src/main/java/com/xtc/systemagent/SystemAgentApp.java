package com.xtc.systemagent;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import com.xtc.bigdata.collector.BehaviorCollector;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.common.bigdata.BehaviorUtil;
import com.xtc.httplib.HttpManager;
import com.xtc.httplib.bean.AppInfo;
import com.xtc.httplib.okhttp.OnGetAppInfoListener;
import com.xtc.log.LogConfig;
import com.xtc.log.LogUtil;
import com.xtc.log.crash.CrashHandler;
import com.xtc.log.crash.CrashListener;
import com.xtc.system.account.AppInfoImpl;
import com.xtc.systemagent.module.log.serve.impl.ModemLogServeImpl;
import com.xtc.systemagent.module.log.serve.impl.TcpDumpServeImpl;
import com.xtc.systemagent.module.watchDrop.WatchDrop;
import com.xtc.systemagent.service.CheckService;
import com.xtc.utils.storage.FolderManager;

/**
 * 应用入口。
 *
 * <p>启动顺序是刻意安排好的：先落盘目录（后面的日志要写进去），
 * 再初始化日志与埋点，然后装崩溃监听、注册 HTTP 参数提供者，
 * 最后拉起常驻检查服务与通话状态广播。</p>
 */
public class SystemAgentApp extends Application {

    private static final String TAG = "Log_SystemAgentApp";

    /** 采集模块名。 */
    private static final String MODULE_NAME = "ibwatch";

    /** 目录名。 */
    private static final String APP_NAME = "SystemAgent";

    /** 埋点的宿主应用 id。 */
    private static final String HOST_APP_ID = "com.xtc.i3launcher";

    /** 设备类型。 */
    private static final String DEVICE_TYPE = "watch";

    @Override
    public void onCreate() {
        super.onCreate();
        initFolder();
        initLog();
        initBigData();
        initCustomCrashHandler();
        initHttpCreator();
        startServiceToCheck();
        registerCallReceiver();
    }

    /** 初始化应用私有目录。 */
    private void initFolder() {
        FolderManager.getInstance().init(MODULE_NAME, APP_NAME);
    }

    /** 初始化日志：控制台 + 落盘。 */
    private void initLog() {
        LogConfig.builder()
                .isPrintConsole(true)
                .saveLog(true)
                .module(MODULE_NAME)
                .appName(BuildConfig.APPLICATION_ID)
                .build(LogConfig.Builder.BuildMode.Android, new Object[0]);
    }

    /** 初始化行为采集 SDK。 */
    private void initBigData() {
        LogUtil.d(TAG, "initBigData ===");
        BehaviorUtil.init(new BehaviorCollector.Builder(this)
                .setHostAppId(HOST_APP_ID)
                .setDeviceType(DEVICE_TYPE)
                .openActivityDurationTrack(true)
                .setDebugMode(false)
                .build());
    }

    /** 崩溃时把堆栈交给埋点 SDK 落盘，下次启动再上报。 */
    private void initCustomCrashHandler() {
        LogUtil.d(TAG, "initCustomCrashHandler -->");
        CrashHandler.setCrashListener(new CrashListener() {
            @Override
            public void onUncaughtException(Thread thread, Throwable throwable) {
                BehaviorCollector.getInstance().onCrash(throwable);
            }
        });
    }

    /** 把版本 / 灰度 / 加密开关 / RSA 公钥交给 HTTP 库。 */
    private void initHttpCreator() {
        LogUtil.d(TAG, "initHttpCreator -->");
        HttpManager.getInstance(this).setOnGetAppInfoListener(new OnGetAppInfoListener() {
            @Override
            public AppInfo getAppInfo() {
                AppInfoImpl impl = AppInfoImpl.getDefaultInstance(
                        SystemAgentApp.this.getApplicationContext());
                AppInfo appInfo = new AppInfo();
                appInfo.setEncSwitch(impl.getEncSwitch());
                appInfo.setGrey(impl.getGrey());
                appInfo.setRsaPublicKey(impl.getRsaPublicKey());
                appInfo.setVersion(impl.getVersion());
                return appInfo;
            }
        });
    }

    /** 拉起常驻检查服务。 */
    private void startServiceToCheck() {
        LogUtil.d(TAG, "startServiceToCheck -->");
        startService(new Intent(this, CheckService.class));
    }

    /** 监听通话状态，转给 Modem / TcpDump 两个采集器。 */
    public void registerCallReceiver() {
        LogUtil.d(TAG, "registerCallReceiver -->");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "callReceiver , action = " + action);
                if ("android.intent.action.PHONE_STATE".equals(action)) {
                    SystemAgentApp.this.dealPhoneStateChanged();
                }
            }
        }, new IntentFilter("android.intent.action.PHONE_STATE"));
    }

    /**
     * 通话状态变化处理。
     *
     * <p>空闲(0) 视为一通电话结束，响铃/摘机(1/2) 视为开始。</p>
     */
    private void dealPhoneStateChanged() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Constants.PHONE);
        if (telephonyManager == null) {
            LogUtil.e(TAG, "error , TelephonyManager = null , return !");
        }
        int callState = telephonyManager.getCallState();
        LogUtil.d(TAG, "callReceiver callState = " + callState);
        switch (callState) {
            case TelephonyManager.CALL_STATE_IDLE:
                LogUtil.i(TAG, "dealCallIdle -->");
                ModemLogServeImpl.getInstance(this).dealCallIdle();
                TcpDumpServeImpl.getInstance(this).dealCallIdle();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                LogUtil.d(TAG, "dealCallIdle -->");
                ModemLogServeImpl.getInstance(this).dealCallRinging();
                TcpDumpServeImpl.getInstance(this).dealCallRinging();
                break;
            default:
                break;
        }
    }
}
