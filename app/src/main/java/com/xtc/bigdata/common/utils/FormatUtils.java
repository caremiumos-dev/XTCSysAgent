package com.xtc.bigdata.common.utils;

import android.text.TextUtils;

import com.xtc.bigdata.common.constants.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 格式化工具：堆栈转义还原 + 触发时间。
 */
public class FormatUtils {

    private FormatUtils() {
    }

    /**
     * 把日志文本里被转义的 {@code \\n} / {@code \\t} 还原成真实换行与制表符。
     *
     * <p>服务端下发的日志是单行 JSON 字符串，里面带着字面量反斜杠 n，
     * 直接入库会让堆栈变成一行。这里只做这两个替换，不动其它内容。</p>
     *
     * @param log 原始文本
     * @return 还原后的文本；入参为空时原样返回
     */
    public static String formatExceptionLog(String log) {
        return TextUtils.isEmpty(log) ? log : log.replace("\\n", "\n").replace("\\t", "\t");
    }

    /**
     * 取当前时间的触发时间字符串。
     *
     * @return 形如 {@code 2024-01-01 10:00:00.000}
     */
    public static String getDate() {
        return new SimpleDateFormat(Constants.DATA_TRIGGER_DATE_FORMAT, Locale.getDefault()).format(new Date());
    }
}
