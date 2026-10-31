package com.xtc.utils.ui;

/**
 * UI 工具库的构建信息（原厂 Gradle 生成，混淆后类名成了 {@code b}）。
 *
 * <p>这些字段只在构建期写入，运行期被日志、崩溃上报、灰度开关读取，
 * 值是“这个 aar 是哪次构建产出的”唯一凭证，所以必须与 APK 内的字符串逐字一致，
 * 不能用当前工程的构建参数重新生成。</p>
 */
public final class BuildConfig {

    /** 调试开关：原厂发布包恒为 false。 */
    public static final boolean DEBUG = false;

    /** 库自身的 applicationId，用来区分是哪一个 XTC 工具库。 */
    public static final String APPLICATION_ID = "com.xtc.utils.ui";

    /** 构建类型。 */
    public static final String BUILD_TYPE = "release";

    /** 渠道名：该库没有配置渠道，为空串。 */
    public static final String FLAVOR = "";

    /** 版本号。 */
    public static final int VERSION_CODE = 1;

    /** 版本名。 */
    public static final String VERSION_NAME = "1.0.0";

    /** 构建时所在的 git 分支。 */
    public static final String GitBranch = "origin/master";

    /** 构建时所在的 git 提交短 SHA。 */
    public static final String GitSHA = "70b4f10";

    /** 构建时所在的 git tag（utils-ui 系列）。 */
    public static final String GitTag = "utils-ui-v2.1.0-4-g70b4f10";

    /** 构建所使用的 Jenkins 任务名，原厂为 AL_Utils_UI。 */
    public static final String JenkinsName = "AL_Utils_UI";

    /** Jenkins 构建序号。 */
    public static final String JenkinsRevision = "22";
}
