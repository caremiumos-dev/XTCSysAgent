package com.xtc.im.core.app;

/**
 * IM Core 的进程名、广播 action 与数据后缀常量。
 *
 * <p>这些字符串是**跨进程契约**：IM 核心服务与宿主各自在自己进程里注册同名 action，
 * 一个字符都不能改，否则广播收不到（且不会报错，只会静默失效）。</p>
 *
 * <p>原厂类名为单字母 {@code a}。</p>
 */
public interface ImCoreConstants {

    /** IM Core 的包名 / 进程标识。 */
    String CORE_PACKAGE_NAME = "com.xtc.im.core";

    /** 网络已连接。 */
    String ACTION_NETWORK_CONNECTED = "com.xtc.im.core.network.connected";

    /** 设备关机。 */
    String ACTION_DEVICE_SHUTDOWN = "com.xtc.im.core.device.shutdown";

    /** 可以读数据了。 */
    String ACTION_READ_DATA = "com.xtc.im.core.read_data";

    /** 长连接已建立。 */
    String ACTION_CONNECTED = "com.xtc.im.core.connected";

    /** 长连接已断开。 */
    String ACTION_DISCONNECTED = "com.xtc.im.core.disconnected";

    /** 正在连接。 */
    String ACTION_CONNECTING = "com.xtc.im.core.connecting";

    /** 推送信息变更。 */
    String ACTION_PUSH_INFO_CHANGED = "com.xtc.im.core.push_info_changed";

    /** 连接失败。 */
    String ACTION_CONNECT_FAIL = "com.xtc.im.core.connect_fail";

    /** 触发重连。 */
    String ACTION_RECONNECT = "com.xtc.im.core.reconnect";

    /** 心跳。 */
    String ACTION_HEARTBEAT = "com.xtc.im.core.heartbeat";

    /** 启动 IM Core。 */
    String ACTION_START = "com.xtc.im.core.start";

    /** 停止连接服务。 */
    String ACTION_STOP_CONN_SERVICE = "com.xtc.im.core.stop_conn_service";

    /** 检查推送宿主服务。 */
    String ACTION_PUSH_HOST_SERVICE_CHECK = "com.xtc.im.core.push.host_service.check";

    /** 采集推送信息。 */
    String ACTION_PUSH_INFO_COLLECTION = "com.xtc.im.core.push.info.collection";

    /** 杀掉推送进程。 */
    String ACTION_KILL_PUSH_PROCESS = "com.xtc.im.core.kill.push_process";

    /** 服务端错误。 */
    String ACTION_SERVER_ERROR = "com.xtc.im.core.server_error";

    /** 推送进程崩溃。 */
    String ACTION_CRASH_ON_PUSH = "com.xtc.im.core.crash.on_push";

    /** UDP 初始化（注意这个不是完整包名的 action）。 */
    String ACTION_UDP_INIT = "udp_init";

    /** UDP 初始化应答。 */
    String ACTION_UDP_INIT_RESP = "udp_init_resp";

    /** 同步响应到达。 */
    String ACTION_SYNC_RESPONSE = "com.xtc.im.core.sync_response";

    /** IM 数据文件的后缀。 */
    String IM_DATA_SUFFIX = ".im.data";
}
