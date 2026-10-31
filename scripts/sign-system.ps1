#requires -Version 5.1
<#
.SYNOPSIS
    为 com.xtc.systemagent 打系统签名（platform key），产出可直接替换原厂 APK 的产物。

.DESCRIPTION
    本应用在 AndroidManifest.xml 里声明了 android:sharedUserId="android.uid.system"，
    因此必须用设备 ROM 的平台签名私钥签名，否则安装/运行时会因为 sharedUserId 与
    签名证书不匹配而被 PackageManager 拒绝（INSTALL_FAILED_SHARED_USER_INCOMPATIBLE）。

    平台签名材料通常是二选一：
      1) platform.pk8 + platform.x509.pem （AOSP build/target/product/security/ 下的标准形式）
      2) 由上面两个文件转换出来的 platform.jks / platform.keystore

    流程：zipalign -> apksigner sign -> apksigner verify。
    顺序不能反：先对齐再签名，否则 v2/v3 签名会失效。

.PARAMETER Apk
    待签名的 APK。默认取 release 未签名产物。

.PARAMETER Output
    签名后的输出路径。默认同目录下 *-platform.apk。

.PARAMETER Pk8
    platform.pk8 路径（PKCS#8 DER 私钥）。

.PARAMETER Pem
    platform.x509.pem 路径（X.509 证书）。

.PARAMETER KeyStore
    JKS/BKS 签名库路径。与 -Pk8/-Pem 二选一。

.PARAMETER KeyAlias
    JKS 里的别名。

.PARAMETER StorePass
    JKS 口令。

.PARAMETER KeyPass
    私钥口令，默认与 StorePass 相同。

.PARAMETER BuildTools
    指定 build-tools 目录。默认自动取 SDK 下版本号最大的一个。

.EXAMPLE
    .\scripts\sign-system.ps1 -Pk8 D:\keys\platform.pk8 -Pem D:\keys\platform.x509.pem

.EXAMPLE
    .\scripts\sign-system.ps1 -KeyStore D:\keys\platform.jks -KeyAlias platform -StorePass android
#>
[CmdletBinding(DefaultParameterSetName = 'PlatformKey')]
param(
    [string]$Apk,
    [string]$Output,
    [Parameter(ParameterSetName = 'PlatformKey', Mandatory = $true)]
    [string]$Pk8,
    [Parameter(ParameterSetName = 'PlatformKey', Mandatory = $true)]
    [string]$Pem,
    [Parameter(ParameterSetName = 'KeyStore', Mandatory = $true)]
    [string]$KeyStore,
    [Parameter(ParameterSetName = 'KeyStore', Mandatory = $true)]
    [string]$KeyAlias,
    [Parameter(ParameterSetName = 'KeyStore', Mandatory = $true)]
    [string]$StorePass,
    [Parameter(ParameterSetName = 'KeyStore')]
    [string]$KeyPass,
    [string]$BuildTools
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# 路径解析：脚本在 scripts/ 下，工程根是它的父目录
# ---------------------------------------------------------------------------
$root = Split-Path -Parent $PSScriptRoot
if (-not $Apk) {
    $Apk = Join-Path $root 'app\build\outputs\apk\release\app-release-unsigned.apk'
}
if (-not (Test-Path -LiteralPath $Apk)) {
    throw "APK 不存在：$Apk。请先执行  .\gradlew.bat :app:assembleRelease"
}
$Apk = (Resolve-Path -LiteralPath $Apk).Path
if (-not $Output) {
    $Output = [System.IO.Path]::ChangeExtension($Apk, $null).TrimEnd('.') + '-platform.apk'
}
$Output = [System.IO.Path]::GetFullPath($Output)

# ---------------------------------------------------------------------------
# 定位 build-tools（zipalign / apksigner 都在里面）
# ---------------------------------------------------------------------------
if (-not $BuildTools) {
    $sdk = $env:ANDROID_SDK_ROOT
    if (-not $sdk) { $sdk = $env:ANDROID_HOME }
    if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    $btRoot = Join-Path $sdk 'build-tools'
    if (-not (Test-Path -LiteralPath $btRoot)) {
        throw "找不到 build-tools：$btRoot。请用 -BuildTools 显式指定。"
    }
    # 按版本号排序取最大；版本号是 数字.数字.数字，用 [version] 排序才准确
    $BuildTools = Get-ChildItem -LiteralPath $btRoot -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'apksigner.bat') } |
        Sort-Object { try { [version]$_.Name } catch { [version]'0.0.0' } } -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $BuildTools) {
        throw "build-tools 目录下没有 apksigner：$btRoot"
    }
}

$zipalign = Join-Path $BuildTools 'zipalign.exe'
$apksigner = Join-Path $BuildTools 'apksigner.bat'
if (-not (Test-Path -LiteralPath $apksigner)) { $apksigner = Join-Path $BuildTools 'apksigner' }
foreach ($t in @($zipalign, $apksigner)) {
    if (-not (Test-Path -LiteralPath $t)) { throw "缺少工具：$t" }
}

Write-Host "[1/3] build-tools = $BuildTools"

# ---------------------------------------------------------------------------
# zipalign：4 字节对齐，先输出到临时文件
# ---------------------------------------------------------------------------
$aligned = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(),
    ('xtc-sysagent-aligned-{0}.apk' -f [guid]::NewGuid().ToString('N')))
Write-Host "[2/3] zipalign -> $aligned"
& $zipalign -p -f 4 $Apk $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign 失败，退出码 $LASTEXITCODE" }

# ---------------------------------------------------------------------------
# apksigner sign
# ---------------------------------------------------------------------------
if ($PSCmdlet.ParameterSetName -eq 'PlatformKey') {
    foreach ($f in @($Pk8, $Pem)) {
        if (-not (Test-Path -LiteralPath $f)) { throw "密钥文件不存在：$f" }
    }
    $signArgs = @(
        'sign',
        '--key', (Resolve-Path -LiteralPath $Pk8).Path,
        '--cert', (Resolve-Path -LiteralPath $Pem).Path,
        '--min-sdk-version', '19',
        '--v1-signing-enabled', 'true',
        '--v2-signing-enabled', 'true',
        '--out', $Output,
        $aligned
    )
} else {
    if (-not (Test-Path -LiteralPath $KeyStore)) { throw "签名库不存在：$KeyStore" }
    if (-not $KeyPass) { $KeyPass = $StorePass }
    $signArgs = @(
        'sign',
        '--ks', (Resolve-Path -LiteralPath $KeyStore).Path,
        '--ks-key-alias', $KeyAlias,
        '--ks-pass', ('pass:' + $StorePass),
        '--key-pass', ('pass:' + $KeyPass),
        '--min-sdk-version', '19',
        '--v1-signing-enabled', 'true',
        '--v2-signing-enabled', 'true',
        '--out', $Output,
        $aligned
    )
}

Write-Host "[3/3] apksigner sign -> $Output"
& $apksigner @signArgs
$signExit = $LASTEXITCODE
Remove-Item -LiteralPath $aligned -Force -ErrorAction SilentlyContinue
if ($signExit -ne 0) { throw "apksigner 签名失败，退出码 $signExit" }

# ---------------------------------------------------------------------------
# 自检：验签 + 打印证书指纹，便于与 ROM 里的平台证书比对
# ---------------------------------------------------------------------------
Write-Host ''
Write-Host '=== 验签结果 ==='
& $apksigner verify --min-sdk-version 19 --verbose --print-certs $Output
if ($LASTEXITCODE -ne 0) { throw "签名校验失败，退出码 $LASTEXITCODE" }

Write-Host ''
Write-Host "完成：$Output"
Write-Host ('大小：{0:N2} MB' -f ((Get-Item -LiteralPath $Output).Length / 1MB))
Write-Host ''
Write-Host '推送到设备（需先 remount /system）：'
Write-Host '  adb root; adb remount'
Write-Host ('  adb push "' + $Output + '" /system/priv-app/XTCSysAgent/XTCSysAgent.apk')
Write-Host '  adb shell chmod 644 /system/priv-app/XTCSysAgent/XTCSysAgent.apk'
Write-Host '  adb reboot'
