package com.xtc.database.core;

import rx.Subscription;
import rx.subscriptions.BooleanSubscription;

/**
 * RxJava 1.x 的小工具，避免对 null 订阅做判空。
 *
 * <p>反编译源码里的 {@code rx.i.b} 是混淆后的 {@link BooleanSubscription}，
 * {@code rx.m} 是 {@link Subscription}。</p>
 */
public class RxUtils {

    /** 返回一个可安全调用的订阅对象；入参为 null 或已取消时给一个新的空订阅。 */
    public static BooleanSubscription safeSubscription(BooleanSubscription subscription) {
        if (subscription == null || subscription.isUnsubscribed()) {
            return new BooleanSubscription();
        }
        return subscription;
    }

    /** 安全取消订阅。 */
    public static void unsubscribe(Subscription subscription) {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }
}
