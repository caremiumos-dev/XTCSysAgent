package com.xtc.systemagent.module.sys;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.text.TextUtils;
import com.xtc.httplib.net.BaseUrlManager;
import com.xtc.log.LogUtil;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.systemagent.module.sys.bean.RpmImContent;
import com.xtc.systemagent.net.SysHttpProxy;
import com.xtc.systemagent.net.bean.RpmStateParam;
import com.xtc.systemagent.util.SharedTool;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.lang.ref.SoftReference;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * RPM（资源功耗管理）状态的上报。
 *
 * <p>IM 下发一条 {@link RpmImContent} 后，本类负责：</p>
 * <ol>
 *   <li>把调试值写进系统属性 {@code log.tag.rpmdebug}；</li>
 *   <li>把调试值缓存在本地，作为"待上报"标记；</li>
 *   <li>立刻上报一次 {@code rpm[x] prop set.}；</li>
 *   <li>10 秒后再上报一次真实的 RPM 模式，成功后把待上报标记清成 -1。</li>
 * </ol>
 */
public class RpmServeImpl {

    private static final String TAG = "Log_RpmServeImpl";

    /** 延迟上报的闹钟广播。 */
    private static final String RPM_FEED_ALARM = "com.xtc.rpm.ACTION.RpmValueFeed";

    /** 延迟上报的等待时间：10 秒。 */
    private static final long FEED_DELAY = 10000L;

    /** {@code AlarmManager.RTC_WAKEUP}。 */
    private static final int RTC_WAKEUP = 2;

    private static SoftReference<RpmServeImpl> rpmServeImplRef;

    private Context mContext;
    private SysHttpProxy httpProxy;
    private AlarmManager am;
    private PendingIntent feedPI;
    private BroadcastReceiver feedReceiver = null;

    private RpmServeImpl(Context context) {
        this.mContext = context;
        this.httpProxy = new SysHttpProxy(context, BaseUrlManager.getGatewayUrl(context));
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static synchronized RpmServeImpl getInstance(Context context) {
        if (rpmServeImplRef == null) {
            rpmServeImplRef = new SoftReference<>(new RpmServeImpl(context));
        }
        RpmServeImpl instance = rpmServeImplRef.get();
        if (instance == null) {
            instance = new RpmServeImpl(context);
            rpmServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    /**
     * 处理 IM 下发的 RPM 设置。
     *
     * <p>{@code rpm_debug_value} 会同时写系统属性和本地缓存。</p>
     */
    public void setRpmByImContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return;
        }
        RpmImContent rpmContent = (RpmImContent) JSONUtil.fromJSON(content, RpmImContent.class);
        LogUtil.d(TAG, "setRpmByImContent rpmContent:" + rpmContent);
        if (rpmContent == null) {
            LogUtil.d(TAG, "setRpmByImContent but rpmImContent is null.");
            return;
        }
        RpmPropUtil.setRpmDebugValue(rpmContent.getRpm_debug_value());
        SharedTool.saveRpmDebugValue(this.mContext, rpmContent.getRpm_debug_value());
        feedRpmState("rpm[" + RpmPropUtil.getRpmDebugValue() + "] prop set.", false);
    }

    /**
     * 如果有待上报的 RPM 变更，注册接收器并安排 10 秒后的上报。
     *
     * <p>本地缓存的 -1 表示"没有待上报的内容"。</p>
     */
    public void startFeedDelayAlarm() {
        if (SharedTool.getRpmDebugValue(this.mContext) == -1) {
            LogUtil.d(TAG, "无效的rpm debug值, 2种情况:a.没有设置rpm，b.rpm上报已完成且无更新");
            return;
        }
        registerFeedReceiver();
        // 原厂把 NotificationFlag.NOTIFICATION_FLAG_HEADER (0x08000000) 当 PendingIntent flags 用，
        // 数值上等于 FLAG_UPDATE_CURRENT，保留原常量。
        this.feedPI = PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(RPM_FEED_ALARM), NotificationFlag.NOTIFICATION_FLAG_HEADER);
        this.am.setExact(RTC_WAKEUP, SystemClock.elapsedRealtime() + FEED_DELAY, this.feedPI);
    }

    private void registerFeedReceiver() {
        LogUtil.i(TAG, "registerFeedRpmReceiver");
        this.feedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.d(TAG, "feedRpmReceiver , action = " + action);
                if (RPM_FEED_ALARM.equals(action)) {
                    RpmServeImpl.this.feedRpmMode();
                    RpmServeImpl.this.cancelFeedAlarm();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(RPM_FEED_ALARM);
        this.mContext.registerReceiver(this.feedReceiver, filter);
    }

    private void cancelFeedAlarm() {
        LogUtil.d(TAG, "cancelFeedRpmAlarm");
        if (this.feedReceiver != null) {
            this.mContext.unregisterReceiver(this.feedReceiver);
        }
        if (this.am != null && this.feedPI != null) {
            this.am.cancel(this.feedPI);
        }
        this.feedReceiver = null;
    }

    private void feedRpmMode() {
        LogUtil.i(TAG, "feedRpmMode:" + RpmPropUtil.getRpmMode());
        feedRpmState(RpmPropUtil.getRpmMode(), true);
    }

    /**
     * 上报一次 RPM 状态。
     *
     * @param clearPending 成功后是否把本地"待上报"标记清成 -1
     */
    private void feedRpmState(final String state, final boolean clearPending) {
        if (TextUtils.isEmpty(state)) {
            return;
        }
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        RpmStateParam param = new RpmStateParam();
        param.setBindNum(bindNumber);
        param.setRpmMode(state);
        this.httpProxy.feedRpmState(param)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.d(TAG, "rpm 上报完成 state:" + state);
                        if (clearPending) {
                            SharedTool.saveRpmDebugValue(RpmServeImpl.this.mContext, -1);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.d(TAG, "rpm 上报失败 state:" + state + " ,ex:" + throwable);
                    }

                    @Override
                    public void onNext(String result) {
                    }
                });
    }
}
