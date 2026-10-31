package com.xtc.im.common;

/**
 * IM 公共库自带的资源索引。
 *
 * <p>这些 id 是 {@code com.xtc.im.common} 这个小库自己的 R 值，与宿主的
 * {@code com.xtc.systemagent.R} 无关；数值必须与原厂一致，否则运行期查不到资源。</p>
 *
 * <p>与 AGP 生成的 R 一样是纯常量容器，构造器保持隐式（不加私有构造器），
 * 避免与原厂签名产生任何差异。</p>
 */
public final class R {

    /** 字符串资源。 */
    public static final class string {

        /** 应用名。 */
        public static final int app_name = 0x7f0b0028;
    }
}
