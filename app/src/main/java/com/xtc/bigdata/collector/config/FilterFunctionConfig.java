package com.xtc.bigdata.collector.config;

import com.google.gson.annotations.SerializedName;

/**
 * 服务端下发的功能点过滤配置。
 *
 * <p>这是一个 <b>JSON 载体</b>：字段名就是服务端下发的键名
 * （原厂就是单字母 {@code k} / {@code v}）。为了在 Java 侧给出可读名字，
 * 这里改用 {@link SerializedName} 固定线上键名，字段本身换成语义名 ——
 * 序列化结果与原来逐字节一致。</p>
 */
public class FilterFunctionConfig {

    /** 配置说明。 */
    private String description;

    /** 是否可见。 */
    private String isVisible;

    /**
     * 配置生效的机型条件串。
     *
     * <p>线上键名是 {@code k}，值是"多个内部型号用分隔符拼起来"的串，
     * 采集库用 {@code contains(本机 innerModel)} 判断本机是否命中。</p>
     */
    @SerializedName("k")
    private String modelCondition;

    /**
     * 配置内容。
     *
     * <p>线上键名是 {@code v}，值本身是一段 JSON 数组字符串，
     * 会被二次解析成 {@code List<FilterFunctionItem>}。</p>
     */
    @SerializedName("v")
    private String filterItemsJson;

    /** @return 配置说明 */
    public String getDescription() {
        return this.description;
    }

    /** @return 是否可见 */
    public String getIsVisible() {
        return this.isVisible;
    }

    /** @return 机型条件串（线上键名 k） */
    public String getK() {
        return this.modelCondition;
    }

    /** @return 功能点 JSON 数组串（线上键名 v） */
    public String getV() {
        return this.filterItemsJson;
    }

    /**
     * 设置配置说明。
     *
     * @param description 说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 设置是否可见。
     *
     * @param isVisible 是否可见
     */
    public void setIsVisible(String isVisible) {
        this.isVisible = isVisible;
    }

    /**
     * 设置机型条件串。
     *
     * @param modelCondition 机型条件串
     */
    public void setK(String modelCondition) {
        this.modelCondition = modelCondition;
    }

    /**
     * 设置功能点 JSON 数组串。
     *
     * @param filterItemsJson 功能点 JSON
     */
    public void setV(String filterItemsJson) {
        this.filterItemsJson = filterItemsJson;
    }

    @Override
    public String toString() {
        return "FilterFunctionConfig{k='" + this.modelCondition + "', isVisible='" + this.isVisible
                + "', description='" + this.description + "', v='" + this.filterItemsJson + "'}";
    }
}
