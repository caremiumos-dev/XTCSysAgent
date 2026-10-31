package com.xtc.bigdata.collector.encapsulation.interfaces;

import android.content.ContentValues;

/**
 * 一条"属性"—— 对应数据库里的一张属性表的一列集合。
 *
 * <p>一次事件由多条属性拼成（事件、用户、应用、机器、其它），
 * 每条属性负责把自己的字段塞进同一个 {@link ContentValues}，
 * 最终由 provider 一行写完。</p>
 */
public interface IAttr {

    /**
     * 深拷贝。
     *
     * <p>DropBox 扫描要用"改了包名版本号的副本"落库，
     * 不能污染原事件里缓存的属性实例，所以必须有 clone。</p>
     *
     * @return 新实例
     */
    IAttr clone();

    /**
     * 把本属性写进 ContentValues。
     *
     * @param contentValues 目标
     */
    void insert(ContentValues contentValues);
}
