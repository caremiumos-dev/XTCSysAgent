package com.xtc.qiniu.net;

import com.xtc.httplib.bean.NetBaseResult;
import com.xtc.qiniu.bean.NetDownloadTokenParam;
import java.util.List;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

/**
 * 取云存储 token 的服务端接口。
 *
 * <p>和其它业务接口一样，返回的是带信封的 {@link NetBaseResult}，
 * 由 {@code HttpRxJavaCallback} 统一拆包。</p>
 */
public interface ICloudHttpService {

    /** 批量取下载 token。 */
    @POST("/cloud/token/download")
    Observable<NetBaseResult<List<String>>> getDownloadToken(@Body NetDownloadTokenParam param);

    /** 取上传 token。 */
    @GET("/cloud/token/upload")
    Observable<NetBaseResult<String>> getUploadToken(@Query("fileType") int fileType);

    /** 取上传 token（绑定 key）。 */
    @GET("/cloud/token/upload")
    Observable<NetBaseResult<String>> getUploadToken(@Query("fileType") int fileType,
                                                     @Query("key") String key);
}
