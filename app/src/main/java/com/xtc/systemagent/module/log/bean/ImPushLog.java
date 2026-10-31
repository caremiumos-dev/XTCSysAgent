package com.xtc.systemagent.module.log.bean;

/**
 * IM 推送的日志抓取指令体。
 */
public class ImPushLog {

    /**
     * 推送子类型。
     */
    public interface Type {
        /** 抓取 xtc 黑客日志。 */
        int XTC_HACKER = 100;
        /** 抓取 xtc bug 报告。 */
        int LOG_HACKER_XTC_BUG_REPORT = 101;
    }

    private Integer type;
    private String content;

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ImPushLog{type=" + this.type + ", content='" + this.content + "'}";
    }
}
