package com.xtc.bigdata.collector.utils;

import android.text.TextUtils;

/**
 * 扩展判定串的拼接与解析。
 *
 * <p>判定串的形态是 {@code "|类型|"}，可以多个拼在一起。
 * 用竖线包裹是为了让 {@code contains} 不会把 "1" 误判成 "11" 的一部分。</p>
 */
public class Judgment {

    /** 判定串的分隔符。 */
    private static final String SYMBOL = "|";

    /** 判定类型：使用了移动数据流量。 */
    public static final int TYPE_MOBILE_TRAFFIC = 1;

    private Judgment() {
    }

    /**
     * 追加一个判定。
     *
     * @param judgment 原判定串，可为空
     * @param type     判定类型
     * @return 追加后的串；已存在则原样返回
     */
    public static String addJudgment(String judgment, int type) {
        String symbolPart = format(type);
        if (TextUtils.isEmpty(judgment)) {
            return symbolPart;
        }
        return !judgment.contains(symbolPart) ? judgment + symbolPart : judgment;
    }

    /**
     * 把判定类型包成 {@code "|type|"}。
     *
     * @param type 判定类型
     * @return 带分隔符的判定
     */
    private static String format(int type) {
        return SYMBOL + type + SYMBOL;
    }

    /**
     * 判定串里是否存在某类型。
     *
     * @param judgment 判定串
     * @param type     判定类型
     * @return 是否存在
     */
    public static boolean isExistType(String judgment, int type) {
        return !TextUtils.isEmpty(judgment) && judgment.contains(format(type));
    }

    /**
     * 是否走了移动数据。
     *
     * @param judgment 判定串
     * @return 是否走了移动数据
     */
    public static boolean isMobileTraffic(String judgment) {
        return isExistType(judgment, TYPE_MOBILE_TRAFFIC);
    }

    /**
     * 移除一个判定。
     *
     * @param judgment 原判定串
     * @param type     判定类型
     * @return 移除后的串；本来就没有则原样返回
     */
    public static String removeJudgment(String judgment, int type) {
        String symbolPart = format(type);
        return (TextUtils.isEmpty(judgment) || !judgment.contains(symbolPart)) ? judgment : judgment.replace(symbolPart, "");
    }

    /**
     * 按"是否走移动数据"增删判定。
     *
     * @param judgment         原判定串
     * @param useMobileTraffic 是否走移动数据
     * @return 处理后的串
     */
    public static String useMobileTraffic(String judgment, boolean useMobileTraffic) {
        return useMobileTraffic ? addJudgment(judgment, TYPE_MOBILE_TRAFFIC) : removeJudgment(judgment, TYPE_MOBILE_TRAFFIC);
    }
}
