package com.xtc.utils.encode;

import java.util.Random;

/**
 * 随机小写字母串工具。
 *
 * <p>原厂类名被混淆成 {@code j}。只生成 a~z，随机源由调用方传入，
 * 这样调用方可以自己控制种子（测试时可复现）。</p>
 */
public final class RandomStringUtils {

    /** 小写字母表，字符集固定为 26 个 a~z。 */
    private static final char[] LOWER_CASE_ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private RandomStringUtils() {
    }

    /**
     * 生成指定长度的小写随机串。
     *
     * @param random 调用方提供的随机源
     * @param length 需要的字符个数
     */
    public static String randomLetters(Random random, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(LOWER_CASE_ALPHABET[random.nextInt(26)]);
        }
        return builder.toString();
    }
}
