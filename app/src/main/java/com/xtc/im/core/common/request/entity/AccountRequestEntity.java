package com.xtc.im.core.common.request.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 业务账号绑定请求（命令 5）。
 *
 * <p>一个 IM 账号下可以挂多个业务身份（businessId），用 businessToken 证明归属。
 * 类里两个常量是原厂留下的业务类型取值，没有语义线索，按取值命名。</p>
 */
@CommandValue(ImCommand.ACCOUNT_REQUEST)
public class AccountRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long registId;

    @TagValue(11)
    private int businessType;

    @TagValue(12)
    private String businessId;

    @TagValue(13)
    private String businessToken;

    @TagValue(14)
    private int status;

    @TagValue(15)
    private String appKey;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    public long getRegistId() {
        return this.registId;
    }

    public void setRegistId(long registId) {
        this.registId = registId;
    }

    public int getBusinessType() {
        return this.businessType;
    }

    public void setBusinessType(int businessType) {
        this.businessType = businessType;
    }

    public String getBusinessId() {
        return this.businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessToken() {
        return this.businessToken;
    }

    public void setBusinessToken(String businessToken) {
        this.businessToken = businessToken;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    /** 业务类型 1（原厂常量，无语义线索，按取值命名）。 */
    public static final int BUSINESS_TYPE_1 = 1;

    /** 业务类型 2（原厂常量，无语义线索，按取值命名）。 */
    public static final int BUSINESS_TYPE_2 = 2;

    @Override
    public String toString() {
        return "AccountRequestEntity{RID=" + this.rid + ", registId=" + this.registId
                + ", businessType=" + this.businessType + ", businessId='" + this.businessId
                + "', businessToken='" + this.businessToken + "', status=" + this.status
                + ", appKey='" + this.appKey + "'}";
    }
}
