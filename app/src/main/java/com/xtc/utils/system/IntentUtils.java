package com.xtc.utils.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import com.xtc.system.account.constant.NotificationFlag;
import java.io.File;

/**
 * Intent 构造工厂。
 *
 * <p>原厂类名为单字母 {@code g}，方法名全是单字母且大量重载。这里按"这个 Intent 要干什么"
 * 还原命名，返回值、flag、extra 的 key、mime 类型一个字都没改。</p>
 *
 * <p>所有跳转类 Intent 都带 {@link NotificationFlag#NOTIFICATION_FLAG_CUSTOM}
 * （数值 268435456，等于 {@code Intent.FLAG_ACTIVITY_NEW_TASK}）—— 原厂统一用这个常量，
 * 因为它需要从 Service / Receiver 里直接 startActivity。</p>
 */
public class IntentUtils {

    /** 拍照 Intent 的 flag：原厂此处直接写死 {@code 0x10000001}。 */
    private static final int FLAG_CAPTURE_IMAGE = 268435457;

    private IntentUtils() {
        // 原厂异常文案为 "u can't fuck me..."，与 ROM 无关且不会触发，按"字符串不改"原则原样保留。
        throw new UnsupportedOperationException("u can't fuck me...");
    }

    /** 关机广播 Intent（带 NEW_TASK）。 */
    public static Intent getShutdownIntent() {
        return new Intent("android.intent.action.ACTION_SHUTDOWN").addFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }

    /** 按包名取启动 Intent；包不存在时返回 null（这是原厂判断"是否已安装"的依据）。 */
    public static Intent getLaunchAppIntent(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(packageName);
    }

    /** 拍照 Intent，输出到给定 Uri。 */
    public static Intent getCaptureImageIntent(Uri outputUri) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra("output", outputUri);
        return intent.addFlags(FLAG_CAPTURE_IMAGE);
    }

    /**
     * 打开文件的 Intent。
     *
     * <p>API 23 以下固定按 APK 包类型处理（原厂的安装包分发路径）；API 23 及以上
     * 用扩展名查真实 mime 类型 —— 23 起系统对 file:// Uri 有更严的校验，
     * 必须给出正确的 mime 才能预览/安装。</p>
     */
    public static Intent getOpenFileIntent(File file) {
        if (file == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        String mimeType;
        if (Build.VERSION.SDK_INT < 23) {
            mimeType = "application/vnd.android.package-archive";
        } else {
            if (file == null) {
                // 原厂保留了这层冗余判空，实际不可达，原样保留。
                return new Intent();
            }
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileUtils.getFileExtension(file.getPath()));
        }
        intent.setDataAndType(Uri.fromFile(file), mimeType);
        return intent.addFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }

    /** 按路径打开文件。 */
    public static Intent getOpenFileIntent(String filePath) {
        return getOpenFileIntent(FileUtils.toFile(filePath));
    }

    /** 分享图文：文本 + 图片 Uri。 */
    public static Intent getShareImageIntent(String text, Uri imageUri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.TEXT", text);
        intent.putExtra("android.intent.extra.STREAM", imageUri);
        intent.setType("image/*");
        return intent.setFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }

    /** 分享图文：图片文件不存在时返回 null。 */
    public static Intent getShareImageIntent(String text, File imageFile) {
        if (FileUtils.isFileExists(imageFile)) {
            return getShareImageIntent(text, Uri.fromFile(imageFile));
        }
        return null;
    }

    /** 分享图文：按图片路径。 */
    public static Intent getShareImageIntent(String text, String imagePath) {
        return getShareImageIntent(text, FileUtils.toFile(imagePath));
    }

    /** 构造显式组件跳转 Intent。 */
    public static Intent getComponentIntent(String packageName, String className, Bundle bundle) {
        Intent intent = new Intent("android.intent.action.VIEW");
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.setComponent(new ComponentName(packageName, className));
        return intent.addFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }

    /** 卸载应用的 Intent。 */
    public static Intent getUninstallAppIntent(String packageName) {
        Intent intent = new Intent("android.intent.action.DELETE");
        intent.setData(Uri.parse("package:" + packageName));
        return intent.addFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }

    /** 构造显式组件跳转 Intent（不带 extras）。 */
    public static Intent getComponentIntent(String packageName, String className) {
        return getComponentIntent(packageName, className, null);
    }

    /** 应用详情设置页的 Intent。 */
    public static Intent getAppDetailsSettingsIntent(String packageName) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.parse("package:" + packageName));
        return intent.addFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }

    /** 纯文本分享的 Intent。 */
    public static Intent getShareTextIntent(String text) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.TEXT", text);
        return intent.setFlags(NotificationFlag.NOTIFICATION_FLAG_CUSTOM);
    }
}
