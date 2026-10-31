package com.xtc.systemagent.module;

import com.xtc.systemagent.util.SdCardUtil;

/**
 * 全模块共享的常量集合。按用途分组为若干内部接口，方便以
 * {@code Constants.Xxx.Yyy} 的形式静态引用。
 */
public interface Constants {

    /** 通话失败上报的字段名（与服务端约定）。 */
    interface CallFailure {
        String OTHER_NUMBER = "other_number";
        String VOICE_NETWORK_TYPE = "voice_network_type";
        String GSMCDMA_OR_IMS = "gsmCdma_or_ims";
        String SELF_DIAL_MODE = "self_dial_mode";
        String CALL_FAIL_START_TIME = "call_fail_start_time";
        String CALL_FAIL_CAUSE_CODE = "call_fail_cause_code";
        String CALL_FAIL_VENDOR_CAUSE = "call_fail_vendor_cause";
        String SELF_CELL_ID = "self_cell_id";
    }

    /** 系统日志文件夹的前缀。 */
    interface GlobalVariable {
        String SYSTEM_LOG = "Launcher_system_";
    }

    /** 初始化时写入 SharedPreferences 的键。 */
    interface Init {
        String MACHINE_ID = "machineId";
        String NETWORK_TYPE = "current_network_type";
    }

    /** 日志采集相关的 SharedPreferences 键与最小值。 */
    interface LogCollect {
        /** 采集前必须保留的最小剩余空间：300MB。 */
        long MIN_REMAIN_STORAGE_SPACE = 314572800L;

        String TAG = "Log_";

        String SP_ADSP_DUMP_STATUS = "sp_adsp_dump_status";
        String SP_ADSP_DUMP_STRATEGY = "sp_adsp_dump_strategy";
        String SP_CALL_LOG_NET_NUMBER = "sp_call_log_use_net_number";
        String SP_CALL_LOG_OVER_TIME = "sp_call_log_over_time";
        String SP_CALL_LOG_STATUS = "sp_call_log_status";
        String SP_CALL_LOG_STRATEGY = "sp_call_log_strategy";
        String SP_MODEM_DUMP_STATUS = "sp_modem_dump_status";
        String SP_MODEM_DUMP_STRATEGY = "sp_modem_dump_strategy";
        String SP_MODEM_LOG_OVER_TIME = "sp_modem_log_over_time";
        String SP_MODEM_LOG_STATUS = "sp_modem_log_status";
        String SP_MODEM_LOG_STRATEGY = "sp_modem_log_strategy";
        String SP_TCP_DUMP_FILE_NAME = "sp_tcp_dump_file_name";
        String SP_TCP_DUMP_STRATEGY = "sp_tcp_dump_strategy";
        String SP_WIFI_LOG_STATUS = "sp_wifi_log_status";
        String SP_WIFI_LOG_STRATEGY = "sp_wifi_log_strategy";
    }

    /** 日志上传允许的网络类型。 */
    interface LogStrategyNet {
        int ONLY_TRAFFIC = 1;
        int ONLY_WIFI = 2;
        int WIFI_TRAFFIC = 3;
    }

    /** 各日志类型对应的应用包名，用于匹配服务端下发的策略。 */
    interface LogStrategyPackageName {
        String PACKAGE_NAME_ADSP_DUMP = "com.xtc.adsp_dump";
        String PACKAGE_NAME_CALL_FAILURE_LOG = "com.xtc.volte";
        String PACKAGE_NAME_CANCEL_MODEM_LOG = "com.xtc.i3launcher.cancel.modem.log";
        String PACKAGE_NAME_MCU_DUMP = "com.xtc.mcudump";
        String PACKAGE_NAME_MOBILE_NETWORK = "com.xtc.telephony.mobile_network";
        String PACKAGE_NAME_MODEM_DUMP = "com.xtc.modem_dump";
        String PACKAGE_NAME_MODEM_LOG = "com.xtc.i3launcher.modem.log.condition";
        String PACKAGE_NAME_TCP_DUMP = "com.xtc.i3launcher.tcp.dump";
        String PACKAGE_NAME_WIFI_LOG = "com.xtc.wifi_log";
    }

    /** modem 日志相关目录（类加载时一次性算好）。 */
    interface ModemLogPath {
        String MODEM_LOG_FILE_PATH = SdCardUtil.getModemLogFilePath();
        String PRE_MODEM_LOG_FILE_PATH = SdCardUtil.getModemBackupLogFilePath();
        String ZIP_FILE_PATH = SdCardUtil.getModemLogZipFilePath();
    }

    /** 网络侧 modem 日志相关常量。 */
    interface NetWorkModemLog {
        String SP_NETWORK_MODEMLOG_STATUS = "sp_network_modem_log_status";
        String SP_NETWORK_MODEMLOG_STRATEGY = "sp_network_modem_log_strategy";
        String SP_NETWORK_STATE_STRATEGY = "sp_network_state_strategy";
        String ZIP_FILE_PATH = SdCardUtil.getNertWorkModemLogZipFilePath();
    }

    /** 推送协议里的动作与类型。 */
    interface Push {
        int PUSH_ACTION_COLLECTION_LOG = 1;
        int PUSH_ACTION_LOG_SWITCH = 3;
        int ACTION_INIT_LOG_STRATEGY = 4;
        int PUSH_TYPE_COLLECTION_LOG = 310;
        int PUSH_TYPE_UPLOAD_FILE = 320;
        int PUSH_TYPE_LOG_SWITCH = 321;
    }

    /** RPM 调试模式。 */
    interface RpmDebugValue {
        /** 无效值。 */
        int RPM_DEB_VALUE_INVALID = -1;
        String SP_NET_RPM_DBG_VALUE = "sp_net_rpm_debug_value";
    }

    /** 系统配置读写动作。 */
    interface SysConfigAction {
        int ACTION_SET_PROP = 1;
        int ACTION_GET_PROP = 2;
    }

    /** IMM 指令动作。 */
    interface SysImAction {
        int ACTION_SET_RPM_DEBUG_VALUE = 1;
        int ACTION_SET_SYS_CONFIG = 2;
        int ACTION_IM_DELETE_FILE = 3;
    }

    /** 系统属性键。 */
    interface SystemProperty {
        String MODEM_SELF = "persist.service.modem_bootST";
    }

    /** 通话相关的广播 Action 与 extra 键。 */
    interface callLogBroadcast {
        String BROADCAST_END_CALL = "com.xtc.broadcast.dial.END_CALL";
        String CALL_LOG_OTHER_CALL_ERROR = "com.xtc.i3launcher.callLog.other.call.error";
        String CALL_LOG_RETRY_UPLOAD = "com.xtc.i3launcher.ACTION.CallLog.Retry";
        String RECEIVE_CALL_DELAYGRAB_TIMEOUT = "com.xtc.call.modem.DELAYGRAB_TIMEOUT";
        String RECEIVE_CALL_FAILURE = "com.xtc.call.modem.ACTION_END_MODEM";
        String RECEIVE_CALL_LOG_TIME_OUT = "com.xtc.call.log.timeout";
        String RECEIVE_CALL_WAITMODEM_TIME_OUT = "com.xtc.call.waitModem.TIMEOUT";
        String RECEIVE_PRE_USER_CALL = "com.xtc.prepare.call";
        String REC_EXTRA_FRIEND_ID = "extra_friend_id";
        String REC_EXTRA_IS_NORMAL_CALL = "com.xtc.call.modem.extra.IS_NORMAL_CALL";
    }
}
