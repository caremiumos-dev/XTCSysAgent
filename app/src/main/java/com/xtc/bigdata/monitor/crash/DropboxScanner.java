package com.xtc.bigdata.monitor.crash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;

import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.collector.encapsulation.entity.attr.ApplicationAttr;
import com.xtc.bigdata.collector.encapsulation.entity.event.AEvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.DropBoxANREvent;
import com.xtc.bigdata.collector.encapsulation.entity.event.DropBoxCrashEvent;
import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.collector.utils.DateFormatUtil;
import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.bigdata.common.utils.SharedPrefUtils;
import com.xtc.log.LogUtil;
import com.xtc.system.account.bean.AppInfoBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * DropBox 崩溃/ANR 采集器。
 *
 * <p>系统把 native crash、ANR、strictmode 等都以 "DropBox 条目" 的形式存着，
 * 本类就是把这些条目解析成 {@code DropBoxCrashEvent}/{@code DropBoxANREvent}
 * 再落库。三条关键设计：</p>
 *
 * <ul>
 *   <li><b>游标推进</b>：每种 tag 记住上次扫到的条目时间（存 SP，
 *       key 是 {@code <tag>_collect_time}），下次从那里继续，不重复扫；</li>
 *   <li><b>单线程串行</b>：DropBox 读取是 IO 密集的，用一个单线程池串行处理，
 *       并发的 {@code start()} 只会堆任务不会并发读；</li>
 *   <li><b>去重落盘</b>：相同堆栈会复用已存在的文件名（时间戳），
 *       避免同一个崩溃刷出一堆文件。</li>
 * </ul>
 *
 * <p>另外它会替换掉 {@link ApplicationAttr} 里的包名/版本号 ——
 * DropBox 里崩的是<b>别的应用</b>，属性表必须跟着改，否则上报归属就错了。</p>
 */
public class DropboxScanner {

    private static final String COLLECT_TIME_KEY = "_collect_time";

    private static final String DATA_APP_ANR = "data_app_anr";

    private static final String DATA_APP_CRASH = "data_app_crash";

    private static final String SYSTEM_APP_ANR = "system_app_anr";

    private static final String SYSTEM_APP_CRASH = "system_app_crash";

    private static final String TAG = "DropboxScanner";

    /** 全局唯一的 DropBox 广播接收者。 */
    private static DropboxReceiver dropboxReceiver = null;

    /** 需要解析的字段名，顺序决定字段落进哪个属性。 */
    private static List<String> keyList = new ArrayList<String>();

    /** 仅 debug 模式下使用的测试 action。 */
    private static final String testAction = "test.action";

    /** 仅 debug 模式下使用的测试 tag。 */
    private static final String testKey = "system_app_strictmode";

    /** 崩溃堆栈缓冲。 */
    private StringBuilder sb;

    /** 是否正在扫（防止并发扫同一个 DropBox）。 */
    private boolean scanning;

    /** 单线程执行器。 */
    private ThreadPoolExecutor singleExecutor;

    /** 最近一次解析出的堆栈。 */
    private String stackTrace;

    /** 调试开关：打开后会响应 {@link #testAction} 并放开 tag 白名单。 */
    private boolean debug = false;

    /** 每种 tag 上次扫到的时间。 */
    private volatile Map<String, Long> collectTimeMap = new HashMap<String, Long>();

    /** DropBox 条目新增广播的接收者。 */
    public class DropboxReceiver extends BroadcastReceiver {

        /** 是否已注册。 */
        boolean registed;

        public DropboxReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if ("android.intent.action.DROPBOX_ENTRY_ADDED".equals(action)
                    || (DropboxScanner.this.debug && DropboxScanner.testAction.equals(action))) {
                // time - 1 是为了把"刚好等于游标"的那条也扫进来
                startScan(context, intent.getStringExtra(AppInfoBase.KEY_TAG),
                        intent.getLongExtra("time", 0L) - 1);
            }
        }

        /**
         * 注册（重复调用无副作用）。
         *
         * @param context 上下文
         */
        synchronized void regist(Context context) {
            if (!this.registed) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.DROPBOX_ENTRY_ADDED");
                intentFilter.addAction(DropboxScanner.testAction);
                context.registerReceiver(this, intentFilter);
                this.registed = true;
            }
        }

        /**
         * 注销（未注册时无副作用）。
         *
         * @param context 上下文
         */
        synchronized void unregist(Context context) {
            if (this.registed) {
                context.unregisterReceiver(this);
                this.registed = false;
            }
        }
    }

    static {
        keyList.add("Process");
        keyList.add("Flags");
        keyList.add("Package");
        keyList.add("Activity");
        keyList.add("Parent-Process");
        keyList.add("Parent-Activity");
        keyList.add("Foreground");
        keyList.add("Subject");
        keyList.add("Build");
        keyList.add("Debugger");
    }

    public DropboxScanner() {
        dropboxReceiver = new DropboxReceiver();
        loadCollectTime();
        this.singleExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 把堆栈写到按天目录下；如果同样的内容已经存在，复用旧的文件名（时间戳）。
     *
     * @param stackTrace 堆栈
     * @param time       条目时间
     * @return 写入的文件路径
     */
    private String checkStackTraceInfoAndWrite(String stackTrace, long time) {
        String dirPath = Environment.getExternalStorageDirectory().getPath() + "/bigData/dropBox/"
                + DateFormatUtil.format("yyyyMMdd", System.currentTimeMillis()) + "/";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return saveToFile(dirPath, stackTrace, time + ".txt");
        }
        File[] existingFiles = dir.listFiles();
        if (existingFiles == null || existingFiles.length == 0) {
            return saveToFile(dirPath, stackTrace, time + ".txt");
        }
        for (File existingFile : existingFiles) {
            if (existingFile != null && stackTrace.equals(readFileContent(existingFile))) {
                String fileName = existingFile.getName();
                String fileTime = fileName.substring(0, fileName.indexOf(".txt"));
                LogUtil.i(TAG, "the same stack dropBox file name = " + fileTime);
                time = Long.valueOf(fileTime).longValue();
                break;
            }
        }
        return saveToFile(dirPath, stackTrace, time + ".txt");
    }

    /** 清理过期的 DropBox 落盘文件（单文件/目录超过 20MB，或超过 7 天）。 */
    private void clearExpireDropBoxFile() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/bigData/dropBox/");
        File[] existingFiles;
        if (!dir.exists() || (existingFiles = dir.listFiles()) == null || existingFiles.length == 0) {
            return;
        }
        for (File existingFile : existingFiles) {
            if (existingFile != null) {
                long fileLength = getFileLength(existingFile);
                LogUtil.d(TAG, "total fileLength = " + fileLength);
                if (fileLength > 20971520) {
                    FileUtils.deleteDir(existingFile);
                    LogUtil.i(TAG, "delete anr file when file/Dir is larger than 20MB = " + existingFile);
                } else if (System.currentTimeMillis() - existingFile.lastModified() >= 604800000) {
                    FileUtils.deleteDir(existingFile);
                    LogUtil.i(TAG, "delete expire dropBox file = " + existingFile);
                }
            }
        }
    }

    /**
     * 解析一条 ANR 条目。
     *
     * <p>只读前 {@code keyList.size() + 2} 行（字段行之后紧跟着一段堆栈，
     * 不读完是为了省 IO），行格式是 {@code "Key: value"}。</p>
     *
     * @param entry       DropBox 条目
     * @param inputStream 条目内容流
     */
    private void collectANR(DropBoxManager.Entry entry, InputStream inputStream) throws Exception {
        LogUtil.w(TAG, "collectANR begin ---");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        long beginTime = SystemClock.elapsedRealtime();
        DropBoxANREvent dropBoxANREvent = new DropBoxANREvent();
        dropBoxANREvent.functionName = "DropBox_ANR";
        boolean debuggerConnected = false;
        int lineCount = 0;
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
            lineCount++;
            if (lineCount >= keyList.size() + 2) {
                break;
            }
            if (!TextUtils.isEmpty(line)) {
                String[] keyAndValue = line.split(":");
                if (keyAndValue.length >= 2) {
                    String key = keyAndValue[0];
                    String value = line.substring(line.indexOf(":") + 1, line.length()).trim();
                    LogUtil.d(TAG, key + "----" + value);
                    switch (keyList.indexOf(key)) {
                        case 0:
                            dropBoxANREvent.processName = value;
                            break;
                        case 1:
                            dropBoxANREvent.flags = value;
                            break;
                        case 2:
                            dropBoxANREvent.packageInfo = value;
                            break;
                        case 3:
                            dropBoxANREvent.activity = value;
                            break;
                        case 4:
                            dropBoxANREvent.parentProcess = value;
                            break;
                        case 5:
                            dropBoxANREvent.parentActivity = value;
                            break;
                        case 6:
                            dropBoxANREvent.foreground = value;
                            break;
                        case 7:
                            dropBoxANREvent.subject = value;
                            break;
                        case 8:
                            dropBoxANREvent.build = value;
                            break;
                        case 9:
                            // Debugger 行只要有内容就说明当时连着调试器
                            debuggerConnected = true;
                            break;
                        default:
                            LogUtil.i(TAG, "don't care !");
                            break;
                    }
                } else {
                    LogUtil.e(TAG, "line split array's length error !");
                }
            }
        }
        bufferedReader.close();
        LogUtil.d(TAG, "本次 anr " + dropBoxANREvent + ", read stream cost = "
                + (SystemClock.elapsedRealtime() - beginTime));
        if (debuggerConnected) {
            LogUtil.w(TAG, "debug connected when this anr occur , so don't collect !");
        } else {
            String[] packageAndVersion = getPV(dropBoxANREvent.packageInfo);
            finalPackInfoAndInsert(dropBoxANREvent, packageAndVersion[0], packageAndVersion[1]);
        }
    }

    /**
     * 解析一条原生崩溃条目。
     *
     * <p>从出现 {@code "Exception:"} 的那行开始整段留下（那才是堆栈），
     * 之前只挑 {@code Process:} / {@code Package:} 两行。</p>
     *
     * @param entry       DropBox 条目
     * @param inputStream 条目内容流
     */
    private void collectCrash(DropBoxManager.Entry entry, InputStream inputStream) throws Exception {
        LogUtil.w(TAG, "collectCrash begin ---");
        this.sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        long beginTime = SystemClock.elapsedRealtime();
        String processName = "";
        String packageName = "";
        String versionName = "";
        boolean stackStarted = false;
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                bufferedReader.close();
                LogUtil.d(TAG, "本次 crash , processName=" + processName + ", packageName=" + packageName
                        + ", versionName=" + versionName + ", read stream cost = "
                        + (SystemClock.elapsedRealtime() - beginTime));
                collectCrashException(this.sb.toString(), processName, packageName, versionName);
                resetTemp();
                return;
            }
            if (stackStarted) {
                this.sb.append(line).append("\n");
            } else if (line.contains("Exception:")) {
                this.sb.append(line).append("\n");
                stackStarted = true;
            } else if (line.contains("Process:")) {
                processName = getProcessNames(line);
            } else if (line.contains("Package:")) {
                String[] packageAndVersionNames = getPackageAndVersionNames(line);
                packageName = packageAndVersionNames[0];
                versionName = packageAndVersionNames[1];
            } else {
                LogUtil.d(TAG, "do nothing ~");
            }
        }
    }

    /**
     * 组装崩溃事件并落库。
     *
     * @param stackTrace  堆栈
     * @param processName 进程名
     * @param packageName 包名
     * @param versionName 版本名
     */
    private void collectCrashException(String stackTrace, String processName, String packageName, String versionName) {
        this.stackTrace = stackTrace;
        LogUtil.i(TAG, "collectCrashException , stackTraceInfo = " + this.stackTrace);
        if (TextUtils.isEmpty(this.stackTrace)) {
            return;
        }
        DropBoxCrashEvent dropBoxCrashEvent = new DropBoxCrashEvent();
        dropBoxCrashEvent.functionName = "DropBox_Crash";
        if (!Objects.equals(processName, packageName)) {
            LogUtil.w(TAG, "Warning , processName != packageInfo !");
        }
        dropBoxCrashEvent.processName = processName;
        dropBoxCrashEvent.reason = this.stackTrace;
        dropBoxCrashEvent.stack = this.stackTrace;
        finalPackInfoAndInsert(dropBoxCrashEvent, packageName, versionName);
    }

    /**
     * 按 tag 分派到崩溃/ANR 解析。
     *
     * @param tag         DropBox tag
     * @param entry       条目
     * @param inputStream 条目内容流
     */
    private void dealWithOneException(String tag, DropBoxManager.Entry entry, InputStream inputStream) throws Exception {
        if (DATA_APP_CRASH.equals(tag) || SYSTEM_APP_CRASH.equals(tag)) {
            collectCrash(entry, inputStream);
        } else if (DATA_APP_ANR.equals(tag) || SYSTEM_APP_ANR.equals(tag)) {
            collectANR(entry, inputStream);
        } else {
            LogUtil.w(TAG, "error ! do not care about this tag = " + tag);
        }
    }

    /**
     * 克隆属性、替换应用包名版本号，然后落库。
     *
     * <p>必须 clone：事件里的属性实例是全局共享的，直接改会把其它事件的
     * 包名一起改掉。</p>
     *
     * @param event       事件
     * @param packageName 崩溃应用的包名
     * @param versionName 崩溃应用的版本名
     */
    private void finalPackInfoAndInsert(AEvent event, String packageName, String versionName) {
        ArrayList<IAttr> attrs = new ArrayList<IAttr>();
        for (IAttr attr : event.makeData()) {
            if (attr != null) {
                attrs.add(attr.clone());
            } else {
                attrs.add(null);
            }
        }
        for (int index = 0; index < attrs.size(); index++) {
            IAttr attr = attrs.get(index);
            if (attr instanceof ApplicationAttr) {
                ((ApplicationAttr) attr).setPackageName(packageName).setAppVersion(versionName);
                LogUtil.d(TAG, "after modify , final ApplicationAttr = " + attr);
                break;
            }
        }
        ShareHelper.getInstance().insert(event.getTempContentValues(attrs));
    }

    /**
     * 取某个 tag 上次扫到的时间。
     *
     * @param tag DropBox tag
     * @return 毫秒时间戳；没记录过返回 0
     */
    public long getCollectTime(String tag) {
        Long collectTime = this.collectTimeMap.get(tag);
        if (collectTime == null) {
            return 0L;
        }
        return collectTime.longValue();
    }

    /**
     * 递归统计目录大小。
     *
     * @param dir 目录
     * @return 字节数
     */
    private long getDirFileLength(File dir) {
        File[] children;
        long totalLength = 0;
        if (dir != null && (children = dir.listFiles()) != null && children.length != 0) {
            for (File child : children) {
                if (child != null) {
                    if (child.isFile()) {
                        totalLength += child.length();
                    } else if (child.isDirectory()) {
                        totalLength += getDirFileLength(child);
                    } else {
                        LogUtil.w(TAG, "unknown file type = " + child);
                    }
                }
            }
        }
        return totalLength;
    }

    /**
     * 取文件或目录大小。
     *
     * @param file 文件
     * @return 字节数
     */
    private long getFileLength(File file) {
        if (file == null) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        if (file.isDirectory()) {
            return getDirFileLength(file);
        }
        LogUtil.w(TAG, "unknown file type = " + file);
        return 0L;
    }

    /**
     * 从 {@code "包名 (版本名)"} 里拆出包名与版本名。
     *
     * @param packageInfo 形如 {@code "com.xtc.app (1.0)"}
     * @return 长度 2 的数组：[0]=包名，[1]=版本名
     */
    private String[] getPV(String packageInfo) {
        String[] packageAndVersion = {"", ""};
        if (!TextUtils.isEmpty(packageInfo)) {
            // 分隔符原厂用的是 jackson 的 MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR，就是一个空格
            packageAndVersion[0] = packageInfo.split(" ")[0];
            if (packageInfo.contains("(")) {
                packageAndVersion[1] = packageInfo.substring(packageInfo.indexOf("(") + 1, packageInfo.indexOf(")"));
            }
        }
        return packageAndVersion;
    }

    /**
     * 从 {@code "Package: com.xtc.app (1.0)"} 行里拆出包名与版本名。
     *
     * @param packageLine 原始行
     * @return 长度 2 的数组：[0]=包名，[1]=版本名
     */
    private String[] getPackageAndVersionNames(String packageLine) {
        String[] packageAndVersion = {"", ""};
        String[] keyAndValue = packageLine.split(":");
        if (keyAndValue.length >= 2) {
            String value = keyAndValue[1].trim();
            packageAndVersion[0] = value.split(" ")[0];
            if (value.contains("(")) {
                packageAndVersion[1] = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
            }
        }
        return packageAndVersion;
    }

    /**
     * 从 {@code "Process: com.xtc.app"} 行里取进程名。
     *
     * @param processLine 原始行
     * @return 进程名；格式不对返回空串
     */
    private String getProcessNames(String processLine) {
        String[] keyAndValue = processLine.split(":");
        return keyAndValue.length < 2 ? "" : keyAndValue[1].trim();
    }

    /** 从 SP 恢复每种 tag 的扫锚游标。 */
    private void loadCollectTime() {
        this.collectTimeMap.put(DATA_APP_CRASH,
                Long.valueOf(SharedPrefUtils.getInstance().getKeyLongValue(DATA_APP_CRASH + COLLECT_TIME_KEY, 0L)));
        this.collectTimeMap.put(DATA_APP_ANR,
                Long.valueOf(SharedPrefUtils.getInstance().getKeyLongValue(DATA_APP_ANR + COLLECT_TIME_KEY, 0L)));
        this.collectTimeMap.put(SYSTEM_APP_CRASH,
                Long.valueOf(SharedPrefUtils.getInstance().getKeyLongValue(SYSTEM_APP_CRASH + COLLECT_TIME_KEY, 0L)));
        this.collectTimeMap.put(SYSTEM_APP_ANR,
                Long.valueOf(SharedPrefUtils.getInstance().getKeyLongValue(SYSTEM_APP_ANR + COLLECT_TIME_KEY, 0L)));
    }

    /**
     * 读文件内容（用于"同堆栈去重"比对）。
     *
     * <p>读失败时返回的是<b>已读到的半截内容</b>而不是空串，
     * 这是原厂行为（对比时最多误判为"不同"，不会误判为"相同"）。</p>
     *
     * @param file 文件
     * @return 内容
     */
    private String readFileContent(File file) {
        int fileLength = (int) file.length();
        if (fileLength <= 0) {
            return "";
        }
        byte[] data = new byte[fileLength];
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(data, 0, fileLength);
        } catch (IOException e) {
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
        return new String(data);
    }

    /** 清掉本次解析的临时状态。 */
    private void resetTemp() {
        this.stackTrace = null;
        if (this.sb != null) {
            this.sb.delete(0, this.sb.length());
            this.sb = null;
        }
    }

    /**
     * 记录某个 tag 的扫锚游标（内存 + SP）。
     *
     * @param tag   DropBox tag
     * @param time  条目时间
     */
    private void saveCollectTime(String tag, long time) {
        this.collectTimeMap.put(tag, Long.valueOf(time));
        SharedPrefUtils.getInstance().saveKeyLongValue(tag + COLLECT_TIME_KEY, time);
        LogUtil.i(TAG, "update collectTime = " + time + " of dropBoxTag = " + tag);
    }

    /**
     * 清理过期文件后落盘。
     *
     * @param stackTrace 堆栈
     * @param time       条目时间
     * @return 文件路径
     */
    private String saveStackInfoAsFile(String stackTrace, long time) {
        clearExpireDropBoxFile();
        return checkStackTraceInfoAndWrite(stackTrace, time);
    }

    /**
     * 写文件并返回完整路径。
     *
     * @param dirPath    目录
     * @param content    内容
     * @param fileName   文件名
     * @return 完整路径
     */
    private String saveToFile(String dirPath, String content, String fileName) {
        FileUtils.saveFile(dirPath, content, fileName);
        return dirPath + fileName;
    }

    /**
     * 扫一个 tag 下的所有新条目。
     *
     * @param context 上下文
     * @param tag     DropBox tag
     * @param time    起始时间（上次扫到的位置）
     */
    public void scanAndCollect(Context context, String tag, long time) {
        int scannedCount;
        if (!this.debug && !DATA_APP_CRASH.equals(tag) && !DATA_APP_ANR.equals(tag)
                && !SYSTEM_APP_CRASH.equals(tag) && !SYSTEM_APP_ANR.equals(tag)) {
            LogUtil.w(TAG, "do not care about this tag = " + tag);
            return;
        }
        if (getCollectTime(tag) > time) {
            LogUtil.w(TAG, tag + " this time = " + time + " happened in dropBox has scanned before !");
            return;
        }
        long beginTime = SystemClock.elapsedRealtime();
        DropBoxManager dropBoxManager = (DropBoxManager) context.getSystemService("dropbox");
        if (dropBoxManager == null) {
            LogUtil.e(TAG, "error , dropBoxManager = null !");
            return;
        }
        int count = 0;
        try {
            synchronized (this) {
                this.scanning = true;
            }
            LogUtil.w(TAG, "start scan dropBoxTag = " + tag + " , time = " + time);
            while (true) {
                DropBoxManager.Entry entry = dropBoxManager.getNextEntry(tag, time);
                if (entry == null) {
                    break;
                }
                LogUtil.i(TAG, "scanned a dropBox entry,tag = " + entry.getTag() + " , time = " + entry.getTimeMillis());
                InputStream inputStream = entry.getInputStream();
                if (inputStream == null) {
                    entry.close();
                    break;
                }
                dealWithOneException(tag, entry, inputStream);
                time = entry.getTimeMillis();
                saveCollectTime(tag, time);
                entry.close();
                count++;
            }
            scannedCount = count;
        } catch (Exception e) {
            scannedCount = 0;
            LogUtil.e(TAG, "loop scan error = " + e);
            e.printStackTrace();
        } finally {
            synchronized (this) {
                this.scanning = false;
            }
        }
        LogUtil.i(TAG, "scan finished , count = " + scannedCount + " ,  this cost time = "
                + (SystemClock.elapsedRealtime() - beginTime));
    }

    /**
     * 异步触发一次扫描。
     *
     * @param context 上下文
     * @param tag     DropBox tag
     * @param time    起始时间
     */
    public void startScan(final Context context, final String tag, final long time) {
        synchronized (this) {
            if (this.scanning) {
                LogUtil.w(TAG, "dropBox is scanning , return !");
            } else {
                this.singleExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        scanAndCollect(context, tag, time);
                    }
                });
            }
        }
    }

    /**
     * 开始跟随 DropBox（注册广播 + 立刻补扫一次）。
     *
     * @param context 上下文
     */
    public void start(final Context context) {
        dropboxReceiver.regist(context);
        if (this.singleExecutor.isShutdown()) {
            this.singleExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
        }
        long activeCount = this.singleExecutor.getActiveCount();
        if (activeCount >= 10) {
            LogUtil.w(TAG, "dropBox scan active task count is too many = " + activeCount);
        } else {
            this.singleExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    scanAndCollect(context, DATA_APP_CRASH, getCollectTime(DATA_APP_CRASH));
                    scanAndCollect(context, DATA_APP_ANR, getCollectTime(DATA_APP_ANR));
                    scanAndCollect(context, SYSTEM_APP_CRASH, getCollectTime(SYSTEM_APP_CRASH));
                    scanAndCollect(context, SYSTEM_APP_ANR, getCollectTime(SYSTEM_APP_ANR));
                    clearExpireDropBoxFile();
                }
            });
        }
    }

    /**
     * 停止跟随（注销广播 + 关掉线程池）。
     *
     * @param context 上下文
     */
    public void stop(Context context) {
        dropboxReceiver.unregist(context);
        this.singleExecutor.shutdownNow();
    }

    /**
     * 手动触发一次 strictmode 扫描（调试用）。
     *
     * @param context 上下文
     */
    public void testScan(Context context) {
        startScan(context, testKey, 0L);
    }
}
