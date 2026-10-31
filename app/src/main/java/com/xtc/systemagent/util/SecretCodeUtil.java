package com.xtc.systemagent.util;

import com.xtc.systemagent.activity.LocalFetchSecretActivity;
import java.util.HashMap;

/**
 * 暗码到页面的映射表：在手表上输入 {@code *#*#<code>#*#*} 就能拉起对应页面。
 */
public class SecretCodeUtil {

    private static final HashMap<String, Class<?>> sCodeClassMap = new HashMap<>();

    static {
        // 0769873 -> 本地触发日志抓取页面
        sCodeClassMap.put("0769873", LocalFetchSecretActivity.class);
    }

    /**
     * 按暗码取对应页面。
     *
     * @return 没有匹配时返回 null
     */
    public static Class<?> getActivityFromSecretCode(String code) {
        if (sCodeClassMap.containsKey(code)) {
            return sCodeClassMap.get(code);
        }
        return null;
    }
}
