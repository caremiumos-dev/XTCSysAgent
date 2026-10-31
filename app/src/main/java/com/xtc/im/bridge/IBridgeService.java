package com.xtc.im.bridge;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.xtc.im.transpond.TranspondCallback;
import com.xtc.im.core.common.listener.ImMessageListener;
import com.xtc.im.core.common.listener.OnFinishListener;
import com.xtc.im.core.common.voice.ISliceSender;
import com.xtc.im.core.common.voice.entity.VoiceDescEntity;

/**
 * IM 桥接服务（AIDL）：宿主（本 APK）与 IM 核心进程之间的唯一入口。
 *
 * <p>九个事务分成四组：</p>
 * <ol>
 *   <li>事务 1、2 —— HTTP 透传与数据读取；</li>
 *   <li>事务 3、5 与 4、6 —— 发消息（裸数据 / 语音描述），两组参数完全一致，
 *       是原厂同一业务的两条通道（{@code a}/{@code b}），这里按事务码还原成
 *       {@code xxx} 与 {@code xxxEx}，参数与顺序一字未改；</li>
 *   <li>事务 7、8 —— 取语音切片发送器；</li>
 *   <li>事务 9 —— 释放。</li>
 * </ol>
 *
 * <p>descriptor 必须逐字保持 {@code com.xtc.im.aidl.IBridgeService}。</p>
 *
 * <p>原厂接口名为单字母 {@code a}，Stub 为 {@code a$a}、代理为 {@code a$a$a}。</p>
 */
public interface IBridgeService extends IInterface {

    /**
     * AIDL 生成的服务端桩。
     *
     * <p>本地实现（IM 进程）继承它即可；{@link Proxy} 负责远端调用。</p>
     */
    public static abstract class Stub extends Binder implements IBridgeService {

        /** 事务码：HTTP 透传。 */
        static final int TRANSACTION_TRANSPOND = 1;

        /** 事务码：读数据。 */
        static final int TRANSACTION_GET_DATA = 2;

        /** 事务码：发消息（裸数据）。 */
        static final int TRANSACTION_SEND_MESSAGE = 3;

        /** 事务码：发消息（语音描述）。 */
        static final int TRANSACTION_SEND_VOICE_MESSAGE = 4;

        /** 事务码：发消息（裸数据，第二条通道）。 */
        static final int TRANSACTION_SEND_MESSAGE_EX = 5;

        /** 事务码：发消息（语音描述，第二条通道）。 */
        static final int TRANSACTION_SEND_VOICE_MESSAGE_EX = 6;

        /** 事务码：取切片发送器。 */
        static final int TRANSACTION_CREATE_SLICE_SENDER = 7;

        /** 事务码：取切片发送器（第二条通道）。 */
        static final int TRANSACTION_CREATE_SLICE_SENDER_EX = 8;

        /** 事务码：释放。 */
        static final int TRANSACTION_RELEASE = 9;

        /** 跨进程契约。 */
        private static final String DESCRIPTOR = "com.xtc.im.aidl.IBridgeService";

        /**
         * 远端代理：把九个方法翻译成 Binder 事务。
         */
        private static class Proxy implements IBridgeService {

            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            @Override
            public boolean transpond(String url, int port, byte[] body, byte[] header,
                                     TranspondCallback callback) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(url);
                    data.writeInt(port);
                    data.writeByteArray(body);
                    data.writeByteArray(header);
                    data.writeStrongBinder(toBinder(callback));
                    this.remote.transact(TRANSACTION_TRANSPOND, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public byte[] getData(String key, int length) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(key);
                    data.writeInt(length);
                    this.remote.transact(TRANSACTION_GET_DATA, data, reply, 0);
                    reply.readException();
                    return reply.createByteArray();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void sendMessage(long dialogId, int msgType, String msgId, byte[] payload,
                                    int contentType, ImMessageListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(dialogId);
                    data.writeInt(msgType);
                    data.writeString(msgId);
                    data.writeByteArray(payload);
                    data.writeInt(contentType);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.remote.transact(TRANSACTION_SEND_MESSAGE, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void sendVoiceMessage(long dialogId, String msgId, VoiceDescEntity desc,
                                         int contentType, ImMessageListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(dialogId);
                    data.writeString(msgId);
                    writeVoiceDesc(data, desc);
                    data.writeInt(contentType);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.remote.transact(TRANSACTION_SEND_VOICE_MESSAGE, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void sendMessageEx(long dialogId, int msgType, String msgId, byte[] payload,
                                      int contentType, ImMessageListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(dialogId);
                    data.writeInt(msgType);
                    data.writeString(msgId);
                    data.writeByteArray(payload);
                    data.writeInt(contentType);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.remote.transact(TRANSACTION_SEND_MESSAGE_EX, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void sendVoiceMessageEx(long dialogId, String msgId, VoiceDescEntity desc,
                                           int contentType, ImMessageListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(dialogId);
                    data.writeString(msgId);
                    writeVoiceDesc(data, desc);
                    data.writeInt(contentType);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.remote.transact(TRANSACTION_SEND_VOICE_MESSAGE_EX, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public ISliceSender createSliceSender(long dialogId, OnFinishListener listener)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(dialogId);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.remote.transact(TRANSACTION_CREATE_SLICE_SENDER, data, reply, 0);
                    reply.readException();
                    return ISliceSender.Stub.asInterface(reply.readStrongBinder());
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public ISliceSender createSliceSenderEx(long dialogId, OnFinishListener listener)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(dialogId);
                    data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.remote.transact(TRANSACTION_CREATE_SLICE_SENDER_EX, data, reply, 0);
                    reply.readException();
                    return ISliceSender.Stub.asInterface(reply.readStrongBinder());
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void release() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.remote.transact(TRANSACTION_RELEASE, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public IBinder asBinder() {
                return this.remote;
            }

            /** 本端接口的描述符（AIDL 生成的辅助方法）。 */
            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        /**
         * 把 IBinder 还原成接口：本地对象直接返回自己，远端对象包一层 {@link Proxy}。
         */
        public static IBridgeService asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local == null || !(local instanceof IBridgeService)) {
                return new Proxy(binder);
            }
            return (IBridgeService) local;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case TRANSACTION_TRANSPOND:
                    data.enforceInterface(DESCRIPTOR);
                    boolean transponded = transpond(data.readString(), data.readInt(),
                            data.createByteArray(), data.createByteArray(),
                            TranspondCallbackBinder.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(transponded ? 1 : 0);
                    return true;
                case TRANSACTION_GET_DATA:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] result = getData(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeByteArray(result);
                    return true;
                case TRANSACTION_SEND_MESSAGE:
                    data.enforceInterface(DESCRIPTOR);
                    sendMessage(data.readLong(), data.readInt(), data.readString(),
                            data.createByteArray(), data.readInt(),
                            ImMessageListener.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case TRANSACTION_SEND_VOICE_MESSAGE:
                    data.enforceInterface(DESCRIPTOR);
                    sendVoiceMessage(data.readLong(), data.readString(), readVoiceDesc(data),
                            data.readInt(),
                            ImMessageListener.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case TRANSACTION_SEND_MESSAGE_EX:
                    data.enforceInterface(DESCRIPTOR);
                    sendMessageEx(data.readLong(), data.readInt(), data.readString(),
                            data.createByteArray(), data.readInt(),
                            ImMessageListener.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case TRANSACTION_SEND_VOICE_MESSAGE_EX:
                    data.enforceInterface(DESCRIPTOR);
                    sendVoiceMessageEx(data.readLong(), data.readString(), readVoiceDesc(data),
                            data.readInt(),
                            ImMessageListener.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                case TRANSACTION_CREATE_SLICE_SENDER:
                    data.enforceInterface(DESCRIPTOR);
                    ISliceSender sender = createSliceSender(data.readLong(),
                            OnFinishListener.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeStrongBinder(sender != null ? sender.asBinder() : null);
                    return true;
                case TRANSACTION_CREATE_SLICE_SENDER_EX:
                    data.enforceInterface(DESCRIPTOR);
                    ISliceSender senderEx = createSliceSenderEx(data.readLong(),
                            OnFinishListener.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeStrongBinder(senderEx != null ? senderEx.asBinder() : null);
                    return true;
                case TRANSACTION_RELEASE:
                    data.enforceInterface(DESCRIPTOR);
                    release();
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    /**
     * 透传回调在 **IM 进程侧** 的形态。
     *
     * <p>为什么要单独有这么一个类：工程内（宿主侧）的
     * {@link TranspondCallback} 已经被简化成单方法 {@code onTranspondFinished()}，
     * 但原厂的跨进程契约是
     * {@code com.xtc.im.transpond.ITranspondCallback}，带 {@code onSuccess(byte[], int)} /
     * {@code onError(String)} 两个事务，而且**回调是宿主实现、IM 进程调用**。
     * 宿主那边（{@code PreprocessorInterceptor.TranspondListener}）已经把这两个事务
     * 自包含地还原好了，所以 IM 进程侧只要有对称的代理即可，两个事务码与
     * "成功事务要把 byte[] 原样回写"的收尾顺序完全照搬原厂 AIDL。</p>
     */
    public static class TranspondCallbackBinder implements TranspondCallback {

        /** 跨进程契约（与原厂 AIDL 一致）。 */
        private static final String DESCRIPTOR = "com.xtc.im.transpond.ITranspondCallback";

        /** 事务码：成功。 */
        private static final int TRANSACTION_ON_SUCCESS = 1;

        /** 事务码：失败。 */
        private static final int TRANSACTION_ON_ERROR = 2;

        private final IBinder remote;

        TranspondCallbackBinder(IBinder remote) {
            this.remote = remote;
        }

        /**
         * 把 IBinder 还原成回调：本地对象（宿主把回调放在同一个进程里时）直接返回，
         * 否则包一层远端代理。
         */
        public static TranspondCallback asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local instanceof TranspondCallback) {
                return (TranspondCallback) local;
            }
            return new TranspondCallbackBinder(binder);
        }

        /**
         * 简化契约的唯一方法。
         *
         * <p>原厂这里没有对应方法，是上级为了工程内类型统一加的；**刻意留空**，
         * 真正的结果仍然走下面的 {@link #onSuccess(byte[], int)} /
         * {@link #onError(String)} 两个 Binder 事务。</p>
         */
        @Override
        public void onTranspondFinished() {
        }

        /**
         * 通知宿主透传成功。
         *
         * @param data 响应体（宿主会把它读回来，所以这里是双向的）
         * @param code 附加状态码
         */
        public void onSuccess(byte[] data, int code) throws RemoteException {
            Parcel parcel = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                parcel.writeInterfaceToken(DESCRIPTOR);
                parcel.writeByteArray(data);
                parcel.writeInt(code);
                this.remote.transact(TRANSACTION_ON_SUCCESS, parcel, reply, 0);
                reply.readException();
                reply.readByteArray(data);
            } finally {
                reply.recycle();
                parcel.recycle();
            }
        }

        /**
         * 通知宿主透传失败。
         */
        public void onError(String message) throws RemoteException {
            Parcel parcel = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                parcel.writeInterfaceToken(DESCRIPTOR);
                parcel.writeString(message);
                this.remote.transact(TRANSACTION_ON_ERROR, parcel, reply, 0);
                reply.readException();
            } finally {
                reply.recycle();
                parcel.recycle();
            }
        }
    }

    /**
     * 发起一次 HTTP 透传。
     *
     * <p>参数按位置对应原厂 AIDL：第 3 个是**头 JSON 字节**、第 4 个是**加密后的 body**
     * （与 {@code TranspondAdapter.transpond} 的约定一致，见调用方注释）。</p>
     *
     * @return 是否成功交给 IM 通道
     */
    boolean transpond(String url, int port, byte[] body, byte[] header, TranspondCallback callback)
            throws RemoteException;

    /**
     * 读一段 IM 侧的数据。
     *
     * @param key    数据标识
     * @param length 期望长度
     */
    byte[] getData(String key, int length) throws RemoteException;

    /**
     * 发一条消息（裸数据）。
     */
    void sendMessage(long dialogId, int msgType, String msgId, byte[] payload, int contentType,
                     ImMessageListener listener) throws RemoteException;

    /**
     * 发一条消息（语音描述）。
     */
    void sendVoiceMessage(long dialogId, String msgId, VoiceDescEntity desc, int contentType,
                          ImMessageListener listener) throws RemoteException;

    /**
     * 发一条消息（裸数据），第二条通道：参数与 {@link #sendMessage} 完全一致，
     * 原厂用同名重载区分，事务码为 5。
     */
    void sendMessageEx(long dialogId, int msgType, String msgId, byte[] payload, int contentType,
                       ImMessageListener listener) throws RemoteException;

    /**
     * 发一条消息（语音描述），第二条通道，事务码为 6。
     */
    void sendVoiceMessageEx(long dialogId, String msgId, VoiceDescEntity desc, int contentType,
                            ImMessageListener listener) throws RemoteException;

    /**
     * 取一个语音切片发送器。
     */
    ISliceSender createSliceSender(long dialogId, OnFinishListener listener) throws RemoteException;

    /**
     * 取一个语音切片发送器，第二条通道，事务码为 8。
     */
    ISliceSender createSliceSenderEx(long dialogId, OnFinishListener listener) throws RemoteException;

    /** 释放。 */
    void release() throws RemoteException;

    /**
     * 把简化后的回调还原成 Binder：真实运行期回调对象
     * （{@code PreprocessorInterceptor.TranspondListener}）本身就 extends {@link Binder}，
     * 所以这里能拿到它的 Binder；拿不到时按原厂写法写 null，事务布局不变。
     */
    static IBinder toBinder(TranspondCallback callback) {
        return callback instanceof IBinder ? (IBinder) callback : null;
    }

    /**
     * 写语音描述：先写 1/0 标志，再写 Parcelable 本体（原厂 AIDL 的固定写法）。
     */
    static void writeVoiceDesc(Parcel parcel, VoiceDescEntity desc) {
        if (desc != null) {
            parcel.writeInt(1);
            desc.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
    }

    /**
     * 读语音描述：与 {@link #writeVoiceDesc} 对称。
     */
    static VoiceDescEntity readVoiceDesc(Parcel parcel) {
        return parcel.readInt() != 0 ? VoiceDescEntity.CREATOR.createFromParcel(parcel) : null;
    }
}
