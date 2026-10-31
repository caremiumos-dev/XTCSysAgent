package com.xtc.im.core.common.request;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.xtc.im.core.common.ImLogTag;
import com.xtc.im.core.common.tlv.TLVCodec;
import com.xtc.im.core.common.tlv.TLVDecodeResult;
import com.xtc.im.core.common.tlv.TLVObject;
import com.xtc.im.core.common.tlv.TLVRegistry;
import com.xtc.im.core.common.request.entity.RequestEntity;
import com.xtc.im.core.common.response.entity.ResponseEntity;
import com.xtc.log.LogUtil;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

/**
 * 实体 ↔ TLV 字节 的编解码器。
 *
 * <p>编码：命令号当外层 tag，实体每个字段写成一个内层 TLV（tag 来自
 * {@code @TagValue}）。值为 0 的整型/长整型**直接跳过并打 warning**，
 * 这是协议里的"默认值不上行"约定，靠它省流量。</p>
 *
 * <p>解码：按 tag 反射找字段写回；字段类型是实体（{@code ImEntity} 子类）时，
 * 说明该字段是嵌套容器，再按"容器里子节点的 tag 就是子实体的命令号"递归解一层。</p>
 *
 * <p>原厂类名为单字母 {@code i}，日志 tag 就是 {@code TLVObjectUtil}。</p>
 */
public class EntityCodec {

    private static final String TAG = ImLogTag.tag("TLVObjectUtil");

    /**
     * 编码一个实体：外层 TLV 的 tag 是命令号，值是"所有字段拼成的容器"。
     */
    public static TLVObject encodeToTlv(ImEntity entity) {
        TLVObject tlv = new TLVObject();
        try {
            tlv.put(entity.getCommand(), encodeFields(entity));
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return tlv;
    }

    /**
     * 把一段字节解成响应实体。
     */
    public static ResponseEntity decodeResponse(byte[] data) throws Throwable {
        return (ResponseEntity) decodeAny(data);
    }

    /**
     * 按给定类型实例化并填充。
     */
    @SuppressWarnings("unchecked")
    private static <T> T decodeEntity(TLVDecodeResult result, Class<?> clazz) throws Throwable {
        T entity = (T) ((ImEntity) clazz.newInstance());
        readEntity(result, (ImEntity) entity);
        return entity;
    }

    /**
     * 把一段字节解成指定类型的实体。
     */
    @SuppressWarnings("unchecked")
    public static <T> T decodeEntity(byte[] data, Class<?> clazz) throws Throwable {
        return (T) decodeEntity(TLVCodec.decode(data), clazz);
    }

    /**
     * 编码一个字段：按字段类型选择写入方式。
     *
     * <p>注意判空在最前面：字段是 null 时只打 warning 就返回，
     * 因为协议里"没值"和"值为 0"必须区分开——null 表示调用方没设，0 表示明确要清空。</p>
     */
    private static void writeField(int tagValue, Field field, ImEntity entity, TLVObject out)
            throws Exception {
        field.setAccessible(true);
        Class<?> type = field.getType();
        Object value = field.get(entity);
        if (value == null) {
            LogUtil.w(TAG, "formatdata error(value is null),name:" + field.getName()
                    + ",type:" + field.getType());
            return;
        }
        if (type == Integer.TYPE) {
            int intValue = ((Integer) value).intValue();
            if (intValue != 0) {
                out.put(tagValue, intValue);
                return;
            }
            LogUtil.w(TAG, "formatdata error(" + value.getClass().getSimpleName()
                    + " value is 0):" + field.getName());
            return;
        }
        if (type == Long.TYPE) {
            long longValue = ((Long) value).longValue();
            if (longValue != 0) {
                out.put(tagValue, longValue);
                return;
            }
            LogUtil.w(TAG, "formatdata error(" + value.getClass().getSimpleName()
                    + " value is 0):" + field.getName());
            return;
        }
        if (type == String.class) {
            out.put(tagValue, value.toString());
            return;
        }
        if (type == byte[].class) {
            out.put(tagValue, (byte[]) value);
        } else if (value instanceof ImEntity) {
            out.put(tagValue, encodeToTlv((ImEntity) value));
        } else {
            LogUtil.w(TAG, "formatdata error(unsupport type " + value.getClass().getName()
                    + "):" + field.getName());
        }
    }

    /**
     * 填充一个实体：容器节点就按子节点逐个填，普通节点直接填自己。
     */
    private static void readEntity(TLVDecodeResult result, ImEntity entity)
            throws IllegalAccessException, NoSuchFieldException {
        if (result.getDataType() == 32) {
            readFields((List<TLVDecodeResult>) result.getValue(), entity);
        } else {
            readFieldByTag(result, entity);
        }
    }

    /**
     * 把单个节点的值写进字段。
     */
    private static void readField(TLVDecodeResult result, ImEntity entity, Field field)
            throws IllegalAccessException, NoSuchFieldException {
        field.setAccessible(true);
        Class<?> type = field.getType();
        if (type == Integer.TYPE) {
            field.setInt(entity, result.getValueAsInt());
            return;
        }
        if (type == Long.TYPE) {
            field.setLong(entity, result.getValueAsLong());
            return;
        }
        if (type == String.class) {
            field.set(entity, result.getValueAsString());
            return;
        }
        if (type == byte[].class) {
            field.set(entity, result.getValue());
            return;
        }
        if (type != ImEntity.class && type.getSuperclass() != ImEntity.class) {
            LogUtil.w(TAG, "不支持值类型:" + field.getName()
                    + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + type);
            return;
        }
        if (result.getDataType() != 32) {
            LogUtil.w(TAG, "TLV数据类型错误!");
            return;
        }
        List<TLVDecodeResult> children = (List<TLVDecodeResult>) result.getValue();
        if (children != null) {
            for (TLVDecodeResult child : children) {
                Class<?> childClass = CommandRegistry.commandClasses.get(
                        Integer.valueOf(child.getTagValue()));
                if (childClass != null) {
                    try {
                        ImEntity childEntity = (ImEntity) childClass.newInstance();
                        readEntity(child, childEntity);
                        field.set(entity, childEntity);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.w(TAG, "未找到该类型:" + field.getName()
                            + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + type);
                }
            }
        }
    }

    /**
     * 逐个填充容器里的子节点。
     */
    private static void readFields(List<TLVDecodeResult> children, ImEntity entity)
            throws IllegalAccessException, NoSuchFieldException {
        if (children == null) {
            return;
        }
        Iterator<TLVDecodeResult> iterator = children.iterator();
        while (iterator.hasNext()) {
            readFieldByTag(iterator.next(), entity);
        }
    }

    /**
     * 编码一个"只有 tag、没有值"的报文（心跳用）。
     */
    public static byte[] encodeEmpty(int tagValue) throws IOException {
        TLVObject tlv = new TLVObject();
        tlv.put(tagValue, (byte[]) null);
        return tlv.toBytes();
    }

    /**
     * 把一段字节解成请求实体。
     */
    public static RequestEntity decodeRequest(byte[] data) throws Throwable {
        return (RequestEntity) decodeAny(data);
    }

    /**
     * 按 tag 找到字段再填值。
     */
    private static void readFieldByTag(TLVDecodeResult result, ImEntity entity)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = findField(result, entity);
        if (field != null) {
            readField(result, entity, field);
        }
    }

    /**
     * 编码成字节。
     */
    public static byte[] encodeToBytes(ImEntity entity) {
        return encodeToTlv(entity).toBytes();
    }

    /**
     * 把实体所有字段拼成内层容器。
     */
    private static TLVObject encodeFields(ImEntity entity) throws Exception {
        TLVObject tlv = new TLVObject();
        for (Field field : entity.getFields()) {
            writeField(entity.getTagValue(field.getName()), field, entity, tlv);
        }
        return tlv;
    }

    /**
     * 解出任意实体（请求或响应，按首 tag 查表决定类型）。
     */
    public static ImEntity decodeAny(byte[] data) throws Throwable {
        TLVDecodeResult result = TLVCodec.decode(data);
        return (ImEntity) decodeEntity(result, CommandRegistry.commandClasses.get(
                Integer.valueOf(result.getTagValue())));
    }

    /**
     * 按 tag 找字段：先查缓存，再遍历字段表（遍历时顺手把结果写回缓存）。
     */
    private static Field findField(TLVDecodeResult result, ImEntity entity)
            throws NoSuchFieldException {
        Field cached = TLVRegistry.getField(entity.getClass().getName(), result.getTagValue());
        if (cached != null) {
            return cached;
        }
        Field[] fields = entity.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (result.getTagValue() == entity.getTagValue(field.getName())) {
                TLVRegistry.registerField(entity.getClass().getName(), result.getTagValue(), field);
                return field;
            }
        }
        return cached;
    }
}
