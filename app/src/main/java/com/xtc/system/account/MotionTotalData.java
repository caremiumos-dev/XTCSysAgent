package com.xtc.system.account;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.system.account.bean.NetFriendRank;
import com.xtc.utils.date.DateUtils;
import com.xtc.utils.encode.JSONUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 运动汇总数据（步数 / 卡路里 / 距离）。
 *
 * <p>数据来自运动进程的 ContentProvider（{@code com.xtc.motion}）。
 * 那个 Provider 把结果塞在 {@code getType()} 里返回 JSON 字符串 ——
 * 不是常规的 query 用法，但原厂就这么干的。</p>
 */
public class MotionTotalData {

    public String dateToday;
    public int totalCalories;
    public int totalDistance;
    public int totalStep;

    public MotionTotalData() {
    }

    public MotionTotalData(String dateToday, int totalStep, int totalCalories, int totalDistance) {
        this.dateToday = dateToday;
        this.totalStep = totalStep;
        this.totalCalories = totalCalories;
        this.totalDistance = totalDistance;
    }

    /** 今天的日期（yyyy-MM-dd）。 */
    public static String getDateToDay() {
        return new SimpleDateFormat(DateUtils.PATTERN_DATE, Locale.CHINA).format(new Date());
    }

    /** 好友排行榜。Provider 不可用时返回 null。 */
    public static synchronized List<NetFriendRank> getFriendList(Context context) {
        String json;
        try {
            json = context.getContentResolver()
                    .getType(Uri.parse("content://com.xtc.motion/friend_list"));
        } catch (Exception e) {
            json = "";
        }
        LogUtil.i("life", "json:" + json);
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        //noinspection unchecked
        return (List<NetFriendRank>) JSONUtil.toCollection(json, List.class, NetFriendRank.class);
    }

    /** 目标步数。读不到时用 8000。 */
    public static synchronized int getGoalSteps(Context context) {
        String steps;
        try {
            steps = context.getContentResolver()
                    .getType(Uri.parse("content://com.xtc.motion/goal_steps"));
        } catch (Exception e) {
            steps = "8000";
        }
        LogUtil.i("life", "steps:" + steps);
        return Integer.valueOf(steps).intValue();
    }

    /**
     * 最近一次汇总数据。
     *
     * @param refresh true 走 {@code refresh_data}（触发重算），false 走 {@code read_data}（只读）
     */
    public static synchronized MotionTotalData getLastTotalData(Context context, boolean refresh) {
        String json;
        try {
            json = context.getContentResolver().getType(refresh
                    ? Uri.parse("content://com.xtc.motion/refresh_data")
                    : Uri.parse("content://com.xtc.motion/read_data"));
        } catch (Exception e) {
            json = "";
        }
        LogUtil.i("life", "json:" + json);
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        return (MotionTotalData) JSONUtil.fromJSON(json, MotionTotalData.class);
    }

    public String getDateToday() {
        return this.dateToday;
    }

    public void setDateToday(String dateToday) {
        this.dateToday = dateToday;
    }

    public int getTotalCalories() {
        return this.totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public int getTotalDistance() {
        return this.totalDistance;
    }

    public void setTotalDistance(int totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTotalStep() {
        return this.totalStep;
    }

    public void setTotalStep(int totalStep) {
        this.totalStep = totalStep;
    }

    @Override
    public String toString() {
        return "MotionTotalData{dateToday='" + this.dateToday + '\''
                + ", totalStep=" + this.totalStep
                + ", totalCalories=" + this.totalCalories
                + ", totalDistance=" + this.totalDistance + '}';
    }
}
