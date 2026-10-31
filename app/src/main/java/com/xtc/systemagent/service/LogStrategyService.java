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
import com.xtc.systemagent.module.Constants;
import com.xtc.systemagent.module.log.serve.impl.AdspDumpServeImpl;
import com.xtc.systemagent.module.log.serve.impl.CallFailureLogServeImpl;
import com.xtc.systemagent.module.log.serve.impl.LogCollectServiceImpl;
import com.xtc.systemagent.module.log.serve.impl.MobileNetLogServelmpl;
import com.xtc.systemagent.module.log.serve.impl.ModemDumpServeImpl;
import com.xtc.systemagent.module.log.serve.impl.ModemLogServeImpl;
import com.xtc.systemagent.module.log.serve.impl.TcpDumpServeImpl;
import com.xtc.systemagent.module.log.serve.impl.WifiLogServeImpl;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.util.LmkPersistHelper;
import com.xtc.systemagent.util.PropertiesUtil;
import com.xtc.utils.common.CollectionUtils;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 日志采集策略的分发服务。
 *
 * <p>launcher 把一份 {@code LogStrategy} 数组 JSON 发过来，这里按
 * {@code appPackage} 把策略分桶，再交给对应的采集实现：</p>
 *
 * <table>
 *   <tr><td>tcpdump</td><td>抓包</td></tr>
 *   <tr><td>modem_log</td><td>Modem 日志</td></tr>
 *   <tr><td>cancel_modem_log</td><td>取消 Modem 抓取</td></tr>
 *   <tr><td>call_failure_log</td><td>掉话监控</td></tr>
 *   <tr><td>modem_dump / adsp_dump</td><td>抓 dump</td></tr>
 *   <tr><td>wifi_log / mobile_network</td><td>WiFi / 移动网络日志</td></tr>
 *   <tr><td>其它</td><td>走通用上传</td></tr>
 * </table>
 */
public class LogStrategyService extends Service {

    private static final String TAG = "Log_LogStrategyService";

    /** {@code Service.START_NOT_STICKY}。 */
    private static final int START_NOT_STICKY = 2;

    private static final String EXTRA_CONTENT = "content";
    private static final String EXTRA_ACTION_INT = "actionInt";

    /** 收集日志。 */
    private static final int ACTION_COLLECTION_LOG = 1;
    /** 同步日志策略。 */
    private static final int ACTION_LOG_STRATEGY_INIT = 4;

    /** 策略文件的更新时间键。 */
    private static final String KEY_UPDATE_TIME = "updateTime";

    /** 策略有效期：24 小时。 */
    private static final long STRATEGY_VALID_TIME = 86400000L;

    /** 上传前先让磁盘信息日志缓存刷出去。 */
    private static final String PROP_DISKINFO = "log.tag.diskinf";

    /** 上传前延迟 5 秒，等日志落盘。 */
    private static final long UPLOAD_DELAY = 5000L;

    /** 掉话类型标记。 */
    private static final String LOG_TYPE_ERROR = "isError";

    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;

    private final class ServiceHandler extends Handler {

        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogUtil.i(TAG, "handleMessage startId = " + message.arg1);
            LogStrategyService.this.onHandleIntent((Intent) message.obj);
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

    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            LogUtil.e(TAG, "intent is null , return !");
            return;
        }
        int actionInt = intent.getIntExtra(EXTRA_ACTION_INT, -1);
        String content = intent.getStringExtra(EXTRA_CONTENT);
        LogUtil.i(TAG, "actionInt = " + actionInt + " , content = " + content);
        // 类型 4 本身就是"没有 content"的同步指令，所以不校验 content。
        if (ACTION_LOG_STRATEGY_INIT != actionInt && TextUtils.isEmpty(content)) {
            LogUtil.e(TAG, "error , content is null , return !");
            return;
        }
        switch (actionInt) {
            case ACTION_COLLECTION_LOG:
                dealLogStrategyInit();
                dealCollectionLog(content);
                break;
            case ACTION_LOG_STRATEGY_INIT:
                break;
            default:
                LogUtil.w(TAG, "unknown actionInt = " + actionInt);
                break;
        }
    }

    /**
     * 策略初始化：本地缓存的策略未过期（24h）就跳过，否则重新拉一次。
     */
    private void dealLogStrategyInit() {
        LogUtil.i(TAG, "dealLogStrategyInit -->");
        boolean needInit = true;
        Properties properties = PropertiesUtil.loadConfig();
        if (properties != null) {
            String lastUpdateTimeStr = properties.getProperty(KEY_UPDATE_TIME, "0");
            LogUtil.d(TAG, "lastUpdateTimeStr = " + lastUpdateTimeStr);
            if (System.currentTimeMillis() - Long.valueOf(lastUpdateTimeStr).longValue()
                    < STRATEGY_VALID_TIME) {
                needInit = false;
            }
        }
        if (!needInit) {
            LogUtil.i(TAG, "不需要 重新初始化 log策略 , return !");
            return;
        }
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(bindNumber)) {
            LogUtil.e(TAG, "bindNumber = null , return !");
        } else {
            new LogCollectServiceImpl(this).initLogCollectStrategy(bindNumber);
        }
    }

    /** 解析策略数组并按 appPackage 分桶后分发。 */
    private void dealCollectionLog(String content) {
        if (TextUtils.isEmpty(content)) {
            LogUtil.e(TAG, "dealCollectionLog , content is null , return !");
            return;
        }
        List<LogStrategy> strategies = (List<LogStrategy>) JSONUtil.toCollection(
                content, List.class, LogStrategy.class);
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is null , return !");
            return;
        }
        List<LogStrategy> defaultLogs = new ArrayList<>();
        List<LogStrategy> tcpDumpLogs = new ArrayList<>();
        List<LogStrategy> modemLogs = new ArrayList<>();
        List<LogStrategy> cancelModemLogs = new ArrayList<>();
        List<LogStrategy> callFailureLogs = new ArrayList<>();
        List<LogStrategy> modemDumpLogs = new ArrayList<>();
        List<LogStrategy> adspDumpLogs = new ArrayList<>();
        List<LogStrategy> wifiLogs = new ArrayList<>();
        List<LogStrategy> mobileNetLogs = new ArrayList<>();
        for (LogStrategy strategy : strategies) {
            String appPackage = strategy.getAppPackage();
            if (TextUtils.isEmpty(appPackage)) {
                defaultLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_TCP_DUMP)) {
                tcpDumpLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_MODEM_LOG)) {
                modemLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_CANCEL_MODEM_LOG)) {
                cancelModemLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_CALL_FAILURE_LOG)) {
                callFailureLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_MODEM_DUMP)) {
                modemDumpLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_ADSP_DUMP)) {
                adspDumpLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_WIFI_LOG)) {
                wifiLogs.add(strategy);
            } else if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_MOBILE_NETWORK)) {
                mobileNetLogs.add(strategy);
            } else {
                defaultLogs.add(strategy);
            }
        }
        if (tcpDumpLogs.size() > 0) {
            dealTcpDump(tcpDumpLogs);
        }
        if (modemLogs.size() > 0) {
            dealModemLog(modemLogs);
        }
        if (cancelModemLogs.size() > 0) {
            dealCancelModemLog(cancelModemLogs);
        }
        if (defaultLogs.size() > 0) {
            dealUploadLog(defaultLogs);
        }
        if (callFailureLogs.size() > 0) {
            dealCallFailureLog(callFailureLogs);
        }
        if (modemDumpLogs.size() > 0) {
            dealModemDumpLog(modemDumpLogs);
        }
        if (adspDumpLogs.size() > 0) {
            dealAdspDumpLog(adspDumpLogs);
        }
        if (wifiLogs.size() > 0) {
            dealWifiLog(wifiLogs);
        }
        if (mobileNetLogs.size() > 0) {
            dealMobileNetLog(mobileNetLogs);
        }
    }

    private void dealTcpDump(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealTcpDump , 抓 tcpDump -->");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty , return !");
        } else {
            TcpDumpServeImpl.getInstance(getApplicationContext()).startTcpDump(strategies, false);
        }
    }

    private void dealModemLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealModemLog , 抓 modem的log");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
        } else {
            CallFailureLogServeImpl.getInstance(getApplicationContext())
                    .cancelCallFailLog(strategies);
            ModemLogServeImpl.getInstance(getApplicationContext()).fetchingModemLog(strategies);
        }
    }

    private void dealCancelModemLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealCancelModemLog , 取消 modem的抓取");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
        } else {
            ModemLogServeImpl.getInstance(getApplicationContext()).cancelModemLog(strategies);
        }
    }

    /**
     * 掉话监控。
     *
     * <p>{@code extra.logType == "isError"} 表示这是一条"已经失败"的通知，
     * 直接广播出去让通话应用处理；否则启动首轮掉话监听。</p>
     */
    private void dealCallFailureLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "监控掉话异常 , logStrategies = " + strategies);
        if (CollectionUtils.isEmpty(strategies)) {
            return;
        }
        LogStrategy strategy = strategies.get(0);
        if (strategy == null || strategy.getExtra() == null) {
            LogUtil.e(TAG, "extra message is null !");
        } else if (LOG_TYPE_ERROR.equals(strategy.getExtra().getLogType())) {
            LogUtil.i(TAG, "negative call , prepare ~");
            sendBroadcast(new Intent(Constants.callLogBroadcast.CALL_LOG_OTHER_CALL_ERROR));
        } else {
            LogUtil.i(TAG, "prepare first loop monitor ~");
            CallFailureLogServeImpl.getInstance(getApplicationContext())
                    .startDealCallLog(strategies, !strategy.getExtra().isUnActive());
        }
    }

    private void dealModemDumpLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealModemDumpLog , 抓 modem dump log");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
        } else {
            ModemDumpServeImpl.getInstance(getApplicationContext()).startModemDump(strategies);
        }
    }

    private void dealAdspDumpLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealAdspDumpLog , 抓 adsp dump log");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
        } else {
            AdspDumpServeImpl.getInstance(getApplicationContext()).startAdspDump(strategies);
        }
    }

    private void dealWifiLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealWifiLog , 抓 wifi log");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
        } else {
            WifiLogServeImpl.getInstance(getApplicationContext()).startWifiLog(strategies);
        }
    }

    private void dealMobileNetLog(List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealWifiLog , 抓 MobileNet log - modem log and tcp dump");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
        } else {
            MobileNetLogServelmpl.getInstance(getApplicationContext())
                    .startMobileNetLog(strategies);
        }
    }

    /** 通用日志上传：先刷盘，延迟 5 秒再真正开始上传。 */
    private void dealUploadLog(final List<LogStrategy> strategies) {
        LogUtil.i(TAG, "dealUploadLog 上传日志");
        if (CollectionUtils.isEmpty(strategies)) {
            LogUtil.e(TAG, "logStrategies is empty !");
            return;
        }
        LogUtil.flush();
        SystemPropertiesUtils.set(PROP_DISKINFO, "1");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new LogCollectServiceImpl(LogStrategyService.this.getApplicationContext())
                        .startUploadLog(strategies);
            }
        }, UPLOAD_DELAY);
    }

    /** 上传指定文件。 */
    private void dealUploadFile(String content) {
        LogUtil.flush();
        new LogCollectServiceImpl(getApplicationContext()).uploadFile(content);
    }
}
