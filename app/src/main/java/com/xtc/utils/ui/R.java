package com.xtc.utils.ui;

/**
 * UI 工具库自身引用的资源（原厂由 aapt 生成，资源 id 与宿主应用同一份）。
 *
 * <p>反编译里这个类没有被混淆（{@code com.xtc.utils.ui.R}），但它的两个资源 id 是 aapt 分配的常量，
 * 与宿主资源的编号一一对应，必须原样保留，不能换成别的 id。</p>
 */
public final class R {

    /** 高斯模糊用的遮罩图，模糊完成后盖在结果上，压掉噪点。 */
    public static final class drawable {
        public static final int ic_blur_mask = 0x7f060055;
    }

    /** 库内字符串资源。 */
    public static final class string {
        public static final int app_name = 0x7f0b0028;
    }
}
