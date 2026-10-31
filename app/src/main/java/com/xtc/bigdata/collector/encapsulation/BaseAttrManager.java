package com.xtc.bigdata.collector.encapsulation;

import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.xtc.bigdata.collector.SessionAgent;
import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.collector.encapsulation.entity.BaseAttr;
import com.xtc.bigdata.collector.encapsulation.entity.attr.ApplicationAttr;
import com.xtc.bigdata.collector.encapsulation.entity.attr.MachineAttr;
import com.xtc.bigdata.collector.encapsulation.entity.attr.OtherAttr;
import com.xtc.bigdata.collector.encapsulation.entity.attr.UserAttr;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.SharedPrefUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;

/**
 * 把 {@link BaseAttr} 翻译成数据库用的各类属性对象。
 *
 * <p>单例本身用 {@code InstanceHolder} 保证线程安全，但 {@link #baseAttr}
 * 是一份可被外部替换的静态引用（{@code setBaseAttr}），因为
 * {@code BaseAttr.saveConfig} 之后需要立刻让新值生效。</p>
 *
 * <p>{@link #getApplicationAttr()} 与 {@link #getMachineAttr()} 每次都会
 * 新建实例（只有 appAttr 会缓存），而 {@link #getOtherAttr(String, String)}
 * 一定新建 —— 因为会话 id 与 extend 每次事件都不同。</p>
 */
public class BaseAttrManager {

    /** 当前基信息，可能为 null（还没采集）。 */
    private static BaseAttr baseAttr;

    /** 缓存的应用属性。 */
    private ApplicationAttr mApplicationAttr;

    /** 缓存的机器属性。 */
    private MachineAttr mMachineAttr;

    /** 缓存的用户属性。 */
    private UserAttr mUserAttr;

    /** 单例持有者。 */
    private static class InstanceHolder {

        /** 唯一实例。 */
        private static final BaseAttrManager mInstance = new BaseAttrManager();

        private InstanceHolder() {
        }
    }

    private BaseAttrManager() {
    }

    /**
     * 取单例。
     *
     * <p>顺带做一次基信息的惰性加载：第一次调用时从 SharedPreferences 里读，
     * 读不到也继续返回单例（后续属性组装会因 baseAttr 为 null 而少填字段）。</p>
     *
     * @return 单例
     */
    public static BaseAttrManager getInstance() {
        if (baseAttr == null) {
            baseAttr = BaseAttr.getBaseAttr();
            LogUtil.i("bigData", "baseAttr = " + baseAttr);
        }
        return InstanceHolder.mInstance;
    }

    /**
     * 替换当前基信息。
     *
     * @param newBaseAttr 新的基信息
     */
    public static void setBaseAttr(BaseAttr newBaseAttr) {
        baseAttr = newBaseAttr;
    }

    /**
     * 取应用属性。
     *
     * <p>只有"没缓存"或"缓存里 appId 为空"时才重建 —— appId 是应用属性
     * 是否有效的判据。渠道号只在手机形态下读 SP。</p>
     *
     * @return 应用属性；baseAttr 为 null 时返回 null
     */
    public ApplicationAttr getApplicationAttr() {
        if (this.mApplicationAttr == null || TextUtils.isEmpty(this.mApplicationAttr.getAppId())) {
            String channelId = Constants.deviceType.equals(Constants.PHONE)
                    ? SharedPrefUtils.getInstance().getKeyStringValue(DeviceInfo.PHONE_CHANNEL_ID, "")
                    : "";
            if (baseAttr != null) {
                this.mApplicationAttr = new ApplicationAttr()
                        .setAppId(baseAttr.getAppId())
                        .setAppVersion(baseAttr.getAppVersion())
                        .setModuleName(baseAttr.getModuleName())
                        .setchannleId(channelId)
                        .setPackageName(baseAttr.getPackageName());
            }
        }
        return this.mApplicationAttr;
    }

    /**
     * 取机器属性（每次都新建，保证拿到最新值）。
     *
     * @return 机器属性；baseAttr 为 null 时返回 null
     */
    public MachineAttr getMachineAttr() {
        if (baseAttr != null) {
            this.mMachineAttr = new MachineAttr()
                    .setmId(baseAttr.getmId())
                    .setOsVersion(baseAttr.getOsVer())
                    .setDevName(baseAttr.getDevName())
                    .setInnerModel(baseAttr.getInnerModel())
                    .setBrand(baseAttr.getBrand())
                    .setRooted(baseAttr.getRooted());
        }
        return this.mMachineAttr;
    }

    /**
     * 取"其它"属性（每次新建）。
     *
     * @param extend         业务扩展 JSON
     * @param extendJudgment 扩展判定串
     * @return 其它属性（baseAttr 为 null 时各字段为空）
     */
    public OtherAttr getOtherAttr(String extend, String extendJudgment) {
        OtherAttr otherAttr = new OtherAttr();
        if (baseAttr != null) {
            otherAttr.setRouterMac(baseAttr.getRouterMac());
            otherAttr.setImei(baseAttr.getImei());
            otherAttr.setSessionid(SessionAgent.getSessionId());
            otherAttr.setDaVer("bigdataClient_d1c2086");
            otherAttr.setExtend(extend);
            otherAttr.setExtendJudgment(extendJudgment);
        }
        return otherAttr;
    }

    /**
     * 取屏幕分辨率串，形如 {@code "{320,320}"}。
     *
     * @return 分辨率串；没有 Context 时返回空串
     */
    public String getScreenResolution() {
        if (ContextUtils.isEmpty()) {
            return "";
        }
        DisplayMetrics displayMetrics = ContextUtils.getContext().getResources().getDisplayMetrics();
        return "{" + displayMetrics.widthPixels + "," + displayMetrics.heightPixels + "}";
    }

    /**
     * 取用户属性（惰性从 SP 反序列化）。
     *
     * @return 用户属性；没存过返回 null
     */
    public UserAttr getUserAttr() {
        if (this.mUserAttr == null) {
            String json = SharedPrefUtils.getInstance().getKeyStringValue(DeviceInfo.USERINFO, "");
            if (!TextUtils.isEmpty(json)) {
                this.mUserAttr = JSONUtil.fromJSON(json, UserAttr.class);
            }
        }
        return this.mUserAttr;
    }

    /**
     * 设置用户属性（写内存 + 落 SP）。
     *
     * @param userAttr 用户属性
     */
    public void setUserAttr(UserAttr userAttr) {
        this.mUserAttr = userAttr;
        SharedPrefUtils.getInstance().saveKeyStringValue(DeviceInfo.USERINFO, JSONUtil.toJSON(this.mUserAttr));
    }
}
