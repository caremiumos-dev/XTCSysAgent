package com.xtc.dns.client.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于正则的字符串校验器（原厂 {@code com.xtc.dns.client.util.RegexValidator}，
 * 移植自 Apache Commons Validator，类名本来就是语义名，保留）。
 *
 * <p>构造时一次性把正则编译成 {@link Pattern}，之后每次校验只做 matcher 匹配。
 * 支持多个候选正则：任意一个命中即算通过（{@link #isValid}），
 * 或取第一个命中的分组结果（{@link #match} / {@link #validate}）。</p>
 */
public class RegexValidator implements Serializable {

    private static final long serialVersionUID = -8832409930574867162L;

    /** 所有候选正则，编译期已就绪。 */
    private final Pattern[] patterns;

    /** 单正则构造，默认大小写敏感。 */
    public RegexValidator(String regex) {
        this(regex, true);
    }

    /**
     * 单正则构造。
     *
     * @param regex         正则
     * @param caseSensitive 是否大小写敏感
     */
    public RegexValidator(String regex, boolean caseSensitive) {
        this(new String[]{regex}, caseSensitive);
    }

    /** 多正则构造，默认大小写敏感。 */
    public RegexValidator(String[] regexes) {
        this(regexes, true);
    }

    /**
     * 多正则构造。
     *
     * @param regexes       正则数组
     * @param caseSensitive 是否大小写敏感
     */
    public RegexValidator(String[] regexes, boolean caseSensitive) {
        if (regexes == null || regexes.length == 0) {
            throw new IllegalArgumentException("Regular expressions are missing");
        }
        this.patterns = new Pattern[regexes.length];
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        for (int i = 0; i < regexes.length; i++) {
            if (regexes[i] == null || regexes[i].length() == 0) {
                throw new IllegalArgumentException("Regular expression[" + i + "] is missing");
            }
            this.patterns[i] = Pattern.compile(regexes[i], flags);
        }
    }

    /** 任意一个正则整体匹配即返回 true。 */
    public boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < this.patterns.length; i++) {
            if (this.patterns[i].matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回第一个命中正则的分组（不含整体匹配的第 0 组）。
     *
     * @return 分组数组；全都不命中时返回 null
     */
    public String[] match(String value) {
        if (value == null) {
            return null;
        }
        for (int i = 0; i < this.patterns.length; i++) {
            Matcher matcher = this.patterns[i].matcher(value);
            if (matcher.matches()) {
                int count = matcher.groupCount();
                String[] groups = new String[count];
                for (int j = 0; j < count; j++) {
                    groups[j] = matcher.group(j + 1);
                }
                return groups;
            }
        }
        return null;
    }

    /** 把编译好的正则拼成可读串，仅用于日志排错。 */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegexValidator{");
        for (int i = 0; i < this.patterns.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(this.patterns[i].pattern());
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * 返回第一个命中正则的分组拼接结果。
     *
     * <p>只有一个分组时直接返回该分组（保留 null），多个分组时按顺序拼成一个串，
     * 与原厂一致：单分组场景下调用方拿到的可能是 null，不能私自改成空串。</p>
     */
    public String validate(String value) {
        if (value == null) {
            return null;
        }
        for (int i = 0; i < this.patterns.length; i++) {
            Matcher matcher = this.patterns[i].matcher(value);
            if (matcher.matches()) {
                int count = matcher.groupCount();
                if (count == 1) {
                    return matcher.group(1);
                }
                StringBuilder builder = new StringBuilder();
                for (int j = 0; j < count; j++) {
                    String group = matcher.group(j + 1);
                    if (group != null) {
                        builder.append(group);
                    }
                }
                return builder.toString();
            }
        }
        return null;
    }
}
