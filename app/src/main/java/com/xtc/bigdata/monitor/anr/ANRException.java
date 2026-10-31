package com.xtc.bigdata.monitor.anr;

/**
 * ANR 异常类型（只是给"卡顿也算异常"套一个可识别的壳）。
 *
 * @deprecated 原厂已标记废弃：新的卡顿采集走 {@code WatchDogs}，
 *             该类只作为日志载体保留。
 */
@Deprecated
public class ANRException extends Exception {

    public ANRException() {
    }

    /**
     * @param message 卡顿描述
     */
    public ANRException(String message) {
        super(message);
    }
}
