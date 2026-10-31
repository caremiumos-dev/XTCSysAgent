package com.xtc.bigdata.common.utils;

/**
 * Gson 门面。
 *
 * <p>本类只是 {@code com.xtc.utils.encode.JSONUtil} 的一层壳，之所以再包一层，
 * 是为了让采集库内部换序列化实现时只改这一个文件。方法名与底层保持一致。</p>
 *
 * <p>因为本类的简单类名与底层类重名，这里只能用全限定名调用，
 * 不能 import（同一编译单元内同名会编译不过）。</p>
 */
public class JSONUtil {

    private JSONUtil() {
    }

    /**
     * JSON 反序列化成对象。
     *
     * @param json      文本
     * @param classType 目标类型
     * @param <T>       目标类型
     * @return 对象；解析失败返回 null
     */
    public static <T> T fromJSON(String json, Class<T> classType) {
        return com.xtc.utils.encode.JSONUtil.fromJSON(json, classType);
    }

    /**
     * 取 JSON 里的某个字段。
     *
     * @param json 文本
     * @param key  字段名
     * @return 字段值；取不到返回 null
     */
    public static Object get(String json, String key) {
        return com.xtc.utils.encode.JSONUtil.get(json, key);
    }

    /**
     * JSON 反序列化成集合。
     *
     * @param json            文本
     * @param collectionClass 集合类型（如 {@code List.class}）
     * @param elementClasses  元素类型
     * @param <T>             集合类型
     * @return 集合；解析失败返回 null
     */
    public static <T> T toCollection(String json, Class<?> collectionClass, Class<?>... elementClasses) {
        return com.xtc.utils.encode.JSONUtil.toCollection(json, collectionClass, elementClasses);
    }

    /**
     * 对象序列化成 JSON。
     *
     * @param object 对象
     * @return 文本
     */
    public static String toJSON(Object object) {
        return com.xtc.utils.encode.JSONUtil.toJSON(object);
    }
}
