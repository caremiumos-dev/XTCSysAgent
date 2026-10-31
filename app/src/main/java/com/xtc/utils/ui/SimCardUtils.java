package com.xtc.utils.ui;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtc.bigdata.common.constants.Constants;

/**
 * SIM 卡状态查询工具（原厂类名被混淆成 {@code k}）。
 *
 * <p>这里是系统手表类产品判断“能不能读卡”的前置检查：
 * 未就绪时上层会跳过读 IMSI / 号码，避免抛异常。</p>
 */
public class SimCardUtils {

    /**
     * 取 SIM 卡状态。
     *
     * @param context 上下文
     * @return {@link TelephonyManager#getSimState()} 的原值
     *         （0=ABSENT，1=PIN_REQUIRED，2=PIN_PUK_REQUIRED，3=NETWORK_LOCKED，
     *         4=READY，5=NOT_READY，6=PERM_DISABLED，7=CARD_IO_ERROR 之外的组合见系统文档）
     */
    public static int getSimState(Context context) {
        return ((TelephonyManager) context.getSystemService(Constants.PHONE)).getSimState();
    }

    /**
     * SIM 卡是否已就绪（{@code getSimState() == 5}）。
     *
     * <p>注意这里是直接与 5 比较，而系统常量 {@link TelephonyManager#SIM_STATE_READY}
     * 的值恰好也是 5；原厂写的是字面量，未就绪（{@code SIM_STATE_NOT_READY}）同样可能落到 5 之外的取值。</p>
     *
     * @param context 上下文
     * @return 就绪返回 true
     */
    public static boolean isSimCardReady(Context context) {
        return getSimState(context) == 5;
    }
}
