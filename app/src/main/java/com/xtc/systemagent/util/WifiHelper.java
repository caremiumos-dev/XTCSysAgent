package com.xtc.systemagent.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import com.xtc.log.LogUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * 网络连通性变化的分发器。
 *
 * <p>注册是懒加载的：第一个回调加入时才注册广播，最后一个回调移除时注销，
 * 避免应用在后台长期持有广播接收器。</p>
 */
public class WifiHelper {

    private static final String TAG = "Log_WifiHelper";

    private static final String ACTION_WIFI_STATE_CHANGED = "android.net.wifi.WIFI_STATE_CHANGED";
    private static final String ACTION_WIFI_STATE_CHANGE = "android.net.wifi.STATE_CHANGE";
    private static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

    /** {@code ConnectivityManager.TYPE_MOBILE}。 */
    private static final int TYPE_MOBILE = 0;
    /** {@code ConnectivityManager.TYPE_WIFI}。 */
    private static final int TYPE_WIFI = 1;

    private static WifiHelper sInstance;

    private Context context;
    private ConnectivityManager connectivityManager;
    private List<ConnectivityCallback> connectivityCallbacks = new ArrayList<>();

    /** 连通性变化回调。 */
    public interface ConnectivityCallback {
        /**
         * @param mobileConnected 移动数据是否连接
         * @param wifiConnected   WiFi 是否连接
         */
        void onConnectivityChanged(boolean mobileConnected, boolean wifiConnected);
    }

    private BroadcastReceiver wiFiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_WIFI_STATE_CHANGED.equals(action)
                    || ACTION_WIFI_STATE_CHANGE.equals(action)
                    || ACTION_CONNECTIVITY_CHANGE.equals(action)) {
                WifiHelper.dealConnectivityChanged(context, action,
                        WifiHelper.this.connectivityManager, WifiHelper.this.connectivityCallbacks);
            } else {
                LogUtil.e(TAG, "action don't care");
            }
        }
    };

    private WifiHelper(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static WifiHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WifiHelper(context);
        }
        return sInstance;
    }

    /** 广播回调：把两条网络的状态分发给所有监听者。 */
    public static void dealConnectivityChanged(Context context, String action,
                                               ConnectivityManager connectivityManager,
                                               List<ConnectivityCallback> callbacks) {
        boolean mobileConnected = connectivityManager.getNetworkInfo(TYPE_MOBILE).isConnected();
        boolean wifiConnected = connectivityManager.getNetworkInfo(TYPE_WIFI).isConnected();
        LogUtil.d(TAG, "dealConnectivityChanged action =" + action
                + "---mobConnected = " + mobileConnected
                + "---wifiConnected = " + wifiConnected);
        for (ConnectivityCallback callback : callbacks) {
            if (callback != null) {
                callback.onConnectivityChanged(mobileConnected, wifiConnected);
            }
        }
    }

    public void setConnectivityCallback(@NonNull ConnectivityCallback callback) {
        this.connectivityCallbacks.add(callback);
        if (this.connectivityCallbacks.size() == 1) {
            registerReceiver();
        }
    }

    public void removeConnectivityCallback(@NonNull ConnectivityCallback callback) {
        if (this.connectivityCallbacks.remove(callback) && this.connectivityCallbacks.size() == 0) {
            unregisterReceiver();
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_WIFI_STATE_CHANGED);
        filter.addAction(ACTION_WIFI_STATE_CHANGE);
        filter.addAction(ACTION_CONNECTIVITY_CHANGE);
        this.context.registerReceiver(this.wiFiBroadcastReceiver, filter);
    }

    private void unregisterReceiver() {
        if (this.wiFiBroadcastReceiver != null) {
            this.context.unregisterReceiver(this.wiFiBroadcastReceiver);
        }
    }
}
