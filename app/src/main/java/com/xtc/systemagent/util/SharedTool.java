package com.xtc.systemagent.util;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.systemagent.module.Constants;
import com.xtc.utils.encode.UuidUtils;
import com.xtc.utils.storage.SPUtils;

/**
 * 采集策略与状态在 SharedPreferences 里的读写封装。
 *
 * <p>每个采集模块（通话日志 / Modem / 网络 Modem / WiFi / TCP Dump / ADSP / RPM）
 * 都把自己那份"策略 JSON + 状态 JSON + 时间戳"存到同一个 SP 文件里，
 * 键名前缀统一走 {@code Constants}。</p>
 *
 * <p>注意 {@code saveMachineId} 返回 {@code Boolean} 包装类型，
 * 与原厂一致，不做拆箱。</p>
 */
public class SharedTool {

    private SharedTool() {
    }

    /** 读取 ADSP dump 状态 JSON。 */
    public static String getAdspDumpStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_ADSP_DUMP_STATUS, "");
    }

    /** 读取 ADSP dump 策略 JSON。 */
    public static String getAdspDumpStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_ADSP_DUMP_STRATEGY, "");
    }

    /** 读取通话日志采集的超时时间。 */
    public static String getCallLogOverTime(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_CALL_LOG_OVER_TIME, "");
    }

    /** 读取通话日志采集状态 JSON。 */
    public static String getCallLogStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_CALL_LOG_STATUS, "");
    }

    /** 读取通话日志采集策略 JSON。 */
    public static String getCallLogStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_CALL_LOG_STRATEGY, "");
    }

    /** 读取 Modem dump 状态 JSON。 */
    public static String getModemDumpStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_MODEM_DUMP_STATUS, "");
    }

    /** 读取 Modem dump 策略 JSON。 */
    public static String getModemDumpStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_MODEM_DUMP_STRATEGY, "");
    }

    /** 读取 Modem 日志采集的超时时间。 */
    public static String getModemLogOverTime(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_MODEM_LOG_OVER_TIME, "");
    }

    /** 读取 Modem 日志采集状态 JSON。 */
    public static String getModemLogStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_MODEM_LOG_STATUS, "");
    }

    /** 读取 Modem 日志采集策略 JSON。 */
    public static String getModemLogStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_MODEM_LOG_STRATEGY, "");
    }

    /** 读取网络状态采集策略 JSON。 */
    public static String getModemStateStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.NetWorkModemLog.SP_NETWORK_STATE_STRATEGY, "");
    }

    /** 读取网络 Modem 日志采集状态 JSON。 */
    public static String getNetWorkModemLogStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.NetWorkModemLog.SP_NETWORK_MODEMLOG_STATUS, "");
    }

    /** 读取网络 Modem 日志采集策略 JSON。 */
    public static String getNetWorkModemLogStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.NetWorkModemLog.SP_NETWORK_MODEMLOG_STRATEGY, "");
    }

    /** 读取 tcpdump 文件名（作为状态）。 */
    public static String getTcpDumpStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_TCP_DUMP_FILE_NAME, "");
    }

    /** 读取 tcpdump 采集策略 JSON。 */
    public static String getTcpDumpStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_TCP_DUMP_STRATEGY, "");
    }

    /** 读取 WiFi 日志采集状态 JSON。 */
    public static String getWifiLogStatus(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_WIFI_LOG_STATUS, "");
    }

    /** 读取 WiFi 日志采集策略 JSON。 */
    public static String getWifiLogStrategy(Context context) {
        return SPUtils.getInstance(context).getString(Constants.LogCollect.SP_WIFI_LOG_STRATEGY, "");
    }

    /** 读取通话日志的网络类型序号。 */
    public static int getCallNetNumber(Context context) {
        return SPUtils.getInstance(context).getInt(Constants.LogCollect.SP_CALL_LOG_NET_NUMBER, 0);
    }

    /** 读取当前网络类型（-1 表示未知）。 */
    public static int getCurrentNetworkType(Context context) {
        return SPUtils.getInstance(context).getInt(Constants.Init.NETWORK_TYPE, -1);
    }

    /** 读取 RPM debug 值（-1 表示未下发）。 */
    public static int getRpmDebugValue(Context context) {
        return SPUtils.getInstance(context).getInt(Constants.RpmDebugValue.SP_NET_RPM_DBG_VALUE, -1);
    }

    /**
     * 读取机器唯一标识；没有就生成一个 32 位 UUID 并落盘。
     */
    public static String getMachineId(Context context) {
        String machineId = SPUtils.getInstance(context).getString(Constants.Init.MACHINE_ID, "");
        if (!TextUtils.isEmpty(machineId)) {
            return machineId;
        }
        String generated = UuidUtils.uuid();
        saveMachineId(context, generated);
        return generated;
    }

    /** 保存 ADSP dump 状态 JSON。 */
    public static boolean saveAdspDumpStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_ADSP_DUMP_STATUS, value);
    }

    /** 保存 ADSP dump 策略 JSON。 */
    public static boolean saveAdspDumpStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_ADSP_DUMP_STRATEGY, value);
    }

    /** 保存通话日志采集的超时时间。 */
    public static boolean saveCallLogOverTime(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_CALL_LOG_OVER_TIME, value);
    }

    /** 保存通话日志采集状态 JSON。 */
    public static boolean saveCallLogStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_CALL_LOG_STATUS, value);
    }

    /** 保存通话日志采集策略 JSON。 */
    public static boolean saveCallLogStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_CALL_LOG_STRATEGY, value);
    }

    /** 保存 Modem dump 状态 JSON。 */
    public static boolean saveModemDumpStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_MODEM_DUMP_STATUS, value);
    }

    /** 保存 Modem dump 策略 JSON。 */
    public static boolean saveModemDumpStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_MODEM_DUMP_STRATEGY, value);
    }

    /** 保存 Modem 日志采集的超时时间。 */
    public static boolean saveModemLogOverTime(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_MODEM_LOG_OVER_TIME, value);
    }

    /** 保存 Modem 日志采集状态 JSON。 */
    public static boolean saveModemLogStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_MODEM_LOG_STATUS, value);
    }

    /** 保存 Modem 日志采集策略 JSON。 */
    public static boolean saveModemLogStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_MODEM_LOG_STRATEGY, value);
    }

    /** 保存网络状态采集策略 JSON。 */
    public static boolean saveModemStateStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.NetWorkModemLog.SP_NETWORK_STATE_STRATEGY, value);
    }

    /** 保存网络 Modem 日志采集状态 JSON。 */
    public static boolean saveNetWorkModemLogStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.NetWorkModemLog.SP_NETWORK_MODEMLOG_STATUS, value);
    }

    /** 保存网络 Modem 日志采集策略 JSON。 */
    public static boolean saveNetWorkModemLogStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.NetWorkModemLog.SP_NETWORK_MODEMLOG_STRATEGY, value);
    }

    /** 保存 tcpdump 文件名（作为状态）。 */
    public static boolean saveTcpDumpStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_TCP_DUMP_FILE_NAME, value);
    }

    /** 保存 tcpdump 采集策略 JSON。 */
    public static boolean saveTcpDumpStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_TCP_DUMP_STRATEGY, value);
    }

    /** 保存 WiFi 日志采集状态 JSON。 */
    public static boolean saveWifiLogStatus(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_WIFI_LOG_STATUS, value);
    }

    /** 保存 WiFi 日志采集策略 JSON。 */
    public static boolean saveWifiLogStrategy(Context context, String value) {
        return SPUtils.getInstance(context).putString(Constants.LogCollect.SP_WIFI_LOG_STRATEGY, value);
    }

    /** 保存通话日志的网络类型序号。 */
    public static boolean saveCallNetNumber(Context context, int value) {
        return SPUtils.getInstance(context).putInt(Constants.LogCollect.SP_CALL_LOG_NET_NUMBER, value);
    }

    /** 保存当前网络类型。 */
    public static boolean saveCurrentNetworkType(Context context, int value) {
        return SPUtils.getInstance(context).putInt(Constants.Init.NETWORK_TYPE, value);
    }

    /** 保存 RPM debug 值。 */
    public static boolean saveRpmDebugValue(Context context, int value) {
        return SPUtils.getInstance(context).putInt(Constants.RpmDebugValue.SP_NET_RPM_DBG_VALUE, value);
    }

    /** 保存机器唯一标识。返回包装类型，与原厂签名一致。 */
    private static Boolean saveMachineId(Context context, String value) {
        return Boolean.valueOf(SPUtils.getInstance(context).putString(Constants.Init.MACHINE_ID, value));
    }
}
