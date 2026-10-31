package com.xtc.httplib.bean;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import com.xtc.httplib.LogTag;
import com.xtc.log.LogUtil;
import com.xtc.utils.system.SystemConstants;
import java.lang.reflect.InvocationTargetException;

/**
 * {@link DeviceInfo} 的手表端实现：把手表的系统属性拼成请求头需要的设备信息。
 *
 * <p>三个要点：</p>
 * <ul>
 *   <li>每个值都**懒加载 + 缓存**。系统属性读取要走反射，比较贵，
 *       而"绑定号/芯片号/版本号"在一次进程生命周期内不会变。</li>
 *   <li>取不到时统一返回 {@link #UNKOWN}。注意这个单词原厂就拼错了（正确是 unknown），
 *       但服务端/日志已经按这个值处理，不能"顺手改对"。</li>
 *   <li>{@code androidVersion} 只判空、{@code bindNumber} 还要额外判一次
 *       {@code "unkown"}：因为绑定号可能在运行期被写入，缓存了"unkown"时允许重读。</li>
 * </ul>
 */
public class WatchInfo implements DeviceInfo {

    private static final String TAG = LogTag.tag("WatchInfo");

    /** 取不到值时的占位符。原厂拼写即 {@code "unkown"}。 */
    public static final String UNKOWN = "unkown";

    private String androidVersion;
    private String bindNumber;
    private String chipId;
    private Context context;
    private String macAddr;
    private String watchVersion;

    public WatchInfo(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Android 版本号。 */
    @Override
    public String getAndroidVersion() {
        if (!TextUtils.isEmpty(this.androidVersion)) {
            return this.androidVersion;
        }
        this.androidVersion = getSystemProperty("ro.build.version.release");
        if (TextUtils.isEmpty(this.androidVersion)) {
            this.androidVersion = UNKOWN;
        }
        return this.androidVersion;
    }

    /**
     * 绑定号。
     *
     * <p>缓存值如果是 {@code "unkown"} 会重新读一次：开机早期属性可能还没写进来。</p>
     */
    @Override
    public String getBindNumber() {
        if (!TextUtils.isEmpty(this.bindNumber) && !UNKOWN.equals(this.bindNumber)) {
            return this.bindNumber;
        }
        // 原厂用 SystemConstants 里绑号对应的属性键（o.ae）
        this.bindNumber = getSystemProperty(SystemConstants.PROP_BIND_NUMBER);
        if (TextUtils.isEmpty(this.bindNumber)) {
            this.bindNumber = UNKOWN;
        }
        return this.bindNumber;
    }

    /** 编译类型（user / userdebug），不做缓存也不需要兜底值。 */
    @Override
    public String getBuildType() {
        return getSystemProperty("ro.build.type");
    }

    /** 芯片 id。 */
    @Override
    public String getChipId() {
        if (!TextUtils.isEmpty(this.chipId)) {
            return this.chipId;
        }
        this.chipId = getSystemProperty("ro.boot.xtc.chipid");
        if (TextUtils.isEmpty(this.chipId)) {
            this.chipId = UNKOWN;
        }
        return this.chipId;
    }

    /**
     * WiFi MAC 地址。
     *
     * <p>先写 {@code "unkown"} 再覆盖，这样 {@code getConnectionInfo()} 返回 null 时
     * 也能直接返回兜底值（原厂的写法，保留）。Android 6 以后真实 MAC 会被系统抹成
     * {@code 02:00:00:00:00:00}，这里不做任何补救——补救会改变上报内容。</p>
     */
    @Override
    public String getMacAddr() {
        if (!TextUtils.isEmpty(this.macAddr)) {
            return this.macAddr;
        }
        WifiInfo wifiInfo = ((WifiManager) this.context.getSystemService("wifi")).getConnectionInfo();
        this.macAddr = UNKOWN;
        if (wifiInfo == null) {
            return this.macAddr;
        }
        this.macAddr = wifiInfo.getMacAddress();
        if (TextUtils.isEmpty(this.macAddr)) {
            this.macAddr = UNKOWN;
        }
        return this.macAddr;
    }

    /**
     * 反射读 {@code android.os.SystemProperties#get(String)}。
     *
     * <p>为什么要反射：{@code SystemProperties} 是隐藏 API，编译期不可见。
     * 四类异常各自记日志后返回 {@code null}（不是空串），调用方用
     * {@link TextUtils#isEmpty(CharSequence)} 统一兜底。</p>
     */
    @Override
    public String getSystemProperty(String key) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            return (String) systemPropertiesClass.getMethod("get", String.class).invoke(systemPropertiesClass, key);
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, e);
            return null;
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
            return null;
        } catch (NoSuchMethodException e) {
            LogUtil.e(TAG, e);
            return null;
        } catch (InvocationTargetException e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /** 手表当前软件版本号。 */
    @Override
    public String getWatchVersion() {
        if (!TextUtils.isEmpty(this.watchVersion)) {
            return this.watchVersion;
        }
        // 原厂用 SystemConstants 里版本号对应的属性键（o.T）
        this.watchVersion = getSystemProperty(SystemConstants.PROP_SOFT_VERSION);
        if (TextUtils.isEmpty(this.watchVersion)) {
            this.watchVersion = UNKOWN;
        }
        return this.watchVersion;
    }
}
