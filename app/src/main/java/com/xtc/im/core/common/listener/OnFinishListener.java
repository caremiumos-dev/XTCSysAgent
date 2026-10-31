package com.xtc.im.core.common.listener;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * 语音切片发送的完成回调（AIDL）。
 *
 * <p>三个事务在字节码里分别对应 {@code a(long, String)}、{@code a()}、{@code a(String)}；
 * 结合调用点（先拿到 {@code ISliceSender} 逐片发送，发送方再等这里回调）可确定语义：
 * 事务 1 = 单片进度、事务 2 = 全部发完、事务 3 = 出错。方法名按此推断，
 * 事务码顺序与参数类型保持原样。</p>
 *
 * <p>descriptor 必须逐字保持
 * {@code com.xtc.im.core.common.listener.OnFinishListener}。</p>
 *
 * <p>原厂接口名为单字母 {@code b}，Stub 为 {@code b$a}、代理为 {@code b$a$a}。</p>
 */
public interface OnFinishListener extends IInterface {

    /**
     * AIDL 生成的服务端桩。
     */
    public static abstract class Stub extends Binder implements OnFinishListener {

        /** 事务码：单片进度。 */
        static final int TRANSACTION_ON_SLICE = 1;

        /** 事务码：全部发完。 */
        static final int TRANSACTION_ON_FINISH = 2;

        /** 事务码：出错。 */
        static final int TRANSACTION_ON_ERROR = 3;

        /** 跨进程契约。 */
        private static final String DESCRIPTOR = "com.xtc.im.core.common.listener.OnFinishListener";

        /**
         * 远端代理。
         */
        private static class Proxy implements OnFinishListener {

            private final IBinder remote;

            Proxy(IBinder remote) {
                this.remote = remote;
            }

            @Override
            public void onFinish() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.remote.transact(TRANSACTION_ON_FINISH, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void onSlice(long sentBytes, String resourceKey) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeLong(sentBytes);
                    data.writeString(resourceKey);
                    this.remote.transact(TRANSACTION_ON_SLICE, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void onError(String message) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(message);
                    this.remote.transact(TRANSACTION_ON_ERROR, data, reply, 0);
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
        public static OnFinishListener asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local == null || !(local instanceof OnFinishListener)) {
                return new Proxy(binder);
            }
            return (OnFinishListener) local;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case TRANSACTION_ON_SLICE:
                    data.enforceInterface(DESCRIPTOR);
                    onSlice(data.readLong(), data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_ON_FINISH:
                    data.enforceInterface(DESCRIPTOR);
                    onFinish();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_ON_ERROR:
                    data.enforceInterface(DESCRIPTOR);
                    onError(data.readString());
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

    /** 全部切片发完。 */
    void onFinish() throws RemoteException;

    /**
     * 单片发送进度。
     *
     * @param sentBytes   已发送字节数
     * @param resourceKey 语音资源键
     */
    void onSlice(long sentBytes, String resourceKey) throws RemoteException;

    /** 发送出错。 */
    void onError(String message) throws RemoteException;
}
