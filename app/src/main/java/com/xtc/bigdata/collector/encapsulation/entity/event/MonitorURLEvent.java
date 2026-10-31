package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;

/**
 * URL 监控事件（eventType = 14）。
 *
 * <p>注意它的 {@code functionName} 是 private 且从未被赋值 ——
 * 原厂就是只写 {@code moduleDetail} 与 {@code trigValue}，
 * 上报时 functionName 为空串。<b>不要"顺手"补一个 functionName</b>，
 * 服务端按空值走 URL 监控的专用解析分支。</p>
 */
public class MonitorURLEvent extends AEvent {

    /** 序列化版本号（原厂就有，并不实现 Serializable）。 */
    public static final long serialVersionUID = 1;

    /** 功能点名（原厂从未赋值）。 */
    private String functionName;

    /** 模块细节（一般是 URL）。 */
    public String moduleDetail;

    /** 触发值（一般是耗时或状态码）。 */
    public String trigValue;

    @Override
    public int eventType() {
        return 14;
    }

    @Override
    protected String getJsonExtend() {
        HashMap<String, String> extendMap = new HashMap<String, String>();
        extendMap.put("NetWorkType", NetworkUtil.getInstance().getNetworkType());
        return hashMap2Json(extendMap);
    }

    @Override
    protected EventAttr packageEventAttr() {
        return new EventAttr().setEventType(14).setModuleDetail(this.moduleDetail).setTrigValue(this.trigValue);
    }

    @Override
    protected void packageExtendAttr() {
    }
}
