package com.xtc.qiniu;

import android.content.Context;
import com.xtc.log.LogUtil;
import java.io.File;

/**
 * 云存储的统一门面。目前只有七牛一个实现。
 *
 * <p>所有方法都是静态的，内部委托给 {@link ICloudManager} 单例。</p>
 */
public class ICloudService {

    /** 七牛。 */
    public static final int Type_QiNiu = 1;
    /** 网宿（预留）。 */
    public static final int Type_Wcs = 2;

    private static int iCloudType = Type_QiNiu;

    private ICloudService() {
    }

    private static ICloudManager getInstance(Context context) {
        return QiNiuManager.getInstance(context.getApplicationContext());
    }

    public static void setICloudType(int type) {
        if (iCloudType != type) {
            LogUtil.d("切换云存储原类型=" + iCloudType + ", 新类型=" + type);
            iCloudType = type;
        }
    }

    /** 上传字节数组。 */
    public static String upLoadData(Context context, int type, String key, byte[] data,
                                    ICloudManager.OnUpLoadListener listener) {
        return getInstance(context).uploadData(type, key, data, listener);
    }

    /** 上传文件。 */
    public static String upLoadFile(Context context, int type, String key, File file,
                                    ICloudManager.OnUpLoadListener listener) {
        return getInstance(context).uploadFile(type, key, file, listener);
    }

    /** 上传文件（带封面）。 */
    public static String upLoadFileByCover(Context context, int type, String key, File file,
                                           ICloudManager.OnUpLoadListener listener) {
        return getInstance(context).uploadFileByCover(type, key, file, listener);
    }

    /** 上传文件（显式指定文件名与业务类型）。 */
    public static String uploadFile(Context context, String fileName, String key, String type,
                                    ICloudManager.OnUpLoadListener listener) {
        return getInstance(context).uploadFile(fileName, key, type, listener);
    }

    /** 下载成字节数组。 */
    public static void downLoadForByte(Context context, String url,
                                       ICloudManager.OnDownLoadListener listener) {
        getInstance(context).downloadForBytes(url, listener);
    }

    /** 下载到文件。 */
    public static void downLoadForFile(Context context, String url, String dir, String fileName,
                                       ICloudManager.OnDownLoadListener listener) {
        getInstance(context).downloadForFile(url, dir, fileName, listener);
    }

    /** 取消下载。 */
    public static void cancle(Context context, String url) {
        getInstance(context).cancle(url);
    }
}
