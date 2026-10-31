package com.xtc.utils.storage;

import android.content.Context;
import android.content.ContextWrapper;
import com.xtc.log.LogUtil;

/**
 * 跨包拿 Context 的小工具。
 *
 * <p>systemagent 需要读手表应用（{@code com.xtc.i3launcher}）私有目录下的数据库文件。
 * {@link Context#createPackageContext(String, int)} 拿到的 Context 其
 * {@code getDatabasePath()} 指向目标包的数据目录，配合 {@link ShareDBHelper#open(String, String)}
 * 就能直接读那个库。</p>
 *
 * <p>目标包没装或没权限时会抛 {@code NameNotFoundException}，这里统一转成 null，
 * 调用方按"拿不到"处理。</p>
 */
public class PackageUtils {

    private static final String TAG = "PackageUtils";

    private PackageUtils() {
    }

    /**
     * 拿到指定包的 Context。
     *
     * @param context     宿主 Context
     * @param packageName 目标包名
     * @return 目标包的 Context；失败返回 null
     */
    public static Context getPackageContext(Context context, String packageName) {
        if (context == null || packageName == null) {
            return null;
        }
        try {
            return context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            LogUtil.e(TAG, "getPackageContext() failed, packageName = " + packageName + " ,ex = " + e);
            return null;
        }
    }

    /**
     * 用 {@link ContextWrapper} 再包一层，避免调用方直接依赖目标包的 Context 类型。
     *
     * <p>原厂就多包了这一层，保留。</p>
     */
    public static Context wrap(Context context, String packageName) {
        Context packageContext = getPackageContext(context, packageName);
        return packageContext == null ? null : new ContextWrapper(packageContext);
    }
}
