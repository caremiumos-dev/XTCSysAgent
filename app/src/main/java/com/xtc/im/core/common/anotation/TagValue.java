package com.xtc.im.core.common.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实体字段对应的 TLV tag 号。
 *
 * <p>编解码时字段名完全不参与协议，只有这里的 tag 号参与；所以字段可以随意改名，
 * 改动这里的数值才是破坏协议。</p>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TagValue {

    /** TLV tag 号。 */
    int value() default 0;
}
