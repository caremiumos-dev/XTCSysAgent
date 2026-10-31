package com.xtc.database.core;

import android.content.Context;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.xtc.log.LogUtil;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * 把 {@link DaoBase} 的每一个同步数据库操作包装成 RxJava 1.x 的 {@link Observable}。
 *
 * <p>原厂类被混淆成单字母 {@code com.xtc.database.a.l}，父类 {@code com.xtc.database.a.j}
 * 在本工程里重写为 {@link DaoBase}。本类的职责只有一个：让业务层不必自己开线程，
 * 调一个 {@code xxxAsync(...)} 就能拿到结果。</p>
 *
 * <p>线程模型（原厂 {@code l.a(Callable, Action1)} 的 {@code execute -> delay} 链）：</p>
 * <ol>
 *   <li>数据库读写跑在 {@link Schedulers#io()} —— {@code subscribeOn}；</li>
 *   <li>回调抛回主线程 —— {@code observeOn(AndroidSchedulers.mainThread())}；</li>
 *   <li>返回的 {@link Subscription} 收进 {@link CompositeSubscription}，便于页面销毁时统一取消。</li>
 * </ol>
 *
 * <p>约定：使用前必须先调 {@link #resetSubscriptions()}（原厂 {@code f()}）建好订阅容器，
 * 否则 {@link #submit} 会抛 {@code RuntimeException("Do you call subscribe()")}；
 * 用完调 {@link #unsubscribeAll()}（原厂 {@code g()}）取消全部未完成的操作。</p>
 *
 * <p>注意：原厂把"任务执行失败"当成日志处理 —— Observable 只 {@code onNext} + {@code onCompleted}，
 * 异常时只打一条 {@code "Error reading from the database"} 日志、既不 {@code onError} 也不
 * {@code onCompleted}。这个行为（订阅者会一直等到被取消）被原样保留，不做"修正"。</p>
 *
 * @param <T> 实体类型
 */
public class RxDao<T> extends DaoBase<T> {

    /** 日志 tag，原厂字符串常量 {@code "RxDao"}，保持不变。 */
    private static final String TAG = "RxDao";

    /** 批量操作编号：删除（原厂 {@code j.b(List)} 传的 2）。 */
    private static final int OPERATOR_DELETE = 2;

    /** 批量操作编号：更新（原厂 {@code j.c(List)} 传的 3）。 */
    private static final int OPERATOR_UPDATE = 3;

    /**
     * 当前这一批订阅的容器。
     *
     * <p>原厂字段类型是混淆后的 {@code rx.i.b}，从 {@code add(Subscription)} /
     * {@code isUnsubscribed()} 的用法可以确定它就是 {@link CompositeSubscription}。</p>
     */
    private CompositeSubscription subscriptions;

    /**
     * 三参构造器，签名与原厂一致（Context / 实体类型 / 数据库名），直接交给 {@link DaoBase}。
     *
     * @param context      上下文，基类会取 applicationContext
     * @param entityClass  实体类型
     * @param databaseName 数据库名
     */
    public RxDao(Context context, Class<T> entityClass, String databaseName) {
        super(context, entityClass, databaseName);
    }

    // =====================================================================
    // 一、Observable 包装与订阅入口
    // =====================================================================

    /**
     * 把一个同步任务包成"最多发一个 onNext"的 Observable（原厂 {@code l.a(Callable)}）。
     *
     * <p>失败时只打日志，不调 {@code onError}，因此下游不会收到终止事件 —— 这是原厂行为。</p>
     *
     * <p>这里用的 {@code Observable.create(OnSubscribe)} 在 rxjava 1.3.8 里已被标记为
     * deprecated（官方推荐 {@code fromCallable} / {@code unsafeCreate}），但原厂就是这一种写法，
     * 为了保持"任务在订阅时才执行、异常不往下游传"的形状，继续沿用，不做替换。</p>
     *
     * @param task 在订阅时执行的任务
     * @param <R>  结果类型
     * @return 冷的 Observable
     */
    private <R> Observable<R> wrap(final Callable<R> task) {
        return Observable.create(new Observable.OnSubscribe<R>() {
            @Override
            public void call(Subscriber<? super R> subscriber) {
                try {
                    subscriber.onNext(task.call());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    LogUtil.e(TAG, "Error reading from the database" + e);
                }
            }
        });
    }

    /**
     * 订阅入口（原厂 {@code l.a(Callable, Action1)}）：io 执行 + 主线程回调 + 登记到容器。
     *
     * <p>注意先订阅、后检查容器：容器没建时抛异常，但任务其实已经跑起来了 —— 原厂就是这个顺序。</p>
     *
     * @param task     同步任务
     * @param onResult 结果回调
     * @param <R>      结果类型
     * @return 本次操作对应的 Observable
     * @throws RuntimeException 忘记先调 {@link #resetSubscriptions()} 时抛出，文案保持原样
     */
    private <R> Observable<R> submit(Callable<R> task, Action1<R> onResult) {
        Observable<R> observable = wrap(task);
        Subscription subscription = observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onResult);
        if (this.subscriptions == null) {
            throw new RuntimeException("Do you call subscribe()");
        }
        this.subscriptions.add(subscription);
        return observable;
    }

    /**
     * 建好（或重建）订阅容器（原厂 {@code f()}）。
     *
     * <p>对应原厂的 {@code this.d = m.a(this.d)}：容器为空或已被取消时换一个新的，
     * 否则沿用旧的，避免把已有的未完成操作丢掉。</p>
     */
    public void resetSubscriptions() {
        if (this.subscriptions == null || this.subscriptions.isUnsubscribed()) {
            this.subscriptions = new CompositeSubscription();
        }
    }

    /**
     * 取消当前容器里的全部订阅（原厂 {@code g()}）。
     *
     * <p>对应原厂的 {@code m.a(this.d)}，内部判空。</p>
     */
    public void unsubscribeAll() {
        RxUtils.unsubscribe(this.subscriptions);
    }

    // =====================================================================
    // 二、增删改：异步入口（原厂 l 的全部 public 方法，顺序与原文一致）
    // =====================================================================

    /**
     * 清空整表，先数行数再决定是否删除（原厂 {@code l.a(i)} → {@code j.b()}）。
     *
     * @param callback 结果回调：true 表示表已空/已清干净
     */
    public void clearTableAsync(final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return clearTableWithCountCheck();
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 插入一条（原厂 {@code l.a(T, i)} → {@code j.a(T)}）。
     *
     * @param entity   实体
     * @param callback 结果回调：true 表示插入成功
     */
    public void insertAsync(final T entity, final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return insert(entity) > 0;
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按条件列找到那条记录，再用实体里的非空字段覆盖它（原厂 {@code l.a(T, String, Object, i)}
     * → {@code j.a(T, String, Object)}）。
     *
     * <p>直接复用 {@link DaoBase#updateByColumn(Object, String, Object)}。基类那条方法已经按原厂
     * {@code j.a(T, String, Object)} 重写过：先按 {@code column = value} 定位记录，再把实体里的
     * 非空、非主键字段合并进去，最后按主键写回。因此这里与原文 1:1。</p>
     *
     * @param entity   新值载体
     * @param column   条件列
     * @param value    条件值
     * @param callback 结果回调：true 表示至少改了一行
     */
    public void updateByColumnAsync(final T entity, final String column, final Object value,
                                    final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return updateByColumn(entity, column, value);
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按多条件找到那条记录，再用实体里的非空字段覆盖它（原厂 {@code l.a(T, Map, i)}
     * → {@code j.a(T, Map)}）。
     *
     * <p>{@link DaoBase} 没有"条件更新"能力，这里用 {@link #queryForFirst(Map)} 定位记录后
     * 自己做字段合并，再走 ormlite 的 update。</p>
     *
     * @param entity     新值载体（非空字段才生效，主键字段被忽略）
     * @param conditions 等值条件，多个条件之间是 and
     * @param callback   结果回调：true 表示至少改了一行；记录不存在时 false
     */
    public void updateByConditionsAsync(final T entity, final Map<String, Object> conditions,
                                        final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return updateByConditions(entity, conditions);
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按单个字段删（原厂 {@code l.a(String, Object, i)} → {@code j.a(String, Object)}）。
     *
     * @param column   列名
     * @param value    列值
     * @param callback 结果回调：true 表示删掉了至少一行
     */
    public void deleteByColumnAsync(final String column, final Object value,
                                    final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return deleteByColumn(column, value);
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 条件 + 排序取第一条（原厂 {@code l.a(String, Object, String, boolean, i)}
     * → {@code j.a(String, Object, String, boolean)}）。
     *
     * <p>用 {@link DaoBase#query(String, String, Object, boolean)} 拿列表后取第一条，
     * 语义等价于原厂的 {@code queryForFirst()}。</p>
     *
     * @param whereColumn   条件列
     * @param value         条件值
     * @param orderByColumn 排序列
     * @param ascending     是否升序
     * @param callback      结果回调：没有记录时为 null
     */
    public void queryForFirstAsync(final String whereColumn, final Object value,
                                   final String orderByColumn, final boolean ascending,
                                   final DaoCallback callback) {
        submit(new Callable<T>() {
            @Override
            public T call() {
                return firstOrNull(query(orderByColumn, whereColumn, value, ascending));
            }
        }, new Action1<T>() {
            @Override
            public void call(T result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 条件 + 排序 + 分页取一页（原厂 {@code l.a(String, Object, String, boolean, Long, Long, i)}
     * → {@code j.a(String, Object, String, boolean, Long, Long)}）。
     *
     * <p>{@link DaoBase} 的 {@code query(...)} 不支持 offset/limit，这里用 {@link #getDao()} 兜底。</p>
     *
     * @param whereColumn   条件列
     * @param value         条件值
     * @param orderByColumn 排序列
     * @param ascending     是否升序
     * @param offset        偏移，可为 null
     * @param limit         条数，可为 null
     * @param callback      结果回调
     */
    public void queryPageAsync(final String whereColumn, final Object value,
                               final String orderByColumn, final boolean ascending,
                               final Long offset, final Long limit, final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryPage(whereColumn, value, orderByColumn, ascending, offset, limit);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 条件为 {@code column >= value}，并按同一列排序（原厂 {@code l.a(String, Object, boolean, i)}
     * → {@code j.a(String, Object, boolean)}）。
     *
     * <p>{@link DaoBase} 没有 ge 条件，用 {@link #getDao()} 兜底。</p>
     *
     * @param column    列名（既做条件也做排序列）
     * @param value     下界（含等于）
     * @param ascending 是否升序
     * @param callback  结果回调
     */
    public void queryGreaterOrEqualAsync(final String column, final Object value,
                                         final boolean ascending, final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryGreaterOrEqual(column, value, ascending);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按排序 + 条件查询（原厂 {@code l.a(String, String, Object, boolean, i)}
     * → {@code j.a(String, String, Object, boolean)}），映射到
     * {@link DaoBase#query(String, String, Object, boolean)}。
     *
     * @param orderByColumn 排序列
     * @param whereColumn   条件列
     * @param value         条件值
     * @param ascending     是否升序
     * @param callback      结果回调
     */
    public void queryAsync(final String orderByColumn, final String whereColumn, final Object value,
                           final boolean ascending, final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return query(orderByColumn, whereColumn, value, ascending);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 批量插入（原厂 {@code l.a(List, i)} → {@code j.a(List)}，内部 {@code a(list, 1)}）。
     *
     * <p>映射到 {@link DaoBase#insertAll(java.util.Collection)}：原厂要求"条数等于列表长度"，
     * 用返回的受影响行数与列表长度比较即可。</p>
     *
     * @param entities 实体列表
     * @param callback 结果回调：true 表示整批都插入成功
     */
    public void insertAllAsync(final List<T> entities, final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return insertAll(entities) == entities.size();
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 多条件删除（原厂 {@code l.a(Map, i)} → {@code j.a(Map)}）。
     *
     * <p>{@link DaoBase} 没有条件删除，用 {@link #getDao()} 兜底；条件之间是 and。</p>
     *
     * @param conditions 等值条件
     * @param callback   结果回调：true 表示删掉了至少一行
     */
    public void deleteByConditionsAsync(final Map<String, Object> conditions,
                                        final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return deleteByConditions(conditions);
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 多条件 + 排序取第一条（原厂 {@code l.a(Map, String, boolean, i)}
     * → {@code j.a(Map, String, boolean)}）。
     *
     * <p>{@link DaoBase#queryForFirst(Map)} 不接排序，排序会决定"第一条"是哪条，因此用
     * {@link #getDao()} 兜底。</p>
     *
     * @param conditions    等值条件，and 连接
     * @param orderByColumn 排序列
     * @param ascending     是否升序
     * @param callback      结果回调：没有记录时为 null
     */
    public void queryForFirstByConditionsAsync(final Map<String, Object> conditions,
                                               final String orderByColumn, final boolean ascending,
                                               final DaoCallback callback) {
        submit(new Callable<T>() {
            @Override
            public T call() {
                return queryForFirstByConditions(conditions, orderByColumn, ascending);
            }
        }, new Action1<T>() {
            @Override
            public void call(T result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 多条件 + 排序 + 分页（原厂 {@code l.a(Map, String, boolean, Long, Long, i)}
     * → {@code j.a(Map, String, boolean, Long, Long)}）。
     *
     * @param conditions    等值条件，and 连接
     * @param orderByColumn 排序列
     * @param ascending     是否升序
     * @param offset        偏移，可为 null
     * @param limit         条数，可为 null
     * @param callback      结果回调
     */
    public void queryPageByConditionsAsync(final Map<String, Object> conditions,
                                          final String orderByColumn, final boolean ascending,
                                          final Long offset, final Long limit,
                                          final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryPageByConditions(conditions, orderByColumn, ascending, offset, limit);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 只取指定列（原厂 {@code l.a(String[], i)} → {@code j.a(String[])}）。
     *
     * <p>{@link DaoBase} 不支持 selectColumns，用 {@link #getDao()} 兜底。
     * 未选中的字段在实体里是默认值。</p>
     *
     * @param columns  要查的列名
     * @param callback 结果回调
     */
    public void queryColumnsAsync(final String[] columns, final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryColumns(columns);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按多条件数行数，同步返回（原厂 {@code l.b(Map)}）。
     *
     * <p>{@link DaoBase#count()} 只支持整表计数，条件计数用 {@link #getDao()} 兜底。</p>
     *
     * @param conditions 等值条件，and 连接
     * @return 命中行数；SQL 失败返回 0
     */
    public long countByConditions(Map<String, Object> conditions) {
        return countByConditionsSync(conditions);
    }

    /**
     * 整表计数（原厂 {@code l.b(i)} → {@code j.d()}），映射到 {@link DaoBase#count()}。
     *
     * @param callback 结果回调
     */
    public void countAsync(final DaoCallback callback) {
        submit(new Callable<Long>() {
            @Override
            public Long call() {
                return count();
            }
        }, new Action1<Long>() {
            @Override
            public void call(Long result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按主键整行更新（原厂 {@code l.b(T, i)} → {@code j.b(T)}），映射到
     * {@link DaoBase#update(Object)}。
     *
     * @param entity   实体（主键必须已存在）
     * @param callback 结果回调：true 表示至少改了一行
     */
    public void updateAsync(final T entity, final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return update(entity) > 0;
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 删除 {@code column < value} 的行（原厂 {@code l.b(String, Object, i)}
     * → {@code j.b(String, Object)}）。
     *
     * <p>{@link DaoBase} 只有等值删除，lt 条件用 {@link #getDao()} 兜底。</p>
     *
     * @param column   列名
     * @param value    上界（不含）
     * @param callback 结果回调：删掉的行数
     */
    public void deleteLessThanAsync(final String column, final Object value,
                                    final DaoCallback callback) {
        submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return deleteLessThan(column, value);
            }
        }, new Action1<Integer>() {
            @Override
            public void call(Integer result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 条件为 {@code column <= value}，并按同一列排序（原厂 {@code l.b(String, Object, boolean, i)}
     * → {@code j.b(String, Object, boolean)}）。
     *
     * <p>{@link DaoBase} 没有 le 条件，用 {@link #getDao()} 兜底。</p>
     *
     * @param column    列名（既做条件也做排序列）
     * @param value     上界（含等于）
     * @param ascending 是否升序
     * @param callback  结果回调
     */
    public void queryLessOrEqualAsync(final String column, final Object value,
                                      final boolean ascending, final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryLessOrEqual(column, value, ascending);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 批量删除（原厂 {@code l.b(List, i)} → {@code j.b(List)}，内部 {@code a(list, 2)}）。
     *
     * <p>{@link DaoBase} 没有批量删除，用 {@link #getDao()} 兜底，并沿用原厂的"整批一个事务"。</p>
     *
     * @param entities 实体列表
     * @param callback 结果回调：true 表示整批都删掉了
     */
    public void deleteAllAsync(final List<T> entities, final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return executeBatch(entities, OPERATOR_DELETE);
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按多条件数行数（原厂 {@code l.b(Map, i)} → {@code j.b(Map)}）。
     *
     * @param conditions 等值条件，and 连接
     * @param callback   结果回调：命中行数
     */
    public void countByConditionsAsync(final Map<String, Object> conditions,
                                       final DaoCallback callback) {
        submit(new Callable<Long>() {
            @Override
            public Long call() {
                return countByConditionsSync(conditions);
            }
        }, new Action1<Long>() {
            @Override
            public void call(Long result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 查全表（原厂 {@code l.c(i)} → {@code j.e()}），映射到 {@link DaoBase#queryAll()}。
     *
     * @param callback 结果回调
     */
    public void queryAllAsync(final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryAll();
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 按单个字段查（原厂 {@code l.c(String, Object, i)} → {@code j.c(String, Object)}），
     * 映射到 {@link DaoBase#queryByColumn(String, Object)}。
     *
     * @param column   列名
     * @param value    列值
     * @param callback 结果回调
     */
    public void queryByColumnAsync(final String column, final Object value,
                                   final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryByColumn(column, value);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 批量更新（原厂 {@code l.c(List, i)} → {@code j.c(List)}，内部 {@code a(list, 3)}）。
     *
     * <p>{@link DaoBase} 没有批量更新，用 {@link #getDao()} 逐条 update，整批一个事务。</p>
     *
     * @param entities 实体列表（主键必须已存在）
     * @param callback 结果回调：true 表示整批都更新成功
     */
    public void updateAllAsync(final List<T> entities, final DaoCallback callback) {
        submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return executeBatch(entities, OPERATOR_UPDATE);
            }
        }, new Action1<Boolean>() {
            @Override
            public void call(Boolean result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 多条件查列表（原厂 {@code l.c(Map, i)} → {@code j.c(Map)}）。
     *
     * <p>{@link DaoBase#queryForFirst(Map)} 只给第一条，整批结果用 {@link #getDao()} 兜底。</p>
     *
     * @param conditions 等值条件，and 连接
     * @param callback   结果回调
     */
    public void queryByConditionsAsync(final Map<String, Object> conditions,
                                       final DaoCallback callback) {
        submit(new Callable<List<T>>() {
            @Override
            public List<T> call() {
                return queryByFieldValues(conditions);
            }
        }, new Action1<List<T>>() {
            @Override
            public void call(List<T> result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 整表计数，同步返回（原厂 {@code l.d()}）。
     *
     * <p>原厂重写了父类的 {@code d()} 只为把父类实现再转发一次，这里保留同样的转发，
     * 语义与 {@link DaoBase#count()} 完全一致。</p>
     *
     * @return 行数
     */
    @Override
    public long count() {
        return super.count();
    }

    /**
     * 按单个字段取第一条（原厂 {@code l.d(String, Object, i)} → {@code j.e(String, Object)}）。
     *
     * <p>{@link DaoBase} 没有"等值取第一条"，用 {@link #queryByColumn(String, Object)}
     * 的结果取第一条代替。</p>
     *
     * @param column   列名
     * @param value    列值
     * @param callback 结果回调：没有记录时为 null
     */
    public void queryForFirstAsync(final String column, final Object value,
                                   final DaoCallback callback) {
        submit(new Callable<T>() {
            @Override
            public T call() {
                return firstOrNull(queryByColumn(column, value));
            }
        }, new Action1<T>() {
            @Override
            public void call(T result) {
                callback.onResult(result);
            }
        });
    }

    /**
     * 多条件取第一条（原厂 {@code l.d(Map, i)} → {@code j.d(Map)}），映射到
     * {@link DaoBase#queryForFirst(Map)}。
     *
     * @param conditions 等值条件，and 连接
     * @param callback   结果回调：没有记录时为 null
     */
    public void queryForFirstByConditionsAsync(final Map<String, Object> conditions,
                                               final DaoCallback callback) {
        submit(new Callable<T>() {
            @Override
            public T call() {
                return queryForFirst(conditions);
            }
        }, new Action1<T>() {
            @Override
            public void call(T result) {
                callback.onResult(result);
            }
        });
    }

    // =====================================================================
    // 三、兜底实现：DaoBase 缺的能力，直接走 ormlite 的 Dao
    // =====================================================================

    /** 取列表第一条，空列表返回 null。 */
    private T firstOrNull(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /** 条件 + 排序 + 分页（原厂 {@code j.a(String, Object, String, boolean, Long, Long)}）。 */
    private List<T> queryPage(String whereColumn, Object value, String orderByColumn,
                              boolean ascending, Long offset, Long limit) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            queryBuilder.where().eq(whereColumn, value);
            queryBuilder.orderBy(orderByColumn, ascending);
            queryBuilder.offset(offset);
            queryBuilder.limit(limit);
            return queryBuilder.query();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryPage() failed, ex = " + e);
            return null;
        }
    }

    /** 多条件 + 排序 + 分页（原厂 {@code j.a(Map, String, boolean, Long, Long)}）。 */
    private List<T> queryPageByConditions(Map<String, Object> conditions, String orderByColumn,
                                          boolean ascending, Long offset, Long limit) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            Where<T, Integer> where = queryBuilder.where();
            queryBuilder.orderBy(orderByColumn, ascending);
            queryBuilder.offset(offset);
            queryBuilder.limit(limit);
            boolean first = true;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (first) {
                    where.eq(entry.getKey(), entry.getValue());
                    first = false;
                } else {
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            return queryBuilder.query();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryPageByConditions() failed, ex = " + e);
            return null;
        }
    }

    /** {@code column >= value}（原厂 {@code j.a(String, Object, boolean)}）。 */
    private List<T> queryGreaterOrEqual(String column, Object value, boolean ascending) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            queryBuilder.where().ge(column, value);
            queryBuilder.orderBy(column, ascending);
            return queryBuilder.query();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryGreaterOrEqual() failed, ex = " + e);
            return null;
        }
    }

    /** {@code column <= value}（原厂 {@code j.b(String, Object, boolean)}）。 */
    private List<T> queryLessOrEqual(String column, Object value, boolean ascending) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            queryBuilder.where().le(column, value);
            queryBuilder.orderBy(column, ascending);
            return queryBuilder.query();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryLessOrEqual() failed, ex = " + e);
            return null;
        }
    }

    /** {@code column < value} 的删除行数（原厂 {@code j.b(String, Object)}）。 */
    private int deleteLessThan(String column, Object value) {
        try {
            DeleteBuilder<T, Integer> deleteBuilder = getDao().deleteBuilder();
            deleteBuilder.where().lt(column, value);
            return deleteBuilder.delete();
        } catch (SQLException e) {
            LogUtil.e(TAG, "deleteLessThan() failed, ex = " + e);
            return 0;
        }
    }

    /** 多条件删除（原厂 {@code j.a(Map)}）。 */
    private boolean deleteByConditions(Map<String, Object> conditions) {
        try {
            DeleteBuilder<T, Integer> deleteBuilder = getDao().deleteBuilder();
            Where<T, Integer> where = deleteBuilder.where();
            boolean first = true;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (first) {
                    where.eq(entry.getKey(), entry.getValue());
                    first = false;
                } else {
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            return deleteBuilder.delete() > 0;
        } catch (SQLException e) {
            LogUtil.e(TAG, "deleteByConditions() failed, ex = " + e);
            return false;
        }
    }

    /** 多条件查列表（原厂 {@code j.c(Map)}）。 */
    private List<T> queryByFieldValues(Map<String, Object> conditions) {
        try {
            return getDao().queryForFieldValuesArgs(conditions);
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryByFieldValues() failed, ex = " + e);
            return null;
        }
    }

    /** 只查指定列（原厂 {@code j.a(String[])}）。 */
    private List<T> queryColumns(String[] columns) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            queryBuilder.selectColumns(columns);
            return queryBuilder.query();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryColumns() failed, ex = " + e);
            return null;
        }
    }

    /** 多条件 + 排序取第一条（原厂 {@code j.a(Map, String, boolean)}）。 */
    private T queryForFirstByConditions(Map<String, Object> conditions, String orderByColumn,
                                        boolean ascending) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            Where<T, Integer> where = queryBuilder.where();
            queryBuilder.orderBy(orderByColumn, ascending);
            boolean first = true;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (first) {
                    where.eq(entry.getKey(), entry.getValue());
                    first = false;
                } else {
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            LogUtil.e(TAG, "queryForFirstByConditions() failed, ex = " + e);
            return null;
        }
    }

    /** 多条件计数（原厂 {@code j.b(Map)}）。 */
    private long countByConditionsSync(Map<String, Object> conditions) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            queryBuilder.setCountOf(true);
            Where<T, Integer> where = queryBuilder.where();
            boolean first = true;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (first) {
                    where.eq(entry.getKey(), entry.getValue());
                    first = false;
                } else {
                    where.and().eq(entry.getKey(), entry.getValue());
                }
            }
            return getDao().countOf(queryBuilder.prepare());
        } catch (SQLException e) {
            LogUtil.e(TAG, "countByConditions() failed, ex = " + e);
            return 0L;
        }
    }

    /**
     * 条件更新（原厂 {@code j.a(T, Map)}）：先按条件找到记录，再把 {@code entity} 的非空字段
     * 覆盖上去，最后按主键整行写回。
     */
    private boolean updateByConditions(T entity, Map<String, Object> conditions) {
        T existing = queryForFirst(conditions);
        if (existing == null) {
            LogUtil.e(TAG, "no find this data in database:" + entity);
            return false;
        }
        try {
            copyNonNullFields(entity, existing);
            return getDao().update(existing) > 0;
        } catch (Exception e) {
            LogUtil.e(TAG, "update error,update line:0", e);
            return false;
        }
    }

    /**
     * 把 {@code source} 的非空字段逐个写到 {@code target} 的同名字段上，跨类字段名一致时才有意义。
     *
     * <p>主键字段跳过：否则会把目标记录的主键改掉，update 就落到别的行上了。</p>
     */
    private void copyNonNullFields(T source, T target) throws IllegalAccessException,
            NoSuchFieldException {
        String primaryKeyFieldName = findPrimaryKeyFieldName(source);
        Class<?> targetClass = target.getClass();
        for (Field sourceField : source.getClass().getDeclaredFields()) {
            sourceField.setAccessible(true);
            Object value = sourceField.get(source);
            if (value == null || sourceField.getName().equals(primaryKeyFieldName)) {
                continue;
            }
            Field targetField = targetClass.getDeclaredField(sourceField.getName());
            targetField.setAccessible(true);
            targetField.set(target, value);
        }
    }

    /**
     * 找主键字段名：优先 {@code @DatabaseField(id/generatedId/generatedIdSequence)}，
     * 找不到就按 {@code "id"} 容错（原厂 {@code j.f(Object)} 的做法）。
     */
    private static String findPrimaryKeyFieldName(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            DatabaseField annotation = field.getAnnotation(DatabaseField.class);
            if (annotation == null) {
                continue;
            }
            boolean hasSequence = annotation.generatedIdSequence() != null
                    && annotation.generatedIdSequence().length() > 0;
            if (annotation.id() || annotation.generatedId() || hasSequence) {
                return field.getName();
            }
        }
        LogUtil.e(TAG, "primary key field not found, fallback to id");
        return "id";
    }

    /**
     * 批量操作（原厂 {@code j.a(List, int)}）：整批一个事务，逐条累加受影响行数，
     * 最后用"行数 == 列表长度"判断是否全部成功。
     *
     * <p>批量插入走 {@link DaoBase#insertAll(java.util.Collection)}，这里只处理删除与更新。</p>
     *
     * @param entities 实体列表
     * @param operator {@link #OPERATOR_DELETE} 或 {@link #OPERATOR_UPDATE}
     * @return 是否整批成功
     */
    private boolean executeBatch(final List<T> entities, final int operator) {
        try {
            return new TransactionManager(getDao().getConnectionSource())
                    .callInTransaction(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return doBatchInTransaction(entities, operator);
                        }
                    }).booleanValue();
        } catch (SQLException e) {
            LogUtil.e(TAG, "executeBatch() failed, ex = " + e);
            return false;
        }
    }

    /** 事务体：逐条执行，单条失败只记日志不中断（原厂 {@code j.b(List, int)} 的行为）。 */
    private boolean doBatchInTransaction(List<T> entities, int operator) {
        int affected = 0;
        for (T entity : entities) {
            try {
                switch (operator) {
                    case OPERATOR_DELETE:
                        affected += getDao().delete(entity);
                        break;
                    case OPERATOR_UPDATE:
                        affected += getDao().update(entity);
                        break;
                    default:
                        LogUtil.w(TAG, "no this type.");
                        break;
                }
            } catch (SQLException e) {
                LogUtil.e(TAG, "batch item failed, ex = " + e);
            }
        }
        return affected == entities.size();
    }
}
