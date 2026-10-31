# XTCSysAgent

> `com.xtc.systemagent` 的**纯手写重建工程**。
> 原厂 APK 是一个运行在手表上的系统级后台服务（`sharedUserId="android.uid.system"`），
> 职责是：接收服务器下发的采集策略 → 按策略抓各类系统日志（modem / tcpdump / wifi / adsp dump / 掉话异常 …）
> → 打包压缩 → 上传七牛 → 回报采集状态。

本工程从**空目录**开始，参照反编译源码与 smali 资源，逐类重新实现，目标是**可编译、可签名、可替换原厂 APK**。

---

## 当前状态

| 项目 | 结果 |
| --- | --- |
| 编译 | `assembleDebug` / `assembleRelease` 均 **BUILD SUCCESSFUL**，javac **0 error** |
| 源码 | **514** 个手写 `.java`，100% Java（原厂也是 Java，无 Kotlin） |
| 第一方类对齐 | **515 / 515**，原厂每一个 `com.xtc.*` 类都有对应实体 |
| dex 结构 | **单 dex**，与原厂一致（~4.0 万方法，< 64K 上限） |
| 清单 | 121 条 / 121 条，15 个组件的类全部存在于 dex |
| 资源 | 1167 / 1170，**缺失 0** |
| 混淆痕迹 | 单字母包名 0 个、单字母类名 0 个 |
| 产物 | `app-debug.apk` 3.01 MB / `app-release-unsigned.apk` 2.40 MB |
| 真机 | uid=1000 生效，启动 / 组件 / 策略分发 / 状态上报 四条链已验证 |

---

## 快速开始

### 环境要求

| 项 | 版本 / 路径 |
| --- | --- |
| JDK | **17**（`gradle.properties` 里已钉 `D:/Program Files/Java/jdk-17`） |
| Android SDK | 通过 `local.properties` 的 `sdk.dir` 指定，需装 `platforms;android-34` + `build-tools` |
| Gradle | 用仓库自带 wrapper，无需另装 |
| 依赖源 | 全部走**阿里云镜像**（`settings.gradle` + `build.gradle`） |

### 构建

```powershell
# 一条命令（默认 release）
.\scripts\build.ps1

# 调试包 + 先清理
.\scripts\build.ps1 -Configuration debug -Clean

# 直接用 gradle 也行
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

产物位置：

```
app\build\outputs\apk\debug\app-debug.apk
app\build\outputs\apk\release\app-release-unsigned.apk
```

### 打系统签名（替换原厂必做）

本应用声明了 `sharedUserId="android.uid.system"`，**必须用平台签名**，否则装上去拿不到 system uid。

```powershell
# 平台密钥对
.\scripts\build.ps1 -Pk8 <platform.pk8> -Pem <platform.x509.pem>

# 或者已有 JKS
.\scripts\build.ps1 -KeyStore <platform.jks> -KeyAlias platform -StorePass <pwd>

# 也可以单独签名
.\scripts\sign-system.ps1 -Pk8 <platform.pk8> -Pem <platform.x509.pem>
```

脚本内部流程：`zipalign -p -f 4` → `apksigner sign`（v1+v2）→ `apksigner verify --print-certs`。

### 验收

```powershell
# 自动比对 dex 结构 / 清单组件 / 类覆盖度 / 版本特征 / 混淆痕迹
.\scripts\verify.ps1

# 也可以指定别的 APK
.\scripts\verify.ps1 -Apk app\build\outputs\apk\release\app-release-unsigned.apk -Strict
```

任何一项退化都会以**非 0 退出码**结束，方便接 CI。

---

## 工程结构

```
XTCSysAgent/
├─ app/
│  ├─ build.gradle                 单模块，AGP 8.6.0
│  └─ src/main/
│     ├─ AndroidManifest.xml       与原厂 121 条逐条对齐
│     ├─ java/com/xtc/             ← 514 个手写源文件
│     │  ├─ systemagent/           主应用：service / receiver / module / util
│     │  ├─ bigdata/               行为采集 SDK
│     │  ├─ im/                    推送与透传 SDK
│     │  ├─ dns/                   HttpDns（含七牛上传用的 XtcDns 解析器）
│     │  ├─ httplib/               OkHttp 封装 / 拦截器 / 重试
│     │  ├─ qiniu/                 七牛上传封装
│     │  ├─ database/              ORMLite 封装（DaoBase / DbCache / RxDao）
│     │  ├─ log/                   日志与崩溃采集
│     │  ├─ architecture/          自研 MVP 基类
│     │  ├─ system/                系统账号体系
│     │  └─ utils/                 common / date / encode / storage / system / ui
│     └─ res/                      资源（与原厂资源名集合一致）
├─ docs/                           见下方「文档」
├─ scripts/                        build / verify / sign / 资源生成脚本
├─ gradle/                         wrapper
├─ build.gradle  settings.gradle  gradle.properties
```

> 原厂 APK 里**没有任何 `.so`**（`lib/` 条目为 0），因此本工程不需要 `prebuilt` 目录。

---

## 与原厂的一致性

### 类覆盖度（顶层类，按包比对）

| 包 | 原厂 | 本工程 |
| --- | --- | --- |
| `com.xtc.systemagent` | 86 | 86 |
| `com.xtc.bigdata` | 87 | 87 |
| `com.xtc.im` | 98 | 98 |
| `com.xtc.httplib` | 43 | 43 |
| `com.xtc.dns` | 37 | 37 |
| `com.xtc.utils.system` | 25 | 25 |
| `com.xtc.utils.ui` | 14 | 14 |
| `com.xtc.system` | 20 | 20 |
| `com.xtc.database` | 15 | 15 |
| `com.xtc.log` | 14 | 14 |
| `com.xtc.qiniu` | 14 | 14 |
| `com.xtc.architecture` | 7 | 7 |
| `com.xtc.utils.common` / `.date` | 4 / 5 | 4 / 5 |
| `com.xtc.common` | 3 | 3 |
| **`com.xtc.*` 合计** | **515** | **515** |

> `com.xtc.utils` 单看前缀会显示 76 vs 84，原因是原厂把 8 个回调接口放在混淆包 `com.xtc.a.a.a` 下，
> 本工程重命名到 `com.xtc.utils.callback`（8 = 8）。按 `com.xtc.*` 全量比对是 **515 / 515**。

### 版本特征

```
package       com.xtc.systemagent
versionCode   24261
versionName   2.4.27.AW_SysAgent_J100_G97de394
minSdk        19      targetSdk 25      compileSdk 34
```

与原厂 `aapt dump badging` 输出逐项一致。

### 行为一致性是怎么保证的

- **字符串零改动**：所有日志文案、服务器契约串、异常文案（包括原厂那句 `"u can't fuck me..."`）逐字保留；
- **数值零改动**：魔数、flag、超时、阈值一律保留原值，必要的才抽成常量并注明来源；
- **分支顺序零改动**：例如 `LogStrategyService` 的 9 个分桶、`cleanAppData` 的非短路 `&` 与五步顺序；
- **反编译假象按真实形状还原**：例如 JADX 拆 try 块产生的非法 `catch (FileNotFoundException)`、
  自适应图标的内联 `<aapt:attr>` 渐变（`drawable/$ic_launcher_foreground__0` 必须由 aapt2 生成）。

---

## 几个关键取舍

### 1. 单 dex，不用 multidex

原厂是单 dex（5175 类 / 46881 方法），且 `SystemAgentApp` 直接 `extends Application`。
若开 `multiDexEnabled true`，在 **API 19** 上需要显式 `MultiDex.install()`，否则 `NoClassDefFoundError`。
因此剔除原厂本来就没有的依赖（`support:design`、`recyclerview-v7`、`transition`、`jackson-databind`），
把方法数压到 ~4.0 万，回到单 dex。

### 2. 命名规范

原厂把类名、方法名、甚至**包名**都混淆了。本工程全部还原成有语义的名字，
详细映射（含推断依据）见 `docs/NAMING*.md`。**工程内没有任何单字母包名或单字母类名**（`R` 类除外）。

### 3. 第三方库版本差异（不影响第一方代码）

| 库 | 原厂 | 本工程 | 说明 |
| --- | --- | --- | --- |
| mosby (MVP) | 有，被 proguard 压成 `mvp.a/a..h`、`b`、`c`（13 个） | 2.0.1 未混淆（44 个） | 同一库、同样 API 角色 |
| rxjava | 1.x，混淆成 `rx.a`/`rx.b`/`rx.i` | 1.3.8 未混淆 | 行为一致 |
| retrofit2 | 2.6~2.8（有 `ExecutorCallAdapterFactory`） | 2.9.0 | 只用 `Retrofit.create` + RxJava adapter |

---

## 真机验证记录

设备上用 **CorePatch** 绕过签名校验后 `adb install`，`dumpsys package` 显示 `userId=1000`（system uid 生效）。

| 阶段 | 内容 | 结果 |
| --- | --- | --- |
| L0 | `SystemAgentApp.onCreate` 七步初始化 | ✅ 日志齐全，无 `AndroidRuntime` 异常 |
| L1 | `CheckService` 常驻 + `AlarmManager` 自唤醒 | ✅ `dumpsys` 可见 |
| L3 | `LogStrategyService` 策略注入与 9 分桶分发 | ✅ `dealUploadLog 上传日志` 正确触发 |
| L4a | 上传链路判定无日志 → 上报状态 | ✅ 真实请求打到 `watch.okii.com`，`已通知服务器上传状态` |
| L4b | 真实打包 + 七牛上传 | ⏳ 未触发（`fileLimit=5120000`，日志量不足） |
| L5 | 原厂 A/B logcat 文本对比 | ⏳ 待做 |

真机测试步骤与判定口径见 [docs/STATUS.md §6](docs/STATUS.md)。

---

## 文档

| 文件 | 内容 |
| --- | --- |
| [docs/STATUS.md](docs/STATUS.md) | 完成度、构建/签名/验收命令、逐项验证证据、真机步骤 |
| [docs/NAMING.md](docs/NAMING.md) | 类/方法/包重命名总表（database / utils / 包名去混淆） |
| [docs/NAMING-dns.md](docs/NAMING-dns.md) | `com.xtc.dns` 映射 |
| [docs/NAMING-httplib.md](docs/NAMING-httplib.md) | `com.xtc.httplib` 映射 |
| [docs/NAMING-im.md](docs/NAMING-im.md) | `com.xtc.im` 映射 |
| [docs/REWRITE_SPEC.md](docs/REWRITE_SPEC.md) | 重写规范（命名、注释、禁止事项） |

---

## 注意

- 本工程用于**替换自有设备的系统应用**，请确认你对目标设备与固件有合法处置权。
- `local.properties`、`*.jks`、`*.pk8`、`*.pem`、构建产物均已在 `.gitignore` 中排除，**不要提交签名材料**。
- 改代码后请跑一遍 `scripts\verify.ps1`，任何一项退化都会以非 0 退出码报出来。
