package com.xtc.systemagent.module.log.serve.impl;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.httplib.net.BaseSubscriber;
import com.xtc.httplib.net.HttpSubscriber;
import com.xtc.log.LogUtil;
import com.xtc.qiniu.ICloudManager;
import com.xtc.qiniu.ICloudService;
import com.xtc.qiniu.TokenManager;
import com.xtc.qiniu.bean.NetUploadToken;
import com.xtc.systemagent.R;
import com.xtc.systemagent.module.Constants;
import com.xtc.systemagent.module.log.serve.ILogCollectService;
import com.xtc.systemagent.net.LogHttpProxy;
import com.xtc.systemagent.net.bean.LogStrategy;
import com.xtc.systemagent.net.bean.LogStrategyParam;
import com.xtc.systemagent.net.bean.UploadNotifyParam;
import com.xtc.systemagent.util.DateUtil;
import com.xtc.systemagent.util.LogCollectUtil;
import com.xtc.systemagent.util.PropertiesUtil;
import com.xtc.systemagent.util.SdCardUtil;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.utils.common.CollectionUtils;
import com.xtc.utils.date.DateUtils;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.encode.ZipUtils;
import com.xtc.utils.system.NetworkUtils;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 通用日志采集与上传实现。
 *
 * <p>职责：
 * <ol>
 *   <li>按服务端下发的 {@link LogStrategy} 从各个日志目录收集文件；</li>
 *   <li>打包成 zip；</li>
 *   <li>向七牛申请上传 token 并上传；</li>
 *   <li>把采集/上传结果回通知给服务端。</li>
 * </ol>
 *
 * <p>上传前的网络处理策略与原厂一致：优先走 WiFi/4G；若当前网络不满足，
 * 会临时把网络模式切到 4G（{@link LogCollectUtil#forceMobileNetworkTo4G(Context)}），
 * 上传结束（无论成功失败）后延迟 5s 恢复原网络。</p>
 */
public class LogCollectServiceImpl implements ILogCollectService {

    private static final String TAG = "Log_LogCollectServiceImpl";

    /** 电池状态：允许采集。 */
    public static final byte BATTERY_STATUS_ENABLE = 1;

    /** 上传网络限制：仅 WiFi。 */
    private static final int UPLOAD_NET_WIFI = 2;

    /** hprof 文件前缀，也是需要在上传成功后清理的临时文件。 */
    private static final String HPROF_PREFIX = "hprof";

    /** 系统日志包名：走 systemLogDir 收集流程。 */
    private static final String SYSTEM_PACKAGE_NAME = "com.xtc.i11system";

    /** hprof 包名：在系统日志基础上额外带上 hprof 文件。 */
    private static final String SYSTEM_HPROF_PACKAGE_NAME = "com.xtc.hprof";

    /** 系统日志 zip 的文件名前缀。 */
    private static final String ZIP_PREFIX_SYS_LOG = "SYSTEM_LOG_";

    private static final String ZIP_SUFFIX = ".zip";

    /** 语音通话 SDK：其日志目录整体打包，不按日期过滤。 */
    private String NAME_AGORA = "com.xtc.videochat.agora";
    /** 语音通话 SDK：其日志目录整体打包，不按日期过滤。 */
    private String NAME_NETEASE = "com.xtc.videochat.netease";

    private final Context context;
    private final LogHttpProxy logHttpProxy;

    /** 本轮采集对应的服务端日志 id。 */
    private Long id;
    /** 本轮采集对应的策略 id。 */
    private Integer strategyId;
    /** 运营人员 id。 */
    private String operatorId;
    /** 是否为了上传而临时改过网络模式（改过才需要恢复）。 */
    private boolean isNetworkModify = false;

    public LogCollectServiceImpl(Context context) {
        this.context = context.getApplicationContext();
        this.logHttpProxy = new LogHttpProxy(context);
    }

    // ------------------------------------------------------------------
    // 文件名过滤器
    // ------------------------------------------------------------------

    /** 文件名包含指定子串则接受。 */
    private class FileContainsFilter implements FilenameFilter {
        private final String filter;

        FileContainsFilter(String filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.contains(this.filter);
        }
    }

    /** 文件名以指定前缀开头则接受。 */
    private class FileStartsFilter implements FilenameFilter {
        private final String filter;

        FileStartsFilter(String filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(this.filter);
        }
    }

    /**
     * 按目录名里的 {@code Folder_yyyyMMdd} 日期判断是否在最近 15 天内。
     *
     * <p>保留原厂实现：日期解析失败（例如目录名不含 Folder_ 前缀）直接返回 false。</p>
     */
    private class FileOtherFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            String timeStr = reLogFolderTime(name);
            Date fileDate = DateUtil.String2Date(timeStr, "yyyyMMdd");
            LogUtil.d(TAG, "FileOtherFilter fileTimeStr: " + timeStr);
            if (fileDate == null) {
                return false;
            }
            Date now = new Date();
            boolean accept = DateUtil.beforeDate(fileDate, now, -15);
            LogUtil.d(TAG, "FileOtherFilter fileDate: " + fileDate + " ,now: " + now + " ,accept: " + accept);
            return accept;
        }
    }

    // ------------------------------------------------------------------
    // 文件收集
    // ------------------------------------------------------------------

    /**
     * 把 {@code dir} 下满足过滤器的文件追加到 {@code out}。
     *
     * <p>原厂方法带 {@code synchronized}：并发采集时避免 ArrayList 被并发写。</p>
     */
    private synchronized void addFileByFilter(List<File> out, File dir, FilenameFilter filter) {
        if (out == null || dir == null) {
            return;
        }
        try {
            File[] files = filter == null ? dir.listFiles() : dir.listFiles(filter);
            if (files != null && files.length > 0) {
                Collections.addAll(out, files);
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    /**
     * 删除指定后缀的 zip，但跳过 {@code excludeList} 里列出的关键字。
     *
     * @param file        待判定的文件
     * @param suffix      后缀，不匹配直接返回
     * @param excludeList 排除关键字列表；为 null/空表示不排除
     */
    private void deleteFileBySuffix(File file, String suffix, List<String> excludeList) {
        if (file == null || TextUtils.isEmpty(suffix) || !file.getName().endsWith(suffix)) {
            return;
        }
        if (CollectionUtils.isEmpty(excludeList)) {
            LogUtil.d(TAG, "HXQ-delete zip file excludeList is empty,result:" + LogCollectUtil.deleteFile(file));
            return;
        }
        boolean excluded = false;
        for (String exclude : excludeList) {
            if (!TextUtils.isEmpty(exclude)) {
                excluded = file.getName().toLowerCase().contains(exclude.toLowerCase()) ? true : excluded;
            }
        }
        LogUtil.d(TAG, "HXQ-delete zip file,result:" + (excluded ? false : LogCollectUtil.deleteFile(file)));
    }

    /** 删除 systemLogDir 下所有 hprof 文件（上传成功或失败后都会清理）。 */
    private void deleteHprofFile() {
        ArrayList<File> fileList = new ArrayList<>();
        addFileByFilter(fileList, new File(SdCardUtil.getSystemLogDir()), new FileStartsFilter(HPROF_PREFIX));
        if (fileList.isEmpty()) {
            LogUtil.w(TAG, "deleteHprofFile but fileList is empty.");
            return;
        }
        for (File file : fileList) {
            LogCollectUtil.deleteFile(file);
        }
        LogUtil.d(TAG, "deleteHprofFile DEL (" + fileList.size() + ") hprof files");
    }

    /**
     * 删除目录下所有 zip 文件。
     *
     * @param dirPath     目录
     * @param excludeList 排除关键字，为 null 表示全部删除
     */
    private void deleteZipFile(String dirPath, List<String> excludeList) {
        if (TextUtils.isEmpty(dirPath)) {
            return;
        }
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            if (file.getName().endsWith(ZIP_SUFFIX)) {
                deleteFileBySuffix(file, ZIP_SUFFIX, excludeList);
            }
        }
    }

    /**
     * 按策略里的 {@code dates} 过滤目录下的日志文件。
     *
     * <p>{@code dates} 中含 {@code -1} 表示"全部日期"，此时整个目录都要。</p>
     */
    private void filterLogFileByDate(LogStrategy strategy, String dirPath, List<File> out) {
        if (strategy == null || TextUtils.isEmpty(dirPath) || out == null) {
            return;
        }
        List<Long> dates = strategy.getDates();
        LogUtil.d(TAG, "uploadDates: " + dates);
        if (dates == null) {
            return;
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return;
        }
        boolean allDates = false;
        for (Long date : dates) {
            if (date.longValue() == -1) {
                LogUtil.d(TAG, "uploadDate: " + date);
                allDates = true;
                break;
            }
            String dateStr = DateUtils.format("yyyyMMdd", date);
            LogUtil.d(TAG, "dateStr: " + dateStr);
            addFileByFilter(out, dir, new FileContainsFilter(dateStr));
        }
        if (allDates) {
            addFileByFilter(out, dir, null);
        }
    }

    /**
     * 收集第三方应用日志并打包。
     *
     * <p>agora / netease 两个 SDK 的目录整体打包（不按日期过滤），其余按日期过滤。</p>
     */
    private File getAppLogFile(LogStrategy strategy) {
        String[] appPackages = strategy.getAppPackage().split(",");
        List<Long> dates = strategy.getDates();
        if (dates == null) {
            return null;
        }
        ArrayList<File> fileList = new ArrayList<>();

        // 先清掉上一轮遗留的 zip，避免把旧包也打进去。
        File[] appLogFiles = new File(SdCardUtil.getAppLogDir()).listFiles();
        if (appLogFiles != null && appLogFiles.length > 0) {
            for (File file : appLogFiles) {
                if (file.getName().endsWith(ZIP_SUFFIX)) {
                    LogUtil.d(TAG, "HXQ-delete zip file,result:" + LogCollectUtil.deleteFile(file));
                }
            }
        }

        for (String appPackage : appPackages) {
            String oneAppLogDir = SdCardUtil.getOneAppLogDir(appPackage);
            File appLogDir = new File(oneAppLogDir);
            if (this.NAME_AGORA.equals(appPackage)) {
                if (appLogDir.exists()) {
                    LogUtil.i(TAG, "agora log");
                    fileList.add(appLogDir);
                }
            } else if (this.NAME_NETEASE.equals(appPackage)) {
                if (appLogDir.exists()) {
                    LogUtil.i(TAG, "netease log");
                    fileList.add(appLogDir);
                }
            } else {
                for (Long date : dates) {
                    addFileByFilter(fileList, appLogDir, new FileContainsFilter(DateUtils.format("yyyyMMdd", date)));
                }
            }
        }

        if (fileList.isEmpty()) {
            LogUtil.d(TAG, "no appLog file to collection. not to zip");
            return null;
        }
        String zipPath = SdCardUtil.getAppLogDir() + ZIP_PREFIX_SYS_LOG + strategy.getAccountId()
                + "_" + DateUtils.format("yyyyMMddHHmmss") + ZIP_SUFFIX;
        try {
            if (ZipUtils.zipFiles(fileList, zipPath)) {
                return new File(zipPath);
            }
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    /**
     * 收集 dropbox（系统 ANR/crash/tombstone）日志并按天打包。
     *
     * @return 打包后的 zip；没有可收集内容时返回 null
     */
    private File getDropboxFile(LogStrategy strategy) {
        LogUtil.d(TAG, "get dropbox file...");
        List<Long> dates = strategy.getDates();
        if (dates == null) {
            return null;
        }

        File dropboxDir = new File(SdCardUtil.getDropboxLogDir());
        File[] existing = dropboxDir.listFiles();
        if (dropboxDir.exists() && existing != null && existing.length > 0) {
            for (File file : existing) {
                if (file.getName().endsWith(ZIP_SUFFIX)) {
                    LogUtil.d(TAG, "delete dropbox zip file,result:" + LogCollectUtil.deleteFile(file));
                }
            }
        }

        ArrayList<File> dayDirs = new ArrayList<>();
        for (Long date : dates) {
            File dayDir = new File(SdCardUtil.getDropboxOneDayDir(DateUtils.format("yyyyMMdd", date)));
            if (dayDir.exists()) {
                File[] children = dayDir.listFiles();
                if (children != null && children.length != 0) {
                    dayDirs.add(dayDir);
                }
            }
        }
        LogUtil.i(TAG, "collect dropbox files = " + dayDirs);
        if (CollectionUtils.isEmpty(dayDirs)) {
            return null;
        }

        String dropboxZipDir = SdCardUtil.getDropboxZipDir();
        try {
            return ZipUtils.zipFiles(dayDirs, dropboxZipDir) ? new File(dropboxZipDir) : null;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /**
     * 删除 zip 时需要排除的文件名关键字。
     *
     * <p>hprof 是崩溃现场，上传后才由 {@link #deleteHprofFile()} 清理，
     * 不能在这里被当成旧 zip 删掉。</p>
     */
    private List<String> getExcludeFileStrList() {
        ArrayList<String> list = new ArrayList<>();
        list.add(HPROF_PREFIX);
        return list;
    }

    /**
     * 收集某个系统模块（例如 MCU dump）目录下的日志并打包。
     *
     * @param strategy 策略
     * @param dirPath  模块日志目录
     */
    private File getSysModuleLog(LogStrategy strategy, String dirPath) {
        if (TextUtils.isEmpty(dirPath)) {
            return null;
        }
        ArrayList<File> fileList = new ArrayList<>();
        filterLogFileByDate(strategy, dirPath, fileList);
        if (fileList.isEmpty()) {
            LogUtil.d(TAG, "no sys module files. logDirPath is: " + dirPath);
            return null;
        }

        String appPackage = strategy.getAppPackage();
        String outZipDir = SdCardUtil.getSdCardLogDir(appPackage);
        deleteZipFile(outZipDir, null);
        LogUtil.d(TAG, "collect file from sys module: " + appPackage
                + ", logDirPath:" + dirPath + " ,outZipDir:" + outZipDir);

        for (File file : fileList) {
            LogUtil.d(TAG, "file to zip: " + file.getAbsolutePath() + " ,listSize:" + fileList.size());
        }

        File outDir = new File(outZipDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String zipPath = outZipDir + ZIP_PREFIX_SYS_LOG + strategy.getAccountId()
                + "_" + DateUtils.format("yyyyMMddHHmmss") + ZIP_SUFFIX;
        try {
            if (ZipUtils.zipFiles(fileList, zipPath)) {
                return new File(zipPath);
            }
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    /**
     * 收集系统日志（可选带上 hprof）并打包。
     *
     * @param strategy    策略
     * @param withHprof   true 表示额外打包 hprof 文件
     */
    private File getSystemLogFile(LogStrategy strategy, boolean withHprof) {
        List<Long> dates = strategy.getDates();
        LogUtil.d(TAG, "uploadDates: " + dates);
        if (dates == null) {
            return null;
        }
        ArrayList<File> fileList = new ArrayList<>();

        String systemLogDir = SdCardUtil.getSystemLogDir();
        File systemLogDirFile = new File(systemLogDir);
        if (!systemLogDirFile.exists()) {
            LogUtil.d(TAG, "collect systemLogDir not exist. ");
            return null;
        }
        LogUtil.d(TAG, "collect systemLogDir: " + systemLogDir);

        String outZipDir = SdCardUtil.getSdCardLogDir();
        deleteZipFile(outZipDir, getExcludeFileStrList());

        boolean allDates = false;
        for (Long date : dates) {
            if (date.longValue() == -1) {
                LogUtil.d(TAG, "uploadDate: " + date);
                allDates = true;
                break;
            }
            String dateStr = DateUtils.format("yyyyMMdd", date);
            LogUtil.d(TAG, "dateStr: " + dateStr);
            addFileByFilter(fileList, systemLogDirFile, new FileContainsFilter(dateStr));
        }
        if (allDates) {
            addFileByFilter(fileList, systemLogDirFile, null);
        }

        File dropboxFile = getDropboxFile(strategy);
        if (dropboxFile != null) {
            fileList.add(dropboxFile);
        }
        if (withHprof) {
            addFileByFilter(fileList, systemLogDirFile, new FileStartsFilter(HPROF_PREFIX));
        }

        if (fileList.isEmpty()) {
            LogUtil.d(TAG, "no systemLog file to collection. not to zip");
            return null;
        }
        for (File file : fileList) {
            LogUtil.d(TAG, "file to zip: " + file.getAbsolutePath() + " ,listSize:" + fileList.size());
        }

        File outDir = new File(outZipDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String zipPath = outZipDir + ZIP_PREFIX_SYS_LOG + strategy.getAccountId()
                + "_" + DateUtils.format("yyyyMMddHHmmss") + ZIP_SUFFIX;
        try {
            if (ZipUtils.zipFiles(fileList, zipPath)) {
                return new File(zipPath);
            }
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    /**
     * 根据策略里的包名分派到对应的收集流程。
     *
     * <ul>
     *   <li>多包名 → 第三方应用日志</li>
     *   <li>{@code com.xtc.i11system} → 系统日志</li>
     *   <li>{@code com.xtc.hprof} → 系统日志 + hprof</li>
     *   <li>{@code com.xtc.mcudump} → MCU dump 目录</li>
     *   <li>其余 → 第三方应用日志</li>
     * </ul>
     */
    private File getUploadFile(LogStrategy strategy) {
        if (strategy.getAppPackage().split(",").length != 1) {
            return getAppLogFile(strategy);
        }
        String appPackage = strategy.getAppPackage();
        if (appPackage.equals(SYSTEM_PACKAGE_NAME)) {
            return getSystemLogFile(strategy, false);
        }
        if (appPackage.equals(SYSTEM_HPROF_PACKAGE_NAME)) {
            return getSystemLogFile(strategy, true);
        }
        if (appPackage.equals(Constants.LogStrategyPackageName.PACKAGE_NAME_MCU_DUMP)) {
            return getSysModuleLog(strategy, SdCardUtil.getMcuDumpFilePath());
        }
        return getAppLogFile(strategy);
    }

    /** 把 {@code path} 指向的文件/目录整体压成同目录下的 {@code yyyyMMddHHmmss.zip}。 */
    private File getZipFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File target = new File(path);
        if (!target.exists()) {
            return null;
        }
        String zipPath = target.getParent() + File.separator + DateUtils.format("yyyyMMddHHmmss") + ZIP_SUFFIX;
        if (TextUtils.isEmpty(zipPath)) {
            return null;
        }
        try {
            return ZipUtils.zipFile(target.getAbsolutePath(), zipPath) ? new File(zipPath) : null;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 上传结果回通知
    // ------------------------------------------------------------------

    /**
     * 通知服务端"采集失败"。
     *
     * @param appPackage 包名
     * @param remark     失败原因
     */
    private void notifyErrorInfo(String appPackage, String remark) {
        String accountId = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(accountId)) {
            LogCollectUtil.getInstance().releaseWakeLock();
            return;
        }
        UploadNotifyParam param = new UploadNotifyParam();
        param.setAccountId(accountId);
        param.setKey("");
        param.setAppPackage(appPackage);
        param.setMachineId(SharedTool.getMachineId(this.context));
        param.setId(this.id);
        param.setStrategyId(this.strategyId);
        param.setOperatorId(this.operatorId);
        param.setRemark(remark);
        param.setCollectStatus(2);
        LogUtil.d(TAG, "HXQ-uploadNotifyParam：" + param.toString());
        notifyUploadState(param);
    }

    /**
     * 通知服务端"上传成功"。
     *
     * @param appPackage 包名
     * @param key        七牛文件 key
     * @param spaceType  存储空间类型
     */
    private void notifySuccessInfo(String appPackage, String key, int spaceType) {
        String accountId = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        if (TextUtils.isEmpty(accountId)) {
            LogCollectUtil.getInstance().releaseWakeLock();
            return;
        }
        UploadNotifyParam param = new UploadNotifyParam();
        param.setAccountId(accountId);
        param.setKey(key);
        param.setSpaceType(spaceType);
        param.setAppPackage(appPackage);
        param.setId(this.id);
        param.setStrategyId(this.strategyId);
        param.setOperatorId(this.operatorId);
        param.setMachineId(SharedTool.getMachineId(this.context));
        param.setCollectStatus(1);
        notifyUploadState(param);
    }

    /** 提交上传状态；无论成败都会恢复网络并释放唤醒锁。 */
    private void notifyUploadState(UploadNotifyParam param) {
        this.logHttpProxy.notifyUploadSuccess(param)
                .observeOn(Schedulers.io())
                .subscribe(new HttpSubscriber<Object>() {
                    @Override
                    public void onHttpError(Throwable th) {
                        super.onHttpError(th);
                        LogUtil.e(TAG, "HXQ--notify fail:" + th.getMessage());
                        resumeOriginNetwork();
                        LogCollectUtil.getInstance().releaseWakeLock();
                    }

                    @Override
                    public void onNext(Object obj) {
                        super.onNext(obj);
                        LogUtil.i(TAG, "HXQ-已通知服务器上传状态");
                        resumeOriginNetwork();
                        LogCollectUtil.getInstance().releaseWakeLock();
                    }
                });
    }

    /** 从 {@code Folder_yyyyMMdd} 形式的目录名里取出日期串。 */
    @Nullable
    private String reLogFolderTime(String name) {
        Matcher matcher = Pattern.compile("Folder_(\\d{8})").matcher(name);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 如果为了上传改过网络模式，延迟 5s 后恢复。
     *
     * <p>延迟是为了让服务端通知请求先发完，避免刚通知完就被断网。</p>
     */
    private void resumeOriginNetwork() {
        if (!this.isNetworkModify) {
            return;
        }
        Observable.timer(5000L, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe(new BaseSubscriber<Long>() {
                    @Override
                    public void onError(Throwable th) {
                        super.onError(th);
                    }

                    @Override
                    public void onNext(Long value) {
                        super.onNext(value);
                        LogCollectUtil.resumeMobileNetwork(LogCollectServiceImpl.this.context);
                        LogCollectServiceImpl.this.isNetworkModify = false;
                    }
                });
    }

    /**
     * 上传文件到七牛。
     *
     * <p>先申请 token，成功后才真正上传；任何一步失败都会删包、清 hprof、
     * 通知服务端并释放唤醒锁。</p>
     *
     * @param appPackage 包名，可为 null（{@link #uploadFile(String)} 场景）
     * @param file       待上传的 zip
     */
    private void startQiniuUpload(final String appPackage, final File file) {
        LogUtil.i(TAG, "HXQ-start upload file to qiniu appPackage:" + appPackage + " ,uploadFile:" + file);
        if (file == null) {
            return;
        }
        final String key = file.getName();
        TokenManager.getUploadToken(this.context, 1, key, new TokenManager.OnUpLoadTokenListener() {
            @Override
            public void onFailure(String message) {
                LogUtil.i(TAG, "HXQ--get upload token fail:" + message);
                boolean deleted = LogCollectUtil.deleteFile(file);
                deleteHprofFile();
                notifyErrorInfo(appPackage, "上传失败:" + message);
                LogUtil.d(TAG, "HXQ-delete zip file,result:" + deleted);
                LogCollectUtil.getInstance().releaseWakeLock();
            }

            @Override
            public void onSuccess(String token) {
                LogUtil.i(TAG, "HXQ-token value = " + token);
                NetUploadToken netUploadToken = new NetUploadToken();
                netUploadToken.setToken(token);
                netUploadToken.setTimestamp(Long.valueOf(System.currentTimeMillis()));
                com.xtc.qiniu.SharedTool.saveUploadToken(LogCollectServiceImpl.this.context, 1,
                        JSONUtil.toJSON(netUploadToken));

                ICloudService.upLoadFile(LogCollectServiceImpl.this.context, 1, key, file,
                        new ICloudManager.OnUpLoadListener() {
                            @Override
                            public void onFailure(String key, int code, String message) {
                                LogUtil.e(TAG, "HXQ--upload file fail," + code + ":" + message);
                                boolean deleted = LogCollectUtil.deleteFile(file);
                                deleteHprofFile();
                                LogUtil.d(TAG, "HXQ-delete zip file,result:" + deleted);
                                notifyErrorInfo(appPackage,
                                        LogCollectServiceImpl.this.context.getString(R.string.qiniu_upload_error_remark)
                                                + code + "(" + message + ")");
                                LogCollectUtil.getInstance().releaseWakeLock();
                            }

                            @Override
                            public void onProgress(String key, double progress) {
                                LogUtil.d(TAG, "HXQ-log upload progress:" + progress);
                            }

                            @Override
                            public void onSuccess(String key) {
                                LogUtil.i(TAG, "HXQ--upload success,the key is " + key);
                                boolean deleted = LogCollectUtil.deleteFile(file);
                                deleteHprofFile();
                                LogUtil.d(TAG, "HXQ-delete zip file,result:" + deleted);
                                notifySuccessInfo(appPackage, key, 1);
                                LogCollectUtil.getInstance().releaseWakeLock();
                            }
                        });
            }
        });
    }

    // ------------------------------------------------------------------
    // ILogCollectService
    // ------------------------------------------------------------------

    @Override
    public void initLogCollectStrategy(String accountId) {
        LogStrategyParam param = new LogStrategyParam();
        param.setAccountId(accountId);
        this.logHttpProxy.getCollectStrategy(param)
                .observeOn(Schedulers.io())
                .subscribe(new HttpSubscriber<List<LogStrategy>>() {
                    @Override
                    public void onHttpError(Throwable th) {
                        super.onHttpError(th);
                        LogUtil.e(TAG, "HXQ-get strategy fail = " + th.getMessage());
                    }

                    @Override
                    public void onNext(List<LogStrategy> strategies) {
                        super.onNext(strategies);
                        LogUtil.i(TAG, "HXQ-获取日志策略成功 list =" + strategies);
                        if (CollectionUtils.isEmpty(strategies)) {
                            LogUtil.w(TAG, "logStrategies = null , return !");
                            return;
                        }
                        saveLogStrategies(strategies);
                    }
                });
    }

    /**
     * 把策略写进 {@code log.properties}。
     *
     * <p>包名为空时写默认的 {@code enableStatus}/{@code logLevel}；
     * 系统包（i11system）额外通过 {@code persist.service.xtc_hacker} 控制
     * 打开/关闭哪些 logd buffer。</p>
     */
    @Override
    public void saveLogStrategies(List<LogStrategy> strategies) {
        Properties loaded = PropertiesUtil.loadConfig();
        Properties properties = loaded == null ? new Properties() : loaded;
        for (LogStrategy strategy : strategies) {
            String appPackage = strategy.getAppPackage();
            String enableStatus = String.valueOf(strategy.getEnableStatus());
            String logLevel = String.valueOf(strategy.getLogLevel());
            if (TextUtils.isEmpty(appPackage)) {
                properties.put("enableStatus", enableStatus);
                properties.put("logLevel", logLevel);
            } else if (!appPackage.equals(SYSTEM_PACKAGE_NAME)) {
                properties.put(appPackage + "_enableStatus", enableStatus);
                properties.put(appPackage + "_logLevel", logLevel);
            } else if (strategy.getEnableStatus().intValue() == 1) {
                SystemPropertiesUtils.set(SystemConstants.PROP_XTC_HACKER,
                        "enable,kernel,crash,main,system,events,radio,collect");
            } else {
                SystemPropertiesUtils.set(SystemConstants.PROP_XTC_HACKER, "enable,kernel,crash");
            }
        }
        String updateTime = String.valueOf(System.currentTimeMillis());
        properties.put("updateTime", updateTime);
        PropertiesUtil.saveConfig(properties);
        LogUtil.i(TAG, "saveLogStrategies done , updateTime = " + updateTime);
    }

    /**
     * 执行一次完整的"收集 → 上传"。
     *
     * <p>前置检查顺序与原厂一致：emmc 寿命 → 日志系统开关 → 网络条件 →
     * 文件是否存在 → 文件大小是否超限 → 网络是否可用。</p>
     */
    @Override
    public void startUploadLog(List<LogStrategy> strategies) {
        LogCollectUtil.getInstance().acquireWakeLockForTenMinutes(this.context);
        LogStrategy strategy = strategies.get(0);

        // 1. emmc 寿命耗尽后系统会置位该属性，此时禁止采集。
        int splogOff = SystemPropertiesUtils.getInt("log.tag.stopsplog", -1);
        if (splogOff == 1) {
            LogUtil.i(TAG, "startUploadLog done , log.tag.stopsplog=" + splogOff);
            LogStateServeImpl.getInstance(this.context).notifyErrorInfo(strategy,
                    this.context.getString(R.string.emmc_for_splog_off));
            LogCollectUtil.getInstance().releaseWakeLock();
            return;
        }

        // 2. 日志系统被整体关闭。
        int logDisabled = SystemPropertiesUtils.getInt("persist.sys.superlog.disable", -1);
        if (logDisabled == 1) {
            LogUtil.i(TAG, "startUploadLog done , persist.sys.superlog.disable=" + logDisabled);
            LogStateServeImpl.getInstance(this.context).notifyErrorInfo(strategy,
                    this.context.getString(R.string.log_off));
            LogCollectUtil.getInstance().releaseWakeLock();
            return;
        }

        // 3. 策略要求仅 WiFi 上传。
        int uploadNet = strategy.getUploadNet();
        NetworkUtils.NetworkType networkType = NetworkUtils.getNetworkType(this.context);
        if (uploadNet == UPLOAD_NET_WIFI && networkType != NetworkUtils.NetworkType.NETWORK_WIFI) {
            LogUtil.i(TAG, "startUploadLog done , uploadNet= " + uploadNet + " ,networkType= " + networkType);
            LogStateServeImpl.getInstance(this.context).notifyErrorInfo(strategy,
                    this.context.getString(R.string.collect_error_info_network_not_match));
            LogCollectUtil.getInstance().releaseWakeLock();
            return;
        }

        this.id = strategy.getLogId();
        this.strategyId = strategy.getId();
        this.operatorId = strategy.getUserId();
        LogUtil.i(TAG, "startUploadLog, id = " + this.id);

        long fileLimitKb = strategy.getFileLimit().intValue();
        File uploadFile = getUploadFile(strategy);
        String appPackage = strategy.getAppPackage();

        // 4. 没有可上传的日志。
        if (uploadFile == null || !uploadFile.exists()) {
            notifyErrorInfo(appPackage, this.context.getString(R.string.no_log_file));
            return;
        }

        // 5. 文件大小超限（单位：KB）。
        long fileSizeKb = FileUtils.getFileSize(uploadFile.getAbsolutePath())
                / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if (fileSizeKb > fileLimitKb) {
            LogUtil.d(TAG, "HXQ-delete zip file,result:" + LogCollectUtil.deleteFile(uploadFile));
            notifyErrorInfo(appPackage, String.format(Locale.getDefault(),
                    this.context.getString(R.string.upload_remark), Long.valueOf(fileSizeKb)));
            return;
        }

        // 6. 当前已有 WiFi 或 LTE，直接上传。
        if (NetworkUtils.isWifiConnected(this.context) || NetworkUtils.isLteAvailable(this.context)) {
            startQiniuUpload(appPackage, uploadFile);
            return;
        }

        // 7. 否则尝试切到 4G，15s 后再上传。
        if (LogCollectUtil.forceMobileNetworkTo4G(this.context)) {
            this.isNetworkModify = true;
            final File fileToUpload = uploadFile;
            final String targetPackage = appPackage;
            Observable.timer(15000L, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.io())
                    .subscribe(new BaseSubscriber<Long>() {
                        @Override
                        public void onError(Throwable th) {
                            LogCollectUtil.getInstance().releaseWakeLock();
                        }

                        @Override
                        public void onNext(Long value) {
                            startQiniuUpload(targetPackage, fileToUpload);
                        }
                    });
        } else {
            LogUtil.e(TAG, "startUploadLog but network not ok.");
            LogStateServeImpl.getInstance(this.context).notifyErrorInfo(strategy,
                    this.context.getString(R.string.collect_error_info_network_not_match));
            LogCollectUtil.getInstance().releaseWakeLock();
        }
    }

    /**
     * 直接上传指定路径（内部会先压成 zip）。
     *
     * @param path 待上传的文件或目录
     */
    @Override
    public void uploadFile(final String path) {
        LogCollectUtil.getInstance().acquireWakeLockForTenMinutes(this.context);
        Observable.just(path)
                .observeOn(Schedulers.io())
                .subscribe(new BaseSubscriber<String>() {
                    @Override
                    public void onNext(String value) {
                        super.onNext(value);
                        final File zipFile = getZipFile(path);
                        if (zipFile == null || !zipFile.exists()) {
                            return;
                        }
                        if (NetworkUtils.isWifiConnected(LogCollectServiceImpl.this.context)
                                || NetworkUtils.isLteAvailable(LogCollectServiceImpl.this.context)) {
                            startQiniuUpload(null, zipFile);
                        } else if (LogCollectUtil.forceMobileNetworkTo4G(LogCollectServiceImpl.this.context)) {
                            LogCollectServiceImpl.this.isNetworkModify = true;
                            Observable.timer(15000L, TimeUnit.MILLISECONDS)
                                    .observeOn(Schedulers.io())
                                    .subscribe(new BaseSubscriber<Long>() {
                                        @Override
                                        public void onError(Throwable th) {
                                            LogCollectUtil.getInstance().releaseWakeLock();
                                        }

                                        @Override
                                        public void onNext(Long value) {
                                            startQiniuUpload(null, zipFile);
                                        }
                                    });
                        }
                    }
                });
    }
}
