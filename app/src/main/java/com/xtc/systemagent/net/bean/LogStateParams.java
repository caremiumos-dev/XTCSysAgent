package com.xtc.systemagent.net.bean;

/**
 * 日志采集状态上报参数。{@code collectStatus} 的取值见 {@link CollectStatus}。
 */
public class LogStateParams {

    /**
     * 采集状态码。数值是与服务端约定的协议，不能改动。
     */
    public interface CollectStatus {
        /** 已推送过。 */
        int HAD_PUSHED = 0;
        /** 文件上传成功。 */
        int UPLOAD_FILE_SUCCESS = 1;
        /** 文件上传失败。 */
        int UPLOAD_FILE_FAILURE = 2;
        /** 正在监控。 */
        int MONITORING = 3;
        /** 正在抓取。 */
        int FETCHING = 4;
        /** 抓取失败：上一次还没结束。 */
        int FETCH_FAIL_LAST_ONE = 21;
        /** 抓取失败：存储卡不可用。 */
        int FETCH_FAIL_STORAGE_NOT_AVAILABLE = 22;
        /** 抓取失败：空间不足。 */
        int FETCH_FAIL_SPACE_NOT_ENOUGH = 23;
        /** 抓取失败：系统错误。 */
        int FETCH_FAIL_SYSTEM_ERROR = 24;
    }

    private long id;
    private int collectStatus;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCollectStatus() {
        return this.collectStatus;
    }

    public void setCollectStatus(int collectStatus) {
        this.collectStatus = collectStatus;
    }

    @Override
    public String toString() {
        return "LogStateParams{id=" + this.id + ", collectStatus=" + this.collectStatus + '}';
    }
}
