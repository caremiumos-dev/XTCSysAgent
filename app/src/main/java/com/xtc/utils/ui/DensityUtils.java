package com.xtc.utils.ui;

import android.content.Context;

/**
 * 屏幕密度换算工具（原厂类名被混淆成 {@code d}）。
 *
 * <p>五个换算方法的取整方式<b>并不一致</b>，这是原厂行为，必须原样保留：</p>
 * <ul>
 *   <li>{@link #dp2px(Context, float)}：{@code density * dp + 0.5} 后取整，四舍五入；</li>
 *   <li>{@link #dp2pxFloat(Context, float)}：不取整，直接返回浮点；</li>
 *   <li>{@link #px2dp(Context, float)}：直接截断，<b>没有</b> +0.5；</li>
 *   <li>{@link #sp2px(Context, float)} / {@link #px2sp(Context, float)}：都带 +0.5 四舍五入。</li>
 * </ul>
 *
 * <p>注意 {@code px2dp} 用的是 {@code density}，而 {@code sp} 换算用的是
 * {@code scaledDensity}（会跟随系统字体大小），两套密度不能混用。</p>
 */
public class DensityUtils {

    /**
     * 取当前屏幕密度（density，dp → px 的系数）。
     *
     * @param context 上下文
     * @return 密度值，例如 1.0 / 1.5 / 3.0
     */
    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * dp 转 px（四舍五入）。
     *
     * @param context 上下文
     * @param dpValue dp 值
     * @return 像素值
     */
    public static int dp2px(Context context, float dpValue) {
        return (int) ((getDensity(context) * dpValue) + 0.5f);
    }

    /**
     * dp 转 px（保留浮点，不取整）。
     *
     * @param context 上下文
     * @param dpValue dp 值
     * @return 像素值
     */
    public static float dp2pxFloat(Context context, float dpValue) {
        return getDensity(context) * dpValue;
    }

    /**
     * px 转 dp（截断取整，原厂未做 +0.5）。
     *
     * @param context 上下文
     * @param pxValue 像素值
     * @return dp 值
     */
    public static int px2dp(Context context, float pxValue) {
        return (int) (pxValue / getDensity(context));
    }

    /**
     * sp 转 px（四舍五入，跟随系统字体缩放）。
     *
     * @param context 上下文
     * @param spValue sp 值
     * @return 像素值
     */
    public static int sp2px(Context context, float spValue) {
        return (int) ((context.getResources().getDisplayMetrics().scaledDensity * spValue) + 0.5f);
    }

    /**
     * px 转 sp（四舍五入，跟随系统字体缩放）。
     *
     * @param context 上下文
     * @param pxValue 像素值
     * @return sp 值
     */
    public static int px2sp(Context context, float pxValue) {
        return (int) ((pxValue / context.getResources().getDisplayMetrics().scaledDensity) + 0.5f);
    }
}
