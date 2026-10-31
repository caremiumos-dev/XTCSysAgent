# com.xtc.dns 去混淆命名对照表

> 本文件记录 `com.xtc.dns` 全包（37 个文件）的改名依据，作为 `docs/NAMING.md`
> 的补充。类名/成员名一律按"调用点能读懂的语义"推断；**字符串常量、数值常量、
> 判断条件、调用顺序、日志 tag 与内容一字未改**。

## 1. 类级对照（37 / 37）

| 原全限定名 | 新全限定名 | 依据 |
| --- | --- | --- |
| `com.xtc.dns.a` | `com.xtc.dns.BuildConfig` | DEBUG/APPLICATION_ID/BUILD_TYPE/FLAVOR/VERSION_CODE/VERSION_NAME 六件套 |
| `com.xtc.dns.b` | `com.xtc.dns.DnsLogTag` | 返回 `"HttpDnsServer"` / `"HttpDnsServer-" + s`，纯 tag 工厂 |
| `com.xtc.dns.R` | `com.xtc.dns.R` | 库内独立小 R（`string/app_name=0x7f0b0028`），保留 |
| `com.xtc.dns.a.a` | `com.xtc.dns.a.HttpDnsInfo` | `toString()` 里写死 `HttpDnsInfo{...}`，且承载四项下发配置 |
| `com.xtc.dns.a.b` | `com.xtc.dns.a.HttpDnsResult` | 一次解析的产物（domain + 记录列表 + ips + 来源），供 provider 返回 |
| `com.xtc.dns.api.a` | `com.xtc.dns.api.BuildConfig` | 构建常量 |
| `com.xtc.dns.api.b` | `com.xtc.dns.api.DnsStrategy` | implements `okhttp3.Dns` + `EventListener.Factory`，即 okhttp 的 DNS 策略 |
| `com.xtc.dns.api.c` | `com.xtc.dns.api.ApiLogTag` | `"HttpDnsApi"` / `"HttpDnsApi" + s`（注意无连字符） |
| `com.xtc.dns.api.R` | `com.xtc.dns.api.R` | 库内小 R，保留 |
| `com.xtc.dns.b.a` | `com.xtc.dns.b.DnsEventReporter` | 只做 `BehaviorUtil.customEvent("apm_http_dns_req", ...)` |
| `com.xtc.dns.c.a` | `com.xtc.dns.c.DnsConfig` | 静态配置容器（ip/id/缓存时间/ttl/开关/请求前缀） |
| `com.xtc.dns.c.b` | `com.xtc.dns.c.DnsServiceImpl` | tag `DnsServiceImpl`，实现 `c`（服务接口） |
| `com.xtc.dns.c.c` | `com.xtc.dns.c.DnsService` | 服务侧能力接口（查缓存/清过期/降权/清空） |
| `com.xtc.dns.c.a.a` | `com.xtc.dns.c.a.DnsProvider` | 单一 DNS 数据来源的抽象 |
| `com.xtc.dns.c.a.b` | `com.xtc.dns.c.a.LocalDnsProvider` | tag `LocalDnsProvider`，用系统 `InetAddress` 兜底 |
| `com.xtc.dns.c.a.c` | `com.xtc.dns.c.a.TencentDnsProvider` | tag `TencentHttpDnsProvider`，查腾讯 HttpDns |
| `com.xtc.dns.client.a` | `com.xtc.dns.client.BuildConfig` | 构建常量 |
| `com.xtc.dns.client.b` | `com.xtc.dns.client.DomainInfo` | `toString()` 里写死 `DomainInfo{...}` |
| `com.xtc.dns.client.c` | `com.xtc.dns.client.HttpDnsManager` | tag `HttpDnsManager`，进程内单例门面 |
| `com.xtc.dns.client.d` | `com.xtc.dns.client.ClientLogTag` | `"HttpDnsClient"` / `"HttpDnsClient-" + s` |
| `com.xtc.dns.client.R` | `com.xtc.dns.client.R` | 库内小 R，保留 |
| `com.xtc.dns.client.a.a` | `com.xtc.dns.client.a.DnsEventReporter` | 与 `com.xtc.dns.b.DnsEventReporter` 同源同体的 client 副本 |
| `com.xtc.dns.client.b.a` | `com.xtc.dns.client.b.DnsProviderContract` | 客户端侧的 Uri 路径与列名契约（常量接口） |
| `com.xtc.dns.client.b.b` | `com.xtc.dns.client.b.DnsResolver` | tag `DnsResolver`，用 `getType` 跨进程下发写动作 |
| `com.xtc.dns.client.util.InetAddressValidator` | 同名 | 移植自 Apache Commons，本来就是语义名 |
| `com.xtc.dns.client.util.RegexValidator` | 同名 | 同上 |
| `com.xtc.dns.d.a` | `com.xtc.dns.d.HttpUrlConnectionEngine` | 用 `HttpURLConnection` 实现 GET |
| `com.xtc.dns.d.b` | `com.xtc.dns.d.HttpEngine` | 极简 GET 抽象（url / host / headers 三个重载） |
| `com.xtc.dns.e.a` | `com.xtc.dns.e.DnsStorageServiceImpl` | tag `DnsStorageServiceImpl`，内存 + ORMLite 两级存储 |
| `com.xtc.dns.e.b` | `com.xtc.dns.e.DnsStorageService` | 存储服务接口 |
| `com.xtc.dns.e.a.a` | `com.xtc.dns.e.a.DatabaseManager` | tag `DatabaseManager`，`HttpDns.db` 的 helper 单例 |
| `com.xtc.dns.e.a.b` | `com.xtc.dns.e.a.DomainModel` | `@DatabaseTable(tableName="domain_model")`，表中一行 |
| `com.xtc.dns.e.a.c` | `com.xtc.dns.e.a.DomainModelDao` | tag `DomainModelDao`，继承通用 DAO 基类 |
| `com.xtc.dns.f.a` | `com.xtc.dns.f.DesEncryptUtil` | tag `DesEncryptUtil`，DES/ECB 加解密 + 十六进制 |
| `com.xtc.dns.f.b` | `com.xtc.dns.f.HttpDnsConfigFile` | tag `Tools`，读写 `httpdnsinfo.properties` |
| `com.xtc.dns.provider.a` | `com.xtc.dns.provider.HttpDnsProviderContract` | provider 侧的 Uri/列名契约（路径带前导斜杠） |
| `com.xtc.dns.provider.HttpDnsContentProvider` | 同名 | **Manifest 里注册了它，类名不可改** |

## 2. 成员级对照

### com.xtc.dns.a.HttpDnsInfo（原 a.a，字段 a/b/c/d = Integer/Integer/String/Integer）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` / `a()` / `a(Integer)` | `cacheTime` / `getCacheTime()` / `setCacheTime(Integer)` | web 配置项 `cacheTime` |
| `b` / `b()` / `b(Integer)` | `dnsSwitch` / `getDnsSwitch()` / `setDnsSwitch(Integer)` | 配置项 `enableHttpDns`，1 开 0 关 |
| `c` / `c()` / `a(String)` | `ip` / `getIp()` / `setIp(String)` | 配置项 `httpDnsIp` |
| `d` / `d()` / `c(Integer)` | `ttl` / `getTtl()` / `setTtl(Integer)` | 配置项 `dnsTtl` |

### com.xtc.dns.a.HttpDnsResult（原 a.b）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a`（String） | `domain` | 被 `DnsStorageServiceImpl#save` 当 key 用 |
| `b`（List） | `models` | 逐条 `DomainModel` |
| `c`（String） | `code` | **包内无写入点**，按 HTTP 响应码推断（注释已说明） |
| `d`（String） | `ips` | 上报事件里作为 `ips` 字段的值 |
| `e`（int） | `source` | 本地分支写 1、腾讯分支写 0 |
| `a()/a(String)/a(int)` | `getDomain()/setDomain(String)/setSource(int)` | |
| `b()/a(List)` | `getModels()/setModels(List)` | |
| `c()/b(String)` | `getCode()/setCode(String)` | |
| `d()/c(String)` | `getIps()/setIps(String)` | |
| `e()` | `getSource()` | |

### com.xtc.dns.api.DnsStrategy（原 api.b）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `TAG` | `apiTag("DnsStrategy")` |
| `b` | `PROVIDER_PACKAGE` | 值为 `"com.xtc.i3launcher"`，provider 借用方包名 |
| `c` | `context` | |
| `d` | `connectFailedListener` | 原匿名 `EventListener` |
| `b$1`（匿名类） | 内部类 `ConnectFailedListener` | 还原成命名内部类（`EventListener` 是抽象类，不能用 lambda） |
| `b.a(Context)` 构造 | `DnsStrategy(Context)` | |
| `create` | `create` | 接口方法，不得改名 |
| `lookup` | `lookup` | 接口方法，不得改名 |

### com.xtc.dns.c.DnsConfig（原 c.a）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `k` | `TAG` | `serverTag("DnsConfig")` |
| `a` | `httpDnsIp` | `"119.29.29.29"`，被当作 ip 写日志 |
| `b` | `appId` | `"196"`，拼在 `&id=` 后 |
| `c` | `maxCacheTime` | `259200`，`deleteExpired` 的窗口 |
| `d` | `dnsTtl` | `480`，写进 `domain_ttl` |
| `e` | `httpDnsEnable` | provider `query` 先判断它 |
| `f` | `tencentRequestUrl` | `http://ip/d?dn=` 前缀 |
| `g` | `tencentAvailable` | `TencentDnsProvider#isAvailable` 返回它 |
| `h=1` / `i=0` / `j=1` | `SOURCE_LOCAL` / `SOURCE_TENCENT` / `SWITCH_ENABLE` | 与两个 provider 里写的 `1`/`0` 对应；`j` 包内无引用，按"开启"推断 |
| `a()` | `loadFromConfigFile()` | 从 sdcard 配置读 |
| `a(HttpDnsInfo)` | `apply(HttpDnsInfo)` | 应用并回写配置文件 |
| `b(String)` | `setHttpDnsIp(String)` | |
| `b(boolean)` | `setHttpDnsEnable(boolean)` | |
| `c(int)` | `setMaxCacheTime(int)` | 注意原 `c(int)` 写的是 `c` 字段（最大缓存时间） |
| `d(int)` | `setDnsTtl(int)` | |

### com.xtc.dns.c.DnsServiceImpl（原 c.b）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `TAG` | `serverTag("DnsServiceImpl")` |
| `b` | `SP_HTTP_DNS_REQUEST_COUNT` | `"http_dns_request_count"` |
| `c` / `d` / `e` / `f` / `g` / `h` / `i` | `context` / `pendingRequests` / `providers` / `connectivityManager` / `lastRequestTime` / `retryCount` / `storage` | 按字段用途 |
| `b$a` | 静态内部类 `PendingRequest` | 同域名去重的表项 |
| `a`（内部字段 Runnable） | `task` | |
| `b`（内部字段 long） | `createTime` | |
| `a()` / `b()`（内部类方法） | `start()` / `getCreateTime()` | |
| `c()` | `deleteExpiredFromDatabase()` | 调 `storage.deleteExpired(now - maxCacheTime*1000)` |
| `c(String)` | `requestDnsData(String)` | 后台线程里真正干活的那段 |
| `d(String)` | `startRequest(String)` | 去重 + 补发 |
| `d()` | `canRequest()` | 指数退避 + 网络判断 |
| `e(String)` | `resolveByProviders(String)` | 依次问 provider |
| `e()` | `isNetworkAvailable()` | |
| `a(String)` | `getDomainModel(String)` | 接口方法（语义化名称） |
| `a()` | `checkIpOverTime()` | |
| `a(String,String)` | `checkIpExpire(String,String)` | |
| `b()` | `clear()` | |
| `b(String)` | `decreasePriority(String)` | |

### com.xtc.dns.c.a.*（provider 三件套）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a.a(String)` | `DnsProvider.lookup(String)` | |
| `a.a()` | `DnsProvider.isAvailable()` | |
| `a.b()` | `DnsProvider.getBaseUrl()` | |
| `a.c()` | `DnsProvider.getSourceName()` | 返回 `"local"` / `"tencent"` |
| `c.a.b` 除接口外无额外成员 | — | `LocalDnsProvider` 只实现 `lookup/isAvailable/getBaseUrl/getSourceName` 四个方法 |
| `c.a.c#b(String)` | `TencentDnsProvider#encryptDomain(String)` | 调 `DesEncryptUtil.encrypt(domain)` |
| `c.a.c#d()` | `TencentDnsProvider#getAppId()` | 返回 `DnsConfig.appId`（每次现读，配置热更新即时生效） |

### com.xtc.dns.client.HttpDnsManager（原 client.c）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `TAG` | `clientTag("HttpDnsManager")` |
| `b` / `c` | `sInstance` / `LOCK` | |
| `d` / `e` / `f` | `context` / `domainCache` / `dnsResolver` | |
| `a(Context)` | `getInstance(Context)` | |
| `a()` | `checkIpOverTime()` | |
| `a(String)` | `initDnsResolver(String)` | 按包名选 provider，失败回落本包 |
| `a(String,String)` | `reportInvalidIp(String host, String ip)` | 原语义是"上报坏 ip"，不是"写缓存" |
| `b(String)` | `getDomainInfo(String)` | |
| `b()` | `clearAll()` | |
| `c(String)` | `clearCache(String ip)` | 清空内存缓存 + 通知过期检查 |

> 跨包调用点（qiniu `XtcDns`、httplib `DefaultOkHttpClient`）请用：
> `HttpDnsManager.getInstance(ctx).clearCache(ip)` 与
> `HttpDnsManager.getInstance(ctx).reportInvalidIp(host, ip)`。

### com.xtc.dns.client.b.DnsResolver（原 client.b.b）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `TAG` | |
| `b` / `c` / `d` | `baseUri` / `contentResolver` / `inetAddressValidator` | |
| `a()` | `checkIpOverTime()` | `getType(.../checkIpOverTime)` |
| `a(String)` | `checkIpOverTime(String ip)` | |
| `a(String,String)` | `checkIpExpire(String host, String ip)` | |
| `b(String)` | `getDomainInfo(String)` | |
| `b()` | `clear()` | |

### com.xtc.dns.client.DomainInfo（原 client.b）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` / `a()` / `b(String)` | `url` / `getUrl()` / `setUrl(String)` | `toString` 里第一个字段是 `url` |
| `b` / `c()` / `c(String)` | `host` / `getHost()` / `setHost(String)` | |
| `c` / `a()` / `a(String)` | `ip` / `getIp()` / `setIp(String)` | `DnsStrategy` 用 `getIp()` 去 `InetAddress.getByName` |
| `d` | `data` | |
| `e` | `startTime` | |
| `f` | `stopTime` | |
| `g` | `code` | |
| 构造 `(String,String)` | `(String host, String ip)` | 两个调用点都传 host 与 ip |

### com.xtc.dns.d.HttpUrlConnectionEngine（原 d.a）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a=5000` / `b=5000` | `CONNECT_TIMEOUT` / `READ_TIMEOUT` | 分别传给 `setConnectTimeout`/`setReadTimeout` |
| `a(String)` / `a(String,String)` / `a(String,HashMap)` | `get(...)` 三个重载 | |

### com.xtc.dns.e.DnsStorageServiceImpl（原 e.a）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` / `b` / `c` | `TAG` / `CACHE_INITIAL_CAPACITY` / `CACHE_LOAD_FACTOR` | `new ConcurrentHashMap<>(8, 32.0f)` |
| `d` / `e` | `memoryCache` / `domainModelDao` | |
| `a(List)` | `cache(List)` | |
| `b()` | `clearMemoryCache()` | |
| `c(String)` | `removeCache(String)` | |
| `a(String,String)` | `queryByHostAndIp(String,String)` | |
| `a(String)` / `a()` / `a(long)` / `a(HttpDnsResult)` / `b(String)` | `queryByDomain` / `clearAll` / `deleteExpired` / `save` / `decreasePriority` | 接口方法 |

### com.xtc.dns.e.a.DomainModelDao（原 e.a.c）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `c` | `TAG` | `DomainModelDao` |
| `a(long)` | `deleteCreatedBefore(long)` | `where().lt("create_time", ...)` |
| `a(String,String)` | `findByHostAndIp(String,String)` | |
| `a(String,List)` | `replaceByDomain(String,List)` | 先 `e(domain)` 删、再 `a(list)` 插 |
| `c(String)` | `queryByDomain(String)` | `orderBy("priority", false)` + `eq("domain",...)` |
| `d(String)` | `queryByIp(String)` | |
| `d(List)` | `updateInTransaction(List)` | `TransactionManager` 内逐条 `a(model,"ip",model.d())` |
| `e(String)` | `deleteByDomain(String)` | |

### com.xtc.dns.e.a.DomainModel（原 e.a.b）

字段按 ORMLite 列名反推：`a=id`、`b=domain`、`c=ip`、`d=ipTtl(ip_ttl)`、
`e=priority`、`f=sp`、`g=domainTtl(domain_ttl)`、`h=createTime(create_time)`；
getter/setter 一一对应成 `getId/setId` … `getCreateTime/setCreateTime`。

### com.xtc.dns.e.a.DatabaseManager（原 e.a.a）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` | `DATABASE_NAME` | `"HttpDns.db"` |
| `b` | `TAG` | `DatabaseManager` |
| `c` / `d` / `e` | `sInstance` / `databaseHelper` / `initialized` | |
| `a(Context)` | `getInstance(Context)` | |
| `a()` | `init()` | 建表（幂等） |

### com.xtc.dns.f.DesEncryptUtil（原 f.a）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` / `b` | `TAG` / `DES_KEY` | 密钥字面量不变 |
| `a(char)` | `hexCharToByte(char)` | `"0123456789abcdef"` 反查 |
| `a(String)` | `encrypt(String)` | |
| `a(byte[])` | `toHexString(byte[])` | |
| `b(String)` | `decrypt(String)` | |
| `c(String)` | `hexStringToBytes(String)` | |

### com.xtc.dns.f.HttpDnsConfigFile（原 f.b）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` / `b` | `TAG` / `CONFIG_PATH` | `/mnt/sdcard/xtc/ibwatch/common/httpdnsinfo.properties` |
| `a()` / `a(Properties)` / `b()` | `load()` / `save(Properties)` / `delete()` | |

### com.xtc.dns.b.DnsEventReporter 与 com.xtc.dns.client.a.DnsEventReporter

| 原 | 新 | 依据 |
| --- | --- | --- |
| `a` / `b` | `SOURCE_LOCAL` / `SOURCE_TENCENT` | `"local"` / `"tencent"` |
| `c`~$i` | `LABEL` / `EVENT_HTTP_DNS_REQUEST` / `KEY_SUCCESS`…`KEY_SOURCE` | 事件名与字段名 |
| `a(Context,String,String)` | `reportFailure(Context,String source,String host)` | 第三个参数落在 `host` 字段 |
| `a(Context,String,String,long,String)` | `reportSuccess(...)` | |

### 契约接口里的常量

- `com.xtc.dns.provider.a`：`a→AUTHORITY_FORMAT`、`b→MATCH_PATH_DOMAIN_MODEL`、
  `c→PATH_CHECK_IP_EXPIRE`、`d→PATH_CHECK_IP_OVER_TIME`、
  `e→PATH_DECREASE_IP_PRIORITY`、`f→PATH_CLEAR`；
  内部 `InterfaceC0009a→Switch`（`a→IS_HTTP_DNS_ENABLE`）、`b→Column`（`a..h→ID/DOMAIN/CREATE_TIME/DOMAIN_TTL/IP/IP_TTL/PRIORITY/SP`）。
- `com.xtc.dns.client.b.a`：`a→AUTHORITY_FORMAT`、`b→PATH_DOMAIN_MODEL`、
  `c→PATH_CHECK_IP_EXPIRE`、`d→PATH_CHECK_IP_OVER_TIME`、
  `e→PATH_DECREASE_IP_PRIORITY`、`f→PATH_CLEAR`；内部接口同上。

### provider / resolver 的成员

| 原 | 新 | 依据 |
| --- | --- | --- |
| `HttpDnsContentProvider.b` | `TAG` | `serverTag("HttpDnsContentProvider")` |
| `HttpDnsContentProvider.d` | `MATCH_DOMAIN_MODEL` | `addURI(..., 1)` 的编号 |
| `HttpDnsContentProvider.a` | `dnsService` | 包级可见，类型 `DnsService` |
| `HttpDnsContentProvider.c` | `uriMatcher` | `new UriMatcher(-1)` |
| `client.util.InetAddressValidator` 常量 | `IPV4_MAX_OCTET_VALUE(255)` / `IPV6_MAX_HEX_VALUE(65535)` / `HEX_RADIX(16，无引用点)` / `IPV4_REGEX` / `IPV6_MAX_OCTETS(8)` / `IPV6_MAX_OCTET_CHARS(4)` / `INSTANCE` / `ipv4Validator` | |
| `RegexValidator.a` | `patterns` | |

## 3. 必须保持原样、因此没改名的地方

1. **`com.xtc.dns.provider.HttpDnsContentProvider` 类名**：AndroidManifest 以
   `android:name=".dns.provider.HttpDnsContentProvider"` 注册、`exported="true"`，
   改名等于让系统起不来 provider。
2. **authority `%s.com.xtc.httpdns.provider`**（两侧契约各有一份常量）：
   实际值 `com.xtc.systemagent.com.xtc.httpdns.provider` 由宿主包名拼出。
3. **Uri 路径段**：provider 侧 `/checkIpExpire`、`/checkIpOverTime`、
   `/decreaseIpPriority`、`/clear`（带前导斜杠）；client 侧
   `DomainModel`、`checkIpExpire`、`checkIpOverTime`、`decreaseIpPriority`、`clear`
   （不带斜杠）。两侧不对称是原厂设计。
4. **游标列名与顺序**：`id, createTime, domain, domainTTL, ip, ipTTL, priority, sp`。
5. **数据库表名/列名/索引名**：`domain_model`、`id/domain/ip/ip_ttl/priority/sp/domain_ttl/create_time`、
   唯一索引 `_idx_domain_ip`、普通索引 `_id_ip`。
6. **所有日志 tag 与日志内容**：`HttpDnsServer-Xxx`、`HttpDnsApiXxx`（无连字符）、
   `HttpDnsClient-Xxx`、以及 `HXQ-TAG`（原厂写死的人名样式 tag）。
7. **字符串常量**：`"119.29.29.29"`、`"196"`、`"com.xtc.i3launcher"`、
   `"http_dns_request_count"`、`"apm_http_dns_req"` 及其字段名、`"host"`、
   `"<Yb0AzXu"`、`"DES/ECB/PKCS5Padding"`、`"0123456789abcdef"`、
   `"/mnt/sdcard/xtc/ibwatch/common/httpdnsinfo.properties"`、
   `"enableHttpDns"/"httpDnsIp"/"cacheTime"/"dnsTtl"`、`"connectivity"`、
   `"Get Http Dns Data"`（线程名）、`"local"/"tencent"`。
8. **数值常量**：连接/读超时 `5000`、指数退避上限 `10`、
   本地 ip ttl `60`、缓存初始容量 `8` + 装载因子 `32.0f`、
   API 判断 `19`、UriMatcher 的 `-1`。
9. **`InetAddressValidator` / `RegexValidator` 的类名与算法**：来自 Apache Commons，
   本身就是语义名；IPv6 的判定算法属于"边界行为"，逐行保留。
10. **`com.xtc.dns.R` / `api/R` / `client/R` 的 `string.app_name = 0x7f0b0028`**：
    与应用包的 `R.string.app_name` 同一个 id。

## 4. 跨包依赖（本包引用了、但不在本包内实现的符号）

| 符号 | 用在哪 | 说明 |
| --- | --- | --- |
| `com.xtc.log.LogUtil`（i/d/w/e，含 `e(tag,msg,Throwable)`、`e(tag,Throwable)`） | 全包 | 已存在 |
| `com.xtc.httplib.ConfigOptions.ProtocolType.HTTP` | `DnsConfig.tencentRequestUrl` | 已存在，值 `"http://"` |
| `com.xtc.utils.storage.SPUtils`（`getInstance/getInt/putInt`） | `DnsServiceImpl` | 已存在（原 `com.xtc.utils.storage.l`） |
| `com.xtc.common.bigdata.BehaviorUtil.customEvent(Context,String,String,String,HashMap)` | 两个 `DnsEventReporter` | 行为埋点，**不能改成空实现** |
| `com.xtc.database.a.DatabaseHelper`（`open(Context,String)`、`registerTable(Class)`） | `DatabaseManager` | 已由数据库负责人按此签名落地 |
| `com.xtc.database.a.DaoBase`（原 `com.xtc.database.a.j`） | `DomainModelDao` | 见下节“待对齐” |
| `com.qiniu.android.common.Constants.UTF_8` | `DesEncryptUtil` | 七牛 SDK 自带，值 `"utf-8"` |
| okhttp3 `Dns/Call/EventListener/Protocol`、support `NonNull/Nullable`、ORMLite 注解与 `TransactionManager` | 见各处 | 依赖版本已在 `app/build.gradle` 固定 |

### 待对齐

`DaoBase`（原 `com.xtc.database.a.j`）在本包里按下列语义调用，若基类最终命名不同，
**只需改 `DomainModelDao` 一个文件**：

| 原 | 本包调用名 | 语义 |
| --- | --- | --- |
| `a()` | `getDao()` | 取 `Dao<T,Integer>` |
| `a(String,Object)` | `deleteByColumn(String,Object)` | 按列删，返回 boolean |
| `a(List)` | `insertAll(List)` | 事务内批量插入 |
| `a(T,String,Object)` | `updateByColumn(T,String,Object)` | 按列条件更新 |
| `a(String,String,Object,boolean)` | `query(String orderBy,String where,Object,boolean asc)` | |
| `c(String,Object)` | `queryByColumn(String,Object)` | |
| `d(Map)` | `queryForFirst(Map)` | |
| `b()` | `clearTableWithCountCheck()` | 先 `countOf`，为 0 返回 true，否则删全部 |
