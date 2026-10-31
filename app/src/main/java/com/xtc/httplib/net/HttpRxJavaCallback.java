package com.xtc.httplib.net;

import com.xtc.httplib.bean.NetBaseResult;
import com.xtc.httplib.okhttp.WatchHttpResultException;
import com.xtc.log.LogUtil;
import rx.functions.Func1;

/**
 * 把 {@link NetBaseResult} 信封拆开：成功返回业务数据，失败抛
 * {@link WatchHttpResultException}。
 */
public class HttpRxJavaCallback<T> implements Func1<NetBaseResult<T>, T> {

    @Override
    public T call(NetBaseResult<T> result) {
        if (result == null) {
            LogUtil.e("terrible error:http response of httpResponse is null!");
            throw new NullPointerException("terrible error:http response of httpResponse is null!");
        }
        String code = result.getCode();
        if (NetBaseResult.SUCCESS.equals(code)) {
            return result.getData();
        }
        throw new WatchHttpResultException(code);
    }
}
