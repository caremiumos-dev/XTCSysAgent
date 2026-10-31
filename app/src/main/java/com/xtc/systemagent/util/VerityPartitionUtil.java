package com.xtc.systemagent.util;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;

/**
 * 走 Binder 调用系统的 "xtcpartitionverify" 服务，删除 verity 分区上的文件。
 *
 * <p>这是一个私有服务，只能通过 {@code ServiceManager.getService} 反射拿到；
 * 拿不到时抛 {@link RemoteException}，由调用方决定是否降级。</p>
 */
public class VerityPartitionUtil {

    private static final String TAG = "VerityPartitionUtil";

    /** 服务名 / Binder 描述符。 */
    private static final String DESCRIPTOR = "xtcpartitionverify";

    /** 事务码：删除目录下的文件。 */
    private static final int TRANSACTION_RemoveDirFile = 102;

    private static VerityPartitionUtil sInstance;

    private IBinder mService;

    private VerityPartitionUtil(IBinder service) {
        this.mService = service;
    }

    /**
     * 取单例。
     *
     * @throws RemoteException 系统里没有这个服务
     */
    public static VerityPartitionUtil getInstance() throws RemoteException {
        IBinder service = null;
        try {
            service = getXtcPartitionVerifyService();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }
        if (service == null) {
            throw new RemoteException("missing VerityPartition Service");
        }
        if (sInstance == null) {
            sInstance = new VerityPartitionUtil(service);
        }
        return sInstance;
    }

    /** 反射从 ServiceManager 拿 Binder。 */
    @SuppressLint({"PrivateApi"})
    private static IBinder getXtcPartitionVerifyService() {
        try {
            return (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class)
                    .invoke(null, DESCRIPTOR);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除指定路径。
     *
     * @return 服务端返回的结果码；未连上服务或异常时为 -1
     */
    public int removeDirFile(String path) {
        int result = -1;
        if (this.mService == null) {
            return result;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeString(path);
            this.mService.transact(TRANSACTION_RemoveDirFile, data, reply, 0);
            result = reply.readInt();
            Log.d(TAG, "get result :" + result);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
        return result;
    }
}
