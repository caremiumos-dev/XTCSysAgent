package com.xtc.common.bigdata;

import android.content.Context;
import com.xtc.bigdata.collector.BehaviorCollector;
import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.collector.config.BehaviorConfig;
import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import com.xtc.bigdata.collector.encapsulation.entity.event.CustomEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.SystemInfoEvent;
import com.xtc.bigdata.collector.utils.MainHandlerUtil;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.utils.SharedPrefUtils;
import com.xtc.bigdata.monitor.crash.DropboxScanner;
import com.xtc.bigdata.monitor.performance.PerfMonitor;
import com.xtc.httplib.ConfigOptions;
import com.xtc.log.LogUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 埋点门面。
 *
 * <p>所有业务代码只跟这个类打交道，具体走哪条通道由 {@code behaviorMode} 决定：</p>
 * <ul>
 *   <li>{@link #BEHAVIOR_MODE_UMENG}(0) —— 关闭上报，只把配置标记为不可用；</li>
 *   <li>{@link #BEHAVIOR_MODE_BBK}(1) —— 步步高自有通道（默认）；</li>
 *   <li>{@link #BEHAVIOR_MODE_ALL}(2) —— 全通道。</li>
 * </ul>
 *
 * <p>没有 {@link #init} 之前所有上报都只打一条错误日志然后丢弃，
 * 避免埋点 SDK 未就绪时崩溃。</p>
 */
public class BehaviorUtil {

    /** 友盟通道。 */
    public static final int BEHAVIOR_MODE_UMENG = 0;
    /** 步步高自有通道。 */
    public static final int BEHAVIOR_MODE_BBK = 1;
    /** 全通道。 */
    public static final int BEHAVIOR_MODE_ALL = 2;

    private static final String TAG = "BehaviorUtil";

    /** 是否已初始化。 */
    private static boolean isInit = false;

    private static int behaviorMode;

    private static Behavior bbkBehavior;

    private static DropboxScanner dropboxScanner;

    private BehaviorUtil() {
    }

    /** 初始化埋点 SDK，默认走步步高通道。 */
    public static void init(BehaviorConfig behaviorConfig) {
        isInit = true;
        BehaviorCollector.getInstance().init(behaviorConfig);
        setBehaviorMode(BEHAVIOR_MODE_BBK);
    }

    public static void setBehaviorMode(int mode) {
        behaviorMode = mode;
        switch (mode) {
            case BEHAVIOR_MODE_UMENG:
                ConfigAgent.getBehaviorConfig().usable = false;
                break;
            case BEHAVIOR_MODE_BBK:
                ConfigAgent.getBehaviorConfig().usable = true;
                getBbkBehavior();
                break;
            case BEHAVIOR_MODE_ALL:
                ConfigAgent.getBehaviorConfig().usable = true;
                getBbkBehavior();
                break;
            default:
                break;
        }
    }

    /** 懒加载步步高通道实现。 */
    private static Behavior getBbkBehavior() {
        if (bbkBehavior == null) {
            bbkBehavior = new BbkBehavior();
        }
        return bbkBehavior;
    }

    // ---------------------------------------------------------------- 点击

    public static void clickEvent(Context context, String functionName) {
        if (isInit) {
            clickEvent(context, functionName, null, null);
        } else {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        }
    }

    public static void clickEvent(Context context, String functionName, String moduleDetail,
                                  HashMap<String, String> extend) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().clickEvent(context, functionName, moduleDetail, extend);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().clickEvent(context, functionName, moduleDetail, extend);
        }
    }

    // ---------------------------------------------------------------- 计数

    public static void countEvent(Context context, String functionName, String trigValue) {
        if (isInit) {
            countEvent(context, functionName, null, trigValue, null);
        } else {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        }
    }

    public static void countEvent(Context context, String functionName, String moduleDetail,
                                  String trigValue, HashMap<String, String> extend) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().countEvent(context, functionName, moduleDetail, trigValue, extend);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().countEvent(context, functionName, moduleDetail, trigValue, extend);
        }
    }

    // ---------------------------------------------------------------- 自定义

    public static void customEvent(Context context, String functionName, HashMap<String, String> extend) {
        if (isInit) {
            customEvent(context, functionName, null, null, extend);
        } else {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        }
    }

    public static void customEvent(Context context, String functionName, String moduleDetail,
                                   String trigValue, HashMap<String, String> extend) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().customEvent(context, functionName, moduleDetail, trigValue, extend);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().customEvent(context, functionName, moduleDetail, trigValue, extend);
        }
    }

    /**
     * 实时事件：绕过 SDK 的批量队列，直接落库。
     *
     * <p>用单线程池写，保证不阻塞调用方线程，也保证写入顺序。</p>
     */
    public static void realTimeEvent(Context context, String functionName, String moduleDetail,
                                     String trigValue, HashMap<String, String> extend) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
            return;
        }
        if (BEHAVIOR_MODE_BBK == behaviorMode) {
            insertRealTimeEvent(context, functionName, moduleDetail, trigValue, extend);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            insertRealTimeEvent(context, functionName, moduleDetail, trigValue, extend);
        }
    }

    private static void insertRealTimeEvent(Context context, String functionName,
                                            String moduleDetail, String trigValue,
                                            HashMap<String, String> extend) {
        final CustomEvent event = new CustomEvent();
        event.functionName = functionName;
        event.moduleDetail = moduleDetail;
        event.trigValue = trigValue;
        event.activity = context.getClass().getName();
        event.setExtend(extend);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                event.makeData();
                ShareHelper.getInstance().insert(event.getContentValues());
            }
        });
    }

    // ---------------------------------------------------------------- 搜索

    public static void searchEvent(Context context, String functionName, String keyword,
                                   String resultCount, String searchType) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().searchEvent(context, functionName, keyword, resultCount, searchType);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().searchEvent(context, functionName, keyword, resultCount, searchType);
        }
    }

    // ---------------------------------------------------------------- 页面

    public static void onPageStart(String pageName) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().onPageStart(pageName);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().onPageStart(pageName);
        }
    }

    public static void onPageEnd(String pageName) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().onPageEnd(pageName, null, null, null);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().onPageEnd(pageName, null, null, null);
        }
    }

    public static void pageEnd(String pageName, String fromPage, String toPage, Map extend) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().onPageEnd(pageName, fromPage, toPage, extend);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().onPageEnd(pageName, fromPage, toPage, extend);
        }
    }

    public static void onResume(Context context) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().onResume(context);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().onResume(context);
        }
    }

    public static void onPause(Context context) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().onPause(context);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().onPause(context);
        }
    }

    // ---------------------------------------------------------------- 用户

    public static void initUserInfo(String userId) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (Constants.deviceType.equals("watch")) {
            UserAttr userAttr = new UserAttr();
            userAttr.setUserId(userId);
            BehaviorCollector.getInstance().initUserInfo(userAttr);
        }
    }

    public static void userSingIn(Context context, UserAttr userAttr) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().userSingIn(context, userAttr);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().userSingIn(context, userAttr);
        }
    }

    public static void userSingOut(Context context) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            getBbkBehavior().userSingOut(context);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            getBbkBehavior().userSingOut(context);
        }
    }

    // ---------------------------------------------------------------- 配置

    public static void setDebug(boolean debug) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            Constants.setDebug(debug);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            Constants.setDebug(debug);
        }
    }

    public static void setSessionTimeout(long timeout) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            ConfigAgent.getBehaviorConfig().sessionTimeout = timeout;
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            ConfigAgent.getBehaviorConfig().sessionTimeout = timeout;
        }
    }

    /** 渠道号只在手表上才有意义。 */
    public static void setChannelId(String channelId, String channelValue) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (BEHAVIOR_MODE_BBK == behaviorMode) {
            SharedPrefUtils.getInstance().saveKeyStringValue(DeviceInfo.PHONE_CHANNEL_ID, channelValue);
        } else if (BEHAVIOR_MODE_ALL == behaviorMode) {
            SharedPrefUtils.getInstance().saveKeyStringValue(DeviceInfo.PHONE_CHANNEL_ID, channelValue);
        }
    }

    public static void setSystemInfo(String functionName, Map<String, String> extend) {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
            return;
        }
        SystemInfoEvent event = new SystemInfoEvent();
        event.functionName = functionName;
        event.setExtend(extend);
        BehaviorCollector.getInstance().systemInfoEvent(event);
    }

    public static void setWatchConfig(String config) {
        if (isInit) {
            BehaviorCollector.getInstance().saveWatchConfig(config);
        } else {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        }
    }

    public static void loadFilterFunctions() {
        if (isInit) {
            BehaviorCollector.getInstance().loadFilterFunctions();
        } else {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        }
    }

    public static void saveFilterFunctions(String functionNames) {
        if (isInit) {
            BehaviorCollector.getInstance().saveFilterFunctionName(functionNames);
        } else {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        }
    }

    // ---------------------------------------------------------------- 监控

    public static void startPerfMonitorLooper(Context context) {
        PerfMonitor.getInstance().startLooper(context);
    }

    public static void stopPerfMonitorLooper() {
        PerfMonitor.stopLooper();
    }

    public static void changePerfMonitorInterval(int interval) {
        PerfMonitor.setCollectInterval(interval);
    }

    public static void quitPerfMonitor() {
        PerfMonitor.quit();
    }

    public static void collectScreenOnInfo() {
        PerfMonitor.collectScreenOn();
    }

    public static void collectScreenOffInfo() {
        PerfMonitor.collectScreenOff();
    }

    /** 开始跟踪 dropbox 崩溃日志，延迟一个"频繁请求统计窗口"再启动。 */
    public static void followDropbox(final Context context) {
        if (dropboxScanner == null) {
            dropboxScanner = new DropboxScanner();
        }
        MainHandlerUtil.postDelay(new Runnable() {
            @Override
            public void run() {
                BehaviorUtil.dropboxScanner.start(context);
            }
        }, ConfigOptions.MonitorCondition.FREQUENT_REQUEST_PERIOD);
    }

    public static void unfollowDropbox(Context context) {
        if (dropboxScanner == null) {
            return;
        }
        dropboxScanner.stop(context);
    }

    public static void uplaodSystemCrashInfo() {
        if (!isInit) {
            LogUtil.e(TAG, "BehaviorUtil don't have init.");
        } else if (ConfigAgent.getBehaviorConfig().isUploadSysLog) {
            BehaviorCollector.getInstance().uploadSystemCrash();
        }
    }
}
