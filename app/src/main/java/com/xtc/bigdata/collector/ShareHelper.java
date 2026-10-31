package com.xtc.bigdata.collector;

import android.content.ContentResolver;
import android.content.ContentValues;

import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.error.ErrorCode;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.UriUtils;
import com.xtc.log.LogUtil;

/**
 * 跨进程写数据/发通知的门面。
 *
 * <p>采集库自己不开数据库，所有落库都通过宿主 ContentProvider 完成：
 * {@code insert}/{@code bulkInsert} 写数据，{@code getType} 触发宿主的
 * "启动/退出/实时上报/查询" 等分支。</p>
 *
 * <p>所有方法都吞异常并只打 {@link ErrorCode#CONTROL_INIT_PROVIDER} ——
 * provider 没起来时上报会整体失效，但绝不能因此让宿主崩溃。</p>
 */
public class ShareHelper {

    private static final String TAG = "ShareHelper";

    /** 单例。 */
    private static ShareHelper mInstance;

    private ShareHelper() {
    }

    /**
     * 取单例（双重检查）。
     *
     * @return 单例
     */
    public static ShareHelper getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        synchronized (ShareHelper.class) {
            if (mInstance == null) {
                mInstance = new ShareHelper();
            }
        }
        return mInstance;
    }

    /**
     * 取 ContentResolver。
     *
     * @return resolver
     */
    private ContentResolver getResolver() {
        return ContextUtils.getContext().getContentResolver();
    }

    /** 通知宿主：应用退出。 */
    public void appExitNotify() {
        if (ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().getType(UriUtils.getAppExitContentUri(ContextUtils.getContext()));
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /** 通知宿主：应用启动（顺带触发一次上报）。 */
    public void appLaunchNotify() {
        if (ContextUtils.isEmpty()) {
            return;
        }
        try {
            if (Constants.isDebug) {
                LogUtil.i(TAG, "启动上传");
            }
            getResolver().getType(UriUtils.getAppLaunchContentUri(ContextUtils.getContext()));
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /**
     * 删除全部数据。
     *
     * @return 是否真的删掉了记录（返回 0 条也算 false）
     */
    public boolean deleteAll() {
        int deletedCount;
        try {
            deletedCount = getResolver().delete(UriUtils.getContentUri(ContextUtils.getContext()), null, null);
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
            deletedCount = 0;
        }
        return deletedCount != 0;
    }

    /**
     * 插入一条数据。
     *
     * @param contentValues 数据；为 null 或没有 Context 时静默返回
     */
    public void insert(ContentValues contentValues) {
        if (contentValues == null || ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().insert(UriUtils.getContentUri(ContextUtils.getContext()), contentValues);
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /**
     * 批量插入数据。
     *
     * @param contentValuesArray 数据数组；为 null 或空数组时静默返回
     */
    public void insertBulk(ContentValues[] contentValuesArray) {
        if (contentValuesArray == null || contentValuesArray.length == 0 || ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().bulkInsert(UriUtils.getContentUri(ContextUtils.getContext()), contentValuesArray);
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /** 通知宿主：按下了 Home 键。 */
    public void pressHomeKeyNotify() {
        if (ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().getType(UriUtils.getPressHomeKeyContentUri(ContextUtils.getContext()));
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /**
     * 通知指定包名的宿主去查询数据。
     *
     * @param packageName 目标包名
     */
    public void queryData(String packageName) {
        if (ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().getType(UriUtils.getQueryDataContentUri(packageName));
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /** 通知宿主：实时上报一次。 */
    public void realTimeNotify() {
        if (ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().getType(UriUtils.getRealTimeContentUri(ContextUtils.getContext()));
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }

    /**
     * 通知指定包名的宿主上报数据。
     *
     * @param packageName 目标包名
     */
    public void reportData(String packageName) {
        if (ContextUtils.isEmpty()) {
            return;
        }
        try {
            getResolver().getType(UriUtils.getReportDataContentUri(packageName));
        } catch (Exception e) {
            LogUtil.e(TAG, ErrorCode.CONTROL_INIT_PROVIDER);
            e.printStackTrace();
        }
    }
}
