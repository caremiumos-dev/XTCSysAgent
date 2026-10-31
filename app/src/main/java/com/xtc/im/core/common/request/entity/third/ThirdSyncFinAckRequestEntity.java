package com.xtc.im.core.common.request.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.request.entity.RequestEntity;

/**
 * 第三方同步结束应答（命令 116）：确认第三方同步已处理完，上报最新 syncKey。
 */
@CommandValue(ImCommand.THIRD_SYNC_FIN_ACK)
public class ThirdSyncFinAckRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long registId;

    @TagValue(11)
    private String appKey;

    @TagValue(12)
    private String alias;

    @TagValue(13)
    private long syncKey;

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

    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public long getSyncKey() {
        return this.syncKey;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    @Override
    public String toString() {
        return "ThirdSyncFinAckRequestEntity{RID=" + this.rid + ", registId=" + this.registId
                + ", appKey='" + this.appKey + "', alias='" + this.alias
                + "', syncKey=" + this.syncKey + '}';
    }
}
