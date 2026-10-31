package com.xtc.systemagent.util;

import android.content.Context;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import com.xtc.qiniu.ICloudManager;
import com.xtc.qiniu.ICloudService;
import com.xtc.qiniu.TokenManager;
import com.xtc.utils.common.CollectionUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 日志上传的统一入口：打包 → 取 token → 传七牛 → 回调。
 *
 * <p>上传过程有个"进度不变"的看门狗：{@link #TEN_MINUTES} 内进度没涨过、
 * 且还没到 90%，就认为卡死，取消上传并回调失败。</p>
 *
 * <p>上传实例按"目录 + 文件名"登记在 {@link #uploadBeanMap} 里，
 * 取消时凭 key 找订阅并 unsubscribe。</p>
 */
public class UploadFile {

    private static final String TAG = "Log_UploadFile";

    /** 进度不变的容忍时间：10 分钟。 */
    private static final long TEN_MINUTES = 600000L;

    /** 重传轮询周期：30 秒。 */
    private static final long RETRY_INTERVAL_SECONDS = 30L;

    /** 重传轮询次数上限。 */
    private static final long RETRY_COUNT = 2L;

    /** 上传进度到 90% 以上就不再判定卡死。 */
    private static final double PROGRESS_STUCK_THRESHOLD = 0.9d;

    /** 上传类型：七牛。 */
    private static final int UPLOAD_TYPE_QINIU = 1;

    /** 打包/读文件缓冲区。 */
    private static final int BUFFER_SIZE = 4096;

    /** 上传中的任务，key 是"目录 + 文件名"。 */
    private static Map<String, UploadBean> uploadBeanMap = new HashMap<>();

    /** 一次上传的运行时状态。 */
    private static class UploadBean {

        String key;
        double lastProgress;
        Subscription subscription;
        long timestamp;

        private UploadBean() {
            this.timestamp = 0L;
            this.lastProgress = 0.0d;
        }

        @Override
        public String toString() {
            return "UploadBean{subscription=" + this.subscription
                    + ", key='" + this.key + "'"
                    + ", timestamp=" + this.timestamp
                    + ", lastProgress=" + this.lastProgress + '}';
        }
    }

    /** 上传回调。 */
    public interface UploadFileCallback {
        void onNoLogFile();

        void onUploadFileFailure(String error);

        void onUploadFileSuccess(String key);

        void onZipFileFailure(String error);
    }

    /** 打包回调。 */
    public interface ZipFileCallback {
        void onNoLogFile();

        void onZipFileFailure(String error);

        void onZipFileSuccess(String key, String zipFilePath);
    }

    private UploadFile() {
    }

    /**
     * 把一个 zip 的内容追加进另一个 zip。
     *
     * <p>store 方式的条目直接沿用原 {@link ZipEntry}（保留 CRC），
     * deflate 的只保留名字 —— 内容反正要重新压缩。</p>
     *
     * @return 写入的字节数
     */
    public static long copyZipFile(File srcZip, File destZip) throws Throwable {
        LogUtil.d(TAG, "copyZipFile");
        ZipFile zipFile = null;
        ZipOutputStream zipOutputStream = null;
        long copied = 0L;
        try {
            zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
            zipFile = new ZipFile(srcZip.getAbsolutePath());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                copied += entry.getSize() > 0 ? entry.getSize() : 0L;
                copyZipEntry(entry, zipFile, zipOutputStream);
            }
            zipOutputStream.finish();
        } finally {
            if (zipOutputStream != null) {
                try {
                    zipOutputStream.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, "copyZipFile close error = " + e);
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, "copyZipFile close zip error = " + e);
                }
            }
        }
        return copied;
    }

    private static void copyZipEntry(ZipEntry zipEntry, ZipFile zipFile,
                                     ZipOutputStream zipOutputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        LogUtil.d(TAG, "copyZipEntry");
        zipOutputStream.putNextEntry(zipEntry.getMethod() == ZipEntry.STORED
                ? new ZipEntry(zipEntry)
                : new ZipEntry(zipEntry.getName()));
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, read);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        zipOutputStream.flush();
    }

    /**
     * 取 token 后开始上传。
     *
     * @param fileName 压缩包文件名
     * @param dir      压缩包所在目录（结尾必须带分隔符）
     */
    public static void dealUploadFile(Context context, String fileName, String dir,
                                      boolean deleteAfterUpload,
                                      UploadFileCallback uploadFileCallback) {
        LogUtil.d(TAG, "uploadFile filePath = " + dir + " , fileName = " + fileName);
        if (NetworkUtil.isConnected(context)) {
            getNewToken(context, fileName, dir, deleteAfterUpload, uploadFileCallback);
            return;
        }
        LogUtil.e(TAG, "uploadFile Network is not Connected");
        if (uploadFileCallback != null) {
            uploadFileCallback.onZipFileFailure("Network is not Connected");
        }
    }

    /** 递归删除文件或目录。 */
    public static void deleteFile(String path, String whoDeletes) {
        LogUtil.d(TAG, "deleteFile filePath = . " + path + " ,who delete:" + whoDeletes);
        File file = new File(path);
        if (!file.exists()) {
            LogUtil.e(TAG, "deleteFile fileFolder not exists");
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null || children.length == 0) {
                file.delete();
                return;
            }
            for (File child : children) {
                deleteFile(child.getAbsolutePath(), whoDeletes);
            }
            file.delete();
        }
    }

    private static void getNewToken(final Context context, final String fileName, final String dir,
                                    final boolean deleteAfterUpload,
                                    final UploadFileCallback uploadFileCallback) {
        TokenManager.getUploadToken(context, UPLOAD_TYPE_QINIU,
                new TokenManager.OnUpLoadTokenListener() {
                    @Override
                    public void onFailure(String error) {
                        LogUtil.i(TAG, "token value onFailure= " + error);
                        if (uploadFileCallback != null) {
                            uploadFileCallback.onUploadFileFailure(error);
                        }
                    }

                    @Override
                    public void onSuccess(String token) {
                        LogUtil.i(TAG, "token value = " + token);
                        startUpload(context, fileName, dir, deleteAfterUpload, uploadFileCallback);
                    }
                });
    }

    /**
     * 第 n 次重传的间隔（秒）。
     *
     * <p>原厂就是 {@code n * 10 * n} —— 二次增长，不是指数退避。</p>
     */
    public static long getNextRetryUploadTime(int retryCount) {
        return retryCount * 10 * retryCount;
    }

    /**
     * 每 30 秒轮询一次上传，最多两轮。
     *
     * <p>轮询的意义是给七牛 SDK 一个"重来一次"的机会：
     * 第 2 的整数倍 tick 发起上传，第 2 的整数倍 + 1 tick 取消上一次，
     * 交替进行。</p>
     */
    private static void startUpload(final Context context, final String fileName, final String dir,
                                    final boolean deleteAfterUpload,
                                    final UploadFileCallback uploadFileCallback) {
        final File file = new File(dir + fileName);
        if (!file.exists()) {
            LogUtil.e(TAG, "uploadFile file not exists");
            if (uploadFileCallback != null) {
                uploadFileCallback.onZipFileFailure("zip is not exists");
            }
            return;
        }
        LogUtil.d(TAG, "uploadFile interval = 30 , count = 2");
        final UploadBean uploadBean = new UploadBean();
        uploadBean.subscription = Observable.interval(RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.d(TAG, "uploadFile completed");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.e(TAG, "uploadFile error = " + throwable);
                    }

                    @Override
                    public void onNext(Long tick) {
                        LogUtil.d(TAG, "uploadFile onNext number = " + tick
                                + "---uploadBean = " + uploadBean);
                        whenIntervalComes(context, tick, RETRY_COUNT, dir, fileName, file,
                                uploadBean, uploadFileCallback, deleteAfterUpload);
                    }
                });
        uploadBeanMap.put(dir + fileName, uploadBean);
    }

    /** 取消某个上传任务。 */
    private static void unsubscribe(String key) {
        UploadBean uploadBean = uploadBeanMap.get(key);
        LogUtil.d(TAG, "unsubscribe uploadBean = " + uploadBean);
        if (uploadBean == null) {
            return;
        }
        if (uploadBean.subscription == null) {
            LogUtil.e(TAG, "unsubscribe subscription is null , return !");
            return;
        }
        if (uploadBean.subscription.isUnsubscribed()) {
            return;
        }
        LogUtil.d(TAG, "unsubscribe !");
        uploadBean.subscription.unsubscribe();
    }

    /**
     * 单个文件/目录打包后上传。
     *
     * @param originalFilePath 要打包的源路径
     * @param fileName         压缩包文件名
     * @param zipDir           压缩包输出目录（结尾必须带分隔符）
     */
    public static void uploadZipFile(final Context context, final String originalFilePath,
                                     final String fileName, final String zipDir,
                                     final boolean deleteAfterUpload,
                                     final UploadFileCallback uploadFileCallback) {
        LogUtil.d(TAG, "uploadZipFile originalFilePath = " + originalFilePath);
        final File originalFile = new File(originalFilePath);
        if (!originalFile.exists()) {
            LogUtil.e(TAG, "original file not exists");
            if (uploadFileCallback != null) {
                uploadFileCallback.onNoLogFile();
            }
            return;
        }
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                File dirFile = new File(zipDir);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                File zipFile = new File(zipDir + fileName);
                if (zipFile.exists()) {
                    subscriber.onNext(Boolean.TRUE);
                    subscriber.onCompleted();
                    return;
                }
                ZipOutputStream zipOutputStream = null;
                boolean success;
                try {
                    zipOutputStream = new ZipOutputStream(
                            new BufferedOutputStream(new FileOutputStream(zipFile)));
                    zipDirectory(zipOutputStream, originalFile, "");
                    success = true;
                } catch (Throwable e) {
                    LogUtil.e(TAG, "uploadZipFile " + e.getMessage());
                    e.printStackTrace();
                    success = false;
                } finally {
                    if (zipOutputStream != null) {
                        try {
                            zipOutputStream.close();
                        } catch (IOException e) {
                            LogUtil.e(TAG, "uploadZipFile " + e.getMessage());
                            e.printStackTrace();
                            success = false;
                        }
                    }
                }
                subscriber.onNext(Boolean.valueOf(success));
                subscriber.onCompleted();
            }
        })
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (uploadFileCallback != null) {
                            uploadFileCallback.onZipFileFailure(throwable.getMessage());
                        }
                    }

                    @Override
                    public void onNext(Boolean success) {
                        if (!success.booleanValue()) {
                            if (uploadFileCallback != null) {
                                uploadFileCallback.onZipFileFailure("zip Exception");
                            }
                            return;
                        }
                        if (deleteAfterUpload) {
                            LogUtil.i(TAG, "uploadZipFile 245 onNext call delete deleteOldFile");
                            deleteFile(originalFilePath, "uploadFile");
                        }
                        dealUploadFile(context, fileName, zipDir, deleteAfterUpload, uploadFileCallback);
                    }
                });
    }

    /**
     * 多个目录打进同一个压缩包再上传。
     *
     * <p>不存在的源路径会先从列表里剔除；全都不存在就当"没有日志"。</p>
     */
    public static void uploadZipFile(final Context context, final List<String> originalFilePaths,
                                     final String fileName, final String zipDir,
                                     final boolean deleteAfterUpload,
                                     final UploadFileCallback uploadFileCallback) {
        LogUtil.d(TAG, "uploadZipFile , originalFilePaths = " + originalFilePaths);
        if (CollectionUtils.isEmpty(originalFilePaths)) {
            LogUtil.e(TAG, "originalFilePaths is Empty");
            if (uploadFileCallback != null) {
                uploadFileCallback.onNoLogFile();
            }
            return;
        }
        Iterator<String> iterator = originalFilePaths.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            if (!new File(path).exists()) {
                LogUtil.e(TAG, "uploadZipFile " + path + " , but file dose not exists");
                iterator.remove();
            }
        }
        if (originalFilePaths.isEmpty()) {
            return;
        }
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                File dirFile = new File(zipDir);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                File zipFile = new File(zipDir + fileName);
                if (zipFile.exists()) {
                    subscriber.onNext(Boolean.TRUE);
                    subscriber.onCompleted();
                    return;
                }
                ZipOutputStream zipOutputStream = null;
                boolean success;
                try {
                    zipOutputStream = new ZipOutputStream(
                            new BufferedOutputStream(new FileOutputStream(zipFile)));
                    for (String path : originalFilePaths) {
                        zipDirectory(zipOutputStream, new File(path), "");
                    }
                    success = true;
                } catch (Throwable e) {
                    LogUtil.e(TAG, "uploadZipFile error = " + e);
                    e.printStackTrace();
                    success = false;
                } finally {
                    if (zipOutputStream != null) {
                        try {
                            zipOutputStream.close();
                        } catch (IOException e) {
                            LogUtil.e(TAG, "uploadZipFile error = " + e);
                            e.printStackTrace();
                            success = false;
                        }
                    }
                }
                subscriber.onNext(Boolean.valueOf(success));
                subscriber.onCompleted();
            }
        })
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.i(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.i(TAG, "onError = " + throwable);
                        if (uploadFileCallback != null) {
                            uploadFileCallback.onZipFileFailure(throwable.getMessage());
                        }
                    }

                    @Override
                    public void onNext(Boolean success) {
                        if (!success.booleanValue()) {
                            if (uploadFileCallback != null) {
                                uploadFileCallback.onZipFileFailure("zip Exception");
                            }
                            return;
                        }
                        if (deleteAfterUpload) {
                            for (String path : originalFilePaths) {
                                deleteFile(path, "uploadFile");
                            }
                        }
                        dealUploadFile(context, fileName, zipDir, deleteAfterUpload, uploadFileCallback);
                    }
                });
    }

    /**
     * 轮询回调。
     *
     * <p>tick 是 2 的整数倍时发起上传；tick 是 2 的整数倍 + 1 时取消上一轮的请求 ——
     * 也就是说上传发起后 30 秒如果 SDK 自己还没结束，就主动取消，让它下一轮重来。</p>
     */
    public static void whenIntervalComes(final Context context, Long tick, long retryCount,
                                         final String dir, final String fileName, File file,
                                         final UploadBean uploadBean,
                                         final UploadFileCallback uploadFileCallback,
                                         final boolean deleteAfterUpload) {
        if (tick.longValue() % retryCount == 0) {
            uploadBean.key = ICloudService.upLoadFile(context, UPLOAD_TYPE_QINIU, fileName, file,
                    new ICloudManager.OnUpLoadListener() {
                        @Override
                        public void onFailure(String key, int statusCode, String error) {
                            LogUtil.e(TAG, "onFailure s = " + key + "---i = " + statusCode
                                    + "---s1 = " + error);
                            // -2 表示是本地主动取消，不当作失败上报。
                            if (statusCode != -2) {
                                unsubscribe(dir + fileName);
                                if (uploadFileCallback != null) {
                                    uploadFileCallback.onUploadFileFailure(key);
                                }
                            }
                        }

                        @Override
                        public void onProgress(String key, double progress) {
                            LogUtil.d(TAG, "onProgress fileName = " + key
                                    + " , currentProgress = " + progress
                                    + " , lastProgress = " + uploadBean.lastProgress);
                            if (uploadBean.lastProgress < progress) {
                                uploadBean.timestamp = System.currentTimeMillis();
                            } else if (System.currentTimeMillis() - uploadBean.timestamp > TEN_MINUTES
                                    && progress < PROGRESS_STUCK_THRESHOLD) {
                                LogUtil.d(TAG, "progress does not change !");
                                unsubscribe(dir + key);
                                if (uploadFileCallback != null) {
                                    uploadFileCallback.onUploadFileFailure("progress does not change !");
                                }
                            }
                            uploadBean.lastProgress = progress;
                        }

                        @Override
                        public void onSuccess(String key) {
                            LogUtil.d(TAG, "onSuccess fileName = " + key);
                            unsubscribe(dir + key);
                            if (deleteAfterUpload) {
                                LogUtil.i(TAG, "whenIntervalComes onSuccess call delete deleteFile");
                                deleteFile(dir, "uploadFile");
                            }
                            if (uploadFileCallback != null) {
                                uploadFileCallback.onUploadFileSuccess(key);
                            }
                        }
                    });
        } else if (tick.longValue() % retryCount == 1) {
            if (TextUtils.isEmpty(uploadBean.key)) {
                return;
            }
            ICloudService.cancle(context, uploadBean.key);
        } else {
            LogUtil.d(TAG, "do nothing");
        }
    }

    /**
     * 把若干目录/文件打成一个 zip 并上传。
     *
     * @param zipFileName 压缩包文件名
     * @param zipDir      压缩包输出目录（结尾必须带分隔符）
     */
    public static void zipDirectory(final Context context, final List<String> originalFilePaths,
                                    final String zipFileName, final String zipDir,
                                    final boolean deleteAfterUpload,
                                    final ZipFileCallback zipFileCallback) {
        LogUtil.d(TAG, "zipDirectory originalFilePaths = " + originalFilePaths);
        if (CollectionUtils.isEmpty(originalFilePaths)) {
            LogUtil.e(TAG, "zipDirectory originalFilePaths is Empty");
            if (zipFileCallback != null) {
                zipFileCallback.onNoLogFile();
            }
            return;
        }
        Iterator<String> iterator = originalFilePaths.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            if (!new File(path).exists()) {
                LogUtil.e(TAG, "zipDirectory " + path + " , but file dose not exists");
                iterator.remove();
            }
        }
        if (originalFilePaths.isEmpty()) {
            LogUtil.e(TAG, "zipDirectory originalFilePaths is Empty after filter");
            if (zipFileCallback != null) {
                zipFileCallback.onNoLogFile();
            }
            return;
        }
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                File dirFile = new File(zipDir);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                File zipFile = new File(zipDir + zipFileName);
                if (zipFile.exists()) {
                    subscriber.onNext(Boolean.TRUE);
                    subscriber.onCompleted();
                    return;
                }
                ZipOutputStream zipOutputStream = null;
                boolean success;
                try {
                    zipOutputStream = new ZipOutputStream(
                            new BufferedOutputStream(new FileOutputStream(zipFile)));
                    for (String path : originalFilePaths) {
                        zipDirectory(zipOutputStream, new File(path), "");
                    }
                    success = true;
                } catch (Throwable e) {
                    LogUtil.e(TAG, "zipDirectory error = " + e);
                    e.printStackTrace();
                    success = false;
                } finally {
                    if (zipOutputStream != null) {
                        try {
                            zipOutputStream.close();
                        } catch (IOException e) {
                            LogUtil.e(TAG, "zipDirectory error = " + e);
                            e.printStackTrace();
                            success = false;
                        }
                    }
                }
                subscriber.onNext(Boolean.valueOf(success));
                subscriber.onCompleted();
            }
        })
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.i(TAG, "zipDirectory onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.i(TAG, "zipDirectory onError = " + throwable);
                        if (zipFileCallback != null) {
                            zipFileCallback.onZipFileFailure(throwable.getMessage());
                        }
                    }

                    @Override
                    public void onNext(Boolean success) {
                        if (!success.booleanValue()) {
                            if (zipFileCallback != null) {
                                zipFileCallback.onZipFileFailure("zip Exception");
                            }
                            return;
                        }
                        zipFileCallback.onZipFileSuccess(zipFileName, zipDir + zipFileName);
                    }
                });
    }

    /**
     * 递归把一个目录写进 zip。
     *
     * @param currentPath 当前条目在 zip 里的前缀（已含分隔符）
     */
    public static void zipDirectory(ZipOutputStream zipOutputStream, File file,
                                    String currentPath) throws Throwable {
        LogUtil.d(TAG, "zipDirectory currentPath = " + currentPath);
        if (!file.isDirectory()) {
            LogUtil.i(TAG, "isFile");
            zipFile(zipOutputStream, file, currentPath);
            return;
        }
        LogUtil.i(TAG, "isDirectory");
        File[] children = file.listFiles();
        if (children == null || children.length == 0) {
            zipEmptyFolder(zipOutputStream, file, currentPath);
            return;
        }
        for (File child : children) {
            zipDirectory(zipOutputStream, child, currentPath + file.getName() + File.separator);
        }
    }

    /** 空目录也要写一个以分隔符结尾的条目，否则解压后目录会消失。 */
    private static void zipEmptyFolder(ZipOutputStream zipOutputStream, File file,
                                       String currentPath) {
        LogUtil.d(TAG, "zipEmptyFolder currentPath = " + currentPath);
        try {
            zipOutputStream.putNextEntry(new ZipEntry(currentPath + file.getName() + File.separator));
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void zipFile(ZipOutputStream zipOutputStream, File file, String currentPath)
            throws Throwable {
        LogUtil.d(TAG, "zipFile currentPath = " + currentPath);
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
            zipOutputStream.putNextEntry(new ZipEntry(currentPath + file.getName()));
            int read;
            while ((read = inputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                zipOutputStream.write(buffer, 0, read);
            }
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            LogUtil.i(TAG, "ex = " + e);
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
