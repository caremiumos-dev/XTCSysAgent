package com.xtc.utils.common;

import android.text.TextUtils;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 字符串判断与整形工具。
 *
 * <p>原厂类名被混淆成 {@code c}，里面的方法名全是单字母，这里按用途逐个还原。
 * 这一套方法散布在账号、设备、日志、Wifi 上报等业务里，做的是“脏数据清洗 + 轻量校验”，
 * 每条正则、每个魔数（全角空格 12288、全角区 65280~65375、偏移 65248、显示宽度 8 …）
 * 都是与上游数据格式绑定的契约，不能改动。</p>
 *
 * <p>注意几个“看起来像 bug、但必须照抄”的点：</p>
 * <ul>
 *   <li>{@link #isIpAddress(String)} 只校验每段是 0~255 的数字，不校验段数是否为 4；</li>
 *   <li>{@link #toCharCodeString(String)} 是把码点直接拼接（不补零），
 *       而 {@link #fromCharCodeString(String)} 固定按 5 位一组截取，两边只对 5 位码点对称；</li>
 *   <li>{@link #truncateByDisplayWidth(String)} 的上限 8 是按“全角算 2、半角算 1”的显示宽度算的；</li>
 *   <li>所有空判断都走 {@link TextUtils#isEmpty(CharSequence)}，因此全空格串也算“非空”。</li>
 * </ul>
 *
 * <p>原厂把这个类写成了可被继承的形状（{@code TAG} 是 protected 静态字段、构造器是隐式的公有无参构造），
 * 这里保持同样的可见性，不做成 final、也不做私有构造。</p>
 */
public class StringUtils {

    /** 日志 TAG。原厂用 {@code c.class.getSimpleName()} 取，改名后自然变成 "StringUtils"。 */
    protected static final String TAG = StringUtils.class.getSimpleName();

    /** 随机串字母表：数字在前、小写字母在后，顺序不能变（原厂如此）。 */
    private static final String RANDOM_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    /** 字符码点串的定长宽度，{@link #fromCharCodeString(String)} 按它切分。 */
    private static final int CHAR_CODE_WIDTH = 5;

    /** 显示宽度上限（全角按 2 计），{@link #truncateByDisplayWidth(String)} 用。 */
    private static final int MAX_DISPLAY_WIDTH = 8;

    /** 全角空格。 */
    private static final char FULL_WIDTH_SPACE = (char) 12288;
    /** 全角字符区间的下界（不含）。 */
    private static final int FULL_WIDTH_RANGE_LOW = 65280;
    /** 全角字符区间的上界（不含）。 */
    private static final int FULL_WIDTH_RANGE_HIGH = 65375;
    /** 全角转半角的码点偏移。 */
    private static final int FULL_TO_HALF_OFFSET = 65248;

    /** 判断整串是否都是数字（ASCII 0~9）；配合 {@code matches} 使用时，{@code ^\d+} 等价于“全数字且非空”。 */
    private static final String DIGIT_PATTERN = "^\\d+";
    /** 只含 0~9 的判断。 */
    private static final String NUMERIC_PATTERN = "[0-9]*";
    /** URL 判断。 */
    private static final String URL_PATTERN = "^((https?)|(ftp))://(?:(\\s+?)(?::(\\s+?))?@)?([a-zA-Z0-9\\-.]+)(?::(\\d+))?((?:/[a-zA-Z0-9\\-._?,'+\\&%$=~*!():@\\\\]*)+)?$";
    /** 非法字符白名单取反：命中的都要删掉。 */
    private static final String ILLEGAL_CHAR_PATTERN = "[^0-9a-zA-Z一-龥`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？[-]•－／｜＼／～＠《》〈〉〔〕［］<>-_ˇ｛｝ˉ¨＝＜％＄＃＋︿＿＆＊＂｀．〃‖々「」『』〖〗∶＇＂＊ ＆]+";
    /** 需要去掉的引号类字符。 */
    private static final String STRIP_QUOTE_PATTERN = "[『』]";
    /** 汉字 / 字母 / 数字 / 下划线 组成的名字。 */
    private static final String NAME_PATTERN = "^[一-龥_a-zA-Z0-9]+$";

    /**
     * 生成指定个数的空格串。
     *
     * <p>原厂写法是拿 {@code %Ns} 去格式化空串，N 就是个数；个数为 0 时得到 ""。</p>
     *
     * @param length 空格个数
     */
    public static String getSpaceString(int length) {
        return String.format("%" + length + "s", "");
    }

    /**
     * 截取 char 数组的 [startIndex, endIndex] 区间（两端都包含）。
     *
     * @param chars      源数组
     * @param startIndex 起始下标
     * @param endIndex   结束下标（包含）
     * @return 区间字符串；起始下标大于结束下标时返回空串
     */
    private static String substring(char[] chars, int startIndex, int endIndex) {
        if (startIndex > endIndex) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        while (startIndex <= endIndex) {
            buffer.append(chars[startIndex]);
            startIndex++;
        }
        return buffer.toString();
    }

    /**
     * 单个字符是否是数字（ASCII 0~9）。
     *
     * <p>原厂拿 {@code ^\d+} 去匹配“单字符转成的字符串”，所以非 0~9 的字符一律 false。</p>
     */
    public static boolean isDigit(char value) {
        return Pattern.compile(DIGIT_PATTERN).matcher(value + "").matches();
    }

    /**
     * 是否是 IPv4 形态的点分十进制串。
     *
     * <p>逐段 {@code split("\\.")} 后要求每段 trim 非空且落在 0~255；原厂不校验段数，
     * 所以 "1.2" 也会返回 true，这里保持不变。</p>
     */
    public static boolean isIpAddress(String value) {
        String[] segments;
        int segmentValue;
        if (value == null || (segments = value.split("\\.")) == null) {
            return false;
        }
        try {
            for (String segment : segments) {
                if (TextUtils.isEmpty(segment.trim())
                        || (segmentValue = Integer.parseInt(segment.trim())) < 0
                        || segmentValue > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 列表是否为 null 或空。 */
    public static <T> boolean isEmpty(List<T> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 生成指定长度的随机串（字符集为 0~9 与 a~z，随机源在方法内部新建）。
     *
     * @param length 需要的字符个数
     */
    public static String randomAlphaNumeric(int length) {
        StringBuffer alphabet = new StringBuffer(RANDOM_ALPHABET);
        StringBuffer result = new StringBuffer();
        Random random = new Random();
        int alphabetLength = alphabet.length();
        for (int i = 0; i < length; i++) {
            result.append(alphabet.charAt(random.nextInt(alphabetLength)));
        }
        return result.toString();
    }

    /**
     * 是否是（广义的）中文字符。
     *
     * <p>原厂按 UnicodeBlock 判定：CJK 统一表意文字（含扩展 A/B）、CJK 兼容表意文字、
     * CJK 符号与标点、半角及全角形式、通用标点，命中的都算“中文”。</p>
     */
    public static boolean isChineseChar(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    /** 是否非空且只由 0~9 组成。 */
    public static boolean isAllDigits(String value) {
        return !TextUtils.isEmpty(value) && value.matches(NUMERIC_PATTERN);
    }

    /**
     * 把整数格式化成两位（或原样）字符串：100 及以上原样输出，负数输出 "00"。
     *
     * <p>负数和 >= 100 的行为是原厂的特殊约定（用于拼时间/序号），保留。</p>
     */
    public static String toTwoDigits(int value) {
        if (value >= 100) {
            return "" + value;
        }
        return value < 0 ? "00" : String.format("%02d", Integer.valueOf(value));
    }

    /** 是否是 ASCII 字符（码点小于 128）。 */
    public static boolean isAscii(char value) {
        return value / 128 == 0;
    }

    /** 是否以数字开头（空串返回 false）。 */
    public static boolean startsWithDigit(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        return Pattern.compile(DIGIT_PATTERN).matcher(value.charAt(0) + "").matches();
    }

    /** 是否是 http/https/ftp 形式的 URL（字符串全匹配）。 */
    public static boolean isUrl(String value) {
        return !TextUtils.isEmpty(value) && value.matches(URL_PATTERN);
    }

    /**
     * 把每个字符的码点按十进制拼接成字符串。
     *
     * <p>原厂不补零、直接 append，所以结果是变长串；能还原的前提见
     * {@link #fromCharCodeString(String)}。</p>
     *
     * @return 码点串；入参为空返回 null
     */
    public static String toCharCodeString(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        char[] chars = value.toCharArray();
        StringBuffer buffer = new StringBuffer();
        for (char c : chars) {
            buffer.append((int) c);
        }
        return buffer.toString();
    }

    /**
     * 把 {@link #toCharCodeString(String)} 的结果还原成字符串。
     *
     * <p>固定按每 5 个字符一组转回 char，尾部不足 5 位的部分会被丢弃——原厂如此，
     * 因此只对码点恰好 5 位（如中文）的输入可逆。</p>
     *
     * @return 还原出的字符串；入参为空返回 null
     */
    public static String fromCharCodeString(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimmed = value.trim();
        int groupCount = trimmed.length() / CHAR_CODE_WIDTH;
        StringBuffer buffer = new StringBuffer();
        int end = 0;
        for (int i = 0; i < groupCount; i++) {
            end += CHAR_CODE_WIDTH;
            buffer.append((char) Integer.valueOf(trimmed.substring(i * CHAR_CODE_WIDTH, end)).intValue());
        }
        return buffer.toString();
    }

    /**
     * 全角转半角：全角空格变普通空格，65280~65375 区间的字符减去 65248。
     */
    public static String toHalfWidth(String value) {
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == FULL_WIDTH_SPACE) {
                chars[i] = ' ';
            } else if (chars[i] > FULL_WIDTH_RANGE_LOW && chars[i] < FULL_WIDTH_RANGE_HIGH) {
                chars[i] = (char) (chars[i] - FULL_TO_HALF_OFFSET);
            }
        }
        return new String(chars);
    }

    /**
     * 删掉所有不在白名单里的字符（邮编、地址等场景的清洗）。
     *
     * @throws PatternSyntaxException 正则本身有语法问题时抛出（与原厂声明一致）
     */
    public static String removeIllegalChars(String value) throws PatternSyntaxException {
        return Pattern.compile(ILLEGAL_CHAR_PATTERN).matcher(value).replaceAll("");
    }

    /**
     * 标点归一化：把全角【】！：换成半角，再去掉 『』 并 trim。
     */
    public static String normalizeSymbols(String value) {
        return Pattern.compile(STRIP_QUOTE_PATTERN)
                .matcher(value.replaceAll("【", "[")
                        .replaceAll("】", "]")
                        .replaceAll("！", "!")
                        .replaceAll("：", ":"))
                .replaceAll("")
                .trim();
    }

    /** 是否含有中文字符（判据见 {@link #isChineseChar(char)}）。 */
    public static boolean containsChinese(String value) {
        for (char c : value.toCharArray()) {
            if (isChineseChar(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算显示宽度：半角算 1，非 ASCII 算 2。
     *
     * @return 显示宽度；入参为 null 返回 0
     */
    public static int getDisplayWidth(String value) {
        if (value == null) {
            return 0;
        }
        int width = 0;
        for (char c : value.toCharArray()) {
            width++;
            if (!isAscii(c)) {
                width++;
            }
        }
        return width;
    }

    /**
     * 按显示宽度截断到 8 以内（用于列表里的一两行展示）。
     *
     * <p>原厂从尾部往前逐字缩短，返回第一个宽度不超过 8 的前缀；一个字符都放不下时原样返回。</p>
     */
    public static String truncateByDisplayWidth(String value) {
        if (value == null) {
            return "";
        }
        char[] chars = value.toCharArray();
        for (int endIndex = chars.length - 1; endIndex >= 0; endIndex--) {
            String candidate = substring(chars, 0, endIndex);
            if (getDisplayWidth(candidate) <= MAX_DISPLAY_WIDTH) {
                return candidate;
            }
        }
        return value;
    }

    /** 是否只由汉字、字母、数字、下划线组成。 */
    public static boolean isChineseAlphaNumeric(String value) {
        return Pattern.compile(NAME_PATTERN).matcher(value).matches();
    }
}
