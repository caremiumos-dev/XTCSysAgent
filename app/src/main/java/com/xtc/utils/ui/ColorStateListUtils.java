package com.xtc.utils.ui;

import android.content.res.ColorStateList;

/**
 * 复选框 / 单选框用的 ColorStateList 生成器（原厂类名被混淆成 {@code c}）。
 *
 * <p>状态数组里的 {@code 16842910 / 16842912 / 16842919} 分别是
 * {@code android.R.attr.state_enabled / state_checked / state_pressed}，
 * 前面加负号表示“该状态不成立”。状态顺序就是匹配优先级，不能调换。</p>
 *
 * <p>色值部分全是“基色 ± 一个 ARGB 常量”算出来的：常量把基色的透明度压到不同档位
 * （33% / 40% / 12% / 19% 等），剩下两档是写死的灰阶。这些偏移量是与设计稿对齐的，
 * 反编译把它们打印成了 {@code i - (-1442840576)} 这种“减负数”的形状，
 * 这里原样保留减法写法，保证位运算结果逐位相同。</p>
 */
public class ColorStateListUtils {

    /** {@code android.R.attr.state_enabled}：可用态。 */
    private static final int STATE_ENABLED = 16842910;
    /** {@code android.R.attr.state_checked}：选中态。 */
    private static final int STATE_CHECKED = 16842912;
    /** {@code android.R.attr.state_pressed}：按下态。 */
    private static final int STATE_PRESSED = 16842919;

    /**
     * 生成“选中态用基色、禁用态用灰阶”的 ColorStateList（复选框风格）。
     *
     * <p>六个状态依次是：禁用+选中、禁用、按下+未选中、按下+选中、选中、未选中。
     * 其中未选中是写死的浅灰，禁用是写死的中灰，其余由基色叠透明度得到。</p>
     *
     * @param color 基色（通常取主题色）
     * @return 状态色列表
     */
    public static ColorStateList createCheckedColorStateList(int color) {
        return new ColorStateList(
                new int[][]{
                        new int[]{-STATE_ENABLED, STATE_CHECKED},
                        new int[]{-STATE_ENABLED},
                        new int[]{STATE_PRESSED, -STATE_CHECKED},
                        new int[]{STATE_PRESSED, STATE_CHECKED},
                        new int[]{STATE_CHECKED},
                        new int[]{-STATE_CHECKED}
                },
                new int[]{
                        color - (-1442840576),
                        -4539718,
                        color - (-1728053248),
                        color - (-1728053248),
                        (-16777216) | color,
                        -1118482
                });
    }

    /**
     * 生成“按下态用基色、未选中用极浅黑”的 ColorStateList（单选框风格）。
     *
     * <p>状态数组与 {@link #createCheckedColorStateList(int)} 有一处顺序差异：
     * 这里按下态的“选中 / 未选中”两条写成了 {@code {checked, pressed}} 与
     * {@code {-checked, pressed}}，先判选中再判按下，匹配优先级同样不能动。</p>
     *
     * <p>另外两档写死的颜色在反编译里被还原成了
     * {@code NotificationFlag.NOTIFICATION_FLAG_CUSTOM / NOTIFICATION_FLAG_BOOT_USE}，
     * 那是常量合并时随手挑的同值符号，实际数值是 {@code 0x10000000 / 0x20000000}，
     * 这里直接写字面量，避免把“通知 flag”这种无关概念引进颜色工具里。</p>
     *
     * @param color 基色（通常取主题色）
     * @return 状态色列表
     */
    public static ColorStateList createPressedColorStateList(int color) {
        return new ColorStateList(
                new int[][]{
                        new int[]{-STATE_ENABLED, STATE_CHECKED},
                        new int[]{-STATE_ENABLED},
                        new int[]{STATE_CHECKED, STATE_PRESSED},
                        new int[]{-STATE_CHECKED, STATE_PRESSED},
                        new int[]{STATE_CHECKED},
                        new int[]{-STATE_CHECKED}
                },
                new int[]{
                        color - (-520093696),
                        0x10000000,
                        color - (-805306368),
                        0x20000000,
                        color - (-805306368),
                        0x20000000
                });
    }
}
