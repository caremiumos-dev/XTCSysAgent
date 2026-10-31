package com.xtc.im.core.common.response.entity;

/**
 * 响应错误码常量。
 *
 * <p>四个取值（20000、20001、20001、20002）里 20001 出现了两次——原厂就是这样，
 * 说明其中一个是历史遗留的重复常量。名字按取值命名：原厂类名与常量名都已混淆，
 * 字节码里也没有引用点，无法推断语义。</p>
 *
 * <p>原厂类名为单字母 {@code a}。</p>
 */
public class ResponseCode {

    public static final int CODE_20000 = 20000;

    public static final int CODE_20001 = 20001;

    /** 与 {@link #CODE_20001} 数值相同：原厂重复定义，两个名字都保留。 */
    public static final int CODE_20001_DUPLICATE = 20001;

    public static final int CODE_20002 = 20002;
}
