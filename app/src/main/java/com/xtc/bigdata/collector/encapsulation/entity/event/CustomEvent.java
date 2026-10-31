package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义事件（eventType = 7）。
 *
 * <p>{@code eventName} 与 {@code eventType} 两个字段标了 {@code @Deprecated}：
 * 老版本业务方用它们传递事件名与子类型，新版本统一走 functionName。
 * 字段本身不能删 —— 业务代码还在往里写。</p>
 */
public class CustomEvent extends AEvent {

    /** 序列化版本号（原厂就有，并不实现 Serializable）。 */
    public static final long serialVersionUID = 1;

    /** 触发页面。 */
    public String activity;

    /** 老版事件名。 */
    @Deprecated
    public String eventName;

    /** 老版事件子类型。 */
    @Deprecated
    public int eventType;

    /** 业务扩展。 */
    public Map<String, String> extend;

    /** 功能点名。 */
    public String functionName;

    /** 模块细节。 */
    public String moduleDetail;

    /** 触发值。 */
    public String trigValue;

    @Override
    public int eventType() {
        return 7;
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
