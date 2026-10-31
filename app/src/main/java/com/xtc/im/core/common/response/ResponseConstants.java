package com.xtc.im.core.common.response;

/**
 * 响应侧用到的 5 个命令 / 状态常量（3、4、1000、1001、1002）。
 *
 * <p>原厂接口名与常量名都已混淆，字节码里没有引用点，无法推断语义，
 * 因此按取值命名，**数值与原厂一致**。</p>
 *
 * <p>原厂接口名为单字母 {@code b}。</p>
 */
public interface ResponseConstants {

    int CODE_3 = 3;

    int CODE_4 = 4;

    int CODE_1001 = 1001;

    int CODE_1000 = 1000;

    int CODE_1002 = 1002;
}
