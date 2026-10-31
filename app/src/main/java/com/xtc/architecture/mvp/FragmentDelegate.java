package com.xtc.architecture.mvp;

import android.os.Bundle;
import android.view.View;
import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.hannesdorfmann.mosby.mvp.delegate.BaseMvpDelegateCallback;
import com.hannesdorfmann.mosby.mvp.delegate.FragmentMvpDelegateImpl;

/**
 * 覆写 mosby 的 Fragment 委托：跳过"从 savedInstanceState 里恢复 Presenter"的逻辑，
 * 保证每次进入页面都拿到一个全新的 Presenter。
 *
 * <p>原厂类名为单字母 {@code c}。原实现里调用了父类的两参版本
 * （那是被混淆过的 mosby 分支），语义等价于"传 null 跳过恢复"，
 * 这里用真实 mosby 2.0.1 的单参 API 表达同一行为。</p>
 */
public class FragmentDelegate<V extends MvpView, P extends MvpPresenter<V>>
        extends FragmentMvpDelegateImpl<V, P> {

    public FragmentDelegate(BaseMvpDelegateCallback<V, P> delegateCallback) {
        super(delegateCallback);
    }

    @Override
    public void onCreate(Bundle savedState) {
        // 故意传 null：不恢复历史 Presenter 状态。
        super.onCreate(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedState) {
        // 原实现为空，保留这个空覆写。
    }
}
