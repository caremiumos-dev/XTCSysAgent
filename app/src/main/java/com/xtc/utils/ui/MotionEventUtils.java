package com.xtc.utils.ui;

import android.view.MotionEvent;

import com.xtc.log.LogUtil;

/**
 * MotionEvent 取坐标的“防崩”包装（原厂类名被混淆成 {@code g}）。
 *
 * <p>多指手势处理里常见一种崩溃：手指抬起后 {@code historySize} / pointer 下标失效，
 * {@link MotionEvent#getX(int)} 会抛 {@link IllegalArgumentException}。
 * 这里把四个取坐标 API 全部包一层 try-catch，出错时记日志并返回 0，
 * 让上层手势算法继续跑而不是直接崩掉。</p>
 *
 * <p>四个方法都只吞 {@link IllegalArgumentException}，其它异常照旧上抛。</p>
 */
public class MotionEventUtils {

    /** 日志 TAG。原厂是写死的字符串字面量，不是 {@code getSimpleName()} 的结果。 */
    public static final String TAG = "MotionEventUtils";

    /**
     * 取第一个触点的 X 坐标。
     *
     * @param event 触摸事件
     * @return X 坐标；取不到时返回 0
     */
    public static float getX(MotionEvent event) {
        try {
            return event.getX();
        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, e);
            return 0.0f;
        }
    }

    /**
     * 取指定触点的 X 坐标。
     *
     * @param event        触摸事件
     * @param pointerIndex 触点下标
     * @return X 坐标；下标越界时返回 0
     */
    public static float getX(MotionEvent event, int pointerIndex) {
        try {
            return event.getX(pointerIndex);
        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, e);
            return 0.0f;
        }
    }

    /**
     * 取第一个触点的 Y 坐标。
     *
     * @param event 触摸事件
     * @return Y 坐标；取不到时返回 0
     */
    public static float getY(MotionEvent event) {
        try {
            return event.getY();
        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, e);
            return 0.0f;
        }
    }

    /**
     * 取指定触点的 Y 坐标。
     *
     * @param event        触摸事件
     * @param pointerIndex 触点下标
     * @return Y 坐标；下标越界时返回 0
     */
    public static float getY(MotionEvent event, int pointerIndex) {
        try {
            return event.getY(pointerIndex);
        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, e);
            return 0.0f;
        }
    }
}
