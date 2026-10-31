package com.xtc.im.core.common.response.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.response.entity.ResponseEntity;

/**
 * 第三方同步通知（命令 112）：服务端告诉客户端「这个 alias 有变更」，客户端再去拉。
 *
 * <p>原厂没有 toString，字段语义按 ThirdSyncInformRequestEntity 同族推断。</p>
 */
@CommandValue(ImCommand.THIRD_SYNC_INFORM)
public class ThirdSyncInformResponseEntity extends ResponseEntity {

    @TagValue(10)
    private String appKey;

    @TagValue(11)
    private String alias;

    @TagValue(12)
    private long syncKey;

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
