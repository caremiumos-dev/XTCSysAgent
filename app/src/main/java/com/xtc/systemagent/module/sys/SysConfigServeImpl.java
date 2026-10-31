package com.xtc.systemagent.module.sys;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.httplib.net.BaseUrlManager;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.sys.bean.SysConfigImContent;
import com.xtc.systemagent.net.SysHttpProxy;
import com.xtc.systemagent.net.bean.SysConfigCbParam;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.lang.ref.SoftReference;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * 远程系统配置的读写。
 *
 * <p>IM 下发的 {@link SysConfigImContent} 里 {@code configAction} 决定动作：</p>
 * <ul>
 *   <li>1 —— 写属性。{@code configMsg} 的格式是 {@code "key value"}（用空格分隔）；
 *       写完回读一次并上报 {@code set:[key: value]}。</li>
 *   <li>2 —— 读属性。上报 {@code get:[key: value]}，读不到时值是 {@code invalid value}。</li>
 * </ul>
 */
public class SysConfigServeImpl {

    private static final String TAG = "Log_SysConfigServeImpl";

    /** 写属性。 */
    private static final int ACTION_SET = 1;
    /** 读属性。 */
    private static final int ACTION_GET = 2;

    /** key 与 value 之间的分隔符。 */
    private static final String KEY_VALUE_SEPARATOR = " ";

    /** 读不到属性时的占位值。 */
    private static final String INVALID_VALUE = "invalid value";

    private static SoftReference<SysConfigServeImpl> sysConfigServeImplRef;

    private Context mContext;
    private SysHttpProxy httpProxy;

    private SysConfigServeImpl(Context context) {
        this.mContext = context;
        this.httpProxy = new SysHttpProxy(context, BaseUrlManager.getGatewayUrl(context));
    }

    public static synchronized SysConfigServeImpl getInstance(Context context) {
        if (sysConfigServeImplRef == null) {
            sysConfigServeImplRef = new SoftReference<>(new SysConfigServeImpl(context));
        }
        SysConfigServeImpl instance = sysConfigServeImplRef.get();
        if (instance == null) {
            instance = new SysConfigServeImpl(context);
            sysConfigServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    /** 处理 IM 下发的配置指令。 */
    public void setSysConfigImContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return;
        }
        SysConfigImContent sysConfigContent =
                (SysConfigImContent) JSONUtil.fromJSON(content, SysConfigImContent.class);
        LogUtil.d(TAG, "setSysConfigImContent sysConfigContent:" + sysConfigContent);
        if (sysConfigContent == null) {
            LogUtil.d(TAG, "setSysConfigImContent but sysConfigContent is null.");
            return;
        }
        if (sysConfigContent.getConfigAction() == ACTION_SET) {
            setProperty(sysConfigContent);
        } else if (sysConfigContent.getConfigAction() == ACTION_GET) {
            getProperty(sysConfigContent);
        }
    }

    /** 读属性并上报。 */
    private void getProperty(SysConfigImContent content) {
        feedSysConfigState("get:[" + content.getConfigMsg() + ": "
                + SystemPropertiesUtils.get(content.getConfigMsg(), INVALID_VALUE) + "]", content);
    }

    /**
     * 写属性并上报结果。
     *
     * <p>{@code configMsg} 格式不对（没有空格分隔、或 key/value 为空）时直接放弃。
     * 解析异常只打警告，不抛出。</p>
     */
    private void setProperty(SysConfigImContent content) {
        String configMsg = content.getConfigMsg();
        if (TextUtils.isEmpty(configMsg)) {
            return;
        }
        try {
            int separatorIndex = configMsg.indexOf(KEY_VALUE_SEPARATOR);
            String key = configMsg.substring(0, separatorIndex);
            String value = configMsg.substring(separatorIndex + 1, configMsg.length());
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                return;
            }
            SystemPropertiesUtils.set(key, value);
            String readBack = SystemPropertiesUtils.get(key, INVALID_VALUE);
            feedSysConfigState("set:[" + key + ": " + readBack + "]", content);
            LogUtil.d(TAG, "setSysConfigImContent prop:" + key
                    + " ,setValue:" + value + " ,getValue:" + readBack);
        } catch (Exception e) {
            LogUtil.w(TAG, "setSysConfigImContent ex:" + e);
        }
    }

    /** 把处理结果回报给服务端。 */
    private void feedSysConfigState(String cbMsg, SysConfigImContent content) {
        if (TextUtils.isEmpty(cbMsg) || content == null) {
            return;
        }
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        SysConfigCbParam param = new SysConfigCbParam();
        param.setBindNum(bindNumber);
        param.setPushId((int) content.getId());
        param.setCbMsg(cbMsg);
        param.setConfigMsg(content.getConfigMsg());
        this.httpProxy.feedSysConfigState(param)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.d(TAG, "feedSysConfigState 上报完成");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.d(TAG, "feedSysConfigState 上报失败 ,ex:" + throwable);
                    }

                    @Override
                    public void onNext(String result) {
                    }
                });
    }
}
