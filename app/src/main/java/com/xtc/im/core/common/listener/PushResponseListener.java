package com.xtc.im.core.common.listener;

import com.xtc.im.core.common.request.PushRequest;
import com.xtc.im.core.common.response.PushResponse;

/**
 * 收到一条 IM 响应时的回调。
 *
 * <p>原厂接口名为单字母 {@code d}。</p>
 */
public interface PushResponseListener {

    /**
     * @param request  对应的请求
     * @param response 服务端响应
     */
    void onResponse(PushRequest request, PushResponse response);
}
