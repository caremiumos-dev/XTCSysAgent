package com.xtc.httptransmit;

/**
 * 透传请求的回调。
 */
public interface TransmitCallback {
    /** 请求失败，参数是错误描述。 */
    void onError(String message);

    /** 请求成功，参数是响应体。 */
    void onSuccess(byte[] data);
}
