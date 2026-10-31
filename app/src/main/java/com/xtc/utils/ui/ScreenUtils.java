package com.xtc.utils.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

/**
 * 屏幕尺寸 / 方向 / 锁屏状态工具（原厂类名被混淆成 {@code j}）。
 *
 * <p>方向相关的常量直接写字面量：{@code 0} = {@link android.content.pm.ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE}，
 * {@code 1} = {@link android.content.pm.ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}；
 * 配置里的 {@code orientation} 则是 {@code 1=竖屏、2=横屏}。两套数值含义不同，别混。</p>
 *
 * <p>两个截屏方法都靠 {@code View#getDrawingCache()}——在硬件加速的窗口上可能拿到 null，
 * 原厂并没有判空，这里同样不判（保持行为一致），调用方需自行保证在软件绘制场景调用。</p>
 */
public class ScreenUtils {

    private ScreenUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 取屏幕宽度（px）。
     *
     * @param context 上下文
     * @return 宽度像素
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    /**
     * 强制横屏。
     *
     * @param activity 目标 Activity
     */
    public static void setLandscape(Activity activity) {
        activity.setRequestedOrientation(0);
    }

    /**
     * 取屏幕高度（px）。
     *
     * @param context 上下文
     * @return 高度像素
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    /**
     * 强制竖屏。
     *
     * @param activity 目标 Activity
     */
    public static void setPortrait(Activity activity) {
        activity.setRequestedOrientation(1);
    }

    /**
     * 取屏幕当前旋转角度。
     *
     * @param activity 目标 Activity
     * @return 0 / 90 / 180 / 270，其它旋转值一律按 0 处理
     */
    public static int getScreenRotation(Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case 1:
                return 90;
            case 2:
                return 180;
            case 3:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * 当前是否横屏（{@code Configuration.orientation == 2}）。
     *
     * @param context 上下文
     * @return 横屏返回 true
     */
    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == 2;
    }

    /**
     * 截当前窗口（含状态栏）。
     *
     * <p>直接按 {@link DisplayMetrics} 的宽高裁剪绘制缓存，因此包含状态栏那一条。</p>
     *
     * @param activity 目标 Activity
     * @return 位图
     */
    public static Bitmap captureWithStatusBar(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setDrawingCacheEnabled(true);
        decorView.buildDrawingCache();
        Bitmap drawingCache = decorView.getDrawingCache();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Bitmap bitmap = Bitmap.createBitmap(drawingCache, 0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        decorView.destroyDrawingCache();
        return bitmap;
    }

    /**
     * 当前是否竖屏（{@code Configuration.orientation == 1}）。
     *
     * @param context 上下文
     * @return 竖屏返回 true
     */
    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == 1;
    }

    /**
     * 截当前窗口（不含状态栏）。
     *
     * <p>纵向从状态栏高度处开始裁，所以结果比 {@link #captureWithStatusBar(Activity)} 矮一条状态栏。</p>
     *
     * @param activity 目标 Activity
     * @return 位图
     */
    public static Bitmap captureWithoutStatusBar(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setDrawingCacheEnabled(true);
        decorView.buildDrawingCache();
        Bitmap drawingCache = decorView.getDrawingCache();
        int statusBarHeight = BarUtils.getStatusBarHeight((Context) activity);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Bitmap bitmap = Bitmap.createBitmap(drawingCache, 0, statusBarHeight, displayMetrics.widthPixels, displayMetrics.heightPixels - statusBarHeight);
        decorView.destroyDrawingCache();
        return bitmap;
    }

    /**
     * 是否处于锁屏（密码键盘）状态。
     *
     * @param context 上下文
     * @return 锁屏中返回 true
     */
    public static boolean isScreenLock(Context context) {
        return ((KeyguardManager) context.getSystemService("keyguard")).inKeyguardRestrictedInputMode();
    }
}
