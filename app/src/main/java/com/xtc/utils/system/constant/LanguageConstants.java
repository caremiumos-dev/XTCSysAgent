package com.xtc.utils.system.constant;

/**
 * 语言与地区常量。
 *
 * <p>原厂包名 {@code com.xtc.utils.system.a}、接口名 {@code a} 都是混淆产物，
 * 内层 {@code a}/{@code b} 因为重名还被重命名成了 {@code InterfaceC0025a}。
 * 这里按用途还原为 {@link Language} 与 {@link Region}。</p>
 */
public interface LanguageConstants {

    /** 语言代码。 */
    interface Language {
        String ZH = "zh";
        String EN = "en";
        String ID = "in";
        String TH = "th";
    }

    /** 地区代码。 */
    interface Region {
        String CN = "CN";
        String HK = "HK";
        String ID = "ID";
        String TH = "TH";
        String UNKNOWN = "";
    }
}
