package com.xtc.systemagent.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.sys.ImDeleteFileServeImpl;
import com.xtc.systemagent.module.sys.RpmServeImpl;
import com.xtc.systemagent.module.sys.SysConfigServeImpl;
import com.xtc.systemagent.util.LmkPersistHelper;

/**
 * 系统指令服务：把 IM 下发的运维指令放到后台线程执行。
 *
 * <p>指令类型：1 = 设置 RPM 调试值，2 = 下发系统配置，3 = 删除系统文件。</p>
 *
 * <p>用 {@link HandlerThread} 而不是 {@code IntentService}，是为了让服务在
 * 处理完一条指令后继续活着等下一次（原厂注释写的就是"not quit"）。</p>
 */
public class SysImService extends Service {

    private static final String TAG = "Log_SysImService";

    /** {@code Service.START_NOT_STICKY}。 */
    private static final int START_NOT_STICKY = 2;

    private static final String EXTRA_CONTENT = "content";
    private static final String EXTRA_ACTION_INT = "actionInt";

    private static final int ACTION_SET_RPM = 1;
    private static final int ACTION_SET_SYS_CONFIG = 2;
    private static final int ACTION_DELETE_SYS_FILE = 3;

    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;

    /** 在后台 Looper 上串行执行指令。 */
    private final class ServiceHandler extends Handler {

        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogUtil.i(TAG, "handleMessage startId = " + message.arg1);
            SysImService.this.onHandleIntent((Intent) message.obj);
            LogUtil.i(TAG, "onHandleIntent --> one msg completed , but not quit ~");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate --> start looper");
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mServiceLooper = handlerThread.getLooper();
        this.mServiceHandler = new ServiceHandler(this.mServiceLooper);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message message = this.mServiceHandler.obtainMessage();
        message.arg1 = startId;
        message.obj = intent;
        this.mServiceHandler.sendMessage(message);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy --> quit looper");
        this.mServiceLooper.quit();
        LmkPersistHelper.unregisterLmkPersist(getPackageName());
    }

    /**
     * 处理一条指令。
     *
     * <p>注意原厂在 {@code intent == null} 时只打日志、**不 return**，
     * 紧接着就会 NPE —— 这是原样保留的行为。</p>
     */
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            LogUtil.e(TAG, "intent is null , return !");
            return;
        }
        int actionInt = intent.getIntExtra(EXTRA_ACTION_INT, -1);
        String content = intent.getStringExtra(EXTRA_CONTENT);
        LogUtil.i(TAG, "actionInt = " + actionInt + " , content = " + content);
        if (TextUtils.isEmpty(content)) {
            LogUtil.e(TAG, "error , content is null , return !");
            return;
        }
        switch (actionInt) {
            case ACTION_SET_RPM:
                setRpmDebugValue(content);
                break;
            case ACTION_SET_SYS_CONFIG:
                setSysConfig(content);
                break;
            case ACTION_DELETE_SYS_FILE:
                deleteSysFile(content);
                break;
            default:
                LogUtil.w(TAG, "unknown actionInt = " + actionInt);
                break;
        }
    }

    private void setRpmDebugValue(String content) {
        RpmServeImpl.getInstance(getApplicationContext()).setRpmByImContent(content);
    }

    private void setSysConfig(String content) {
        SysConfigServeImpl.getInstance(getApplicationContext()).setSysConfigImContent(content);
    }

    private void deleteSysFile(String content) {
        ImDeleteFileServeImpl.getInstance(getApplicationContext()).deleteSysFileImContent(content);
    }
}
