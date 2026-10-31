package com.xtc.im.core.common.tlv;

import android.text.TextUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 三张注册表的门面：
 * <ul>
 *   <li>编码结果缓存 —— 相同 (帧类型, 数据类型, tag, 值) 只编码一次；</li>
 *   <li>解码结果缓存 —— 相同原始字节只解析一次；</li>
 *   <li>实体字段表 —— 实体类 → (tag, 字段)。</li>
 * </ul>
 *
 * <p>两张缓存都用 {@link ArrayList} + "超过 20 条就从头删" 的笨办法做容量控制。
 * 不用 LRU 是因为命中模式是"最近几条反复用"，先进先出在这里足够，而且实现更省。</p>
 *
 * <p>原厂类名为单字母 {@code b}。</p>
 */
public class TLVRegistry {

    /** 编码结果缓存（最多 20 条）。 */
    private static List<TLVEncodeEntry> encodeEntries = new ArrayList<TLVEncodeEntry>();

    /** 解码结果缓存（最多 20 条）。 */
    private static List<TLVDecodeEntry> decodeEntries = new ArrayList<TLVDecodeEntry>();

    /** 实体字段表：类名 → tag/字段映射。 */
    private static Map<String, TagFieldRegistry> fieldRegistries =
            new ConcurrentHashMap<String, TagFieldRegistry>();

    /**
     * 按类名 + 字段名反查 tag 号。
     */
    public static int getTagByFieldName(String className, String fieldName) {
        if (TextUtils.isEmpty(className) || TextUtils.isEmpty(fieldName)) {
            return 0;
        }
        TagFieldRegistry registry = fieldRegistries.get(className);
        if (registry != null) {
            return registry.getTagByFieldName(fieldName);
        }
        return 0;
    }

    /**
     * 查解码缓存。
     */
    public static TLVDecodeResult findDecodeResult(byte[] rawData) {
        synchronized (decodeEntries) {
            Iterator<TLVDecodeEntry> iterator = decodeEntries.iterator();
            while (iterator.hasNext()) {
                TLVDecodeResult result = iterator.next().match(rawData);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    /**
     * 查编码缓存。
     */
    public static TLVEncodeResult findEncodeResult(int frameType, int dataType, int tagValue,
                                                   byte[] value) {
        synchronized (encodeEntries) {
            Iterator<TLVEncodeEntry> iterator = encodeEntries.iterator();
            while (iterator.hasNext()) {
                TLVEncodeResult result = iterator.next().match(frameType, dataType, tagValue, value);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    /**
     * 按类名 + tag 号取字段。
     */
    public static Field getField(String className, int tagValue) {
        if (TextUtils.isEmpty(className) || tagValue == 0) {
            return null;
        }
        TagFieldRegistry registry = fieldRegistries.get(className);
        if (registry != null) {
            return registry.getField(tagValue);
        }
        return null;
    }

    /**
     * 清空全部缓存（IM 重新登录 / 切换账号时调用）。
     */
    public static void clear() {
        synchronized (encodeEntries) {
            encodeEntries.clear();
        }
        synchronized (decodeEntries) {
            decodeEntries.clear();
        }
        fieldRegistries.clear();
    }

    /**
     * 登记一条编码结果。
     */
    public static void registerEncodeResult(int frameType, int dataType, int tagValue, byte[] value,
                                            TLVEncodeResult encodeResult) {
        if (value == null || value.length == 0 || encodeResult == null) {
            return;
        }
        TLVEncodeEntry entry = new TLVEncodeEntry(frameType, dataType, tagValue, value, encodeResult);
        synchronized (encodeEntries) {
            while (encodeEntries.size() > 20) {
                encodeEntries.remove(0);
            }
            encodeEntries.add(entry);
        }
    }

    /**
     * 登记一个字段：第一次见到某个实体类的某个 tag 时建表并缓存。
     */
    public static void registerField(String className, int tagValue, Field field) {
        if (TextUtils.isEmpty(className) || tagValue == 0 || field == null) {
            return;
        }
        TagFieldRegistry registry = fieldRegistries.get(className);
        if (registry == null) {
            registry = new TagFieldRegistry(className);
            fieldRegistries.put(className, registry);
        }
        registry.putField(tagValue, field);
    }

    /**
     * 登记一条解码结果。
     */
    public static void registerDecodeResult(byte[] rawData, TLVDecodeResult decodeResult) {
        if (rawData == null || rawData.length == 0 || decodeResult == null) {
            return;
        }
        TLVDecodeEntry entry = new TLVDecodeEntry(rawData, decodeResult);
        synchronized (decodeEntries) {
            while (decodeEntries.size() > 20) {
                decodeEntries.remove(0);
            }
            decodeEntries.add(entry);
        }
    }
}
