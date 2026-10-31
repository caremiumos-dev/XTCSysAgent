package com.xtc.httplib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单次请求的超时覆盖注解。
 *
 * <p>标注在 Retrofit 服务接口的方法上，由 {@code TimeoutInterceptor} 在拦截链里读取。
 * 四个值都默认 {@code -1}，表示"这一项不覆盖、继续用 OkHttpClient 的全局超时"——
 * 所以 {@code -1} 不是"零超时"，而是"未设置"的哨兵值，判断时必须用 {@code >= 0}。</p>
 *
 * <p>为什么保留到运行时（{@link RetentionPolicy#RUNTIME}）：注解在拦截阶段
 * 通过 {@code retrofit2.Invocation} 反射读取，编译期保留是拿不到的。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {

    /** 同时覆盖连接、读、写三个超时（毫秒）。 */
    int all() default -1;

    /** 只覆盖连接超时（毫秒）。 */
    int connect() default -1;

    /** 只覆盖读超时（毫秒）。 */
    int read() default -1;

    /** 只覆盖写超时（毫秒）。 */
    int write() default -1;
}
