package com.xtc.bigdata.collector.exception;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.xtc.bigdata.collector.ShareHelper;
import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.collector.encapsulation.entity.event.ExceptionEvent;
import com.xtc.bigdata.collector.utils.MD5Coder;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.constants.EType;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.SharedPrefUtils;
import com.xtc.bigdata.common.utils.StoreUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * 在独立 Service 里上报崩溃。
 *
 * <p>为什么要有它：进程崩溃时主进程的采集器可能已经跟着死了，
 * 把 {@link Throwable} 通过 Intent 传给一个独立组件去落库，
 * 是"崩溃现场还能写数据"的最后一道保险。服务在 Manifest 里注册为
 * {@code com.xtc.bigdata.collector.exception.CrashIntentService}，不可导出。</p>
 *
 * <p>去重逻辑与 {@link CrashHandler#collectCrash} 完全一致
 * （同一堆栈 + 5 秒窗口），因为两条路径可能同时被触发。</p>
 */
public class CrashIntentService extends IntentService {

    /** 堆栈在文件里的分隔标记。 */
    private static final String STACK_TRACE = "STACK_TRACE";

    private static final String TAG = "CrashIntentService";

    /** 去重临界区。 */
    private final Object lock;

    public CrashIntentService() {
        super(TAG);
        this.lock = new Object();
    }

    /**
     * 给堆栈加可读头。
     *
     * @param stackText 原始堆栈
     * @return 带头的堆栈
     */
    private String formatStackTrace(String stackText) {
        // "=" 是原厂用 ormlite 的 SimpleComparison.EQUAL_TO_OPERATION 拼的，值就是等号
        return TextUtils.concat("#", "\n", "#", new Date().toString(), "\n", STACK_TRACE, "=", stackText).toString();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Throwable throwable = (Throwable) intent.getSerializableExtra("CRASH_INFO");
        String stackText;
        boolean shouldSave = true;
        String errorMd5 = "";
        synchronized (this.lock) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause()) {
                cause.printStackTrace(printWriter);
            }
            stackText = stringWriter.toString();
            printWriter.close();
            LogUtil.d(TAG, "logTxt:" + stackText);
            try {
                errorMd5 = new MD5Coder().encode(stackText.getBytes(Charset.forName("UTF-8")));
            } catch (Exception e) {
                if (Constants.isDebug) {
                    LogUtil.e(TAG, e);
                }
            }
            CrashInfo crashInfo;
            String cachedJson = SharedPrefUtils.getInstance().getKeyStringValue(DeviceInfo.CRASH_INFO, "");
            if (TextUtils.isEmpty(cachedJson)) {
                crashInfo = new CrashInfo();
                crashInfo.setErrorMd5(errorMd5);
                crashInfo.setUpdateTime(System.currentTimeMillis());
            } else {
                crashInfo = JSONUtil.fromJSON(cachedJson, CrashInfo.class);
                if (crashInfo == null) {
                    crashInfo = new CrashInfo();
                    crashInfo.setErrorMd5(errorMd5);
                    crashInfo.setUpdateTime(System.currentTimeMillis());
                } else if (!crashInfo.getErrorMd5().equals(errorMd5)) {
                    crashInfo = new CrashInfo();
                    crashInfo.setErrorMd5(errorMd5);
                    crashInfo.setUpdateTime(System.currentTimeMillis());
                } else if (System.currentTimeMillis() - crashInfo.getUpdateTime() > 5000) {
                    crashInfo = new CrashInfo();
                    crashInfo.setErrorMd5(errorMd5);
                    crashInfo.setUpdateTime(System.currentTimeMillis());
                } else {
                    shouldSave = false;
                }
            }
            if (shouldSave) {
                SharedPrefUtils.getInstance().saveKeyStringValue(DeviceInfo.CRASH_INFO, JSONUtil.toJSON(crashInfo));
            }
        }
        if (!shouldSave || TextUtils.isEmpty(errorMd5) || TextUtils.isEmpty(stackText)) {
            return;
        }
        String stackTrace = formatStackTrace(stackText);
        if (Constants.isDebug) {
            LogUtil.e(TAG, stackTrace);
        }
        ExceptionEvent exceptionEvent = new ExceptionEvent();
        // 注意：这里 reason 拼上了 MD5（CrashHandler 那条路径没有），原厂就是不一致
        exceptionEvent.reason = throwable.toString() + errorMd5;
        exceptionEvent.stack = stackTrace;
        long internalStoreTotalSize = StoreUtils.getInternalStoreTotalSize();
        long internalStoreAvailableSize = StoreUtils.getInternalStoreAvailableSize();
        exceptionEvent.diskTotal = StoreUtils.convertSizeUnit(internalStoreTotalSize);
        exceptionEvent.diskUsage = StoreUtils.availablepercent(internalStoreAvailableSize, internalStoreTotalSize);
        long externalStoreTotalSize = StoreUtils.getExternalStoreTotalSize();
        long externalStoreAvailableSize = StoreUtils.getExternalStoreAvailableSize();
        exceptionEvent.sdTotal = StoreUtils.convertSizeUnit(externalStoreTotalSize);
        exceptionEvent.sdUsage = StoreUtils.availablepercent(externalStoreAvailableSize, externalStoreTotalSize);
        long memoryTotalSize = StoreUtils.getMemoryTotalSize();
        long memoryAvailable = StoreUtils.getMemoryAvailable(ContextUtils.getContext());
        exceptionEvent.memTotal = StoreUtils.convertSizeUnit(StoreUtils.getMemoryTotalSize());
        exceptionEvent.memUsage = StoreUtils.availablepercent(memoryAvailable, memoryTotalSize);
        exceptionEvent.functionName = EType.NAME_APP_EXCEPTION;
        exceptionEvent.makeData();
        ShareHelper.getInstance().insert(exceptionEvent.getContentValues());
    }
}
