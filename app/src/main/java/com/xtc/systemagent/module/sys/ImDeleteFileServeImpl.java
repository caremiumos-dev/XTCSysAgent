package com.xtc.systemagent.module.sys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.text.TextUtils;
import com.xtc.httplib.net.BaseUrlManager;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.module.sys.bean.DeleteImContent;
import com.xtc.systemagent.net.SysHttpProxy;
import com.xtc.systemagent.net.bean.DelFileFeedParam;
import com.xtc.utils.encode.JSONUtil;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * IM 下发的"删除系统文件"指令的执行者。
 *
 * <p>这个功能依赖系统的文件清理服务：本类把待删路径写成 {@code <SD卡>/.delSysFile}
 * （每行 {@code path,-1}），再等系统发 {@code android.xtc.action.delsysfile} 广播，
 * 然后逐行回读 —— 值为 {@code 0} 表示删成功，非 {@code 0} 收集成失败列表上报。</p>
 */
public class ImDeleteFileServeImpl {

    private static final String TAG = "Log_ImDeleteFileServeImpl";

    /** 系统清理完成的广播。 */
    private static final String ACTION_DEL_SYS_FILE = "android.xtc.action.delsysfile";

    /** 待删清单文件名（放在 SD 卡根目录）。 */
    private static final String DEL_SYS_FILE_NAME = ".delSysFile";

    /** 标记"还没处理"的值。 */
    private static final String PENDING_FLAG = "-1";

    /** 标记"已删除"的值。 */
    private static final String DONE_FLAG = "0";

    /** 让系统把文件清理日志打开。 */
    private static final String PROP_FILE_CLEAN = "log.tag.file_clean";

    /** 正式环境网关。 */
    private static final String URL_RELEASE = "https://api.watch.okii.com";
    /** 灰度/开发环境网关。 */
    private static final String URL_DEBUG = "https://api-module.okii.com";

    private static SoftReference<ImDeleteFileServeImpl> deleteFileServeImplRef;

    private static ImDeleteFileReceiver receiver;

    private Context mContext;
    private SysHttpProxy httpProxy;
    private File mDeleteSysFile;
    private DeleteImContent delSysFileContent;

    /** 用来做 synchronized 的锁对象（原厂就叫 mHost）。 */
    private ImDeleteFileServeImpl mHost = this;

    /**
     * 收到系统清理完成广播后，回读清单并上报失败项。
     */
    private class ImDeleteFileReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "receive ImDeleteFile");
            synchronized (ImDeleteFileServeImpl.this.mHost) {
                if (!ImDeleteFileServeImpl.this.mDeleteSysFile.exists()) {
                    LogUtil.d(TAG, "mDeleteSysFile not exist");
                    return;
                }
                List<String> failList = new ArrayList<>();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(
                            new FileReader(ImDeleteFileServeImpl.this.mDeleteSysFile));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LogUtil.d(TAG, "lineContent=" + line);
                        String[] parts = line.split(",");
                        if (parts[0] != null && parts[1] != null && !parts[1].equals(DONE_FLAG)) {
                            failList.add(parts[0]);
                        }
                    }
                } catch (IOException e) {
                    LogUtil.e(TAG, " isDeleteApp IOException");
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            LogUtil.e(TAG, " isDeleteApp Close ... IOException");
                        }
                    }
                }
                Iterator<String> iterator = failList.iterator();
                while (iterator.hasNext()) {
                    LogUtil.d(TAG, "fail path=" + iterator.next());
                }
                ImDeleteFileServeImpl.this.feedDelFileState(failList,
                        ImDeleteFileServeImpl.this.delSysFileContent.getId());
            }
        }
    }

    private ImDeleteFileServeImpl(Context context) {
        this.mContext = context;
        if (BaseUrlManager.isReleaseEnv(context)) {
            this.httpProxy = new SysHttpProxy(context, URL_RELEASE);
        } else {
            this.httpProxy = new SysHttpProxy(context, URL_DEBUG);
        }
    }

    public static synchronized ImDeleteFileServeImpl getInstance(Context context) {
        if (deleteFileServeImplRef == null) {
            deleteFileServeImplRef = new SoftReference<>(new ImDeleteFileServeImpl(context));
        }
        ImDeleteFileServeImpl instance = deleteFileServeImplRef.get();
        if (instance == null) {
            instance = new ImDeleteFileServeImpl(context);
            deleteFileServeImplRef = new SoftReference<>(instance);
        }
        return instance;
    }

    /**
     * 处理一条"删除系统文件"的 IM 内容。
     *
     * <p>每次都会先把上一次的清单删掉，避免残留路径被重复处理。</p>
     */
    public void deleteSysFileImContent(String content) {
        synchronized (this.mHost) {
            if (TextUtils.isEmpty(content)) {
                return;
            }
            if (receiver == null) {
                LogUtil.d(TAG, "register " + ACTION_DEL_SYS_FILE);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_DEL_SYS_FILE);
                receiver = new ImDeleteFileReceiver();
                this.mContext.registerReceiver(receiver, filter);
            }
            this.mDeleteSysFile = new File(Environment.getExternalStorageDirectory(),
                    DEL_SYS_FILE_NAME);
            if (this.mDeleteSysFile.exists()) {
                LogUtil.d(TAG, "delete mDeleteSysFile.");
                this.mDeleteSysFile.delete();
            }
            this.delSysFileContent = (DeleteImContent) JSONUtil.fromJSON(content, DeleteImContent.class);
            LogUtil.d(TAG, "deleteSysFileImContent delSysFileContent:" + this.delSysFileContent);
            if (this.delSysFileContent == null) {
                LogUtil.d(TAG, "deleteSysFileImContent but delSysFileContent is null.");
                return;
            }
            final List<String> files = this.delSysFileContent.getFiles();
            if (files == null || files.isEmpty()) {
                LogUtil.d(TAG, "listPath is null.");
                return;
            }
            Observable.create(new Observable.OnSubscribe<Integer>() {
                @Override
                public void call(Subscriber<? super Integer> subscriber) {
                    try {
                        subscriber.onNext(Integer.valueOf(deleteListPath(files)));
                    } catch (Throwable throwable) {
                        subscriber.onError(throwable);
                        return;
                    }
                    subscriber.onCompleted();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer result) {
                            LogUtil.d(TAG, "deleteListPath result = " + result);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    /**
     * 把待删路径追加写入清单文件。
     *
     * @return 恒为 0（原厂如此）
     */
    private int deleteListPath(List<String> paths) throws Throwable {
        this.mDeleteSysFile.createNewFile();
        FileWriter writer = new FileWriter(this.mDeleteSysFile, true);
        try {
            Iterator<String> iterator = paths.iterator();
            while (iterator.hasNext()) {
                writer.write(iterator.next() + "," + PENDING_FLAG + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            LogUtil.e(TAG, "deleteListPath:  e=" + e);
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SystemPropertiesUtils.set(PROP_FILE_CLEAN, 1);
        return 0;
    }

    /** 上报删除结果。空失败列表时不上报 failList 字段。 */
    private void feedDelFileState(List<String> failList, final int id) {
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        DelFileFeedParam param = new DelFileFeedParam();
        param.setBindnumber(bindNumber);
        param.setId(id);
        if (!failList.isEmpty()) {
            param.setFailList(failList);
        }
        this.httpProxy.feedDelFileState(param)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.d(TAG, "id=" + id + " 上报完成");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LogUtil.d(TAG, "id=" + id + "上报失败 ,ex:" + throwable);
                    }

                    @Override
                    public void onNext(String result) {
                    }
                });
    }
}
