package com.xtc.im.core.common.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实体类对应的命令号。
 *
 * <p>IM 协议里每个报文的首个 TLV 的 tag 就是命令号，运行时通过反射读这个注解决定
 * “这条字节流该还原成哪个实体”。注解必须留在 class 文件里（{@code RUNTIME}），
 * 否则反射拿不到。</p>
 *
 * <p>原厂拼写就是 {@code anotation}（少一个 n），改包名会破坏与外部 APK 的类名约定，
 * 因此保持原样。</p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandValue {

    /** 命令号，默认 0 表示“不是具体命令”（基类用）。 */
    int value() default 0;
}
