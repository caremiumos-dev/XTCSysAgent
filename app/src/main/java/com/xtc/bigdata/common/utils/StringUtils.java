package com.xtc.bigdata.common.utils;

import com.xtc.log.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * 字符串工具：十六进制、URL 编解码、各种格式校验。
 *
 * <p>本类是纯静态工具，构造器直接抛异常 —— 原厂用
 * {@code "Are u ok ?"} 当提示语，看着像玩笑，但它是一道防线：
 * 反射/误用创建实例会立刻暴露。</p>
 *
 * <p>校验用的正则全部保留原值。它们不是"看着能跑就行"的装饰：
 * 手机号、身份证、IP 的判定边界直接决定上报数据是否被服务端接受。</p>
 */
public final class StringUtils {

    /** URL 编解码用的字符集。 */
    private static final String DEFAULT_URL_ENCODE = "utf-8";

    /** 日期格式（含闰年判断）。 */
    private static final String REGEX_DATE = "^(?:(?!0000)[0-9]{4}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)-02-29)$";

    /** 邮箱。 */
    private static final String REGEX_EMAIL = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    /** 18 位身份证。 */
    private static final String REGEX_IDCARD18 = "^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9Xx])$";

    /** IPv4。 */
    private static final String REGEX_IP = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";

    /** IPv6（支持内嵌 IPv4 与 zone id）。 */
    private static final String REGEX_IPV6 = "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$";

    /** 中国大陆手机号（含 147 数据卡号段）。 */
    private static final String REGEX_MOBILE_EXACT = "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(17[0,3,5-8])|(18[0-9])|(147))\\d{8}$";

    /** 日志 tag —— 原厂沿用 BFC 公共库的命名，不要改成当前类名。 */
    private static final String TAG = "BfcCommon_StringUtils";

    /** 十六进制字符表，大小写为大写。 */
    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private StringUtils() {
        throw new UnsupportedOperationException("Are u ok ?");
    }

    /**
     * 字节数组转大写十六进制串。
     *
     * @param data 字节数组
     * @return 十六进制串（长度为入参的两倍）
     */
    public static String bytes2HexString(byte[] data) {
        int writeIndex = 0;
        char[] chars = new char[data.length << 1];
        for (int index = 0; index < data.length; index++) {
            int highIndex = writeIndex + 1;
            chars[writeIndex] = hexDigits[(data[index] >>> 4) & 15];
            writeIndex = highIndex + 1;
            chars[highIndex] = hexDigits[data[index] & 15];
        }
        return new String(chars);
    }

    /**
     * URL 解码。
     *
     * @param url 编码后的串
     * @return 解码结果；失败时原样返回入参
     */
    public static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, "utf-8");
        } catch (Exception e) {
            LogUtil.w(TAG, e + ":url decode解码异常, 直接返回url");
            return url;
        }
    }

    /**
     * URL 编码。
     *
     * @param url 原始串
     * @return 编码结果；失败时原样返回入参
     */
    public static String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (Exception e) {
            LogUtil.w(TAG, e + ":url encode异常, 直接返回url");
            return url;
        }
    }

    /**
     * 是否邮箱。
     *
     * @param text 待校验串
     * @return 是否匹配
     */
    public static boolean isEmail(String text) {
        return isMatch(REGEX_EMAIL, text);
    }

    /**
     * 是否空串（不 trim，纯 {@code length() == 0}）。
     *
     * @param text 待校验串
     * @return 是否为 null 或空串
     */
    public static boolean isEmpty(String text) {
        return text == null || text.length() == 0;
    }

    /**
     * 是否 18 位身份证。
     *
     * @param text 待校验串
     * @return 是否匹配
     */
    public static boolean isIdCard(String text) {
        return isMatch(REGEX_IDCARD18, text);
    }

    /**
     * 是否 IPv4 或 IPv6。
     *
     * @param text 待校验串
     * @return 是否匹配
     */
    public static boolean isIp(String text) {
        return isMatch(REGEX_IP, text) || isMatch(REGEX_IPV6, text);
    }

    /**
     * 是否是合法 JSON（对象或数组）。
     *
     * <p>开发期用，业务包里没有调用点，保留是为了不改变类的形状。</p>
     *
     * @param json 待校验串
     * @return 是否合法
     */
    private static boolean isJson(String json) {
        boolean matched = true;
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("{")) {
                new JSONObject(trimmed);
            } else if (trimmed.startsWith("[")) {
                new JSONArray(trimmed);
            } else {
                matched = false;
            }
            return matched;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 正则全匹配。
     *
     * @param regex 正则
     * @param text  待校验串
     * @return 是否匹配；串为空直接 false
     */
    public static boolean isMatch(String regex, String text) {
        return !isEmpty(text) && Pattern.matches(regex, text);
    }

    /**
     * 是否大陆手机号。
     *
     * @param text 待校验串
     * @return 是否匹配
     */
    public static boolean isMobileNum(String text) {
        return isMatch(REGEX_MOBILE_EXACT, text);
    }

    /**
     * 是否全空白。
     *
     * @param text 待校验串
     * @return 是否为 null 或 trim 后为空
     */
    public static boolean isSpace(String text) {
        return text == null || text.trim().length() == 0;
    }
}
