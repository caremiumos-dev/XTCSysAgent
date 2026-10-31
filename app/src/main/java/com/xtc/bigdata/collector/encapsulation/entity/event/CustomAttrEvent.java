package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;

/**
 * "用现成 EventAttr 直接拼出来"的事件。
 *
 * <p>{@code CollectionManager.createCustomAttrEvent} 用它统一构造
 * 那些没有专属事件类的类型；事件类型取自 {@link EventAttr#getEventType()}，
 * 所以属性为 null 时类型退化成 0。</p>
 */
public class CustomAttrEvent extends AEvent {

    /** 外部传入的事件属性。 */
    public EventAttr eventAttr;

    @Override
    public int eventType() {
        if (this.eventAttr == null) {
            return 0;
        }
        return this.eventAttr.getEventType();
    }

    @Override
    protected String getJsonExtend() {
        HashMap<String, String> extendMap = new HashMap<String, String>();
        extendMap.put("NetWorkType", NetworkUtil.getInstance().getNetworkType());
        return hashMap2Json(extendMap);
    }

    @Override
    protected EventAttr packageEventAttr() {
        return this.eventAttr;
    }

    @Override
    protected void packageExtendAttr() {
    }
}
