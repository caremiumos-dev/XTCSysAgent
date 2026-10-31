package com.xtc.utils.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * 状态栏 / 导航栏 / 沉浸式工具（原厂未被混淆，类名就叫 {@code BarUtils}）。
 *
 * <p>按 API 分成三条实现路径，判断顺序不能动：</p>
 * <ul>
 *   <li><b>&gt;= 21</b>：{@code addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)} + 清除半透明标记 +
 *       {@code setStatusBarColor(calculateStatusColor(color, alpha))}；</li>
 *   <li><b>19 ~ 20</b>：给 {@code decorView} 追加/更新一个 {@link StatusBarView} 占位条，
 *       再把根布局设为 {@code fitsSystemWindows}；</li>
 *   <li><b>&lt; 19</b>：直接返回，什么都不做。</li>
 * </ul>
 *
 * <p>关于 {@code alpha} 的语义：{@code 0} 表示“完全不透明（用原色）”，{@code 255} 表示“全透明”，
 * 中间值按 {@code 1 - alpha/255} 的系数压暗原色，见 {@link #calculateStatusColor(int, int)}。</p>
 *
 * <p>反编译里本类引用的 {@code NotificationFlag.NOTIFICATION_FLAG_NOT_IN_MAIN / NOTIFICATION_FLAG_HEADER}
 * 是 R8 常量合并后挑的同值符号，真实含义是
 * {@link WindowManager.LayoutParams#FLAG_TRANSLUCENT_STATUS}（0x04000000）与
 * {@link WindowManager.LayoutParams#FLAG_TRANSLUCENT_NAVIGATION}（0x08000000），
 * {@code Integer.MIN_VALUE} 则是 {@link WindowManager.LayoutParams#FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS}；
 * 这里改用框架常量表达，数值逐位相同。</p>
 */
public class BarUtils {

    /** 默认的状态栏压暗系数（112/255 ≈ 44% 透明度）。 */
    public static final int DEFAULT_STATUS_BAR_ALPHA = 112;

    /**
     * 4.4~4.4W 上用来占位的状态栏条。
     *
     * <p>只当“一块有颜色的 View”用，不画任何内容。</p>
     */
    public static class StatusBarView extends View {
        public StatusBarView(Context context) {
            super(context);
        }

        public StatusBarView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }
    }

    private BarUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 按 alpha 系数把颜色压暗，并强制不透明。
     *
     * <p>系数 {@code f = 1 - alpha/255}，三通道各自乘 f 后四舍五入，
     * 最高字节固定 {@code -16777216}（0xFF000000）。所以 alpha=0 得到原色，alpha=255 得到纯黑。</p>
     *
     * @param color 原色
     * @param alpha 透明度系数 0~255
     * @return 压暗后的不透明色
     */
    private static int calculateStatusColor(int color, int alpha) {
        float factor = 1.0f - (alpha / 255.0f);
        return ((int) (((double) (factor * (color & 255))) + 0.5d)) | (((int) (((double) (((color >> 16) & 255) * factor)) + 0.5d)) << 16) | (-16777216) | (((int) (((double) (((color >> 8) & 255) * factor)) + 0.5d)) << 8);
    }

    /**
     * 取系统状态栏高度。
     *
     * @param context 上下文
     * @return 状态栏高度（px）；系统里查不到该 dimen 时返回 -1
     */
    public static int getStatusBarHeight(Context context) {
        int identifier = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (identifier > 0) {
            return context.getResources().getDimensionPixelSize(identifier);
        }
        return -1;
    }

    /**
     * 按默认 alpha（112）设置半透明黑状态栏。
     *
     * @param activity 目标 Activity
     */
    public static void setColor(Activity activity) {
        setColorWithAlpha(activity, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 用指定颜色、默认 alpha（112）设置状态栏颜色。
     *
     * @param activity 目标 Activity
     * @param color    状态栏原色
     */
    public static void setColor(Activity activity, int color) {
        setColor(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 设置状态栏颜色（核心实现，按 API 分三路）。
     *
     * @param activity 目标 Activity
     * @param color    状态栏原色
     * @param alpha    压暗系数，0 表示原色不透明，255 表示全黑
     */
    public static void setColor(Activity activity, int color, int alpha) {
        if (Build.VERSION.SDK_INT >= 21) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(calculateStatusColor(color, alpha));
        } else if (Build.VERSION.SDK_INT >= 19) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            int childCount = decorView.getChildCount();
            if (childCount <= 0 || !(decorView.getChildAt(childCount - 1) instanceof StatusBarView)) {
                decorView.addView(createStatusBarView(activity, color, alpha));
            } else {
                decorView.getChildAt(childCount - 1).setBackgroundColor(calculateStatusColor(color, alpha));
            }
            setRootViewFitsSystemWindows(activity);
        }
    }

    /**
     * 侧滑返回场景：透明状态栏 + 叠一条 alpha 黑条 + 让指定 View 让出状态栏高度。
     *
     * @param activity 目标 Activity
     * @param alpha    黑条透明度系数
     * @param view     需要下移的 View（通常是标题栏），允许为 null
     */
    public static void setColorForSwipeBack(Activity activity, int alpha, View view) {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        applyImmersiveStatusBar(activity);
        addStatusViewWithAlpha(activity, alpha);
        if (view != null) {
            ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).setMargins(0, getStatusBarHeight((Context) activity), 0, 0);
        }
    }

    /**
     * 侧滑返回场景（黑条 alpha = 0）。
     *
     * @param activity 目标 Activity
     * @param view     需要下移的 View
     */
    public static void setColorForSwipeBack(Activity activity, View view) {
        setColorForSwipeBack(activity, 0, view);
    }

    /**
     * 反射调用 {@code android.app.StatusBarManager} 的方法（展开/收起通知栏）。
     *
     * <p>这是系统私有 API，非系统应用调用会抛异常，原厂统一打印堆栈后忽略。</p>
     *
     * @param context    上下文
     * @param methodName {@code expand / collapse / expandSettingsPanel / expandNotificationsPanel ...}
     */
    private static void invokeStatusBarManager(Context context, String methodName) {
        try {
            Class.forName("android.app.StatusBarManager").getMethod(methodName, new Class[0]).invoke(context.getSystemService("statusbar"), new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 展开通知栏。
     *
     * @param context         上下文
     * @param isSettingsPanel true 展开快捷设置面板，false 展开通知列表；API 16 及以下统一走 "expand"
     */
    public static void expandNotificationPanel(Context context, boolean isSettingsPanel) {
        String methodName;
        if (Build.VERSION.SDK_INT <= 16) {
            methodName = "expand";
        } else {
            methodName = isSettingsPanel ? "expandSettingsPanel" : "expandNotificationsPanel";
        }
        invokeStatusBarManager(context, methodName);
    }

    /**
     * 设置透明状态栏，并让根布局 {@code fitsSystemWindows}。
     *
     * @param activity 目标 Activity
     */
    public static void setTransparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        makeStatusBarTransparent(activity);
        setRootViewFitsSystemWindows(activity);
    }

    /**
     * 把状态栏高度让给 {@code android.R.id.content}，并给它铺底色。
     *
     * @param activity 目标 Activity
     * @param color    底色原色
     */
    public static void setColorForContentView(Activity activity, int color) {
        setColorForContentView(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * 把状态栏高度让给 {@code android.R.id.content}，并给它铺底色（核心实现）。
     *
     * @param activity 目标 Activity
     * @param color    底色原色
     * @param alpha    压暗系数
     */
    public static void setColorForContentView(Activity activity, int color, int alpha) {
        if (Build.VERSION.SDK_INT >= 19) {
            ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
            contentView.setPadding(0, getStatusBarHeight((Context) activity), 0, 0);
            contentView.setBackgroundColor(calculateStatusColor(color, alpha));
            applyImmersiveStatusBar(activity);
        }
    }

    /**
     * 侧滑返回场景，并在 4.4 上额外清理掉占位状态栏条。
     *
     * @param activity 目标 Activity
     * @param alpha    黑条透明度系数
     * @param view     需要下移的 View
     */
    public static void setColorForSwipeBackCompat(Activity activity, int alpha, View view) {
        setColorForSwipeBack(activity, alpha, view);
        if (Build.VERSION.SDK_INT < 19 || Build.VERSION.SDK_INT >= 21) {
            return;
        }
        removeStatusBarView(activity);
    }

    /**
     * 侧滑返回场景（默认 alpha，不清理占位条）。
     *
     * @param activity 目标 Activity
     * @param view     需要下移的 View
     */
    public static void setColorForSwipeBackDefault(Activity activity, View view) {
        setColorForSwipeBack(activity, DEFAULT_STATUS_BAR_ALPHA, view);
    }

    /**
     * 收起通知栏 / 快捷设置面板。
     *
     * @param context 上下文
     */
    public static void collapseNotificationPanel(Context context) {
        invokeStatusBarManager(context, Build.VERSION.SDK_INT <= 16 ? "collapse" : "collapsePanels");
    }

    /**
     * 造一个高度等于状态栏、背景为计算色的占位条。
     *
     * @param activity 目标 Activity
     * @param color    原色
     * @param alpha    压暗系数
     * @return 占位条
     */
    private static StatusBarView createStatusBarView(Activity activity, int color, int alpha) {
        StatusBarView statusBarView = new StatusBarView(activity);
        statusBarView.setLayoutParams(new LinearLayout.LayoutParams(-1, getStatusBarHeight((Context) activity)));
        statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
        return statusBarView;
    }

    /**
     * 设置半透明状态栏（19+），已废弃，等价于 {@code addFlags(FLAG_TRANSLUCENT_STATUS) + fitsSystemWindows}。
     *
     * @param activity 目标 Activity
     */
    @Deprecated
    public static void setTranslucentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= 19) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            setRootViewFitsSystemWindows(activity);
        }
    }

    /**
     * 用指定颜色设置状态栏，alpha 传 0，即“原色、不压暗”。
     *
     * @param activity 目标 Activity
     * @param color    状态栏原色
     */
    public static void setColorOpaque(Activity activity, int color) {
        setColor(activity, color, 0);
    }

    /**
     * 侧滑返回场景，并在 4.4 上额外清理占位条（默认 alpha）。
     *
     * @param activity 目标 Activity
     * @param view     需要下移的 View
     */
    public static void setColorForSwipeBackCompatDefault(Activity activity, View view) {
        setColorForSwipeBackCompat(activity, DEFAULT_STATUS_BAR_ALPHA, view);
    }

    /**
     * 状态栏与导航栏都设为半透明（19+ 才有意义）。
     *
     * @param activity 目标 Activity
     */
    public static void setTranslucent(Activity activity) {
        if (Build.VERSION.SDK_INT >= 19) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 用调用方给的<b>原始 ARGB 值</b>直接当占位条背景，不做 alpha 压暗，已废弃。
     *
     * @param activity 目标 Activity
     * @param color    直接可用的 ARGB 颜色
     */
    @Deprecated
    public static void setColorDirectly(Activity activity, int color) {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        int childCount = decorView.getChildCount();
        if (childCount <= 0 || !(decorView.getChildAt(childCount - 1) instanceof StatusBarView)) {
            decorView.addView(createStatusBarViewWithColor(activity, color));
        } else {
            decorView.getChildAt(childCount - 1).setBackgroundColor(color);
        }
        setRootViewFitsSystemWindows(activity);
    }

    /**
     * 侧滑返回场景，并在 4.4 上额外清理占位条（alpha = 0）。
     *
     * @param activity 目标 Activity
     * @param view     需要下移的 View
     */
    public static void setColorForSwipeBackCompatNoAlpha(Activity activity, View view) {
        setColorForSwipeBackCompat(activity, 0, view);
    }

    /**
     * 全屏：去掉标题栏（{@code requestWindowFeature(1)}）并加 {@code FLAG_FULLSCREEN}。
     *
     * @param activity 目标 Activity
     */
    public static void setFullScreen(Activity activity) {
        activity.requestWindowFeature(1);
        activity.getWindow().setFlags(1024, 1024);
    }

    /**
     * 透明状态栏 + 叠一条 alpha 黑条（不处理根布局、不给 View 让位）。
     *
     * @param activity 目标 Activity
     * @param alpha    黑条透明度系数
     */
    public static void setColorWithAlpha(Activity activity, int alpha) {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        setTransparentStatusBar(activity);
        addStatusViewWithAlpha(activity, alpha);
    }

    /**
     * 透明状态栏 + 叠一条 alpha 黑条，但<b>不</b>让根布局 fitsSystemWindows。
     *
     * <p>与 {@link #setColorWithAlpha(Activity, int)} 只差最后那一步（少了
     * {@code setRootViewFitsSystemWindows}），兼容旧调用方时保留。</p>
     *
     * @param activity 目标 Activity
     * @param alpha    黑条透明度系数
     */
    public static void setColorWithAlphaCompat(Activity activity, int alpha) {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        makeStatusBarTransparent(activity);
        addStatusViewWithAlpha(activity, alpha);
    }

    /**
     * 当前是否<b>非</b>全屏（{@code FLAG_FULLSCREEN} 未设置）。
     *
     * <p>注意返回值与原意相反：全屏时返回 false。原厂如此，调用方按此约定使用。</p>
     *
     * @param activity 目标 Activity
     * @return 非全屏返回 true
     */
    public static boolean isNotFullScreen(Activity activity) {
        return (activity.getWindow().getAttributes().flags & 1024) != 1024;
    }

    /**
     * 取 ActionBar 高度。
     *
     * @param activity 目标 Activity
     * @return 高度（px）；主题里没有 {@code actionBarSize} 时返回 0
     */
    public static int getActionBarHeight(Activity activity) {
        TypedValue typedValue = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, activity.getResources().getDisplayMetrics());
        }
        return 0;
    }

    /**
     * 在 {@code android.R.id.content} 里叠一条 alpha 黑条。
     *
     * <p>content 已经有子 View 时直接给第 2 个子 View 上色，否则新加一条占位条。</p>
     *
     * @param activity 目标 Activity
     * @param alpha    黑条透明度系数（{@code Color.argb(alpha, 0, 0, 0)}）
     */
    private static void addStatusViewWithAlpha(Activity activity, int alpha) {
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        if (contentView.getChildCount() > 1) {
            contentView.getChildAt(1).setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        } else {
            contentView.addView(createStatusBarViewWithAlpha(activity, alpha));
        }
    }

    /**
     * 造一个高度等于状态栏、背景为指定颜色的占位条。
     *
     * @param activity 目标 Activity
     * @param color    背景色
     * @return 占位条
     */
    private static StatusBarView createStatusBarViewWithColor(Activity activity, int color) {
        StatusBarView statusBarView = new StatusBarView(activity);
        statusBarView.setLayoutParams(new LinearLayout.LayoutParams(-1, getStatusBarHeight((Context) activity)));
        statusBarView.setBackgroundColor(color);
        return statusBarView;
    }

    /**
     * 移除 4.4 路径追加的占位条，并把 content 首个子的 padding 复位。
     *
     * <p>只在 {@code decorView} 最后一个子 View 确实是 {@link StatusBarView} 时才动手；
     * 复位 padding 是原厂硬编码的 {@code setPadding(0, 0, 0, 0)}。</p>
     *
     * @param activity 目标 Activity
     */
    @TargetApi(19)
    private static void removeStatusBarView(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        int childCount = decorView.getChildCount();
        if (childCount <= 0 || !(decorView.getChildAt(childCount - 1) instanceof StatusBarView)) {
            return;
        }
        decorView.removeViewAt(childCount - 1);
        ((ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0)).setPadding(0, 0, 0, 0);
    }

    /**
     * 造一个高度等于状态栏、背景为 {@code Color.argb(alpha, 0, 0, 0)} 的占位条。
     *
     * @param activity 目标 Activity
     * @param alpha    黑条透明度系数
     * @return 占位条
     */
    private static StatusBarView createStatusBarViewWithAlpha(Activity activity, int alpha) {
        StatusBarView statusBarView = new StatusBarView(activity);
        statusBarView.setLayoutParams(new LinearLayout.LayoutParams(-1, getStatusBarHeight((Context) activity)));
        statusBarView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        return statusBarView;
    }

    /**
     * 把 content 的第一个子 View 设为 {@code fitsSystemWindows = true} 且不裁剪 padding。
     *
     * @param activity 目标 Activity
     */
    @TargetApi(14)
    private static void setRootViewFitsSystemWindows(Activity activity) {
        ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        rootView.setFitsSystemWindows(true);
        rootView.setClipToPadding(true);
    }

    /**
     * 让状态栏真正“透出内容”：21+ 置透明并把系统 UI 切成沉浸式，19/20 只能用半透明标记。
     *
     * @param activity 目标 Activity
     */
    private static void applyImmersiveStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            activity.getWindow().setStatusBarColor(0);
            activity.getWindow().getDecorView().setSystemUiVisibility(1280);
        } else if (Build.VERSION.SDK_INT >= 19) {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 清除半透明状态栏标记：21+ 走透明色 + 恢复系统绘制，19/20 只是加标记。
     *
     * @param activity 目标 Activity
     */
    @TargetApi(19)
    private static void makeStatusBarTransparent(Activity activity) {
        if (Build.VERSION.SDK_INT < 21) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            return;
        }
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        activity.getWindow().setStatusBarColor(0);
    }
}
