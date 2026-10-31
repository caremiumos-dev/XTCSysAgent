package com.xtc.utils.storage;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.widget.Toast;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.DeviceUtils;
import java.io.File;

/**
 * 应用私有目录的统一管理者（单例）。
 *
 * <p>原厂把所有落盘目录都收敛成 {@code <SD卡>/xtc/<应用名>/<模块名>/} 这一套约定，
 * 本类就是这套约定的唯一出口：外部代码不再自己拼路径，而是
 * {@link #getInstance()} 之后调 {@code init(appName, moduleName)} 再取目录。</p>
 *
 * <p>几个必须注意的原厂特性：</p>
 * <ul>
 *   <li>SD 卡不可用时，所有"取目录"的方法返回硬编码的 {@code /mnt/sdcard/...} 兜底路径，
 *       调用方拿到的是一个可能不存在的路径；</li>
 *   <li>首次取目录会顺带做一次剩余空间检查（低于 10MB 弹 Toast），
 *       因为 {@link #getWorkDir()} / {@link #getCommonDir()} 里对
 *       {@link #sSdCardPath} 为空的分支调用了 {@link #checkSdCardSpace()}；</li>
 *   <li>应用名/模块名可以为 null，拼出来会出现 "null" 段——原厂不做任何校验，照旧。</li>
 * </ul>
 */
public class FolderManager {

    /** 日志 tag。 */
    private static final String TAG = "FolderManager";

    /** SD 卡根路径缓存，初始值是硬编码的兜底值（老机型路径）。 */
    private static String sSdCardPath = "/mnt/sdcard";

    /** 应用数据的顶层目录名。 */
    private static final String DIR_XTC = "xtc";

    /** 恢复出厂设置时的备份目录名。 */
    private static final String DIR_FACTORY_RESET = "factoryreset";

    /** SD 卡剩余空间告警阈值（单位 MB）。 */
    private static final int MIN_SDCARD_SPACE_MB = 10;

    /** 双重检查锁单例。 */
    private static volatile FolderManager sInstance;

    /** 应用名（例如 ibwatch），拼在 xtc 之后。 */
    private String mAppName;

    /** 模块名（例如 SystemAgent），拼在应用名之后。 */
    private String mModuleName;

    /** 用于弹 Toast 的 Context，由 init 注入。 */
    private Context mContext;

    private FolderManager() {
    }

    /** 取单例。 */
    public static FolderManager getInstance() {
        FolderManager instance = sInstance;
        if (instance == null) {
            synchronized (FolderManager.class) {
                instance = sInstance;
                if (instance == null) {
                    instance = new FolderManager();
                    sInstance = instance;
                }
            }
        }
        return instance;
    }

    /**
     * 系统日志收集目录。
     *
     * <p>依据 {@link DeviceUtils#isDomestic()}（是否"国内/量产"机型）决定放 SD 卡下的 Logs_Collector
     * 还是 data 分区的 slog；两处都以分隔符结尾。</p>
     */
    public static String getSystemLogDir() {
        return DeviceUtils.isDomestic()
                ? Environment.getExternalStorageDirectory().getPath() + File.separator + "Logs_Collector" + File.separator
                : Environment.getDataDirectory() + File.separator + "slog" + File.separator;
    }

    /**
     * 恢复出厂设置时老固件留下的备份目录 {@code <SD卡>/xtc/factoryreset/}。
     *
     * <p>SD 卡不可用或目录创建失败时，退回硬编码的 {@code /mnt/sdcard} 前缀，
     * 保证返回值始终以分隔符结尾、始终可拼接。</p>
     */
    private String getOldQualFactoryResetBackPath() {
        StringBuilder builder = new StringBuilder();
        builder.append(File.separator).append(DIR_XTC).append(File.separator).append(DIR_FACTORY_RESET).append(File.separator);
        String suffix = builder.toString();
        if (!Environment.getExternalStorageState().equals("mounted")) {
            LogUtil.e(TAG, "getOldQualFactoryResetBackPath sdcard not mounted");
            return sSdCardPath + suffix;
        }
        String path = Environment.getExternalStorageDirectory() + suffix;
        if (FileUtils.createOrExistsDir(path)) {
            return path;
        }
        LogUtil.e(TAG, "getOldQualFactoryResetBackPath mkdirs failed");
        return sSdCardPath + suffix;
    }

    /**
     * SD 卡剩余空间（MB）。
     *
     * <p>先把"块数 × 块大小"算成字节，再连续除两次 1024
     * （原厂借用了 {@link PlaybackStateCompat#ACTION_PLAY_FROM_MEDIA_ID} 这个 1024 常量）。</p>
     */
    private long getSdCardAvailableMb() {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return ((((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize()))
                / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
    }

    /** 初始化：注入 Context、应用名、模块名。返回单例便于链式调用。 */
    public FolderManager init(Context context, String appName, String moduleName) {
        this.mContext = context;
        this.mAppName = appName;
        this.mModuleName = moduleName;
        return getInstance();
    }

    /** 初始化：只设置应用名与模块名（Context 由调用方在别处设置）。 */
    public FolderManager init(String appName, String moduleName) {
        this.mAppName = appName;
        this.mModuleName = moduleName;
        return getInstance();
    }

    /** 在工作目录下创建一级子目录。 */
    public void makeModuleDir(String dirName) {
        FileUtils.createOrExistsDir(getWorkDir() + dirName + "/");
    }

    /**
     * 批量创建子目录。
     *
     * <p>入参里的空串/纯空白项会被跳过，其余按 {@code a/b/c/} 的方式拼接后一次性创建。</p>
     */
    public void makeModuleDirs(String[] dirNames) {
        if (dirNames == null || dirNames.length <= 0) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String dirName : dirNames) {
            if (dirName.trim().length() > 0) {
                builder.append(dirName).append("/");
            }
        }
        FileUtils.createOrExistsDir(getWorkDir() + builder.toString());
    }

    /**
     * 当前工作目录 {@code <SD卡>/xtc/<应用名>/<模块名>/}。
     *
     * <p>SD 卡不可用、或目录创建失败时返回 {@code /mnt/sdcard/xtc/ibwatch/launcher/}。</p>
     */
    public String getWorkDir() {
        StringBuilder builder = new StringBuilder();
        if (!Environment.getExternalStorageState().equals("mounted")) {
            // 原厂这里调的是"只带消息"的 LogUtil.e 重载，而该重载是空实现，实际不输出日志。
            LogUtil.e("内存卡不可用");
            return "/mnt/sdcard/xtc/ibwatch/launcher/";
        }
        if (TextUtils.isEmpty(sSdCardPath)) {
            checkSdCardSpace();
            sSdCardPath = Environment.getExternalStorageDirectory().getPath();
        }
        builder.append(sSdCardPath).append(File.separator).append(DIR_XTC).append(File.separator)
                .append(this.mAppName).append(File.separator).append(this.mModuleName).append(File.separator);
        return FileUtils.createOrExistsDir(builder.toString()) ? builder.toString() : "/mnt/sdcard/xtc/ibwatch/launcher/";
    }

    /** 在公共目录下创建一级子目录。 */
    public void makeCommonSubDir(String dirName) {
        FileUtils.createOrExistsDir(getCommonDir() + dirName + "/");
    }

    /** 在工作目录下创建两级子目录。 */
    public void makeWorkSubDir(String dirName, String subDirName) {
        FileUtils.createOrExistsDir(getWorkDir() + dirName + "/" + subDirName + "/");
    }

    /**
     * 公共目录 {@code <SD卡>/xtc/<应用名>/common/}。
     *
     * <p>SD 卡不可用或创建失败时返回 {@code /mnt/sdcard/xtc/ibwatch/common/}。</p>
     */
    public String getCommonDir() {
        StringBuilder builder = new StringBuilder();
        if (!Environment.getExternalStorageState().equals("mounted")) {
            LogUtil.e(TAG, "内存卡不可用");
            return "/mnt/sdcard/xtc/ibwatch/common/";
        }
        if (TextUtils.isEmpty(sSdCardPath)) {
            checkSdCardSpace();
            sSdCardPath = Environment.getExternalStorageDirectory().getPath();
        }
        builder.append(sSdCardPath).append(File.separator).append(DIR_XTC).append(File.separator)
                .append(this.mAppName).append(File.separator).append(StorageConstants.DIR_COMMON).append(File.separator);
        return FileUtils.createOrExistsDir(builder.toString()) ? builder.toString() : "/mnt/sdcard/xtc/ibwatch/common/";
    }

    /** 在工作目录下创建并返回子目录路径；创建失败返回空串（而不是抛异常）。 */
    public String makeWorkDirChild(String childName) {
        String path = getWorkDir() + childName;
        return FileUtils.createOrExistsDir(path) ? path : "";
    }

    /** 在指定的父目录下创建子目录。 */
    public void makeDirIn(String parentPath, String childName) {
        FileUtils.createOrExistsDir(new File(parentPath, childName));
    }

    /** "上一次生效的配置"文件路径（配置目录 + last_AppConfig）。 */
    public String getLastAppConfigPath() {
        return getConfigFilePath() + "last_AppConfig";
    }

    /** 在公共目录下创建并返回子目录路径；失败返回空串。 */
    public String makeCommonDirChild(String childName) {
        String path = getCommonDir() + childName;
        return FileUtils.createOrExistsDir(path) ? path : "";
    }

    /**
     * 在工作目录下创建并返回路径；失败返回空串。
     *
     * <p>注意第一个参数 {@code parentName} 在原厂实现里完全没有被使用
     * （历史遗留：早期版本会拼上父目录名），这里保留签名与行为不变。</p>
     */
    public String makeWorkDirPath(String parentName, String childName) {
        String path = getWorkDir() + childName;
        return FileUtils.createOrExistsDir(path) ? path : "";
    }

    /**
     * 普通应用读取配置文件的完整路径。
     *
     * <p>是否走公共目录由 {@link DeviceUtils#isOversea()} 决定：真机量产走 {@code common/AppConfig}，
     * 否则走恢复出厂备份目录下的 {@code AppConfig}。</p>
     */
    public String getConfigFilePathForNormalApp() {
        String path = DeviceUtils.isOversea()
                ? getCommonDir() + StorageConstants.DIR_APP_CONFIG
                : getOldQualFactoryResetBackPath() + StorageConstants.DIR_APP_CONFIG;
        LogUtil.d(TAG, "getConfigFilePathForNormalApp = " + path);
        return path;
    }

    /**
     * 检查 SD 卡剩余空间，不足 10MB 时在子线程里弹 Toast 并常驻消息循环。
     *
     * <p>为什么要开线程 + {@code Looper.prepare()/loop()}：原厂在非 UI 线程
     * （Binder/初始化线程）里直接弹 Toast，只能自己起一个带 Looper 的线程，
     * 并且必须 {@code Looper.loop()} 常驻，否则 Toast 还没显示线程就退出了。</p>
     *
     * @return true 表示空间不足（已弹提示）；false 表示空间充足
     */
    public boolean checkSdCardSpace() {
        if (getSdCardAvailableMb() >= MIN_SDCARD_SPACE_MB) {
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                // 1 即 Toast.LENGTH_LONG，原厂写的是字面量
                Toast.makeText(FolderManager.this.mContext, "SD卡存储空间不足", 1).show();
                Looper.loop();
            }
        }).start();
        return true;
    }

    /**
     * 配置文件目录 {@code /cache/recovery/}。
     *
     * <p>用绝对路径而不用 context.getCacheDir()，是为了让"恢复出厂/升级"流程
     * 在不启动应用的情况下也能读到这份配置。</p>
     */
    public String getConfigFilePath() {
        String path = File.separator + "cache" + File.separator + "recovery" + File.separator;
        if (FileUtils.createOrExistsDir(path)) {
            LogUtil.d(TAG, "getConfigFilePath dir exists");
        } else {
            LogUtil.e(TAG, "getConfigFilePath mkdirs failed");
        }
        return path;
    }
}
