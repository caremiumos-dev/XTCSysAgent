package com.xtc.utils.system.constant;

/**
 * 芯片平台常量。{@code QCOM} 同时是 {@code ro.hardware} 的取值，
 * 用来判断是不是国内高通平台。
 */
public interface PlatformConstants {
    String QUALCOMM = "Qualcomm";
    /** {@code ro.hardware} 的值。 */
    String QCOM = "qcom";
    String UNKNOWN = "";
    String MEDIATEK = "MediaTek";
    String UNISOC = "Unisoc";
}
