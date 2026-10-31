package com.xtc.bigdata;

/**
 * {@code com.xtc.bigdata} 采集库的构建常量。
 *
 * <p>原厂由 gradle 插件生成，AGP 8 不再为三方包生成 {@code BuildConfig}，
 * 这里手工重建。字段名保持原厂的非常规大小写（{@code GitBranch} 等），
 * 因为日志与上报里直接按字段名取值。</p>
 *
 * <p>注意：该类只承载"构建期"信息，不参与任何业务判断；业务开关在
 * {@link com.xtc.bigdata.common.constants.Constants} 里。</p>
 */
public final class BuildConfig {

    /** 库的 applicationId，用于区分埋点来自哪个包。 */
    public static final String APPLICATION_ID = "com.xtc.bigdata";

    /** 构建类型。 */
    public static final String BUILD_TYPE = "release";

    /** 是否为 debug 构建（注意：与运行时开关 {@code Constants.isDebug} 不是一回事）。 */
    public static final boolean DEBUG = false;

    /** 渠道风味，空串表示无风味。 */
    public static final String FLAVOR = "";

    /** 构建时的 git 分支。 */
    public static final String GitBranch = "origin/master";

    /** 构建时的 git 提交号。 */
    public static final String GitSHA = "d1c2086";

    /** 构建时的 git tag。 */
    public static final String GitTag = "bigdata-client-v2.1.6-2-gd1c2086";

    /** 构建机任务名。 */
    public static final String JenkinsName = "AL_Bigdata_Common";

    /** 构建号。 */
    public static final String JenkinsRevision = "20";

    /** 版本号。 */
    public static final int VERSION_CODE = 1;

    /** 版本名，同时被 {@code Constants.VERSION_NAME} 与
     *  {@code CollectionManager.getBehaviorVersion()} 复用。 */
    public static final String VERSION_NAME = "bigdataClient_d1c2086";

    private BuildConfig() {
    }
}
