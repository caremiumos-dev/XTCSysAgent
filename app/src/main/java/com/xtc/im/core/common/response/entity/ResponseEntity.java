package com.xtc.im.core.common.response.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.request.ImEntity;

/**
 * 响应报文基类。
 *
 * <p>所有响应都有三个公共 tag：1 = 请求号（RID）、2 = 状态码、3 = 描述。
 * 基类把它们都实现成"空/零"，具体子类只覆写自己真正解析的那几个——
 * 这样上层可以统一用基类类型访问，不用 instanceof 分派。</p>
 */
@CommandValue(0)
public class ResponseEntity extends ImEntity implements Parcelable {

    public static final Parcelable.Creator<ResponseEntity> CREATOR =
            new Parcelable.Creator<ResponseEntity>() {
                @Override
                public ResponseEntity createFromParcel(Parcel parcel) {
                    return new ResponseEntity(parcel);
                }

                @Override
                public ResponseEntity[] newArray(int size) {
                    return new ResponseEntity[size];
                }
            };

    public ResponseEntity() {
    }

    protected ResponseEntity(Parcel parcel) {
    }

    /** 请求号。 */
    public int getRid() {
        return 0;
    }

    public void setRid(int rid) {
    }

    public void setCode(int code) {
    }

    public void setDesc(String desc) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** 状态码。 */
    public int getCode() {
        return 0;
    }

    /** 状态描述。 */
    public String getDesc() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
    }
}
