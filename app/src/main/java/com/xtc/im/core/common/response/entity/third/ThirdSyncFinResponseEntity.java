package com.xtc.im.core.common.response.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.response.entity.ResponseEntity;

/**
 * 第三方同步结束（命令 115）：服务端通知该 alias 的同步已推完。
 *
 * <p>原厂没有 toString，字段语义按同族实体（ThirdSyncFinAckRequestEntity /
 * ThirdSyncInformResponseEntity）的 tag 序列对齐推断。</p>
 */
@CommandValue(ImCommand.THIRD_SYNC_FIN)
public class ThirdSyncFinResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private String appKey;

    @TagValue(11)
    private String alias;

    @TagValue(12)
    private long syncKey;

    @Override
    public int getRid() {
        return this.rid;
    }

    @Override
    public void setRid(int rid) {
        this.rid = rid;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    public void setDesc(String desc) {
        this.desc = desc;
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
}
