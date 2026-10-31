package com.xtc.bigdata.collector.encapsulation.entity.event;

import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.utils.NetworkUtil;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.SystemInfoUtils;

import java.util.HashMap;

/**
 * App 启动事件（eventType = 3）。
 *
 * <p>它的 extend 是整条链路里最重的一次采集：网络类型、CPU、内存、ROM、
 * 分辨率、屏幕尺寸 —— 六项一次性带上，服务端用来做机型画像。</p>
 */
public class AppLaunchEvent extends AEvent {

    /** 触发时的页面名。 */
    private String activityName;

    /** 组装出的 extend JSON，仅用于排查。 */
    private String extend;

    /** 功能点名。 */
    public String functionName = "BigDataSDK_AppLaunchEvent";

    /** 模块细节。 */
    public String moduleDetail = "BigDataSDK_AppLaunch";

    /** 触发值。 */
    private String trigValue;

    @Override
    public int eventType() {
        return 3;
    }

    @Override
    protected String getJsonExtend() {
        HashMap<String, String> extendMap = new HashMap<String, String>();
        extendMap.put("NetWorkType", NetworkUtil.getInstance().getNetworkType());
        extendMap.put("cpuInfo", SystemInfoUtils.getCpuName());
        extendMap.put("memoryInfo", SystemInfoUtils.getTotalRam(ContextUtils.getContext()));
        extendMap.put("deviceCapacity", SystemInfoUtils.getRomTotalSize(ContextUtils.getContext()));
        extendMap.put("screenResolution", SystemInfoUtils.getScreenResolutionRatio(ContextUtils.getContext()));
        extendMap.put("screenSize", String.valueOf(SystemInfoUtils.getScreenInch(ContextUtils.getContext())));
        String json = hashMap2Json(extendMap);
        this.extend = json;
        return json;
    }

    @Override
    protected EventAttr packageEventAttr() {
        return new EventAttr().setFunctionName(this.functionName).setPage(this.activityName).setTrigValue(this.trigValue);
    }

    @Override
    protected void packageExtendAttr() {
    }
}
