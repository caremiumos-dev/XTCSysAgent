package com.xtc.httplib.confupdate;

/**
 * 配置更新广播的协议常量。
 *
 * <p>服务端在任意响应头里塞 {@code Conf-Updates} / {@code Conf-Updates-V}，
 * HTTP 层解析后延迟向本应用（注意：{@code setPackage} 到本包，不跨进程）广播一个
 * {@code conf_update_type://<配置项编号>} 的 Intent，业务侧靠 data 里的编号区分
 * "该去重新拉哪份配置"。协议是服务端与客户端约定的，字符串不能改。</p>
 */
public interface HttpConfUpdate {

    /** 配置更新广播的 action。 */
    String ACTION_HTTP_CONF_UPDATE = "com.xtc.http.action.conf_update";

    /** 配置更新广播 data 的 scheme，后面直接拼配置项编号。 */
    String SCHEME_HTTP_CONF_UPDATE = "conf_update_type://";
}
