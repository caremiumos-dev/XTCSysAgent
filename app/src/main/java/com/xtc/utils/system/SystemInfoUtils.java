package com.xtc.utils.system;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 应用（包）信息与安装 / 卸载 / 清数据入口。
 *
 * <p>原厂类名为单字母 {@code a}，内部还有一个被重命名为 {@code a.C0024a} 的静态嵌套类，
 * 这里还原为 {@link AppInfo}：一条"应用档案"（包名、名称、图标、安装路径、版本名、
 * 版本码、是否系统应用）。</p>
 *
 * <p>别和子包 {@code com.xtc.utils.system.constant}（原 {@code com.xtc.utils.system.a.a/b/c/d}）
 * 搞混 —— 那里放的是常量，这里的嵌套类是本文件私有的数据模型。</p>
 *
 * <p>几点与原件保持一致的约定：</p>
 * <ul>
 *   <li>"装 / 卸载"有两条路：走 Intent 让包安装器处理（{@code installApp} /
 *       {@code uninstallApp}），或者直接 {@code pm install / pm uninstall}
 *       （{@code installAppByShell} / {@code uninstallAppByShell}），两者命令串里的
 *       {@code LD_LIBRARY_PATH} 前缀不能少；</li>
 *   <li>shell 方式是否用 root，取决于"自己是不是系统应用"（系统应用直接用 sh 即可）；</li>
 *   <li>是否成功只看 stdout 里有没有 {@code success}（忽略大小写），不看退出码；</li>
 *   <li>"清应用数据"用非短路的 {@code &} 串起 5 个清理动作，保证每个动作都被执行。</li>
 * </ul>
 */
public class SystemInfoUtils {

    /**
     * 一条应用档案。原厂为 {@code a.C0024a}，字段是 {@code a}..{@code g}。
     *
     * <p>{@link #toString()} 的输出格式被原厂用于日志排查，标签与换行位置原样保留。</p>
     */
    public static class AppInfo {

        /** 应用名称（{@code ApplicationInfo.loadLabel} 的结果）。 */
        private String appName;

        /** 应用图标。 */
        private Drawable icon;

        /** 包名。 */
        private String packageName;

        /** 安装包路径（{@code sourceDir}，即 APK 路径）。 */
        private String sourceDir;

        /** 版本名（{@code versionName}）。 */
        private String versionName;

        /** 版本码（{@code versionCode}）。 */
        private int versionCode;

        /** 是否系统应用（{@code ApplicationInfo.FLAG_SYSTEM}）。 */
        private boolean systemApp;

        /**
         * 构造顺序与原厂一致：名称 → 图标 → 包名 → 路径 → 版本名 → 版本码 → 是否系统应用。
         */
        public AppInfo(String packageName, String appName, Drawable icon, String sourceDir,
                       String versionName, int versionCode, boolean systemApp) {
            setAppName(appName);
            setIcon(icon);
            setPackageName(packageName);
            setSourceDir(sourceDir);
            setVersionName(versionName);
            setVersionCode(versionCode);
            setSystemApp(systemApp);
        }

        public Drawable getIcon() {
            return this.icon;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public void setSystemApp(boolean systemApp) {
            this.systemApp = systemApp;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public boolean isSystemApp() {
            return this.systemApp;
        }

        public String getAppName() {
            return this.appName;
        }

        public void setSourceDir(String sourceDir) {
            this.sourceDir = sourceDir;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public String getSourceDir() {
            return this.sourceDir;
        }

        public int getVersionCode() {
            return this.versionCode;
        }

        public String getVersionName() {
            return this.versionName;
        }

        @Override
        public String toString() {
            return "App包名：" + getPackageName() + "\nApp名称：" + getAppName()
                    + "\nApp图标：" + getIcon() + "\nApp路径：" + getSourceDir()
                    + "\nApp版本号：" + getVersionName() + "\nApp版本码：" + getVersionCode()
                    + "\n是否系统App：" + isSystemApp() + "\n";
        }
    }

    private SystemInfoUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 由 {@link PackageInfo} 组装一条 {@link AppInfo}；任一入参为 null 时返回 null。
     *
     * <p>{@code (flags & 1) != 0} 就是 {@code ApplicationInfo.FLAG_SYSTEM} 的判断，
     * 原件直接写 1，这里保持同一数值。</p>
     */
    private static AppInfo createAppInfo(PackageManager packageManager, PackageInfo packageInfo) {
        if (packageManager == null || packageInfo == null) {
            return null;
        }
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        return new AppInfo(
                packageInfo.packageName,
                applicationInfo.loadLabel(packageManager).toString(),
                applicationInfo.loadIcon(packageManager),
                applicationInfo.sourceDir,
                packageInfo.versionName,
                packageInfo.versionCode,
                (applicationInfo.flags & 1) != 0);
    }

    /** 当前应用包名。 */
    public static String getPackageName(Context context) {
        return context.getPackageName();
    }

    /** 安装 APK：用 VIEW Intent 交给包安装器，带 requestCode 以便拿回调。 */
    public static void installApp(Activity activity, File apkFile, int requestCode) {
        if (FileUtils.isFileExists(apkFile)) {
            activity.startActivityForResult(IntentUtils.getOpenFileIntent(apkFile), requestCode);
        }
    }

    /** 安装 APK（按路径）。 */
    public static void installApp(Activity activity, String apkPath, int requestCode) {
        installApp(activity, FileUtils.toFile(apkPath), requestCode);
    }

    /** 安装 APK（无回调）。 */
    public static void installApp(Context context, File apkFile) {
        if (FileUtils.isFileExists(apkFile)) {
            context.startActivity(IntentUtils.getOpenFileIntent(apkFile));
        }
    }

    /** 是否已安装：判据是"能拿到启动 Intent"，且包名不是空白串。 */
    public static boolean isAppInstalled(Context context, String packageName) {
        return !(TextUtils.isEmpty(packageName.trim())
                || IntentUtils.getLaunchAppIntent(context, packageName) == null);
    }

    /**
     * 用 {@code pm uninstall} 卸载。
     *
     * <p>命令串由三部分组成：固定的 {@code LD_LIBRARY_PATH} 前缀、可选的 {@code -k }
     * （保留数据）、包名。是否 root 取决于自己是不是系统应用；成功判据是 stdout 含
     * {@code success}（忽略大小写）。</p>
     *
     * @param keepData true 时追加 {@code -k}，卸载但保留数据目录
     */
    public static boolean uninstallAppByShell(Context context, String packageName, boolean keepData) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return false;
        }
        ShellUtils.ExecResult result = ShellUtils.exec(
                "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm uninstall "
                        + (keepData ? "-k " : "") + packageName,
                !isSystemApp(context), true);
        return result.stdout != null && result.stdout.toLowerCase().contains("success");
    }

    /**
     * 清空应用私有数据。
     *
     * <p>顺序与原厂一致：外部缓存 → 内部缓存 → 数据库 → shared_prefs → 内部文件，
     * 中间用非短路的 {@code &} 连接，保证 5 个动作都会执行；随后再逐个清传入的目录。</p>
     */
    public static boolean cleanAppData(Context context, File... dirs) {
        boolean allCleaned = CleanUtils.cleanExternalCache(context) & CleanUtils.cleanInternalCache(context)
                & CleanUtils.cleanDatabases(context) & CleanUtils.cleanSharedPreference(context)
                & CleanUtils.cleanFiles(context);
        for (File dir : dirs) {
            allCleaned &= CleanUtils.cleanDirectory(dir);
        }
        return allCleaned;
    }

    /** 清空应用私有数据（按路径）。 */
    public static boolean cleanAppData(Context context, String... dirPaths) {
        int index = 0;
        File[] dirs = new File[dirPaths.length];
        for (String dirPath : dirPaths) {
            dirs[index] = new File(dirPath);
            index++;
        }
        return cleanAppData(context, dirs);
    }

    /** 卸载应用：走 DELETE Intent，需要用户确认，所以带 requestCode。 */
    public static void uninstallApp(Activity activity, String packageName, int requestCode) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return;
        }
        activity.startActivityForResult(IntentUtils.getUninstallAppIntent(packageName), requestCode);
    }

    /** 打开本应用的"应用详情"设置页。 */
    public static void openAppDetailsSettings(Context context) {
        openAppDetailsSettings(context, context.getPackageName());
    }

    /** 安装 APK：走 VIEW Intent（包安装器），适合从通知 / 后台直接拉起。 */
    public static void installApp(Context context, String apkPath) {
        installApp(context, FileUtils.toFile(apkPath));
    }

    /** 本应用名称；取不到时返回 null。 */
    public static String getAppName(Context context) {
        return getAppName(context, context.getPackageName());
    }

    /** 启动应用：用启动 Intent + requestCode。 */
    public static void startApp(Activity activity, String packageName, int requestCode) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return;
        }
        activity.startActivityForResult(IntentUtils.getLaunchAppIntent(activity, packageName), requestCode);
    }

    /**
     * 用 {@code pm install} 静默安装。
     *
     * <p>先确认 APK 文件存在，再拼 {@code LD_LIBRARY_PATH} 前缀 + {@code pm install} + 路径；
     * 是否 root 同样取决于"自己是不是系统应用"。</p>
     */
    public static boolean installAppByShell(Context context, String apkPath) {
        if (!FileUtils.isFileExists(FileUtils.toFile(apkPath))) {
            return false;
        }
        ShellUtils.ExecResult result = ShellUtils.exec(
                "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm install " + apkPath,
                !isSystemApp(context), true);
        return result.stdout != null && result.stdout.toLowerCase().contains("success");
    }

    /** 本应用图标。 */
    public static Drawable getAppIcon(Context context) {
        return getAppIcon(context, context.getPackageName());
    }

    /** 卸载应用：走 DELETE Intent（无回调）。 */
    public static void uninstallApp(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return;
        }
        context.startActivity(IntentUtils.getUninstallAppIntent(packageName));
    }

    /** 本应用安装包路径。 */
    public static String getAppPath(Context context) {
        return getAppPath(context, context.getPackageName());
    }

    /** 启动应用（无回调）。 */
    public static void startApp(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return;
        }
        context.startActivity(IntentUtils.getLaunchAppIntent(context, packageName));
    }

    /** 本应用版本名。 */
    public static String getAppVersionName(Context context) {
        return getAppVersionName(context, context.getPackageName());
    }

    /** 打开指定应用的"应用详情"设置页。 */
    public static void openAppDetailsSettings(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return;
        }
        context.startActivity(IntentUtils.getAppDetailsSettingsIntent(packageName));
    }

    /** 本应用版本码；取不到返回 -1。 */
    public static int getAppVersionCode(Context context) {
        return getAppVersionCode(context, context.getPackageName());
    }

    /** 指定包名对应的应用名称；包不存在返回 null。 */
    public static String getAppName(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.applicationInfo.loadLabel(packageManager).toString();
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 指定包名对应的应用图标；包不存在返回 null。 */
    public static Drawable getAppIcon(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.applicationInfo.loadIcon(packageManager);
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 本应用是否系统应用。 */
    public static boolean isSystemApp(Context context) {
        return isSystemApp(context, context.getPackageName());
    }

    /** 指定包的安装路径（APK 的 sourceDir）；包不存在返回 null。 */
    public static String getAppPath(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return null;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.applicationInfo.sourceDir;
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 本应用签名。 */
    public static Signature[] getAppSignatures(Context context) {
        return getAppSignatures(context, context.getPackageName());
    }

    /** 指定包的版本名；包不存在返回 null。 */
    public static String getAppVersionName(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return null;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.versionName;
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 本应用是否在前台（委托 {@link ForegroundAppUtils}）。 */
    public static boolean isAppForeground(Context context) {
        return ForegroundAppUtils.isAppForeground(context);
    }

    /** 指定包的版本码；包不存在返回 -1。 */
    public static int getAppVersionCode(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return -1;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.versionCode;
            }
            return -1;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /** 本应用的完整档案；包信息异常时返回 null。 */
    public static AppInfo getAppInfo(Context context) {
        return getAppInfo(context, context.getPackageName());
    }

    /**
     * 指定包是否系统应用。
     *
     * <p>判据是 {@code ApplicationInfo.flags & 1}（{@code FLAG_SYSTEM}）；查不到包时
     * 打堆栈并返回 false。</p>
     */
    public static boolean isSystemApp(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            return !(applicationInfo == null || (applicationInfo.flags & 1) == 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 已安装应用列表，按系统返回顺序（原厂未排序）。 */
    public static List<AppInfo> getInstalledApps(Context context) {
        List<AppInfo> appInfos = new ArrayList<AppInfo>();
        PackageManager packageManager = context.getPackageManager();
        Iterator<PackageInfo> iterator = packageManager.getInstalledPackages(0).iterator();
        while (iterator.hasNext()) {
            AppInfo appInfo = createAppInfo(packageManager, iterator.next());
            if (appInfo != null) {
                appInfos.add(appInfo);
            }
        }
        return appInfos;
    }

    /** 指定包的签名；包不存在返回 null。 */
    public static Signature[] getAppSignatures(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName.trim())) {
            return null;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.signatures;
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 本应用版本名，走 {@code applicationContext} 查，失败一律返回 null、不打堆栈。
     *
     * <p>与 {@link #getAppVersionName(Context)} 的区别：那条用 {@code context} 自己的
     * PackageManager 且会打堆栈，这条更"安静"，适合在初始化阶段反复调用。</p>
     */
    public static String getOwnVersionNameOrNull(Context context) {
        return getVersionNameOrNull(context, context.getPackageName());
    }

    /** 已废弃：指定包是否在前台（委托 {@link ForegroundAppUtils#isForeground}）。 */
    @Deprecated
    public static boolean isAppForeground(Context context, String packageName) {
        return ForegroundAppUtils.isForeground(context, packageName);
    }

    /** 指定包的完整档案；查不到包时打堆栈并返回 null。 */
    public static AppInfo getAppInfo(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return createAppInfo(packageManager, packageManager.getPackageInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 本应用名称；任何异常都返回空串（不打印堆栈），名称本身为空时也返回空串。 */
    public static String getOwnAppNameOrEmpty(Context context) {
        String appName;
        try {
            PackageManager packageManager = context.getPackageManager();
            appName = packageManager.getPackageInfo(context.getPackageName(), 0)
                    .applicationInfo.loadLabel(packageManager).toString();
        } catch (Exception e) {
            return "";
        }
        return TextUtils.isEmpty(appName) ? "" : appName;
    }

    /**
     * 指定包的版本名，走 {@code applicationContext} 查；包不存在返回 null、不打堆栈。
     */
    @Nullable
    public static String getVersionNameOrNull(Context context, @NonNull String packageName) {
        try {
            PackageInfo packageInfo = context.getApplicationContext().getPackageManager()
                    .getPackageInfo(packageName, 0);
            if (packageInfo == null) {
                return null;
            }
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * 当前前台应用包名。
     *
     * <p>原厂这里是<b>实例方法</b>，而本类构造器私有、构造即抛异常，所以它永远不可能被调用 ——
     * 属于混淆残留。为了保持 API 形状一致仍然保留，实际请用
     * {@link ForegroundAppUtils#getForegroundPackage(Context)}。</p>
     */
    public String getForegroundPackageName(Context context) {
        return ForegroundAppUtils.getForegroundPackage(context);
    }
}
