package com.xtc.bigdata.collector.encapsulation.entity.attr;

import android.content.ContentValues;

import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.common.db.constant.Columns;

/**
 * 事件属性表。
 *
 * <p>字段可见性是包级 —— 原厂就这样，只有 setter/getter 是对外的。
 * 所有字段默认空串（不是 null），因为数据库列不允许 null，
 * 而 {@code ContentValues.put(String, null)} 会写成 NULL。</p>
 *
 * <p>setter 返回 {@code this} 是为了链式赋值（事件类里一行拼完）。</p>
 */
public class EventAttr implements IAttr {

    /** 功能点名。 */
    String functionName;

    /** 事件类型。 */
    int eventType;

    /** 触发时间。 */
    String trigTime;

    /** 触发值。 */
    String trigValue;

    /** 页面名。 */
    String page;

    /** 模块细节。 */
    String moduleDetail;

    /** 全字段空串构造。 */
    public EventAttr() {
        this.functionName = "";
        this.eventType = 0;
        this.trigTime = "";
        this.trigValue = "";
        this.page = "";
        this.moduleDetail = "";
    }

    /**
     * 全字段构造。
     *
     * @param functionName 功能点名
     * @param eventType    事件类型
     * @param trigTime     触发时间
     * @param trigValue    触发值
     * @param page         页面名
     * @param moduleDetail 模块细节
     */
    public EventAttr(String functionName, int eventType, String trigTime, String trigValue,
                     String page, String moduleDetail) {
        this.functionName = "";
        this.eventType = 0;
        this.trigTime = "";
        this.trigValue = "";
        this.page = "";
        this.moduleDetail = "";
        this.functionName = functionName;
        this.eventType = eventType;
        this.trigTime = trigTime;
        this.trigValue = trigValue;
        this.page = page;
        this.moduleDetail = moduleDetail;
    }

    @Override
    public IAttr clone() {
        EventAttr eventAttr = new EventAttr();
        eventAttr.functionName = this.functionName;
        eventAttr.trigValue = this.trigValue;
        eventAttr.page = this.page;
        eventAttr.moduleDetail = this.moduleDetail;
        eventAttr.eventType = this.eventType;
        eventAttr.trigTime = this.trigTime;
        return eventAttr;
    }

    /** @return 事件类型 */
    public int getEventType() {
        return this.eventType;
    }

    /** @return 功能点名 */
    public String getFunctionName() {
        return this.functionName;
    }

    /** @return 模块细节 */
    public String getModuleDetail() {
        return this.moduleDetail;
    }

    /** @return 页面名 */
    public String getPage() {
        return this.page;
    }

    /** @return 触发时间 */
    public String getTrigTime() {
        return this.trigTime;
    }

    /** @return 触发值 */
    public String getTrigValue() {
        return this.trigValue;
    }

    @Override
    public void insert(ContentValues contentValues) {
        contentValues.put(Columns.COLUMN_EA_FUNCTIONNAME, this.functionName);
        contentValues.put(Columns.COLUMN_EA_EVENTTYPE, Integer.valueOf(this.eventType));
        contentValues.put(Columns.COLUMN_EA_TTIME, this.trigTime);
        contentValues.put(Columns.COLUMN_EA_TVALUE, this.trigValue);
        contentValues.put(Columns.COLUMN_EA_PAGE, this.page);
        contentValues.put(Columns.COLUMN_EA_MODULEDETAIL, this.moduleDetail);
    }

    /**
     * 设置事件类型。
     *
     * @param eventType 事件类型
     * @return this
     */
    public EventAttr setEventType(int eventType) {
        this.eventType = eventType;
        return this;
    }

    /**
     * 设置功能点名。
     *
     * @param functionName 功能点名
     * @return this
     */
    public EventAttr setFunctionName(String functionName) {
        this.functionName = functionName;
        return this;
    }

    /**
     * 设置模块细节。
     *
     * @param moduleDetail 模块细节
     * @return this
     */
    public EventAttr setModuleDetail(String moduleDetail) {
        this.moduleDetail = moduleDetail;
        return this;
    }

    /**
     * 设置页面名。
     *
     * @param page 页面名
     * @return this
     */
    public EventAttr setPage(String page) {
        this.page = page;
        return this;
    }

    /**
     * 设置触发时间。
     *
     * @param trigTime 触发时间
     * @return this
     */
    public EventAttr setTrigTime(String trigTime) {
        this.trigTime = trigTime;
        return this;
    }

    /**
     * 设置触发值。
     *
     * @param trigValue 触发值
     * @return this
     */
    public EventAttr setTrigValue(String trigValue) {
        this.trigValue = trigValue;
        return this;
    }
}
