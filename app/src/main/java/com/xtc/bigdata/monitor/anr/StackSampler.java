package com.xtc.bigdata.monitor.anr;

import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * 主线程堆栈采样器。
 *
 * <p>用 {@link LinkedHashMap} 按时间顺序保存"时刻 -> 堆栈"，
 * 条数上限由 {@code maxEntryCount} 控制（超出就丢最早的一条），
 * 单条堆栈截断到 100KB —— 都是为了防止卡顿期间把内存打满。</p>
 *
 * @deprecated 原厂已标记废弃。
 */
@Deprecated
class StackSampler extends AbstractSampler {

    /** 默认保留条数。 */
    private static final int DEFAULT_MAX_ENTRY_COUNT = 1;

    /** 时间格式化（当前只声明未使用，保留类形状）。 */
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    /** 单条堆栈的字符上限。 */
    private static final int MAX_STACK_CHARS = 102400;

    /** 全局共享的采样结果，多个 sampler 共用同一个表。 */
    private static final LinkedHashMap<Long, String> sStackMap = new LinkedHashMap<Long, String>();

    /** 被采样的线程。 */
    private Thread mCurrentThread;

    /** 保留条数。 */
    private int mMaxEntryCount;

    /**
     * @param thread        被采样的线程
     * @param maxEntryCount 保留条数
     * @param sampleInterval 采样间隔
     */
    public StackSampler(Thread thread, int maxEntryCount, long sampleInterval) {
        super(sampleInterval);
        this.mMaxEntryCount = DEFAULT_MAX_ENTRY_COUNT;
        this.mCurrentThread = thread;
        this.mMaxEntryCount = maxEntryCount;
    }

    /**
     * @param thread         被采样的线程
     * @param sampleInterval 采样间隔
     */
    public StackSampler(Thread thread, long sampleInterval) {
        this(thread, DEFAULT_MAX_ENTRY_COUNT, sampleInterval);
    }

    @Override
    protected void doSample() {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement stackTraceElement : this.mCurrentThread.getStackTrace()) {
            builder.append(stackTraceElement.toString()).append("\r\n");
        }
        synchronized (sStackMap) {
            if (sStackMap.size() >= this.mMaxEntryCount && this.mMaxEntryCount > 0) {
                sStackMap.remove(sStackMap.keySet().iterator().next());
            }
            sStackMap.put(Long.valueOf(SystemClock.uptimeMillis()),
                    builder.substring(0, builder.length() < MAX_STACK_CHARS ? builder.length() : MAX_STACK_CHARS));
        }
    }

    /**
     * 取某段时间窗口内的堆栈。
     *
     * @param beginTime 起始 uptime
     * @param endTime   结束 uptime
     * @return 每条前面带两个换行的堆栈列表
     */
    public ArrayList<String> getThreadStackEntries(long beginTime, long endTime) {
        ArrayList<String> entries = new ArrayList<String>();
        synchronized (sStackMap) {
            for (Long timestamp : sStackMap.keySet()) {
                if (beginTime < timestamp.longValue() && timestamp.longValue() < endTime) {
                    entries.add("\r\n" + sStackMap.get(timestamp));
                }
            }
        }
        return entries;
    }
}
