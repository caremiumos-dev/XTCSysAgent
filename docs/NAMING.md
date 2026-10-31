# 去混淆命名对照表（权威版）

> 本文件是全工程改名的**唯一事实来源**。子任务写完后若与这里不一致，
> 以这里为准，并同步修改调用点（编译器会兜住漏改）。

## 1. 已落地（本人手写）

| 原类/成员 | 推断名 | 依据 |
| --- | --- | --- |
| `com.xtc.utils.date.a` | `com.xtc.utils.date.BuildConfig` | 构建常量容器，与 `com.xtc.log.BuildConfig` 同构 |
| `com.xtc.utils.date.b` | `com.xtc.utils.date.DateUtils` | SimpleDateFormat 的格式化/解析封装 |
| `com.xtc.utils.date.c` | `com.xtc.utils.date.TimeUtils` | 基于 elapsedRealtime 的开机时长与时钟校正 |
| `com.xtc.utils.date.d` | `com.xtc.utils.date.CalendarUtils` | 只有"某年某月多少天"一个方法 |
| `com.xtc.utils.storage.d` | `com.xtc.utils.storage.StorageConstants` | 存储子目录/文件名常量 |
| `com.xtc.a.a.a.*` | `com.xtc.utils.callback.*` | 回调接口簇（Error/Success/Complete + 安全分发器） |
| `com.xtc.a.a.a.a` | `com.xtc.utils.callback.ErrorCallback` | `onError(Exception)` |
| `com.xtc.a.a.a.b` | `com.xtc.utils.callback.CallbackInvoker` | 全部入口判空后转发 |
| `com.xtc.a.a.a.c` | `com.xtc.utils.callback.SuccessCallback` | `onSuccess(T)` |
| `com.xtc.a.a.a.d` | `com.xtc.utils.callback.CompleteCallback` | `onComplete()` |
| `com.xtc.a.a.a.e` | `com.xtc.utils.callback.ListCallback` | `onSuccess(List<T>)` |
| `com.xtc.a.a.a.f` | `com.xtc.utils.callback.Callback` | Error + Complete |
| `com.xtc.a.a.a.g` | `com.xtc.utils.callback.ValueCallback` | Error + Success |
| `com.xtc.a.a.a.h` | `com.xtc.utils.callback.ListResultCallback` | Error + List |
| `com.xtc.b.a` | `com.xtc.httptransmit.BuildConfig` | APPLICATION_ID 为 com.xtc.httptransmit |
| `com.xtc.b.b` | `com.xtc.httptransmit.TransmitManager` | 透传实现的注册表与转发入口 |
| `com.xtc.b.c` | `com.xtc.httptransmit.TransmitCallback` | onError(String) / onSuccess(byte[]) |
| `com.xtc.b.d` | `com.xtc.httptransmit.Transmitter` | 底层透传抽象 |
| `com.xtc.b.e` | `com.xtc.httptransmit.TransmitPlaceholder` | 空占位类 |
| `com.xtc.watch.system.a` | `com.xtc.watch.system.BuildConfig` | 构建常量 |
| `com.xtc.database.a` | `com.xtc.database.BuildConfig` | 构建常量 |
| `com.xtc.database.a.a` | `com.xtc.database.a.CollectionUtils` | 集合判空/包含 |
| `com.xtc.database.a.b` | `com.xtc.database.a.ColumnStruct` | 列名 + 列定义 |
| `com.xtc.database.a.c` | `com.xtc.database.a.SqlConstants` | SQL 片段分类编号 |
| `com.xtc.database.a.e` | `com.xtc.database.a.DatabaseHelper` | ORMLite 的 helper 单例管理 |
| `com.xtc.database.a.f` | `com.xtc.database.a.DatabaseInfo` | 库名/版本/helper |
| `com.xtc.database.a.i` | `com.xtc.database.a.DaoCallback` | 单结果回调 |
| `com.xtc.database.a.m` | `com.xtc.database.a.RxUtils` | Subscription 判空与安全取消 |
| `com.xtc.architecture.mvp.a` | `com.xtc.architecture.mvp.BasePresenter` | 只持有 ApplicationContext |
| `com.xtc.architecture.mvp.b` | `com.xtc.architecture.mvp.BuildConfig` | 构建常量 |
| `com.xtc.architecture.mvp.c` | `com.xtc.architecture.mvp.FragmentDelegate` | 改过的 mosby Fragment 委托 |
| `com.xtc.architecture.mvp.d` | `com.xtc.architecture.mvp.PageLifecycle` | i/j/k 推断为 enter/visible/leave |
| `com.xtc.log.LogUtil` 等 | 原样 | 本来就是语义名，只重写实现 |
| `com.tencent.mars.xlog.IXlog` | 原样 | 公开 SDK 接口 |

## 2. 契约（后续代码一律按此引用）

### com.xtc.utils.encode（原 a..l）—— 加密/编码
| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `BuildConfig` | 构建常量 |
| `b` | `Base64Utils` | Base64 编解码 |
| `c` | `HexUtils` | 十六进制转换 |
| `d` | `AesUtils` | AES 对称加解密 |
| `e` | `RsaUtils` | RSA 加解密/签名 |
| `f` | `JsonUtils` | `toJson(Object)` / `fromJson(String, Class)` |
| `g` | `DigestUtils` | `md5(String)` / `sha1(String)` |
| `h` | `UrlEncodeUtils` | URL 编码 |
| `i` | `CrcUtils` | 校验和 |
| `j` | `CharsetUtils` | 字符集常量 |
| `k` | `RandomUtils` | 随机串 |
| `l` | `CryptoManager` | 组合式加解密门面 |
| `R` | `R` | 库内资源 |

> 具体成员名以子任务读到的语义为准，但**类名必须与上表一致**。

### com.xtc.utils.system（原 a..t / NetworkUtils）
| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `SystemInfoUtils` | 设备/系统信息读取 |
| `b`..`n`, `q`, `s`, `t` | 按语义命名，见 NAMING-system.md | 各项系统能力封装 |
| `o` | `SystemConstants` | 系统属性键常量（`o.ae`=绑定号键，`o.T`=版本号键） |
| `p` | `SystemPropertiesUtils` | `get(key,def)` / `set(key,value)` |
| `NetworkUtils` | 原样 | 已是语义名 |

### com.xtc.utils.storage（原 a..m）
| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `BuildConfig` | 构建常量 |
| `d` | `StorageConstants` | 已落地 |
| `f` | `StorageManager` | 单例，`getInstance()` / `getBaseDir()` |
| 其余 | 见 NAMING-storage.md | |

### com.xtc.utils.common
| 原 | 新 |
| --- | --- |
| `a` | `BuildConfig` |
| `b` | `CollectionUtils`（`isEmpty(Collection)`） |
| `c` | 见 NAMING-ui-common.md |
## 3. 本轮新增落地

| 原 | 新 | 依据 |
| --- | --- | --- |
| `com.xtc.utils.system.p` | `com.xtc.utils.system.SystemPropertiesUtils` | SystemProperties 反射封装（get/set 系列） |
| `com.xtc.utils.system.o` | `com.xtc.utils.system.SystemConstants` | 属性键常量 + 硬件/项目代号 |
| `com.xtc.utils.system.m` | `com.xtc.utils.system.ShellUtils`（内部 `ExecResult`） | su/sh 命令执行，返回 code/stdout/stderr |
| `com.xtc.utils.system.t` | `com.xtc.utils.system.DeviceUtils` | 机型/地区/平台判断 |
| `com.xtc.utils.system.s` | `com.xtc.utils.system.VersionUtils` | 版本号比较与规整 |
| `com.xtc.utils.system.n` | `com.xtc.utils.system.LanguageUtils` | 取系统语言 |
| `com.xtc.utils.system.q` | `com.xtc.utils.system.TimeZoneUtils` | 时区显示名 + GMT 偏移拼装 |
| `com.xtc.utils.system.l` | `com.xtc.utils.system.ServiceUtils` | 服务是否运行 |
| `com.xtc.utils.system.k` | `com.xtc.utils.system.StepCounterUtils` | 计步换算 + sysfs 读取 |
| `com.xtc.utils.system.e` | `com.xtc.utils.system.CloseUtils` | Closeable 静默/带日志关闭 |
| `com.xtc.utils.system.c` | `com.xtc.utils.system.CameraUtils` | 相机特性查询 |
| `com.xtc.utils.system.b` | `com.xtc.utils.system.BuildConfig` | 构建常量 |
| `com.xtc.utils.system.a.a` | `com.xtc.utils.system.constant.LanguageConstants` | 语言/地区代号 |
| `com.xtc.utils.system.a.b` | `com.xtc.utils.system.constant.LanguageRegionUtils` | `lang + "_" + region` |
| `com.xtc.utils.system.a.c` | `com.xtc.utils.system.constant.OsConstants` | Android/Nucleus |
| `com.xtc.utils.system.a.d` | `com.xtc.utils.system.constant.PlatformConstants` | Qualcomm/qcom/MediaTek/Unisoc |
| `com.xtc.utils.common.a` | `com.xtc.utils.common.BuildConfig` | 构建常量 |
| `com.xtc.utils.common.b` | `com.xtc.utils.common.CollectionUtils` | 集合判空/包含 |

### 待对齐（子代理产出后统一）

`com.xtc.utils.encode` 子代理当前产出的类名：`AesUtils / Base64Utils / DesUtils / GzipUtils /
JSONUtil / Md5Utils / RandomStringUtils / UuidUtils`。这些名字比 NAMING.md 第 2 节的契约更准确，
**以子代理产出为准**，我会把 `LocalFetchSecretActivity` 等调用点统一改过来。

`com.xtc.utils.storage` 子代理产出后同样对齐；已知调用点：
`SdCardUtil`（已用 `StorageConstants`）、`SharedTool`（暂用 `SharedPreferencesUtils.getInstance(ctx)`）、
`Constants.ModemLogPath`。
## 4. `com.xtc.database` 包（本轮完成）

**包名本身也是混淆残留**：原 `com.xtc.database.a` → `com.xtc.database.core`。

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a/a` | `core.CollectionUtils` | 集合包含 / 判空 |
| `a/b` | `core.ColumnStruct` | 列名 + 列定义（toString 里带 `ColumnStruct{`） |
| `a/c` | `core.TableOperator` | 7 个 int 常量 1..7（无语义线索，按表生命周期推断） |
| `a/d` | `core.TableHandler<T>` | 单表建/升/降/清 |
| `a/e` | `core.DatabaseHelper` | 按库名单例 + 读宿主 BuildConfig 版本号 |
| `a/f` | `core.DatabaseInfo` | 库名 + 版本 + helper |
| `a/g` | `core.TableReflectUtils` | 解析 CREATE TABLE、列差异比较 |
| `a/h` | `core.DbCache` | 表数据的 LruCache（按列数计权，容量 100，tag `DbCache`） |
| `a/i` | `core.DaoCallback` | DAO 回调 |
| `a/j` | `core.DaoBase<T>` | 泛型 ORMLite DAO 基类（635 行） |
| `a/k` | `core.OrmLiteDatabaseHelper` | OrmLiteSqliteOpenHelper 封装 |
| `a/l` | `core.RxDao<T>`（未实现，见下） | RxJava 包装的 DAO，473 行 / 49 个匿名类 |
| `a/m` | `core.RxUtils` | Subscription 判空 / 取消 |
| `a` | `core`（包） | |
| `a`(顶层) | `BuildConfig` / `R` | |

已知 API：`DatabaseHelper.open(Context,String)` / `open(Context,String,int)` /
`registerTable(Class)` / `close(String)` / `getDao(Class)`。

## 5. 单字母**包名**的清除（本轮完成）

原厂把库的包名也混淆了。上一轮只改了类名，包名还是 a / b / c，仍然属于"混淆痕迹"，
本轮全部改成语义包名（只改包名，**类名 / 逻辑 / 字符串零改动**）：

### com.xtc.dns

| 原包 | 新包 | 依据 |
| --- | --- | --- |
| dns.a | dns.info | 只放 HttpDnsInfo / HttpDnsResult 两个数据对象 |
| dns.b | dns.event | 只有 DnsEventReporter |
| dns.c | dns.service | DnsConfig / DnsService / DnsServiceImpl |
| dns.c.a | dns.service.provider | DNS 数据来源抽象（Local / Tencent） |
| dns.d | dns.http | HttpEngine / HttpUrlConnectionEngine |
| dns.e | dns.storage | 内存 + ORMLite 两级存储 |
| dns.e.a | dns.storage.db | 库表 / DAO |
| dns.f | dns.config | DES 加解密 + httpdnsinfo.properties 读写 |
| dns.client.a | dns.client.event | client 侧事件上报副本 |
| dns.client.b | dns.client.contract | 跨进程 Uri / 列名契约 + DnsResolver |

### com.xtc.im

| 原包 | 新包 | 依据 |
| --- | --- | --- |
| im.a | im.bridge | 桥接服务接口 |
| im.b | im.transpond | 透传适配器 / 回调 / 管理器 |
| im.core.app.a | im.core.app.listener | PushMessageListener |
| im.core.common.a | im.core.common.listener | 4 个监听接口 |
| im.core.common.b | im.core.common.manager | BaseManager / ManagerFactory |
| im.core.common.c | im.core.common.tlv | TLV 编解码簇 |

## 6. 明确不重写的两个类（有依据的取舍）

下面两处是"原厂自己也没有任何调用方"的死代码。重写它们对"可用 / 稳定 / 可替换原厂 APK"
零影响，而实现成本高于收益，因此**登记为已知缺口**（将来若要 100% 字节级对齐再补）：

| 原类 | 位置 | 判定依据 |
| --- | --- | --- |
| com.xtc.database.a.l（473 行，49 个匿名类） | RxJava 包装的 DAO | 全仓库反编译源码里除自身外零引用；DaoBase 已提供等价的同步 API |
| com.xtc.utils.ui.*（12 个字母类 + BarUtils，共 13 个） | 状态栏 / 软键盘 / 尺寸等 UI 工具 | 全仓库反编译源码里对 com.xtc.utils.ui. 的引用数为 0；本应用是后台系统服务，无界面依赖 |

> 复核方式（在反编译源码根目录跑）：
> `Get-ChildItem -Recurse -Filter *.java | Select-String 'com[.]xtc[.]utils[.]ui[.]'` → 0 命中
> `Get-ChildItem -Recurse -Filter *.java | Select-String 'com[.]xtc[.]database[.]a[.]l\b'` → 仅 l.java 自身命中

## 7. 本轮补齐的工具库类

### com.xtc.utils.common

| 原 | 新 | 依据 |
| --- | --- | --- |
| `c`（184 行） | `StringUtils` | 整类都是字符串/字符判断与清洗：IP、URL、ASCII、数字、中文、显示宽度、全角转半角、标点归一、码点串互转 |

成员级映射（原 → 新）：`a(int)`→`getSpaceString`、`a(char)`→`isDigit`、`a(String)`→`isIpAddress`、
`a(List)`→`isEmpty`、`b(int)`→`randomAlphaNumeric`、`b(char)`→`isChineseChar`、`b(String)`→`isAllDigits`、
`c(int)`→`toTwoDigits`、`c(char)`→`isAscii`、`c(String)`→`startsWithDigit`、`d`→`isUrl`、
`e`→`toCharCodeString`、`f`→`fromCharCodeString`、`g`→`toHalfWidth`、`h`→`removeIllegalChars`、
`i`→`normalizeSymbols`、`j`→`containsChinese`、`k`→`getDisplayWidth`、`l`→`truncateByDisplayWidth`、
`m`→`isChineseAlphaNumeric`；字段 `a`→`TAG`。

### com.xtc.utils.encode —— 核对后确认无缺口

逐个读过原字母类后确认：现有实现已 100% 覆盖，**不需要新增类**。

| 原 | 实际是什么 | 已被谁覆盖 |
| --- | --- | --- |
| `a` | AES/ECB/PKCS5Padding 的 4 个静态入口 | `AesUtils` |
| `b`（251 行） | Base64（128 项解码表 + 64 项编码表 + `=` 补齐 + 空白剔除） | `Base64Utils` |
| `d` | 实例式 DES | `DesUtils` |
| `e` | RSA 加密 | `RsaUtils` |
| `i` | RSA（内置公钥常量 + PKCS1 四向 + SHA1WithRSA 验签 + selfTest） | `RsaUtils` |
| `f` | Gson 封装 | `JSONUtil` |
| `g` | MD5 | `Md5Utils` |
| `h` | 文件大小/读写工具 | `FileUtils` |
| `j` | 随机字母串 | `RandomStringUtils` |
| `k` | UUID | `UuidUtils` |
| `l` | zip 打包/解包 | `ZipUtils` |

> 备注：原厂 `e` 与 `i` 是两个 RSA 类，我们合并进一个 `RsaUtils`（方法级逐一对应，
> 含内置 `PUBLIC_KEY` 常量与 `selfTest` 入口），因此本包类数比原厂少 1，功能无缺口。

## 8. com.xtc.utils.system 剩余 7 个字母类（本轮完成）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a`（含嵌套 `a.C0024a`） | `SystemInfoUtils` + 内部类 `AppInfo` | 应用包信息查询 + 安装/卸载/清数据入口；嵌套类是一条应用档案（包名/名称/图标/APK 路径/版本名/版本码/是否系统应用） |
| `d` | `CleanUtils` | 清理内部缓存 / files / databases / shared_prefs / 外部缓存 + deleteDatabase |
| `f` | `DeviceInfoUtils` | 设备标识（IMEI / MAC / `/proc/bbksn` / SERIAL）+ 硬件状态（CPU / 内存 / 电量 / 充电 / 震动）。注意 `DeviceUtils` 已被原 `t`（机型/地区）占用，两者内容完全不同 |
| `g` | `IntentUtils` | 全是 Intent 构造：关机、启动、拍照、打开文件、分享图文、组件跳转、卸载、详情页、分享文本 |
| `h` | `FileUtils` | 原厂就是包内可见的小文件 helper（toFile / 存在判定 / 扩展名 / 递归删除 / 空串判定），保持 class 级包内可见 |
| `i` | `ForegroundAppUtils` | 查当前前台应用包名，按 API <21 / 21 / 22+ 分三路（RunningTasks / RunningAppProcesses / UsageStatsManager） |
| `j`（含嵌套接口 `j.a`） | `ResourceUtils` + 接口 `CursorHandler` | 跨包 Resources 取 color/dimen/drawable/string/array/raw，附带两个对 `.db` 文件的裸 execSQL / rawQuery |

### 需要留意的还原点

- `a` 的实例方法 `k(Context)` 在原类里**永远不可达**（构造器私有且直接抛异常），为保持 API 形状仍以实例方法
  `getForegroundPackageName(Context)` 保留，Javadoc 注明请改用 `ForegroundAppUtils`。
- `a` 的 `o(Context)` 反编译出的 try/catch 是残缺的（外层 catch 缺 return），已按可见行为还原为
  "取 label，异常或空串都返回空串、不打堆栈"。
- `f` 的 `!f.class.desiredAssertionStatus()` 是 javac 的 `$assertionsDisabled`，已还原为 `assert batteryIntent != null;`（语义等价）。
- 原厂异常文案 `"u can't fuck me..."`（`g` 构造器）与 `"Are u ok ?"`（`f` 构造器）按"字符串不改"原则原样保留，并注明不会触发。
- `NotificationFlag.NOTIFICATION_FLAG_CUSTOM`（值 `268435456`）与拍照 flag `268435457`（`0x10000001`）分别走常量与字面量，与原厂一致。
- `queryUsageStats` 的区间参数保留字面量 `2`（是 `INTERVAL_MONTHLY`，**不是** `INTERVAL_DAILY`）。

### 本轮顺带补齐

| 原 | 新 | 依据 |
| --- | --- | --- |
| `com.xtc.database.a.h` | `com.xtc.database.core.DbCache` | 表数据的 LruCache（按列数计权、容量 100、tag `DbCache`） |
| `com.xtc.log.R` | `com.xtc.log.R`（手写） | 库内资源索引，只有 `string/app_name = 0x7f0b0028` |
