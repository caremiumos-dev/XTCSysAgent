package com.xtc.system.account;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.xtc.system.account.bean.StepCounter;
import com.xtc.utils.storage.ShareDBHelper;

/**
 * 从运动数据库 / Provider 读取计步数据。
 */
public class MotionData {

    private static final String DB_PACKAGE = "com.xtc.motion";
    private static final String DB_NAME = "motion.db";

    private MotionData() {
    }

    /** 读取最新一条计步记录，没有数据时返回 null。 */
    public static StepCounter getMotionData() {
        StepCounter stepCounter = null;
        ShareDBHelper db = ShareDBHelper.open(DB_PACKAGE, DB_NAME);
        Cursor cursor = db.rawQuery(
                "select * from motion_state order by id desc limit 1 offset 0");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                stepCounter = new StepCounter();
                stepCounter.setDateToDay(cursor.getString(cursor.getColumnIndex("dateToday")));
                stepCounter.setTotalStep(Integer.valueOf(cursor.getInt(cursor.getColumnIndex("totalStep"))));
                stepCounter.setTotalDistance(Integer.valueOf(
                        cursor.getInt(cursor.getColumnIndex("totalDistance"))));
            }
            cursor.close();
            db.close();
        }
        return stepCounter;
    }

    /**
     * 运动状态。读不到时返回 1（"正常"）。
     */
    public static int getMotionStatus(Context context) {
        try {
            return Integer.valueOf(context.getContentResolver()
                    .getType(Uri.parse("content://com.xtc.motion/motion_status"))).intValue();
        } catch (Exception e) {
            return 1;
        }
    }
}
