package com.xtc.bigdata.collector.config;

/**
 * 一个"要采集的功能点"。
 *
 * <p>由服务端下发、Gson 反序列化得到，字段名就是 JSON 键，不能改。</p>
 */
public class FilterFunctionItem {

    /** 功能点名。 */
    private String functionName;

    /** 功能点所属包名。 */
    private String packageName;

    public FilterFunctionItem() {
    }

    /**
     * 只指定功能名。
     *
     * @param functionName 功能点名
     */
    public FilterFunctionItem(String functionName) {
        this.functionName = functionName;
    }

    /**
     * 指定功能名与包名。
     *
     * @param functionName 功能点名
     * @param packageName  包名
     */
    public FilterFunctionItem(String functionName, String packageName) {
        this.functionName = functionName;
        this.packageName = packageName;
    }

    /** @return 功能点名 */
    public String getFunctionName() {
        return this.functionName;
    }

    /** @return 包名 */
    public String getPackageName() {
        return this.packageName;
    }

    /**
     * 设置功能点名。
     *
     * @param functionName 功能点名
     */
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    /**
     * 设置包名。
     *
     * @param packageName 包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "FilterFunctionItem{functionName='" + this.functionName + "', packageName='" + this.packageName + "'}";
    }
}
