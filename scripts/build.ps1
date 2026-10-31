#requires -Version 5.1
<#
.SYNOPSIS
    XTCSysAgent 一键构建脚本。

.DESCRIPTION
    把「清理 -> 编译 -> 打包 ->（可选）打系统签名」串成一条命令，避免手敲多次 gradle。

    签名是可选步骤：
      - 不传 -Pk8/-Pem 也不传 -KeyStore，就只产出未签名 APK；
      - 传了签名材料，就调用 scripts/sign-system.ps1 产出 *-platform.apk。

.PARAMETER Configuration
    debug 或 release，默认 release。

.PARAMETER Clean
    是否先执行 gradlew clean。默认不清理（增量更快）。

.PARAMETER Pk8
    platform.pk8 路径（与 -Pem 配对）。

.PARAMETER Pem
    platform.x509.pem 路径。

.PARAMETER KeyStore
    JKS/BKS 签名库路径（与 -KeyAlias/-StorePass 配对）。

.PARAMETER KeyAlias
    JKS 别名。

.PARAMETER StorePass
    JKS 口令。

.EXAMPLE
    .\scripts\build.ps1
    .\scripts\build.ps1 -Configuration debug
    .\scripts\build.ps1 -Pk8 D:\keys\platform.pk8 -Pem D:\keys\platform.x509.pem
#>
[CmdletBinding()]
param(
    [ValidateSet('debug', 'release')]
    [string]$Configuration = 'release',
    [switch]$Clean,
    [string]$Pk8,
    [string]$Pem,
    [string]$KeyStore,
    [string]$KeyAlias,
    [string]$StorePass
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# ---------------------------------------------------------------------------
# 1. 编译 + 打包
# ---------------------------------------------------------------------------
$task = if ($Configuration -eq 'debug') { ':app:assembleDebug' } else { ':app:assembleRelease' }
$gradleArgs = @($task, '--console=plain')
if ($Clean) { $gradleArgs = @('clean') + $gradleArgs }

Write-Host ("[1/2] gradlew " + ($gradleArgs -join ' '))
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { throw "gradle 构建失败，退出码 $LASTEXITCODE" }

$apk = if ($Configuration -eq 'debug') {
    Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
} else {
    Join-Path $root 'app\build\outputs\apk\release\app-release-unsigned.apk'
}
if (-not (Test-Path -LiteralPath $apk)) { throw "没有找到产物：$apk" }
Write-Host ('  产物：' + $apk)
Write-Host ('  大小：{0:N2} MB' -f ((Get-Item -LiteralPath $apk).Length / 1MB))

# ---------------------------------------------------------------------------
# 2. 可选：系统签名
# ---------------------------------------------------------------------------
$hasPlatformKey = $Pk8 -and $Pem
$hasKeyStore = $KeyStore -and $KeyAlias -and $StorePass
if (-not $hasPlatformKey -and -not $hasKeyStore) {
    Write-Host '[2/2] 未提供签名材料，跳过签名（要替换原厂 APK 必须用平台签名，见 docs/STATUS.md）'
    return
}

Write-Host '[2/2] 打系统签名'
if ($hasPlatformKey) {
    & (Join-Path $PSScriptRoot 'sign-system.ps1') -Apk $apk -Pk8 $Pk8 -Pem $Pem
} else {
    & (Join-Path $PSScriptRoot 'sign-system.ps1') -Apk $apk -KeyStore $KeyStore -KeyAlias $KeyAlias -StorePass $StorePass
}
if ($LASTEXITCODE -ne 0) { throw "签名失败，退出码 $LASTEXITCODE" }
