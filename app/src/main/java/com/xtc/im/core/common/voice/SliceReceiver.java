package com.xtc.im.core.common.voice;

import com.xtc.im.core.common.ImLogTag;
import com.xtc.im.core.common.voice.entity.VoiceSliceEntity;
import com.xtc.log.LogUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * 一段语音的接收缓冲：按 groupId 攒切片，攒齐后拼回整段。
 *
 * <p>「齐了」的判据是「收到的片数 == 末片下标」，而不是「收到标了 isFin 的那片」——
 * 因为末片可能先到、也可能丢失后重传，按数量判断更稳。</p>
 *
 * <p>超过 3 分钟没攒齐就认为过期（{@link #isExpired()}），由管理器负责清理，
 * 否则一条丢失的切片会让这块内存一直挂着。</p>
 *
 * <p>原厂类名为单字母 {@code c}，toString 里已经写着 {@code SliceReceiver}。</p>
 */
public class SliceReceiver {

    static final String TAG = ImLogTag.tag("SliceReceiver");

    /** 过期阈值：3 分钟。 */
    private static final long EXPIRE_TIME = 180000L;

    /** 账号 id（toString 里叫 accountId）。 */
    private Long accountId;

    /** 注测 id。 */
    private Long registId;

    /** 第 index 片。 */
    private Map<Integer, VoiceSliceEntity> slices = new HashMap<Integer, VoiceSliceEntity>();

    /** 末片携带的资源键，攒齐后用它去取文件。 */
    private String resourceKey;

    /** 创建时刻，用于过期判断。 */
    private long createTime = System.currentTimeMillis();

    public SliceReceiver(Long accountId, Long registId) {
        this.accountId = accountId;
        this.registId = registId;
    }

    /**
     * 清空缓冲（拼装完成后由管理器调用）。
     */
    public void clear() {
        this.slices.clear();
        this.resourceKey = null;
    }

    /**
     * 收下一片。
     *
     * <p>末片（isFin = 2）会带上资源键，先把它记下来再入表——
     * 顺序很重要：拼装时依赖这个键，晚一步就会在拼完那一刻取不到。</p>
     */
    public void addSlice(VoiceSliceEntity slice) {
        LogUtil.d(TAG, "add a voice slice entity:" + slice);
        if (slice.isFin() == VoiceSliceEntity.SliceType.TYPE_LAST) {
            String key = slice.getExtra();
            LogUtil.d(TAG, "voice slice entity: " + key);
            this.resourceKey = key;
        }
        this.slices.put(Integer.valueOf(slice.getIndex()), slice);
    }

    /**
     * 拼出整段语音。
     *
     * @param lastIndex 末片下标（服务端告知）
     * @return 拼好返回 byte[]，缺片或参数非法返回 null
     */
    protected byte[] buildVoice(int lastIndex) {
        byte[] fullVoice = null;
        if (lastIndex >= 1) {
            if (this.slices.size() == lastIndex) {
                byte[] data = this.slices.get(1).getVoc();
                for (int index = 2; index <= lastIndex; index++) {
                    data = concat(data, this.slices.get(Integer.valueOf(index)).getVoc());
                }
                fullVoice = data;
            }
            if (fullVoice == null || fullVoice.length == 0) {
                LogUtil.e(TAG, "receiver info:" + toString() + ",lastIndex:" + lastIndex);
            }
        }
        return fullVoice;
    }

    /**
     * 拼接两段字节；任一段为空返回 null（上层据此判定拼装失败）。
     */
    public byte[] concat(byte[] first, byte[] second) {
        if (first == null || first.length == 0 || second == null || second.length == 0) {
            return null;
        }
        byte[] merged = new byte[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    /** 是否已过期（超过 3 分钟）。 */
    public boolean isExpired() {
        return System.currentTimeMillis() - this.createTime > EXPIRE_TIME;
    }

    /** 末片携带的资源键。 */
    public String getResourceKey() {
        return this.resourceKey;
    }

    @Override
    public String toString() {
        return "SliceReceiver{accountId=" + this.accountId + ", registId=" + this.registId + '}';
    }
}
