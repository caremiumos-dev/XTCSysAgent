package com.xtc.bigdata.common.utils;

import android.content.Context;
import android.net.Uri;

import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.db.constant.Columns;

import java.io.File;

/**
 * ContentProvider 的 Uri 拼装。
 *
 * <p>authority 是 {@code 宿主应用id + ".provider"}，路径固定为
 * {@code content://<authority>/userbehavior<动作>}。</p>
 *
 * <p>{@link #getQueryDataContentUri(String)} 与 {@link #getReportDataContentUri(String)}
 * 收的是<b>包名</b>而不是 Context，因为它们要访问的是"别的进程"的 provider。</p>
 */
public class UriUtils {

    private UriUtils() {
    }

    /**
     * App 退出通知 Uri。
     *
     * @param context 上下文
     * @return Uri
     */
    public static Uri getAppExitContentUri(Context context) {
        return getCombineUri(getAuthority(), Columns.APP_EXIT);
    }

    /**
     * App 启动通知 Uri。
     *
     * @param context 上下文
     * @return Uri
     */
    public static Uri getAppLaunchContentUri(Context context) {
        return getCombineUri(getAuthority(), Columns.APP_LAUNCH);
    }

    /**
     * 取本应用的 provider authority。
     *
     * @return authority
     */
    public static String getAuthority() {
        return Constants.HOST_APP_ID + ".provider";
    }

    /**
     * 拼 Uri：{@code content://<authority>/userbehavior<action>}。
     *
     * @param authority authority
     * @param action    路径段（自带前导斜杠）
     * @return Uri
     */
    private static Uri getCombineUri(String authority, String action) {
        return Uri.parse("content://" + authority + File.separator + Columns.DIR_PATH + action);
    }

    /**
     * 目录型 Uri。
     *
     * @param context 上下文
     * @return Uri
     */
    public static Uri getContentUri(Context context) {
        return getCombineUri(getAuthority(), "");
    }

    /**
     * 按下 Home 键通知 Uri。
     *
     * @param context 上下文
     * @return Uri
     */
    public static Uri getPressHomeKeyContentUri(Context context) {
        return getCombineUri(getAuthority(), Columns.PRESS_HOME_KEY);
    }

    /**
     * 查询数据 Uri（指向指定包的 provider）。
     *
     * @param packageName 目标包名
     * @return Uri
     */
    public static Uri getQueryDataContentUri(String packageName) {
        return getCombineUri(packageName + ".provider", Columns.QUERY_DATA_KEY);
    }

    /**
     * 实时上报 Uri。
     *
     * @param context 上下文
     * @return Uri
     */
    public static Uri getRealTimeContentUri(Context context) {
        return getCombineUri(getAuthority(), Columns.REAL_TIME);
    }

    /**
     * 上报数据 Uri（指向指定包的 provider）。
     *
     * @param packageName 目标包名
     * @return Uri
     */
    public static Uri getReportDataContentUri(String packageName) {
        return getCombineUri(packageName + ".provider", Columns.REPORT_DATA_KEY);
    }
}
