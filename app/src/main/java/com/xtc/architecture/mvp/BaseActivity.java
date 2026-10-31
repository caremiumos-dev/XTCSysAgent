package com.xtc.architecture.mvp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import com.hannesdorfmann.mosby.mvp.MvpView;
import com.xtc.bigdata.collector.BehaviorCollector;

/**
 * 所有 Activity 的基类。
 *
 * <p>相比 mosby 的 {@link MvpActivity}，这里补了两件事：</p>
 * <ul>
 *   <li>无 ActionBar 的窗口特性（手表屏幕很小，标题栏没有意义）；</li>
 *   <li>跳转前先确认目标 Activity 存在，避免手表上没有安装对应应用时崩溃。</li>
 * </ul>
 */
public abstract class BaseActivity<V extends MvpView, P extends MvpPresenter<V>>
        extends MvpActivity<V, P> implements PageLifecycle {

    /** 目标 Intent 是否有对应的 Activity 可以处理。 */
    private boolean canResolve(Intent intent) {
        if (intent == null) {
            return false;
        }
        return getPackageManager().queryIntentActivities(intent, 1).size() > 0;
    }

    /** 预留的初始化钩子，原实现为空。 */
    private void initViews() {
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 交给大数据采集器做点击热区统计，再走正常分发。
        BehaviorCollector.getInstance().dealAutoCollect(event, this);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onPageEnter() {
    }

    @Override
    public void onPageVisible() {
    }

    @Override
    public void onPageLeave() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Window.FEATURE_NO_TITLE = 1
        requestWindowFeature(1);
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void startActivity(Intent intent) {
        // 目标不存在时静默丢弃，原厂行为如此。
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
