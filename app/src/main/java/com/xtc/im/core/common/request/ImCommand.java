package com.xtc.im.core.common.request;

/**
 * IM 协议的命令号（报文首 tag 的取值）。
 *
 * <p>这批数字是**服务端契约**：每个实体类的 {@code @CommandValue}、
 * {@link CommandRegistry} 的映射表、以及服务端下发的报文首 tag 必须完全一致，
 * 错一个就会把报文解析成别的实体。</p>
 *
 * <p>名字按 {@link CommandRegistry} 里"命令号 → 实体类"的对应关系反推，
 * 数值与原厂完全一致（原厂接口名为单字母 {@code a}）。</p>
 */
public interface ImCommand {

    /** 注测请求。 */
    int REGIST_REQUEST = 1;

    /** 注测响应。 */
    int REGIST_RESPONSE = 2;

    /** 登录请求。 */
    int LOGIN_REQUEST = 3;

    /** 登录响应。 */
    int LOGIN_RESPONSE = 4;

    /** 业务账号绑定请求。 */
    int ACCOUNT_REQUEST = 5;

    /** 业务账号绑定响应。 */
    int ACCOUNT_RESPONSE = 6;

    /** 心跳请求。 */
    int HEART_BEAT_REQUEST = 7;

    /** 心跳响应。 */
    int HEART_BEAT_RESPONSE = 8;

    /** 查消息请求。 */
    int MESSAGE_REQUEST = 9;

    /** 查消息响应。 */
    int MESSAGE_RESPONSE = 10;

    /** 同步通知（服务端主动下发，无需请求）。 */
    int SYNC_INFORM = 11;

    /** 同步请求。 */
    int SYNC_REQUEST = 12;

    /** 同步响应。 */
    int SYNC_RESPONSE = 13;

    /** 同步结束（服务端主动下发）。 */
    int SYNC_FINISH = 14;

    /** 同步结束应答。 */
    int SYNC_FINISH_ACK = 15;

    /** 已读应答请求。 */
    int READ_ACK_REQUEST = 16;

    /** 已读应答响应。 */
    int READ_ACK_RESPONSE = 17;

    /** 全量设置请求。 */
    int ALL_SET_REQUEST = 18;

    /** 全量设置响应。 */
    int ALL_SET_RESPONSE = 19;

    /** HTTP 透传请求。 */
    int TRANSPOND_REQUEST = 20;

    /** HTTP 透传响应。 */
    int TRANSPOND_RESPONSE = 21;

    /** 公钥请求。 */
    int PUBLIC_KEY_REQUEST = 22;

    /** 公钥响应。 */
    int PUBLIC_KEY_RESPONSE = 23;

    /** 语音切片（服务端主动下发）。 */
    int VOICE_SLICE = 24;

    /** 触发同步请求。 */
    int SYNC_TRIGGER_REQUEST = 25;

    /** 触发同步响应。 */
    int SYNC_TRIGGER_RESPONSE = 26;

    /** 消息合并请求。 */
    int MESSAGE_MERGE_REQUEST = 29;

    /** 清消息请求。 */
    int CLEAR_MSG_REQUEST = 30;

    /** 清消息响应。 */
    int CLEAR_MSG_RESPONSE = 31;

    /** 只有一段裸数据、没有专门实体的命令（{@link RawPayloadEntity}）。 */
    int RAW_PAYLOAD = 32;

    /** 加密设置请求。 */
    int ENCRYPT_SET_REQUEST = 33;

    /** 加密设置响应。 */
    int ENCRYPT_SET_RESPONSE = 34;

    /** 推送消息（服务端主动下发）。 */
    int PUSH_RESPONSE = 35;

    /** 推送消息应答。 */
    int PUSH_RESPONSE_ACK = 36;

    /** 单条消息请求。 */
    int SINGLE_MESSAGE_REQUEST = 40;

    /** 消息合并响应。 */
    int MESSAGE_MERGE_RESPONSE = 41;

    /** 第三方 alias/tag 设置请求。 */
    int ALIAS_AND_TAG_REQUEST = 100;

    /** 第三方 alias/tag 设置响应。 */
    int ALIAS_AND_TAG_RESPONSE = 101;

    /** 第三方触发同步请求。 */
    int THIRD_SYNC_TRIGGER_REQUEST = 110;

    /** 第三方触发同步响应。 */
    int THIRD_SYNC_TRIGGER_RESPONSE = 111;

    /** 第三方同步通知（服务端主动下发）。 */
    int THIRD_SYNC_INFORM = 112;

    /** 第三方同步请求。 */
    int THIRD_SYNC_REQUEST = 113;

    /** 第三方同步响应。 */
    int THIRD_SYNC_RESPONSE = 114;

    /** 第三方同步结束（服务端主动下发）。 */
    int THIRD_SYNC_FIN = 115;

    /** 第三方同步结束应答。 */
    int THIRD_SYNC_FIN_ACK = 116;

    /** 通用错误响应。 */
    int ERROR = 1000;
}
