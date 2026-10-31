package com.xtc.architecture.mvp;

import android.content.Context;

/**
 * 所有 Presenter 的基类，只负责持有 ApplicationContext。
 *
 * <p>原厂类名被混淆成 {@code a}；因为 {@code com.xtc.systemagent} 下所有
 * ServeImpl 都直接继承它，这里按角色还原为 {@code BasePresenter}。
 * 注意它**不**实现 mosby 的 {@code MvpPresenter}，与原厂保持一致。</p>
 */
public class BasePresenter<T> {

    protected Context mContext;

    public BasePresenter(Context context) {
        // 始终持有 ApplicationContext，避免 Presenter 泄漏 Activity。
        this.mContext = context.getApplicationContext();
    }
}
