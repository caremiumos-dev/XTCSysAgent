package com.xtc.utils.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Activity 截图并落盘（原厂类名被混淆成 {@code i}）。
 *
 * <p>截图方式是最老的 {@code getDrawingCache()} 方案：打开绘制缓存 → 取整窗位图 →
 * 用 {@code getWindowVisibleDisplayFrame} 的 top 值裁掉状态栏（有的 ROM 上状态栏
 * 属于装饰视图但不属于可见内容区，这样裁比固定减状态栏高度更准）。</p>
 *
 * <p>落盘时固定 PNG + 质量 100（PNG 会忽略质量参数），父目录不存在会先 mkdirs。</p>
 */
public class ScreenshotUtils {

    /**
     * 截取 Activity 窗口（不含状态栏区域）并返回位图。
     *
     * @param activity 目标 Activity
     * @return 位图
     */
    private static Bitmap getWindowBitmap(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setDrawingCacheEnabled(true);
        decorView.buildDrawingCache();
        Bitmap drawingCache = decorView.getDrawingCache();
        Rect visibleFrame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);
        int top = visibleFrame.top;
        Bitmap bitmap = Bitmap.createBitmap(drawingCache, 0, top, activity.getWindowManager().getDefaultDisplay().getWidth(), activity.getWindowManager().getDefaultDisplay().getHeight() - top);
        decorView.destroyDrawingCache();
        return bitmap;
    }

    /**
     * 截图并保存为 PNG 文件。
     *
     * <p>异常处理按原厂行为还原：</p>
     * <ul>
     *   <li>{@code new FileOutputStream} 抛 {@link FileNotFoundException} 时（文件不可写、无权限）
     *       <b>静默返回</b>，不打印堆栈；</li>
     *   <li>{@code compress} 出来的 {@code IOException} 由调用方感知 ——
     *       原厂方法声明 {@code throws Throwable} 并原样重抛，这里保持一致，改为在签名上声明；</li>
     *   <li>无论成功与否，流都会 flush + close，关闭时的 IOException 打印堆栈。</li>
     * </ul>
     *
     * <p>注意：反编译结果里 {@code compress(...)} 外面挂着一个
     * <code>catch (FileNotFoundException)</code>，那是 JADX 拆 try 块产生的假象 ——
     * {@code compress} 只会抛 {@code IOException}，Java 编译器不允许这么写。
     * 原厂真实的形状是「一个 try 同时包住 new FileOutputStream 和 compress」，
     * 所以按上面的方式拆开即可，运行时行为完全一致。</p>
     *
     * @param activity 目标 Activity
     * @param file     目标文件；为 null 时直接返回
     */
    public static void saveScreenshot(Activity activity, File file) throws IOException {
        if (file == null) {
            return;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            // 原厂这里是静默失败：只把流对象置空后返回，不打印堆栈。
            return;
        }
        try {
            getWindowBitmap(activity).compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } finally {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
