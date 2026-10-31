package com.xtc.bigdata.collector.encapsulation.entity.attr;

import android.content.ContentValues;

import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.common.db.constant.Columns;

/**
 * "其它"属性表：会话、扩展、imei、路由器 mac、状态。
 *
 * <p>{@code extend} 与 {@code extendJudgment} 是成对的：
 * extend 存业务扩展 JSON，extendJudgment 存"这条数据有哪些附加判定"
 * （例如 {@code |1|} 表示走了移动数据流量）。</p>
 *
 * <p>{@code status} 恒写 0，是 provider 侧的"未处理"标记。</p>
 */
public class OtherAttr implements IAttr {

    /** 会话 id。 */
    private String sessionid;

    /** 扩展 JSON。 */
    private String extend = "";

    /** 数据采集版本号。 */
    private String daVer;

    /** 路由器 mac。 */
    private String routerMac;

    /** 扩展判定串。 */
    private String extendJudgment;

    /** imei。 */
    private String imei;

    @Override
    public IAttr clone() {
        OtherAttr otherAttr = new OtherAttr();
        otherAttr.daVer = this.daVer;
        otherAttr.extend = this.extend;
        otherAttr.extendJudgment = this.extendJudgment;
        otherAttr.imei = this.imei;
        otherAttr.routerMac = this.routerMac;
        otherAttr.sessionid = this.sessionid;
        return otherAttr;
    }

    /** @return 数据采集版本号 */
    public String getDaVer() {
        return this.daVer;
    }

    /** @return 扩展 JSON */
    public String getExtend() {
        return this.extend;
    }

    /** @return 扩展判定串 */
    public String getExtendJudgment() {
        return this.extendJudgment;
    }

    /** @return imei */
    public String getImei() {
        return this.imei;
    }

    /** @return 路由器 mac */
    public String getRouterMac() {
        return this.routerMac;
    }

    @Override
    public void insert(ContentValues contentValues) {
        contentValues.put(Columns.COLUMN_EA_SESSIONID, this.sessionid);
        contentValues.put(Columns.COLUMN_OA_EXTEND, this.extend);
        contentValues.put(Columns.COLUMN_OA_DAVER, this.daVer);
        contentValues.put(Columns.COLUMN_OA_ROUTERMAC, this.routerMac);
        contentValues.put(Columns.COLUMN_OA_EXTENDJUDGMENT, this.extendJudgment);
        contentValues.put(Columns.COLUMN_OA_IMEI, this.imei);
        contentValues.put(Columns.COLUMN_OA_STATUS, Integer.valueOf(0));
    }

    /**
     * 设置数据采集版本号。
     *
     * @param daVer 版本号
     */
    public void setDaVer(String daVer) {
        this.daVer = daVer;
    }

    /**
     * 设置扩展 JSON。
     *
     * @param extend 扩展 JSON
     * @return this
     */
    public OtherAttr setExtend(String extend) {
        this.extend = extend;
        return this;
    }

    /**
     * 设置扩展判定串。
     *
     * @param extendJudgment 判定串
     */
    public void setExtendJudgment(String extendJudgment) {
        this.extendJudgment = extendJudgment;
    }

    /**
     * 设置 imei。
     *
     * @param imei imei
     */
    public void setImei(String imei) {
        this.imei = imei;
    }

    /**
     * 设置路由器 mac。
     *
     * @param routerMac mac
     */
    public void setRouterMac(String routerMac) {
        this.routerMac = routerMac;
    }

    /**
     * 设置会话 id。
     *
     * @param sessionid 会话 id
     * @return this
     */
    public OtherAttr setSessionid(String sessionid) {
        this.sessionid = sessionid;
        return this;
    }
}
