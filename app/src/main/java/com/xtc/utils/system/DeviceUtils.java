package com.xtc.utils.system;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import java.util.Locale;

/**
 * 机型 / 区域信息工具。
 *
 * <p>原厂类名为单字母 {@code t}，全部方法也是单字母，语义按"读的是哪个属性"
 * 推断命名。所有属性键都走 {@link SystemPropertiesUtils}，与原件一致。</p>
 */
public class DeviceUtils {

    private static String sModelCache = "";

    private DeviceUtils() {
    }

    /** 完整机型标识：{@code 内部型号-地区}，缺项时降级。 */
    public static String getDeviceModel() {
        String region = getLocaleRegion();
        if (!region.equals("")) {
            return getInnerModel() + "-" + region;
        }
        String serverRegion = getServerRegion();
        if (serverRegion.equals("")) {
            return getInnerModel();
        }
        return serverRegion;
    }

    /** 写入"内网服务器"属性。 */
    public static void setServerRegion(String serverRegion) {
        SystemPropertiesUtils.set(SystemConstants.PROP_SERVER_INNER, serverRegion);
    }

    /** 机型前缀是否等于给定值。 */
    public static boolean matchPrefix(String model, String prefix) {
        return prefix.equals(model.split("-")[0]);
    }

    /** 机型是否等于 {@code prefix-region}。 */
    public static boolean matchModel(String model, @NonNull String prefix, @NonNull String region) {
        return model.equals(prefix + "-" + region);
    }

    /** 系统语言（两字母）。 */
    public static String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /** 当前内部型号是否以 {@code suffix} 结尾。 */
    public static boolean isInnerModelEndsWith(String suffix) {
        return matchPrefix(getInnerModel(), suffix);
    }

    /** {@code value} 是否以 {@code suffix} 结尾；后缀为空时判断"不含 -"。 */
    public static boolean endsWith(String value, String suffix) {
        if (suffix.equals("")) {
            return !value.contains("-");
        }
        return value.endsWith(suffix);
    }

    /** 地区代码：国内按硬件分支取值，海外取 locale region。 */
    public static String getRegion() {
        if (isDomestic()) {
            return resolveDomesticRegion();
        }
        return getLocaleRegionRaw();
    }

    /** 地区代码是否等于给定值。 */
    public static boolean isRegion(String region) {
        return getRegion().equals(region);
    }

    /** 已废弃：是否泰国地区。 */
    @Deprecated
    public static boolean isThailand() {
        return isRegion(com.xtc.utils.system.constant.LanguageConstants.Region.TH);
    }

    /** 完整机型是否以给定串开头。 */
    public static boolean isDeviceModel(String prefix) {
        return endsWith(getDeviceModel(), prefix);
    }

    /** 已废弃：是否印尼地区。 */
    @Deprecated
    public static boolean isIndonesia() {
        return isRegion(com.xtc.utils.system.constant.LanguageConstants.Region.ID);
    }

    /**
     * 硬件代号是否属于某一批老屏幕模组。
     */
    public static boolean isLegacyPanel(String hardwareId) {
        return hardwareId.equals(SystemConstants.Hardware.Id.F2)
                || hardwareId.equals(SystemConstants.Hardware.Id.IA)
                || hardwareId.equals(SystemConstants.Hardware.Id.I2C)
                || hardwareId.equals(SystemConstants.Hardware.Id.I3)
                || hardwareId.equals(SystemConstants.Hardware.Id.I6)
                || hardwareId.equals(SystemConstants.Hardware.Id.I8)
                || hardwareId.equals(SystemConstants.Hardware.Id.IDI6C)
                || hardwareId.equals(SystemConstants.Hardware.Id.Y01)
                || hardwareId.equals(SystemConstants.Hardware.Id.Y02)
                || hardwareId.equals(SystemConstants.Hardware.Id.Y1A);
    }

    /** 已废弃：是否为非 V1 的已知项目。 */
    @Deprecated
    public static boolean isNotV1() {
        if (isRegion(SystemConstants.Project.XTC_Z3) || isRegion("")) {
            return false;
        }
        return true;
    }

    /** 产品型号（带缓存）。 */
    public static String getProductModel() {
        if (TextUtils.isEmpty(sModelCache)) {
            sModelCache = SystemPropertiesUtils.get(SystemConstants.PROP_PRODUCT_MODEL,
                    SystemConstants.Project.XTC_Z3);
        }
        return sModelCache;
    }

    /** 已废弃：是否 I12。 */
    @Deprecated
    public static boolean isI12() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I12);
    }

    /** 已废弃：是否 I13。 */
    @Deprecated
    public static boolean isI13() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I13);
    }

    /** 已废弃：是否 I13C。 */
    @Deprecated
    public static boolean isI13C() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I13C);
    }

    /** 已废弃：是否 I16。 */
    @Deprecated
    public static boolean isI16() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I16);
    }

    /** 已废弃：是否 I17。 */
    @Deprecated
    public static boolean isI17() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I17);
    }

    /** 已废弃：是否 I18。 */
    @Deprecated
    public static boolean isI18() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I18);
    }

    /** 已废弃：是否尚未确认地区（I13C 之外）。 */
    @Deprecated
    public static boolean isPendingRegion() {
        return isInnerModelEndsWith(SystemConstants.Hardware.Id.I2C);
    }

    /** 是否海外机型。 */
    public static boolean isOversea() {
        return !isDomestic();
    }

    /** 是否为国内硬件平台。 */
    public static boolean isDomestic() {
        return "qcom".equals(SystemPropertiesUtils.get("ro.hardware", ""));
    }

    /** 内部型号。 */
    @NonNull
    private static String getInnerModel() {
        return SystemPropertiesUtils.get(SystemConstants.PROP_PRODUCT_INNER_MODEL,
                SystemConstants.Hardware.Id.IA);
    }

    /** 内网服务器地区。 */
    @NonNull
    private static String getServerRegion() {
        return SystemPropertiesUtils.get(SystemConstants.PROP_SERVER_INNER, "");
    }

    /** 产品语言。 */
    @NonNull
    private static String getProductLocale() {
        return SystemPropertiesUtils.get(SystemConstants.PROP_LOCALE, "");
    }

    /** 产品地区。 */
    @NonNull
    private static String getLocaleRegion() {
        return SystemPropertiesUtils.get(SystemConstants.PROP_LOCALE_REGION, "");
    }

    /** 国内机型：优先取产品语言末两位，否则取内网地区末两位。 */
    private static String resolveDomesticRegion() {
        if (!getInnerModel().equals(SystemConstants.Hardware.Id.I13)) {
            String locale = getProductLocale();
            if (locale.equals("")) {
                return "";
            }
            int length = locale.length();
            return locale.substring(length - 2, length);
        }
        String region = getLocaleRegion();
        if (!region.equals("")) {
            return region;
        }
        String serverRegion = getServerRegion();
        if (serverRegion.equals("")) {
            return "";
        }
        int length = serverRegion.length();
        return serverRegion.substring(length - 2, length);
    }

    /** 海外机型：直接取产品地区。 */
    private static String getLocaleRegionRaw() {
        return getLocaleRegion();
    }
}
