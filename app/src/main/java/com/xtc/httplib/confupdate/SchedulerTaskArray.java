package com.xtc.httplib.confupdate;

import android.util.ArrayMap;
import com.xtc.httplib.bean.BaseSchedulerTask;
import java.util.Map;
import java.util.Set;

/**
 * 调度任务表的薄包装。
 *
 * <p>本类只暴露 {@link #entrySet()}，即"只能遍历、不能增删"。
 * 真正的增删由 {@link ConfUpdatesInfoManager} 自己用内部的 ArrayMap 完成。</p>
 *
 * <p>用 {@link ArrayMap} 而不是 HashMap：任务表元素极少（通常 3~5 个），
 * ArrayMap 以两个小数组存储，比 HashMap 的 Node 更省内存——这是系统应用里的常见取舍。</p>
 */
public class SchedulerTaskArray {

    /** 配置项编号 -> 调度任务。 */
    private ArrayMap<Integer, BaseSchedulerTask> confBeanArrayMap = new ArrayMap<Integer, BaseSchedulerTask>();

    public Set<Map.Entry<Integer, BaseSchedulerTask>> entrySet() {
        return this.confBeanArrayMap.entrySet();
    }
}
