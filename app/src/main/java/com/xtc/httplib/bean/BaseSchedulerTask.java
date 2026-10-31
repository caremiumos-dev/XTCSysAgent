package com.xtc.httplib.bean;

/**
 * 一个"配置项延迟通知"的调度任务。
 *
 * <p>服务端会在响应头里下发 {@code Conf-Updates}（哪些配置项变了，二进制位串）和
 * {@code Conf-Updates-V}（各配置项的版本号 + 延迟分钟数）。{@code ConfUpdatesInfoManager}
 * 为每个配置项建一个本对象，等到 {@code delayTime} 之后才向客户端广播，
 * 避免多个配置项在同一时刻涌进来。</p>
 *
 * <p>{@code setTime} 与 {@code delayTime} 的相对基准是 {@code SystemClock.elapsedRealtime()}
 * （开机时长），不是墙钟时间——这样即使用户改时间也不会把延迟拉长或缩短。</p>
 */
public class BaseSchedulerTask {

    /** 配置项编号。 */
    private int index;
    /** 该配置项的版本号，只有新版本才触发通知。 */
    private int version;
    /** 任务创建/最近一次更新的时间戳（elapsedRealtime）。 */
    private long setTime;
    /** 需要等待的延迟时长（毫秒）。 */
    private long delayTime;
    /** 是否还等着派发；派发或判定过期后置 false。 */
    private boolean toDispatch;

    /**
     * 工厂方法：建一个"待派发"的任务。
     *
     * @param index     配置项编号
     * @param version   版本号
     * @param setTime   设置时间（elapsedRealtime）
     * @param delayTime 延迟时长（毫秒）
     */
    public static BaseSchedulerTask createTask(int index, int version, long setTime, long delayTime) {
        BaseSchedulerTask task = new BaseSchedulerTask();
        task.setIndex(index);
        task.setVersion(version);
        task.setSetTime(setTime);
        task.setDelayTime(delayTime);
        task.setToDispatch(false);
        return task;
    }

    public long getDelayTime() {
        return this.delayTime;
    }

    public int getIndex() {
        return this.index;
    }

    public long getSetTime() {
        return this.setTime;
    }

    public int getVersion() {
        return this.version;
    }

    public boolean isToDispatch() {
        return this.toDispatch;
    }

    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setSetTime(long setTime) {
        this.setTime = setTime;
    }

    public void setToDispatch(boolean toDispatch) {
        this.toDispatch = toDispatch;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "BaseSchedulerTask{index=" + this.index
                + ", version=" + this.version
                + ", setTime=" + this.setTime
                + ", delayTime=" + this.delayTime
                + ", toDispatch=" + this.toDispatch + '}';
    }

    /** 收到更新的版本号时刷新任务：重置计时并重新标记为待派发。 */
    public void updateTask(int version, long setTime, long delayTime, boolean toDispatch) {
        setVersion(version);
        setSetTime(setTime);
        setDelayTime(delayTime);
        setToDispatch(toDispatch);
    }
}
