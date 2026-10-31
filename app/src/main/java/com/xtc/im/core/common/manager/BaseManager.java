package com.xtc.im.core.common.manager;

/**
 * 所有 IM 内部管理器的基类。
 *
 * <p>构造时就往 {@link ManagerFactory} 里注册自己，因此子类只要被 new 一次，
 * 后续任何地方都能通过 {@link ManagerFactory#get(Class)} 拿到同一个实例
 * （弱引用持有，没人引用时可以被 GC 掉）。</p>
 *
 * <p>原厂类名为单字母 {@code a}。</p>
 */
public abstract class BaseManager {

    public BaseManager() {
        ManagerFactory.getInstance().register(this);
    }
}
