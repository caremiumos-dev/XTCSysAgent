package com.xtc.utils.storage;

/**
 * 存储工具库的构建信息。
 *
 * <p>原厂由 Gradle 在打包时生成，混淆后类名成了 {@code a}、字段名也退化成单字母；
 * 这里按字段含义还原成 {@code BuildConfig}。字段值本身（applicationId、git 分支/SHA、
 * Jenkins 任务号）会原样进入日志与崩溃上报，是排查线上问题的唯一线索，
 * 因此数值必须与 APK 内的完全一致，不能用当前工程的构建参数重新生成。</p>
 */
public final class BuildConfig {

    /** 调试开关：发布包恒为 false。 */
    public static final boolean DEBUG = false;

    /** 库自身的 applicationId，用来区分是哪一个 XTC 工具库的构建。 */
    public static final String APPLICATION_ID = "com.xtc.utils.storage";

    /** 构建类型：release。 */
    public static final String BUILD_TYPE = "release";

    /** 渠道名：该库没有配置渠道，为空串。 */
    public static final String FLAVOR = "";

    /** 版本号。 */
    public static final int VERSION_CODE = 1;

    /** 版本名。 */
    public static final String VERSION_NAME = "1.0.0";

    /** 打包时所在的 git 分支。 */
    public static final String GitBranch = "dev_storage";

    /** 打包时所在的 git 提交短 SHA。 */
    public static final String GitSHA = "fe6ca63";

    /** 打包时所在的 git tag（utils-system 系列的 52 号提交）。 */
    public static final String GitTag = "utils-system-v2.6.1-52-gfe6ca63";

    /** 打包所使用的 Jenkins 节点/任务别名，本地构建为 Local。 */
    public static final String JenkinsName = "Local";

    /** Jenkins 构建序号，本地构建为 0。 */
    public static final String JenkinsRevision = "0";
}
