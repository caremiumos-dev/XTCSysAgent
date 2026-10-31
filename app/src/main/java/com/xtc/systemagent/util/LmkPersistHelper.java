package com.xtc.systemagent.util;

import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.common.CollectionUtils;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * LMK（Low Memory Killer）保活名单的读写。
 *
 * <p>系统把"不杀名单"存在 {@code persist.sys.special.process} 里，格式是
 * 逗号分隔的进程名。本类负责在名单里加入/移除自己，让采集线程在
 * 内存紧张时不被回收。</p>
 */
public class LmkPersistHelper {

    private static final String TAG = "LmkPersistHelper";

    /** 系统的不杀进程名单属性。 */
    private static final String PROP_LMK_PERSIST_PROCESS = "persist.sys.special.process";

    private LmkPersistHelper() {
    }

    /** 读出当前名单；属性为空或解析不出内容时返回空列表。 */
    private static List<String> getLmkPersist() {
        List<String> processes = new ArrayList<>();
        String value = SystemPropertiesUtils.get(PROP_LMK_PERSIST_PROCESS, "");
        if (!TextUtils.isEmpty(value)) {
            String[] parts = value.split(",");
            if (parts.length != 0) {
                processes.addAll(Arrays.asList(parts));
                LogUtil.d(TAG, "getLmkPersist: " + processes);
            }
        }
        return processes;
    }

    /** 把自己加入不杀名单。已经在里面就直接返回。 */
    public static void registerLmkPersist(String processName) {
        List<String> processes = getLmkPersist();
        if (processes.contains(processName)) {
            return;
        }
        processes.add(processName);
        setLmkPersist(processes);
    }

    /** 把自己移出不杀名单。 */
    public static void unregisterLmkPersist(String processName) {
        List<String> processes = getLmkPersist();
        if (processes.contains(processName)) {
            List<String> remaining = new ArrayList<>(processes);
            remaining.remove(processName);
            setLmkPersist(remaining);
        }
    }

    private static void setLmkPersist(String value) {
        if (TextUtils.isEmpty(value)) {
            SystemPropertiesUtils.set(PROP_LMK_PERSIST_PROCESS, "");
        } else {
            SystemPropertiesUtils.set(PROP_LMK_PERSIST_PROCESS, value);
        }
    }

    /**
     * 把列表拼成逗号串写回属性。
     *
     * <p>空列表写空串；只有一个元素时直接写那个元素（不补逗号）。</p>
     */
    private static void setLmkPersist(List<String> processes) {
        if (CollectionUtils.isEmpty(processes)) {
            SystemPropertiesUtils.set(PROP_LMK_PERSIST_PROCESS, "");
            return;
        }
        if (processes.size() == 1) {
            SystemPropertiesUtils.set(PROP_LMK_PERSIST_PROCESS, processes.get(0));
            return;
        }
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = processes.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next());
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        String value = builder.toString();
        LogUtil.d(TAG, "setLmkPersist allProcess: " + value);
        setLmkPersist(value);
    }
}
