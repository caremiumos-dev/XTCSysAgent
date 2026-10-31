package com.xtc.dns.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * IPv4 / IPv6 字面量校验器（原厂 {@code com.xtc.dns.client.util.InetAddressValidator}，
 * 移植自 Apache Commons Validator，类名本就是语义名，保留）。
 *
 * <p>为什么不直接用 {@code InetAddress.getByName} 判定？因为它对不合法输入会真的发起
 * DNS 查询，而这里只需要"这串字符是不是 ip"，必须是纯字符串判断。</p>
 *
 * <p>IPv6 的判定刻意保持原厂（Apache）那套"按冒号切分 + 压缩零位计数"的写法，
 * 包括它的一些历史怪癖（例如 {@code ::} 只在有压缩位时才放宽位数下限），
 * 换成更"正确"的解析器会改变边界行为。</p>
 */
public class InetAddressValidator implements Serializable {

    private static final long serialVersionUID = -919201640201914789L;

    /** IPv4 单段最大值。 */
    private static final int IPV4_MAX_OCTET_VALUE = 255;

    /** IPv6 单段（十六进制）最大值。 */
    private static final int IPV6_MAX_HEX_VALUE = 65535;

    /** 原字段 c，包内没有任何引用点；按 parseInt(...,16) 的基数推断命名，值保持原样。 */
    private static final int HEX_RADIX = 16;

    /** IPv4 正则：四段 1~3 位数字。 */
    private static final String IPV4_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";

    /** IPv6 最多 8 段。 */
    private static final int IPV6_MAX_OCTETS = 8;

    /** IPv6 单段最多 4 个十六进制字符。 */
    private static final int IPV6_MAX_OCTET_CHARS = 4;

    /** 无状态，直接给单例。 */
    private static final InetAddressValidator INSTANCE = new InetAddressValidator();

    /** IPv4 段的提取器（复用已编译的正则）。 */
    private final RegexValidator ipv4Validator = new RegexValidator(IPV4_REGEX);

    public static InetAddressValidator getInstance() {
        return INSTANCE;
    }

    /** IPv4 或 IPv6 只要有一种成立就算合法。 */
    public boolean isValid(String inetAddress) {
        return isValidInet4Address(inetAddress) || isValidInet6Address(inetAddress);
    }

    /** IPv4 校验：四段 0~255，且不允许前导零（{@code 010} 判为非法）。 */
    public boolean isValidInet4Address(String inet4Address) {
        String[] groups = this.ipv4Validator.match(inet4Address);
        if (groups == null) {
            return false;
        }
        for (String group : groups) {
            if (group == null || group.length() == 0) {
                return false;
            }
            try {
                if (Integer.parseInt(group) > IPV4_MAX_OCTET_VALUE) {
                    return false;
                }
                if (group.length() > 1 && group.startsWith("0")) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * IPv6 校验。
     *
     * <p>规则要点：{@code ::} 只能出现一次；不能以单个冒号开头/结尾；
     * 总段数不超过 8；末尾段可以是内嵌的 IPv4（此时算两段）。
     * 最后一行"段数不足 8 时必须有压缩位"是原厂/Apache 的判定，
     * 例如 {@code 1:2:3:4:5:6:7} 这种 7 段而无 {@code ::} 的写法会被判为非法。</p>
     */
    public boolean isValidInet6Address(String inet6Address) {
        boolean containsCompressedZeroes = inet6Address.contains("::");
        if (containsCompressedZeroes && inet6Address.indexOf("::") != inet6Address.lastIndexOf("::")) {
            return false;
        }
        if (inet6Address.startsWith(":") && !inet6Address.startsWith("::")) {
            return false;
        }
        if (inet6Address.endsWith(":") && !inet6Address.endsWith("::")) {
            return false;
        }
        String[] octets = inet6Address.split(":");
        if (containsCompressedZeroes) {
            ArrayList<String> octetList = new ArrayList<String>(Arrays.asList(octets));
            if (inet6Address.endsWith("::")) {
                octetList.add("");
            } else if (inet6Address.startsWith("::") && !octetList.isEmpty()) {
                octetList.remove(0);
            }
            octets = octetList.toArray(new String[octetList.size()]);
        }
        if (octets.length > IPV6_MAX_OCTETS) {
            return false;
        }
        int validOctets = 0;
        int emptyOctets = 0;
        for (int index = 0; index < octets.length; index++) {
            String octet = octets[index];
            if (octet.length() == 0) {
                emptyOctets++;
                if (emptyOctets > 1) {
                    return false;
                }
            } else {
                if (index == octets.length - 1 && octet.contains(".")) {
                    if (!isValidInet4Address(octet)) {
                        return false;
                    }
                    validOctets += 2;
                    emptyOctets = 0;
                } else {
                    if (octet.length() > IPV6_MAX_OCTET_CHARS) {
                        return false;
                    }
                    try {
                        int value = Integer.parseInt(octet, HEX_RADIX);
                        if (value < 0 || value > IPV6_MAX_HEX_VALUE) {
                            return false;
                        }
                        emptyOctets = 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            validOctets++;
        }
        if (validOctets <= IPV6_MAX_OCTETS) {
            return validOctets >= IPV6_MAX_OCTETS || containsCompressedZeroes;
        }
        return false;
    }
}
