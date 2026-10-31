package com.xtc.httptransmit;

/**
 * 透传实现的注册表。没有注入实现时所有调用都静默失败。
 */
public class TransmitManager {

    private static Transmitter sTransmitter;

    public static Transmitter get() {
        return sTransmitter;
    }

    public static void set(Transmitter transmitter) {
        sTransmitter = transmitter;
    }

    /**
     * 转发一次透传请求。
     *
     * @return 没有注入实现时返回 false
     */
    public static boolean transmit(String path, int port, byte[] body, byte[] header,
                                   TransmitCallback callback) {
        if (sTransmitter == null) {
            return false;
        }
        sTransmitter.transmit(path, port, body, header, callback);
        return true;
    }
}
