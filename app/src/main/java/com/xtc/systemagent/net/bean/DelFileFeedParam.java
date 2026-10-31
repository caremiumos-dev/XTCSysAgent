package com.xtc.systemagent.net.bean;

import java.util.List;

/**
 * "删除手表文件"指令的回执参数：成功/失败清单与绑定号。
 */
public class DelFileFeedParam {

    private String bindnumber;
    private int id;
    private List<String> failList;

    public String getBindnumber() {
        return this.bindnumber;
    }

    public void setBindnumber(String bindnumber) {
        this.bindnumber = bindnumber;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getFailList() {
        return this.failList;
    }

    public void setFailList(List<String> failList) {
        this.failList = failList;
    }
}
