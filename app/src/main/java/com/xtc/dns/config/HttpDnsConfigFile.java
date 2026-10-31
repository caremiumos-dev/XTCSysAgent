package com.xtc.dns.config;

import com.xtc.dns.DnsLogTag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * HttpDns 的本地配置文件读写（原厂 {@code com.xtc.dns.config.b}，日志 tag 是 {@code Tools}）。
 *
 * <p>配置落在 sdcard 的固定路径上，好处是**不需要 root、也不依赖 SharedPreferences**：
 * 现场调试时直接改这个文件、重启进程就能生效，这是原厂留下的排障入口。</p>
 *
 * <p>所有异常都 {@code printStackTrace()} 后返回失败/未找到，
 * 因为配置缺失是"正常状态"（首次开机就没有该文件）。</p>
 */
public class HttpDnsConfigFile {

    /** 日志 tag（原厂类里带了这个字段但没被引用，保留以保持一致）。 */
    private static final String TAG = DnsLogTag.serverTag("Tools");

    /** 配置文件绝对路径。 */
    private static final String CONFIG_PATH = "/mnt/sdcard/xtc/ibwatch/common/httpdnsinfo.properties";

    /**
     * 读取配置。
     *
     * @return 读到的配置；文件不存在或解析失败时返回 null（调用方需判空）
     */
    public static Properties load() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(CONFIG_PATH)));
            return properties;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 写回配置。文件不存在时先创建。
     *
     * @return 是否写成功
     */
    public static boolean save(Properties properties) {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                file.createNewFile();
            }
            properties.store(new FileOutputStream(file), "");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 删除配置文件。 */
    public static boolean delete() {
        File file = new File(CONFIG_PATH);
        return file.exists() && file.delete();
    }
}
