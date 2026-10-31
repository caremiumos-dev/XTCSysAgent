package com.xtc.bigdata.collector.encapsulation.entity;

import android.os.Build;
import android.text.TextUtils;

import com.xtc.bigdata.collector.config.DeviceInfo;
import com.xtc.bigdata.collector.encapsulation.BaseAttrManager;
import com.xtc.bigdata.collector.utils.AppUtils;
import com.xtc.bigdata.common.constants.Constants;
import com.xtc.bigdata.common.utils.ContextUtils;
import com.xtc.bigdata.common.utils.DeviceUtils;
import com.xtc.bigdata.common.utils.FileUtils;
import com.xtc.bigdata.common.utils.SharedPrefUtils;
import com.xtc.bigdata.common.utils.SystemInfoUtils;
import com.xtc.bigdata.common.utils.SystemUtils;
import com.xtc.log.LogUtil;
import com.xtc.utils.encode.JSONUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;

/**
 * 设备基信息（机器 id、系统版本、品牌、应用信息、root、imei、mac）。
 *
 * <p>它是"采集库到底认不认得这台设备"的核心：整个对象被序列化成一串 JSON
 * 存进 SharedPreferences 的 {@code base_attr}，进程重启后直接反序列化复用，
 * 避免每次都去读 {@code /proc}、反射系统属性。</p>
 *
 * <p>主机型（{@code HOST_APP_ID} == 本进程包名）才会去取 mId/品牌/root 等
 * 敏感项；非主机型只填应用自身的信息 —— 因为手表上的采集库可能被多个进程加载，
 * 只有宿主主进程有权限读这些值。</p>
 */
public class BaseAttr {

    /** SharedPreferences 里的键。 */
    private static final String BASE_ATTR = "base_attr";

    private static final String TAG = "BaseAttr";

    /** 基信息是否变过（原厂字段，包内没有读取点，保留以维持类形状）。 */
    public static boolean isConfigChange = false;

    /** imei。 */
    private String imei;

    /** 是否 root，0/1。 */
    private int rooted;

    /** 路由器 mac。 */
    private String routerMac;

    /** 机器 id。 */
    private String mId = "";

    /** 设备名。 */
    private String devName = "";

    /** 内部型号。 */
    private String innerModel = "";

    /** 系统版本。 */
    private String osVer = "";

    /** 品牌。 */
    private String brand = "";

    /** 应用 id。 */
    private String appId = "";

    /** 应用版本名。 */
    private String appVersion = "";

    /** 包名。 */
    private String packageName = "";

    /** 模块名。 */
    private String moduleName = "";

    /**
     * 从 SharedPreferences 反序列化基信息。
     *
     * @return 基信息；没有存过或解析失败返回 null
     */
    public static synchronized BaseAttr getBaseAttr() {
        String json = SharedPrefUtils.getInstance().getKeyStringValue(BASE_ATTR, "");
        return TextUtils.isEmpty(json) ? null : JSONUtil.fromJSON(json, BaseAttr.class);
    }

    /**
     * 从 BigData 目录下读一个配置文件。
     *
     * @param fileName 文件名（带前导斜杠）
     * @return 文件内容；读不到返回空串
     */
    private String readConfigFromFile(String fileName) {
        if (ContextUtils.isEmpty()) {
            return "";
        }
        FileUtils.ensureSdCardPath();
        try {
            return readFileString(FileUtils.getBigDataDirPath() + fileName);
        } catch (Exception e) {
            LogUtil.e(TAG, "readFileString error = " + e);
            return "";
        }
    }

    /**
     * 读文本文件（UTF-8，按文件可用字节数一次性读）。
     *
     * @param filePath 文件路径
     * @return 内容；文件不存在或读失败返回空串
     */
    private String readFileString(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            fileInputStream = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            char[] buffer = new char[fileInputStream.available()];
            inputStreamReader.read(buffer);
            String content = new String(buffer);
            if (Constants.isDebug) {
                LogUtil.i(TAG, "bigdata config = " + content);
            }
            return content;
        } catch (Exception e) {
            if (Constants.isDebug) {
                LogUtil.e(TAG, "read file error = " + e);
                e.printStackTrace();
            }
            return "";
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, "close error = " + e);
                    e.printStackTrace();
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, "close error = " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 把基信息写进 SP，并同步更新内存里的 {@link BaseAttrManager}。
     *
     * @param baseAttr 最新的基信息
     */
    private static void refreshBaseAttr(BaseAttr baseAttr) {
        SharedPrefUtils.getInstance().saveKeyStringValue(BASE_ATTR, JSONUtil.toJSON(baseAttr));
        BaseAttrManager.setBaseAttr(baseAttr);
        LogUtil.i(TAG, "base attribute info = " + baseAttr);
    }

    /**
     * 保存服务端/手表下发的配置串（{@code "mId|osVer"}）。
     *
     * <p>先落盘再刷新内存；SP 里没有 base_attr 时只落盘不刷新
     * —— 因为没有基对象可改，直接构造一个反而会覆盖掉真正的设备信息。</p>
     *
     * @param fileName 文件名（带前导斜杠）
     * @param content  配置内容
     */
    public static synchronized void saveConfig(String fileName, String content) {
        if (!ContextUtils.isEmpty()) {
            saveToBigDataDir(fileName, content);
            BaseAttr baseAttr = getBaseAttr();
            if (baseAttr == null) {
                LogUtil.w(TAG, "原sp中的 baseAttr = null , return !");
            } else {
                String[] parts = content.split("\\|");
                if (parts.length >= 2) {
                    baseAttr.setmId(parts[0]);
                    baseAttr.setOsVer(parts[1]);
                }
                refreshBaseAttr(baseAttr);
            }
        }
    }

    /**
     * 把内容写到 BigData 目录下的文件（覆盖写）。
     *
     * @param fileName 文件名（带前导斜杠）
     * @param content  内容
     */
    private static void saveToBigDataDir(String fileName, String content) {
        FileUtils.ensureSdCardPath();
        String bigDataDirPath = FileUtils.getBigDataDirPath();
        File dir = new File(bigDataDirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(bigDataDirPath + fileName);
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            outputStreamWriter.write(content);
            outputStreamWriter.flush();
            fileOutputStream.flush();
        } catch (Exception e) {
            if (Constants.isDebug) {
                LogUtil.e(TAG, "write error = " + e);
                e.printStackTrace();
            }
        } finally {
            FileUtils.closeIO(fileOutputStream);
            FileUtils.closeIO(outputStreamWriter);
        }
    }

    /**
     * 采集基信息并落库（SP）。
     *
     * <p>机型分支：手表强制从 {@code /config.txt} 读 {@code "mId|osVer"}，
     * 读不到就把两项清空 —— 宁可上报空值，也不要上报一个可能属于别人的
     * {@code Build.SERIAL}。</p>
     */
    public void genBaseAttr() {
        LogUtil.i(TAG, "genBaseAttr begin");
        this.packageName = ContextUtils.getContext().getPackageName();
        this.devName = Build.MODEL;
        this.innerModel = DeviceUtils.getProductInnerModel();
        if (Objects.equals(Constants.HOST_APP_ID, this.packageName)) {
            LogUtil.i(TAG, "host process , init some base attrs !");
            this.brand = Build.BRAND;
            this.mId = Build.SERIAL;
            if (TextUtils.isEmpty(this.mId)) {
                this.mId = DeviceUtils.getIMEI(ContextUtils.getContext());
                if (TextUtils.isEmpty(this.mId)) {
                    String mac = DeviceUtils.getMac(ContextUtils.getContext());
                    this.mId = TextUtils.isEmpty(mac) ? "unknown_machineid" : mac;
                }
            }
            // 三条分支都会把 osVer 置成系统版本号，这里合并成一次赋值（行为完全一致）
            this.osVer = Build.VERSION.RELEASE;
            this.routerMac = SystemUtils.getRouterMac(ContextUtils.getContext());
            this.imei = DeviceUtils.getIMEI(ContextUtils.getContext());
            if (Constants.deviceType.equals("watch")) {
                String config = readConfigFromFile(DeviceInfo.WATCH_CONFIG_PATH);
                LogUtil.i(TAG, "bigdata genBaseAttr config = " + config);
                if (TextUtils.isEmpty(config)) {
                    this.mId = "";
                    this.osVer = "";
                } else {
                    String[] parts = config.split("\\|");
                    if (parts.length >= 2) {
                        this.mId = parts[0];
                        this.osVer = parts[1];
                    }
                }
            }
            this.rooted = SystemInfoUtils.isRoot() ? 1 : 0;
        } else {
            LogUtil.w(TAG, "is not host process , skip some attrs initialization !");
        }
        try {
            this.appId = AppUtils.getAppId(ContextUtils.getContext());
        } catch (Exception e) {
            LogUtil.e(TAG, "getAppId  error = " + e);
            e.printStackTrace();
        }
        this.appVersion = AppUtils.getVersionName(ContextUtils.getContext());
        this.moduleName = AppUtils.getModuleName(ContextUtils.getContext());
        refreshBaseAttr(this);
    }

    /** @return 应用 id */
    public String getAppId() {
        return this.appId;
    }

    /** @return 应用版本名 */
    public String getAppVersion() {
        return this.appVersion;
    }

    /** @return 品牌 */
    public String getBrand() {
        return this.brand;
    }

    /** @return 设备名 */
    public String getDevName() {
        return this.devName;
    }

    /** @return imei */
    public String getImei() {
        return this.imei;
    }

    /** @return 内部型号 */
    public String getInnerModel() {
        return this.innerModel;
    }

    /** @return 模块名 */
    public String getModuleName() {
        return this.moduleName;
    }

    /** @return 系统版本 */
    public String getOsVer() {
        return this.osVer;
    }

    /** @return 包名 */
    public String getPackageName() {
        return this.packageName;
    }

    /** @return 是否 root，0/1 */
    public int getRooted() {
        return this.rooted;
    }

    /** @return 路由器 mac */
    public String getRouterMac() {
        return this.routerMac;
    }

    /** @return 机器 id（方法名沿用原厂的 {@code getmId}） */
    public String getmId() {
        return this.mId;
    }

    /**
     * 设置应用 id。
     *
     * @param appId 应用 id
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 设置应用版本名。
     *
     * @param appVersion 版本名
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * 设置品牌。
     *
     * @param brand 品牌
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /**
     * 设置设备名。
     *
     * @param devName 设备名
     */
    public void setDevName(String devName) {
        this.devName = devName;
    }

    /**
     * 设置 imei。
     *
     * @param imei imei
     */
    public void setImei(String imei) {
        this.imei = imei;
    }

    /**
     * 设置内部型号。
     *
     * @param innerModel 内部型号
     */
    public void setInnerModel(String innerModel) {
        this.innerModel = innerModel;
    }

    /**
     * 设置模块名。
     *
     * @param moduleName 模块名
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * 设置系统版本。
     *
     * @param osVer 系统版本
     */
    public void setOsVer(String osVer) {
        this.osVer = osVer;
    }

    /**
     * 设置包名。
     *
     * @param packageName 包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * 设置 root 标记。
     *
     * @param rooted 0/1
     */
    public void setRooted(int rooted) {
        this.rooted = rooted;
    }

    /**
     * 设置路由器 mac。
     *
     * @param routerMac mac
     */
    public void setRouterMac(String routerMac) {
        this.routerMac = routerMac;
    }

    /**
     * 设置机器 id（方法名沿用原厂的 {@code setmId}）。
     *
     * @param mId 机器 id
     */
    public void setmId(String mId) {
        this.mId = mId;
    }

    @Override
    public String toString() {
        return "BaseAttr{mId='" + this.mId + "', devName='" + this.devName + "', innerModel='" + this.innerModel
                + "', osVer='" + this.osVer + "', brand='" + this.brand + "', appId='" + this.appId
                + "', appVersion='" + this.appVersion + "', packageName='" + this.packageName
                + "', moduleName='" + this.moduleName + "', imei='" + this.imei
                + "', routerMac='" + this.routerMac + "', rooted=" + this.rooted + '}';
    }
}
