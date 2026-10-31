package com.xtc.im.core.common.tlv;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单个实体类的 "tag 号 ↔ 字段" 映射。
 *
 * <p>反射读 {@code @TagValue} 一次之后就不用再读了；正反两个方向都要查
 * （编码时按字段名找 tag，解码时按 tag 找字段），所以维护的是双向表。</p>
 *
 * <p>原厂类名为单字母 {@code i}。</p>
 */
public class TagFieldRegistry {

    /** 实体类全名。 */
    private String className;

    /** tag → 字段。 */
    private Map<Integer, Field> fields = new ConcurrentHashMap<Integer, Field>();

    public TagFieldRegistry(String className) {
        this.className = className;
    }

    /**
     * 按字段名反查 tag 号（遍历，慢路径，只在首次编码某个实体时走到）。
     *
     * @return 找不到返回 0
     */
    public int getTagByFieldName(String fieldName) {
        for (Map.Entry<Integer, Field> entry : this.fields.entrySet()) {
            Field field = entry.getValue();
            if (field != null && field.getName().equals(fieldName)) {
                return entry.getKey().intValue();
            }
        }
        return 0;
    }

    public String getClassName() {
        return this.className;
    }

    public Field getField(int tagValue) {
        return this.fields.get(Integer.valueOf(tagValue));
    }

    public void putField(int tagValue, Field field) {
        this.fields.put(Integer.valueOf(tagValue), field);
    }
}
