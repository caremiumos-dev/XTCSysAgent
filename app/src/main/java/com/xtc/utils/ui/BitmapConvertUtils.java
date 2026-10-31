package com.xtc.utils.ui;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.view.View;

import java.io.ByteArrayOutputStream;

/**
 * Bitmap 与 Drawable / View / byte[] 之间的互转工具（原厂类名被混淆成 {@code a}）。
 *
 * <p>相对 {@link BitmapUtils} 里的同名方法，这里是更“薄”的一层：不做缩放、不做回收，
 * 只负责类型转换。调用方多为列表适配器与本地缓存层。</p>
 *
 * <p>两个必须注意的细节：</p>
 * <ul>
 *   <li>{@link #drawable2Bitmap(Drawable)} 只对 {@link NinePatchDrawable} 走“重绘成位图”的
 *       分支，其余实现一律强转 {@link BitmapDrawable}，传入 ColorDrawable 之类会 ClassCastException；</li>
 *   <li>九宫格重绘时按 {@code getOpacity() != -1} 选像素格式：不透明优先 RGB_565，
 *       透明才用 ARGB_8888，等于顺手省了一半内存。</li>
 * </ul>
 */
public class BitmapConvertUtils {

    /**
     * Drawable 转 Bitmap。
     *
     * @param drawable 源 Drawable
     * @return 位图；九宫格会新建一张与固有尺寸等大的位图重绘，其余直接取内部 Bitmap
     */
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        if (!(drawable instanceof NinePatchDrawable)) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != -1 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * View 转 Bitmap（把 View 及其背景画到一张 ARGB_8888 位图上）。
     *
     * <p>View 没有背景时先铺一层白色（{@code drawColor(-1)}），否则透明区域会变黑。</p>
     *
     * @param view 源 View
     * @return 位图；view 为 null 时返回 null
     */
    public static Bitmap view2Bitmap(View view) {
        if (view == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable background = view.getBackground();
        if (background != null) {
            background.draw(canvas);
        } else {
            canvas.drawColor(-1);
        }
        view.draw(canvas);
        return bitmap;
    }

    /**
     * 字节数组转 Bitmap。
     *
     * @param bytes 图片字节，可为空
     * @return 位图；入参为 null 或空数组时返回 null
     */
    public static Bitmap bytes2Bitmap(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Bitmap 转 Drawable。
     *
     * @param resources 用来取密度等显示参数
     * @param bitmap    源位图
     * @return Drawable；bitmap 为 null 时返回 null
     */
    public static Drawable bitmap2Drawable(Resources resources, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        return new BitmapDrawable(resources, bitmap);
    }

    /**
     * Bitmap 转字节数组（固定 100 质量压缩）。
     *
     * @param bitmap 源位图
     * @param format 压缩格式
     * @return 字节数组；bitmap 为 null 时返回 null
     */
    public static byte[] bitmap2Bytes(Bitmap bitmap, Bitmap.CompressFormat format) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(format, 100, outputStream);
        return outputStream.toByteArray();
    }
}
