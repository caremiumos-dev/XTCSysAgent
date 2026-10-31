package com.xtc.httplib.confupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.bean.BaseSchedulerTask;
import com.xtc.log.LogUtil;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 服务端"配置变更通知"的解析与派发中心（进程内单例）。
 *
 * <p>协议：每个响应都可能带两个头——</p>
 * <ul>
 *   <li>{@code Conf-Updates}：一个二进制位串（字符串形式），从低位起第 {@code n} 位
 *       为 1 表示"编号 n+1 的配置项变了"；</li>
 *   <li>{@code Conf-Updates-V}：逗号分隔的版本号列表，最后一项是"延迟分钟数"，
 *       前面依次对应各配置项的版本号。</li>
 * </ul>
 *
 * <p>收到之后不是立刻广播，而是为每个配置项建一个 {@link BaseSchedulerTask}：
 * 只有等到延迟时间之后才广播，避免一次下发把客户端打醒多次；
 * 而且只有版本号比本地新才重新计时（防止旧响应把新版本顶掉）。</p>
 *
 * <p>单例用 {@link AtomicReference} + CAS 自旋而不是 {@code synchronized}：
 * 取单例发生在网络线程，路径要极短。</p>
 */
public class ConfUpdatesInfoManager {

    private static final String TAG = LogTag.tag("ConfUpdatesInfoManager");

    /** 单例。CAS 保证只创建一次。 */
    private static final AtomicReference<ConfUpdatesInfoManager> INSTANCE =
            new AtomicReference<ConfUpdatesInfoManager>();

    /** 需要关注的配置项编号表，由宿主通过 {@link #putConfInfo(int...)} 注入。 */
    private static int[] confInfos;

    /**
     * {@code Intent#FLAG_RECEIVER_INCLUDE_BACKGROUND}(0x01000000)。
     *
     * <p>原厂没有引用 {@code Intent} 里的常量，而是自己存了一份实例字段（因此也不是
     * static）。广播要送到后台的接收者，必须带这个 flag。</p>
     */
    private int FLAG_RECEIVER_INCLUDE_BACKGROUND = 16777216;

    private Context context;

    /** 配置项编号 -> 调度任务。 */
    private ArrayMap<Integer, BaseSchedulerTask> schedulerTaskArray =
            new ArrayMap<Integer, BaseSchedulerTask>();

    public ConfUpdatesInfoManager(Context context) {
        this.context = context;
    }

    /**
     * 取单例。
     *
     * <p>注意入参 context 只用于第一次创建，之后传什么都返回同一个实例——
     * 也就意味着后传的 context 会被忽略（原厂行为）。</p>
     */
    public static ConfUpdatesInfoManager getInstance(Context context) {
        ConfUpdatesInfoManager manager;
        do {
            manager = INSTANCE.get();
            if (manager != null) {
                break;
            }
            manager = new ConfUpdatesInfoManager(context);
        } while (!INSTANCE.compareAndSet(null, manager));
        return manager;
    }

    /**
     * 向本应用广播"某个配置项变了"。
     *
     * @param context 上下文，为 null 时记日志并返回 false
     * @param confId  配置项编号，拼进 data 的 scheme 后面
     * @return 是否发出
     */
    private boolean sendBroadcast(Context context, int confId) {
        if (context == null) {
            LogUtil.d(TAG, "sendBroadcast: context == null");
            return false;
        }
        Intent intent = new Intent(HttpConfUpdate.ACTION_HTTP_CONF_UPDATE,
                Uri.parse(HttpConfUpdate.SCHEME_HTTP_CONF_UPDATE + confId));
        intent.addFlags(this.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        String packageName = context.getPackageName();
        LogUtil.d(TAG, "sendBroadcast: packageName = " + packageName);
        intent.setPackage(packageName);
        context.sendBroadcast(intent);
        return true;
    }

    /**
     * 扫描所有待派发任务，把"已经过了延迟时间"的广播出去。
     *
     * <p>三种情况：</p>
     * <ul>
     *   <li>偏移量大于延迟时长 → 置为已派发并广播；</li>
     *   <li>偏移量小于 0（用户把时钟往回改过）→ 直接置为已派发但**不广播**，
     *       避免"时钟回拨导致无限补发"；</li>
     *   <li>其它情况 → 继续等下一次响应触发本方法。</li>
     * </ul>
     */
    public void dispathUpdatesToClient() {
        Set<Map.Entry<Integer, BaseSchedulerTask>> entries = this.schedulerTaskArray.entrySet();
        if (entries.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, BaseSchedulerTask> entry : entries) {
            BaseSchedulerTask task = entry.getValue();
            if (!task.isToDispatch()) {
                continue;
            }
            long offsetTime = SystemClock.elapsedRealtime() - task.getSetTime();
            LogUtil.d(TAG, "dispathUpdatesToClient: offsetTime = " + offsetTime + "; task" + task);
            if (offsetTime > task.getDelayTime()) {
                task.setToDispatch(false);
                sendBroadcast(this.context, entry.getKey().intValue());
            } else if (offsetTime < 0) {
                task.setToDispatch(false);
            }
        }
    }

    /**
     * 注入"需要关注的配置项编号表"。
     *
     * <p>编号是 1 基的（内部按 {@code 编号-1} 取位），所以必须大于 0。
     * 参数非法时直接抛 {@link IllegalArgumentException}，异常文案是原厂文案。</p>
     *
     * @param confIds 配置项编号，可变参数
     */
    public void putConfInfo(int... confIds) {
        if (confIds == null || confIds.length == 0) {
            throw new IllegalArgumentException("数组不能为空");
        }
        for (int confId : confIds) {
            if (confId <= 0) {
                throw new IllegalArgumentException("数组值必须大于0");
            }
        }
        confInfos = confIds;
    }

    /**
     * 解析响应头里的配置变更信息并更新调度任务。
     *
     * <p>为什么 {@code Conf-Updates} 要按二进制解析（{@code parseInt(..., 2)}）：
     * 服务端把 32 个配置项的变更位压成了一个整数的二进制字符串。</p>
     *
     * <p>{@code Conf-Updates-V} 的布局是 {@code v1,v2,...,vN,delayMinutes}，
     * 而某个配置项的版本号在里面的位置由 {@code (长度-1-1) - (编号-1)} 逆序算出——
     * 服务端是按"编号越大越靠前"排列的，这里照抄。</p>
     *
     * @param confUpdates    {@code Conf-Updates} 头，二进制字符串
     * @param confUpdatesVer {@code Conf-Updates-V} 头，逗号分隔
     * @return 是否正常处理完；任何解析失败都返回 false，且不做半截修改
     */
    public boolean updateConf(String confUpdates, String confUpdatesVer) {
        if (TextUtils.isEmpty(confUpdates) || TextUtils.isEmpty(confUpdatesVer)) {
            return false;
        }
        try {
            int confFlags = Integer.parseInt(confUpdates, 2);
            String[] versions = confUpdatesVer.split(",");
            if (confInfos == null || confInfos.length == 0) {
                return false;
            }
            try {
                // 最后一项是延迟分钟数，不是版本号
                int delayMinutes = Integer.parseInt(versions[versions.length - 1]);
                int index = 0;
                while (true) {
                    if (index >= confInfos.length) {
                        return false;
                    }
                    int confId = confInfos[index];
                    int bitIndex = confId - 1;
                    int valueIndex = ((versions.length - 1) - 1) - bitIndex;
                    if (valueIndex < versions.length) {
                        if (!this.schedulerTaskArray.containsKey(Integer.valueOf(confId))) {
                            int version = 0;
                            try {
                                version = Integer.parseInt(versions[valueIndex]);
                            } catch (Exception e) {
                                // 解析失败按 0 处理，仍然建任务（原厂行为）
                                LogUtil.e(TAG, ": ", e);
                            }
                            this.schedulerTaskArray.put(Integer.valueOf(confId),
                                    BaseSchedulerTask.createTask(confId, version, SystemClock.elapsedRealtime(),
                                            ((long) (delayMinutes * 60)) * 1000));
                        }
                        if (((confFlags >> bitIndex) & 1) == 1) {
                            int version = 0;
                            try {
                                version = Integer.parseInt(versions[valueIndex]);
                            } catch (Exception e) {
                                LogUtil.e(TAG, ": ", e);
                            }
                            BaseSchedulerTask task = this.schedulerTaskArray.get(Integer.valueOf(confId));
                            if (version > task.getVersion()) {
                                task.updateTask(version, SystemClock.elapsedRealtime(),
                                        ((long) (delayMinutes * 60)) * 1000, true);
                            }
                        }
                    }
                    index = index + 1;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
