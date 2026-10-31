package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 计数事件（eventType = 8）。
 *
 * <p>与点击事件的差别只有一个 {@code trigValue} —— 计数事件要带上计数值。</p>
 */
public class CountEvent extends AEvent {

    /** 序列化版本号（原厂就有，并不实现 Serializable）。 */
    public static final long serialVersionUID = 1;

    /** 触发页面。 */
    public String activity;

    /** 业务扩展。 */
    public Map<String, String> extend;

    /** 功能点名。 */
    public String functionName;

    /** 模块细节。 */
    public String moduleDetail;

    /** 计数值。 */
    public String trigValue;

    @Override
    public int eventType() {
        return 8;
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
        return new EventAttr().setFunctionName(this.functionName).setPage(this.activity)
                .setModuleDetail(this.moduleDetail).setTrigValue(this.trigValue);
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
