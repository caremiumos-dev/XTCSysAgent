package com.xtc.bigdata.monitor.anr;

import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.xtc.bigdata.collector.CollectionManager;
import com.xtc.bigdata.collector.encapsulation.entity.event.ExceptionEvent;
import com.xtc.bigdata.collector.utils.DateFormatUtil;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.log.LogUtil;

import java.io.File;
import java.lang.reflect.Method;

/**
 * 主线程卡顿监控（基于 {@link LooperMonitor}）。
 *
 * <p>卡顿堆栈会①作为异常事件上报，②同时按天落盘到
 * {@code /sdcard/bigdata/anr/<yyyyMMdd>/<时间戳>.txt}，
 * 因为超长堆栈不适合塞进上报报文（超过 10KB 只留一句提示）。</p>
 *
 * <p>目录清理规则：单文件大于 10MB、或超过 7 天没动，就删掉。</p>
 *
 * @deprecated 原厂已标记废弃，替代者是 {@code WatchDogs}。
 */
@Deprecated
public class ANRMonitor {

    private static final String TAG = "ANRMonitor";

    /** 单个 ANR 文件大小上限：10MB。 */
    private static final long MAX_ANR_FILE_SIZE = 10485760;

    /** ANR 文件保留时长：7 天。 */
    private static final long ANR_FILE_KEEP_TIME = 604800000;

    /** 堆栈能进上报报文的字符上限：10KB。 */
    private static final int MAX_REPORT_STACK_CHARS = 10240;

    /** reason 字段的截断长度。 */
    private static final int MAX_REASON_CHARS = 1024;

    /** 反射出来的 {@code ProcessCpuTracker} 实例。 */
    private Object cpuTrackerObj;

    /** 反射出来的 {@code update()} 方法。 */
    private Method updateMethod;

    /** 删除过期的 ANR 文件（超大小或超时长）。 */
    private void clearExpireAnrFile() {
        File[] anrFiles;
        File anrDir = new File(Environment.getExternalStorageDirectory().getPath() + "/bigData/anr/");
        if (!anrDir.exists() || (anrFiles = anrDir.listFiles()) == null || anrFiles.length == 0) {
            return;
        }
        for (File anrFile : anrFiles) {
            if (anrFile != null) {
                if (anrFile.length() > MAX_ANR_FILE_SIZE) {
                    FileUtils.deleteDir(anrFile);
                    LogUtil.i(TAG, "delete anr file when file is larger than 10MB:" + anrFile);
                } else if (System.currentTimeMillis() - anrFile.lastModified() >= ANR_FILE_KEEP_TIME) {
                    FileUtils.deleteDir(anrFile);
                    LogUtil.i(TAG, "delete anr file:" + anrFile);
                }
            }
        }
    }

    /**
     * 上报一次卡顿。
     *
     * @param stackTrace 卡顿堆栈
     */
    private void collectANRException(String stackTrace) {
        if (TextUtils.isEmpty(stackTrace)) {
            return;
        }
        LogUtil.e(TAG, new ANRException(stackTrace));
        ExceptionEvent exceptionEvent = new ExceptionEvent();
        exceptionEvent.functionName = "BLOCK-ANR-ERROR-" + Constants.PACKAGE_NAME;
        exceptionEvent.reason = stackTrace.substring(0,
                stackTrace.length() <= MAX_REASON_CHARS ? stackTrace.length() : MAX_REASON_CHARS);
        saveStackInfoAsFile(stackTrace);
        if (stackTrace.length() <= MAX_REPORT_STACK_CHARS) {
            exceptionEvent.stack = stackTrace;
        } else {
            exceptionEvent.stack = "stack info is larger than 1024 * 10,save as file";
        }
        CollectionManager.getInstance().exceptionEvent(exceptionEvent);
    }

    /**
     * 卡顿回调：打日志 + 上报。
     *
     * @param blockTime 卡顿时长
     * @param stack     堆栈
     */
    private void dumpBlockInfo(long blockTime, String stack) {
        String cpuInfo = getCpuInfo();
        LogUtil.w(TAG, "blockTime:" + blockTime);
        LogUtil.w(TAG, "cpuInfo:" + cpuInfo);
        LogUtil.w(TAG, "stackTraceInfo:" + stack);
        collectANRException(stack);
    }

    /**
     * 反射取系统 CPU 使用明细。
     *
     * <p>{@code ProcessCpuTracker} 是隐藏类，只能反射；
     * 注意两次 {@code update()} 之间要等 500ms，否则算不出占用率。</p>
     *
     * @return CPU 明细文本；失败返回空串
     */
    private String getCpuInfo() {
        try {
            if (this.cpuTrackerObj == null) {
                this.cpuTrackerObj = Class.forName("com.android.internal.os.ProcessCpuTracker")
                        .getConstructor(Boolean.TYPE).newInstance(false);
                this.cpuTrackerObj.getClass().getMethod("init", new Class[0]).invoke(this.cpuTrackerObj, new Object[0]);
                this.updateMethod = this.cpuTrackerObj.getClass().getMethod("update", new Class[0]);
            } else {
                this.updateMethod.invoke(this.cpuTrackerObj, new Object[0]);
            }
            synchronized (ANRMonitor.class) {
                this.cpuTrackerObj.wait(500L);
            }
            this.updateMethod.invoke(this.cpuTrackerObj, new Object[0]);
            return (String) this.cpuTrackerObj.getClass().getMethod("printCurrentState", Long.TYPE)
                    .invoke(this.cpuTrackerObj, Long.valueOf(SystemClock.uptimeMillis()));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return "";
        }
    }

    /**
     * 把堆栈按天落盘。
     *
     * @param stackTrace 堆栈
     */
    private void saveStackInfoAsFile(String stackTrace) {
        clearExpireAnrFile();
        FileUtils.saveFile(Environment.getExternalStorageDirectory().getPath() + "/bigdata/anr/"
                + DateFormatUtil.format("yyyyMMdd", System.currentTimeMillis()) + "/",
                stackTrace, System.currentTimeMillis() + ".txt");
    }

    /**
     * 开始监控指定 Looper。
     *
     * @param looper         目标 Looper
     * @param ignoreDebugger 是否忽略调试器
     * @param timeout        卡顿阈值毫秒
     */
    public void startMonitor(Looper looper, boolean ignoreDebugger, long timeout) {
        looper.setMessageLogging(new LooperMonitor(looper, ignoreDebugger, timeout, new LooperMonitor.BlockListener() {
            @Override
            public void onBlock(long blockTime, String stack) {
                dumpBlockInfo(blockTime, stack);
            }
        }));
        LogUtil.d(TAG, "start monitor thread:" + looper);
    }

    /** 开始监控主线程（阈值 5 秒）。 */
    public void startMonitorMainThread() {
        startMonitor(Looper.getMainLooper(), true, 5000L);
    }
}
