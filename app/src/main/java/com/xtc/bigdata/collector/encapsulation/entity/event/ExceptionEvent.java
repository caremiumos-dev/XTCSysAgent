package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;

import java.util.HashMap;

/**
 * 异常事件（eventType = 9）。
 *
 * <p>异常内容不进 extend，而是序列化成 JSON 放进 {@code trigValue}；
 * extend 只留网络类型与本地日志文件路径 —— 服务端拿到 trigValue
 * 再二次解析出 reason/stack/各项容量。</p>
 */
public class ExceptionEvent extends AEvent {

    /** 序列化版本号（原厂就有，并不实现 Serializable）。 */
    private static final long serialVersionUID = 1;

    /** 功能点名（例如 "应用异常"）。 */
    public String functionName;

    /** 异常原因（一般取 {@code throwable.toString()}）。 */
    public String reason;

    /** 堆栈文本。 */
    public String stack;

    /** 系统日志。 */
    public String sysLog;

    /** 内部存储总量。 */
    public String diskTotal;

    /** 内部存储使用率。 */
    public String diskUsage;

    /** 外部存储总量。 */
    public String sdTotal;

    /** 外部存储使用率。 */
    public String sdUsage;

    /** 内存总量。 */
    public String memTotal;

    /** 内存使用率。 */
    public String memUsage;

    /** 本地日志文件路径。 */
    public String filePath;

    /**
     * 把异常字段拼成 JSON（子类会覆盖它追加自己的字段）。
     *
     * @return JSON 串
     */
    protected String data2Json() {
        HashMap<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("reason", this.reason);
        dataMap.put("stack", this.stack);
        dataMap.put("sysLog", this.sysLog);
        dataMap.put("diskTotal", this.diskTotal);
        dataMap.put("diskUsage", this.diskUsage);
        dataMap.put("sdTotal", this.sdTotal);
        dataMap.put("sdUsage", this.sdUsage);
        dataMap.put("memTotal", this.memTotal);
        dataMap.put("memUsage", this.memUsage);
        return hashMap2Json(dataMap);
    }

    @Override
    public int eventType() {
        return 9;
    }

    @Override
    protected String getJsonExtend() {
        HashMap<String, String> extendMap = new HashMap<String, String>();
        extendMap.put("NetWorkType", NetworkUtil.getInstance().getNetworkType());
        extendMap.put("FilePath", this.filePath);
        return hashMap2Json(extendMap);
    }

    @Override
    protected EventAttr packageEventAttr() {
        return new EventAttr().setFunctionName(this.functionName).setTrigValue(data2Json());
    }

    @Override
    protected void packageExtendAttr() {
    }
}
