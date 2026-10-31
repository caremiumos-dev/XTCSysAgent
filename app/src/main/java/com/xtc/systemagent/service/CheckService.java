package com.xtc.systemagent.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.log.serve.impl.CallFailureLogServeImpl;
import com.xtc.systemagent.module.log.serve.impl.MobileNetLogServelmpl;
import com.xtc.systemagent.module.log.serve.impl.ModemLogServeImpl;
import com.xtc.systemagent.module.log.serve.impl.TcpDumpServeImpl;
import com.xtc.systemagent.module.sys.RpmServeImpl;
import com.xtc.systemagent.util.LmkPersistHelper;

/**
 * 开机后的"补做"服务。
 *
 * <p>进程被杀/重启时，上一轮可能还有没来得及上报的采集结果，这里统一补一次：
 * 先解除 LMK 保活，再依次检查 Modem 日志、tcpdump、通话失败日志、
 * 移动网络日志，最后启动 RPM 的延迟上报闹钟。</p>
 *
 * <p>服务不常驻：{@code onStartCommand} 返回 {@code START_NOT_STICKY}(2)。</p>
 */
public class CheckService extends Service {

    private static final String TAG = "Log_CheckService";

    /** {@code Service.START_NOT_STICKY}。 */
    private static final int START_NOT_STICKY = 2;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent == null) {
            LogUtil.e(TAG, "intent = null , return !");
            return;
        }
        long begin = SystemClock.elapsedRealtime();
        checkLog();
        LogUtil.i(TAG, startId + " --> command has done , but do not quit , cost time = "
                + (SystemClock.elapsedRealtime() - begin));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.i(TAG, "onDestroy ---");
    }

    /** 逐个补做各采集模块的"上次未完成"检查。 */
    private void checkLog() {
        LogUtil.w(TAG, "checkLog start -->");
        LmkPersistHelper.unregisterLmkPersist(getPackageName());
        ModemLogServeImpl.getInstance(getApplicationContext()).checkLastModemLog(true);
        TcpDumpServeImpl.getInstance(getApplicationContext()).checkLastTcpDump();
        CallFailureLogServeImpl.getInstance(getApplicationContext()).checkLastCallLog();
        MobileNetLogServelmpl.getInstance(getApplicationContext()).checkLastMoblieNetWorkLog();
        RpmServeImpl.getInstance(getApplicationContext()).startFeedDelayAlarm();
    }
}
