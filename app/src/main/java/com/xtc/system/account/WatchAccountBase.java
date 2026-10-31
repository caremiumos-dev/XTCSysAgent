package com.xtc.system.account;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.xtc.log.LogUtil;
import com.xtc.system.account.bean.FunSwitch;

/**
 * 手表账号 Provider 的访问入口。
 *
 * <p>所有账号信息都通过 {@code com.xtc.provider} 的 ContentProvider 暴露，
 * 客户端只需要 {@code getType()} 就能拿到一个字符串结果，不用解析 Cursor。</p>
 */
public class WatchAccountBase {

    public static final String AUTHORITY = "com.xtc.initservice.watchaccount";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xtc.initservice";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xtc.initservice";
    public static final String DEFAULT_SORT_ORDER = "id asc";

    public static final int ITEM = 1;
    public static final int ITEM_ID = 2;

    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_SHORT_NUMBER = "shortNumber";
    public static final String KEY_MODEL = "model";
    public static final String KEY_INNER_MODEL = "innerModel";
    public static final String KEY_GENDER = "gender";
    public static final String KEY_BIRTHDAY = "birthday";
    public static final String KEY_GRADE = "grade";

    private static final String TAG = WatchAccountBase.class.getSimpleName();

    private static final String CONTENT_AUTHORITIES = "content://com.xtc.provider/low_authorities";
    private static final String CONTENT_WATCHID_TYPE =
            "content://com.xtc.provider/BaseDataProvider/watchId/1";
    private static final String CONTENT_ICON_TYPE =
            "content://com.xtc.provider/BaseDataProvider/icon/2";
    private static final String CONTENT_NAME_TYPE =
            "content://com.xtc.provider/BaseDataProvider/name/3";
    private static final String CONTENT_TEMPERTATUREMAP_TYPE =
            "content://com.xtc.provider/BaseDataProvider/tempmap/4";
    private static final String CONTENT_NUMBER_TYPE =
            "content://com.xtc.provider/BaseDataProvider/number/5";
    private static final String CONTENT_ICONPATH_TYPE =
            "content://com.xtc.provider/BaseDataProvider/iconpath/6";
    private static final String CONTENT_GENIUSACCOUNT_TYPE =
            "content://com.xtc.provider/BaseDataProvider/geniusaccount/7";
    private static final String CONTENT_OPENID_TYPE =
            "content://com.xtc.provider/BaseDataProvider/openID/8";

    /** 模块显示状态：显示。 */
    private static final int DISPLAY = 0;
    /** 模块显示状态：隐藏。 */
    private static final int HIDDEN = 1;
    /** 模块显示状态：显示但不可用，需要提示用户。 */
    private static final int DISPLAY_HAS_TIP = 2;

    public static final Uri CONTENT_URI =
            Uri.parse("content://com.xtc.initservice.watchaccount/item");

    private WatchAccountBase() {
    }

    public static String getAccountIcon(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_ICON_TYPE));
    }

    public static String getAccountName(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_NAME_TYPE));
    }

    public static String getAccountNumber(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_NUMBER_TYPE));
    }

    public static String getAccountTemperatureMap(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_TEMPERTATUREMAP_TYPE));
    }

    public static String getAccountWatchId(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_WATCHID_TYPE));
    }

    public static String getGeniusNumber(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_GENIUSACCOUNT_TYPE));
    }

    public static String getLocalIconPath(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_ICONPATH_TYPE));
    }

    public static String getOpenID(Context context) {
        return context.getContentResolver().getType(Uri.parse(CONTENT_OPENID_TYPE));
    }

    public static Cursor getWatchAccountCursor(Context context) {
        return context.getContentResolver()
                .query(Uri.parse(CONTENT_AUTHORITIES), null, null, null, null);
    }

    /**
     * 按包名查功能开关。
     *
     * @param defaultOn 查不到时默认的开关状态
     */
    public static FunSwitch queryFunSwitchByPackageName(Context context, String packageName,
                                                        boolean defaultOn) {
        Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://com.xtc.fundata/FunSwitch"),
                null, "appPackage='" + packageName + "'", null, null);
        FunSwitch funSwitch = new FunSwitch();
        if (cursor == null || !cursor.moveToFirst()) {
            LogUtil.e(TAG, "cursor is null");
            funSwitch.setSwitchId(-1);
            funSwitch.setSwitchStatus(defaultOn ? 0 : 1);
        } else {
            int switchStatus = cursor.getInt(cursor.getColumnIndex("switchStatus"));
            int switchId = cursor.getInt(cursor.getColumnIndex("switchId"));
            cursor.close();
            funSwitch.setSwitchStatus(switchStatus);
            funSwitch.setSwitchId(switchId);
            LogUtil.i(TAG, switchId + ":" + funSwitch.toString());
        }
        return funSwitch;
    }

    /** 按模块查是否可用（把三态折叠成布尔）。 */
    public static boolean queryModuleSwitchByBoolean(Context context, int module, boolean defaultOn) {
        switch (queryModuleSwitchByInt(context, module, defaultOn)) {
            case DISPLAY:
                LogUtil.i(TAG, "功能当前显示");
                return true;
            case HIDDEN:
                LogUtil.i(TAG, "功能隐藏");
                return false;
            case DISPLAY_HAS_TIP:
                LogUtil.i(TAG, "功能显示但是不可用，提示用户");
                return true;
            default:
                return false;
        }
    }

    /**
     * 按模块查显示状态。
     *
     * @return 0=显示 1=隐藏 2=显示但有提示；查不到时按 {@code defaultOn} 返回 0 或 1
     */
    public static int queryModuleSwitchByInt(Context context, int module, boolean defaultOn) {
        Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://com.xtc.moduledata/query"),
                null, "module=" + module, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            LogUtil.e(TAG, "cursor is null");
            return defaultOn ? DISPLAY : HIDDEN;
        }
        int display = cursor.getInt(cursor.getColumnIndex("display"));
        cursor.close();
        LogUtil.i(TAG, module + ":" + display);
        return display;
    }
}
