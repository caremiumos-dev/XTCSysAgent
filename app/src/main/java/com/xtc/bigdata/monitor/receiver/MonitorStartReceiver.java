package com.xtc.bigdata.monitor.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xtc.bigdata.common.utils.SystemInfoUtils;
import com.xtc.bigdata.monitor.anr.WatchDogs;
import com.xtc.log.LogUtil;

/**
 * 看门狗开关广播。
 *
 * <p>Manifest 里注册，action 固定为 {@code com.xtc.watchdogs}，
 * 优先级 100000（要比其它监听同一 action 的接收者先拿到）。</p>
 *
 * <p>三个 extra 决定行为：</p>
 * <ul>
 *   <li>{@code type}：0 表示停止监控，非 0 表示启动；</li>
 *   <li>{@code force}：非 0 时即使是 release 版也启动；</li>
 *   <li>{@code timeout}：自定义最小超时阈值，-1 表示用默认 4 秒。</li>
 * </ul>
 *
 * <p>{@link #onReceive} 里必须起线程：{@code start()} 会一直跑，
 * 在广播回调里同步执行会把广播超时。</p>
 */
public class MonitorStartReceiver extends BroadcastReceiver {

    private static final String TAG = "WatchDogs";

    /** 开关看门狗的广播 action。 */
    public static final String WATCHDOGS = "com.xtc.watchdogs";

    /** 原厂的调试线程（保留类形状，当前没有使用者）。 */
    static class CustomThread extends Thread {

        CustomThread() {
        }

        @Override
        public void run() {
            super.run();
            System.out.println("run:" + getName() + ":" + getId());
        }

        @Override
        public synchronized void start() {
            System.out.println("start:" + getName() + ":" + getId());
            super.start();
        }
    }

    /**
     * 处理广播。
     *
     * @param context 上下文
     * @param intent  广播
     */
    private void dealBroadcast(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.i(TAG, "action = " + action);
        if (action == null) {
            return;
        }
        if (WATCHDOGS.equals(action)) {
            dealWatchdogs(intent);
        } else {
            LogUtil.i(TAG, "do not care action = " + action);
        }
    }

    /**
     * 处理看门狗开关。
     *
     * @param intent 广播
     */
    private void dealWatchdogs(Intent intent) {
        if (intent.getIntExtra("type", 0) == 0) {
            WatchDogs.exit();
            return;
        }
        int forceStart = intent.getIntExtra("force", 0);
        LogUtil.d(TAG, "forceStart = " + forceStart);
        if (forceStart == 0 && !isSystemTypeAllow()) {
            LogUtil.d(TAG, "system type not allow start , return !");
            return;
        }
        int minTimeout = intent.getIntExtra("timeout", -1);
        LogUtil.d(TAG, "custom minTimeout = " + minTimeout);
        startWatchDogs(minTimeout);
    }

    /**
     * 是否是允许启动看门狗的系统类型（debug 版）。
     *
     * @return 是否允许
     */
    private boolean isSystemTypeAllow() {
        String buildType = SystemInfoUtils.getSystemProperty("ro.build.type");
        boolean isDebug = buildType != null && buildType.toLowerCase().contains("debug");
        LogUtil.d(TAG, "system type = " + buildType + " , isDebug = " + isDebug);
        return isDebug;
    }

    /**
     * 启动看门狗。
     *
     * @param minTimeout 最小超时阈值
     */
    private void startWatchDogs(int minTimeout) {
        WatchDogs.getInstance(minTimeout).start();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dealBroadcast(context, intent);
                } catch (Exception e) {
                    LogUtil.e(TAG, "error = " + e);
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
