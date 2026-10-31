package com.xtc.im.core.common.request;

/**
 * 4 个取值常量（1..4）。
 *
 * <p>原厂接口名与常量名均已混淆，且字节码里没有任何引用点（常量会被编译期内联），
 * 无法推断语义，因此按取值命名，**数值与原厂一致**。</p>
 *
 * <p>原厂接口名为单字母 {@code e}。</p>
 */
public interface OptionConstants {

    int OPTION_1 = 1;

    int OPTION_2 = 2;

    int OPTION_3 = 3;

    int OPTION_4 = 4;
}
