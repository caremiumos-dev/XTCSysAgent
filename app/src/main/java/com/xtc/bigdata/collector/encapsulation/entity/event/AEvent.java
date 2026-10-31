package com.xtc.bigdata.collector.encapsulation.entity.event;

import android.content.ContentValues;

import com.xtc.bigdata.collector.encapsulation.BaseAttrManager;
import com.xtc.bigdata.collector.encapsulation.entity.attr.EventAttr;
import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.collector.encapsulation.interfaces.IEvent;
import com.xtc.bigdata.collector.utils.CollectionUtil;
import com.xtc.bigdata.collector.utils.Judgment;
import com.xtc.bigdata.common.utils.FormatUtils;
import com.xtc.utils.encode.JSONUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 所有事件的基类。
 *
 * <p>它把"一条事件落库需要哪几块属性"固化成固定顺序：
 * 事件属性 → 用户属性 → 应用属性 → 机器属性 → 其它属性 → 各事件自带的扩展属性。
 * 顺序决定了 ContentValues 里键的写入次序（进而影响 SQL 的拼接顺序），
 * 不要调整。</p>
 *
 * <p>属性列表是<b>惰性且只组装一次</b>的：一旦 {@link #mAttrs} 非空，
 * 后续 {@link #getContentValues()} 都复用同一批实例。这也是为什么
 * 会话 id、包名版本这些"会变化"的字段在组装后就不再刷新。</p>
 */
public abstract class AEvent implements IEvent {

    /** 组装好的属性列表，null 表示还没组装过。 */
    private List<IAttr> mAttrs;

    /** 扩展判定串，例如 {@code |1|} 表示走了移动数据。 */
    private String extendJudgment = "";

    /** 事件自带的扩展属性（例如自定义事件里额外塞的属性表）。 */
    private final List<IAttr> mExtendAttrs = new ArrayList<IAttr>();

    /**
     * 惰性组装属性列表。
     *
     * @return 属性列表（顺序固定）
     */
    private List<IAttr> getAttrs() {
        if (CollectionUtil.isEmpty(this.mAttrs)) {
            this.mAttrs = Collections.synchronizedList(new ArrayList<IAttr>());
            this.mAttrs.add(getEventAttr());
            this.mAttrs.add(BaseAttrManager.getInstance().getUserAttr());
            this.mAttrs.add(BaseAttrManager.getInstance().getApplicationAttr());
            this.mAttrs.add(BaseAttrManager.getInstance().getMachineAttr());
            this.mAttrs.add(BaseAttrManager.getInstance().getOtherAttr(getJsonExtend(), this.extendJudgment));
            packageExtendAttr();
            this.mAttrs.addAll(this.mExtendAttrs);
        }
        return this.mAttrs;
    }

    /**
     * 取（或创建）事件属性，并补上事件类型与触发时间。
     *
     * @return 事件属性，永不为 null
     */
    private EventAttr getEventAttr() {
        EventAttr eventAttr = packageEventAttr();
        if (eventAttr == null) {
            eventAttr = new EventAttr();
        }
        eventAttr.setEventType(eventType());
        eventAttr.setTrigTime(FormatUtils.getDate());
        return eventAttr;
    }

    /**
     * 追加扩展属性（null 会被忽略）。
     *
     * @param attrs 扩展属性
     */
    public void addAttr(IAttr... attrs) {
        for (IAttr attr : attrs) {
            if (attr != null) {
                this.mExtendAttrs.add(attr);
            }
        }
    }

    /**
     * 组装成一行数据库记录。
     *
     * <p>用 {@code synchronized} 是因为属性列表是共享可变状态，
     * 而事件可能被采集线程与上传线程同时摸到。</p>
     *
     * @return ContentValues
     */
    @Override
    public final synchronized ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        for (IAttr attr : getAttrs()) {
            if (attr != null) {
                attr.insert(contentValues);
            }
        }
        return contentValues;
    }

    /**
     * 事件的扩展 JSON（写进 OtherAttr.extend）。
     *
     * @return JSON 串
     */
    protected abstract String getJsonExtend();

    /**
     * 用指定的属性列表组装 ContentValues（不碰缓存的 {@link #mAttrs}）。
     *
     * @param attrs 属性列表
     * @return ContentValues
     */
    public final synchronized ContentValues getTempContentValues(List<IAttr> attrs) {
        ContentValues contentValues = new ContentValues();
        for (IAttr attr : attrs) {
            if (attr != null) {
                attr.insert(contentValues);
            }
        }
        return contentValues;
    }

    /**
     * Map 转 JSON（给子类拼 extend 用）。
     *
     * @param map 待序列化的 map
     * @return JSON 串
     */
    String hashMap2Json(Map<?, ?> map) {
        return JSONUtil.toJSON(map);
    }

    @Override
    public final List<IAttr> makeData() {
        return getAttrs();
    }

    /**
     * 由子类提供事件属性（可以为 null，基类会补一个空的）。
     *
     * @return 事件属性
     */
    protected abstract EventAttr packageEventAttr();

    /**
     * 由子类补充扩展属性。
     */
    protected abstract void packageExtendAttr();

    /**
     * 标记本次事件是否走了移动数据。
     *
     * @param useMobileTraffic true 表示走移动数据
     */
    public void useMobileTraffic(boolean useMobileTraffic) {
        this.extendJudgment = Judgment.useMobileTraffic(this.extendJudgment, useMobileTraffic);
    }
}
