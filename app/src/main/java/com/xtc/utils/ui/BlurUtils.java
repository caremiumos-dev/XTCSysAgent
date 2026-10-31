package com.xtc.utils.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.View;

import com.xtc.log.LogUtil;

import java.lang.reflect.Array;

/**
 * 纯 Java 实现的“栈模糊（stack blur）”，外加几个 View 截图 / 背景设置的小工具
 * （原厂类名被混淆成 {@code e}）。
 *
 * <p>与 {@link RenderScriptBlurUtils} 的分工：这里不依赖 RenderScript，API 14 就能用，
 * 但复杂度是 O(像素)，半径大时明显慢；两个 pass 的滑动窗口算法与
 * 经典 stack blur 实现一致，只是多了“先把图缩一半再模糊、糊完再贴遮罩”的省时做法。</p>
 *
 * <p>几个不能动的点：</p>
 * <ul>
 *   <li>遮罩图 {@code R.drawable.ic_blur_mask} 在模糊结果<b>之后</b>画，画笔固定 {@code setFlags(2)}
 *       （{@code FILTER_BITMAP_FLAG}）；</li>
 *   <li>缩一半时模糊半径也同步减半，否则边缘强度会变；</li>
 *   <li>两个耗时日志的文案与毫秒差值是原厂的性能基线，保留原样；</li>
 *   <li>{@code doBlur} 在半径 &lt; 1 时返回 null，外层并没有判空——这是原厂行为。</li>
 * </ul>
 */
public class BlurUtils {

    /** 默认模糊半径。 */
    private static final int DEFAULT_BLUR_RADIUS = 25;

    /** 缩图倍数：先缩到 1/2 再模糊，半径同步除以它。 */
    private static final int BLUR_SCALE_FACTOR = 2;

    /**
     * 模糊一张位图，并在结果上叠遮罩。
     *
     * @param context   上下文（取遮罩图资源）
     * @param bitmap    源位图
     * @param radius    模糊半径
     * @param scaleDown 是否先把图缩掉一半（半径同步减半）以提速
     * @return 新的已模糊位图；源位图为 null 时返回 null
     */
    public static Bitmap getBlurBitmap(Context context, Bitmap bitmap, int radius, boolean scaleDown) {
        long startTime = System.currentTimeMillis();
        Bitmap resultBitmap = null;
        Bitmap maskBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_blur_mask);
        if (scaleDown) {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / BLUR_SCALE_FACTOR, bitmap.getHeight() / BLUR_SCALE_FACTOR, true);
            radius /= BLUR_SCALE_FACTOR;
        }
        if (bitmap != null) {
            Bitmap blurredBitmap = doBlur(bitmap, radius, true);
            resultBitmap = Bitmap.createBitmap(blurredBitmap.getWidth(), blurredBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resultBitmap);
            Paint paint = new Paint();
            paint.setFlags(2);
            canvas.drawBitmap(blurredBitmap, 0.0f, 0.0f, paint);
            canvas.drawBitmap(maskBitmap, 0.0f, 0.0f, paint);
        }
        LogUtil.d("getBlurBitmap spend time = " + (System.currentTimeMillis() - startTime));
        return resultBitmap;
    }

    /**
     * 按默认半径（25）模糊位图。
     *
     * @param context   上下文
     * @param bitmap    源位图
     * @param scaleDown 是否先缩一半
     * @return 模糊后的位图
     */
    public static Bitmap getBlurBitmap(Context context, Bitmap bitmap, boolean scaleDown) {
        return getBlurBitmap(context, bitmap, DEFAULT_BLUR_RADIUS, scaleDown);
    }

    /**
     * 先截 View 再模糊，最后关闭绘制缓存。
     *
     * @param context   上下文
     * @param view      源 View
     * @param radius    模糊半径
     * @param scaleDown 是否先缩一半
     * @return 模糊后的位图
     */
    public static Bitmap getBlurBitmap(Context context, View view, int radius, boolean scaleDown) {
        Bitmap blurBitmap = getBlurBitmap(context, getViewBitmap(view), radius, scaleDown);
        clearDrawingCache(view);
        return blurBitmap;
    }

    /**
     * 先截 View 再按默认半径模糊，最后关闭绘制缓存。
     *
     * @param context   上下文
     * @param view      源 View
     * @param scaleDown 是否先缩一半
     * @return 模糊后的位图
     */
    public static Bitmap getBlurBitmap(Context context, View view, boolean scaleDown) {
        return getBlurBitmap(context, view, DEFAULT_BLUR_RADIUS, scaleDown);
    }

    /**
     * 栈模糊核心实现（横向 + 纵向两趟滑动窗口求和）。
     *
     * <p>实现细节与原厂逐句对应，勿“优化”：</p>
     * <ol>
     *   <li>预计算 {@code divideTable}，把“除以窗口权重平方和”换成查表；</li>
     *   <li>横向一趟把每像素的 R/G/B 累加值存进 {@code sumR/sumG/sumB}；</li>
     *   <li>纵向一趟再对三张累加图做同样的滑动窗口，回写时保留原 alpha 字节；</li>
     *   <li>{@code stack} 数组在两趟里语义不同：第一趟存 x 下标，第二趟存 y 对应的行偏移。</li>
     * </ol>
     *
     * @param bitmap      源位图
     * @param radius      模糊半径，小于 1 时返回 null
     * @param reuseSource 是否直接改动源位图（true 不拷贝）
     * @return 模糊后的位图
     */
    private static Bitmap doBlur(Bitmap bitmap, int radius, boolean reuseSource) {
        long startTime = System.currentTimeMillis();
        Bitmap bitmapCopy = reuseSource ? bitmap : bitmap.copy(bitmap.getConfig(), true);
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
        LogUtil.d("doBlur spend time = " + (System.currentTimeMillis() - startTime));
        return bitmapCopy;
    }

    /**
     * 取 View 的绘制缓存（不做模糊）。
     *
     * <p>先 destroy 再 build，等于强制重建缓存；返回的 Bitmap 归 View 所有，
     * 调用方需要保留时请自行拷贝。</p>
     *
     * @param view 目标 View
     * @return 绘制缓存位图
     */
    public static Bitmap getViewBitmap(View view) {
        long startTime = System.currentTimeMillis();
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap drawingCache = view.getDrawingCache();
        LogUtil.d("getViewBitmap spend time = " + (System.currentTimeMillis() - startTime));
        return drawingCache;
    }

    /**
     * Bitmap 转 BitmapDrawable。
     *
     * @param context 上下文（取 Resources）
     * @param bitmap  源位图
     * @return BitmapDrawable
     */
    public static BitmapDrawable bitmap2BitmapDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * 给 View 设置背景图（按 API 版本走两套 API）。
     *
     * @param view           目标 View
     * @param bitmapDrawable 背景图
     */
    public static void setBackground(View view, BitmapDrawable bitmapDrawable) {
        if (Build.VERSION.SDK_INT < 16) {
            view.setBackgroundDrawable(bitmapDrawable);
        } else {
            view.setBackground(bitmapDrawable);
        }
    }

    /**
     * 关闭 View 的绘制缓存（截完图后释放那块内存）。
     *
     * @param view 目标 View
     */
    public static void clearDrawingCache(View view) {
        view.setDrawingCacheEnabled(false);
    }
}
