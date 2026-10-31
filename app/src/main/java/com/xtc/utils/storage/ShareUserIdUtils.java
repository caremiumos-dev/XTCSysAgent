package com.xtc.utils.storage;

import android.content.Context;
import android.content.pm.PackageManager;
import com.xtc.log.LogUtil;

/**
 * 跨包（sharedUserId）访问工具。
 *
 * <p>XTC 的守护进程和手表应用是同一个 sharedUserId，因此可以直接打开对方的
 * databases 目录。本类只做两件事：拿到对方包名的 Context，
 * 以及按"包内数据库文件"打开一个 {@link ShareDBHelper}。</p>
 */
public class ShareUserIdUtils {

    /**
     * 创建目标包名的 Context。
     *
     * <p>包不存在时返回 null 而不是抛异常——调用方（初始化流程）据此静默跳过。</p>
     */
    public static Context createPackageContext(Context context, String packageName) {
        try {
            return IOUtils.createPackageContext(context, packageName);
        } catch (PackageManager.NameNotFoundException e) {
            // 原厂这里调用的是"只带 Throwable"的重载，而该重载内部是空实现，
            // 也就是说异常被彻底吞掉、连日志都不会输出。保持原样。
            LogUtil.e(e);
            return null;
        }
    }

    /**
     * 打开指定数据库文件（位于目标进程的 databases 目录下）。
     *
     * @return 数据库不可用时返回的 helper 内部 db 为 null，后续查询方法会自行判空
     */
    public static ShareDBHelper openDatabase(Context context, String dbName) {
        String path = context.getDatabasePath(dbName).getPath();
        LogUtil.i("ShareUserIdUtil", path);
        return ShareDBHelper.open(path);
    }
}
