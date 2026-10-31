package com.xtc.im.core.common.request;

import com.xtc.im.core.common.request.entity.AccountRequestEntity;
import com.xtc.im.core.common.request.entity.AllSetRequestEntity;
import com.xtc.im.core.common.request.entity.ClearMsgRequestEntity;
import com.xtc.im.core.common.request.entity.EncryptSetRequestEntity;
import com.xtc.im.core.common.request.entity.HeartBeatRequestEntity;
import com.xtc.im.core.common.request.entity.LoginRequestEntity;
import com.xtc.im.core.common.request.entity.MessageMergeRequestEntity;
import com.xtc.im.core.common.request.entity.MessageRequestEntity;
import com.xtc.im.core.common.request.entity.PublicKeyRequestEntity;
import com.xtc.im.core.common.request.entity.PushResponseAckRequestEntity;
import com.xtc.im.core.common.request.entity.ReadAckRequestEntity;
import com.xtc.im.core.common.request.entity.RegistRequestEntity;
import com.xtc.im.core.common.request.entity.SingleMessageRequestEntity;
import com.xtc.im.core.common.request.entity.SyncFinishAckRequestEntity;
import com.xtc.im.core.common.request.entity.SyncRequestEntity;
import com.xtc.im.core.common.request.entity.SyncTriggerRequestEntity;
import com.xtc.im.core.common.request.entity.TranspondRequestEntity;
import com.xtc.im.core.common.request.entity.third.AliasAndTagRequestEntity;
import com.xtc.im.core.common.request.entity.third.ThirdSyncFinAckRequestEntity;
import com.xtc.im.core.common.request.entity.third.ThirdSyncRequestEntity;
import com.xtc.im.core.common.request.entity.third.ThirdSyncTriggerRequestEntity;
import com.xtc.im.core.common.response.entity.AccountResponseEntity;
import com.xtc.im.core.common.response.entity.AllSetResponseEntity;
import com.xtc.im.core.common.response.entity.ClearMsgResponseEntity;
import com.xtc.im.core.common.response.entity.EncryptSetResponseEntity;
import com.xtc.im.core.common.response.entity.ErrorResponseEntity;
import com.xtc.im.core.common.response.entity.HeartBeatResponseEntity;
import com.xtc.im.core.common.response.entity.LoginResponseEntity;
import com.xtc.im.core.common.response.entity.MessageMergeResponseEntity;
import com.xtc.im.core.common.response.entity.MessageResponseEntity;
import com.xtc.im.core.common.response.entity.PublicKeyResponseEntity;
import com.xtc.im.core.common.response.entity.PushResponseEntity;
import com.xtc.im.core.common.response.entity.ReadAckResponseEntity;
import com.xtc.im.core.common.response.entity.RegistResponseEntity;
import com.xtc.im.core.common.response.entity.SyncFinishResponseEntity;
import com.xtc.im.core.common.response.entity.SyncInformResponseEntity;
import com.xtc.im.core.common.response.entity.SyncResponseEntity;
import com.xtc.im.core.common.response.entity.SyncTriggerResponseEntity;
import com.xtc.im.core.common.response.entity.TranspondResponseEntity;
import com.xtc.im.core.common.response.entity.VoiceSliceResponseEntity;
import com.xtc.im.core.common.response.entity.third.AliasAndTagResponseEntity;
import com.xtc.im.core.common.response.entity.third.ThirdSyncFinResponseEntity;
import com.xtc.im.core.common.response.entity.third.ThirdSyncInformResponseEntity;
import com.xtc.im.core.common.response.entity.third.ThirdSyncResponseEntity;
import com.xtc.im.core.common.response.entity.third.ThirdSyncTriggerResponseEntity;
import java.util.HashMap;
import java.util.Map;

/**
 * 命令号 ↔ 实体类的两张映射表。
 *
 * <p>{@link #commandClasses} 用于解码：报文首 tag 是命令号，直接查表 new 出对应实体。
 * 没有这张表就得写一堆 switch，而且新增命令要改两处。</p>
 *
 * <p>{@link #COMMAND_RESPONSES} 用于"请求 → 期望的响应"配对：例如发命令 1（注测）
 * 就等命令 2（注测响应）。同一个响应可能对应多个请求，所以是 Map 而不是双向表。</p>
 *
 * <p>表里所有命令号都用 {@link ImCommand} 的常量表达，数值与原厂一致。
 * 原厂类名为单字母 {@code h}。</p>
 */
public final class CommandRegistry {

    /** 命令号 → 实体类。包内可见，EntityCodec 解码时直接查。 */
    static final Map<Integer, Class<?>> commandClasses = new HashMap<Integer, Class<?>>();

    /** 请求命令号 → 响应的命令号。 */
    public static final Map<Integer, Integer> COMMAND_RESPONSES = new HashMap<Integer, Integer>();

    static {
        commandClasses.put(ImCommand.REGIST_REQUEST, RegistRequestEntity.class);
        commandClasses.put(ImCommand.REGIST_RESPONSE, RegistResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.REGIST_REQUEST, ImCommand.REGIST_RESPONSE);

        commandClasses.put(ImCommand.LOGIN_REQUEST, LoginRequestEntity.class);
        commandClasses.put(ImCommand.LOGIN_RESPONSE, LoginResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.LOGIN_REQUEST, ImCommand.LOGIN_RESPONSE);

        commandClasses.put(ImCommand.ACCOUNT_REQUEST, AccountRequestEntity.class);
        commandClasses.put(ImCommand.ACCOUNT_RESPONSE, AccountResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.ACCOUNT_REQUEST, ImCommand.ACCOUNT_RESPONSE);

        commandClasses.put(ImCommand.HEART_BEAT_REQUEST, HeartBeatRequestEntity.class);
        commandClasses.put(ImCommand.HEART_BEAT_RESPONSE, HeartBeatResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.HEART_BEAT_REQUEST, ImCommand.HEART_BEAT_RESPONSE);

        commandClasses.put(ImCommand.MESSAGE_REQUEST, MessageRequestEntity.class);
        commandClasses.put(ImCommand.MESSAGE_RESPONSE, MessageResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.MESSAGE_REQUEST, ImCommand.MESSAGE_RESPONSE);

        commandClasses.put(ImCommand.SYNC_INFORM, SyncInformResponseEntity.class);
        commandClasses.put(ImCommand.SYNC_REQUEST, SyncRequestEntity.class);
        commandClasses.put(ImCommand.SYNC_RESPONSE, SyncResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.SYNC_REQUEST, ImCommand.SYNC_RESPONSE);

        commandClasses.put(ImCommand.SYNC_FINISH, SyncFinishResponseEntity.class);
        commandClasses.put(ImCommand.SYNC_FINISH_ACK, SyncFinishAckRequestEntity.class);
        commandClasses.put(ImCommand.PUSH_RESPONSE_ACK, PushResponseAckRequestEntity.class);

        commandClasses.put(ImCommand.READ_ACK_REQUEST, ReadAckRequestEntity.class);
        commandClasses.put(ImCommand.READ_ACK_RESPONSE, ReadAckResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.READ_ACK_REQUEST, ImCommand.READ_ACK_RESPONSE);

        commandClasses.put(ImCommand.ALL_SET_REQUEST, AllSetRequestEntity.class);
        commandClasses.put(ImCommand.ALL_SET_RESPONSE, AllSetResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.ALL_SET_REQUEST, ImCommand.ALL_SET_RESPONSE);

        commandClasses.put(ImCommand.TRANSPOND_REQUEST, TranspondRequestEntity.class);
        commandClasses.put(ImCommand.TRANSPOND_RESPONSE, TranspondResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.TRANSPOND_REQUEST, ImCommand.TRANSPOND_RESPONSE);

        commandClasses.put(ImCommand.PUBLIC_KEY_REQUEST, PublicKeyRequestEntity.class);
        commandClasses.put(ImCommand.PUBLIC_KEY_RESPONSE, PublicKeyResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.PUBLIC_KEY_REQUEST, ImCommand.PUBLIC_KEY_RESPONSE);

        commandClasses.put(ImCommand.VOICE_SLICE, VoiceSliceResponseEntity.class);

        commandClasses.put(ImCommand.SYNC_TRIGGER_REQUEST, SyncTriggerRequestEntity.class);
        commandClasses.put(ImCommand.SYNC_TRIGGER_RESPONSE, SyncTriggerResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.SYNC_TRIGGER_REQUEST, ImCommand.SYNC_TRIGGER_RESPONSE);

        commandClasses.put(ImCommand.CLEAR_MSG_REQUEST, ClearMsgRequestEntity.class);
        commandClasses.put(ImCommand.CLEAR_MSG_RESPONSE, ClearMsgResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.CLEAR_MSG_REQUEST, ImCommand.CLEAR_MSG_RESPONSE);

        commandClasses.put(ImCommand.RAW_PAYLOAD, RawPayloadEntity.class);

        commandClasses.put(ImCommand.ENCRYPT_SET_REQUEST, EncryptSetRequestEntity.class);
        commandClasses.put(ImCommand.ENCRYPT_SET_RESPONSE, EncryptSetResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.ENCRYPT_SET_REQUEST, ImCommand.ENCRYPT_SET_RESPONSE);

        commandClasses.put(ImCommand.PUSH_RESPONSE, PushResponseEntity.class);

        commandClasses.put(ImCommand.ALIAS_AND_TAG_REQUEST, AliasAndTagRequestEntity.class);
        commandClasses.put(ImCommand.ALIAS_AND_TAG_RESPONSE, AliasAndTagResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.ALIAS_AND_TAG_REQUEST, ImCommand.ALIAS_AND_TAG_RESPONSE);

        commandClasses.put(ImCommand.THIRD_SYNC_REQUEST, ThirdSyncRequestEntity.class);
        commandClasses.put(ImCommand.THIRD_SYNC_RESPONSE, ThirdSyncResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.THIRD_SYNC_REQUEST, ImCommand.THIRD_SYNC_RESPONSE);

        commandClasses.put(ImCommand.THIRD_SYNC_TRIGGER_REQUEST, ThirdSyncTriggerRequestEntity.class);
        commandClasses.put(ImCommand.THIRD_SYNC_TRIGGER_RESPONSE, ThirdSyncTriggerResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.THIRD_SYNC_TRIGGER_REQUEST,
                ImCommand.THIRD_SYNC_TRIGGER_RESPONSE);

        commandClasses.put(ImCommand.THIRD_SYNC_FIN_ACK, ThirdSyncFinAckRequestEntity.class);
        commandClasses.put(ImCommand.THIRD_SYNC_FIN, ThirdSyncFinResponseEntity.class);
        commandClasses.put(ImCommand.THIRD_SYNC_INFORM, ThirdSyncInformResponseEntity.class);

        commandClasses.put(ImCommand.SINGLE_MESSAGE_REQUEST, SingleMessageRequestEntity.class);
        // 单条消息发送的响应复用查消息的响应（命令 10），不是命令 41。
        COMMAND_RESPONSES.put(ImCommand.SINGLE_MESSAGE_REQUEST, ImCommand.MESSAGE_RESPONSE);

        commandClasses.put(ImCommand.MESSAGE_MERGE_REQUEST, MessageMergeRequestEntity.class);
        commandClasses.put(ImCommand.MESSAGE_MERGE_RESPONSE, MessageMergeResponseEntity.class);
        COMMAND_RESPONSES.put(ImCommand.MESSAGE_MERGE_REQUEST, ImCommand.MESSAGE_MERGE_RESPONSE);

        commandClasses.put(ImCommand.ERROR, ErrorResponseEntity.class);
    }

    private CommandRegistry() {
    }
}
