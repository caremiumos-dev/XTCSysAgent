package com.xtc.im.core.common.voice;

/**
 * 语音切片的 4 个状态取值（-1、0、1、2）。
 *
 * <p>原厂接口名与常量名都已混淆，字节码里没有引用点（常量会被编译期内联），
 * 只能确定 1 与 2 和 {@code VoiceSliceEntity.isFin} 的取值域重合（2 = 末片）。
 * 其余按取值推断，**数值与原厂一致**。</p>
 *
 * <p>原厂接口名为单字母 {@code b}。</p>
 */
public interface SliceConstants {

    /** 未知 / 失败。 */
    int STATE_UNKNOWN = -1;

    /** 初始状态。 */
    int STATE_IDLE = 0;

    /** 发送中（中间片）。 */
    int STATE_SENDING = 1;

    /** 已结束（末片）。 */
    int STATE_FINISHED = 2;
}
