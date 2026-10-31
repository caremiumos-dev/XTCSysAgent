package com.xtc.im.core.common.request.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.request.ImEntity;

/**
 * 请求报文基类。所有请求都带一个 tag=1 的请求号（RID），
 * 服务端原样回填到响应的 tag=1 里，用来把响应和请求对上。
 *
 * <p>请求实体全部是可序列化的（要跨进程传给 IM Core），所以实现 {@link Parcelable}；
 * 但真正的子类里只有消息类写了 {@code writeToParcel}，其余基类方法都是空实现——
 * 原厂就是靠"跨进程时走 Binder 而不是直接传实体"来规避这一点，保留空实现即可。</p>
 */
@CommandValue(0)
public abstract class RequestEntity extends ImEntity implements Parcelable, Cloneable {

    public static final Parcelable.Creator<RequestEntity> CREATOR =
            new Parcelable.Creator<RequestEntity>() {
                @Override
                public RequestEntity createFromParcel(Parcel parcel) {
                    return null;
                }

                @Override
                public RequestEntity[] newArray(int size) {
                    return null;
                }
            };

    /** 请求号。 */
    public int getRid() {
        return 0;
    }

    public void setRid(int rid) {
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 浅拷贝一份请求实体：重发时要在原实体上改字段（比如换请求号），
     * 不能动调用方手里那个对象。
     */
    public RequestEntity cloneEntity() throws CloneNotSupportedException {
        return (RequestEntity) clone();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
    }
}
