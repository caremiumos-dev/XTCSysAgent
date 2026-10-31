package com.xtc.utils.encode;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.xtc.log.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * JSON 序列化/反序列化工具，全工程统一走这里。
 *
 * <p>原厂类名就叫 JSONUtil（混淆只发生在同包其他类上），这里沿用。
 * 关键在于它用的是自己定制过的 Gson：</p>
 * <ul>
 *   <li>不转义 HTML 字符（{@code disableHtmlEscaping}），否则上报的 url 里的 & 会被转义成 unicode 形式；</li>
 *   <li>{@link Date} 用毫秒时间戳收发，而不是 Gson 默认的字符串格式；</li>
 *   <li>boolean/Boolean 既能吃 0/1 也能吃 true/false——服务端两种都发过；</li>
 *   <li>byte[] 用 Base64（NO_WRAP）收发，兼容老版本发来的 int 数组。</li>
 * </ul>
 *
 * <p>所有解析入口在失败时都只记日志并返回 null，不抛异常；调用方依赖这个约定。</p>
 */
public class JSONUtil {

    /** 日志 tag。原厂取的是类名，改名后 tag 会随之变成 com.xtc.utils.encode.JSONUtil。 */
    private static String TAG = JSONUtil.class.getName();

    /** 全局共享的 Gson 实例，配置见 {@link #createGsonBuilder()}。 */
    private static Gson gson = createGsonBuilder().create();

    /**
     * 构造本工程定制的 GsonBuilder。
     *
     * <p>Retrofit 的 GsonConverterFactory 也会直接拿这个 builder 再 create 一次，
     * 所以这里不能缓存成单例返回前做副作用。</p>
     */
    public static GsonBuilder createGsonBuilder() {
        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();
        // Date <-> 毫秒时间戳
        builder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context)
                -> new Date(json.getAsJsonPrimitive().getAsLong()));
        builder.registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, typeOfT, context)
                -> new JsonPrimitive((Number) Long.valueOf(date.getTime())));
        // boolean：数字 0/1 与 true/false 都接受
        builder.registerTypeAdapter(Boolean.TYPE, (JsonDeserializer<Boolean>) (json, typeOfT, context) -> {
            try {
                return Boolean.valueOf(json.getAsInt() != 0);
            } catch (NumberFormatException e) {
                return Boolean.valueOf(json.getAsBoolean());
            }
        });
        builder.registerTypeAdapter(Boolean.class, (JsonDeserializer<Boolean>) (json, typeOfT, context) -> {
            try {
                return Boolean.valueOf(json.getAsInt() != 0);
            } catch (NumberFormatException e) {
                return Boolean.valueOf(json.getAsBoolean());
            }
        });
        // byte[]：序列化用 Base64（NO_WRAP），反序列化兼容 Base64 串和 JSON 数组
        builder.registerTypeAdapter(byte[].class, (JsonSerializer<byte[]>) (data, typeOfT, context)
                -> new JsonPrimitive(Base64.encodeToString(data, Base64.NO_WRAP)));
        builder.registerTypeAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, typeOfT, context) -> {
            try {
                return Base64.decode(json.getAsString(), Base64.NO_WRAP);
            } catch (IllegalStateException e) {
                JsonArray jsonArray = json.getAsJsonArray();
                byte[] bytes = new byte[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    bytes[i] = jsonArray.get(i).getAsByte();
                }
                return bytes;
            }
        });
        // int 基本类型：统一走 getAsInt，避免 "1" 这种字符串反序列化失败
        builder.registerTypeAdapter(Integer.TYPE, (JsonDeserializer<Integer>) (json, typeOfT, context)
                -> Integer.valueOf(json.getAsInt()));
        return builder;
    }

    /**
     * 反序列化单个对象。
     *
     * @param json      原始 JSON
     * @param classType 目标类型
     * @return 反序列化结果；json 为空或解析失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJSON(String json, Class<T> classType) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return gson.fromJson(json, classType);
        } catch (Exception e) {
            LogUtil.e(TAG, "fromJSON failed with jsonStr=" + json + ", classType=" + classType, e);
            return null;
        }
    }

    /**
     * 反序列化成集合。
     *
     * @param json            原始 JSON
     * @param collectionClass 集合类型，例如 List.class
     * @param elementClasses  元素类型，例如 String.class
     * @return 集合对象；json 为空或解析失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T toCollection(String json, Class<?> collectionClass, Class<?>... elementClasses) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return (T) gson.fromJson(json, TypeToken.getParameterized(collectionClass, elementClasses).getType());
        } catch (Exception e) {
            LogUtil.e(TAG, "toCollection failed with jsonStr=" + json + ", collectionClass=" + collectionClass, e);
            return null;
        }
    }

    /**
     * 从 JSON 串里取一个字段（不转换成实体）。
     *
     * @return 字段值；JSON 非法或缺字段时记日志并返回 null
     */
    public static Object get(String json, String key) {
        try {
            return new JSONObject(json).get(key);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    /**
     * 序列化成 JSON 串。
     *
     * @param object 待序列化对象
     * @return JSON 串；入参为 null 返回 null，序列化失败返回 null
     */
    @Nullable
    public static String toJSON(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            LogUtil.e(TAG, "toJSON failed with obj=" + object, e);
            return null;
        }
    }
}
