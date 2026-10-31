package com.xtc.architecture.mvp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hannesdorfmann.mosby.mvp.MvpFragment;
import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.hannesdorfmann.mosby.mvp.delegate.BaseMvpDelegateCallback;
import com.hannesdorfmann.mosby.mvp.delegate.FragmentMvpDelegate;

/**
 * 所有 Fragment 的基类，行为与 {@link BaseActivity} 对齐：
 * 跳转前校验目标是否存在，并使用自定义的 Fragment 委托。
 */
public abstract class BaseFragment<V extends MvpView, P extends MvpPresenter<V>>
        extends MvpFragment<V, P> {

    /** 自定义委托，第一次访问时创建。 */
    private FragmentMvpDelegate<V, P> mDelegate;

    private boolean canResolve(Intent intent) {
        if (intent == null) {
            return false;
        }
        return getActivity().getPackageManager().queryIntentActivities(intent, 1).size() > 0;
    }

    @Override
    protected FragmentMvpDelegate<V, P> getMvpDelegate() {
        if (this.mDelegate == null) {
            this.mDelegate = new FragmentDelegate<V, P>((BaseMvpDelegateCallback<V, P>) this);
        }
        return this.mDelegate;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void startActivity(Intent intent) {
        if (canResolve(intent)) {
            super.startActivity(intent);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (canResolve(intent)) {
            super.startActivityForResult(intent, requestCode);
        }
    }
}
