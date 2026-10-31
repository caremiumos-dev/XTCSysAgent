package com.xtc.database.core;

/**
 * 一个列的定义：列名 + 建表时用的列定义片段（例如类型与约束）。
 */
public class ColumnStruct {

    private String columnName;
    private String columnLimit;

    public ColumnStruct() {
    }

    public ColumnStruct(String columnName, String columnLimit) {
        this.columnName = columnName;
        this.columnLimit = columnLimit;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnLimit() {
        return this.columnLimit;
    }

    public void setColumnLimit(String columnLimit) {
        this.columnLimit = columnLimit;
    }

    @Override
    public String toString() {
        return "ColumnStruct{columnName='" + this.columnName
                + "', columnLimit='" + this.columnLimit + "'}";
    }
}
