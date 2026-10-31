package com.xtc.system.account.bean;

/**
 * 好友排行榜的一项。
 *
 * <p>三个计数字段在构造器里都先置 0，防止服务端漏字段时出现 null 拆箱崩溃。</p>
 */
public class NetFriendRank {

    private String name;
    private Integer step;
    private Integer thumbupCount;
    private Integer thumbuped;
    private String watchId;

    public NetFriendRank() {
        this.step = 0;
        this.thumbuped = 0;
        this.thumbupCount = 0;
    }

    public NetFriendRank(String name, String watchId, Integer step, Integer thumbuped,
                         Integer thumbupCount) {
        this.step = 0;
        this.thumbuped = 0;
        this.thumbupCount = 0;
        this.name = name;
        this.watchId = watchId;
        this.step = step;
        this.thumbuped = thumbuped;
        this.thumbupCount = thumbupCount;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStep() {
        return this.step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public Integer getThumbupCount() {
        return this.thumbupCount;
    }

    public void setThumbupCount(Integer thumbupCount) {
        this.thumbupCount = thumbupCount;
    }

    public Integer getThumbuped() {
        return this.thumbuped;
    }

    public void setThumbuped(Integer thumbuped) {
        this.thumbuped = thumbuped;
    }

    public String getWatchId() {
        return this.watchId;
    }

    public void setWatchId(String watchId) {
        this.watchId = watchId;
    }

    @Override
    public String toString() {
        return "NetFriendRank{name='" + this.name + '\''
                + ", watchId='" + this.watchId + '\''
                + ", step=" + this.step
                + ", thumbuped=" + this.thumbuped
                + ", thumbupCount=" + this.thumbupCount + '}';
    }
}
