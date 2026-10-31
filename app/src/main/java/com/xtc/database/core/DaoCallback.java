package com.xtc.database.core;

/**
 * 单结果回调。
 *
 * <p>原厂接口名为单字母 {@code i}，方法名 {@code a}，按用途还原。</p>
 */
public interface DaoCallback {
    /** 操作完成时回调，参数是结果对象。 */
    void onResult(Object result);
}
