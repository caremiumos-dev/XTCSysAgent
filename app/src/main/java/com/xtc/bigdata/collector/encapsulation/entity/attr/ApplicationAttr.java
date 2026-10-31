package com.xtc.bigdata.collector.encapsulation.entity.attr;

import android.content.ContentValues;

import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.common.db.constant.Columns;

/**
 * 应用属性表。
 *
 * <p>DropBox 扫描时会把这里的包名/版本号替换成"崩溃的第三方应用"的值，
 * 所以 setter 必须返回 {@code this}，方便一行改两个字段。</p>
 *
 * <p>字段名 {@code channleId} 是原厂拼写错误（少一个 n），
 * 对应的 setter 也叫 {@code setchannleId}，调用点按这个名字编译。</p>
 */
public class ApplicationAttr implements IAttr {

    /** 应用 id（包名的 md5/radix 编码）。 */
    private String appId = "";

    /** 应用版本名。 */
    private String appVersion = "";

    /** 包名。 */
    private String packageName = "";

    /** 模块名（一般是应用在桌面上显示的名字）。 */
    private String moduleName = "";

    /** 渠道号。 */
    private String channleId = "";

    @Override
    public IAttr clone() {
        ApplicationAttr applicationAttr = new ApplicationAttr();
        applicationAttr.appId = this.appId;
        applicationAttr.appVersion = this.appVersion;
        applicationAttr.packageName = this.packageName;
        applicationAttr.moduleName = this.moduleName;
        applicationAttr.channleId = this.channleId;
        return applicationAttr;
    }

    /** @return 应用 id */
    public String getAppId() {
        return this.appId;
    }

    @Override
    public void insert(ContentValues contentValues) {
        contentValues.put(Columns.COLUMN_AA_APPID, this.appId);
        contentValues.put(Columns.COLUMN_AA_APPVER, this.appVersion);
        contentValues.put(Columns.COLUMN_AA_PACKAGENAME, this.packageName);
        contentValues.put(Columns.COLUMN_AA_MODULENAME, this.moduleName);
        contentValues.put(Columns.COLUMN_OA_CHANNELID, this.channleId);
    }

    /**
     * 设置应用 id。
     *
     * @param appId 应用 id
     * @return this
     */
    public ApplicationAttr setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * 设置应用版本名。
     *
     * @param appVersion 版本名
     * @return this
     */
    public ApplicationAttr setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    /**
     * 设置模块名。
     *
     * @param moduleName 模块名
     * @return this
     */
    public ApplicationAttr setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    /**
     * 设置包名。
     *
     * @param packageName 包名
     * @return this
     */
    public ApplicationAttr setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    /**
     * 设置渠道号（方法名沿用原厂拼写）。
     *
     * @param channleId 渠道号
     * @return this
     */
    public ApplicationAttr setchannleId(String channleId) {
        this.channleId = channleId;
        return this;
    }

    @Override
    public String toString() {
        return "ApplicationAttr{appId='" + this.appId + "', appVersion='" + this.appVersion
                + "', packageName='" + this.packageName + "', moduleName='" + this.moduleName
                + "', channleId='" + this.channleId + "'}";
    }
}
