package com.xtc.database.core;

import java.util.Collection;
import java.util.List;

/**
 * 集合相关的判空与查找工具。
 */
public class CollectionUtils {

    /** {@code value} 是否出现在 {@code list} 中。 */
    public static boolean contains(String value, List<String> list) {
        if (list == null || value == null) {
            return false;
        }
        for (String item : list) {
            if (value.equals(item)) {
                return true;
            }
        }
        return false;
    }

    /** 集合是否为 null 或不含元素。 */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }
}
