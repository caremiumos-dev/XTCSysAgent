package com.xtc.utils.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;

/**
 * 位图处理大合集：读取/解码、缩放、旋转、倾斜、裁剪、圆角/圆形、模糊、灰度、反光、
 * 水印、边框、质量压缩、落盘，以及 PNG/JPEG/GIF/BMP 文件头嗅探。
 * （原厂类名被混淆成 {@code f}，80 个方法名全部退化成 {@code a/b/c/d}。）
 *
 * <h3>命名与行为的约定</h3>
 * <p>混淆后同一族方法共用字母（例如所有“缩放”都叫 {@code a}），重写时按语义拆开；
 * 但原厂存在几组<b>实现完全相同、签名相同</b>的重复方法（混淆前是两个不同名字），
 * Java 不允许同签名重载，所以用 {@code scale} / {@code resize} 两个名字分别保留，
 * 见 {@link #scale(Bitmap, float, float)} 与 {@link #resize(Bitmap, float, float)}。</p>
 *
 * <h3>必须照抄的“怪行为”</h3>
 * <ul>
 *   <li>{@link #skew(Bitmap, float, float, float, float)} 的 px/py 两个参数被丢弃，
 *       内层调用固定传 {@code 0.0f, 0.0f}；</li>
 *   <li>{@link #getBitmap(byte[], int, int, int)} 忽略 offset 之后的长度差，
 *       统一按 {@code bytes.length} 当长度；</li>
 *   <li>{@link #compressByMaxBytes(Bitmap, long, boolean)} 质量每次减 5，减到负数直接返回 null；</li>
 *   <li>{@link #addTextWatermark(Bitmap, String, float, int, float, float, boolean)} 的 y 坐标
 *       会自动加一个 textSize（基线偏移）；</li>
 *   <li>{@link #getImageRotateDegree(String)} 对 EXIF 里除 3/8 之外的任何值都返回 90；</li>
 *   <li>{@link #save(Bitmap, File, Bitmap.CompressFormat, boolean)} 会先
 *       {@code System.out.println("<宽>, <高>")} 打一行调试输出。</li>
 * </ul>
 */
public class BitmapUtils {

    private BitmapUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 按目标宽高算 {@code inSampleSize}（2 的幂，向下取）。
     *
     * @param options     已带 outWidth / outHeight 的解码参数
     * @param maxWidth    目标最大宽
     * @param maxHeight   目标最大高
     * @return 采样率，两者有一个为 0 时返回 1
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int maxWidth, int maxHeight) {
        int inSampleSize = 1;
        if (maxWidth != 0 && maxHeight != 0) {
            int outHeight = options.outHeight;
            int outWidth = options.outWidth;
            while (true) {
                outHeight >>= 1;
                if (outHeight < maxHeight || (outWidth = outWidth >> 1) < maxWidth) {
                    break;
                }
                inSampleSize <<= 1;
            }
        }
        return inSampleSize;
    }

    /**
     * 用 RenderScript 做高斯模糊（API 17+，半径自动夹到 1~25）。
     *
     * <p>结果直接写回源位图并返回同一个对象，所以传进来的是“可改”的位图。</p>
     *
     * @param context 上下文
     * @param bitmap  源位图
     * @param radius  期望半径
     * @return 模糊后的位图（就是入参 bitmap）；位图无效时返回 null
     */
    @TargetApi(17)
    public static Bitmap blurByRenderScript(Context context, Bitmap bitmap, float radius) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        RenderScript renderScript = null;
        try {
            renderScript = RenderScript.create(context);
            renderScript.setMessageHandler(new RenderScript.RSMessageHandler());
            Allocation inputAllocation = Allocation.createFromBitmap(renderScript, bitmap, Allocation.MipmapControl.MIPMAP_NONE, 1);
            Allocation outputAllocation = Allocation.createTyped(renderScript, inputAllocation.getType());
            ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            if (radius > 25.0f) {
                radius = 25.0f;
            } else if (radius <= 0.0f) {
                radius = 1.0f;
            }
            scriptIntrinsicBlur.setInput(inputAllocation);
            scriptIntrinsicBlur.setRadius(radius);
            scriptIntrinsicBlur.forEach(outputAllocation);
            outputAllocation.copyTo(bitmap);
            if (renderScript == null) {
                return bitmap;
            }
            renderScript.destroy();
            return bitmap;
        } catch (Throwable th) {
            if (renderScript != null) {
                renderScript.destroy();
            }
            throw th;
        }
    }

    /**
     * 先按比例缩放再模糊。
     *
     * @param context 上下文
     * @param bitmap  源位图
     * @param scale   缩放比例
     * @param radius  模糊半径
     * @return 处理后的位图
     */
    public static Bitmap scaleAndBlur(Context context, Bitmap bitmap, float scale, float radius) {
        return scaleAndBlur(context, bitmap, scale, radius, false);
    }

    /**
     * 先按比例缩放再模糊（核心实现）。
     *
     * <p>缩放比例不为 1 时会再缩回原尺寸，此时新图与中间图互换所有权；
     * {@code recycleSource} 为 true 还会回收入参位图。</p>
     *
     * @param context       上下文
     * @param bitmap        源位图
     * @param scale         缩放比例
     * @param radius        模糊半径
     * @param recycleSource 是否回收源位图
     * @return 处理后的位图
     */
    public static Bitmap scaleAndBlur(Context context, Bitmap bitmap, float scale, float radius, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int scaledWidth = (int) ((width * scale) + 0.5f);
        int scaledHeight = (int) ((height * scale) + 0.5f);
        if (scaledWidth == 0 || scaledHeight == 0) {
            return null;
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        Paint paint = new Paint(3);
        Canvas canvas = new Canvas();
        paint.setColorFilter(new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP));
        canvas.scale(scale, scale);
        canvas.drawBitmap(scaledBitmap, 0.0f, 0.0f, paint);
        Bitmap blurredBitmap = Build.VERSION.SDK_INT >= 17 ? blurByRenderScript(context, scaledBitmap, radius) : fastBlur(scaledBitmap, (int) radius, recycleSource);
        if (scale == 1.0f) {
            return blurredBitmap;
        }
        Bitmap restoredBitmap = Bitmap.createScaledBitmap(blurredBitmap, width, height, true);
        if (blurredBitmap != null && !blurredBitmap.isRecycled()) {
            blurredBitmap.recycle();
        }
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return restoredBitmap;
    }

    /**
     * 从资源解一张位图。
     *
     * @param resources 资源
     * @param resId     图片资源 id
     * @return 位图；resources 为 null 时返回 null
     */
    public static Bitmap getBitmap(Resources resources, int resId) {
        if (resources == null) {
            return null;
        }
        return BitmapFactory.decodeResource(resources, resId);
    }

    /**
     * 从资源解一张按目标尺寸采样的位图。
     *
     * @param resources 资源
     * @param resId     图片资源 id
     * @param maxWidth  目标最大宽
     * @param maxHeight 目标最大高
     * @return 位图；resources 为 null 时返回 null
     */
    public static Bitmap getBitmap(Resources resources, int resId, int maxWidth, int maxHeight) {
        if (resources == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, options);
    }

    /**
     * 裁成圆形（不回收源图）。
     *
     * @param bitmap 源位图
     * @return 圆形位图
     */
    public static Bitmap toCircle(Bitmap bitmap) {
        return toCircle(bitmap, false);
    }

    /**
     * 切圆角（不回收源图）。
     *
     * @param bitmap 源位图
     * @param radius 圆角半径
     * @return 圆角位图
     */
    public static Bitmap toRoundCorner(Bitmap bitmap, float radius) {
        return toRoundCorner(bitmap, radius, false);
    }

    /**
     * 按比例矩阵缩放（不回收源图）。
     *
     * @param bitmap 源位图
     * @param scaleX X 方向缩放系数
     * @param scaleY Y 方向缩放系数
     * @return 缩放后的位图
     */
    public static Bitmap scale(Bitmap bitmap, float scaleX, float scaleY) {
        return scale(bitmap, scaleX, scaleY, false);
    }

    /**
     * 沿 X/Y 方向倾斜。
     *
     * <p><b>注意</b>：形参里的 {@code px / py} 会被丢弃，内层固定传 0；原厂如此，勿改。</p>
     *
     * @param bitmap 源位图
     * @param skewX  X 方向倾斜系数
     * @param skewY  Y 方向倾斜系数
     * @param pivotX 参考点 X（未生效）
     * @param pivotY 参考点 Y（未生效）
     * @return 倾斜后的位图
     */
    public static Bitmap skew(Bitmap bitmap, float skewX, float skewY, float pivotX, float pivotY) {
        return skew(bitmap, skewX, skewY, 0.0f, 0.0f, false);
    }

    /**
     * 沿 X/Y 方向倾斜（核心实现）。
     *
     * @param bitmap        源位图
     * @param skewX         X 方向倾斜系数
     * @param skewY         Y 方向倾斜系数
     * @param pivotX        参考点 X
     * @param pivotY        参考点 Y
     * @param recycleSource 是否回收源位图
     * @return 倾斜后的位图
     */
    public static Bitmap skew(Bitmap bitmap, float skewX, float skewY, float pivotX, float pivotY, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.setSkew(skewX, skewY, pivotX, pivotY);
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * 按比例缩放（用 Matrix.setScale 实现，不回收源图）。
     *
     * @param bitmap 源位图
     * @param scaleX X 方向缩放系数
     * @param scaleY Y 方向缩放系数
     * @param recycleSource 是否回收源位图
     * @return 缩放后的位图
     */
    public static Bitmap scale(Bitmap bitmap, float scaleX, float scaleY, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY);
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * 切圆角。
     *
     * @param bitmap        源位图
     * @param radius        圆角半径
     * @param recycleSource 是否回收源位图
     * @return 圆角位图
     */
    public static Bitmap toRoundCorner(Bitmap bitmap, float radius, boolean recycleSource) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Paint paint = new Paint();
        Canvas canvas = new Canvas(resultBitmap);
        Rect rect = new Rect(0, 0, width, height);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(new RectF(rect), radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * 加倒影（不回收源图）。
     *
     * @param bitmap           源位图
     * @param reflectionHeight 倒影高度
     * @return 加了倒影的位图
     */
    public static Bitmap addReflection(Bitmap bitmap, int reflectionHeight) {
        return addReflection(bitmap, reflectionHeight, false);
    }

    /**
     * 旋转（不回收源图）。
     *
     * @param bitmap  源位图
     * @param degrees 旋转角度
     * @param pivotX  旋转中心 X
     * @param pivotY  旋转中心 Y
     * @return 旋转后的位图
     */
    public static Bitmap rotate(Bitmap bitmap, int degrees, float pivotX, float pivotY) {
        return rotate(bitmap, degrees, pivotX, pivotY, false);
    }

    /**
     * 旋转（核心实现）。
     *
     * <p>角度为 0 时原样返回入参位图（连拷贝都不做）。</p>
     *
     * @param bitmap        源位图
     * @param degrees       旋转角度
     * @param pivotX        旋转中心 X
     * @param pivotY        旋转中心 Y
     * @param recycleSource 是否回收源位图
     * @return 旋转后的位图
     */
    public static Bitmap rotate(Bitmap bitmap, int degrees, float pivotX, float pivotY, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, pivotX, pivotY);
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return resultBitmap;
    }

    /**
     * 缩放到指定宽高（{@code createScaledBitmap}，不回收源图）。
     *
     * @param bitmap    源位图
     * @param newWidth  目标宽
     * @param newHeight 目标高
     * @return 缩放后的位图
     */
    public static Bitmap scale(Bitmap bitmap, int newWidth, int newHeight) {
        return scale(bitmap, newWidth, newHeight, false);
    }

    /**
     * 裁剪（不回收源图）。
     *
     * @param bitmap 源位图
     * @param x      起点 X
     * @param y      起点 Y
     * @param width  宽
     * @param height 高
     * @return 裁剪后的位图
     */
    public static Bitmap crop(Bitmap bitmap, int x, int y, int width, int height) {
        return crop(bitmap, x, y, width, height, false);
    }

    /**
     * 裁剪（核心实现）。
     *
     * @param bitmap        源位图
     * @param x             起点 X
     * @param y             起点 Y
     * @param width         宽
     * @param height        高
     * @param recycleSource 是否回收源位图
     * @return 裁剪后的位图
     */
    public static Bitmap crop(Bitmap bitmap, int x, int y, int width, int height, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * 缩放到指定宽高（核心实现）。
     *
     * @param bitmap        源位图
     * @param newWidth      目标宽
     * @param newHeight     目标高
     * @param recycleSource 是否回收源位图
     * @return 缩放后的位图
     */
    public static Bitmap scale(Bitmap bitmap, int newWidth, int newHeight, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Bitmap resultBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * 纯 Java 栈模糊（横向 + 纵向两趟滑动窗口）。
     *
     * <p>与 {@link BlurUtils} 里的实现逐句一致，只是这里不带遮罩、不打耗时日志。
     * 算法细节见 {@link BlurUtils} 的说明；关键在于 {@code stack}/{@code stackPixels}
     * 两个数组在两趟里语义不同，重写时不能“化简”。</p>
     *
     * @param bitmap        源位图
     * @param radius        模糊半径，小于 1 时返回 null
     * @param recycleSource 是否直接改动源位图（true 不拷贝）
     * @return 模糊后的位图
     */
    public static Bitmap fastBlur(Bitmap bitmap, int radius, boolean recycleSource) {
        Bitmap bitmapCopy = recycleSource ? bitmap : bitmap.copy(bitmap.getConfig(), true);
        if (radius < 1) {
            return null;
        }
        int width = bitmapCopy.getWidth();
        int height = bitmapCopy.getHeight();
        int[] pixels = new int[width * height];
        bitmapCopy.getPixels(pixels, 0, width, 0, 0, width, height);
        int maxX = width - 1;
        int maxY = height - 1;
        int pixelCount = width * height;
        int windowSize = radius + radius + 1;
        int[] sumR = new int[pixelCount];
        int[] sumG = new int[pixelCount];
        int[] sumB = new int[pixelCount];
        int[] stack = new int[Math.max(width, height)];
        int divisor = (windowSize + 1) >> 1;
        int divisorSquare = divisor * divisor;
        int[] divideTable = new int[divisorSquare * 256];
        for (int index = 0; index < divisorSquare * 256; index++) {
            divideTable[index] = index / divisorSquare;
        }
        int[][] stackPixels = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, windowSize, 3);
        int radiusPlusOne = radius + 1;
        int writeIndex = 0;
        int rowOffset = 0;
        for (int y = 0; y < height; y++) {
            int rInSum = 0;
            int gInSum = 0;
            int bInSum = 0;
            int rOutSum = 0;
            int gOutSum = 0;
            int bOutSum = 0;
            int rSum = 0;
            int gSum = 0;
            int bSum = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int pixel = pixels[Math.min(maxX, Math.max(offset, 0)) + rowOffset];
                int[] stackPixel = stackPixels[offset + radius];
                stackPixel[0] = (16711680 & pixel) >> 16;
                stackPixel[1] = (65280 & pixel) >> 8;
                stackPixel[2] = pixel & 255;
                int weight = radiusPlusOne - Math.abs(offset);
                rSum += stackPixel[0] * weight;
                gSum += stackPixel[1] * weight;
                bSum += weight * stackPixel[2];
                if (offset > 0) {
                    rInSum += stackPixel[0];
                    gInSum += stackPixel[1];
                    bInSum += stackPixel[2];
                } else {
                    rOutSum += stackPixel[0];
                    gOutSum += stackPixel[1];
                    bOutSum += stackPixel[2];
                }
            }
            int currentRSum = rSum;
            int currentGSum = gSum;
            int currentBSum = bSum;
            int stackPointer = radius;
            for (int x = 0; x < width; x++) {
                sumR[writeIndex] = divideTable[currentRSum];
                sumG[writeIndex] = divideTable[currentGSum];
                sumB[writeIndex] = divideTable[currentBSum];
                int nextRSum = currentRSum - rOutSum;
                int nextGSum = currentGSum - gOutSum;
                int nextBSum = currentBSum - bOutSum;
                int[] outgoingPixel = stackPixels[((stackPointer - radius) + windowSize) % windowSize];
                int prevOutR = rOutSum - outgoingPixel[0];
                int prevOutG = gOutSum - outgoingPixel[1];
                int prevOutB = bOutSum - outgoingPixel[2];
                if (y == 0) {
                    stack[x] = Math.min(x + radius + 1, maxX);
                }
                int newPixel = pixels[stack[x] + rowOffset];
                outgoingPixel[0] = (16711680 & newPixel) >> 16;
                outgoingPixel[1] = (65280 & newPixel) >> 8;
                outgoingPixel[2] = newPixel & 255;
                int newInR = rInSum + outgoingPixel[0];
                int newInG = gInSum + outgoingPixel[1];
                int newInB = bInSum + outgoingPixel[2];
                currentRSum = nextRSum + newInR;
                currentGSum = nextGSum + newInG;
                currentBSum = nextBSum + newInB;
                stackPointer = (stackPointer + 1) % windowSize;
                int[] incomingPixel = stackPixels[stackPointer % windowSize];
                rOutSum = prevOutR + incomingPixel[0];
                gOutSum = prevOutG + incomingPixel[1];
                bOutSum = prevOutB + incomingPixel[2];
                rInSum = newInR - incomingPixel[0];
                gInSum = newInG - incomingPixel[1];
                bInSum = newInB - incomingPixel[2];
                writeIndex++;
            }
            rowOffset += width;
        }
        for (int x = 0; x < width; x++) {
            int bInSum = 0;
            int columnOffset = (-radius) * width;
            int rInSum = 0;
            int bOutSum = 0;
            int gOutSum = 0;
            int rOutSum = 0;
            int offset = -radius;
            int bSum = 0;
            int gSum = 0;
            int rSum = 0;
            int gInSum = 0;
            while (offset <= radius) {
                int rowPixelIndex = Math.max(0, columnOffset) + x;
                int[] stackPixel = stackPixels[offset + radius];
                stackPixel[0] = sumR[rowPixelIndex];
                stackPixel[1] = sumG[rowPixelIndex];
                stackPixel[2] = sumB[rowPixelIndex];
                int weight = radiusPlusOne - Math.abs(offset);
                int newRSum = (sumR[rowPixelIndex] * weight) + rSum;
                int newGSum = (sumG[rowPixelIndex] * weight) + gSum;
                int newBSum = (sumB[rowPixelIndex] * weight) + bSum;
                if (offset > 0) {
                    rInSum += stackPixel[0];
                    gInSum += stackPixel[1];
                    bInSum += stackPixel[2];
                } else {
                    rOutSum += stackPixel[0];
                    gOutSum += stackPixel[1];
                    bOutSum += stackPixel[2];
                }
                if (offset < maxY) {
                    columnOffset += width;
                }
                offset++;
                bSum = newBSum;
                gSum = newGSum;
                rSum = newRSum;
            }
            int currentGSum = gSum;
            int currentRSum = rSum;
            int currentBSum = bSum;
            int writeIndex2 = x;
            int currentBInSum = bInSum;
            int currentGInSum = gInSum;
            int currentRInSum = rInSum;
            int currentBOutSum = bOutSum;
            int currentGOutSum = gOutSum;
            int currentROutSum = rOutSum;
            int stackPointer = radius;
            for (int y = 0; y < height; y++) {
                pixels[writeIndex2] = ((-16777216) & pixels[writeIndex2]) | (divideTable[currentRSum] << 16) | (divideTable[currentGSum] << 8) | divideTable[currentBSum];
                int nextRSum = currentRSum - currentROutSum;
                int nextGSum = currentGSum - currentGOutSum;
                int nextBSum = currentBSum - currentBOutSum;
                int[] outgoingPixel = stackPixels[((stackPointer - radius) + windowSize) % windowSize];
                int prevOutR = currentROutSum - outgoingPixel[0];
                int prevOutG = currentGOutSum - outgoingPixel[1];
                int prevOutB = currentBOutSum - outgoingPixel[2];
                if (x == 0) {
                    stack[y] = Math.min(y + radiusPlusOne, maxY) * width;
                }
                int rowPixelIndex = stack[y] + x;
                outgoingPixel[0] = sumR[rowPixelIndex];
                outgoingPixel[1] = sumG[rowPixelIndex];
                outgoingPixel[2] = sumB[rowPixelIndex];
                int newInR = currentRInSum + outgoingPixel[0];
                int newInG = currentGInSum + outgoingPixel[1];
                int newInB = currentBInSum + outgoingPixel[2];
                currentRSum = nextRSum + newInR;
                currentGSum = nextGSum + newInG;
                currentBSum = nextBSum + newInB;
                stackPointer = (stackPointer + 1) % windowSize;
                int[] incomingPixel = stackPixels[stackPointer];
                currentROutSum = prevOutR + incomingPixel[0];
                currentGOutSum = prevOutG + incomingPixel[1];
                currentBOutSum = prevOutB + incomingPixel[2];
                currentRInSum = newInR - incomingPixel[0];
                currentGInSum = newInG - incomingPixel[1];
                currentBInSum = newInB - incomingPixel[2];
                writeIndex2 += width;
            }
        }
        bitmapCopy.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmapCopy;
    }

    /**
     * 压到指定字节数以内（不回收源图）。
     *
     * @param bitmap      源位图
     * @param maxByteSize 目标最大字节数
     * @return 压缩后的位图
     */
    public static Bitmap compressByMaxBytes(Bitmap bitmap, long maxByteSize) {
        return compressByMaxBytes(bitmap, maxByteSize, false);
    }

    /**
     * 压到指定字节数以内：从质量 100 开始每次减 5，直到字节数达标或质量减到负数。
     *
     * @param bitmap        源位图
     * @param maxByteSize   目标最大字节数
     * @param recycleSource 是否回收源位图
     * @return 压缩后的位图；质量减到负数仍未达标时返回 null
     */
    public static Bitmap compressByMaxBytes(Bitmap bitmap, long maxByteSize, boolean recycleSource) {
        if (isBitmapEmpty(bitmap) || maxByteSize <= 0) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int quality = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        while (outputStream.toByteArray().length > maxByteSize && quality >= 0) {
            outputStream.reset();
            quality -= 5;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }
        if (quality < 0) {
            return null;
        }
        byte[] bytes = outputStream.toByteArray();
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 叠一张图片水印（不回收源图）。
     *
     * @param bitmap    源位图
     * @param watermark 水印位图
     * @param x         水印左上角 X
     * @param y         水印左上角 Y
     * @param alpha     水印透明度 0~255
     * @return 加了水印的位图
     */
    public static Bitmap addImageWatermark(Bitmap bitmap, Bitmap watermark, int x, int y, int alpha) {
        return addImageWatermark(bitmap, watermark, x, y, alpha, false);
    }

    /**
     * 叠一张图片水印（核心实现）。
     *
     * @param bitmap        源位图
     * @param watermark     水印位图
     * @param x             水印左上角 X
     * @param y             水印左上角 Y
     * @param alpha         水印透明度 0~255
     * @param recycleSource 是否回收源位图
     * @return 加了水印的位图
     */
    public static Bitmap addImageWatermark(Bitmap bitmap, Bitmap watermark, int x, int y, int alpha, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(), true);
        if (!isBitmapEmpty(watermark)) {
            Paint paint = new Paint(1);
            Canvas canvas = new Canvas(bitmapCopy);
            paint.setAlpha(alpha);
            canvas.drawBitmap(watermark, x, y, paint);
        }
        if (!recycleSource || bitmap.isRecycled()) {
            return bitmapCopy;
        }
        bitmap.recycle();
        return bitmapCopy;
    }

    /**
     * 只保留 alpha 通道（提取轮廓）。
     *
     * @param bitmap        源位图
     * @param recycleSource 是否回收源位图（包装类型，原厂如此，传 null 会 NPE）
     * @return 提取出的 alpha 位图
     */
    public static Bitmap toAlpha(Bitmap bitmap, Boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Bitmap alphaBitmap = bitmap.extractAlpha();
        if (!recycleSource.booleanValue() || bitmap.isRecycled()) {
            return alphaBitmap;
        }
        bitmap.recycle();
        return alphaBitmap;
    }

    /**
     * 叠文字水印（核心实现）。
     *
     * <p>绘制基线是 {@code y + textSize}，即调用方传的 y 会被自动下移一个字高。</p>
     *
     * @param bitmap        源位图
     * @param text          水印文字
     * @param textSize      字号
     * @param color         文字颜色
     * @param x             文字起点 X
     * @param y             文字起点 Y（会再加一个 textSize）
     * @param recycleSource 是否回收源位图
     * @return 加了水印的位图
     */
    public static Bitmap addTextWatermark(Bitmap bitmap, String text, float textSize, int color, float x, float y, boolean recycleSource) {
        if (isBitmapEmpty(bitmap) || text == null) {
            return null;
        }
        Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(), true);
        Paint paint = new Paint(1);
        Canvas canvas = new Canvas(bitmapCopy);
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), new Rect());
        canvas.drawText(text, x, y + textSize, paint);
        if (!recycleSource || bitmap.isRecycled()) {
            return bitmapCopy;
        }
        bitmap.recycle();
        return bitmapCopy;
    }

    /**
     * 叠文字水印（不回收源图）。
     *
     * @param bitmap   源位图
     * @param text     水印文字
     * @param textSize 字号
     * @param color    文字颜色
     * @param x        文字起点 X
     * @param y        文字起点 Y
     * @return 加了水印的位图
     */
    public static Bitmap addTextWatermark(Bitmap bitmap, String text, int textSize, int color, float x, float y) {
        return addTextWatermark(bitmap, text, textSize, color, x, y, false);
    }

    /**
     * 裁成圆形。
     *
     * @param bitmap        源位图
     * @param recycleSource 是否回收源位图
     * @return 圆形位图
     */
    public static Bitmap toCircle(Bitmap bitmap, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int radius = Math.min(width, height) >> 1;
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Paint paint = new Paint();
        Canvas canvas = new Canvas(resultBitmap);
        Rect rect = new Rect(0, 0, width, height);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(width >> 1, height >> 1, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * Drawable 转 Bitmap（强转 BitmapDrawable）。
     *
     * @param drawable 源 Drawable
     * @return 位图；drawable 为 null 时返回 null
     */
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        return ((BitmapDrawable) drawable).getBitmap();
    }

    /**
     * View 转 Bitmap。
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
     * 从文件解码位图。
     *
     * @param file 源文件
     * @return 位图；文件为 null、不存在或解码失败时返回 null
     */
    public static Bitmap getBitmap(File file) {
        BufferedInputStream inputStream = null;
        Bitmap bitmap = null;
        try {
            if (file != null) {
                inputStream = new BufferedInputStream(new FileInputStream(file));
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            FileIoUtils.closeIO(inputStream);
        }
        return bitmap;
    }

    /**
     * 从文件解码按目标尺寸采样的位图。
     *
     * @param file      源文件
     * @param maxWidth  目标最大宽
     * @param maxHeight 目标最大高
     * @return 位图；文件为 null 或解码失败时返回 null
     */
    public static Bitmap getBitmap(File file, int maxWidth, int maxHeight) {
        BufferedInputStream inputStream = null;
        Bitmap bitmap = null;
        try {
            if (file != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                inputStream = new BufferedInputStream(new FileInputStream(file));
                BitmapFactory.decodeStream(inputStream, null, options);
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            FileIoUtils.closeIO(inputStream);
        }
        return bitmap;
    }

    /**
     * 从文件描述符解码位图。
     *
     * @param fileDescriptor 文件描述符
     * @return 位图；入参为 null 时返回 null
     */
    public static Bitmap getBitmap(FileDescriptor fileDescriptor) {
        if (fileDescriptor == null) {
            return null;
        }
        return BitmapFactory.decodeFileDescriptor(fileDescriptor);
    }

    /**
     * 从文件描述符解码按目标尺寸采样的位图。
     *
     * @param fileDescriptor 文件描述符
     * @param maxWidth       目标最大宽
     * @param maxHeight      目标最大高
     * @return 位图；入参为 null 时返回 null
     */
    public static Bitmap getBitmap(FileDescriptor fileDescriptor, int maxWidth, int maxHeight) {
        if (fileDescriptor == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    /**
     * 从输入流解码按目标尺寸采样的位图。
     *
     * @param inputStream 输入流（只读一次，采样后无法回绕）
     * @param maxWidth    目标最大宽
     * @param maxHeight   目标最大高
     * @return 位图；入参为 null 时返回 null
     */
    public static Bitmap getBitmap(InputStream inputStream, int maxWidth, int maxHeight) {
        if (inputStream == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(inputStream, null, options);
    }

    /**
     * 从文件路径解码位图。
     *
     * @param filePath 文件路径
     * @return 位图；路径为空或全空格时返回 null
     */
    public static Bitmap getBitmap(String filePath) {
        if (TextUtils.isEmpty(filePath.trim())) {
            return null;
        }
        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * 从文件路径解码按目标尺寸采样的位图。
     *
     * @param filePath  文件路径
     * @param maxWidth  目标最大宽
     * @param maxHeight 目标最大高
     * @return 位图；路径为空或全空格时返回 null
     */
    public static Bitmap getBitmap(String filePath, int maxWidth, int maxHeight) {
        if (TextUtils.isEmpty(filePath.trim())) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * 先按路径解，解不到再用资源兜底。
     *
     * @param filePath  文件路径
     * @param resources 兜底资源
     * @param resId     兜底图片资源 id
     * @return 位图；路径无效且 resources 为 null 时返回 null
     */
    public static Bitmap getBitmap(String filePath, Resources resources, int resId) {
        Bitmap bitmap = getBitmap(filePath);
        if (bitmap != null) {
            return bitmap;
        }
        if (resources == null) {
            return null;
        }
        return BitmapFactory.decodeResource(resources, resId);
    }

    /**
     * 字节数组转 Bitmap。
     *
     * @param bytes 图片字节
     * @return 位图；入参为 null 或空数组时返回 null
     */
    public static Bitmap bytes2Bitmap(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 字节数组按目标尺寸采样解码。
     *
     * <p><b>注意</b>：长度参数原厂传的是 {@code bytes.length} 而不是 {@code bytes.length - offset}，
     * 有 offset 时相当于多读了一段，照抄保留。</p>
     *
     * @param bytes     图片字节
     * @param offset    起始偏移
     * @param maxWidth  目标最大宽
     * @param maxHeight 目标最大高
     * @return 位图；空数组返回 null
     */
    public static Bitmap getBitmap(byte[] bytes, int offset, int maxWidth, int maxHeight) {
        if (bytes.length == 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, offset, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, offset, bytes.length, options);
    }

    /**
     * Bitmap 转 Drawable。
     *
     * @param resources 取密度用
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
     * 字节数组转 Drawable。
     *
     * @param resources 取密度用
     * @param bytes     图片字节
     * @return Drawable；resources 为 null 或字节无效时返回 null
     */
    public static Drawable bytes2Drawable(Resources resources, byte[] bytes) {
        if (resources == null) {
            return null;
        }
        return bitmap2Drawable(resources, bytes2Bitmap(bytes));
    }

    /**
     * 保存位图到文件（不回收源图）。
     *
     * @param bitmap 源位图
     * @param file   目标文件
     * @param format 压缩格式
     * @return 是否成功
     */
    public static boolean save(Bitmap bitmap, File file, Bitmap.CompressFormat format) {
        return save(bitmap, file, format, false);
    }

    /**
     * 保存位图到文件（核心实现）。
     *
     * <p>开头会 {@code System.out.println("<宽>, <高>")}，这行调试输出是原厂留下的，保留。
     * 目标文件必须先通过 {@link FileIoUtils#createOrExistsFile(File)} 的检查（原厂判断被取反，
     * 实际等价于“只有文件已存在才继续”）。</p>
     *
     * @param bitmap        源位图
     * @param file          目标文件
     * @param format        压缩格式
     * @param recycleSource 是否回收源位图
     * @return 是否成功
     */
    public static boolean save(Bitmap bitmap, File file, Bitmap.CompressFormat format, boolean recycleSource) {
        if (isBitmapEmpty(bitmap) || !FileIoUtils.createOrExistsFile(file)) {
            return false;
        }
        System.out.println(bitmap.getWidth() + ", " + bitmap.getHeight());
        boolean compressed = false;
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            compressed = bitmap.compress(format, 100, outputStream);
            if (recycleSource && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        } catch (IOException e) {
            e.printStackTrace();
            compressed = false;
        } finally {
            FileIoUtils.closeIO(outputStream);
        }
        return compressed;
    }

    /**
     * 保存位图到路径（不回收源图）。
     *
     * @param bitmap   源位图
     * @param filePath 目标路径
     * @param format   压缩格式
     * @return 是否成功
     */
    public static boolean save(Bitmap bitmap, String filePath, Bitmap.CompressFormat format) {
        return save(bitmap, FileIoUtils.getFileByPath(filePath), format, false);
    }

    /**
     * 保存位图到路径。
     *
     * @param bitmap        源位图
     * @param filePath      目标路径
     * @param format        压缩格式
     * @param recycleSource 是否回收源位图
     * @return 是否成功
     */
    public static boolean save(Bitmap bitmap, String filePath, Bitmap.CompressFormat format, boolean recycleSource) {
        return save(bitmap, FileIoUtils.getFileByPath(filePath), format, recycleSource);
    }

    /**
     * Bitmap 转字节数组（固定 100 质量）。
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

    /**
     * Drawable 转字节数组。
     *
     * @param drawable 源 Drawable
     * @param format   压缩格式
     * @return 字节数组；drawable 为 null 时返回 null
     */
    public static byte[] drawable2Bytes(Drawable drawable, Bitmap.CompressFormat format) {
        if (drawable == null) {
            return null;
        }
        return bitmap2Bytes(drawable2Bitmap(drawable), format);
    }

    /**
     * 读 EXIF 里的旋转角度。
     *
     * <p>映射很粗暴：3 → 180，8 → 270，<b>其它一律 90</b>（包括 1 正立的情况）。
     * 读失败返回 0。原厂如此，调用方需按“非 0 才旋转”使用。</p>
     *
     * @param filePath 图片路径
     * @return 旋转角度
     */
    public static int getImageRotateDegree(String filePath) {
        try {
            switch (new ExifInterface(filePath).getAttributeInt("Orientation", 1)) {
                case 3:
                    return 180;
                case 8:
                    return 270;
                default:
                    return 90;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 只保留 alpha 通道（不回收源图）。
     *
     * @param bitmap 源位图
     * @return 提取出的 alpha 位图
     */
    public static Bitmap toAlpha(Bitmap bitmap) {
        return toAlpha(bitmap, false);
    }

    /**
     * 沿 X/Y 方向倾斜（不回收源图，参考点固定 0）。
     *
     * @param bitmap 源位图
     * @param skewX  X 方向倾斜系数
     * @param skewY  Y 方向倾斜系数
     * @return 倾斜后的位图
     */
    public static Bitmap skew(Bitmap bitmap, float skewX, float skewY) {
        return skew(bitmap, skewX, skewY, 0.0f, 0.0f, false);
    }

    /**
     * 沿 X/Y 方向倾斜（参考点固定 0）。
     *
     * @param bitmap        源位图
     * @param skewX         X 方向倾斜系数
     * @param skewY         Y 方向倾斜系数
     * @param recycleSource 是否回收源位图
     * @return 倾斜后的位图
     */
    public static Bitmap skew(Bitmap bitmap, float skewX, float skewY, boolean recycleSource) {
        return skew(bitmap, skewX, skewY, 0.0f, 0.0f, recycleSource);
    }

    /**
     * 按质量压缩（不回收源图）。
     *
     * @param bitmap  源位图
     * @param quality 质量 0~100
     * @return 压缩后的位图
     */
    public static Bitmap compressByQuality(Bitmap bitmap, int quality) {
        return compressByQuality(bitmap, quality, false);
    }

    /**
     * 加边框（不回收源图）。
     *
     * @param bitmap     源位图
     * @param borderSize 边框厚度（左右各一圈，新图会大 2 倍厚度）
     * @param color      边框颜色
     * @return 加了边框的位图
     */
    public static Bitmap addBorder(Bitmap bitmap, int borderSize, int color) {
        return addBorder(bitmap, borderSize, color, false);
    }

    /**
     * 加边框（核心实现）。
     *
     * @param bitmap        源位图
     * @param borderSize    边框厚度
     * @param color         边框颜色
     * @param recycleSource 是否回收源位图
     * @return 加了边框的位图
     */
    public static Bitmap addBorder(Bitmap bitmap, int borderSize, int color, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        int doubleBorderSize = borderSize << 1;
        int width = bitmap.getWidth() + doubleBorderSize;
        int height = bitmap.getHeight() + doubleBorderSize;
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Canvas canvas = new Canvas(resultBitmap);
        Rect rect = new Rect(0, 0, width, height);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(doubleBorderSize);
        canvas.drawRect(rect, paint);
        canvas.drawBitmap(bitmap, borderSize, borderSize, (Paint) null);
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return resultBitmap;
    }

    /**
     * 加倒影（核心实现）：把底部一段翻折到下方，再用渐变把接缝淡出。
     *
     * @param bitmap           源位图
     * @param reflectionHeight 倒影高度
     * @param recycleSource    是否回收源位图
     * @return 加了倒影的位图
     */
    public static Bitmap addReflection(Bitmap bitmap, int reflectionHeight, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == 0 || height == 0) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        Bitmap reflectionBitmap = Bitmap.createBitmap(bitmap, 0, height - reflectionHeight, width, reflectionHeight, matrix, false);
        if (reflectionBitmap == null) {
            return null;
        }
        Bitmap resultBitmap = Bitmap.createBitmap(width, height + reflectionHeight, bitmap.getConfig());
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
        canvas.drawBitmap(reflectionBitmap, 0.0f, height + 0, (Paint) null);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new LinearGradient(0.0f, height, 0.0f, resultBitmap.getHeight() + 0, 1895825407, ViewCompat.MEASURED_SIZE_MASK, Shader.TileMode.MIRROR));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.save();
        canvas.drawRect(0.0f, height, width, resultBitmap.getHeight() + 0, paint);
        canvas.restore();
        if (!reflectionBitmap.isRecycled()) {
            reflectionBitmap.recycle();
        }
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return resultBitmap;
    }

    /**
     * 转灰度。
     *
     * @param bitmap        源位图
     * @param recycleSource 是否回收源位图
     * @return 灰度位图
     */
    public static Bitmap toGray(Bitmap bitmap, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.0f);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
        if (!recycleSource || bitmap.isRecycled()) {
            return resultBitmap;
        }
        bitmap.recycle();
        return resultBitmap;
    }

    /**
     * 读流头部 8 字节判断图片类型。
     *
     * @param inputStream 输入流
     * @return "JPEG" / "GIF" / "PNG" / "BMP"；读不出或识别不了返回 null
     */
    public static String getImageType(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            byte[] header = new byte[8];
            if (inputStream.read(header, 0, 8) != -1) {
                return getImageType(header);
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 按文件头判断图片类型。
     *
     * <p>判定顺序固定为 JPEG → GIF → PNG → BMP，先命中先返回。</p>
     *
     * @param bytes 字节数组
     * @return "JPEG" / "GIF" / "PNG" / "BMP"；识别不了返回 null
     */
    public static String getImageType(byte[] bytes) {
        if (isJpeg(bytes)) {
            return "JPEG";
        }
        if (isGif(bytes)) {
            return "GIF";
        }
        if (isPng(bytes)) {
            return "PNG";
        }
        if (isBmp(bytes)) {
            return "BMP";
        }
        return null;
    }

    /**
     * 按后缀判断是否图片文件。
     *
     * @param file 文件
     * @return 是图片返回 true；file 为 null 返回 false
     */
    public static boolean isImage(File file) {
        return file != null && isImage(file.getPath());
    }

    /**
     * 转灰度（不回收源图）。
     *
     * @param bitmap 源位图
     * @return 灰度位图
     */
    public static Bitmap toGray(Bitmap bitmap) {
        return toGray(bitmap, false);
    }

    /**
     * 按比例缩放（不回收源图）。
     *
     * <p>与 {@link #scale(Bitmap, float, float)} 实现完全一致；原厂是两个独立方法，
     * 为了保持方法数量与签名一一对应，这里用第二个名字保留。</p>
     *
     * @param bitmap 源位图
     * @param scaleX X 方向缩放系数
     * @param scaleY Y 方向缩放系数
     * @return 缩放后的位图
     */
    public static Bitmap resize(Bitmap bitmap, float scaleX, float scaleY) {
        return scale(bitmap, scaleX, scaleY, false);
    }

    /**
     * 按比例缩放。
     *
     * @param bitmap        源位图
     * @param scaleX        X 方向缩放系数
     * @param scaleY        Y 方向缩放系数
     * @param recycleSource 是否回收源位图
     * @return 缩放后的位图
     */
    public static Bitmap resize(Bitmap bitmap, float scaleX, float scaleY, boolean recycleSource) {
        return scale(bitmap, scaleX, scaleY, recycleSource);
    }

    /**
     * 按采样率压缩（不回收源图）。
     *
     * @param bitmap     源位图
     * @param sampleSize 采样率（&gt;1 才有效果）
     * @return 压缩后的位图
     */
    public static Bitmap compressBySampleSize(Bitmap bitmap, int sampleSize) {
        return compressBySampleSize(bitmap, sampleSize, false);
    }

    /**
     * 缩放到指定宽高。
     *
     * @param bitmap    源位图
     * @param newWidth  目标宽
     * @param newHeight 目标高
     * @return 缩放后的位图
     */
    public static Bitmap resize(Bitmap bitmap, int newWidth, int newHeight) {
        return scale(bitmap, newWidth, newHeight, false);
    }

    /**
     * 缩放到指定宽高。
     *
     * @param bitmap        源位图
     * @param newWidth      目标宽
     * @param newHeight     目标高
     * @param recycleSource 是否回收源位图
     * @return 缩放后的位图
     */
    public static Bitmap resize(Bitmap bitmap, int newWidth, int newHeight, boolean recycleSource) {
        return scale(bitmap, newWidth, newHeight, recycleSource);
    }

    /**
     * 按质量压缩（核心实现）。
     *
     * <p>先按 JPEG 质量压一遍再解码回来，质量越低成本越低（画质也越差）。</p>
     *
     * @param bitmap        源位图
     * @param quality       质量 0~100，超出范围返回 null
     * @param recycleSource 是否回收源位图
     * @return 压缩后的位图
     */
    public static Bitmap compressByQuality(Bitmap bitmap, int quality, boolean recycleSource) {
        if (isBitmapEmpty(bitmap) || quality < 0 || quality > 100) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] bytes = outputStream.toByteArray();
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 读文件头部判断图片类型。
     *
     * @param file 文件
     * @return "JPEG" / "GIF" / "PNG" / "BMP"；file 为 null 或读取失败返回 null
     */
    public static String getImageType(File file) {
        FileInputStream inputStream = null;
        String imageType = null;
        try {
            if (file != null) {
                inputStream = new FileInputStream(file);
                imageType = getImageType(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileIoUtils.closeIO(inputStream);
        }
        return imageType;
    }

    /**
     * 按后缀判断是否图片路径。
     *
     * <p>先整体转大写再比后缀，所以 {@code .png / .PNG} 都认；只认
     * PNG / JPG / JPEG / BMP / GIF 五种。</p>
     *
     * @param filePath 文件路径
     * @return 是图片返回 true
     */
    public static boolean isImage(String filePath) {
        String upperCasePath = filePath.toUpperCase();
        return upperCasePath.endsWith(".PNG") || upperCasePath.endsWith(".JPG") || upperCasePath.endsWith(".JPEG") || upperCasePath.endsWith(".BMP") || upperCasePath.endsWith(".GIF");
    }

    /**
     * 是否 JPEG（{@code FF D8}）。
     *
     * @param bytes 字节数组
     * @return 是返回 true
     */
    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == -1 && bytes[1] == -40;
    }

    /**
     * 按采样率压缩（核心实现）。
     *
     * @param bitmap        源位图
     * @param sampleSize    采样率
     * @param recycleSource 是否回收源位图
     * @return 压缩后的位图
     */
    public static Bitmap compressBySampleSize(Bitmap bitmap, int sampleSize, boolean recycleSource) {
        if (isBitmapEmpty(bitmap)) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] bytes = outputStream.toByteArray();
        if (recycleSource && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    /**
     * 读文件头部判断图片类型。
     *
     * @param filePath 文件路径
     * @return "JPEG" / "GIF" / "PNG" / "BMP"；路径为空或读不到返回 null
     */
    public static String getImageType(String filePath) {
        return getImageType(FileIoUtils.getFileByPath(filePath));
    }

    /**
     * 位图是否无效（null、宽 0 或高 0）。
     *
     * @param bitmap 位图
     * @return 无效返回 true
     */
    private static boolean isBitmapEmpty(Bitmap bitmap) {
        return bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0;
    }

    /**
     * 是否 GIF（{@code GIF87a / GIF89a}）。
     *
     * @param bytes 字节数组
     * @return 是返回 true
     */
    private static boolean isGif(byte[] bytes) {
        return bytes.length >= 6 && bytes[0] == 71 && bytes[1] == 73 && bytes[2] == 70 && bytes[3] == 56 && (bytes[4] == 55 || bytes[4] == 57) && bytes[5] == 97;
    }

    /**
     * 是否 PNG（{@code 89 50 4E 47 0D 0A 1A 0A}）。
     *
     * @param bytes 字节数组
     * @return 是返回 true
     */
    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8 && bytes[0] == -119 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
    }

    /**
     * 是否 BMP（{@code 42 4D}）。
     *
     * @param bytes 字节数组
     * @return 是返回 true
     */
    private static boolean isBmp(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 66 && bytes[1] == 77;
    }

    /**
     * 从流解码位图（实例方法，原厂遗留）。
     *
     * <p>原厂这个类还有私有构造器（禁止实例化），所以这两个实例方法实际不可达；
     * 为了保持类的方法签名与 APK 完全一致，仍然按实例方法保留。</p>
     *
     * @param inputStream 输入流
     * @return 位图；入参为 null 时返回 null
     */
    public Bitmap decodeBitmap(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        return BitmapFactory.decodeStream(inputStream);
    }

    /**
     * 从字节数组解码位图（实例方法，原厂遗留）。
     *
     * <p>长度参数原厂传的是 {@code bytes.length}，非 {@code bytes.length - offset}，照抄。</p>
     *
     * @param bytes  图片字节
     * @param offset 起始偏移
     * @return 位图；空数组返回 null
     */
    public Bitmap decodeBitmap(byte[] bytes, int offset) {
        if (bytes.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bytes, offset, bytes.length);
    }
}
