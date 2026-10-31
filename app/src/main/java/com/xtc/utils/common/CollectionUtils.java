package com.xtc.utils.common;

import java.util.Collection;
import java.util.List;

/**
 * 集合工具。原厂类名为单字母 {@code b}。
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    /** {@code value} 是否在列表中。 */
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

    /** 集合是否为 null 或空。 */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }
}
