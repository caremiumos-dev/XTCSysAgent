package com.xtc.utils.system;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.xtc.bigdata.common.constants.Constants;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.NetworkInterface;

/**
 * 设备标识与硬件状态工具。
 *
 * <p>原厂类名为单字母 {@code f}（早期版本的 DeviceUtils）。这里改名 {@code DeviceInfoUtils}
 * 是因为本包里的 {@link DeviceUtils}（原 {@code t}）已经被"机型 / 地区"占用了 ——
 * 两个类内容完全不同，不能合并。</p>
 *
 * <p>本类做三件事：</p>
 * <ul>
 *   <li>拿设备标识：IMEI、MAC、{@code /proc/bbksn} 里的序列号、{@code Build.SERIAL}，
 *       并按原件顺序降级，最终兜底 {@code "1234567890"}；</li>
 *   <li>拿硬件状态：CPU 核数、可用/总内存、电量、是否充电、是否手表（有电话功能）；</li>
 *   <li>两个小动作：震动、静默关流。</li>
 * </ul>
 */
public class DeviceInfoUtils {

    /** 所有标识都拿不到时的兜底设备号，原件就是这个串，不能改。 */
    public static final String DEFAULT_DEVICE_ID = "1234567890";

    private static final String TAG = "DeviceUtils";

    /** WiFi 未连接 / 被 ROM 打码时返回的假 MAC。 */
    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    private DeviceInfoUtils() {
        // 原厂异常文案，原样保留。
        throw new UnsupportedOperationException("Are u ok ?");
    }

    /** 系统 API 级别。 */
    public static int getSDKVersion() {
        return Build.VERSION.SDK_INT;
    }

    /** IMEI。取不到时由系统返回 null / 空串，这里不做兜底。 */
    public static String getDeviceId(Context context) {
        return ((TelephonyManager) context.getApplicationContext().getSystemService(Constants.PHONE)).getDeviceId();
    }

    /** 按给定毫秒数震动一次（只震一次：repeat = -1）。 */
    public static void vibrate(Context context, long milliseconds) {
        ((Vibrator) context.getApplicationContext().getSystemService("vibrator"))
                .vibrate(new long[]{0, milliseconds}, -1);
    }

    /** 静默关闭流；{@code null} 直接忽略，异常吞掉。 */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            // 故意留空：原件同样是空 catch。
        }
    }

    /** 厂商名。 */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * MAC 地址。
     *
     * <p>优先取当前 WiFi 连接信息；拿到系统打码值 {@code 02:00:00:00:00:00}
     * 时改从 {@code wlan0} 网卡读硬件地址；最后仍为空则返回打码值。</p>
     */
    public static String getMacAddress(Context context) {
        String macAddress;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService("wifi");
        if (wifiManager == null) {
            macAddress = "";
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                macAddress = "";
            } else {
                macAddress = wifiInfo.getMacAddress();
                if (DEFAULT_MAC_ADDRESS.equalsIgnoreCase(macAddress)) {
                    macAddress = getMacFromWlan0();
                }
            }
        }
        return TextUtils.isEmpty(macAddress) ? DEFAULT_MAC_ADDRESS : macAddress;
    }

    /** 机型：去首尾空白并删掉所有空白字符。{@code Build.MODEL} 为 null 时原样返回 null。 */
    public static String getModel() {
        String model = Build.MODEL;
        return model != null ? model.trim().replaceAll("\\s*", "") : model;
    }

    /** 是否具备电话功能（{@code getPhoneType() != NONE}）。 */
    public static boolean isPhone(Context context) {
        return ((TelephonyManager) context.getApplicationContext().getSystemService(Constants.PHONE)).getPhoneType() != 0;
    }

    /** CPU 核数。 */
    public static int getCPUCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    /** 当前可用内存（字节）。 */
    public static long getAvailableMemory(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getApplicationContext().getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }

    /**
     * 总内存（字节）。
     *
     * <p>API 16 起用 {@code MemoryInfo.totalMem}；更低版本退化成读 {@code /proc/meminfo}
     * 第一行、只留数字（即 kB 数）。读失败返回 0。</p>
     */
    public static long getTotalMemory(Context context) {
        long totalMemory = 0;
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(memoryInfo);
            return memoryInfo.totalMem;
        }
        try {
            RandomAccessFile memInfoFile = new RandomAccessFile("/proc/meminfo", "r");
            totalMemory = Integer.parseInt(memInfoFile.readLine().replaceAll("\\D+", ""));
            memInfoFile.close();
            return totalMemory;
        } catch (IOException e) {
            return totalMemory;
        }
    }

    /**
     * 读 {@code /proc/bbksn} 的序列号。
     *
     * <p>逐行读、只保留最后一行；文件不存在或读取失败时返回已读到的内容（通常是 null），
     * 异常不向上抛 —— 原件的 try/catch 结构就是"吞掉异常继续返回"。</p>
     */
    public static String getBbksnSerial() {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        String serial = null;
        try {
            fileReader = new FileReader("/proc/bbksn");
            bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                serial = line;
            }
        } catch (Exception e) {
            // 故意留空：读不到就返回 null。
        } finally {
            // 关闭顺序与原件一致：先 FileReader 后 BufferedReader。
            closeQuietly(fileReader);
            closeQuietly(bufferedReader);
        }
        return serial;
    }

    /** 电量百分比（0..100）；取不到时返回 -1。 */
    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        assert batteryIntent != null;
        return batteryIntent.getIntExtra("level", -1);
    }

    /** 增量版本号（{@code Build.VERSION.INCREMENTAL}）。 */
    public static String getIncremental() {
        return Build.VERSION.INCREMENTAL;
    }

    /** 直接读 {@code wlan0} 网卡的硬件地址，格式化成 {@code XX:XX:...}；异常返回 null。 */
    private static String getMacFromWlan0() {
        try {
            byte[] hardwareAddress = NetworkInterface.getByName("wlan0").getHardwareAddress();
            StringBuilder builder = new StringBuilder();
            for (byte addressByte : hardwareAddress) {
                builder.append(String.format("%02X:", addressByte));
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 是否正在充电（电池状态 == 2，即 {@code BATTERY_STATUS_CHARGING}）。 */
    public static boolean isCharging(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        assert batteryIntent != null;
        return batteryIntent.getIntExtra("status", 1) == 2;
    }

    /**
     * 取"唯一设备号"，按原件顺序降级：
     * <ol>
     *   <li>{@code /proc/bbksn} 里的序列号；</li>
     *   <li>IMEI；</li>
     *   <li>MAC 地址；</li>
     *   <li>MAC 仍是打码值时用 {@code Build.SERIAL}；</li>
     *   <li>仍然为空则返回 {@link #DEFAULT_DEVICE_ID}。</li>
     * </ol>
     */
    public static String getUniqueDeviceId(Context context) {
        String deviceId = getBbksnSerial();
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = getDeviceId(context);
        }
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = getMacAddress(context);
        }
        if (TextUtils.equals(deviceId, DEFAULT_MAC_ADDRESS)) {
            deviceId = Build.SERIAL;
        }
        return TextUtils.isEmpty(deviceId) ? DEFAULT_DEVICE_ID : deviceId;
    }
}
