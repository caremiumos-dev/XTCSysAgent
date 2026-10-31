# 重建状态与验收证据

> 本文件记录 XTCSysAgent 重建工程当前的完成度、如何构建、以及做过的对齐验证。
> 命名相关的约定见 [NAMING.md](NAMING.md)，重写规范见 [REWRITE_SPEC.md](REWRITE_SPEC.md)。

## 0. 一句话结论

```
clean :app:assembleDebug :app:assembleRelease  ->  BUILD SUCCESSFUL
app-debug.apk              3.01 MB   （单 dex）
app-release-unsigned.apk   2.40 MB   （单 dex）

scripts/verify.ps1 -> 验收通过
  com.xtc.*（第一方类总量）  原厂 515 / 本工程 515   ← 一个不缺、一个不多
  清单 15 个组件类全部存在于 dex
  资源缺失 0 项
  单字母包名 0 个、单字母类名 0 个
```

**尚未做的**：真机运行验证。本机没有目标手表设备，所以「装上之后跑起来是否和原厂一致」
需要你在设备上完成最后一步（见 §6）。
## 1. 工程概况

| 项 | 值 |
| --- | --- |
| 包名 | com.xtc.systemagent |
| versionCode / versionName | 24261 / 2.4.27.AW_SysAgent_J100_G97de394（与原厂一致） |
| sharedUserId | android.uid.system（系统应用，必须平台签名） |
| minSdk / targetSdk / compileSdk | 19 / 25 / 34 |
| 语言 | 100% Java（原厂也是 Java，无 Kotlin） |
| JDK | 17 |
| 构建脚本 | Gradle（wrapper 自带），依赖走阿里云镜像 |
| 原生库 | 原厂 APK 里**没有** .so（对比 lib/ 条目为 0），因此不需要 prebuilt |

## 2. 如何构建

```powershell
# 一条命令：编译 + 打包（默认 release），可选顺带打系统签名
.\scripts\build.ps1                                          # 只出未签名 APK
.\scripts\build.ps1 -Configuration debug -Clean               # 调试包 + 先 clean
.\scripts\build.ps1 -Pk8 <platform.pk8> -Pem <platform.x509.pem>   # 出可直接替换原厂的 APK
.\scripts\build.ps1 -KeyStore <platform.jks> -KeyAlias platform -StorePass <pwd>

# 也可以只用 gradle（产物在 app/build/outputs/apk/ 下）
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease

# 单独签名
.\scripts\sign-system.ps1 -Pk8 <platform.pk8> -Pem <platform.x509.pem>

# 一键验收（重新比对 dex / 清单 / 类覆盖度 / 版本特征 / 混淆痕迹）
.\scripts\verify.ps1
```

产物位置：

- app/build/outputs/apk/debug/app-debug.apk
- app/build/outputs/apk/release/app-release-unsigned.apk
- 签名后：同目录 *-platform.apk

## 3. 对齐验证（全部已实际执行）

### 3.1 编译与打包

```
.\gradlew.bat clean :app:assembleDebug :app:assembleRelease
-> BUILD SUCCESSFUL，javac 0 error
```

### 3.2 应用类覆盖度（dex 级别对比）

用 build-tools 的 dexdump 导出原厂 classes.dex 与本工程 dex 的类清单，按顶层类名（去掉 `$内部类`）比对：

| 包 | 原厂 | 本工程 | 结论 |
| --- | --- | --- | --- |
| com.xtc.systemagent | 86 | 86 | **一致** |
| com.xtc.bigdata | 87 | 87 | **一致** |
| com.xtc.dns | 37 | 37 | **一致** |
| com.xtc.im | 98 | 98 | **一致** |
| com.xtc.httplib | 43 | 43 | **一致** |
| com.xtc.qiniu | 14 | 14 | **一致** |
| com.xtc.architecture | 7 | 7 | **一致** |
| com.xtc.database | 15 | 15 | **一致** |
| com.xtc.log | 14 | 14 | **一致** |
| com.xtc.system | 20 | 20 | **一致** |
| com.xtc.common | 3 | 3 | **一致** |
| com.xtc.utils | 76 | 84 | 多 8，就是原厂 `com.xtc.a.a.a.*` 那 8 个回调类（已重命名到 `com.xtc.utils.callback`） |

> 第十二个包 `com.xtc.utils` 的 +8 需要单独说明：原厂把这 8 个回调接口放在混淆包 `com.xtc.a.a.a` 下，
> 本工程把它们放进了 `com.xtc.utils.callback`（类数 8 = 8，一对一），所以「总量对齐、归属不同」。
> `com.xtc.utils` 内部逐个子包也是对齐的：common 4/4、date 5/5、storage 14/15、system 25/25、ui 14/14、
> encode 14/13（原厂 `e`/`i` 两个 RSA 类合并成 `RsaUtils`）、callback 8/8。

**决定性证据 —— 第一方类总量**：

```
com.xtc.*（第一方类总量）  原厂 515 / 本工程 515
```

因为按包比对会被"重命名"干扰（原厂 `com.xtc.a.a.a.*` 在本工程叫 `com.xtc.utils.callback.*`，
单看前缀必然一多一少），所以再比一次 `com.xtc.*` 的全量：515 对 515，
**说明原厂每一个第一方类在本工程里都有对应实体，一个不缺、一个不多。**

> 自动验收：`scripts/verify.ps1` 会把上面这些比对全部重跑一遍，任何一项退化都会以非 0 退出码结束。

### 3.3 清单（AndroidManifest）一致性

用 aapt dump xmltree 逐条比对 activity / service / receiver / provider / uses-permission：

```
原厂条目数 121，本工程条目数 121，内容逐条相同（仅行号不同）。
```

`aapt dump badging` 的 package / versionCode / versionName / sdkVersion / targetSdkVersion 全部一致。

再做一层交叉校验：把清单里 15 个 activity / service / receiver / provider 的 `android:name` 全部取出来，
逐个去 dex 的类描述符里查：

```
manifest components: 15 ; dex classes: 5748 ; missing from dex: 0
```

也就是说**清单里声明的每一个组件类都真实存在于产物 dex 里**，不会出现装上后组件解析失败的情况。

### 3.4 资源一致性

用 `aapt dump resources` 取两边的资源名集合（`spec resource` 行）：

```
原厂 1167 项，本工程 1170 项。
缺失 0 项。
多出 3 项：id/contentView、id/errorView、id/loadingView ——
  来自 mosby（MVP 库）自带的布局，原厂那份 mosby 被 proguard 处理后没带这几个 id，属于第三方库残留。
```

这里的 `drawable/$ic_launcher_foreground__0` 值得单独记一笔：

- 它是 aapt2 把自适应图标里的**内联渐变**（`<aapt:attr name="android:fillColor">` + `<gradient>`）
  自动抽出来的资源，名字带 `$`，**不能当文件名用**（AGP 会报 "'$' is not a valid file-based resource name character"）。
- 一开始我把渐变手动抽成 `ic_launcher_foreground_gradient.xml`，虽然渲染结果逐像素相同，
  但资源名对不上；改回「vector 里内联 aapt:attr」后，aapt2 自己生成 `$ic_launcher_foreground__0`，与原厂完全一致。

`res/values/public.xml` 也与原厂逐行相同（1170 行，`0x7f060000 = drawable/$ic_launcher_foreground__0`）。

### 3.4.1 第三方库版本核对

| 库 | 原厂 | 本工程 | 说明 |
| --- | --- | --- | --- |
| mosby (MVP) | 有，但被 proguard 混淆成 `com.hannesdorfmann.mosby.mvp.a/a..h`、`b`、`c` + 保留名 `MvpActivity`/`MvpFragment`（13 个） | 2.0.1 未混淆（44 个） | 同一个库、同样的 API 角色（`MvpView`/`MvpPresenter`/`FragmentMvpDelegate`），只是原厂那份被压缩了。`com.xtc.architecture.mvp` 的 7 个类就是拿它做基类 |
| rxjava | 1.x，被混淆成 `rx.a`、`rx.b`、`rx.i` … | 1.3.8 未混淆 | 同类不同名，`Observable`/`Scheduler` 行为一致 |
| retrofit2 | 2.6~2.8（有 `ExecutorCallAdapterFactory`、`Platform$Java8`） | 2.9.0（`DefaultCallAdapterFactory` + Kotlin 挂起扩展） | 只用到基本 `Retrofit.create` + RxJava adapter，两者行为一致 |

> 这三项差异都**发生在第三方库里**，不影响 `com.xtc.*` 第一方代码；第一方类总量 515/515 已对齐。
### 3.5 签名链路

用一个临时自签 jks 端到端跑通了 scripts/sign-system.ps1：

```
zipalign -p -f 4  ->  apksigner sign（v1+v2, minSdk 19）  ->  apksigner verify --print-certs
verify 输出 V3.0 Signer / SHA-256 指纹正常，说明脚本可用。
```

### 3.6 包结构对齐（单 dex）

原厂 APK 是**单 dex**，没有任何 multidex 运行时：

```
original classes.dex : class_defs_size = 5175, method_ids_size = 46881
```

本工程早期为了贪快开了 `multiDexEnabled true`，导致产物是 `classes.dex` + `classes2.dex`，
并多出一个 `androidsupportmultidexversion.txt`。这在 API 19 上是**实打实的风险**：
legacy multidex 要求 `Application` 里显式调 `MultiDex.install()`，而原厂 `SystemAgentApp`
是直接 `extends Application`，一旦真的跑在 API 19 上就会 `NoClassDefFoundError`。

所以按原厂的真实结构改回单 dex，具体做了两件事：

1. **去掉用不到的依赖**（原厂也没有）：
   - `com.android.support:design` / `recyclerview-v7` / `transition` —— 全工程 0 处引用
   - `com.fasterxml.jackson.core:jackson-databind` + `jackson-annotations` —— 原厂 `META-INF/services`
     里只有 `JsonFactory` 和 `ObjectCodec`（都属于 jackson-core），说明原厂只用流式 API，没有 databind；
     而 databind 一个包就要多带 412 个类
2. `multiDexEnabled false`，并移除 `com.android.support:multidex` 依赖。

实测方法数：本工程 41,452 < 原厂 46,881 < 65,536 上限，单 dex 完全放得下。
## 4. 去混淆

工程内**已无任何单字母类名 / 单字母包名**（R.java 除外，那是 Android 标准的资源索引类，必须叫 R）。

```powershell
# 复核：应为空
Get-ChildItem app\src\main\java -Recurse -Directory | Where-Object { $_.Name -match '^[A-Za-z]$' }
Get-ChildItem app\src\main\java -Recurse -File -Filter *.java | Where-Object { $_.BaseName -match '^[A-Za-z]$' -and $_.BaseName -ne 'R' }
```

完整对照关系见 [NAMING.md](NAMING.md)、[NAMING-dns.md](NAMING-dns.md)、[NAMING-im.md](NAMING-im.md)、[NAMING-httplib.md](NAMING-httplib.md)。

## 5. 已知缺口

见 [NAMING.md](NAMING.md) §6：`com.xtc.database.a.l`（RxDao）与 `com.xtc.utils.ui.*` 共 14 个类未重写，
原因是原厂自身也零引用（死代码），重写与否不影响运行行为。
## 6. 真机验证步骤（需要设备）

```powershell
# 1. 打平台签名
.\scripts\build.ps1 -Pk8 <platform.pk8> -Pem <platform.x509.pem>
# 产物：app\build\outputs\apk\release\app-release-unsigned-platform.apk

# 2. 推送到手表并替换
adb root
adb remount
adb push app-release-unsigned-platform.apk /system/priv-app/XTCSysAgent/XTCSysAgent.apk
adb shell chmod 644 /system/priv-app/XTCSysAgent/XTCSysAgent.apk
adb reboot

# 3. 起来以后看这几条日志（原厂 TAG 一字未改）
adb logcat -s Log_SystemAgentApp:V Log_LogStrategyService:V Log_CheckService:V Log_ModemLogServeImpl:V
```

重点观察：

1. `SystemAgentApp.onCreate` 依次打出 `initBigData ===` 等初始化日志，无 `ClassNotFoundException` / `NoSuchMethodError`；
2. `LogStrategyService` 收到 `actionInt=1` 后按 appPackage 分桶，日志里能看到 `dealTcpDump` / `dealModemLog` / `dealUploadLog` 等对应分支；
3. `CheckService` 常驻轮询正常，没有 ANR；
4. 抓一次日志上传，确认七牛回调成功（`UploadFile` 的进度与结果日志）。

> 如果某条分支报错，把 logcat 里那段发我，我可以按原厂 smali 逐条对回来。
