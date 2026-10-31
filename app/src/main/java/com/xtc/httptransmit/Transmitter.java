package com.xtc.httptransmit;

import android.content.Context;

/**
 * 底层透传能力的抽象，由宿主（通常是主 App）注入实现。
 */
public interface Transmitter {
    /** 初始化，传入上下文、服务器地址与端口。 */
    void init(Context context, String host, int port);

    /**
     * 发起一次透传。
     *
     * @return 是否成功把请求交给了底层
     */
    boolean transmit(String path, int port, byte[] body, byte[] header, TransmitCallback callback);
}
