package com.xtc.httplib.okhttp;

/**
 * 服务端返回非成功码时抛出的异常，同时带上服务端码与本地映射码。
 */
public class WatchHttpResultException extends RuntimeException {

    private final String serverCode;
    private final int appCode;

    public WatchHttpResultException(String serverCode) {
        super(serverCode);
        this.serverCode = serverCode;
        this.appCode = convert(serverCode);
    }

    /** 服务端码到本地码的映射。原实现恒返回 0。 */
    private int convert(String serverCode) {
        return 0;
    }

    public int appCode() {
        return this.appCode;
    }

    public String serverCode() {
        return this.serverCode;
    }
}
