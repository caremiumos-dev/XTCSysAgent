package com.xtc.im.core.common.response;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.response.entity.ResponseEntity;

/**
 * 一条 IM 响应的对外视图：包着具体的 {@link ResponseEntity} 和附加字节。
 *
 * <p>为什么不让上层直接用 {@link ResponseEntity}：上层要判断的是「这条响应算什么」——
 * 是推送、是登录态、还是普通成功——这些判断逻辑写在这里，而不是散落在调用方。</p>
 *
 * <p>原厂类名为 {@code PushResponse}，内部的状态码常量接口原本是单字母 {@code a}。</p>
 */
public class PushResponse implements Parcelable {

    public static final Parcelable.Creator<PushResponse> CREATOR =
            new Parcelable.Creator<PushResponse>() {
                @Override
                public PushResponse createFromParcel(Parcel parcel) {
                    return new PushResponse(parcel);
                }

                @Override
                public PushResponse[] newArray(int size) {
                    return new PushResponse[size];
                }
            };

    /** 附加字节（原厂原样透传，本类不解释其含义）。 */
    private byte[] extraData;

    /** 具体响应实体。 */
    private ResponseEntity responseEntity;

    /**
     * 状态码常量。
     *
     * <p>1..7 是原厂直接写死的七个取值，只有 1 能从
     * {@link PushResponse#isNormalSuccess()} 里看出是「普通成功」；
     * 200 / 3200 / 4200 / 5200 分别对应推送成功、两种登录状态和加密设置完成。
     * 其余按取值命名。</p>
     */
    public interface StatusCode {

        /** 普通请求成功。 */
        int SUCCESS = 1;

        int CODE_2 = 2;

        int CODE_3 = 3;

        int CODE_4 = 4;

        int CODE_5 = 5;

        int CODE_6 = 6;

        int CODE_7 = 7;

        /** 推送类响应成功（HTTP 风格的状态码）。 */
        int OK = 200;

        /** 登录响应状态码 3200。 */
        int LOGIN_3200 = 3200;

        /** 登录响应状态码 4200。 */
        int LOGIN_4200 = 4200;

        /** 加密设置响应状态码 5200。 */
        int ENCRYPT_5200 = 5200;
    }

    /**
     * 从 Parcel 还原。
     *
     * <p>readByteArray 传的是还没初始化的 null，原厂如此（只有写入时 extraData 为 null
     * 才不会炸），保留。
     */
    protected PushResponse(Parcel parcel) {
        this.responseEntity = (ResponseEntity) parcel.readValue(getClass().getClassLoader());
        parcel.readByteArray(this.extraData);
    }

    public PushResponse(ResponseEntity responseEntity, byte[] extraData) {
        this.responseEntity = responseEntity;
        this.extraData = extraData;
    }

    /**
     * 就地读取（Parcelable 的另一种反序列化约定）。
     */
    public void readFromParcel(Parcel parcel) {
        this.responseEntity = (ResponseEntity) parcel.readValue(getClass().getClassLoader());
        parcel.readByteArray(this.extraData);
    }

    public void setResponseEntity(ResponseEntity responseEntity) {
        this.responseEntity = responseEntity;
    }

    public void setExtraData(byte[] extraData) {
        this.extraData = extraData;
    }

    public byte[] getExtraData() {
        return this.extraData;
    }

    public ResponseEntity getResponseEntity() {
        return this.responseEntity;
    }

    /** 响应命令号。 */
    public int getCommand() {
        return this.responseEntity.getCommand();
    }

    /** 响应状态码。 */
    public int getStatusCode() {
        return this.responseEntity.getCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** 状态描述。 */
    public String getDesc() {
        return this.responseEntity.getDesc();
    }

    /** 请求号。 */
    public int getRid() {
        return this.responseEntity.getRid();
    }

    /**
     * 是否是「服务端主动推」的响应（不需要客户端回执）。
     *
     * <p>判据：命令是同步通知 / 语音切片 / 第三方同步通知 / 推送消息 / 心跳响应，
     * 或者状态码是 200。前五个命令压根没有对应的请求，最后一个是推送成功。
     */
    public boolean isAsyncPush() {
        int command = this.responseEntity.getCommand();
        return command == ImCommand.SYNC_INFORM
                || command == ImCommand.VOICE_SLICE
                || command == ImCommand.THIRD_SYNC_INFORM
                || command == ImCommand.PUSH_RESPONSE
                || command == ImCommand.HEART_BEAT_RESPONSE
                || this.responseEntity.getCode() == StatusCode.OK;
    }

    /** 登录响应且状态码 4200。 */
    public boolean isLoginStatus4200() {
        return this.responseEntity.getCommand() == ImCommand.LOGIN_RESPONSE
                && this.responseEntity.getCode() == StatusCode.LOGIN_4200;
    }

    /** 登录响应且状态码 3200。 */
    public boolean isLoginStatus3200() {
        return this.responseEntity.getCommand() == ImCommand.LOGIN_RESPONSE
                && this.responseEntity.getCode() == StatusCode.LOGIN_3200;
    }

    /** 加密设置响应且状态码 5200（加密协商完成）。 */
    public boolean isEncryptStatus5200() {
        return this.responseEntity.getCommand() == ImCommand.ENCRYPT_SET_RESPONSE
                && this.responseEntity.getCode() == StatusCode.ENCRYPT_5200;
    }

    /**
     * 是否普通请求成功：排除那五个主动推送命令后，状态码为 1 即成功。
     */
    public boolean isNormalSuccess() {
        int command = this.responseEntity.getCommand();
        if (command == ImCommand.SYNC_INFORM
                || command == ImCommand.VOICE_SLICE
                || command == ImCommand.THIRD_SYNC_INFORM
                || command == ImCommand.PUSH_RESPONSE
                || command == ImCommand.HEART_BEAT_RESPONSE) {
            return false;
        }
        return this.responseEntity.getCode() == StatusCode.SUCCESS;
    }

    @Override
    public String toString() {
        return "PushResponse{responseEntity=" + this.responseEntity + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeValue(this.responseEntity);
        parcel.writeByteArray(this.extraData);
    }
}
