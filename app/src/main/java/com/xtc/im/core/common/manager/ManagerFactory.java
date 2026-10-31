package com.xtc.im.core.common.manager;

import com.xtc.im.core.common.ImLogTag;
import com.xtc.log.LogUtil;
import java.util.WeakHashMap;

/**
 * 管理器工厂：按类名持有管理器单例。
 *
 * <p>用 {@link WeakHashMap} 而不是 HashMap，是为了让“没人用的管理器”能被回收——
 * IM 里管理器不少，其中一部分只在某个业务期间存在。</p>
 *
 * <p>原厂类名为单字母 {@code b}。</p>
 */
public class ManagerFactory {

    private static final String TAG = ImLogTag.tag("ManagerFactory");

    /** 双重检查锁的单例。 */
    private static volatile ManagerFactory sInstance;

    private final WeakHashMap<String, BaseManager> managerMap = new WeakHashMap<String, BaseManager>();

    private ManagerFactory() {
    }

    /**
     * 取单例。
     */
    public static ManagerFactory getInstance() {
        if (sInstance == null) {
            synchronized (ManagerFactory.class) {
                if (sInstance == null) {
                    sInstance = new ManagerFactory();
                }
            }
        }
        return sInstance;
    }

    /**
     * 取管理器：先查注册表，查不到就反射 new 一个（new 出来的实例在构造里会自己注册进来）。
     *
     * <p>反射失败时返回 null 而不是抛异常——IM 里管理器拿不到只会退化成功能不可用，
     * 不应该把调用方拖崩。</p>
     *
     * @param clazz 管理器类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<? extends BaseManager> clazz) {
        T manager;
        synchronized (this.managerMap) {
            manager = (T) this.managerMap.get(clazz.getName());
        }
        if (manager != null) {
            return manager;
        }
        try {
            return (T) clazz.newInstance();
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e);
            return manager;
        } catch (InstantiationException e) {
            LogUtil.e(TAG, e);
            return manager;
        }
    }

    /**
     * 注册管理器。同一个类名只保留第一次注册的实例（后注册的直接丢弃）。
     */
    protected void register(BaseManager manager) {
        String name = manager.getClass().getName();
        synchronized (this.managerMap) {
            if (!this.managerMap.containsKey(name)) {
                this.managerMap.put(name, manager);
                LogUtil.i(TAG, "put manager:" + name);
            }
        }
    }
}
