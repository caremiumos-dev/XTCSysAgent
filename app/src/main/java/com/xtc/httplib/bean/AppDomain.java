package com.xtc.httplib.bean;

/**
 * 一套业务域名集合（服务端下发的域名配置里的 {@code appDomain} 节点）。
 *
 * <p>由 Gson 直接反序列化，字段名必须与 JSON 的 key 完全一致，所以这里不做任何改名。
 * 空的字段表示"这次下发的配置没带这一项"，{@code BaseUrlManager} 会在使用时
 * 直接拼进 URL，不做兜底。</p>
 */
public class AppDomain {

    /** 活动/运营域名。 */
    private String activityDomain;
    /** 聊天域名。 */
    private String chatDomain;
    /** 游戏域名。 */
    private String gameDomain;
    /** 网关域名。 */
    private String gatewayDomain;
    /** 定位域名。 */
    private String locationDomain;
    /** 动态（朋友圈）域名。 */
    private String momentDomain;
    /** 积分商城域名。 */
    private String pointsDomain;
    /** 运动域名。 */
    private String sportDomain;
    /** 应用商店域名。 */
    private String storeDomain;
    /** 表盘主题域名。 */
    private String themeDomain;
    /** 第三方域名。 */
    private String thirdPartyDomain;
    /** 主域名。 */
    private String watchDomain;
    /** 天气域名。 */
    private String weatherDomain;

    public String getActivityDomain() {
        return this.activityDomain;
    }

    public String getChatDomain() {
        return this.chatDomain;
    }

    public String getGameDomain() {
        return this.gameDomain;
    }

    public String getGatewayDomain() {
        return this.gatewayDomain;
    }

    public String getLocationDomain() {
        return this.locationDomain;
    }

    public String getMomentDomain() {
        return this.momentDomain;
    }

    public String getPointsDomain() {
        return this.pointsDomain;
    }

    public String getSportDomain() {
        return this.sportDomain;
    }

    public String getStoreDomain() {
        return this.storeDomain;
    }

    public String getThemeDomain() {
        return this.themeDomain;
    }

    public String getThirdPartyDomain() {
        return this.thirdPartyDomain;
    }

    public String getWatchDomain() {
        return this.watchDomain;
    }

    public String getWeatherDomain() {
        return this.weatherDomain;
    }

    public void setActivityDomain(String activityDomain) {
        this.activityDomain = activityDomain;
    }

    public void setChatDomain(String chatDomain) {
        this.chatDomain = chatDomain;
    }

    public void setGameDomain(String gameDomain) {
        this.gameDomain = gameDomain;
    }

    public void setGatewayDomain(String gatewayDomain) {
        this.gatewayDomain = gatewayDomain;
    }

    public void setLocationDomain(String locationDomain) {
        this.locationDomain = locationDomain;
    }

    public void setMomentDomain(String momentDomain) {
        this.momentDomain = momentDomain;
    }

    public void setPointsDomain(String pointsDomain) {
        this.pointsDomain = pointsDomain;
    }

    public void setSportDomain(String sportDomain) {
        this.sportDomain = sportDomain;
    }

    public void setStoreDomain(String storeDomain) {
        this.storeDomain = storeDomain;
    }

    public void setThemeDomain(String themeDomain) {
        this.themeDomain = themeDomain;
    }

    public void setThirdPartyDomain(String thirdPartyDomain) {
        this.thirdPartyDomain = thirdPartyDomain;
    }

    public void setWatchDomain(String watchDomain) {
        this.watchDomain = watchDomain;
    }

    public void setWeatherDomain(String weatherDomain) {
        this.weatherDomain = weatherDomain;
    }

    /**
     * 字段顺序与拼接格式被日志/问题定位直接消费，保持原样（包括逗号与引号的位置）。
     */
    @Override
    public String toString() {
        return "AppDomain{watchDomain='" + this.watchDomain
                + "', chatDomain='" + this.chatDomain
                + "', locationDomain='" + this.locationDomain
                + "', pointsDomain='" + this.pointsDomain
                + "', activityDomain='" + this.activityDomain
                + "', sportDomain='" + this.sportDomain
                + "', weatherDomain='" + this.weatherDomain
                + "', storeDomain='" + this.storeDomain
                + "', thirdPartyDomain='" + this.thirdPartyDomain
                + "', gameDomain='" + this.gameDomain
                + "', themeDomain='" + this.themeDomain
                + "', momentDomain='" + this.momentDomain
                + "', gatewayDomain='" + this.gatewayDomain + "'}";
    }
}
