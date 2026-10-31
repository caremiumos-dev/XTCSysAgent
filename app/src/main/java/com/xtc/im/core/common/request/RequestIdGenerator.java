package com.xtc.im.core.common.request;

import com.xtc.im.core.common.response.entity.ResponseEntity;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求号生成器。
 *
 * <p>请求号是 {@code 前缀 * 100000 + 自增序号}：前缀由宿主（业务方）在构造时指定，
 * 用来区分"这次连接是谁的请求"；自增序号从 1 开始，到 10000 就绕回 1。</p>
 *
 * <p>之所以要用前缀，是因为同一条长连接上会混跑多个业务的请求，
 * 收到响应时只能靠请求号区间判断"这条响应是不是我这边的"。</p>
 *
 * <p>原厂类名为单字母 {@code g}。</p>
 */
public class RequestIdGenerator {

    /** 前缀上限：请求号 = 前缀 * 100000 + 序号。 */
    private static final int PREFIX_SCALE = 100000;

    /** 序号回绕阈值。 */
    private static final int SEQUENCE_LIMIT = 10000;

    /** 当前前缀（静态，最后一次构造生效，与原厂一致）。 */
    private static int sPrefix = 0;

    private final AtomicInteger sequence = new AtomicInteger(1);

    public RequestIdGenerator(int prefix) {
        sPrefix = prefix;
    }

    /**
     * 请求号是否属于当前前缀。
     */
    public static boolean isSameSession(int requestId) {
        return requestId / PREFIX_SCALE == sPrefix;
    }

    /**
     * 响应是否带请求号（没有请求号的响应不归任何人管）。
     */
    public static boolean hasRid(ResponseEntity response) {
        return response.getRid() != 0;
    }

    /**
     * 取下一个请求号。
     */
    public int nextRequestId() {
        if (this.sequence.get() >= SEQUENCE_LIMIT) {
            this.sequence.set(1);
        }
        return (sPrefix * PREFIX_SCALE) + this.sequence.getAndIncrement();
    }
}
