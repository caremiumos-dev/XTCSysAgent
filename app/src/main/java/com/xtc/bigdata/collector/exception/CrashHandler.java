package com.xtc.bigdata.collector.exception;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.collector.config.ConfigAgent;
import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.collector.encapsulation.entity.event.ExceptionEvent;
import com.xtc.bigdata.collector.utils.MD5Coder;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.constants.EType;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.bigdata.common.utils.SharedPrefUtils;
import com.xtc.bigdata.common.utils.StoreUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

/**
 * 全局崩溃捕获与落库。
 *
 * <p>它在两处被用到：{@code registerCrashHandler()} 把自己装成
 * {@link Thread.UncaughtExceptionHandler}；以及宿主自己捕获到崩溃时
 * 通过 {@code BehaviorCollector.onCrash} 直接调 {@link #handleExec(Throwable)}。</p>
 *
 * <p>去重是核心：同一个堆栈在 5 秒内连续崩（例如主线程死循环、
 * 或者被系统反复拉起），只上报第一次，否则服务端会被刷爆。
 * 指纹存在 SharedPreferences 里，所以跨进程重启也有效。</p>
 *
 * <p>崩溃采集一律在 {@code Crash_Handler} 后台线程执行（优先级 10）：
 * 崩溃路径上主线程随时可能被杀，只有后台线程才有机会把数据写完。</p>
 */
public final class CrashHandler implements Thread.UncaughtExceptionHandler {

    /** 崩溃报告文件后缀。 */
    private static final String CRASH_REPORTER_EXTENSION = ".cr";

    /** 崩溃日志目录默认上限：1MB。 */
    private static final long DEFAULT_CRASH_CACHE_SIZE = 1048576;

    /** 连续重启次数上限。 */
    private static final int RESTART_COUNT = 5;

    /** 堆栈在文件里的分隔标记。 */
    private static final String STACK_TRACE = "STACK_TRACE";

    /** 单例。 */
    private static CrashHandler mInstance;

    /** 崩溃处理专用线程的 Handler。 */
    private Handler crashHandler;

    /** 系统原来的默认 handler，处理完要还给它。 */
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static final String TAG = CrashHandler.class.getSimpleName();

    /** 调试开关在类加载时取一次快照（原厂如此）。 */
    private static final boolean DEBUG = Constants.isDebug;

    /** SD 卡根路径。 */
    private static final String SDCARD_PATH = "/mnt/sdcard";

    /** 默认崩溃日志目录。 */
    private static final String DEFAULT_CRASH_LOG_PATH = SDCARD_PATH + File.separator + ".crash" + File.separator;

    /** 当前日志目录。 */
    private String mLogPath = DEFAULT_CRASH_LOG_PATH;

    /** 当前缓存上限。 */
    private long mCrashCacheSize = DEFAULT_CRASH_CACHE_SIZE;

    /** 是否需要 UI 上报（原厂字段，当前没有读取点）。 */
    private boolean needUIReport = true;

    /** 崩溃时是否 Toast。 */
    private boolean needToast = false;

    /** SP 键：首次崩溃时间。 */
    private final String FIRST_CRASH_TIME = "first_crash_time";

    /** SP 键：最近一次崩溃时间。 */
    private final String LAST_CRASH_TIME = "last_crash_time";

    /** SP 键：崩溃次数。 */
    private final String CRASH_COUNT = "crash_count";

    /** 版本号（原厂字段，当前没有读写点）。 */
    private int mVersionCode = 0;

    /** 崩溃去重的临界区锁。 */
    private final Object lock = new Object();

    private CrashHandler() {
        HandlerThread handlerThread = new HandlerThread("Crash_Handler", 10);
        handlerThread.start();
        this.crashHandler = new Handler(handlerThread.getLooper());
    }

    /** 崩溃日志目录超过上限就整个删掉。 */
    private void checkCacheSize() {
        String logPath = resolveLogPath();
        if (logPath != null && StoreUtils.getFileSize(logPath) >= this.mCrashCacheSize) {
            LogUtil.d(TAG, "the cache size is rearch max size, we will clear it, clear: " + StoreUtils.deleteFolder(logPath));
        }
    }

    /**
     * 采集并落库一次崩溃（去重后的真正执行体）。
     *
     * @param throwable 崩溃
     */
    private void collectCrash(Throwable throwable) {
        String stackText;
        boolean shouldSave = true;
        String errorMd5 = "";
        synchronized (this.lock) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            // cause 链要一起打，否则只剩一层 "Caused by"
            for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause()) {
                cause.printStackTrace(printWriter);
            }
            stackText = stringWriter.toString();
            printWriter.close();
            try {
                errorMd5 = new MD5Coder().encode(stackText.getBytes(Charset.forName("UTF-8")));
            } catch (Exception e) {
                if (Constants.isDebug) {
                    LogUtil.e(TAG, e);
                }
            }
            CrashInfo crashInfo;
            String cachedJson = SharedPrefUtils.getInstance().getKeyStringValue(DeviceInfo.CRASH_INFO, "");
            if (TextUtils.isEmpty(cachedJson)) {
                crashInfo = new CrashInfo();
                crashInfo.setErrorMd5(errorMd5);
                crashInfo.setUpdateTime(System.currentTimeMillis());
            } else {
                crashInfo = JSONUtil.fromJSON(cachedJson, CrashInfo.class);
                if (crashInfo == null) {
                    crashInfo = new CrashInfo();
                    crashInfo.setErrorMd5(errorMd5);
                    crashInfo.setUpdateTime(System.currentTimeMillis());
                } else if (!crashInfo.getErrorMd5().equals(errorMd5)) {
                    crashInfo = new CrashInfo();
                    crashInfo.setErrorMd5(errorMd5);
                    crashInfo.setUpdateTime(System.currentTimeMillis());
                } else if (System.currentTimeMillis() - crashInfo.getUpdateTime() > 5000) {
                    crashInfo = new CrashInfo();
                    crashInfo.setErrorMd5(errorMd5);
                    crashInfo.setUpdateTime(System.currentTimeMillis());
                } else {
                    // 5 秒内同一个堆栈，判为重复崩溃
                    shouldSave = false;
                }
            }
            if (shouldSave) {
                SharedPrefUtils.getInstance().saveKeyStringValue(DeviceInfo.CRASH_INFO, JSONUtil.toJSON(crashInfo));
            }
        }
        if (!shouldSave || TextUtils.isEmpty(errorMd5) || TextUtils.isEmpty(stackText)) {
            return;
        }
        String stackTrace = formatStackTrace(stackText);
        if (Constants.isDebug) {
            LogUtil.e(TAG, stackTrace);
        }
        ExceptionEvent exceptionEvent = new ExceptionEvent();
        exceptionEvent.reason = throwable.toString();
        exceptionEvent.stack = stackTrace;
        long internalStoreTotalSize = StoreUtils.getInternalStoreTotalSize();
        long internalStoreAvailableSize = StoreUtils.getInternalStoreAvailableSize();
        exceptionEvent.diskTotal = StoreUtils.convertSizeUnit(internalStoreTotalSize);
        exceptionEvent.diskUsage = StoreUtils.availablepercent(internalStoreAvailableSize, internalStoreTotalSize);
        long externalStoreTotalSize = StoreUtils.getExternalStoreTotalSize();
        long externalStoreAvailableSize = StoreUtils.getExternalStoreAvailableSize();
        exceptionEvent.sdTotal = StoreUtils.convertSizeUnit(externalStoreTotalSize);
        exceptionEvent.sdUsage = StoreUtils.availablepercent(externalStoreAvailableSize, externalStoreTotalSize);
        long memoryTotalSize = StoreUtils.getMemoryTotalSize();
        long memoryAvailable = StoreUtils.getMemoryAvailable(ContextUtils.getContext());
        exceptionEvent.memTotal = StoreUtils.convertSizeUnit(StoreUtils.getMemoryTotalSize());
        exceptionEvent.memUsage = StoreUtils.availablepercent(memoryAvailable, memoryTotalSize);
        exceptionEvent.functionName = EType.NAME_APP_EXCEPTION;
        exceptionEvent.makeData();
        ShareHelper.getInstance().insert(exceptionEvent.getContentValues());
        LogUtil.d(TAG, "record crash info done");
    }

    /**
     * 给堆栈加一个可读的头（时间 + 分隔标记）。
     *
     * @param stackText 原始堆栈
     * @return 带头的堆栈
     */
    private String formatStackTrace(String stackText) {
        // "=" 是原厂用 ormlite 的 SimpleComparison.EQUAL_TO_OPERATION 拼的，值就是等号
        return TextUtils.concat("#", "\n", "#", new Date().toString(), "\n", STACK_TRACE, "=", stackText).toString();
    }

    /**
     * 取单例。
     *
     * @return 单例
     */
    public static CrashHandler getInstance() {
        if (mInstance == null) {
            mInstance = new CrashHandler();
        }
        return mInstance;
    }

    /**
     * 找最近一次写下的崩溃报告文件。
     *
     * @return 文件绝对路径；目录不存在或没有文件时返回 null
     */
    private String getLastCrashReportFile() {
        String latestFile = null;
        String logPath = resolveLogPath();
        if (logPath == null) {
            return null;
        }
        File logDir = new File(logPath);
        if (logDir.isFile()) {
            return logDir.getAbsolutePath();
        }
        File[] files = logDir.listFiles();
        if (files == null) {
            return null;
        }
        long lastModified = 0;
        for (File file : files) {
            if (!file.isDirectory() && file.lastModified() >= lastModified) {
                latestFile = file.getAbsolutePath();
                lastModified = file.lastModified();
            }
        }
        return latestFile;
    }

    /**
     * 处理异常（null 直接忽略）。
     *
     * @param throwable 异常
     * @return 是否处理了
     */
    private boolean handleException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        handleExec(throwable);
        return true;
    }

    /**
     * 按配置初始化日志目录与提示开关。
     *
     * @param needToast 崩溃时是否 Toast
     */
    private void init(boolean needToast) {
        if (ContextUtils.isEmpty()) {
            return;
        }
        this.mLogPath = DEFAULT_CRASH_LOG_PATH + ContextUtils.getContext().getPackageName() + File.separator;
        this.mCrashCacheSize = DEFAULT_CRASH_CACHE_SIZE;
        this.needToast = needToast;
    }

    /**
     * 重启次数是否过多。
     *
     * @return 是否达到上限
     */
    private boolean isReStartTooMany() {
        return readReStartCount() >= RESTART_COUNT;
    }

    /**
     * 是否需要 UI 上报。
     *
     * @return 是否需要
     */
    private boolean needUIReport() {
        return this.needUIReport;
    }

    /**
     * 把 Properties 逐行打出来。
     *
     * @param properties 属性表
     * @param printWriter 输出
     */
    private void printProperties(Properties properties, PrintWriter printWriter) {
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            printWriter.print(key);
            printWriter.print('=');
            String value = (String) properties.get(key);
            if (value == null) {
                value = "unknown.";
            }
            printWriter.println(value);
        }
    }

    /**
     * 读首次崩溃时间。
     *
     * @return 毫秒时间戳；没记录过返回 0
     */
    private long readFirstTime() {
        return SharedPrefUtils.getInstance().getKeyLongValue(FIRST_CRASH_TIME, 0L);
    }

    /**
     * 读连续重启次数。
     *
     * @return 次数
     */
    private int readReStartCount() {
        return SharedPrefUtils.getInstance().getKeyIntValue(CRASH_COUNT, 0);
    }

    /**
     * 读最近一次崩溃时间。
     *
     * @return 毫秒时间戳
     */
    private long readRestartTime() {
        return SharedPrefUtils.getInstance().getKeyLongValue(LAST_CRASH_TIME, 0L);
    }

    /** 重启次数 +1。 */
    private void recordReStartCount() {
        int restartCount = readReStartCount();
        if (DEBUG) {
            LogUtil.d(TAG, "read re-start count: " + restartCount);
        }
        saveReStartCount(restartCount + 1);
    }

    /**
     * 生成崩溃日志文件名（时间戳到秒）。
     *
     * @return 文件名；生成失败返回 null
     */
    private String resolveLogFileName() {
        try {
            Calendar calendar = Calendar.getInstance();
            return String.format(Locale.CHINA, "crash--%d-%d-%d-%d.%d.%d%s",
                    Integer.valueOf(calendar.get(Calendar.YEAR)),
                    Integer.valueOf(calendar.get(Calendar.MONTH) + 1),
                    Integer.valueOf(calendar.get(Calendar.DATE)),
                    Integer.valueOf(calendar.get(Calendar.HOUR_OF_DAY)),
                    Integer.valueOf(calendar.get(Calendar.MINUTE)),
                    Integer.valueOf(calendar.get(Calendar.SECOND)),
                    CRASH_REPORTER_EXTENSION);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 取崩溃日志目录（没配就用应用私有 cache 目录）。
     *
     * @return 目录路径；取不到返回 null
     */
    private String resolveLogPath() {
        if (this.mLogPath != null) {
            return this.mLogPath;
        }
        try {
            return ContextUtils.getContext().getCacheDir().toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 把崩溃堆栈写到文件（带"先建目录、超限清理"的完整流程）。
     *
     * @param throwable 崩溃
     * @return 带头的堆栈文本
     */
    private String saveCrashInfoToFile(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause()) {
            cause.printStackTrace(printWriter);
        }
        String stackTrace = formatStackTrace(stringWriter.toString());
        printWriter.close();
        String logPath = resolveLogPath();
        if (logPath == null) {
            LogUtil.e(TAG, "log path invalid, can't save crash log file !!");
        } else {
            String logFileName = resolveLogFileName();
            if (logFileName == null) {
                LogUtil.e(TAG, "log file name invalid, can't save crash log file !!");
            } else {
                String filePath = logPath + File.separator + logFileName;
                if (StoreUtils.checkFileDirExisted(filePath)) {
                    checkCacheSize();
                    try {
                        FileUtils.writeFile(new File(filePath), stackTrace, true);
                    } catch (IOException e) {
                        LogUtil.e(TAG, e);
                    }
                } else {
                    LogUtil.e(TAG, "save crash info: create crash file dir error !");
                }
            }
        }
        return stackTrace;
    }

    /**
     * 保存重启计数（第一次同时记首次崩溃时间）。
     *
     * @param restartCount 次数
     */
    private void saveReStartCount(int restartCount) {
        if (restartCount == 1) {
            SharedPrefUtils.getInstance().saveKeyLongValue(FIRST_CRASH_TIME, System.currentTimeMillis());
        }
        SharedPrefUtils.getInstance().saveKeyLongValue(LAST_CRASH_TIME, System.currentTimeMillis());
        SharedPrefUtils.getInstance().saveKeyIntValue(CRASH_COUNT, restartCount);
    }

    /** 崩溃提示用的 Toast 线程（原厂就是起一个临时线程自己 prepare/loop）。 */
    private static final class CrashToastThread extends Thread {

        @Override
        public void run() {
            try {
                Looper.prepare();
                Toast.makeText(ContextUtils.getContext(), "程序异常", 0).show();
                Looper.loop();
            } catch (Exception e) {
                LogUtil.e(CrashHandler.TAG, e);
            }
        }
    }

    /** 弹崩溃提示（没有 Context 或没开开关时什么都不做）。 */
    private void showToast() {
        if (ContextUtils.isEmpty() || !this.needToast) {
            return;
        }
        new CrashToastThread().start();
    }

    /** 重置连续重启次数。 */
    public void cleanReStartCount() {
        if (DEBUG) {
            LogUtil.d(TAG, "clean re-start count !!");
        }
        saveReStartCount(0);
    }

    /**
     * 处理一个崩溃。
     *
     * <p>主线程崩溃时丢到后台线程处理（主线程马上就要被杀），
     * 其它线程直接在当前线程处理。</p>
     *
     * @param throwable 崩溃
     */
    public void handleExec(final Throwable throwable) {
        if (ContextUtils.isEmpty()) {
            LogUtil.w(TAG, "进程死亡，无法保存异常");
        } else if (Looper.getMainLooper() == Looper.myLooper()) {
            this.crashHandler.post(new Runnable() {
                @Override
                public void run() {
                    collectCrash(throwable);
                }
            });
        } else {
            collectCrash(throwable);
        }
    }

    /**
     * 读取最近一次崩溃报告的内容。
     *
     * @return 报告文本；没有报告返回 null
     */
    public String loadLastCrashReport() {
        String lastCrashReportFile = getLastCrashReportFile();
        if (lastCrashReportFile == null) {
            return null;
        }
        Properties properties = new Properties();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(lastCrashReportFile));
            properties.load(fileInputStream);
            printWriter.println(lastCrashReportFile + " -----");
            printProperties(properties, printWriter);
            properties.clear();
            printWriter.println();
            printWriter.println();
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }
        return stringWriter.toString();
    }

    /** 注册为全局未捕获异常处理器。 */
    public void registerCrashHandler() {
        init(ConfigAgent.getBehaviorConfig().crashToastUsable);
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 设置崩溃日志缓存上限。
     *
     * @param cacheSize 字节数；<=0 时忽略
     */
    public void setCrashCacheSize(long cacheSize) {
        if (cacheSize <= 0) {
            return;
        }
        this.mCrashCacheSize = cacheSize;
    }

    /**
     * 设置崩溃日志目录。
     *
     * @param logPath 目录路径
     */
    public void setCrashLogPath(String logPath) {
        this.mLogPath = logPath;
    }

    /**
     * 设置是否需要 UI 上报。
     *
     * @param needUIReport 是否需要
     */
    public void setCrashNeedUIReport(boolean needUIReport) {
        this.needUIReport = needUIReport;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        handleException(throwable);
        // 处理完把异常还给系统默认处理器，保证系统仍能弹"应用已停止"
        if (this.mDefaultHandler != null) {
            this.mDefaultHandler.uncaughtException(thread, throwable);
        }
    }
}
