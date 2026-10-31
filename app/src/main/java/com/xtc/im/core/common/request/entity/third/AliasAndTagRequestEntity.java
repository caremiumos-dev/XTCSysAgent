package com.xtc.im.core.common.request.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.request.entity.RequestEntity;

/**
 * 第三方 alias / tag 设置请求（命令 100）。
 *
 * <p>给指定 registId 的推送设置别名与标签，服务端按 alias/tag 定向下发。</p>
 */
@CommandValue(ImCommand.ALIAS_AND_TAG_REQUEST)
public class AliasAndTagRequestEntity extends RequestEntity {

    @TagValue(1)
    private int rid;

    @TagValue(10)
    private long registId;

    @TagValue(11)
    private String appKey;

    @TagValue(12)
    private String alias;

    @TagValue(13)
    private String tag;

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

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "AliasAndTagRequestEntity{RID=" + this.rid + ", registId=" + this.registId
                + ", appKey='" + this.appKey + "', alias='" + this.alias
                + "', tag='" + this.tag + "'}";
    }
}
