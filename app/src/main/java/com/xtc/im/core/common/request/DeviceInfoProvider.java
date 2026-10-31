package com.xtc.im.core.common.request;

/**
 * 设备信息提供者：注册请求里有 12 个字符串字段，全部由宿主（App 层）提供。
 *
 * <p>IM 核心库不直接读 {@code Build}，而是让宿主实现这个接口传进来——
 * 这样系统级设备号（imei/imsi/mac 等）的取值时机与权限申请都由宿主控制，
 * 库本身不需要任何权限声明。方法顺序与 {@code RegistRequestEntity} 的字段顺序一致。</p>
 *
 * <p>原厂接口名为单字母 {@code b}。</p>
 */
public interface DeviceInfoProvider {

    String getAppKey();

    String getPkgName();

    String getDeviceId();

    String getSysName();

    String getSysVersion();

    String getImei();

    String getImsi();

    String getMac();

    String getModelNumber();

    String getBasebandVersion();

    String getBuildNumber();

    String getResolution();
}
