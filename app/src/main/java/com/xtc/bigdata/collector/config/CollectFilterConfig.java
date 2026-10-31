package com.xtc.bigdata.collector.config;

import com.xtc.log.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能点过滤配置的容器。
 *
 * <p>键是 {@code "包名+功能名"}（{@link FilterInfoManager#ADD_MASK} 拼接），
 * 值是"是否采集"。默认空 map —— 注意空 map 的语义是<b>没配置</b>，
 * 不是"全都不采集"，判断全在调用方。</p>
 */
public class CollectFilterConfig {

    private static final String TAG = "CollectFilterConfig";

    /** 功能点过滤表：key = 包名+功能名，value = 是否采集。 */
    private Map<String, Boolean> filterFunctionItemMap = new HashMap<String, Boolean>();

    /**
     * 取过滤表。
     *
     * @return 过滤表（可能被外部直接改）
     */
    public Map<String, Boolean> getFilterFunctionItemMap() {
        return this.filterFunctionItemMap;
    }

    /**
     * 设置过滤表（整体替换）。
     *
     * @param filterFunctionItemMap 新过滤表
     */
    public void setFilterFunctionItemMap(Map<String, Boolean> filterFunctionItemMap) {
        LogUtil.d(TAG, "setFilterFunctionItemMap = " + filterFunctionItemMap);
        this.filterFunctionItemMap = filterFunctionItemMap;
    }
}
