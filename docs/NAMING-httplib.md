# com.xtc.httplib 重写：改名与保留对照

> 本文件覆盖 `com.xtc.httplib` 本轮重写的 25 个文件（任务清单里的全部条目）。
> 全工程权威对照表仍以 `docs/NAMING.md` 为准；本文件登记的条目，需要在
> `NAMING.md` 里补一行时请照抄这里。

## 1. 本次重写的文件

| 包 | 文件 |
| --- | --- |
| `annotation` | `Timeout` |
| `bean` | `AppDomain`、`BaseSchedulerTask`、`DomainInfo`、`FrequentRequestInfo`、`ImDomain`、`NetBaseRequestParam`、`RequestTrafficInfo`、`WatchInfo` |
| `confupdate` | `ConfUpdatesInfoManager`、`HttpConfUpdate`、`SchedulerTaskArray` |
| `net` | `BaseUrlManager` |
| `okhttp` | `BaseInterceptor`、`ConfUpdateInterceptor`、`DefaultOkHttpClient`、`HttpHelper`、`HttpRequestInterceptor`、`HttpResponseInterceptor`、`MonitorInterceptor`、`PreprocessorInterceptor`、`RetryInterceptor`、`TimeoutInterceptor` |
| `util` | `DesEncryptUtil`、`SystemProperties` |

未改动（已由前序任务写好）：`BuildConfig`、`LogTag`、`R`、`ConfigOptions`、`DomainConfig`、
`HttpClient`、`HttpManager`、`bean.NetBaseResult`、`bean.AppInfo`、`bean.DeviceInfo`、
`bean.NetPushError`、`net.HttpServiceProxy`、`net.HttpRxJavaCallback`、`net.HttpSubscriber`、
`net.BaseSubscriber`、`okhttp.OnGetAppInfoListener`、`okhttp.WatchHttpResultException`、
`util.HandlerUtil`。

## 2. 包内类名：无单字母类

`com.xtc.httplib` 下的类名本来就是语义名，**没有需要改名的类**。
需要还原的是 JADX 生成的合成物，逐一处理如下。

| 原形态 | 还原为 | 依据 |
| --- | --- | --- |
| `BaseInterceptor$1`（匿名 `ResponseBody`） | `BaseInterceptor.EmptyResponseBody`（private static final 命名内部类） | 3 个抽象方法，无法用 lambda；语义就是"空响应体" |
| `PreprocessorInterceptor$TimeOutReceiver` | 保留为命名内部类 `TimeOutReceiver` | 原本已有名字，只清理 JADX 注释 |
| `PreprocessorInterceptor$TranspondListener` | 保留为命名内部类 `TranspondListener` | 同上；内部重新手写了 AIDL Stub 的事务处理（见 4.2） |
| `ConfUpdatesInfoManager` 里的 `i`/`i2`/`j`/`z`/`iArr` | `confFlags`/`delayMinutes`/`index`/`bitIndex`/`valueIndex`/`version`/`countable`/`confIds` | 参数含义由协议决定 |
| `BaseUrlManager` 里的 `str`/`str2`/`strArrSplit`/`list` | `path`/`imJoinDomain`/`imBackupJson`/`parts`/`backupList` | 同上 |
| `HttpHelper` 里的 `str`/`str2`/`str3`/`bArr` | `body`/`eebbkKey`/`aesKey`/`signKey`/`url`/`baseRequestParamJson`/`bodyBytes` | 同上 |
| `MonitorInterceptor` 里的 `i`/`j`/`z` | `errorCode`/`requestTime`/`traffic`/`countable` | 同上 |
| `PreprocessorInterceptor` 里的 `i`/`j`/`str`/`bArr`/`aVar` | `imMethod`/`delayMillis`/`url`/`bodyStr`/`listener` | 同上 |
| `DefaultOkHttpClient` 里的 `str`/`cls`/`z`/`map` | `baseUrl`/`service`/`sync`/`headers` | 同上 |
| `DesEncryptUtil` 里的 `bArr`/`i`/`i2` | `data`/`length`/`index`/`charIndex` | 同上 |
| `SystemProperties` 里的 `str`/`i`/`z` | `key`/`defaultValue`/`value` | 同上 |

## 3. 跨包依赖：使用的最终名字（务必与对应包一致）

这张表是**编译契约**，`httplib` 里的调用点已经按"新名"写死。

| 反编译原名 | 本工程使用的新名 | 调用点与成员 |
| --- | --- | --- |
| `com.xtc.utils.encode.f` | `com.xtc.utils.encode.JSONUtil` | `fromJSON(String,Class)`、`toCollection(String,Class,Class...)`、`toJSON(Object)`、`createGsonBuilder()` |
| `com.xtc.utils.encode.a` | `com.xtc.utils.encode.AesUtils` | `encrypt(String,String)`、`encrypt(byte[],String)`、`decryptToBytes(String,String)` |
| `com.xtc.utils.encode.e` | `com.xtc.utils.encode.GzipUtils` | `compress(String,String)`、`decompress(byte[])` |
| `com.xtc.utils.encode.g` | `com.xtc.utils.encode.Md5Utils` | `md5(byte[])` |
| `com.xtc.utils.encode.i` | `com.xtc.utils.encode.RsaUtils` | `encryptByPublicKey(String,String)` |
| `com.xtc.utils.encode.k` | `com.xtc.utils.encode.UuidUtils` | `uuid()` |
| `com.xtc.utils.storage.f` | `com.xtc.utils.storage.FolderManager` | `getInstance()`、`getLastAppConfigPath()`（原 `d()`）、`getConfigFilePathForNormalApp()`（原 `e()`） |
| `com.xtc.utils.system.t` | `com.xtc.utils.system.DeviceUtils` | `getDeviceModel()`（原 `a()`），用作 `model` 请求头 |
| `com.xtc.utils.system.o` | `com.xtc.utils.system.SystemConstants` | `o.ae` → `PROP_BIND_NUMBER`，`o.T` → `PROP_SOFT_VERSION` |
| `com.xtc.im.b.c` | `com.xtc.im.b.TranspondManager` | `transpond(String,int,byte[],byte[],TranspondCallback)` |
| `com.xtc.im.b.b` | `com.xtc.im.b.TranspondAdapter` | 间接使用（`TranspondManager` 内部） |
| `com.xtc.im.b.a` | `com.xtc.im.b.TranspondCallback` | **注意**：本工程这一版只声明了 `onTranspondFinished()`，原厂是 AIDL 的 `ITranspondCallback`（`onSuccess(byte[],int)` / `onError(String)`）。`httplib` 侧已用 `PreprocessorInterceptor.TranspondListener` 自包含还原了 AIDL 事务（见 4.2），不依赖该接口提供这两个方法 |
| `com.xtc.dns.api.b` | `com.xtc.dns.api.DnsStrategy` | 构造 `DnsStrategy(Context)`，同时实现 `okhttp3.Dns` 与 `okhttp3.EventListener.Factory` |
| `com.xtc.common.bigdata.BehaviorUtil` | 同名（**未重写**） | `MonitorInterceptor` 用它上报 4 个埋点；该包目前不在工程里，属于阻塞项，见第 5 节 |
| `com.xtc.im.core.common.response.PushResponse.a.h` | 内联字面量 `200` | 只为取一个常量，按规范不引用无关类 |
| `PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED` | 内联字面量 `1048576` | 同上（`peekBody` 上限 1MB） |
| `com.qiniu.android.common.Constants.UTF_8` | 内联字面量 `"utf-8"` | 同上；`CHARSET_UTF8` 常量集中一处 |
| `android.os.Process` 优先级 `10` | 保留字面量 `10` | 原 smali 就是 `const/16 0xa`，不用 `THREAD_PRIORITY_BACKGROUND` 换写法 |
| `com.xtc.system.account.constant.NotificationFlag` | 同名（已存在） | `NOTIFICATION_FLAG_HEADER` 作为 `PendingIntent` 的 flag（原厂就这么传） |

## 4. 必须保持原样的地方（**没有改名、没有"修正"**）

### 4.1 协议/序列化契约
- 所有会被 Gson 收发的字段名：`AppDomain` 13 个 `xxxDomain`、`ImDomain` 4 个 `imXxx`、
  `DomainInfo` 的 `name/domainId/appDomain/imDomain`、`NetBaseRequestParam` 10 个字段、
  `FrequentRequestInfo`/`RequestTrafficInfo` 的字段。改一个字母服务端就解析不到。
- 所有 `toString()` 的拼接格式，包括 `DomainInfo` 里 `name='...'` 与 `domainId='...'`
  之间**缺一个逗号**的原厂笔误。
- `Timeout` 注解的 4 个成员名（`all/connect/read/write`）与默认值 `-1`。
- `HttpConfUpdate.ACTION_HTTP_CONF_UPDATE`、`SCHEME_HTTP_CONF_UPDATE`，
  `ConfUpdateInterceptor.HEADER_CONF_UPDATES`/`HEADER_CONF_UPDATES_VERSION`，
  `TimeOutReceiver.ACTION_TIME_OUT`/`EXTRA_REQUEST_CODE`。
- `WatchInfo.UNKOWN = "unkown"`：原厂拼写错误，但服务端/日志按此值处理。
- `DesEncryptUtil.encKey = "<Yb0AzXu"`，以及"加密输出大写十六进制、解析表只认小写"
  这对不对称行为。

### 4.2 AIDL 回调（跨进程契约）
`PreprocessorInterceptor.TranspondListener` 必须保持：
- `DESCRIPTOR = "com.xtc.im.transpond.ITranspondCallback"`；
- 事务码 `1 = onSuccess(byte[], int)`、`2 = onError(String)`、`1598968902 = INTERFACE_TRANSACTION`；
- 成功事务要把 `byte[]` 原样回写给远端（远端会 `readByteArray`）。

这是与 IM 进程的二进制契约，改一个数字就回调不到。

### 4.3 原厂"看着不对"但必须保留的实现
| 位置 | 现象 | 为什么保留 |
| --- | --- | --- |
| `RequestTrafficInfo.setStartTime/setTotalTraffic` | 用 `addAndGet` 而不是 `set` | 改了就改变了"跨天重置"的实际数值 |
| `MonitorInterceptor.dealDayTotalTraffic` | 边界算式 `((起点/24)*3600*1000+1)*24*3600*1000` | 换成 `Calendar` 会移动跨天时刻 |
| `BaseUrlManager.tryInitUrls` | `chatUrl = appDomain.getWatchDomain()` | 聊天与主域名共用同一个值 |
| `BaseUrlManager.setImUrl` | 主域名不含 `:` 时只记日志、不改任何字段 | 保留"沿用上一次值"的语义 |
| `PreprocessorInterceptor.getRequestCode` | `get() > Integer.MAX_VALUE` 恒 false | 原厂笔误，删掉会改变赋值序列 |
| `PreprocessorInterceptor` | 看门狗 `21000ms`、等待 `20000ms`、间隔 `600000ms` | 三个数字是配合关系 |
| `MonitorInterceptor.isBootLimitTime` | 命中 `NECESSARY_BOOT_URLS` 返回 **true** | 语义反直觉，但调用点的分支就是按它写的 |
| `MonitorInterceptor.NO_NEED_MONITOR` 分支 | 命中只把错误码置 -1，仍继续遍历 | 遍历次数/日志条数与原厂一致 |
| `HttpRequestInterceptor.doHttpDns` | 异常吞掉后在 `finally` 里造 `code=1005` 的兜底响应并返回 | JADX 把赋值丢了，按 smali 还原（`move-result-object v0`） |
| `HttpHelper` | 日志 tag 复用 `LogTag.tag("DefaultOkHttpClient")` | 线上日志检索规则依赖它 |
| `HttpHelper.isEncrypt` | AppInfo 为 null 时返回 **true** | 最安全假设，实际因无 key 退化成明文 |
| `ConfUpdatesInfoManager.FLAG_RECEIVER_INCLUDE_BACKGROUND` | 是**实例**字段、值为 `16777216` | 不是 static，也不引用 `Intent` 常量 |
| `ConfUpdatesInfoManager.updateConf` | `versions[length-1-1-bitIndex]` 逆序取值；循环末尾 `return false` | 服务端按"编号越大越靠前"排列 |
| `BaseInterceptor.ENCRYPT_KEY` | `static final` 在类初始化时取一次随机 uuid 前 16 位 | 进程级密钥，冷启动轮换 |
| `HttpResponseInterceptor` | 窥探上限 1MB，重建响应时 `Content-Type` 固定 `MEDIA_TYPE` | 下游 Retrofit 依赖它 |
| `DefaultOkHttpClient.dnsStrategy` | **同一个实例**同时给 `builder.dns()` 与 `builder.eventListenerFactory()` | `DnsStrategy` 内部按 host 统计失败次数，两个实例会统计不到；原厂也只是一个实例 |
| `DefaultOkHttpClient.getCallAdapterFactory` | `sync == true` 时用 `create()`，异步才挂自定义线程池 | 参数语义与名字相反，保持原样 |

### 4.4 保留的死代码（**不要删**）
原厂存在但当前没有调用方的结构，删除会改变类初始化顺序、拦截器顺序或后续 diff，
一律原样保留：
- `SchedulerTaskArray`（整类没有使用者）；
- `httplib.util.SystemProperties`（6 个方法全 private 且无调用方，是库内预留能力）；
- `MonitorInterceptor.mainHandler`（每次都 new 但从不 post）；
- `PreprocessorInterceptor.createFromCache`（恒返回 null 的预留分支）；
- `DefaultOkHttpClient.createRetrofit`（被 `getOrCreateRetrofit` 取代的私有方法）；
- `BaseInterceptor.intercept`（返回 null 的基类占位，子类都覆写）。

## 5. 阻塞项：`com.xtc.common.bigdata.BehaviorUtil`

`MonitorInterceptor` 的 4 个埋点上报依赖
`com.xtc.common.bigdata.BehaviorUtil#customEvent(Context, String, String, String, HashMap<String,String>)`，
该包在工程里**还不存在**（`com.xtc.dns.client.a.DnsEventReporter` 也在用它，
`com.xtc.architecture.mvp.BaseActivity` 还引用了 `com.xtc.bigdata.collector.BehaviorCollector`）。

已按原样引用、未做降级（降级会丢埋点）。集成时三种处理都可接受：
1. 补写该包（最优，行为完全一致）；
2. 补一个空实现的 `BehaviorUtil`（埋点丢失，其余行为不变）；
3. 由集成方统一替换为新的埋点入口。

## 6. 顺带发现的、不属于本包的问题

隔离编译时（只带 `httplib` + 它依赖的包）出现的其它报错，供集成阶段参考：

1. `com/xtc/utils/storage/FileUtils.java:1132` ——
   `try { Log.d(TAG, content); } catch (IOException e)`：
   `Log.d` 不抛 `IOException`，javac 报"try 语句体不能抛出异常 IOException"。
   属于 `com.xtc.utils.storage` 包的真实编译错误。
2. `com/xtc/dns/api/DnsStrategy.java` 引用了 `HttpDnsManager.putCache(String,String)`，
   但 `HttpDnsManager` 里目前没有这个方法（隔离编译时是唯一一个 dns 侧报错）。
3. `com/xtc/architecture/mvp/BaseActivity.java` 引用的
   `com.xtc.bigdata.collector.BehaviorCollector` 同样属于尚未重写的
   `com.xtc.bigdata.*`，与第 5 节的 `BehaviorUtil` 是同一个阻塞源。

## 7. 自检方式

用 `javac` 做了**仅本包**的类型检查（不是 gradle 构建），
`-sourcepath` 指向工程源码、并临时给 `BehaviorUtil` 打桩：
25 个文件 **0 error**（打桩前只有行为点引用报"找不到符号"）。
即：除第 5 节的阻塞项外，本包与其它已写好的包之间的名字/签名都对得上。
