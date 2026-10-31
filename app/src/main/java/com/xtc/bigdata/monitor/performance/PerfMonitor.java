package com.xtc.bigdata.monitor.performance;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;

import com.xtc.bigdata.collector.CollectionManager;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomEvent;
import com.xtc.bigdata.common.utils.ResourceUsedUtil;
import com.xtc.log.LogUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * 应用自身性能采集（CPU / PSS / USS / 采集耗时）。
 *
 * <p>通过反射 {@code com.android.internal.os.ProcessCpuTracker} 拿到
 * 整机与进程的 jiffies，两次采样之间固定等 500ms，
 * 再用"本进程 jiffies 增量 / 整机 jiffies 增量"算出 CPU 占用率。</p>
 *
 * <p>结果作为 {@code CustomEvent}（functionName = {@code "PerfMonitor"}）
 * 上报，extend 里带场景（normal/screenOn/screenOff）与采集耗时段，
 * 服务端据此判断"采集本身是否拖慢了应用"。</p>
 *
 * <p>所有异常都在 {@link #collectInfo(String)} 里被吞掉并打日志 ——
 * 性能采集失败绝不能影响业务。</p>
 */
public class PerfMonitor {

    /** 场景：常规轮询。 */
    private static final String SCENE_NORMAL = "normal";

    /** 场景：息屏瞬间。 */
    private static final String SCENE_SCREEN_OFF = "screenOff";

    /** 场景：亮屏瞬间。 */
    private static final String SCENE_SCREEN_ON = "screenOn";

    private static final String TAG = "PerfMonitor";

    /** 应用上下文。 */
    private Context applicationContext;

    /** 采集线程的 Handler。 */
    private Handler handler;

    /** 采集线程。 */
    private HandlerThread handlerThread;

    /** 上次采集开始时的 uptime。 */
    private long lastCollectBeginTimeE;

    /** 本进程号。 */
    private int myPid;

    /** 反射出来的 {@code ProcessCpuTracker} 实例。 */
    private Object processCpuTracker;

    /** 单例。 */
    private static PerfMonitor monitor = null;

    /** 采集间隔（毫秒），volatile 因为可以从任意线程改。 */
    private static volatile int collectInterval = 20000;

    /** 轮询是否已启动。 */
    private volatile boolean looperStarted = false;

    /** 常规轮询任务：到点就采一次，然后继续排下一次。 */
    private Runnable normalMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (SystemClock.elapsedRealtime() - PerfMonitor.this.lastCollectBeginTimeE >= PerfMonitor.collectInterval) {
                PerfMonitor.this.collectInfo(PerfMonitor.SCENE_NORMAL);
            }
            if (PerfMonitor.this.handler == null || !PerfMonitor.this.looperStarted) {
                return;
            }
            PerfMonitor.this.handler.postDelayed(PerfMonitor.this.normalMonitorRunnable, PerfMonitor.collectInterval);
        }
    };

    /** 亮屏采集任务。 */
    private Runnable screenOnRunnable = new Runnable() {
        @Override
        public void run() {
            PerfMonitor.this.collectInfo(PerfMonitor.SCENE_SCREEN_ON);
        }
    };

    /** 息屏采集任务。 */
    private Runnable screenOffRunnable = new Runnable() {
        @Override
        public void run() {
            PerfMonitor.this.collectInfo(PerfMonitor.SCENE_SCREEN_OFF);
        }
    };

    private PerfMonitor() {
    }

    /**
     * 采集一次性能数据并上报。
     *
     * @param scene 场景名
     */
    private void collectInfo(String scene) {
        this.lastCollectBeginTimeE = SystemClock.elapsedRealtime();
        long collectBeginTime = System.currentTimeMillis();
        try {
            Class<?> cpuTrackerClass = Class.forName("com.android.internal.os.ProcessCpuTracker");
            Constructor<?> constructor = cpuTrackerClass.getConstructor(Boolean.TYPE);
            constructor.setAccessible(true);
            Method initMethod = cpuTrackerClass.getDeclaredMethod("init", new Class[0]);
            initMethod.setAccessible(true);
            Method updateMethod = cpuTrackerClass.getDeclaredMethod("update", new Class[0]);
            updateMethod.setAccessible(true);
            Method cpuTimeForPidMethod = cpuTrackerClass.getDeclaredMethod("getCpuTimeForPid", Integer.TYPE);
            cpuTimeForPidMethod.setAccessible(true);
            Method lastUserTimeMethod = cpuTrackerClass.getDeclaredMethod("getLastUserTime", new Class[0]);
            lastUserTimeMethod.setAccessible(true);
            Method lastSystemTimeMethod = cpuTrackerClass.getDeclaredMethod("getLastSystemTime", new Class[0]);
            lastSystemTimeMethod.setAccessible(true);
            Method lastIoWaitTimeMethod = cpuTrackerClass.getDeclaredMethod("getLastIoWaitTime", new Class[0]);
            lastIoWaitTimeMethod.setAccessible(true);
            Method lastIrqTimeMethod = cpuTrackerClass.getDeclaredMethod("getLastIrqTime", new Class[0]);
            lastIrqTimeMethod.setAccessible(true);
            Method lastSoftIrqTimeMethod = cpuTrackerClass.getDeclaredMethod("getLastSoftIrqTime", new Class[0]);
            lastSoftIrqTimeMethod.setAccessible(true);
            Method lastIdleTimeMethod = cpuTrackerClass.getDeclaredMethod("getLastIdleTime", new Class[0]);
            lastIdleTimeMethod.setAccessible(true);
            Field lastSampleWallTimeField = cpuTrackerClass.getDeclaredField("mLastSampleWallTime");
            lastSampleWallTimeField.setAccessible(true);
            Field currentSampleWallTimeField = cpuTrackerClass.getDeclaredField("mCurrentSampleWallTime");
            currentSampleWallTimeField.setAccessible(true);
            if (this.processCpuTracker == null) {
                this.processCpuTracker = constructor.newInstance(Boolean.TRUE);
            }
            initMethod.invoke(this.processCpuTracker, new Object[0]);
            updateMethod.invoke(this.processCpuTracker, new Object[0]);
            long startPidCpuTime = ((Long) cpuTimeForPidMethod.invoke(this.processCpuTracker,
                    Integer.valueOf(this.myPid))).longValue();
            synchronized (PerfMonitor.class) {
                this.processCpuTracker.wait(500L);
            }
            updateMethod.invoke(this.processCpuTracker, new Object[0]);
            long startSampleWallTime = ((Long) lastSampleWallTimeField.get(this.processCpuTracker)).longValue();
            long endSampleWallTime = ((Long) currentSampleWallTimeField.get(this.processCpuTracker)).longValue();
            long endPidCpuTime = ((Long) cpuTimeForPidMethod.invoke(this.processCpuTracker,
                    Integer.valueOf(this.myPid))).longValue();
            int userTime = ((Integer) lastUserTimeMethod.invoke(this.processCpuTracker, new Object[0])).intValue();
            int systemTime = ((Integer) lastSystemTimeMethod.invoke(this.processCpuTracker, new Object[0])).intValue();
            int idleTime = ((Integer) lastIdleTimeMethod.invoke(this.processCpuTracker, new Object[0])).intValue();
            long totalCpuTime = ((Integer) lastSoftIrqTimeMethod.invoke(this.processCpuTracker, new Object[0])).intValue()
                    + userTime + systemTime + idleTime
                    + ((Integer) lastIoWaitTimeMethod.invoke(this.processCpuTracker, new Object[0])).intValue()
                    + ((Integer) lastIrqTimeMethod.invoke(this.processCpuTracker, new Object[0])).intValue();
            float totalCpuUsed = save2BitDecimal(((totalCpuTime - ((long) idleTime)) * 100.0f) / totalCpuTime);
            float totalProcUsed = save2BitDecimal(((endPidCpuTime - startPidCpuTime) * 100.0f) / totalCpuTime);
            long memoryCollectBeginTime = System.currentTimeMillis();
            int[] memoryUsed = ResourceUsedUtil.getMemoryUsedOfProc(this.myPid, this.applicationContext);
            float nowPss = save2BitDecimal((memoryUsed[0] * 1.0f) / 1024.0f);
            float nowUss = save2BitDecimal((memoryUsed[1] * 1.0f) / 1024.0f);
            long memCollectUsedTime = System.currentTimeMillis() - memoryCollectBeginTime;
            long totalCollectUsedTime = System.currentTimeMillis() - collectBeginTime;
            CustomEvent customEvent = new CustomEvent();
            HashMap<String, String> extend = new HashMap<String, String>();
            customEvent.functionName = TAG;
            extend.put("hasBootedTime", this.lastCollectBeginTimeE + "");
            extend.put("scene", scene);
            extend.put("collectInterval", collectInterval + "");
            extend.put("myPid", this.myPid + "");
            extend.put("startCollectTime", startSampleWallTime + "");
            extend.put("endCollectTime", endSampleWallTime + "");
            extend.put("totalCpuTime", totalCpuTime + "");
            extend.put("totalCpuUsed", totalCpuUsed + "");
            extend.put("totalProcUsed", totalProcUsed + "");
            extend.put("nowPss", nowPss + "");
            extend.put("nowUss", nowUss + "");
            extend.put("memCollectUsedTime", memCollectUsedTime + "");
            extend.put("totalCollectUsedTime", totalCollectUsedTime + "");
            customEvent.setExtend(extend);
            CollectionManager.getInstance().customEvent(customEvent);
        } catch (Exception e) {
            LogUtil.e(TAG, "e = " + e);
            e.printStackTrace();
        }
    }

    /** 息屏时采一次（没启动就只打日志）。 */
    public static synchronized void collectScreenOff() {
        if (monitor == null) {
            LogUtil.i(TAG, "monitor = null , just return !");
        } else if (monitor.handler != null) {
            monitor.handler.post(monitor.screenOffRunnable);
        }
    }

    /** 亮屏时采一次。 */
    public static synchronized void collectScreenOn() {
        if (monitor == null) {
            LogUtil.i(TAG, "monitor = null , just return !");
        } else if (monitor.handler != null) {
            monitor.handler.post(monitor.screenOnRunnable);
        }
    }

    /**
     * 取单例。
     *
     * @return 单例
     */
    public static synchronized PerfMonitor getInstance() {
        if (monitor == null) {
            monitor = new PerfMonitor();
        }
        LogUtil.d(TAG, "monitor = " + monitor);
        return monitor;
    }

    /** 退出并释放线程。 */
    public static void quit() {
        if (monitor == null) {
            LogUtil.i(TAG, "monitor = null , just return !");
            return;
        }
        monitor.stopInner();
        if (monitor.handler != null) {
            monitor.handler.removeCallbacksAndMessages(null);
        }
        if (monitor.handlerThread != null) {
            monitor.handlerThread.quit();
        }
        monitor.handlerThread = null;
        monitor.handler = null;
        monitor = null;
        LogUtil.d(TAG, "monitor quitted ---");
    }

    /**
     * 真正启动轮询（首次延迟 2 秒）。
     *
     * @param context 上下文
     */
    private void realStart(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.handler.postDelayed(this.normalMonitorRunnable, 2000L);
        this.looperStarted = true;
        LogUtil.d(TAG, "PerfMonitor looperStarted ---");
    }

    /**
     * 保留两位小数，负数归零。
     *
     * @param value 原值
     * @return 两位小数的值
     */
    private float save2BitDecimal(float value) {
        float result = value;
        if (result <= 0.0f) {
            result = 0.0f;
        }
        return Math.round(result * 100.0f) / 100.0f;
    }

    /**
     * 设置采集间隔。
     *
     * @param interval 毫秒
     */
    public static void setCollectInterval(int interval) {
        collectInterval = interval;
    }

    /**
     * 解析 {@code ProcessCpuTracker.printCurrentState} 的输出。
     *
     * <p>只是打日志用（原厂没有把解析结果上报），当前没有调用点。</p>
     *
     * @param cpuState CPU 状态文本
     */
    private void splitStringToGetData(String cpuState) {
        boolean pidFound = false;
        for (String line : cpuState.split("\n")) {
            if (line.contains("%")) {
                if (!pidFound && line.contains("/")) {
                    if (this.myPid == Integer.parseInt(line.substring(line.indexOf("%") + 1, line.indexOf("/")).trim())) {
                        pidFound = true;
                        LogUtil.d(TAG, "array = " + line + " , procs = "
                                + line.substring(0, line.indexOf("%") + 1).trim());
                    }
                }
                if (line.contains("TOTAL")) {
                    LogUtil.d(TAG, "totals = " + line.substring(0, line.indexOf("TOTAL") - 1).trim());
                }
            }
        }
    }

    /** 停掉轮询（可重复调用）。 */
    private synchronized void stopInner() {
        if (this.looperStarted) {
            if (this.handler != null) {
                this.handler.removeCallbacks(this.normalMonitorRunnable);
            }
            this.looperStarted = false;
            LogUtil.d(TAG, "monitor stopped ---");
        }
    }

    /** 停止轮询。 */
    public static void stopLooper() {
        if (monitor != null) {
            monitor.stopInner();
        } else {
            LogUtil.i(TAG, "monitor = null , just return !");
        }
    }

    /**
     * 启动轮询（重复调用无副作用）。
     *
     * @param context 上下文
     */
    public synchronized void startLooper(Context context) {
        if (context == null) {
            LogUtil.w(TAG, "ctx != null , return !");
        } else if (!this.looperStarted) {
            this.myPid = Process.myPid();
            if (this.handler == null) {
                this.handlerThread = new HandlerThread(TAG);
                this.handlerThread.start();
                this.handler = new Handler(this.handlerThread.getLooper());
            }
            stopInner();
            realStart(context);
        }
    }
}
