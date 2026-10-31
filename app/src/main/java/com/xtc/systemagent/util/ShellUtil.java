package com.xtc.systemagent.util;

import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;
import com.xtc.common.bigdata.BehaviorUtil;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * 命令执行 + 电量读取 + "重启/崩溃"埋点。
 *
 * <p>注意 {@code ELECTRICITY_CMD_STRING} 里写的是 {@code ./sys/...}（相对路径），
 * 这是原厂原样，靠 shell 的工作目录恰好是 {@code /} 才能work，不要"修正"成绝对路径。</p>
 */
public class ShellUtil {

    private static final String TAG = "Log_ShellUtil";

    /** 读电量百分比。原厂写的是相对路径。 */
    private static final String ELECTRICITY_CMD_STRING =
            "cat ./sys/class/power_supply/battery/capacity";

    /** 埋点 Type：正常重启。 */
    private static final String PROPERTY_NORMAL_REBOOT_STRING = "3";
    /** 埋点 Type：关机/重启。 */
    private static final String PROPERTY_SHUTDOW_REBOOT_BIGDATA_STRING = "1";
    /** 埋点 Type：应用崩溃。 */
    private static final String PROPERTY_APP_CRASH_STRING = "2";

    /** 读不到电量时的兜底值。 */
    private static final int DEFAULT_BATTERY = 60;

    private ShellUtil() {
    }

    /**
     * 执行命令并合并 stdout / stderr。
     *
     * <p>读取顺序是"先读一行 stdout，stdout 读完了再读 stderr"，
     * 与原厂一致；任何异常都只打堆栈、返回已累积的内容。</p>
     */
    public static String StringexecShellStr(String command) {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()), 7777);
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()), 7777);
            while (true) {
                String line = stdout.readLine();
                if (line == null && (line = stderr.readLine()) == null) {
                    break;
                }
                if (!isStringEmpty(line)) {
                    result = result + line + "\n";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** 直接 exec 一条命令串，按行拼结果。 */
    public static String exec(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder("");
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            builder.append(line);
            builder.append('\n');
        }
        try {
            if (process.waitFor() != 0) {
                System.err.println("exit value = " + process.exitValue());
            }
        } catch (InterruptedException e) {
            // 被中断时保持与原厂一致：仅打印，不改变已读到的输出。
            e.printStackTrace();
        }
        return builder.toString();
    }

    /** 读电量百分比字符串（已 trim）。失败返回空串。 */
    public static String getCurrentBatteryString() {
        String result = "";
        try {
            result = exec(ELECTRICITY_CMD_STRING).trim();
        } catch (IOException e) {
            LogUtil.w(TAG, "getCurrentBatteryString error = " + e);
            e.printStackTrace();
        }
        LogUtil.d(TAG, "result = " + result);
        return result;
    }

    /** 读电量，读不到返回 60。 */
    public static int getCurrentBattery() {
        String batteryString = getCurrentBatteryString();
        if (!TextUtils.isEmpty(batteryString)) {
            return Integer.valueOf(batteryString).intValue();
        }
        LogUtil.d(TAG, "batteryString 为空串，返回 60");
        return DEFAULT_BATTERY;
    }

    /** 上一次是不是"关机/重启"（1 表示是）。 */
    public static int getShutDownAndRebootProperties() {
        return SystemPropertiesUtils.getInt(SystemConstants.PROP_OCCUR_DUMP, 0);
    }

    /** 上一次是不是"应用崩溃"（1 表示是）。 */
    public static int getAppCrashProperties() {
        return SystemPropertiesUtils.getInt(SystemConstants.PROP_APP_CRASH, 0);
    }

    public static boolean isStringEmpty(String value) {
        return value == null || "".equals(value);
    }

    /** 重启到 recovery 之外的指定 reason。 */
    public static void reboot(Context context) {
        ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).reboot("hgc");
    }

    /**
     * 上报"这次开机是正常重启 / 关机重启 / 应用崩溃"。
     *
     * <p>关机重启与应用崩溃的标记读一次就清掉，避免重复上报。</p>
     */
    public static void systemShutdownOrAppCrashBehavior(Context context) {
        HashMap<String, String> map = new HashMap<>();
        int shutDownAndReboot = getShutDownAndRebootProperties();
        int appCrash = getAppCrashProperties();
        LogUtil.d(TAG, "systemShutdownOrAppCrashBehavior shutdownAndReboot = "
                + shutDownAndReboot + ",appCrash = " + appCrash);
        if (shutDownAndReboot == 1) {
            SystemPropertiesUtils.set(SystemConstants.PROP_OCCUR_DUMP, 0);
            map.put("Type", PROPERTY_SHUTDOW_REBOOT_BIGDATA_STRING);
        } else if (appCrash == 1) {
            SystemPropertiesUtils.set(SystemConstants.PROP_APP_CRASH, 0);
            map.put("Type", PROPERTY_APP_CRASH_STRING);
        } else {
            map.put("Type", PROPERTY_NORMAL_REBOOT_STRING);
        }
        BehaviorUtil.customEvent(context, "System_RestartMonitor", map);
    }
}
