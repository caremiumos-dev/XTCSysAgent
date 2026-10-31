package com.xtc.bigdata.collector.utils;

import java.util.Collection;

/**
 * 集合判空。
 *
 * <p>与 {@code com.xtc.utils.common.CollectionUtils} 是两个类：
 * 这边是采集库自带的极简版本，只判断"null 或 size==0"。</p>
 */
public class CollectionUtil {

    private CollectionUtil() {
    }

    /**
     * 集合是否为空。
     *
     * @param collection 集合，可为 null
     * @return 是否为空
     */
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }
}
