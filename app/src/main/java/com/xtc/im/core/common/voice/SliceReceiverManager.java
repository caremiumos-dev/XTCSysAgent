package com.xtc.im.core.common.voice;

import com.xtc.im.core.common.ImLogTag;
import com.xtc.im.core.common.manager.BaseManager;
import com.xtc.log.LogUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音切片接收缓冲的管理器：按 groupId 分发切片、拼装、拼完即清理。
 *
 * <p>继承 {@link BaseManager}，构造时就注册进 {@code ManagerFactory}，
 * 这样 IM 各处都能拿到同一个实例，不用层层传参。</p>
 *
 * <p>原厂类名为单字母 {@code d}，日志 tag 就是 {@code SliceReceiverManager}。</p>
 */
public class SliceReceiverManager extends BaseManager {

    static final String TAG = ImLogTag.tag("SliceReceiverManager");

    /** groupId → 接收缓冲。 */
    private Map<String, SliceReceiver> receivers = new ConcurrentHashMap<String, SliceReceiver>();

    /** 取缓冲，没有则返回 null。 */
    public SliceReceiver getReceiver(String groupId) {
        return this.receivers.get(groupId);
    }

    /** 放入缓冲（同一 groupId 覆盖）。 */
    public void putReceiver(String groupId, SliceReceiver receiver) {
        LogUtil.d(TAG, "put a slice receiver,groupId:" + groupId);
        this.receivers.put(groupId, receiver);
    }

    /**
     * 拼装整段语音。
     *
     * <p>拼成功就把缓冲移除并清空——一段语音只拼一次，留着只会占内存。
     * 拼失败则**保留**缓冲，等下一片重传再试。</p>
     */
    public byte[] buildVoice(String groupId, int lastIndex) {
        byte[] fullVoice;
        SliceReceiver receiver = getReceiver(groupId);
        if (receiver != null) {
            fullVoice = receiver.buildVoice(lastIndex);
        } else {
            LogUtil.e(TAG, "sliceReceiver is null,groupId:" + groupId);
            fullVoice = null;
        }
        if (fullVoice == null || fullVoice.length <= 0) {
            LogUtil.e(TAG, "buildVoice error:fullVoice is null.");
        } else {
            SliceReceiver removed = this.receivers.remove(groupId);
            if (removed != null) {
                removed.clear();
                LogUtil.d(TAG, "remove a slice receiver,groupId:" + groupId);
            }
        }
        return fullVoice;
    }

    /** 取该 groupId 的资源键（末片带的那个）。 */
    public String getResourceKey(String groupId) {
        SliceReceiver receiver = getReceiver(groupId);
        if (receiver != null) {
            return receiver.getResourceKey();
        }
        LogUtil.e(TAG, "sliceReceiver is null,groupId:" + groupId);
        return null;
    }
}
