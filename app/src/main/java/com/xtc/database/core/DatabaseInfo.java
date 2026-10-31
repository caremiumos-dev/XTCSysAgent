package com.xtc.database.core;

/**
 * 一个数据库的元信息：库名、版本号，以及已打开的 helper 引用。
 */
public class DatabaseInfo {

    private String databaseName;
    private int databaseVersion;
    private DatabaseHelper helper;

    public String getDatabaseName() {
        return this.databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public int getDatabaseVersion() {
        return this.databaseVersion;
    }

    public void setDatabaseVersion(int databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public DatabaseHelper getHelper() {
        return this.helper;
    }

    public void setHelper(DatabaseHelper helper) {
        this.helper = helper;
    }

    @Override
    public String toString() {
        return "DatabaseInfo{databaseName='" + this.databaseName
                + "', databaseVersion=" + this.databaseVersion + '}';
    }
}
