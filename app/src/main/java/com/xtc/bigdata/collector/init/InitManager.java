package com.xtc.bigdata.collector.init;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Process;

import com.xtc.bigdata.collector.AppLifeCycleCallback;
import com.xtc.bigdata.collector.BehaviorCollector;
import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.encapsulation.entity.BaseAttr;
import com.xtc.bigdata.collector.exception.CrashHandler;
import com.xtc.bigdata.collector.utils.QueueUtils;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.bigdata.common.utils.ProcessUtils;
import com.xtc.log.LogUtil;

/**
 * 采集库的初始化编排。
 *
 * <p>整个初始化被丢到后台线程（{@link QueueUtils} 的 {@code bigdata_task}），
 * 因为它要读 {@code /proc}、反射系统属性、读 SP —— 都是毫秒级 IO，
 * 放在主线程会拖慢应用启动。</p>
 *
 * <p>初始化失败的处理很激进：先交给崩溃上报，睡 2 秒（等上报线程写完盘），
 * 然后<b>直接杀掉当前进程</b>让系统重启它。这是原厂行为，不要改成
 * "吞掉异常继续跑" —— 半初始化的采集器比没有采集器更糟。</p>
 */
public class InitManager {

    private static final String TAG = "InitManager";

    private InitManager() {
    }

    /** 启动初始化（后台线程执行）。 */
    @TargetApi(14)
    public static void init() {
        if (ContextUtils.isEmpty()) {
            LogUtil.e(TAG, "error , context = null !");
            return;
        }
        QueueUtils.getInstance().post(new Runnable() {
            @Override
            public void run() {
                try {
                    new BaseAttr().genBaseAttr();
                    initOthers();
                    LogUtil.i(TAG, "bigdata init completed !");
                } catch (Exception e) {
                    LogUtil.e(TAG, "bigdata init error !!!");
                    CrashHandler.getInstance().handleExec(e);
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException interruptedException) {
                        // 原厂忽略中断，仍然继续杀进程
                    } finally {
                        LogUtil.w(TAG, "kill current process to restart !");
                        Process.killProcess(Process.myPid());
                    }
                }
            }
        });
    }

    /**
     * 初始化除基信息之外的模块：生命周期回调、崩溃处理、启动事件。
     */
    private static void initOthers() {
        LogUtil.i(TAG, "initOthers begin");
        FileUtils.ensureSdCardPath();
        if (Build.VERSION.SDK_INT >= 14) {
            ContextUtils.getContext().registerActivityLifecycleCallbacks(new AppLifeCycleCallback());
        }
        if (ConfigAgent.getBehaviorConfig().crashUsable) {
            CrashHandler.getInstance().registerCrashHandler();
        }
        String curProcessName = ProcessUtils.getCurProcessName(ContextUtils.getContext());
        LogUtil.d(TAG, "curProcessName = " + curProcessName + " , HOST_APP_ID = " + Constants.HOST_APP_ID
                + ", PACKAGE_NAME: " + Constants.PACKAGE_NAME);
        if (Constants.PACKAGE_NAME.equals(curProcessName)) {
            BehaviorCollector.getInstance().appLaunch();
        }
        if (Constants.deviceType.equals(Constants.PHONE)) {
            ShareHelper.getInstance().appLaunchNotify();
        }
    }
}
