package com.xtc.utils.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;

import com.xtc.log.LogUtil;

/**
 * 基于 RenderScript 的高斯模糊工具（原厂类名被混淆成 {@code l}）。
 *
 * <p>与 {@link BlurUtils} 的区别：这里走系统 {@code ScriptIntrinsicBlur}（API 17+，GPU/NEON 加速，
 * 半径上限 25），{@link BlurUtils} 是纯 Java 的栈模糊。两条路都依赖同一张遮罩图
 * {@code R.drawable.ic_blur_mask} 压噪。</p>
 *
 * <p>日志文案（含“高斯模糊传入view宽高过大，内存不够”）是原厂的排障线索，一字未改。</p>
 */
public class RenderScriptBlurUtils {

    /**
     * 用 RenderScript 对位图做高斯模糊，并在结果上叠一层遮罩。
     *
     * @param context 上下文（RenderScript 需要）
     * @param bitmap  源位图
     * @param radius  模糊半径（直接透传给 {@code ScriptIntrinsicBlur#setRadius}）
     * @return 模糊后的新位图
     */
    @TargetApi(17)
    private static Bitmap renderScriptBlur(Context context, Bitmap bitmap, float radius) {
        Bitmap blurredBitmap = Bitmap.createBitmap(bitmap);
        RenderScript renderScript = RenderScript.create(context);
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        Allocation inputAllocation = Allocation.createFromBitmap(renderScript, bitmap);
        Allocation outputAllocation = Allocation.createFromBitmap(renderScript, blurredBitmap);
        scriptIntrinsicBlur.setRadius(radius);
        scriptIntrinsicBlur.setInput(inputAllocation);
        scriptIntrinsicBlur.forEach(outputAllocation);
        outputAllocation.copyTo(blurredBitmap);
        renderScript.destroy();
        Bitmap maskBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_blur_mask);
        Canvas canvas = new Canvas(blurredBitmap);
        Paint paint = new Paint();
        paint.setFlags(2);
        canvas.drawBitmap(maskBitmap, new Matrix(), paint);
        return blurredBitmap;
    }

    /**
     * 对 View 做固定半径（25）的高斯模糊。
     *
     * <p>流程：销毁旧缓存 → 打开绘制缓存 → 把整窗内容画到一张 ARGB_8888 位图上（先铺黑色底，
     * {@code -16777216} 即不透明黑）→ RenderScript 模糊 25。</p>
     *
     * @param view    目标 View
     * @param context 上下文
     * @return 模糊后的位图；view 为 null 或拿不到位图时返回 null
     */
    public static Bitmap blurView(View view, Context context) {
        if (view == null) {
            LogUtil.e("传入view = null");
            return null;
        }
        LogUtil.i("originView width=" + view.getWidth() + "originView height=" + view.getHeight());
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(true);
        try {
            Bitmap drawingCache = view.getDrawingCache();
            if (drawingCache == null || drawingCache.isRecycled()) {
                return null;
            }
            Rect rect = new Rect();
            rect.left = 0;
            rect.top = 0;
            rect.right = view.getWidth();
            rect.bottom = view.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(-16777216);
            canvas.drawBitmap(Bitmap.createBitmap(drawingCache), rect, rect, (Paint) null);
            LogUtil.i("newCache=" + drawingCache);
            return renderScriptBlur(context, bitmap, 25.0f);
        } catch (Exception e) {
            LogUtil.e("高斯模糊传入view宽高过大，内存不够");
            return null;
        }
    }

    /**
     * 对 View 做指定半径的高斯模糊。
     *
     * <p>与 {@link #blurView(View, Context)} 的差异：这里直接拿绘制缓存交给 RenderScript，
     * 不再拷到新画布上铺黑底，所以半径可调、耗时更短；缓存为 null 时会直接传进
     * RenderScript 从而抛异常，被下面的 catch 兜住。</p>
     *
     * @param view    目标 View
     * @param context 上下文
     * @param radius  模糊半径
     * @return 模糊后的位图；view 为 null 或发生异常时返回 null
     */
    public static Bitmap blurView(View view, Context context, float radius) {
        if (view != null) {
            LogUtil.i("originView width=" + view.getWidth() + "originView height=" + view.getHeight());
            view.destroyDrawingCache();
            view.setDrawingCacheEnabled(true);
            try {
                Bitmap drawingCache = view.getDrawingCache();
                LogUtil.i("cache=" + drawingCache);
                return renderScriptBlur(context, drawingCache, radius);
            } catch (Exception e) {
                LogUtil.e("高斯模糊传入view宽高过大，内存不够");
            }
        } else {
            LogUtil.e("传入view = null");
        }
        return null;
    }
}
