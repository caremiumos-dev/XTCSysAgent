package com.xtc.bigdata.collector.exception;

/**
 * 上一次崩溃的指纹。
 *
 * <p>存进 SharedPreferences（{@code crashInfo}）的是它的 JSON 形式。
 * 崩溃去重靠两点：堆栈 MD5 相同，且距上次记录不超过 5 秒。</p>
 */
public class CrashInfo {

    /** 堆栈的 MD5。 */
    private String errorMd5;

    /** 记录时间（毫秒时间戳）。 */
    private long updateTime;

    /** @return 堆栈 MD5 */
    public String getErrorMd5() {
        return this.errorMd5;
    }

    /** @return 记录时间 */
    public long getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 设置堆栈 MD5。
     *
     * @param errorMd5 堆栈 MD5
     */
    public void setErrorMd5(String errorMd5) {
        this.errorMd5 = errorMd5;
    }

    /**
     * 设置记录时间。
     *
     * @param updateTime 毫秒时间戳
     */
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "CrashInfo{, errorMd5='" + this.errorMd5 + "', updateTime=" + this.updateTime + '}';
    }
}
