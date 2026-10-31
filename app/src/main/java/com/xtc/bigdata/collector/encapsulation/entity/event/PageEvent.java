package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 页面切出事件（eventType = 2）。
 *
 * <p>{@code duaring}（原厂拼写错误，少一个 r）存的是停留时长毫秒数，
 * 写进 {@code trigValue} 列。字段名与列值都不能改。</p>
 */
public class PageEvent extends AEvent {

    /** 页面名（Activity 全名）。 */
    public String activityName;

    /** 停留时长（毫秒字符串，原厂字段名就拼成 duaring）。 */
    public String duaring;

    /** 业务扩展。 */
    public Map<String, String> extend;

    /** 功能点名。 */
    public String functionName;

    /** 模块细节。 */
    public String moduleDetail;

    @Override
    public int eventType() {
        return 2;
    }

    @Override
    protected String getJsonExtend() {
        if (this.extend == null) {
            this.extend = new HashMap<String, String>();
        }
        this.extend.put("NetWorkType", NetworkUtil.getInstance().getNetworkType());
        return hashMap2Json(this.extend);
    }

    @Override
    protected EventAttr packageEventAttr() {
        return new EventAttr().setFunctionName(this.functionName).setPage(this.activityName)
                .setTrigValue(this.duaring).setModuleDetail(this.moduleDetail);
    }

    @Override
    protected void packageExtendAttr() {
    }

    /**
     * 设置业务扩展。
     *
     * @param extend 扩展 map
     */
    public void setExtend(Map<String, String> extend) {
        this.extend = extend;
    }
}
