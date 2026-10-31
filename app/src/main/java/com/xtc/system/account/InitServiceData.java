package com.xtc.system.account;

import android.content.Context;
import android.database.Cursor;
import com.xtc.log.LogUtil;
import com.xtc.system.account.bean.AppInfoBase;
import com.xtc.system.account.bean.HttpConfig;
import com.xtc.system.account.bean.ImAccountInfo;
import com.xtc.system.account.bean.WatchInfo;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.storage.PackageUtils;
import com.xtc.utils.storage.ShareDBHelper;

/**
 * 从兄弟进程（initService / i3launcher）读账号与配置数据。
 *
 * <p>两条通路：</p>
 * <ul>
 *   <li>{@code com.xtc.initservice} 的 ContentProvider —— 拿灰度、RSA 公钥、加密开关；</li>
 *   <li>{@code com.xtc.i3launcher} 的 {@code i3launcher.db} —— 直接读库拿 watchId 和 IM 账号。</li>
 * </ul>
 *
 * <p>每个方法都有"返回值"和"回调"两个重载。回调版在 listener 为 null 时
 * <strong>抛 IllegalArgumentException</strong> —— 原厂如此，不要改成静默返回。</p>
 */
public class InitServiceData {

    public static final String TAG = "InitServiceData";

    /** 手表桌面应用的包名。 */
    private static final String PACKAGE_NAME = "com.xtc.i3launcher";

    /** 桌面应用的数据库名。 */
    private static final String DB_NAME = "i3launcher.db";

    /** 账号表。 */
    private static final String TABLE_WATCH_ACCOUNT = "watch_account";

    private static final String COLUMN_WATCH_ID = "watchId";
    private static final String COLUMN_IM_ACCOUNT_INFO = "imAccountInfo";

    /** 取 App 信息的回调。 */
    public interface OnGetAppInfoListener {
        void onFail();

        void onSuccess(HttpConfig httpConfig);
    }

    /** 取 IM 账号的回调。 */
    public interface OnGetImAccountInfoListener {
        void onFail();

        void onSuccess(ImAccountInfo imAccountInfo);
    }

    /** 取 watchId 的回调。 */
    public interface OnGetWatchIdListener {
        void onFail();

        void onSuccess(String watchId);
    }

    /** 取手表档案的回调。 */
    public interface OnGetWatchInfoListener {
        void onFail();

        void onSuccess(WatchInfo watchInfo);
    }

    private InitServiceData() {
    }

    /** 灰度 / RSA 公钥 / 加密开关。取不到返回 null。 */
    public static HttpConfig getAppInfo(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(AppInfoBase.CONTENT_URI,
                    new String[]{AppInfoBase.KEY_GREY, AppInfoBase.KEY_RSA,
                            AppInfoBase.KEY_ENC_SWITCH}, null, null, null);
            if (cursor == null || !cursor.moveToNext()) {
                return null;
            }
            String grey = cursor.getString(cursor.getColumnIndex(AppInfoBase.KEY_GREY));
            String rsaPublicKey = cursor.getString(cursor.getColumnIndex(AppInfoBase.KEY_RSA));
            String encSwitch = cursor.getString(cursor.getColumnIndex(AppInfoBase.KEY_ENC_SWITCH));
            LogUtil.i(TAG, "grey = " + grey + ", rsaPublicKey = " + rsaPublicKey
                    + ", encSwitch = " + encSwitch);
            return new HttpConfig(grey, encSwitch, rsaPublicKey);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /** 回调版。listener 为 null 直接抛。 */
    public static void getAppInfo(Context context, OnGetAppInfoListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(AppInfoBase.CONTENT_URI,
                    new String[]{AppInfoBase.KEY_GREY, AppInfoBase.KEY_RSA,
                            AppInfoBase.KEY_ENC_SWITCH}, null, null, null);
            if (cursor == null) {
                listener.onFail();
                return;
            }
            if (!cursor.moveToNext()) {
                listener.onFail();
                return;
            }
            String grey = cursor.getString(cursor.getColumnIndex(AppInfoBase.KEY_GREY));
            String rsaPublicKey = cursor.getString(cursor.getColumnIndex(AppInfoBase.KEY_RSA));
            String encSwitch = cursor.getString(cursor.getColumnIndex(AppInfoBase.KEY_ENC_SWITCH));
            LogUtil.i(TAG, "grey = " + grey + ", rsaPublicKey = " + rsaPublicKey
                    + ", encSwitch = " + encSwitch);
            listener.onSuccess(new HttpConfig(grey, encSwitch, rsaPublicKey));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            listener.onFail();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /** 读 i3launcher.db 的 watch_account.imAccountInfo 字段（JSON）。 */
    public static void getImAccountInfo(Context context, final OnGetImAccountInfoListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        Context packageContext = PackageUtils.getPackageContext(context, PACKAGE_NAME);
        if (packageContext == null) {
            listener.onFail();
            return;
        }
        try {
            ShareDBHelper.open(packageContext, DB_NAME).query(
                    "select * from " + TABLE_WATCH_ACCOUNT,
                    new ShareDBHelper.CursorCallback() {
                        @Override
                        public void callBack(Cursor cursor) {
                            if (cursor == null || !cursor.moveToFirst()) {
                                listener.onFail();
                            } else {
                                listener.onSuccess((ImAccountInfo) JSONUtil.fromJSON(
                                        cursor.getString(cursor.getColumnIndex(
                                                COLUMN_IM_ACCOUNT_INFO)),
                                        ImAccountInfo.class));
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    });
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            listener.onFail();
        }
    }

    /** 同步读 watchId。 */
    public static String getWatchId(Context context) {
        String watchId = null;
        Context packageContext = PackageUtils.getPackageContext(context, PACKAGE_NAME);
        if (packageContext == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = ShareDBHelper.open(packageContext, DB_NAME)
                    .rawQuery("select * from " + TABLE_WATCH_ACCOUNT);
            if (cursor != null && cursor.moveToFirst()) {
                watchId = cursor.getString(cursor.getColumnIndex(COLUMN_WATCH_ID));
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return watchId;
    }

    /** 回调版读 watchId。 */
    public static void getWatchId(Context context, final OnGetWatchIdListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        Context packageContext = PackageUtils.getPackageContext(context, PACKAGE_NAME);
        if (packageContext == null) {
            listener.onFail();
            return;
        }
        LogUtil.d(TAG, "start getWatchId");
        try {
            ShareDBHelper helper = ShareDBHelper.open(packageContext, DB_NAME);
            LogUtil.d(TAG, "start getCursorData");
            helper.query("select * from " + TABLE_WATCH_ACCOUNT,
                    new ShareDBHelper.CursorCallback() {
                        @Override
                        public void callBack(Cursor cursor) {
                            if (cursor == null || !cursor.moveToFirst()) {
                                listener.onFail();
                            } else {
                                LogUtil.d(TAG, "cursor:"
                                        + cursor.getColumnIndex(COLUMN_WATCH_ID));
                                listener.onSuccess(cursor.getString(
                                        cursor.getColumnIndex(COLUMN_WATCH_ID)));
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    });
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            listener.onFail();
        } finally {
            LogUtil.d(TAG, "end getWatchId");
        }
    }

    /** 同步读手表档案（含 model / innerModel）。 */
    public static WatchInfo getWatchInfo(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(WatchAccountBase.CONTENT_URI,
                    new String[]{WatchAccountBase.KEY_NAME, WatchAccountBase.KEY_NUMBER,
                            WatchAccountBase.KEY_SHORT_NUMBER, WatchAccountBase.KEY_GENDER,
                            WatchAccountBase.KEY_GRADE, WatchAccountBase.KEY_BIRTHDAY,
                            WatchAccountBase.KEY_MODEL, WatchAccountBase.KEY_INNER_MODEL},
                    null, null, null);
            if (cursor == null || !cursor.moveToNext()) {
                return null;
            }
            return readWatchInfo(cursor);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /** 回调版读手表档案。 */
    public static void getWatchInfo(Context context, OnGetWatchInfoListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(WatchAccountBase.CONTENT_URI,
                    new String[]{WatchAccountBase.KEY_NAME, WatchAccountBase.KEY_NUMBER,
                            WatchAccountBase.KEY_SHORT_NUMBER, WatchAccountBase.KEY_GENDER,
                            WatchAccountBase.KEY_GRADE, WatchAccountBase.KEY_BIRTHDAY,
                            WatchAccountBase.KEY_MODEL, WatchAccountBase.KEY_INNER_MODEL},
                    null, null, null);
            if (cursor == null) {
                listener.onFail();
                return;
            }
            if (!cursor.moveToNext()) {
                listener.onFail();
                return;
            }
            listener.onSuccess(readWatchInfo(cursor));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            listener.onFail();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /** 从当前游标行拼一个 {@link WatchInfo}。 */
    private static WatchInfo readWatchInfo(Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(WatchAccountBase.KEY_NAME));
        String number = cursor.getString(cursor.getColumnIndex(WatchAccountBase.KEY_NUMBER));
        String shortNumber = cursor.getString(
                cursor.getColumnIndex(WatchAccountBase.KEY_SHORT_NUMBER));
        String gender = cursor.getString(cursor.getColumnIndex(WatchAccountBase.KEY_GENDER));
        String grade = cursor.getString(cursor.getColumnIndex(WatchAccountBase.KEY_GRADE));
        Long birthday = Long.valueOf(
                cursor.getLong(cursor.getColumnIndex(WatchAccountBase.KEY_BIRTHDAY)));
        String model = cursor.getString(cursor.getColumnIndex(WatchAccountBase.KEY_MODEL));
        String innerModel = cursor.getString(
                cursor.getColumnIndex(WatchAccountBase.KEY_INNER_MODEL));
        LogUtil.i(TAG, "number = " + number + ", shortNumber = " + shortNumber
                + ", name = " + name + ", gender = " + gender + ", grade = " + grade
                + ", birthday = " + birthday + ", model = " + model
                + ", innerModel = " + innerModel);
        WatchInfo watchInfo = new WatchInfo(name, number, shortNumber, gender, grade, birthday);
        watchInfo.setModel(model);
        watchInfo.setInnerModel(innerModel);
        return watchInfo;
    }
}
