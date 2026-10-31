package com.xtc.systemagent.util;

import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.storage.FolderManager;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * 日志采集策略的本地缓存（{@code log.properties}）。
 *
 * <p>文件放在公共目录下，字段目前只有一个 {@code updateTime}，
 * 用来判断策略是否需要重新拉取（24 小时有效期，见
 * {@code LogStrategyService#dealLogStrategyInit}）。</p>
 */
public class PropertiesUtil {

    private static final String TAG = "Log_PropertiesUtil";

    /** 策略缓存文件路径。 */
    private static final String PROP_PATH =
            FolderManager.getInstance().getCommonDir() + "log.properties";

    private PropertiesUtil() {
    }

    /** 删除策略缓存。文件本来就不存在时返回 false。 */
    public static boolean deleteConfig() {
        File file = new File(PROP_PATH);
        return file.exists() && file.delete();
    }

    /**
     * 把系统版本号（形如 {@code 2.4.27}）转成整型版本号。
     *
     * <p>规则：去掉小数点后若只有 3 位就补 {@code "00"}、4 位补 {@code "0"}，
     * 凑齐 5 位。比如 {@code 2.4.27} → {@code 2427} → {@code 242700}。</p>
     *
     * @return 转换失败（属性为空或不是数字）返回 -1
     */
    public static int getSystemVerisonCode() {
        String version = SystemPropertiesUtils.get(SystemConstants.PROP_SOFT_VERSION, "");
        if (TextUtils.isEmpty(version)) {
            return -1;
        }
        String digits = version.replace(".", "");
        if (digits.length() == 3) {
            digits = digits + "00";
        } else if (digits.length() == 4) {
            digits = digits + "0";
        }
        int versionCode = Integer.valueOf(digits).intValue();
        LogUtil.i(TAG, "versionCode" + versionCode);
        return versionCode;
    }

    /** 读取策略缓存；文件不存在或读失败返回 null。 */
    public static Properties loadConfig() {
        Properties properties = new Properties();
        try {
            File file = new File(PROP_PATH);
            if (!file.exists()) {
                return null;
            }
            properties.load(new FileInputStream(PROP_PATH));
            return properties;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /** 写入策略缓存。 */
    public static boolean saveConfig(Properties properties) {
        try {
            File file = new File(PROP_PATH);
            if (!file.exists()) {
                file.createNewFile();
            }
            properties.store(new FileOutputStream(file), "");
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
            return false;
        }
    }
}
