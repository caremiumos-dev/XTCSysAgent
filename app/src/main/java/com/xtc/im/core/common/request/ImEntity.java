package com.xtc.im.core.common.request;

import com.xtc.im.core.common.ImLogTag;
import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.tlv.TLVRegistry;
import com.xtc.log.LogUtil;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 所有 IM 报事实体的基类：靠注解把一个 Java 对象映射成 TLV 报文。
 *
 * <p>三件事：</p>
 * <ul>
 *   <li>{@link #getCommand()} —— 类上的 {@code @CommandValue}，即报文首 tag；</li>
 *   <li>{@link #getTagValue(String)} —— 字段上的 {@code @TagValue}，即字段 tag；
 *       这两个都有缓存（字段表在 {@link TLVRegistry}，注解值在本类里），
 *       因为编解码是一条消息一次的热路径，反射读注解扛不住；</li>
 *   <li>{@link #encode()} —— 编码成字节，心跳这条特殊：它没有任何字段，
 *       但仍然要发一个空值 TLV，所以走 {@code encodeEmpty}。</li>
 * </ul>
 *
 * <p>原厂类名为单字母 {@code d}，日志 tag 就是 {@code Entity}。</p>
 */
@CommandValue(0)
public abstract class ImEntity {

    /** 命令号缓存（{@code @CommandValue} 读一次就够了）。 */
    protected transient int command;

    private static final String TAG = ImLogTag.tag("Entity");

    /** 类名 → 反射字段表。{@code getDeclaredFields} 每次都会新建数组，代价不小。 */
    protected static transient Map<String, Field[]> fieldsCache =
            new ConcurrentHashMap<String, Field[]>();

    /**
     * 按字段名取 tag 号。缓存未命中时反射读注解并写回缓存。
     *
     * @return 字段没有 {@code @TagValue} 或不存在时返回 0
     */
    public int getTagValue(String fieldName) {
        String className = getClass().getName();
        int cached = TLVRegistry.getTagByFieldName(className, fieldName);
        if (cached != 0) {
            return cached;
        }
        try {
            Field field = getClass().getDeclaredField(fieldName);
            TagValue annotation = field.getAnnotation(TagValue.class);
            if (annotation != null) {
                TLVRegistry.registerField(className, annotation.value(), field);
            }
            return annotation == null ? 0 : annotation.value();
        } catch (NoSuchFieldException e) {
            LogUtil.e(TAG, e);
            return 0;
        } catch (SecurityException e) {
            LogUtil.e(TAG, e);
            return 0;
        }
    }

    /**
     * 取命令号；类上没有 {@code @CommandValue} 时返回 0。
     */
    public int getCommand() {
        if (this.command != 0) {
            return this.command;
        }
        CommandValue commandValue = getClass().getAnnotation(CommandValue.class);
        if (commandValue != null) {
            this.command = commandValue.value();
        }
        if (commandValue == null) {
            return 0;
        }
        return this.command;
    }

    /**
     * 取本类声明的字段（含缓存）。注意这里**不含父类字段**，与原厂一致。
     */
    public Field[] getFields() {
        String className = getClass().getName();
        Field[] fields = fieldsCache.get(className);
        if (fields != null) {
            return fields;
        }
        Field[] declaredFields = getClass().getDeclaredFields();
        fieldsCache.put(className, declaredFields);
        return declaredFields;
    }

    /**
     * 编码成 TLV 字节。
     *
     * <p>心跳（命令 7）没有字段，但报文仍需一个长度为 0 的 TLV，所以单独走
     * {@code encodeEmpty}；其它命令都按字段逐个小 TLV 写入容器。</p>
     */
    public byte[] encode() {
        int commandValue = getCommand();
        byte[] bytes = null;
        try {
            bytes = commandValue == ImCommand.HEART_BEAT_REQUEST
                    ? EntityCodec.encodeEmpty(commandValue)
                    : EntityCodec.encodeToBytes(this);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return bytes;
    }
}
