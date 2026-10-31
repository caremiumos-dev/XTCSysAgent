package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统信息事件（eventType = 10）。
 *
 * <p>业务侧把要上报的系统信息整包塞进 {@code extend}，
 * 这里只负责补网络类型。事件属性里只有 functionName。</p>
 */
public class SystemInfoEvent extends AEvent {

    /** 系统信息扩展。 */
    public Map<String, String> extend;

    /** 功能点名。 */
    public String functionName;

    @Override
    public int eventType() {
        return 10;
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
        return new EventAttr().setFunctionName(this.functionName);
    }

    @Override
    protected void packageExtendAttr() {
    }

    /**
     * 设置系统信息扩展。
     *
     * @param extend 扩展 map
     */
    public void setExtend(Map<String, String> extend) {
        this.extend = extend;
    }
}
