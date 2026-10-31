package com.xtc.bigdata.collector.encapsulation.entity.event;

import java.util.HashMap;

/**
 * DropBox 里的崩溃事件（eventType = 11）。
 *
 * <p>与 ANR 事件共用一个类型号，服务端靠 functionName
 * （{@code "DropBox_Crash"} / {@code "DropBox_ANR"}）区分。</p>
 */
public class DropBoxCrashEvent extends ExceptionEvent {

    /** 进程名。 */
    public String processName;

    @Override
    protected String data2Json() {
        HashMap<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("processName", this.processName);
        dataMap.put("reason", this.reason);
        dataMap.put("stack", this.stack);
        return hashMap2Json(dataMap);
    }

    @Override
    public int eventType() {
        return 11;
    }
}
