package com.xtc.bigdata.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.xtc.bigdata.common.constants.Constants;
import com.xtc.log.LogUtil;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.NetworkInterface;

/**
 * 设备信息读取（机器 id、mac、imei、内存、电量、充电状态）。
 *
 * <p>机器 id 的取值是一条降级链：bbk 序列号 → imei → mac → {@link Build#SERIAL}，
 * 全拿不到才给兜底值 {@code "1234567890"}。这条链的顺序直接决定了
 * 服务端能不能把同一台设备认成一台，不能调整。</p>
 *
 * <p>注意本类与 {@code com.xtc.utils.system.DeviceUtils} 是两个类，
 * 那个是公共库版本，这边是采集库自带的一份。</p>
 */
public class DeviceUtils {

    /** wifi 未初始化时的默认 mac。 */
    private static final String DEFAULT_MAC = "02:00:00:00:00:00";

    /** 兜底机器 id。 */
    public static final String DEFAULT_MACHINE_ID = "1234567890";

    /** 内部型号属性名。 */
    private static final String PROPERTY_PRODUCT_INNER_MODEL = "ro.product.innermodel";

    private static final String TAG = "DeviceUtils";

    private DeviceUtils() {
        throw new UnsupportedOperationException("Are u ok ?");
    }

    /**
     * 关闭流，失败只在调试模式下提示。
     *
     * @param closeable 待关闭对象
     */
    public static void closeIO(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            if (Constants.isDebug) {
                LogUtil.w(TAG, e + ":关闭io失败, 这不科学 - -");
            }
        }
    }

    /**
     * 读当前电量百分比。
     *
     * <p>用 {@code registerReceiver(null, ...)} 的"粘性广播"技巧同步拿到电量，
     * 不注册真正的接收者。取不到时原厂会踩断言，这里保留 assert 语义。</p>
     *
     * @param context 上下文
     * @return 电量百分比；取不到返回 -1
     */
    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        assert batteryIntent != null;
        return batteryIntent.getIntExtra("level", -1);
    }

    /**
     * 读 {@code /proc/bbksn} 的最后一行（bbk 序列号）。
     *
     * @return 序列号；读失败返回已读到的上一行（可能为 null）
     */
    public static String getBbkSn() {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        String line = null;
        String previousLine = null;
        try {
            fileReader = new FileReader("/proc/bbksn");
            bufferedReader = new BufferedReader(fileReader);
            do {
                previousLine = line;
                line = bufferedReader.readLine();
            } while (line != null);
            return previousLine;
        } catch (Exception e) {
            if (Constants.isDebug) {
                LogUtil.w(TAG, e + ":获取bbksn失败");
            }
            return previousLine;
        } finally {
            closeIO(fileReader);
            closeIO(bufferedReader);
        }
    }

    /**
     * CPU 核心数。
     *
     * @return 核心数
     */
    public static int getCpuCoreSize() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 取 IMEI。
     *
     * @param context 上下文
     * @return IMEI；取不到返回空串
     */
    public static String getIMEI(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Constants.PHONE);
            if (telephonyManager != null) {
                return telephonyManager.getDeviceId();
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "getIMEI error " + e.toString());
        }
        return "";
    }

    /**
     * 用 JDK 的 {@link NetworkInterface} 取 wlan0 的 mac。
     *
     * @return mac 串；失败返回 null
     */
    private static String getMac() {
        try {
            byte[] hardwareAddress = NetworkInterface.getByName("wlan0").getHardwareAddress();
            StringBuilder builder = new StringBuilder();
            for (byte addressPart : hardwareAddress) {
                builder.append(String.format("%02X:", Byte.valueOf(addressPart)));
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        } catch (Exception e) {
            if (Constants.isDebug) {
                LogUtil.w(TAG, e + ":使用jdk方法获取mac地址失败");
            }
            return null;
        }
    }

    /**
     * 取 mac 地址。
     *
     * <p>wifi 返回的 {@code 02:00:00:00:00:00} 是 Android 6.0 之后为了隐私
     * 伪造的默认值，遇到它就改用 {@link NetworkInterface} 兜底；
     * 最后还是拿不到就返回这个默认值本身（不是空串）。</p>
     *
     * @param context 上下文
     * @return mac 地址，永不为空
     */
    public static String getMac(Context context) {
        String macAddress;
        WifiInfo connectionInfo;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService("wifi");
        if (wifiManager == null || (connectionInfo = wifiManager.getConnectionInfo()) == null) {
            macAddress = "";
        } else {
            macAddress = connectionInfo.getMacAddress();
            if (DEFAULT_MAC.equalsIgnoreCase(macAddress)) {
                macAddress = getMac();
            }
        }
        return TextUtils.isEmpty(macAddress) ? DEFAULT_MAC : macAddress;
    }

    /**
     * 取机器唯一 id（bbk 序列号 → imei → mac → Build.SERIAL 的降级链）。
     *
     * @param context 上下文
     * @return 机器 id，永不为空
     */
    public static String getMachineId(Context context) {
        String machineId = getBbkSn();
        if (TextUtils.isEmpty(machineId)) {
            machineId = getIMEI(context);
        }
        if (TextUtils.isEmpty(machineId)) {
            machineId = getMac(context);
        }
        if (TextUtils.equals(machineId, DEFAULT_MAC)) {
            machineId = Build.SERIAL;
        }
        return TextUtils.isEmpty(machineId) ? DEFAULT_MACHINE_ID : machineId;
    }

    /**
     * 厂商。
     *
     * @return {@link Build#MANUFACTURER}
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * 可用内存。
     *
     * @param context 上下文
     * @return 字节数
     */
    public static long getMemoryAvailableSize(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
        }
        return memoryInfo.availMem;
    }

    /**
     * 总内存。
     *
     * <p>API 16 起直接用 {@code MemoryInfo.totalMem}；更老的系统读
     * {@code /proc/meminfo} 第一行的数字（单位 KB，注意这里<b>没有</b>乘 1024，
     * 原厂就是这样直接返回 KB 数）。</p>
     *
     * @param context 上下文
     * @return 字节数（API>=16）或 KB 数（老系统）
     */
    public static long getMemoryTotalSize(Context context) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
            if (activityManager != null) {
                activityManager.getMemoryInfo(memoryInfo);
            }
            return memoryInfo.totalMem;
        }
        long totalMem = 0;
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile("/proc/meminfo", "r");
            totalMem = Integer.parseInt(randomAccessFile.readLine().replaceAll("\\D+", ""));
            randomAccessFile.close();
            return totalMem;
        } catch (IOException e) {
            if (!Constants.isDebug) {
                return totalMem;
            }
            LogUtil.w(TAG, e + ":读取/proc/meminfo去获取RAM大小失败");
            return totalMem;
        }
    }

    /**
     * 机型（去掉所有空白字符）。
     *
     * @return 机型；{@link Build#MODEL} 为 null 时返回 null
     */
    public static String getModel() {
        String model = Build.MODEL;
        return model != null ? model.trim().replaceAll("\\s*", "") : model;
    }

    /**
     * 内部型号。
     *
     * @return 属性值；读不到返回空串
     */
    public static String getProductInnerModel() {
        return getStringSystemProperties(PROPERTY_PRODUCT_INNER_MODEL);
    }

    /**
     * SDK 版本。
     *
     * @return {@link Build.VERSION#SDK_INT}
     */
    public static int getSDK() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 反射读 {@code android.os.SystemProperties.get(String)}。
     *
     * <p>注意原厂把 {@code Class} 对象当成接收者传给了 {@code invoke} ——
     * 因为该方法是静态的，接收者实际被忽略，所以能取到值。保留这个写法，
     * 不要"修正"成 {@code invoke(null, key)}。</p>
     *
     * @param key 属性名
     * @return 属性值；失败返回空串
     */
    private static String getStringSystemProperties(String key) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            return (String) systemPropertiesClass.getMethod("get", String.class).invoke(systemPropertiesClass, key);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return "";
        }
    }

    /**
     * 系统版本（增量版本号）。
     *
     * @return {@link Build.VERSION#INCREMENTAL}
     */
    public static String getSystemVersion() {
        return Build.VERSION.INCREMENTAL;
    }

    /**
     * 是否正在充电。
     *
     * @param context 上下文
     * @return 是否充电中
     */
    public static boolean isCharge(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        assert batteryIntent != null;
        // status == 2 即 BATTERY_STATUS_CHARGING；默认值 1 表示未知
        return batteryIntent.getIntExtra("status", 1) == 2;
    }

    /**
     * 是否具备电话能力。
     *
     * @param context 上下文
     * @return 有 TelephonyManager 且 phoneType 不为 NONE 时返回 true
     */
    public static boolean isPhone(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Constants.PHONE);
        return (telephonyManager == null || telephonyManager.getPhoneType() == 0) ? false : true;
    }
}
