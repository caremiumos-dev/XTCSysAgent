package com.xtc.httplib.okhttp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import com.xtc.httplib.ConfigOptions;
import com.xtc.httplib.LogTag;
import com.xtc.httplib.bean.NetBaseResult;
import com.xtc.im.transpond.TranspondCallback;
import com.xtc.im.transpond.TranspondManager;
import com.xtc.log.LogUtil;
import com.xtc.system.account.constant.NotificationFlag;
import com.xtc.utils.encode.JSONUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * 预处理器拦截器：能走 IM 长连接就不走 HTTP，用来在弱网/无网时保活关键请求。
 *
 * <p>判定顺序（与原厂一致）：</p>
 * <ol>
 *   <li>{@link #createFromCache} —— 预留的本地缓存分支，当前恒返回 null；</li>
 *   <li>{@link #createFromTranspond} —— 满足下面全部条件才走 IM 通道：
 *       <ul>
 *         <li>距离上一次透传超过 {@code INTERVAL_TIME}（10 分钟），避免把 IM 通道打满；</li>
 *         <li>{@link #needTranspond} 判定该 URL 需要透传；</li>
 *       </ul>
 *   </li>
 *   <li>都不满足则正常工作，交给后面的拦截器。</li>
 * </ol>
 *
 * <p>透传是**同步等回调**的：把请求交给 IM 通道后，本线程在
 * {@link TranspondListener} 上 wait 最多 20 秒；同时用 {@link AlarmManager} 挂一个
 * 21 秒的看门狗广播——因为 IM 通道可能连"失败回调"都给不出来，
 * 只能靠闹钟兜底把自己叫醒。</p>
 *
 * <p>两个静态名单由宿主注入（见 {@code HttpManager#setAllTranspond}）：
 * {@link #allTranspond} 为 true 时走"除黑名单外全透传"，否则走"仅白名单透传"。</p>
 */
public class PreprocessorInterceptor extends BaseInterceptor {

    private static final String TAG = LogTag.tag("PreprocessorInterceptor");

    /** 全量透传开关。 */
    public static boolean allTranspond = false;

    /** 走透传的 URL 白名单（子串匹配）。 */
    public static final List<String> NEED_TRANSPOND = new ArrayList<String>();

    /** 不走透传的 URL 黑名单（子串匹配），只在 {@link #allTranspond} 为 true 时生效。 */
    public static final List<String> NO_NEED_TRANSPOND = new ArrayList<String>();

    /** 两次透传之间的最小间隔（10 分钟）。 */
    private static final long TRANSPOND_INTERVAL_TIME = ConfigOptions.TranspondCondition.INTERVAL_TIME;

    /** 等待 IM 回调的超时（20 秒）。 */
    private static final long TRANSPOND_WAIT_TIME_OUT = ConfigOptions.TranspondCondition.TRANSPOND_TIME_OUT;

    /** 看门狗闹钟比等待超时多留 1 秒，保证"先等到回调，再被闹钟叫醒"。 */
    private static final long TRANSPOND_ALARM_TIME = 21000L;

    private AlarmManager alarmManager;

    /** 上一次透传时间（elapsedRealtime）。 */
    private AtomicLong lastImTranspondTime;

    /** 透传请求码自增器，用来把回调与闹钟对应起来。 */
    private AtomicInteger requestCode;

    /** 正在等待回调的监听器。写时复制，避免等待线程与广播线程互相锁住。 */
    private CopyOnWriteArrayList<TranspondListener> transpondListenerList;

    /**
     * 看门狗接收器：闹钟到点后把对应的监听器叫醒，并标记"这次超时了"。
     *
     * <p>为什么要有它：IM 通道在链路彻底断掉时既不会回调 onSuccess 也不会回调 onError，
     * 光靠 {@code wait(timeout)} 也能醒，但拿不到"是超时而不是成功"的语义区分——
     * 有了 {@code hadTimeout} 标记，上层就能直接放弃这次透传。</p>
     */
    class TimeOutReceiver extends BroadcastReceiver {

        /** 超时闹钟的 action。 */
        public static final String ACTION_TIME_OUT = "com.xtc.httplib.IMTranspond.TIME_OUT";

        /** 请求码 extra 的 key。 */
        public static final String EXTRA_REQUEST_CODE = "com.xtc.httplib.IMTranspond.REQUEST_CODE";

        TimeOutReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                LogUtil.e(PreprocessorInterceptor.TAG, "intent is null");
                return;
            }
            if (!ACTION_TIME_OUT.equals(intent.getAction())) {
                return;
            }
            int extraRequestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1);
            LogUtil.i(PreprocessorInterceptor.TAG, "receive time out alarm requestCode = " + extraRequestCode);
            if (extraRequestCode == -1) {
                return;
            }
            TranspondListener listener = findTagById(extraRequestCode);
            if (listener == null) {
                LogUtil.w(PreprocessorInterceptor.TAG, "receive time out alarm tag is null");
            } else {
                listener.waitTimeout();
            }
        }

        /**
         * 注册接收器。
         *
         * <p>注意这里用的是**方法参数**里的 context（即创建拦截器时传入的那个），
         * 而不是 {@code this.context}（应用上下文）——原厂如此，便于宿主指定接收范围。</p>
         */
        public void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_TIME_OUT);
            context.registerReceiver(this, intentFilter);
        }
    }

    /**
     * 透传回调：既实现 IM 通道的回调协议（Binder 事务），又实现工程内的
     * {@link TranspondCallback}，因为 {@link TranspondManager#transpond} 的参数类型是后者。
     *
     * <p>为什么要 extends {@link Binder}：这个对象会被序列化到 IM 进程，IM 进程通过
     * {@code com.xtc.im.transpond.ITranspondCallback} 这个 descriptor 反向调用本对象，
     * 因此必须自己实现 {@link #onTransact}（事务码 1=onSuccess、2=onError）。
     * 这是 AIDL 生成的 Stub 的标准形态，原厂就有一个这样的类，
     * 这里把它还原成有名字的内部类。</p>
     *
     * <p>回调与等待方是两个线程，所以每个状态变更方法都 {@code synchronized} 并
     * {@code notify()}——注意 wait/notify 用的是**同一个**对象监视器，
     * 这也是为什么等待方法也必须 synchronized。</p>
     */
    class TranspondListener extends Binder implements IInterface, TranspondCallback {

        /** 与 IM 侧约定的 Binder 描述符。 */
        private static final String DESCRIPTOR = "com.xtc.im.transpond.ITranspondCallback";

        /** 事务码：成功。 */
        private static final int TRANSACTION_ON_SUCCESS = 1;

        /** 事务码：失败。 */
        private static final int TRANSACTION_ON_ERROR = 2;

        /** IM 通道返回的响应体。 */
        byte[] bytes;

        /** 本次透传的请求码，-1 表示还没分配。 */
        int requestCode = -1;

        /** 是否已经被看门狗判定超时。 */
        boolean hadTimeout = false;

        /** IM 通道是否报告成功。 */
        boolean isSuccess = false;

        TranspondListener() {
            attachInterface(this, DESCRIPTOR);
        }

        /** {@link IInterface} 的唯一方法：本地对象就把自己的 Binder 返回出去。 */
        @Override
        public IBinder asBinder() {
            return this;
        }

        /**
         * Binder 事务入口。
         *
         * <p>事务码与返回时序都按 AIDL 生成的 Stub 写：成功事务要把 byte[] 原样回写
         * （远端会 {@code readByteArray}），失败事务只写 noException。
         * {@code 1598968902} 是 {@code INTERFACE_TRANSACTION}，回写描述符。</p>
         */
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case TRANSACTION_ON_SUCCESS: {
                    data.enforceInterface(DESCRIPTOR);
                    byte[] result = data.createByteArray();
                    onSuccess(result, data.readInt());
                    reply.writeNoException();
                    reply.writeByteArray(result);
                    return true;
                }
                case TRANSACTION_ON_ERROR: {
                    data.enforceInterface(DESCRIPTOR);
                    onError(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case 1598968902: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /** 通道失败：只记日志并把结果标记为失败，随后唤醒等待线程。 */
        public synchronized void onError(String message) {
            LogUtil.e(PreprocessorInterceptor.TAG, "http transmit failed:" + message);
            this.isSuccess = false;
            notify();
        }

        /** 通道成功：非空响应体才覆盖，然后标记成功并唤醒等待线程。 */
        public synchronized void onSuccess(byte[] data, int code) {
            if (data != null && data.length > 0) {
                this.bytes = data;
            }
            this.isSuccess = true;
            notify();
        }

        /** 阻塞等待回调；超时后继续往下走（由 {@link #hadTimeout} 区分是否超时）。 */
        public synchronized void waitResponse(long timeoutMillis) throws InterruptedException {
            wait(timeoutMillis);
        }

        /** 看门狗到点：标记超时并唤醒等待线程。 */
        public synchronized void waitTimeout() {
            this.hadTimeout = true;
            notify();
        }

        /**
         * 工程内 {@link TranspondCallback} 的唯一方法。
         *
         * <p>透传实现（{@code TranspondAdapterImpl}）不在本 APK 内，实际通信走的是
         * 上面的 Binder 事务；这里只是满足类型要求，不做任何事。</p>
         *
         * <p>刻意不写 {@code @Override}：如果后续把
         * {@code com.xtc.im.transpond.TranspondCallback} 还原成原厂的 AIDL 形态
         * （只保留 {@code onSuccess(byte[], int)} / {@code onError(String)}），
         * 这个方法就只是本类的普通方法，不会因为"没有可覆写的方法"而编译失败。</p>
         */
        public void onTranspondFinished() {
        }
    }

    PreprocessorInterceptor(Context context) {
        super(context);
        this.lastImTranspondTime = new AtomicLong(0L);
        this.requestCode = new AtomicInteger(0);
        this.transpondListenerList = new CopyOnWriteArrayList<TranspondListener>();
        this.alarmManager = (AlarmManager) context.getSystemService("alarm");
        new TimeOutReceiver().register(context);
    }

    private void cancelAlarm(PendingIntent pendingIntent) {
        this.alarmManager.cancel(pendingIntent);
    }

    /** HTTP 方法名转 IM 通道的方法编号。未知方法返回 -1（不认识的语义，交给通道判错）。 */
    private int convertToImMethod(String method) {
        String lowerCase = method.toLowerCase();
        if (lowerCase.equals("get")) {
            return 1;
        }
        if (lowerCase.equals("post")) {
            return 2;
        }
        if (lowerCase.equals("delete")) {
            return 3;
        }
        if (lowerCase.equals("put")) {
            return 4;
        }
        LogUtil.e(TAG, "unknown http method: " + lowerCase);
        return -1;
    }

    /**
     * 预留的本地缓存分支。
     *
     * <p>原厂就返回 null（该功能没上线），保留方法是为了让
     * {@link #intercept} 的判定顺序与调用序列不变。</p>
     */
    private Response createFromCache(Interceptor.Chain chain) {
        return null;
    }

    /**
     * 尝试走 IM 长连接透传。
     *
     * <p>四道门槛：距上次透传够久、URL 在允许名单内、（等待超时后）回调成功且不是超时、
     * 响应体能解析成 {@link NetBaseResult}。任何一道没过都返回 null，
     * 调用方退回到正常 HTTP。</p>
     *
     * <p>注意 {@code lastImTranspondTime} 在**发起前**就更新：即使这次失败，
     * 也要等到下一个 10 分钟窗口才会再试，避免连续失败把通道拖死。</p>
     */
    private Response createFromTranspond(Interceptor.Chain chain) throws IOException {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (elapsedRealtime - this.lastImTranspondTime.get() < TRANSPOND_INTERVAL_TIME) {
            LogUtil.w(TAG, "createFromTranspond not in limit time !");
            return null;
        }
        Request request = chain.request();
        String url = request.url().toString();
        if (!needTranspond(url)) {
            return null;
        }
        this.lastImTranspondTime.set(elapsedRealtime);
        TranspondListener listener = new TranspondListener();
        this.transpondListenerList.add(listener);
        listener.requestCode = getRequestCode();
        Map<String, String> headers = requestByTranspond(request, url, listener);
        PendingIntent pendingIntent = createPendingIntent(this.context, listener);
        setAlarm(TRANSPOND_ALARM_TIME, pendingIntent);
        try {
            listener.waitResponse(TRANSPOND_WAIT_TIME_OUT);
        } catch (InterruptedException e) {
            LogUtil.e(TAG, e);
        }
        cancelAlarm(pendingIntent);
        return createTranspondResponse(request, url, headers, listener);
    }

    /**
     * 构造看门狗闹钟的 PendingIntent。
     *
     * <p>flag 用的是 {@link NotificationFlag#NOTIFICATION_FLAG_HEADER}——
     * 这不是常见写法（通常用 {@code FLAG_UPDATE_CURRENT}），但原厂就是这么传的，
     * 改成标准 flag 会改变 PendingIntent 的匹配行为，因此保持原样。</p>
     */
    private PendingIntent createPendingIntent(Context context, TranspondListener listener) {
        Intent intent = new Intent();
        intent.setPackage(context.getPackageName());
        intent.setAction(TimeOutReceiver.ACTION_TIME_OUT);
        intent.putExtra(TimeOutReceiver.EXTRA_REQUEST_CODE, listener.requestCode);
        return PendingIntent.getBroadcast(context, listener.requestCode, intent,
                NotificationFlag.NOTIFICATION_FLAG_HEADER);
    }

    /**
     * 把 IM 通道拿到的字节流包装成一个正常的 OkHttp {@link Response}。
     *
     * <p>三处与普通响应不同，都是为了"让上层察觉不到这是透传"：</p>
     * <ul>
     *   <li>{@code code} 固定 200（原厂引用的是 {@code PushResponse.a.h}）——
     *       透传通道只回二进制体，没有 HTTP 状态码；</li>
     *   <li>{@code sentRequestAtMillis} 填 -1（不知道什么时候发的）；</li>
     *   <li>message 里带 URL，便于日志定位。</li>
     * </ul>
     *
     * <p>响应体先按普通响应做一次解密/解压（{@code decodeHttpResult}），
     * 再重新序列化成 JSON——这样即使 IM 通道回的是压缩加密包也能被统一处理。</p>
     */
    private Response createTranspondResponse(Request request, String url, Map<String, String> headers,
                                             TranspondListener listener) {
        if (listener.hadTimeout || !listener.isSuccess) {
            return null;
        }
        byte[] responseBytes = listener.bytes;
        if (responseBytes == null || responseBytes.length <= 0) {
            return null;
        }
        NetBaseResult<?> netBaseResult = JSONUtil.fromJSON(
                HttpHelper.decodeHttpResult(this.context, headers, new String(responseBytes)),
                NetBaseResult.class);
        if (netBaseResult == null) {
            return null;
        }
        ResponseBody responseBody = ResponseBody.create(
                MediaType.parse(ConfigOptions.HeaderKey.MEDIA_TYPE), JSONUtil.toJSON(netBaseResult));
        LogUtil.i(TAG, "createTranspondResponse url = " + url + ", method = " + request.method()
                + ", responseBody = " + responseBody);
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message(url + " Http Response Successful")
                // 原厂写的是 PushResponse.a.h，值即 200
                .code(200)
                .body(responseBody)
                .sentRequestAtMillis(-1L)
                .receivedResponseAtMillis(System.currentTimeMillis())
                .build();
    }

    /** 按请求码找正在等待的监听器；找不到返回 null（对应"闹钟迟到、请求早已结束"）。 */
    private TranspondListener findTagById(int requestCode) {
        for (TranspondListener listener : this.transpondListenerList) {
            if (listener.requestCode == requestCode) {
                return listener;
            }
        }
        return null;
    }

    /**
     * 分配请求码。
     *
     * <p>{@code get() > Integer.MAX_VALUE} 恒为 false（原厂笔误，本意是判断溢出），
     * 但一旦真的溢出成负数，PendingIntent 的 requestCode 会变成负数——保留这段代码，
     * 因为删掉它会让"赋值顺序"与原厂不同，也失去将来修 bug 的锚点。</p>
     */
    private int getRequestCode() {
        if (this.requestCode.get() > Integer.MAX_VALUE) {
            this.requestCode.set(1);
        }
        return this.requestCode.getAndIncrement();
    }

    /**
     * 该 URL 是否需要透传。
     *
     * <p>两种模式互斥：全量模式下只有黑名单能拦住；白名单模式下只有命中才放行。
     * 匹配方式是**子串包含**，不是精确相等，所以名单里通常只写域名。</p>
     */
    private boolean needTranspond(String url) {
        if (allTranspond) {
            for (String noNeedUrl : NO_NEED_TRANSPOND) {
                if (url.contains(noNeedUrl)) {
                    LogUtil.i(TAG, "NEED_TRANSPOND no need Transpond,allTranspond = " + allTranspond);
                    return false;
                }
            }
        } else {
            boolean need = false;
            for (String needUrl : NEED_TRANSPOND) {
                if (url.contains(needUrl)) {
                    need = true;
                    break;
                }
            }
            if (!need) {
                return false;
            }
            LogUtil.i(TAG, "NEED_TRANSPOND need Transpond,allTranspond = " + allTranspond);
        }
        return true;
    }

    /**
     * 通过 IM 通道发起一次透传。
     *
     * <p>顺序：读请求体 → 加密 → 拼头 → 交给通道。
     * 传给通道的两个字节数组分别是"头 JSON"与"加密后的 body"，
     * 顺序与 {@link TranspondManager#transpond} 的参数位置一一对应，不能交换。</p>
     *
     * <p>返回的 header map 要留着——响应回来后 {@code decodeHttpResult} 需要用它判断
     * 服务端有没有加密。</p>
     */
    private Map<String, String> requestByTranspond(Request request, String url, TranspondListener listener)
            throws IOException {
        int imMethod = convertToImMethod(request.method());
        RequestBody requestBody = request.body();
        Buffer buffer = new Buffer();
        if (requestBody != null) {
            requestBody.writeTo(buffer);
        }
        String bodyStr = buffer.readUtf8();
        if (TextUtils.isEmpty(bodyStr)) {
            LogUtil.w(TAG, "requestByTranspond is empty");
        }
        String encryptedBody = HttpHelper.encryptBody(this.context, bodyStr);
        byte[] bodyBytes = encryptedBody == null ? null : encryptedBody.getBytes();
        Map<String, String> headers = HttpHelper.generateHeaderMapWithBodyBytes(
                this.context, request, url, bodyStr.getBytes());
        LogUtil.i(TAG, "requestByTranspond url = " + url + ", method = " + request.method()
                + ", requestBody = " + bodyStr);
        TranspondManager.transpond(url, imMethod, JSONUtil.toJSON(headers).getBytes(), bodyBytes, listener);
        return headers;
    }

    /**
     * 挂看门狗闹钟。
     *
     * <p>API 20 起必须用 {@code setExact} 才能保证准点（省电策略会把 {@code set} 延迟），
     * 19 及以下没有 {@code setExact} 的可用性保证差异，走 {@code set}。
     * 类型固定 {@code ELAPSED_REALTIME_WAKEUP}：用开机时长计时，且要能唤醒 CPU。</p>
     */
    private void setAlarm(long delayMillis, PendingIntent pendingIntent) {
        long triggerAtMillis = SystemClock.elapsedRealtime() + delayMillis;
        if (Build.VERSION.SDK_INT > 19) {
            this.alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            this.alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response cachedResponse = createFromCache(chain);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        Response transpondResponse = createFromTranspond(chain);
        return transpondResponse == null ? chain.proceed(chain.request()) : transpondResponse;
    }
}
