package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 点击事件（eventType = 5）。
 *
 * <p>业务侧把 {@code extend} 直接塞进来，这里只负责补一个 {@code NetWorkType}
 * —— 服务端要求每条事件的 extend 都带网络类型。</p>
 */
public class ClickEvent extends AEvent {

    /** 序列化版本号（原厂就有，并不实现 Serializable）。 */
    public static final long serialVersionUID = 1;

    /** 触发页面（Activity 全名）。 */
    public String activity;

    /** 业务扩展。 */
    public Map<String, String> extend;

    /** 功能点名。 */
    public String functionName;

    /** 模块细节。 */
    public String moduleDetail;

    @Override
    public int eventType() {
        return 5;
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
        return new EventAttr().setFunctionName(this.functionName).setPage(this.activity).setModuleDetail(this.moduleDetail);
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
