package com.xtc.systemagent.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.R;
import com.xtc.systemagent.module.log.serve.impl.AdspDumpServeImpl;
import com.xtc.utils.system.SystemPropertiesUtils;

/**
 * ADSP dump 采集结束的接收器。
 *
 * <p>采集完成后如果"自检开关"打开（{@code persist.vendor.ssr.enable_ramdumps}），
 * 就亮屏 + 震动 2 秒 + 弹一条通知提醒用户；最后把通知结果交给采集服务上报。
 * 接收器是短生命周期的，所以 WakeLock 在 {@code onReceive} 末尾一定要释放。</p>
 */
public class AdspDumpReceiver extends BroadcastReceiver {

    private static final String TAG = "Log_AdspDumpReceiver";

    /** ADSP dump 采集结束。 */
    private static final String ACTION_ADSP_DUMP_COLLECT_END = "com.xtc.adsp.dump.end";

    /** 自检里控制 ADSP dump 的开关属性。 */
    private static final String PROP_ADSP_DUMP_SWITCH = "persist.vendor.ssr.enable_ramdumps";

    /** 亮屏 WakeLock 的持有时间：3 秒。 */
    private static final long WAKE_LOCK_TIMEOUT = 3000L;

    /** 震动时长：2 秒。 */
    private static final long VIBRATE_TIME = 2000L;

    /** 通知 id。 */
    private static final int NOTIFICATION_ID = 1;

    /**
     * WakeLock 类型：{@code SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP}。
     *
     * <p>原厂直接写了字面量 268435466（0x1000000A），这里保留同样的位组合。</p>
     */
    private static final int WAKE_LOCK_FLAGS =
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;

    private PowerManager mPowerManager;
    private Vibrator mVibrator;
    private PowerManager.WakeLock mWakeLock = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.d(TAG, "action = " + action);
        if (TextUtils.isEmpty(action)) {
            return;
        }
        if (ACTION_ADSP_DUMP_COLLECT_END.equals(action)) {
            notifyUser(context);
            AdspDumpServeImpl.getInstance(context).receiveAdspDumpCollected();
        }
        release();
    }

    /** 自检开关开着才提醒用户。 */
    private void notifyUser(Context context) {
        int adspDumpSwitch = getAdspDumpSwitch();
        LogUtil.d(TAG, "notifyUser adspDumpSwitch prop = " + adspDumpSwitch);
        if (adspDumpSwitch != 1) {
            LogUtil.d(TAG, "notifyUser but adsp dump switch by self-test not opened ! ");
            return;
        }
        wakeUpScreen(context);
        vibrate(context);
        showNotification(context);
    }

    private int getAdspDumpSwitch() {
        return SystemPropertiesUtils.getInt(PROP_ADSP_DUMP_SWITCH, 0);
    }

    /** 先确保拿到 WakeLock，再把屏幕点亮（已经亮着就不重复申请）。 */
    private void wakeUpScreen(Context context) {
        if (this.mWakeLock == null) {
            this.mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = this.mPowerManager.newWakeLock(WAKE_LOCK_FLAGS, TAG);
        }
        if (isScreenInteractive()) {
            return;
        }
        acquireWakeUp();
    }

    private boolean isScreenInteractive() {
        return this.mPowerManager != null && this.mPowerManager.isInteractive();
    }

    private void acquireWakeUp() {
        LogUtil.d(TAG, "acquireWakeLock mWakeLock: " + this.mWakeLock);
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        }
    }

    private void vibrate(Context context) {
        if (this.mVibrator == null) {
            this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (this.mVibrator != null) {
            this.mVibrator.vibrate(VIBRATE_TIME);
        }
    }

    private void showNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        String title = context.getResources().getString(R.string.notification_adsp_dump_title);
        String content = context.getResources().getString(R.string.notification_adsp_dump_content);
        builder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_push_official_news_small)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_push_official_news_large))
                .setAutoCancel(true);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void release() {
        releaseWakeLock();
        this.mWakeLock = null;
        this.mPowerManager = null;
        this.mVibrator = null;
    }

    private void releaseWakeLock() {
        if (this.mWakeLock == null || !this.mWakeLock.isHeld()) {
            return;
        }
        this.mWakeLock.release();
    }
}
