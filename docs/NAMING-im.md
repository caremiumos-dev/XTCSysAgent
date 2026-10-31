# 去混淆命名对照表 —— `com.xtc.im` 包

> 作用域：`com.xtc.im`（98 个文件，与原厂 decompiled 目录逐文件一一对应）。包外调用点只有 `com.xtc.httplib`（`TranspondManager` /
> `TranspondAdapter` / `TranspondCallback` / `PushResponse`），其余类在本 APK 内没有调用者
> （IM 的真正使用方是外部进程，只通过 AIDL 交互），因此内部改名不影响已写代码。
>
> **`com.xtc.im.b` 下的 3 个文件按上级要求保持原样，未做任何改动。**

## 0. 命名依据说明

- 绝大多数实体类的 `toString()` 里带着字段语义（原厂手写的调试字符串），字段名即由此还原；
  少数没有 `toString()` 的类（如 `ThirdSyncResponseEntity`）按同族实体（`SyncResponseEntity`、
  `ThirdSyncRequestEntity`）的 tag 序列对齐推断。
- AIDL 类的语义来自其 descriptor 字符串本身（`com.xtc.im.aidl.IBridgeService` 等）。
- 找不到任何语义证据的纯常量容器，用中性但可读的名字，并在类注释里写明“原类名已混淆，
  按用途推断”。

## 1. 顶层与公共

| 原 | 新 | 依据 |
| --- | --- | --- |
| `com.xtc.im.a.a` | `com.xtc.im.a.IBridgeService` | AIDL，descriptor `com.xtc.im.aidl.IBridgeService` |
| `com.xtc.im.a.a.AbstractBinderC0011a` | `IBridgeService.Stub` | AIDL Stub |
| `com.xtc.im.a.a.AbstractBinderC0011a.C0012a` | `IBridgeService.Stub.Proxy` | AIDL 远端代理 |
| `com.xtc.im.a.a.AbstractBinderC0011a.a..j` | `TRANSACTION_*` / `DESCRIPTOR` | 事务码 + 描述符常量 |
| `com.xtc.im.common.a` | `com.xtc.im.common.BuildConfig` | 构建常量容器 |
| `com.xtc.im.common.R` | `com.xtc.im.common.R` | 库内资源（原样） |
| `com.xtc.im.core.app.a` | `com.xtc.im.core.app.ImCoreConstants` | IM Core 进程名 / 广播 action / 后缀常量 |
| `com.xtc.im.core.app.a.a` | `com.xtc.im.core.app.PushMessageListener` | `onPushMessage(PushMessage)` |
| `com.xtc.im.core.common.a`（类） | `com.xtc.im.core.common.ImLogTag` | `"IM-Core"` / `"IM-Core-"+x` 的日志 tag 工厂 |
| `com.xtc.im.core.common.a`（子包） | 包名保持 `com.xtc.im.core.common.a` | 里面的 AIDL 类改名见 §2；包名不改，避免与外部 APK 的类名约定冲突 |

> `com.xtc.im.core.common` 下同时有一个**同名**的类 `a`（→ `ImLogTag`）和子包 `a`
> （→ `ImMessageListener` 等），Java 里两者不冲突，但读代码时容易看错，这里特别标出。

## 2. `core.common.a`（AIDL 回调簇）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `core.common.a.a` | `ImMessageListener` | descriptor `com.xtc.im.core.common.listener.IMessageListener` |
| `core.common.a.a.AbstractBinderC0015a` | `ImMessageListener.Stub` | AIDL Stub |
| `core.common.a.a.AbstractBinderC0015a.C0016a` | `ImMessageListener.Stub.Proxy` | 远端代理 |
| `core.common.a.a.Stub.a(boolean,boolean,int,long,String)` | `onMessageSent(boolean, boolean, int, long, String)` | 一次消息投递结果回执 |
| `core.common.a.b` | `OnFinishListener` | descriptor `com.xtc.im.core.common.listener.OnFinishListener` |
| `core.common.a.b.a` | `OnFinishListener.Stub` | AIDL Stub |
| `core.common.a.b.a`（方法） | `onSlice(long, String)` / `onFinish()` / `onError(String)` | 事务码 1/2/3 顺序 |
| `core.common.a.c` | `CompleteListener` | `void a()`，多响应收齐后回调一次 |
| `core.common.a.d` | `PushResponseListener` | `a(PushRequest, PushResponse)` |

## 3. `core.common.b`（管理器）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `core.common.b.a` | `BaseManager` | 抽象基类，构造里把自己注册进工厂 |
| `core.common.b.b` | `ManagerFactory` | 弱引用单例注册表 |

成员：`b.a()`→`ManagerFactory.getInstance()`，`b.a(Class)`→`get(Class)`，`b.a(a)`→`register(BaseManager)`。

## 4. `core.common.c`（TLV 协议栈）

| 原 | 新 | 依据 |
| --- | --- | --- |
| `c.a` | `TLVFrameBuffer` | 继承 `ByteArrayOutputStream`，按 TLV 头算帧长并切帧 |
| `c.a.a()` | `hasCompleteFrame()` | 缓冲区里是否已有一整帧 |
| `c.a.b()` | `takeFrame()` | 取出一整帧并保留剩余字节 |
| `c.a.c()/d()/e()/f()` | `prepare()/parseFirstTagSize()/parseFirstLengthSize()/parseFirstTotalSize()` | 首帧三段长度推断 |
| `c.b` | `TLVRegistry` | 解码结果 / 编码结果 / 字段映射三张注册表的门面 |
| `c.c` | `TLVDecodeResult` | `toString` 里就写着 `TLVDecodeResult` |
| `c.d` | `TLVCodec` | TLV 字节 ↔ 结构 的解析 |
| `c.e` | `TLVDecodeEntry` | 原始字节 → 解码结果 的注册项（带命中计数） |
| `c.f` | `TLVEncodeResult` | `toString` 里就写着 `TLVEncodeResult` |
| `c.g` | `TLVEncoder` | 组 tag/length/value 三段字节 |
| `c.h` | `TLVEncodeEntry` | tag+类型+值 → 编码结果 的注册项（带命中计数） |
| `c.i` | `TagFieldRegistry` | 单个实体类名 → (tag, Field) 映射 |
| `c.j` | `TLVObject` | 日志 tag 就是 `TLVObject`；TLV 对象构造器 |
| `c.k` | `TLVNumberUtils` | 大端 byte[] ↔ long |

## 5. `core.common.request`

| 原 | 新 | 依据 |
| --- | --- | --- |
| `request.a` | `ImCommand` | 命令号常量簇（1..116、1000） |
| `request.b` | `DeviceInfoProvider` | 12 个 String getter，与 `RegistRequestEntity` 的 12 个字符串字段一一对应 |
| `request.c` | `RawPayloadEntity` | `@CommandValue(32)`，只有一个 tag=10 的 `byte[]` |
| `request.d` | `ImEntity` | 所有请求/响应实体的基类（日志 tag `Entity`） |
| `request.d.a(String)` | `getTagValue(String)` | 按字段名取 tag 号 |
| `request.d.b()` | `getCommand()` | 取 `@CommandValue` |
| `request.d.c()` | `getDeclaredFields()` | 带缓存的反射字段表 |
| `request.d.d()` | `encode()` | 编码成 TLV 字节 |
| `request.e` | `OptionConstants` | 4 个取值常量，无使用点，按取值命名 |
| `request.f` | `ContentTypeConstants` | 6 个取值常量，无使用点，按取值命名 |
| `request.g` | `RequestIdGenerator` | `前缀*100000+自增` 的请求号生成器 |
| `request.h` | `CommandRegistry` | 命令号 → 实体类 / 请求→响应 映射 |
| `request.i` | `EntityCodec` | 实体 ↔ TLV 字节（日志 tag `TLVObjectUtil`） |
| `request.PushRequest` | 原样 | 本来就是语义名 |
| `request.PushRequest.a` | `PushRequest.Builder` | 内部 Builder |
| `request.entity.RequestEntity` | 原样 | |

### 5.1 实体字段（request）

| 类 | 原字段 → 新字段（tag） |
| --- | --- |
| `RegistRequestEntity` | c→rid(1), d→platform(10), e→appKey(11), f→pkgName(12), g→deviceId(13), h→sdkVersion(14), i→sysName(15), j→sysVersion(16), k→imei(17), l→imsi(18), m→mac(19), n→modelNumber(20), o→basebandVersion(21), p→buildNumber(22), q→resolution(23) |
| `LoginRequestEntity` | c→rid(1), d→registId(10), e→registToken(11), f→sdkVersion(12), g→apnsType(15), h→apnsToken(16), i→deviceToken(17), j→imSdkVersionName(18), k→pushType(19), l→platform(20), m→timestamp(21) |
| `AccountRequestEntity` | c,d（常量）→ `BUSINESS_TYPE_1/BUSINESS_TYPE_2`；e→rid(1), f→registId(10), g→businessType(11), h→businessId(12), i→businessToken(13), j→status(14), k→appKey(15) |
| `HeartBeatRequestEntity` | 无字段 |
| `MessageRequestEntity` | c→rid(1), d→needResponse(2), e→dialogId(10), f→imAccountId(11), g→registId(12), h→msgType(13), i→data(14), j→msgId(15), k→contentType(18) |
| `SingleMessageRequestEntity` | c→rid(1), d→needResponse(2), e→receiverId(10), f→accountId(11), g→registId(12), h→msgType(13), i→data(14), j→msgId(15), k→contentType(16) |
| `AllSetRequestEntity` | c→rid(1), d→dialogId(10), e→imAccountId(11), f→registId(12), g→url(13), h→method(14), i→requestHeader(15), j→requestBody(16) |
| `ClearMsgRequestEntity` | c→rid(1), d→dialogId(10), e→imAccountId(11), f→registId(12), g→syncKey(13) |
| `ReadAckRequestEntity` | 同上 |
| `EncryptSetRequestEntity` | c→rid(1), d→encryptType(10), e→encryptKey(11) |
| `MessageMergeRequestEntity` | c→rid(1), d→data(10) |
| `PublicKeyRequestEntity` | c→rid(1) |
| `PushResponseAckRequestEntity` | c→rid(1), d→dialogId(10, @Deprecated), e→imAccountId(11), f→registId(12), g→syncKey(13) |
| `SyncFinishAckRequestEntity` | c→rid(1), d→dialogId(10, @Deprecated), e→imAccountId(11), f→registId(12), g→syncKey(13)（注意 `a(long)` 设的是 imAccountId、`b(long)` 设的是 @Deprecated 的 dialogId，原样保留） |
| `SyncRequestEntity` | c→rid(1), d→dialogId(10, @Deprecated), e→imAccountId(11), f→registId(12), g→syncKey(13), h→pageSize(14), i→mode(15, @Deprecated) |
| `SyncTriggerRequestEntity` | c→rid(1), d→accountId(10), e→accountToken(11), f→registId(12), g→apnsType(13), h→apnsToken(14) |
| `TranspondRequestEntity` | c→rid(1), d→url(10), e→method(11), f→body(12), g→header(13) |
| `third.AliasAndTagRequestEntity` | c→rid(1), d→registId(10), e→appKey(11), f→alias(12), g→tag(13) |
| `third.ThirdSyncFinAckRequestEntity` | c→rid(1), d→registId(10), e→appKey(11), f→alias(12), g→syncKey(13) |
| `third.ThirdSyncRequestEntity` | c→rid(1), d→registId(10), e→appKey(11), f→alias(12), g→syncKey(13), h→pageSize(14), i→mode(15, @Deprecated) |
| `third.ThirdSyncTriggerRequestEntity` | c→rid(1), d→registId(10), e→appKey(11), f→alias(12) |

基类访问器：`ImEntity` 的 `a()/a(int)` 在 `RequestEntity` 里是 `getRid()/setRid(int)`；
在 `ResponseEntity` 里是 `getRid()/setRid(int)`，另有 `b(int)`→`setCode(int)`、
`b(String)`→`setDesc(String)`、`e()`→`getCode()`、`f()`→`getDesc()`（原厂 `toString`
里就写着 `code=` / `desc=`）。

## 6. `core.common.response`

| 原 | 新 | 依据 |
| --- | --- | --- |
| `response.a` | `AliasAndTagInfo` | `toString` 里就写着 `AliasAndTagInfo` |
| `response.b` | `ResponseConstants` | 5 个状态/命令常量（3、4、1000、1001、1002） |
| `response.PushResponse` | 原样 | |
| `response.PushResponse.a` | `PushResponse.StatusCode` | 状态码常量（1..7、200、3200、4200、5200） |
| `response.PushResponse.g()` | `isAsyncPush()` | 推送型命令（11/24/112/35/8）或 200 |
| `response.PushResponse.h()` | `isLoginStatus4200()` | 命令 4 且状态 4200 |
| `response.PushResponse.i()` | `isLoginStatus3200()` | 命令 4 且状态 3200 |
| `response.PushResponse.j()` | `isEncryptStatus5200()` | 命令 34 且状态 5200 |
| `response.PushResponse.k()` | `isNormalSuccess()` | 非推送命令且状态 1 |
| `response.entity.a` | `ResponseCode` | 4 个错误码常量（20000/20001/20001/20002） |
| `response.entity.ResponseEntity` | 原样 | |

### 6.1 实体字段（response）

| 类 | 原字段 → 新字段（tag） |
| --- | --- |
| `ResponseEntity` | 无字段（`a()`→`getRid`、`b(int)`→`setCode`、`b(String)`→`setDesc`、`e()`→`getCode`、`f()`→`getDesc`） |
| `RegistResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→registId(10), g→registToken(11) |
| `LoginResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→encryptKey(10), g→encryptType(11), h→serverTime(12) |
| `AccountResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→imAccountId(10), g→accountToken(11) |
| `HeartBeatResponseEntity` | c,d,e（常量）→ `SERVER_STATUS_NORMAL/SERVER_STATUS_ABNORMAL/lastServerStatus`；f→serverStatus(10) |
| `MessageResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→syncKey(10) |
| `ErrorResponseEntity` | c→rid(1), d→code(2), e→desc(3) |
| `AllSetResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→dialogId(10), g→imAccountId(11), h→registId(12), i→syncKey(13), j→responseBody(14) |
| `ClearMsgResponseEntity`/`EncryptSetResponseEntity`/`ReadAckResponseEntity`/`SyncTriggerResponseEntity` | c→rid(1), d→code(2), e→desc(3) |
| `MessageMergeResponseEntity` | c→rid(1), d→respCode(2), e→respDesc(3)（`toString` 写的是 `respCode/respDesc`，且这两个访问器不覆写基类） |
| `PublicKeyResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→publicKey(10), g→encryptKey(11), h→encryptSign(12)（后三个无 toString，按协议形态推断） |
| `PushResponseEntity` | c→dialogId(10), d→imAccountId(11), e→registId(12), f→msgType(13), g→data(14), h→syncKey(15), i→msgId(16), j→createTime(17), k→contentType(18) |
| `SyncFinishResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→dialogId(10, @Deprecated), g→imAccountId(11), h→registId(12), i→syncKey(13) |
| `SyncInformResponseEntity` | c→dialogId(10, @Deprecated), d→imAccountId(11), e→syncKey(12) |
| `SyncResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→dialogId(10), g→imAccountId(11), h→registId(12), i→msgType(13), j→data(14), k→syncKey(15), l→msgId(16), m→createTime(17), n→contentType(18) |
| `TranspondResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→responseBody(10), g→responseHeader(11), h→statusCode(12) |
| `VoiceSliceResponseEntity` | c→dialogId(10), d→imAccountId(11), e→registId(12), f→data(13), g→receiverId(14), h→contentType(15) |
| `third.AliasAndTagResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→pkgName(10) |
| `third.ThirdSyncFinResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→appKey(10), g→alias(11), h→syncKey(12) |
| `third.ThirdSyncInformResponseEntity` | c→appKey(10), d→alias(11), e→syncKey(12) |
| `third.ThirdSyncResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→appKey(10), g→alias(11), h→data(12), i→syncKey(13), j→msgId(14), k→createTime(15) |
| `third.ThirdSyncTriggerResponseEntity` | c→rid(1), d→code(2), e→desc(3), f→appKey(10) |

## 7. `core.common.voice`

| 原 | 新 | 依据 |
| --- | --- | --- |
| `voice.a` | `ISliceSender` | descriptor `com.xtc.im.core.common.voice.ISliceSender` |
| `voice.b` | `SliceConstants` | 4 个状态常量（-1/0/1/2），无使用点，按取值推断 |
| `voice.c` | `SliceReceiver` | `toString` 里就写着 `SliceReceiver` |
| `voice.c.a(int)` | `buildVoice(int)` | 按切片下标拼回整段语音 |
| `voice.c.b()` | `isExpired()` | 180000ms 超时 |
| `voice.c.c()` | `getResourceKey()` | 末片的 resourceKey |
| `voice.d` | `SliceReceiverManager` | 日志 tag `SliceReceiverManager` |
| `voice.entity.a` | `VoiceSliceEntity` | `toString` 里就写着 `VoiceSliceEntity` |
| `voice.entity.a.InterfaceC0020a` | `VoiceSliceEntity.SliceType` | 1=中间片、2=末片（`h()==2` 才带 resourceKey） |
| `voice.entity.VoiceDescEntity` | 原样 | |

### 7.1 字段

| 类 | 原字段 → 新字段（tag） |
| --- | --- |
| `VoiceSliceEntity` | c→groupId(10), d→index(11), e→voc(12), f→vocTime(13), g→isFin(14), h→gdLatitude(15), i→gdLongitude(16), j→gdRadius(17), k→bdLatitude(18), l→bdLongitude(19), m→bdRadius(20), n→ggLatitude(21), o→ggLongitude(22), p→ggRadius(23), q→extra(24) |
| `VoiceDescEntity` | c→vocTime(10), d→storeAddr(11), e→groupId(12), f→lastIndex(13), g→gdLatitude(14), h→gdLongitude(15), i→gdRadius(16), j→bdLatitude(17), k→bdLongitude(18), l→bdRadius(19), m→ggLatitude(20), n→ggLongitude(21), o→ggRadius(22), p→resourceKey(23), q→deadline(24), r→extra(25) |

## 8. `common.a` / `core.app.a` 常量

| 原 | 新 |
| --- | --- |
| `common.a.a` | `BuildConfig.DEBUG`（false） |
| `common.a.b` | `BuildConfig.APPLICATION_ID`（`com.xtc.im.common`） |
| `common.a.c` | `BuildConfig.BUILD_TYPE`（`release`） |
| `common.a.d` | `BuildConfig.FLAVOR`（`""`） |
| `common.a.e` | `BuildConfig.VERSION_CODE`（10000） |
| `common.a.f` | `BuildConfig.VERSION_NAME`（`1.0.0`） |
| `common.a.g` | `BuildConfig.GitBranch`（`master`） |
| `common.a.h` | `BuildConfig.GitSHA`（`5f34bb0`） |
| `common.a.i` | `BuildConfig.GitTag`（`v3.0.0-20-g5f34bb0`） |
| `common.a.j` | `BuildConfig.JenkinsName`（`Local`） |
| `common.a.k` | `BuildConfig.JenkinsRevision`（`0`） |
| `core.app.a.a..v` | `ImCoreConstants.CORE_PACKAGE_NAME / ACTION_* / IM_DATA_SUFFIX` |

## 9. 必须保持原样（未改名）

- `com.xtc.im.b.*`：`TranspondAdapter` / `TranspondCallback` / `TranspondManager` ——上级已写好，**未改动**。
- 包名 `com.xtc.im.core.common.anotation` 的拼写错误（`anotation`）：原厂如此，改包名会破坏
  与外部 APK 的类名约定，保持原样。
- 所有 `toString()` 字面量（含原厂的 `sdkVerison`、`reciverId`、`resoureKey` 拼写错误）、
  日志 tag 与日志内容、AIDL descriptor 与事务码、`@TagValue` / `@CommandValue` 数值。
- `TLVObject.toString()` 里 `new BigInteger(1, bytes).toString(2)` 这类“看着怪但必须一致”的写法。

## 10. AIDL 形态的特殊处理（`com.xtc.im.b` 的 ITranspondCallback）

原厂 `com.xtc.im.b.a` 是 AIDL（descriptor `com.xtc.im.transpond.ITranspondCallback`，
`onSuccess(byte[], int)` / `onError(String)`）。上级已把它简化成单方法
`TranspondCallback.onTranspondFinished()`，并且要求 **不再改动 `com.xtc.im.b` 下已有的 3 个文件**。

因此 AIDL 事务被放到 `com.xtc.im.a.IBridgeService` 里，作为一个新增的嵌套类
`IBridgeService.TranspondCallbackBinder`（`implements TranspondCallback`）：

- 它保留原厂的两个事务码与 descriptor，`onSuccess` 仍按 AIDL 的收尾顺序
  （`writeNoException` → `writeByteArray`，调用方 `readException` → `readByteArray`）实现；
- `onTranspondFinished()` 是简化契约的唯一方法，**刻意留空**（宿主侧的
  `PreprocessorInterceptor.TranspondListener` 已经自包含地还原了 AIDL 事务，
  这个方法只用于满足工程内的类型约束）；
- `IBridgeService.transpond(...)` 的第五个参数类型是 `TranspondCallback`，
  序列化时按 `callback instanceof IBinder` 判断：真实运行期对象
  （`TranspondListener extends Binder`）能拿到 Binder，就照原厂写法
  `writeStrongBinder`；拿不到时写 null，**事务布局不变**。

这样既没有改动 `com.xtc.im.b`，又把原厂的 AIDL 形态完整保留在 im 包内。

## 11. 交付清单（98 个文件）

```
a/IBridgeService.java
common/BuildConfig.java                          common/R.java
core/app/ImCoreConstants.java                    core/app/a/PushMessageListener.java
core/app/bean/ImMessage.java                     core/app/bean/ImMessageData.java
core/app/bean/PushMessage.java                   core/common/ImLogTag.java
core/common/a/CompleteListener.java              core/common/a/ImMessageListener.java
core/common/a/OnFinishListener.java              core/common/a/PushResponseListener.java
core/common/anotation/CommandValue.java          core/common/anotation/TagValue.java
core/common/b/BaseManager.java                   core/common/b/ManagerFactory.java
core/common/c/TLVCodec.java                      core/common/c/TLVDecodeEntry.java
core/common/c/TLVDecodeResult.java               core/common/c/TLVEncodeEntry.java
core/common/c/TLVEncodeResult.java               core/common/c/TLVEncoder.java
core/common/c/TLVFrameBuffer.java                core/common/c/TLVNumberUtils.java
core/common/c/TLVObject.java                     core/common/c/TLVRegistry.java
core/common/c/TagFieldRegistry.java
core/common/request/CommandRegistry.java         core/common/request/ContentTypeConstants.java
core/common/request/DeviceInfoProvider.java      core/common/request/EntityCodec.java
core/common/request/ImCommand.java               core/common/request/ImEntity.java
core/common/request/OptionConstants.java         core/common/request/PushRequest.java
core/common/request/RawPayloadEntity.java        core/common/request/RequestIdGenerator.java
core/common/request/entity/AccountRequestEntity.java ... 共 18 个（见 §5.1）
core/common/request/entity/third/*.java          共 4 个
core/common/response/AliasAndTagInfo.java        core/common/response/PushResponse.java
core/common/response/ResponseConstants.java
core/common/response/entity/*.java              共 21 个（见 §6.1）
core/common/response/entity/third/*.java        共 5 个
core/common/voice/ISliceSender.java              core/common/voice/SliceConstants.java
core/common/voice/SliceReceiver.java             core/common/voice/SliceReceiverManager.java
core/common/voice/entity/VoiceDescEntity.java    core/common/voice/entity/VoiceSliceEntity.java
b/TranspondAdapter.java  b/TranspondCallback.java  b/TranspondManager.java   （上级原样，未改）
```

自检方式：用 `android-34/android.jar` + `support-media-compat-25.4.0` + `jackson-core-2.9.10`
把 `com/xtc/im` + `com/xtc/log` + `com/tencent` 单独 `javac` 编译通过（只留 deprecation /
unchecked 提示）。没有跑整包 `gradlew`，因为其它包尚未写全。

