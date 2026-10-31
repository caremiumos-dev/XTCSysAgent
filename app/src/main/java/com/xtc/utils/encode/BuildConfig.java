package com.xtc.utils.encode;

/**
 * 编码工具库的构建信息。
 *
 * <p>原厂的这个类被混淆成了单字母 {@code c}，内容其实是 Gradle 在打包时自动生成的
 * BuildConfig，这里按发布版本把取值固定下来。这些常量只用于日志/上报里的版本标识，
 * 数值必须与原厂 APK 一致，否则服务端按版本做的灰度判断会跟着变。</p>
 */
public final class BuildConfig {

    /** 是否为调试构建。原厂发布 APK 里是 false。 */
    public static final boolean DEBUG = false;
    /** 库的 applicationId。 */
    public static final String APPLICATION_ID = "com.xtc.utils.encode";
    /**
     * 包名的旧别名，与 {@link #APPLICATION_ID} 同值。
     * 原厂已经标了 @Deprecated 但一直没删，这里保留以免依赖方引用不到。
     */
    @Deprecated
    public static final String PACKAGE_NAME = "com.xtc.utils.encode";
    /** 构建类型，发布包固定为 release。 */
    public static final String BUILD_TYPE = "release";
    /** 渠道名，这个库没有分渠道，原厂就是空串。 */
    public static final String FLAVOR = "";
    /** 版本号。 */
    public static final int VERSION_CODE = 1;
    /** 版本名。 */
    public static final String VERSION_NAME = "1.0.0";
    /** 打包时的 git 分支。 */
    public static final String GitBranch = "origin/master";
    /** 打包时的 git commit 短哈希。 */
    public static final String GitSHA = "2eaa78f";
    /** git describe 出来的 tag。 */
    public static final String GitTag = "v2.12.17_utils-system-4-g2eaa78f";
    /** 打包机（Jenkins）任务名。 */
    public static final String JenkinsName = "AL_Utils";
    /** Jenkins 构建号。 */
    public static final String JenkinsRevision = "50";
}
