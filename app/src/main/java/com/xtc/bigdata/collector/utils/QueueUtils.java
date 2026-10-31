package com.xtc.bigdata.collector.utils;

import android.content.ContentValues;
import android.os.Handler;
import android.os.HandlerThread;

import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.collector.encapsulation.interfaces.IEvent;
import com.xtc.log.LogUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 事件落库的串行队列。
 *
 * <p>采集发生在任意线程，落库必须串行且不能阻塞调用方，所以这里做了三级缓冲：</p>
 * <ol>
 *   <li>{@link #queue}：无锁入队，调用方只做 offer 就返回；</li>
 *   <li>{@link #pendingEvents}：由 {@code bigdata_task} 线程批量取到 map 里，
 *       以 {@code hashCode} 去重；单批最多 200 条；</li>
 *   <li>{@link ShareHelper}：一次性 bulkInsert 落库。</li>
 * </ol>
 *
 * <p>{@link #isRunning} 是"后台线程正在攒批"的闸门，配合 {@link #timerLock}
 * 保证同一时刻只有一次 makeData 在跑。</p>
 */
public class QueueUtils {

    private static final String TAG = "QueueUtils";

    /** 单例。 */
    private static QueueUtils mInstance;

    /** 后台任务线程的 Handler。 */
    private Handler taskHandler;

    /** 本批待落库的事件，key 是事件的 hashCode（天然去重）。 */
    private final ConcurrentMap<Integer, IEvent> pendingEvents = new ConcurrentHashMap<Integer, IEvent>();

    /** 攒批状态锁。 */
    private final Object timerLock = new Object();

    /** 是否已经在攒批。 */
    private boolean isRunning = false;

    /** 无锁入队缓冲。 */
    private LinkedBlockingQueue<IEvent> queue = new LinkedBlockingQueue<IEvent>();

    /** 构造时立刻起后台线程（优先级 10 = THREAD_PRIORITY_BACKGROUND）。 */
    private QueueUtils() {
        HandlerThread handlerThread = new HandlerThread("bigdata_task", 10);
        handlerThread.start();
        this.taskHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * 把队列里的事件搬到 map（最多 200 条），然后落库。
     */
    private void checkToSave() {
        while (!this.queue.isEmpty()) {
            IEvent event = this.queue.poll();
            if (event != null) {
                this.pendingEvents.put(Integer.valueOf(event.hashCode()), event);
            }
            if (this.pendingEvents.size() >= 200) {
                break;
            }
        }
        if (this.pendingEvents.size() > 0) {
            makeData();
        }
    }

    /**
     * 队列非空且当前没有攒批任务时，起一次攒批。
     */
    private void execTimer() {
        synchronized (this.timerLock) {
            if (this.pendingEvents.size() == 0 && !this.queue.isEmpty() && !this.isRunning) {
                this.isRunning = true;
                getEvent();
            }
        }
    }

    /** 把攒批动作丢到后台线程。 */
    private void getEvent() {
        post(new Runnable() {
            @Override
            public void run() {
                checkToSave();
            }
        });
    }

    /**
     * 取单例。
     *
     * @return 单例
     */
    public static QueueUtils getInstance() {
        if (mInstance == null) {
            synchronized (QueueUtils.class) {
                if (mInstance == null) {
                    mInstance = new QueueUtils();
                }
            }
        }
        return mInstance;
    }

    /**
     * 触发每个事件的组装（{@code makeData} 会填充属性缓存），再落库。
     */
    private void makeData() {
        List<IEvent> events = new ArrayList<IEvent>();
        Iterator<Map.Entry<Integer, IEvent>> iterator = this.pendingEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            IEvent event = iterator.next().getValue();
            if (event != null) {
                event.makeData();
                events.add(event);
            }
        }
        saveData(events);
    }

    /**
     * 落库：单条走 insert，多条走 bulkInsert。
     *
     * @param events 待落库事件
     */
    private void saveData(List<IEvent> events) {
        int size = events.size();
        if (size == 1) {
            ShareHelper.getInstance().insert(events.get(0).getContentValues());
        } else {
            ContentValues[] contentValuesArray = new ContentValues[size];
            for (int index = 0; index < size; index++) {
                contentValuesArray[index] = events.get(index).getContentValues();
            }
            LogUtil.i(TAG, "saveData() 批量插入数量为 = " + size);
            ShareHelper.getInstance().insertBulk(contentValuesArray);
        }
        synchronized (this.timerLock) {
            events.clear();
            this.pendingEvents.clear();
            this.isRunning = false;
        }
        execTimer();
    }

    /**
     * 入队一个事件。
     *
     * @param event 事件
     */
    public void add(IEvent event) {
        if (this.queue.offer(event)) {
            execTimer();
        }
    }

    /**
     * 投递到后台任务线程。
     *
     * @param runnable 任务
     */
    public void post(Runnable runnable) {
        this.taskHandler.post(runnable);
    }

    /**
     * 延迟投递到后台任务线程。
     *
     * @param runnable    任务
     * @param delayMillis 延迟毫秒
     */
    public void postDelay(Runnable runnable, long delayMillis) {
        this.taskHandler.postDelayed(runnable, delayMillis);
    }
}
