package com.xtc.im.core.common.voice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * 语音切片发送器（AIDL）：一次语音发送由调用方逐片喂数据。
 *
 * <p>descriptor 必须逐字保持 {@code com.xtc.im.core.common.voice.ISliceSender}。</p>
 *
 * <p>原厂接口名为单字母 {@code a}，Stub 为 {@code a$a}、代理为 {@code a$a$a}。</p>
 */
public interface ISliceSender extends IInterface {

    /**
     * AIDL 生成的服务端桩。
     */
    public static abstract class Stub extends Binder implements ISliceSender {

        /** 事务码：取本次发送的分组 id。 */
        static final int TRANSACTION_GET_GROUP_ID = 1;

        /** 事务码：发送一片。 */
        static final int TRANSACTION_SEND_SLICE = 2;

        /** 跨进程契约。 */
        private static final String DESCRIPTOR = "com.xtc.im.core.common.voice.ISliceSender";

        /**
         * 远端代理。
         */
        private static class Proxy implements ISliceSender {

            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            @Override
            public String getGroupId() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.remote.transact(TRANSACTION_GET_GROUP_ID, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void sendSlice(int index, byte[] slice, int vocTime, boolean isFin, String extra)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(index);
                    data.writeByteArray(slice);
                    data.writeInt(vocTime);
                    data.writeInt(isFin ? 1 : 0);
                    data.writeString(extra);
                    this.remote.transact(TRANSACTION_SEND_SLICE, data, reply, 0);
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
         * 把 IBinder 还原成接口。
         */
        public static ISliceSender asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local == null || !(local instanceof ISliceSender)) {
                return new Proxy(binder);
            }
            return (ISliceSender) local;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case TRANSACTION_GET_GROUP_ID:
                    data.enforceInterface(DESCRIPTOR);
                    String groupId = getGroupId();
                    reply.writeNoException();
                    reply.writeString(groupId);
                    return true;
                case TRANSACTION_SEND_SLICE:
                    data.enforceInterface(DESCRIPTOR);
                    sendSlice(data.readInt(), data.createByteArray(), data.readInt(),
                            data.readInt() != 0, data.readString());
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

    /** 本次语音发送的分组 id；同一段语音的所有切片都挂在它下面。 */
    String getGroupId() throws RemoteException;

    /**
     * 发送一片。
     *
     * @param index   切片下标，从 1 开始
     * @param slice   切片数据
     * @param vocTime 语音时长（毫秒）
     * @param isFin   是否为最后一片
     * @param extra   附加信息
     */
    void sendSlice(int index, byte[] slice, int vocTime, boolean isFin, String extra)
            throws RemoteException;
}
