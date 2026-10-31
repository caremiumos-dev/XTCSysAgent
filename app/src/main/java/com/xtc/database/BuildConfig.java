package com.xtc.database;

/**
 * 数据库模块的构建信息。原厂类名被混淆成 {@code a}，按用途还原。
 *
 * <p>注意：{@code com.xtc.database.core.DatabaseHelper} 会反射读取**宿主应用**
 * 的 {@code BuildConfig.VERSION_CODE} 来决定数据库版本，与本类无关。</p>
 */
public final class BuildConfig {
    public static final boolean DEBUG = false;
    public static final String APPLICATION_ID = "com.xtc.database";
    public static final String BUILD_TYPE = "release";
    public static final String FLAVOR = "";
    public static final int VERSION_CODE = 1;
    public static final String VERSION_NAME = "1.0";
    public static final String GitBranch = "origin/master";
    public static final String GitSHA = "d405cfc";
    public static final String GitTag = "database-encrypt-v2.0.6-4-gd405cfc";
    public static final String JenkinsName = "AL_Database";
    public static final String JenkinsRevision = "13";
}
