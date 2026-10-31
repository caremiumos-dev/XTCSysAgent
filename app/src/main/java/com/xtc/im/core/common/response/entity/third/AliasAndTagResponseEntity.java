package com.xtc.im.core.common.response.entity.third;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;
import com.xtc.im.core.common.response.entity.ResponseEntity;

/**
 * 第三方 alias / tag 设置响应（命令 101）：回带本次生效的 pkgName。
 */
@CommandValue(ImCommand.ALIAS_AND_TAG_RESPONSE)
public class AliasAndTagResponseEntity extends ResponseEntity {

    @TagValue(1)
    private int rid;

    @TagValue(2)
    private int code;

    @TagValue(3)
    private String desc;

    @TagValue(10)
    private String pkgName;

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

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    @Override
    public String toString() {
        return "AliasAndTagResponseEntity{RID=" + this.rid + ", code=" + this.code
                + ", desc='" + this.desc + "', pkgName='" + this.pkgName + "'}";
    }
}
