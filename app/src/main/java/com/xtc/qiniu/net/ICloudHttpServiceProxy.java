package com.xtc.qiniu.net;

import android.content.Context;
import com.xtc.httplib.net.BaseUrlManager;
import com.xtc.httplib.net.HttpRxJavaCallback;
import com.xtc.httplib.net.HttpServiceProxy;
import com.xtc.qiniu.bean.NetDownloadTokenParam;
import java.util.List;
import rx.Observable;

/**
 * {@link ICloudHttpService} 的 Retrofit 代理，统一拆响应信封。
 */
public class ICloudHttpServiceProxy extends HttpServiceProxy {

    public ICloudHttpServiceProxy(Context context) {
        super(context);
    }

    public Observable<List<String>> getDownloadToken(NetDownloadTokenParam param) {
        return ((ICloudHttpService) this.httpClient.request(
                BaseUrlManager.getDefaultUrl(this.context), ICloudHttpService.class))
                .getDownloadToken(param)
                .map(new HttpRxJavaCallback<List<String>>());
    }

    public Observable<String> getUploadToken(int fileType) {
        return ((ICloudHttpService) this.httpClient.request(
                BaseUrlManager.getDefaultUrl(this.context), ICloudHttpService.class))
                .getUploadToken(fileType)
                .map(new HttpRxJavaCallback<String>());
    }

    public Observable<String> getUploadToken(int fileType, String key) {
        return ((ICloudHttpService) this.httpClient.request(
                BaseUrlManager.getDefaultUrl(this.context), ICloudHttpService.class))
                .getUploadToken(fileType, key)
                .map(new HttpRxJavaCallback<String>());
    }
}
