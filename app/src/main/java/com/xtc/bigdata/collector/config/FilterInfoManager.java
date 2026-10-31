package com.xtc.bigdata.collector.config;

import android.text.TextUtils;

import com.xtc.bigdata.collector.utils.CollectionUtil;
import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能点过滤配置的读写。
 *
 * <p>配置以文本形式落在 {@code <BigData>/filterFunctions.txt}，
 * 内容是一段 {@link FilterFunctionConfig} JSON，其中的 {@code v} 又是一段
 * 被转义的 {@code List<FilterFunctionItem>} JSON —— 两层嵌套是服务端的原样格式。</p>
 *
 * <p>写入前必须校验 {@code k}（机型条件）是否包含本机 innerModel：
 * 不匹配就直接删掉本地配置，避免把别的机型的配置带上去。</p>
 */
public class FilterInfoManager {

    /** 包名与功能名之间的分隔符。 */
    public static final String ADD_MASK = "+";

    /** 配置文件在 BigData 目录下的文件名。 */
    private static final String FILE_NAME = "/filterFunctions.txt";

    private static final String TAG = "FilterInfoManager";

    private FilterInfoManager() {
    }

    /**
     * 删除本地配置文件并把内存里的过滤表清空。
     *
     * @param filePath 配置文件路径
     */
    private static void deleteLocalConfig(String filePath) {
        LogUtil.d(TAG, "要保存的配置为空，删除 本地的配置文件 -> " + FileUtils.deleteFile(filePath) + " , 再设置内存中的 map 为空");
        ConfigAgent.getBehaviorConfig().collectFilterConfig.setFilterFunctionItemMap(new HashMap<String, Boolean>());
    }

    /**
     * 从磁盘读取功能点过滤配置。
     *
     * <p>任何一层解析失败都退化成空表；磁盘上压根没有配置文件也返回空表。</p>
     *
     * @return 过滤表（不会返回 null）
     */
    public static synchronized Map<String, Boolean> readFilterFunctions() {
        LogUtil.i(TAG, "readFilterFunctions -->");
        Map<String, Boolean> resultMap = new HashMap<String, Boolean>();
        String bigDataDirPath = FileUtils.getBigDataDirPath();
        if (new File(bigDataDirPath).exists()) {
            String configJson;
            try {
                configJson = FileUtils.readFromFile(bigDataDirPath + FILE_NAME);
            } catch (Exception e) {
                LogUtil.e(TAG, e);
                configJson = null;
            }
            LogUtil.d(TAG, "磁盘 读取出来的json字符串为 = " + configJson);
            if (!TextUtils.isEmpty(configJson)) {
                List<FilterFunctionItem> emptyItems = new ArrayList<FilterFunctionItem>();
                FilterFunctionConfig filterFunctionConfig = JSONUtil.fromJSON(configJson, FilterFunctionConfig.class);
                List<FilterFunctionItem> items = filterFunctionConfig != null
                        ? JSONUtil.toCollection(filterFunctionConfig.getV(), List.class, FilterFunctionItem.class)
                        : emptyItems;
                resultMap = !CollectionUtil.isEmpty(items) ? transToFilterFunctionItemMap(items) : resultMap;
            }
        }
        return resultMap;
    }

    /**
     * 保存服务端下发的功能点过滤配置。
     *
     * <p>空串表示"服务端要清空配置"，此时删本地文件；机型条件不匹配时
     * 也走删除分支（此时 {@code items} 保持 null，被 {@link CollectionUtil#isEmpty} 判成空）。</p>
     *
     * @param configJson 配置 JSON；可以为空
     */
    public static synchronized void saveFilterFunctions(String configJson) {
        LogUtil.i(TAG, "saveFilterFunctions -- 将要保存的配置string为 --> " + configJson);
        String bigDataDirPath = FileUtils.getBigDataDirPath();
        File dir = new File(bigDataDirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String filePath = bigDataDirPath + FILE_NAME;
        LogUtil.d(TAG, "保存的路径为 --> " + filePath);
        if (TextUtils.isEmpty(configJson) || TextUtils.isEmpty(configJson.trim())) {
            deleteLocalConfig(filePath);
            return;
        }
        List<FilterFunctionItem> items;
        FilterFunctionConfig filterFunctionConfig = JSONUtil.fromJSON(configJson, FilterFunctionConfig.class);
        if (filterFunctionConfig != null) {
            String modelCondition = filterFunctionConfig.getK();
            String innerModel = SystemPropertiesUtils.get(SystemConstants.PROP_PRODUCT_INNER_MODEL,
                    SystemConstants.Hardware.Id.IB);
            if (TextUtils.isEmpty(modelCondition) || !modelCondition.contains(innerModel)) {
                LogUtil.w(TAG, "本机 innerModel = " + innerModel + " , k值 不符 --> " + modelCondition);
                items = null;
            } else {
                items = JSONUtil.toCollection(filterFunctionConfig.getV(), List.class, FilterFunctionItem.class);
            }
        } else {
            items = new ArrayList<FilterFunctionItem>();
        }
        if (CollectionUtil.isEmpty(items)) {
            deleteLocalConfig(filePath);
        } else {
            FileUtils.saveToFile(configJson, filePath);
            ConfigAgent.getBehaviorConfig().collectFilterConfig.setFilterFunctionItemMap(transToFilterFunctionItemMap(items));
        }
    }

    /**
     * 把功能点列表转成"包名+功能名 -> true"的过滤表。
     *
     * @param items 功能点列表
     * @return 过滤表
     */
    private static Map<String, Boolean> transToFilterFunctionItemMap(List<FilterFunctionItem> items) {
        Map<String, Boolean> resultMap = new HashMap<String, Boolean>();
        for (int index = 0; index < items.size(); index++) {
            FilterFunctionItem filterFunctionItem = items.get(index);
            resultMap.put(filterFunctionItem.getPackageName() + ADD_MASK + filterFunctionItem.getFunctionName(), true);
        }
        return resultMap;
    }
}
