package com.xtc.bigdata.collector.encapsulation.interfaces;

import android.content.ContentValues;

import java.util.List;

/**
 * 一次可上报的事件。
 *
 * <p>事件负责三件事：给出事件类型、把自身与公共属性组装成
 * {@link ContentValues}、提供属性列表给需要改包名的场景（DropBox）。</p>
 */
public interface IEvent {

    /**
     * 事件类型编号，取值见 {@code EType}。
     *
     * @return 类型编号
     */
    int eventType();

    /**
     * 组装成一行数据库记录。
     *
     * @return ContentValues
     */
    ContentValues getContentValues();

    /**
     * 取属性列表（会触发一次组装并缓存）。
     *
     * @return 属性列表
     */
    List<IAttr> makeData();
}
