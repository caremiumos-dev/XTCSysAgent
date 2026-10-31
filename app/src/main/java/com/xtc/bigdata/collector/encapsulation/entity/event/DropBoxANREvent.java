package com.xtc.bigdata.collector.encapsulation.entity.event;

import java.util.HashMap;

/**
 * DropBox 里的 ANR 事件（eventType = 11）。
 *
 * <p>字段一一对应 DropBox 条目里的 {@code Process/Flags/Package/Activity/...}
 * 十行文本，读取顺序由 {@code DropboxScanner} 的 keyList 决定。</p>
 */
public class DropBoxANREvent extends ExceptionEvent {

    /** 进程名。 */
    public String processName;

    /** DropBox 条目的 flags。 */
    public String flags;

    /** 包信息（"包名 (版本名)"）。 */
    public String packageInfo;

    /** 崩溃时所在的 Activity。 */
    public String activity;

    /** 父进程名。 */
    public String parentProcess;

    /** 父 Activity。 */
    public String parentActivity;

    /** 是否前台。 */
    public String foreground;

    /** 主题（DropBox 的 subject 行）。 */
    public String subject;

    /** 构建指纹。 */
    public String build;

    @Override
    protected String data2Json() {
        HashMap<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("processName", this.processName);
        dataMap.put("flags", this.flags);
        dataMap.put("packageInfo", this.packageInfo);
        dataMap.put("activity", this.activity);
        dataMap.put("parentProcess", this.parentProcess);
        dataMap.put("parentActivity", this.parentActivity);
        dataMap.put("foreground", this.foreground);
        dataMap.put("subject", this.subject);
        dataMap.put("build", this.build);
        return hashMap2Json(dataMap);
    }

    @Override
    public int eventType() {
        return 11;
    }

    @Override
    public String toString() {
        return "DropBoxANREvent{processName='" + this.processName + "', flags='" + this.flags
                + "', packageInfo='" + this.packageInfo + "', activity='" + this.activity
                + "', parentProcess='" + this.parentProcess + "', parentActivity='" + this.parentActivity
                + "', foreground='" + this.foreground + "', subject='" + this.subject
                + "', build='" + this.build + "'}";
    }
}
