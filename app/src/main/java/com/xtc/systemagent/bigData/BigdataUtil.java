package com.xtc.systemagent.bigData;

import android.content.Context;
import com.xtc.common.bigdata.BehaviorUtil;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.bigData.bean.CallFailureValue;
import com.xtc.systemagent.module.Constants;
import com.xtc.utils.encode.JSONUtil;
import java.util.HashMap;

/**
 * 埋点上报。
 *
 * <p>只有两个事件：通话失败（{@code call_fail_ld}）与自由落体
 * （{@code Launcher_Watch_Drop}）。</p>
 */
public class BigdataUtil {

    private static final String TAG = "Log_BigdataUtil";

    /** 自由落体事件的点击名。 */
    private static final String FUNCTION_NAME_WATCH_DROP = "Launcher_Watch_Drop";

    private BigdataUtil() {
    }

    /**
     * 把通话失败信息转成埋点用的字符串 Map。
     *
     * <p>注意 {@code GSMCDMA_OR_IMS} 用的也是 {@code voice_network_type} ——
     * 原厂就是这么写的，虽然看起来像笔误，但服务端按此消费，不能"修正"。</p>
     */
    private static HashMap<String, String> beanToMap(CallFailureValue value) {
        HashMap<String, String> map = new HashMap<>();
        if (value != null) {
            map.put(Constants.CallFailure.OTHER_NUMBER, value.getOther_number());
            map.put(Constants.CallFailure.VOICE_NETWORK_TYPE, value.getVoice_network_type());
            map.put(Constants.CallFailure.GSMCDMA_OR_IMS, value.getVoice_network_type());
            map.put(Constants.CallFailure.SELF_DIAL_MODE, value.getSelf_dial_mode());
            map.put(Constants.CallFailure.CALL_FAIL_START_TIME, value.getCall_fail_start_time());
            map.put(Constants.CallFailure.CALL_FAIL_CAUSE_CODE, value.getCall_fail_cause_code());
            map.put(Constants.CallFailure.CALL_FAIL_VENDOR_CAUSE, value.getCall_fail_vendor_cause());
            map.put(Constants.CallFailure.SELF_CELL_ID, value.getSelf_cell_id());
        }
        LogUtil.i(TAG, "beanToMap = " + map);
        return map;
    }

    /** 上报通话失败事件。 */
    public static void callFailed(Context context, String json) {
        BehaviorUtil.customEvent(context, "call_fail_ld",
                beanToMap((CallFailureValue) JSONUtil.fromJSON(json, CallFailureValue.class)));
    }

    /** 上报自由落体事件。 */
    public static void watchDropEvent(Context context) {
        LogUtil.w(TAG, "自由落体事件发生");
        BehaviorUtil.clickEvent(context, FUNCTION_NAME_WATCH_DROP);
    }
}
