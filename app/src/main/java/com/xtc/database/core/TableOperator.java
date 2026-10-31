package com.xtc.database.core;

/**
 * 数据库表操作类型编号。
 *
 * <p>原厂类名为单字母 {@code c}，七个常量也被混淆成 {@code a}..{@code g}；
 * 反编译产物里已无任何调用点，无法从上下文反推确切语义，
 * 这里按 ORMLite 表生命周期中最常见的七种操作命名，**数值保持 1..7 不变**。</p>
 */
public interface TableOperator {
    /** 建表。 */
    int CREATE = 1;
    /** 删表。 */
    int DROP = 2;
    /** 清空表数据。 */
    int CLEAR = 3;
    /** 升级表结构。 */
    int UPGRADE = 4;
    /** 降级表结构。 */
    int DOWNGRADE = 5;
    /** 查询。 */
    int QUERY = 6;
    /** 删除。 */
    int DELETE = 7;
}
