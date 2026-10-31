package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索事件（eventType = 6）。
 *
 * <p>{@code keyWrod} 是原厂拼写错误（字母顺序反了），但业务方与
 * 上报协议都按这个名字取值，改了收不到关键词。</p>
 */
public class SearchEvent extends AEvent {

    /** 序列化版本号（原厂就有，并不实现 Serializable）。 */
    public static final long serialVersionUID = 1;

    /** 触发页面。 */
    public String activity;

    /** 功能点名。 */
    public String functionName;

    /** 搜索关键词（原厂拼写错误）。 */
    public String keyWrod;

    /** 模块细节。 */
    public String moduleDetail;

    /** 结果条数。 */
    public String resultCount;

    /**
     * 组装搜索专属的 extend（多一个 resultCount）。
     *
     * @param resultCount 结果条数
     * @return 扩展 map
     */
    private Map<String, String> getExtend(String resultCount) {
        HashMap<String, String> extendMap = new HashMap<String, String>();
        extendMap.put("NetWorkType", NetworkUtil.getInstance().getNetworkType());
        extendMap.put("resultCount", resultCount);
        return extendMap;
    }

    @Override
    public int eventType() {
        return 6;
    }

    @Override
    protected String getJsonExtend() {
        return hashMap2Json(getExtend(this.resultCount));
    }

    @Override
    protected EventAttr packageEventAttr() {
        return new EventAttr().setFunctionName(this.functionName).setPage(this.activity)
                .setModuleDetail(this.moduleDetail).setTrigValue(this.keyWrod);
    }

    @Override
    protected void packageExtendAttr() {
    }
}
