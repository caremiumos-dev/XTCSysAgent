package com.xtc.im.core.common.listener;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * 消息投递结果回执（AIDL）。IM 进程每把一条消息写进 socket 或从服务端拿到应答，
 * 都会反向调用宿主这边的这个接口一次。
 *
 * <p>descriptor 是跨进程契约，必须逐字保持
 * {@code com.xtc.im.core.common.listener.IMessageListener}。</p>
 *
 * <p>原厂接口名为单字母 {@code a}，Stub 为 {@code a$a}、代理为 {@code a$a$a}。</p>
 */
public interface ImMessageListener extends IInterface {

    /**
     * AIDL 生成的服务端桩：本地实现继承它，远端调用由 {@link Proxy} 转发。
     */
    public static abstract class Stub extends Binder implements ImMessageListener {

        /** 事务码：onMessageSent。 */
        static final int TRANSACTION_ON_MESSAGE_SENT = 1;

        /** 跨进程契约。 */
        private static final String DESCRIPTOR = "com.xtc.im.core.common.listener.IMessageListener";

        /**
         * 远端代理：把方法调用翻译成 Binder 事务。
         */
        private static class Proxy implements ImMessageListener {

            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            /** 本端接口的描述符（AIDL 生成的辅助方法）。 */
            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public IBinder asBinder() {
                return this.remote;
            }

            @Override
            public void onMessageSent(boolean success, boolean needResponse, int msgType,
                                      long syncKey, String msgId) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(success ? 1 : 0);
                    data.writeInt(needResponse ? 1 : 0);
                    data.writeInt(msgType);
                    data.writeLong(syncKey);
                    data.writeString(msgId);
                    this.remote.transact(TRANSACTION_ON_MESSAGE_SENT, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        /**
         * 把 IBinder 还原成接口：本地对象直接返回自己，远端对象包一层 {@link Proxy}。
         */
        public static ImMessageListener asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local == null || !(local instanceof ImMessageListener)) {
                return new Proxy(binder);
            }
            return (ImMessageListener) local;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case TRANSACTION_ON_MESSAGE_SENT:
                    data.enforceInterface(DESCRIPTOR);
                    onMessageSent(data.readInt() != 0, data.readInt() != 0, data.readInt(),
                            data.readLong(), data.readString());
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
     * 一条消息的投递结果。
     *
     * @param success      是否投递成功
     * @param needResponse 本次投递是否还需要服务端应答
     * @param msgType      消息类型
     * @param syncKey      同步位点
     * @param msgId        消息 id
     */
    void onMessageSent(boolean success, boolean needResponse, int msgType, long syncKey, String msgId)
            throws RemoteException;
}
