package com.xtc.im.core.common.request;

import android.os.Parcel;
import android.os.Parcelable;
import com.xtc.im.core.common.ImLogTag;
import com.xtc.im.core.common.listener.CompleteListener;
import com.xtc.im.core.common.listener.PushResponseListener;
import com.xtc.im.core.common.request.entity.RequestEntity;
import com.xtc.im.core.common.response.PushResponse;
import com.xtc.log.LogUtil;

/**
 * 一次 IM 请求的完整描述：报文实体 + 是否需要响应 + 各种回调 + 附加对象。
 *
 * <p>为什么回调要分三层：</p>
 * <ul>
 *   <li>{@code responseListener} —— 每收到一条响应回调一次（多响应请求会回调多次）；</li>
 *   <li>{@code completeListener} —— 全部响应收完之后回调一次；</li>
 *   <li>内部的 {@code innerResponseListener} —— 包在用户的 listener 外面，
 *       负责「只处理第一次响应 + 打日志」这段公共逻辑，避免每个调用方重复写。</li>
 * </ul>
 *
 * <p>原厂类名就是 {@code PushRequest}；内部 Builder 原本是单字母 {@code a}。</p>
 */
public class PushRequest implements Parcelable {

    private static final String TAG = ImLogTag.tag("PushRequest");

    /** 用户设置的响应监听（可以为 null）。 */
    protected PushResponseListener responseListener;

    /** 是否已经处理过一次响应（多响应时只认第一条）。 */
    protected boolean finished;

    /** 用户设置的完成监听（可以为 null）。 */
    protected CompleteListener completeListener;

    /** 报文实体。 */
    private RequestEntity requestEntity;

    /** 是否需要服务端响应。 */
    private boolean needResponse;

    /** 调用方附加的任意对象，原样带回。 */
    private Object extra;

    /** 是否允许多条响应（多条时只有第一条走用户回调）。 */
    private boolean multiResponse;

    /** 内部的响应适配器。 */
    private PushResponseListener innerResponseListener;

    /** 内部的完成适配器。 */
    private CompleteListener innerCompleteListener;

    public static final Parcelable.Creator<PushRequest> CREATOR =
            new Parcelable.Creator<PushRequest>() {
                @Override
                public PushRequest createFromParcel(Parcel parcel) {
                    return new PushRequest(parcel);
                }

                @Override
                public PushRequest[] newArray(int size) {
                    return new PushRequest[size];
                }
            };

    /**
     * 请求构造器。
     *
     * <p>设了 requestEntity 才能 build，否则直接抛 {@link IllegalStateException}——
     * 原厂宁可崩在构造点也不让一个空报文发出去。</p>
     */
    public static class Builder {

        private RequestEntity requestEntity;

        /** 默认需要响应。 */
        private boolean needResponse = true;

        private Object extra;

        private boolean multiResponse;

        private PushResponseListener responseListener;

        private CompleteListener completeListener;

        public Builder setCompleteListener(CompleteListener completeListener) {
            this.completeListener = completeListener;
            return this;
        }

        public Builder setResponseListener(PushResponseListener responseListener) {
            this.responseListener = responseListener;
            return this;
        }

        public Builder setRequestEntity(RequestEntity requestEntity) {
            this.requestEntity = requestEntity;
            return this;
        }

        public Builder setExtra(Object extra) {
            this.extra = extra;
            return this;
        }

        public Builder setNeedResponse(boolean needResponse) {
            this.needResponse = needResponse;
            return this;
        }

        public PushRequest build() {
            if (this.requestEntity == null) {
                throw new IllegalStateException("requestEntity == null");
            }
            return new PushRequest(this);
        }

        public Builder setMultiResponse(boolean multiResponse) {
            this.multiResponse = multiResponse;
            return this;
        }
    }

    protected PushRequest(Parcel parcel) {
        this.innerResponseListener = createResponseListener();
        this.innerCompleteListener = createCompleteListener();
        this.requestEntity = (RequestEntity) parcel.readValue(getClass().getClassLoader());
        this.needResponse = parcel.readByte() != 0;
        this.extra = parcel.readValue(getClass().getClassLoader());
        this.multiResponse = parcel.readByte() != 0;
        this.responseListener = (PushResponseListener) parcel.readValue(getClass().getClassLoader());
        this.finished = parcel.readByte() != 0;
        this.completeListener = (CompleteListener) parcel.readValue(getClass().getClassLoader());
    }

    private PushRequest(Builder builder) {
        this.innerResponseListener = createResponseListener();
        this.innerCompleteListener = createCompleteListener();
        this.requestEntity = builder.requestEntity;
        this.needResponse = builder.needResponse;
        this.extra = builder.extra;
        this.multiResponse = builder.multiResponse;
        this.responseListener = builder.responseListener;
        this.completeListener = builder.completeListener;
    }

    /**
     * 构造内部响应适配器。
     *
     * <p>两条分支而不是覆盖：已经处理过的响应只打 warning 就直接返回，
     * 用户回调也不会再被叫第二次。原厂这里是一个匿名内部类，
     * 这里还原成 lambda，逻辑一字未改。</p>
     */
    private PushResponseListener createResponseListener() {
        return (pushRequest, pushResponse) -> {
            if (PushRequest.this.finished) {
                LogUtil.w(TAG, "request:" + pushRequest.getRequestEntity()
                        + ",response:" + pushResponse.getResponseEntity());
                return;
            }
            if (PushRequest.this.responseListener != null) {
                LogUtil.i(TAG, "onReceive request:" + pushRequest.getRequestEntity()
                        + ",onReceive response:" + pushResponse.getResponseEntity());
                PushRequest.this.responseListener.onResponse(pushRequest, pushResponse);
            }
            PushRequest.this.finished = true;
        };
    }

    /**
     * 构造内部完成适配器（原厂同样是一个匿名内部类）。
     */
    private CompleteListener createCompleteListener() {
        return () -> {
            if (PushRequest.this.completeListener != null) {
                PushRequest.this.completeListener.onComplete();
            }
        };
    }

    public RequestEntity getRequestEntity() {
        return this.requestEntity;
    }

    /**
     * 就地读取（Parcelable 的另一种反序列化约定）。
     */
    public void readFromParcel(Parcel parcel) {
        this.requestEntity = (RequestEntity) parcel.readValue(getClass().getClassLoader());
        this.needResponse = parcel.readByte() != 0;
        this.extra = parcel.readValue(getClass().getClassLoader());
        this.multiResponse = parcel.readByte() != 0;
        this.responseListener = (PushResponseListener) parcel.readValue(getClass().getClassLoader());
        this.finished = parcel.readByte() != 0;
        this.completeListener = (CompleteListener) parcel.readValue(getClass().getClassLoader());
    }

    public boolean isNeedResponse() {
        return this.needResponse;
    }

    public boolean isMultiResponse() {
        return this.multiResponse;
    }

    public Object getExtra() {
        return this.extra;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** 内部响应适配器（交给 IM 服务用）。 */
    public PushResponseListener getResponseCallback() {
        return this.innerResponseListener;
    }

    /** 内部完成适配器（交给 IM 服务用）。 */
    public CompleteListener getCompleteCallback() {
        return this.innerCompleteListener;
    }

    @Override
    public String toString() {
        return "PushRequest{, requestEntity=" + this.requestEntity
                + ", needResponse=" + this.needResponse + ", extra=" + this.extra
                + ", mutiResponse=" + this.multiResponse + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeValue(this.requestEntity);
        parcel.writeByte((byte) (this.needResponse ? 1 : 0));
        parcel.writeValue(this.extra);
        parcel.writeByte((byte) (this.multiResponse ? 1 : 0));
        parcel.writeValue(this.responseListener);
        parcel.writeByte((byte) (this.finished ? 1 : 0));
        parcel.writeValue(this.completeListener);
    }
}
