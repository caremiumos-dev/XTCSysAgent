package com.xtc.im.core.common.response.entity;

import com.xtc.im.core.common.anotation.CommandValue;
import com.xtc.im.core.common.anotation.TagValue;
import com.xtc.im.core.common.request.ImCommand;

/**
 * 心跳响应（命令 8）：服务端把自己的状态带回来。
 *
 * <p>状态变化去重靠一个**静态**字段：同一个进程里所有实例共享上一次的状态，
 * 所以第一次一定返回 true（强制上层处理一次），之后只有真的变化了才返回 true。</p>
 */
@CommandValue(ImCommand.HEART_BEAT_RESPONSE)
public class HeartBeatResponseEntity extends ResponseEntity {

    @TagValue(10)
    private int serverStatus;

    public int getServerStatus() {
        return this.serverStatus;
    }

    public void setServerStatus(int serverStatus) {
        this.serverStatus = serverStatus;
    }

    /** 服务端状态：正常。 */
    public static final int SERVER_STATUS_NORMAL = 0;

    /** 服务端状态：异常。 */
    public static final int SERVER_STATUS_ABNORMAL = 1;

    /** 上一次上报给上层的状态（static：跨实例共享）。 */
    public static int lastServerStatus = 0;

    /**
     * 服务端状态是否变化。
     *
     * <p>判据按原厂写：上一次不是异常、且和这次相同 → 不算变化；
     * 其余情况都算变化并记住这次的值。也就是说从异常恢复时一定会回调一次。</p>
     */
    public boolean isServerStatusChanged() {
        if (lastServerStatus != SERVER_STATUS_ABNORMAL && lastServerStatus == this.serverStatus) {
            return false;
        }
        lastServerStatus = this.serverStatus;
        return true;
    }

    @Override
    public String toString() {
        return "HeartBeatResponseEntity{serverStatus=" + this.serverStatus + '}';
    }
}
