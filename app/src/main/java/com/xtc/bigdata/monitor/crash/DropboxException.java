package com.xtc.bigdata.monitor.crash;

/**
 * Dropbox 采集过程中的异常。
 *
 * <p>纯壳类型：DropBox 采集全程是"解析失败就算了"的策略，
 * 这个异常只用于日志里区分"这是解析出来的异常"而不是运行期崩溃。</p>
 */
public class DropboxException extends Exception {

    public DropboxException() {
    }

    /**
     * @param message 描述
     */
    public DropboxException(String message) {
        super(message);
    }
}
