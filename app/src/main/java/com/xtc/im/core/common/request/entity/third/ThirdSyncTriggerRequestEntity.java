package com.xtc.im.core.common.request.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.request.entity.RequestEntity;

/**
 * 第三方触发同步请求（命令 110）：请服务端主动推一次该 alias 的变更。
 */
@CommandValue(ImCommand.THIRD_SYNC_TRIGGER_REQUEST)
public class ThirdSyncTriggerRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long registId;

    @TagValue(11)
    private String appKey;

    @TagValue(12)
    private String alias;

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

    @Override
    public String toString() {
        return "ThirdSyncTriggerRequestEntity{RID=" + this.rid + ", registId=" + this.registId
                + ", appKey='" + this.appKey + "', alias='" + this.alias + "'}";
    }
}
