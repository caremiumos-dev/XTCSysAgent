package com.xtc.bigdata.collector.encapsulation.entity.attr;

import android.content.ContentValues;

import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.common.db.constant.Columns;

/**
 * 机器属性表。
 *
 * <p>{@code rooted} 用 int 而不是 boolean：数据库里这一列是整型，
 * 服务端也按 0/1 解析。</p>
 */
public class MachineAttr implements IAttr {

    /** 机器 id。 */
    private String mId = "";

    /** 设备名（机型）。 */
    private String devName = "";

    /** 内部型号。 */
    private String innerModel = "";

    /** 系统版本。 */
    private String osVer = "";

    /** 品牌。 */
    private String brand = "";

    /** 是否 root，0/1。 */
    private int rooted = 0;

    @Override
    public IAttr clone() {
        MachineAttr machineAttr = new MachineAttr();
        machineAttr.brand = this.brand;
        machineAttr.devName = this.devName;
        machineAttr.innerModel = this.innerModel;
        machineAttr.mId = this.mId;
        machineAttr.osVer = this.osVer;
        machineAttr.rooted = this.rooted;
        return machineAttr;
    }

    /** @return 是否 root，0/1 */
    public int getRooted() {
        return this.rooted;
    }

    @Override
    public void insert(ContentValues contentValues) {
        contentValues.put(Columns.COLUMN_MA_MID, this.mId);
        contentValues.put(Columns.COLUMN_MA_OSVERSION, this.osVer);
        contentValues.put(Columns.COLUMN_MA_DEVICE, this.devName);
        contentValues.put(Columns.COLUMN_INNER_MODEL, this.innerModel);
        contentValues.put(Columns.COLUMN_MA_BRAND, this.brand);
        contentValues.put(Columns.COLUMN_ROOTED, Integer.valueOf(this.rooted));
    }

    /**
     * 设置品牌。
     *
     * @param brand 品牌
     * @return this
     */
    public MachineAttr setBrand(String brand) {
        this.brand = brand;
        return this;
    }

    /**
     * 设置设备名。
     *
     * @param devName 设备名
     * @return this
     */
    public MachineAttr setDevName(String devName) {
        this.devName = devName;
        return this;
    }

    /**
     * 设置内部型号。
     *
     * @param innerModel 内部型号
     * @return this
     */
    public MachineAttr setInnerModel(String innerModel) {
        this.innerModel = innerModel;
        return this;
    }

    /**
     * 设置系统版本（方法名是 {@code setOsVersion}，字段是 {@code osVer}）。
     *
     * @param osVersion 系统版本
     * @return this
     */
    public MachineAttr setOsVersion(String osVersion) {
        this.osVer = osVersion;
        return this;
    }

    /**
     * 设置 root 标记。
     *
     * @param rooted 0/1
     * @return this
     */
    public MachineAttr setRooted(int rooted) {
        this.rooted = rooted;
        return this;
    }

    /**
     * 设置机器 id（方法名沿用原厂的 {@code setmId}）。
     *
     * @param mId 机器 id
     * @return this
     */
    public MachineAttr setmId(String mId) {
        this.mId = mId;
        return this;
    }
}
