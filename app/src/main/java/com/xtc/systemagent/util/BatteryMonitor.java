package com.xtc.systemagent.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import com.xtc.log.LogUtil;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.module.log.serve.impl.ModemLogServeImpl;
import com.xtc.systemagent.net.bean.ExtraMes;
import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.Arrays;

/**
 * Modem 日志采集期间的耗电看门狗。
 *
 * <p>抓 Modem 日志很费电，所以每 10 分钟采一次电量，和"上一刻""一小时前"
 * 比较：超过策略里配置的阈值就判定异常，立刻收工并上报。</p>
 *
 * <p>只在息屏时采样 —— 亮屏时用户在用，耗电不能算到采集头上。</p>
 */
public class BatteryMonitor {

    private static final String TAG = "Log_BatteryMonitor";

    /** 定时采样的广播。 */
    private static final String BATTERY_MONITOR_ALARM = "com.xtc.i3launcher.ACTION.BatteryMonitor";

    /** 采样间隔：10 分钟。 */
    private static final long MONITOR_INTERVAL = 600000L;

    /** {@code AlarmManager.RTC_WAKEUP}。 */
    private static final int RTC_WAKEUP = 2;

    /** 采样窗口：6 个 10 分钟，正好覆盖一小时。 */
    private static final int LEVEL_COUNT = 6;

    private static volatile BatteryMonitor batteryMonitor;

    private Context context;
    private AlarmManager am;
    private PendingIntent monitorPI;
    private BroadcastReceiver receiver = null;

    /** 最近 6 次采样值，环形复用。 */
    private int[] batteryLevels;
    private int currentIndex;

    private boolean isScreenOn;

    /** 当前采集任务的参数。 */
    private ExtraMes extraModemLog = null;
    /** 当前采集任务的策略。 */
    private LogStrategy strategy = null;

    private BatteryMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.batteryLevels = new int[LEVEL_COUNT];
        this.currentIndex = -1;
        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.isScreenOn = powerManager.isInteractive();
        } else {
            this.isScreenOn = powerManager.isScreenOn();
        }
    }

    public static synchronized BatteryMonitor getInstance(Context context) {
        if (batteryMonitor == null) {
            synchronized (BatteryMonitor.class) {
                if (batteryMonitor == null) {
                    batteryMonitor = new BatteryMonitor(context);
                }
            }
        }
        return batteryMonitor;
    }

    /**
     * 开始监控。
     *
     * @param isFirst   首次启动：清空采样、注册广播
     * @param strategy  触发本次采集的策略
     * @param extraMes  策略里的耗电阈值参数
     */
    public void startBatteryMonitor(boolean isFirst, LogStrategy strategy, ExtraMes extraMes) {
        LogUtil.d(TAG, "startBatteryMonitor isFirst = " + isFirst + " , extraModemLog = " + extraMes);
        if (extraMes == null) {
            LogUtil.e(TAG, "startBatteryMonitor extraModemLog is null");
            return;
        }
        if (this.extraModemLog != null) {
            LogUtil.e(TAG, "startBatteryMonitor , this.extraModemLog is not null, monitoring !");
            return;
        }
        this.extraModemLog = extraMes;
        this.strategy = strategy;
        if (isFirst) {
            LogUtil.i(TAG, "reset level");
            resetBatteryLevel();
            registerReceiver();
        }
        startBatteryMonitor(isFirst);
    }

    /** 停掉监控并清理状态。 */
    public void cancelBatteryMonitor() {
        LogUtil.d(TAG, "cancelBatteryMonitor");
        if (this.receiver != null) {
            this.context.unregisterReceiver(this.receiver);
        }
        if (this.am != null && this.monitorPI != null) {
            this.am.cancel(this.monitorPI);
        }
        this.extraModemLog = null;
        this.strategy = null;
        this.receiver = null;
        resetBatteryLevel();
    }

    /** 清空采样窗口。 */
    private void resetBatteryLevel() {
        for (int i = 0; i < LEVEL_COUNT; i++) {
            this.batteryLevels[i] = 0;
        }
        this.currentIndex = -1;
        LogUtil.d(TAG, "resetBatteryLevel currentIndex = " + this.currentIndex
                + "---batteryLevels = " + Arrays.toString(this.batteryLevels));
    }

    /** 安排下一次采样闹钟。 */
    private void startBatteryMonitor(boolean isFirst) {
        LogUtil.i(TAG, "startBatteryMonitor , isFirst = " + isFirst);
        long triggerAt = SystemClock.elapsedRealtime();
        if (!isFirst) {
            triggerAt += MONITOR_INTERVAL;
        }
        LogUtil.d(TAG, "startBatteryMonitor isFirst = " + isFirst
                + "---AlarmManager = " + this.am + "---alarmTime = " + triggerAt);
        if (this.am != null) {
            this.am.cancel(this.monitorPI);
            this.am.setExact(RTC_WAKEUP, triggerAt, this.monitorPI);
        }
    }

    private void registerReceiver() {
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "BatteryMonitor " + action
                        + "---isScreenOn = " + BatteryMonitor.this.isScreenOn);
                if (BATTERY_MONITOR_ALARM.equals(action)) {
                    BatteryMonitor.this.startBatteryMonitor(false);
                    // 亮屏时不采样，避免把用户自己的耗电算进来。
                    if (BatteryMonitor.this.isScreenOn) {
                        return;
                    }
                    BatteryMonitor.this.dealBatteryMonitor();
                    return;
                }
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    BatteryMonitor.this.isScreenOn = true;
                    BatteryMonitor.this.resetBatteryLevel();
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    BatteryMonitor.this.isScreenOn = false;
                } else {
                    LogUtil.d(TAG, "dont care");
                }
            }
        };
        // 原厂把 NotificationFlag.NOTIFICATION_FLAG_HEADER (0x08000000) 当 PendingIntent flags，
        // 数值上等于 FLAG_UPDATE_CURRENT，这里保留原常量。
        this.monitorPI = PendingIntent.getBroadcast(this.context, 0,
                new Intent(BATTERY_MONITOR_ALARM), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BATTERY_MONITOR_ALARM);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        this.context.registerReceiver(this.receiver, filter);
    }

    /** 采一次电量并判定。 */
    private void dealBatteryMonitor() {
        LogUtil.i(TAG, "check battery cost !");
        int currentBattery = ShellUtil.getCurrentBattery();
        if (this.currentIndex != -1) {
            // 上一次采样值。
            int lastBatteryLevel = this.batteryLevels[this.currentIndex];
            // 一小时前的采样值：环形缓冲区里 currentIndex 的下一个位置。
            int lastHourBatteryLevel = this.batteryLevels[
                    (((this.currentIndex - this.batteryLevels.length) + 1)
                            + this.batteryLevels.length) % this.batteryLevels.length];
            LogUtil.d(TAG, "dealBatteryMonitor currentIndex = " + this.currentIndex
                    + ", currentBatteryLevel = " + currentBattery
                    + ", lastBatteryLevel = " + lastBatteryLevel
                    + ", lastHourBatteryLevel = " + lastHourBatteryLevel
                    + ", batteryLevels = " + Arrays.toString(this.batteryLevels));
            if (this.extraModemLog != null) {
                computeToCheck(currentBattery, lastBatteryLevel, lastHourBatteryLevel);
            } else {
                LogUtil.e(TAG, "extraModemLog = null , pass !");
            }
        } else {
            LogUtil.d(TAG, "dealBatteryMonitor currentIndex = " + this.currentIndex
                    + "--batteryLevels = " + Arrays.toString(this.batteryLevels));
        }
        this.currentIndex = (this.currentIndex + 1) % this.batteryLevels.length;
        this.batteryLevels[this.currentIndex] = currentBattery;
    }

    /** 10 分钟掉电、或一小时掉电超过阈值，就判定耗电异常。 */
    private void computeToCheck(int currentBattery, int lastBatteryLevel, int lastHourBatteryLevel) {
        LogUtil.d(TAG, "computeToCheck , currentBattery = " + currentBattery
                + " , lastBattery = " + lastBatteryLevel
                + " , lastHourBattery = " + lastHourBatteryLevel);
        if (lastBatteryLevel - currentBattery >= this.extraModemLog.getPerTenMinutes()) {
            LogUtil.w(TAG, "battery use fast , 10分钟 耗电 >= "
                    + this.extraModemLog.getPerTenMinutes());
            whenBatteryCostComesException(this.strategy, this.extraModemLog.getFetchingDuration());
            return;
        }
        if (lastHourBatteryLevel == 0
                || lastHourBatteryLevel - currentBattery < this.extraModemLog.getPerOneHour()) {
            return;
        }
        LogUtil.w(TAG, "battery use fast , 一小时 耗电 >= "
                + this.extraModemLog.getPerOneHour());
        whenBatteryCostComesException(this.strategy, this.extraModemLog.getFetchingDuration());
    }

    /** 耗电异常：立刻收工并停止监控。 */
    private void whenBatteryCostComesException(LogStrategy strategy, int fetchingDuration) {
        ModemLogServeImpl.getInstance(this.context).startModemLog(strategy, fetchingDuration);
        cancelBatteryMonitor();
    }
}
