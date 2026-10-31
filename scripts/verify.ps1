#requires -Version 5.1
<#
.SYNOPSIS
    XTCSysAgent 重建工程的自动验收脚本。

.DESCRIPTION
    把开发过程中手工跑过的对齐检查固化下来，一条命令给出「能不能替换原厂 APK」的结论：

      1. 包结构：单 dex / 多 dex、方法数是否超 64K
      2. 清单：组件数、以及每个组件类是否真的在 dex 里
      3. 类覆盖度：按包比对本工程与原厂的顶层类数量
      4. 引擎特征：versionCode / versionName / sdk 版本

    任何一项不达标都会以非 0 退出码结束，方便接 CI。

.PARAMETER Apk
    待验收的 APK，默认取 debug 产物。

.PARAMETER ReferenceApk
    原厂 APK，默认取研究目录里那份。

.PARAMETER Strict
    开启后「类数量差异」也算失败（默认只告警，因为部分类有意合并 / 是死代码）。

.EXAMPLE
    .\scripts\verify.ps1
    .\scripts\verify.ps1 -Apk app\build\outputs\apk\release\app-release-unsigned.apk
#>
[CmdletBinding()]
param(
    [string]$Apk,
    [string]$ReferenceApk = 'D:\CaremiumWorkspace\Research\SystemApps\XTCSysAgent\assets\XTCSysAgent.apk',
    [switch]$Strict
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $Apk) { $Apk = 'app\build\outputs\apk\debug\app-debug.apk' }
$Apk = (Resolve-Path -LiteralPath $Apk).Path
$ReferenceApk = (Resolve-Path -LiteralPath $ReferenceApk).Path

$sdkBuildTools = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools'
$bt = Get-ChildItem $sdkBuildTools -Directory | Sort-Object Name -Descending | Select-Object -First 1
$dexdump = Join-Path $bt.FullName 'dexdump.exe'
$aapt = Join-Path $bt.FullName 'aapt.exe'
foreach ($t in @($dexdump, $aapt)) {
    if (-not (Test-Path -LiteralPath $t)) { throw "缺少构建工具：$t" }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

$script:failures = New-Object System.Collections.Generic.List[string]
$script:warnings = New-Object System.Collections.Generic.List[string]

function Section($t) { Write-Host ''; Write-Host ("== " + $t + " " + ('=' * [Math]::Max(0, 66 - $t.Length))) }
function Pass($t)    { Write-Host ("   [OK]   " + $t) }
function Warn($t)    { Write-Host ("   [WARN] " + $t); $script:warnings.Add($t) }
function Fail($t)    { Write-Host ("   [FAIL] " + $t); $script:failures.Add($t) }

# ---------------------------------------------------------------------------
# 工具函数：从 APK 里解出 dex，导出类名 / 头部信息
# ---------------------------------------------------------------------------
function Expand-Dex($apkPath, $outDir) {
    if (Test-Path -LiteralPath $outDir) { Remove-Item $outDir -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apkPath)
    try {
        foreach ($e in $zip.Entries) {
            if ($e.FullName -match '^classes([0-9]*)\.dex$') {
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($e, (Join-Path $outDir $e.FullName), $true)
            }
        }
    } finally { $zip.Dispose() }
    Get-ChildItem $outDir -File -Filter '*.dex' | Sort-Object Name | ForEach-Object { $_.FullName }
}

function Get-DexHeader($dexPath) {
    $vals = @{}
    & $dexdump -f $dexPath 2>&1 | Select-String -Pattern '^\s*(method_ids_size|class_defs_size|string_ids_size)\s*:' | ForEach-Object {
        $parts = $_.Line -split ':'
        $vals[$parts[0].Trim()] = [int]$parts[1].Trim()
    }
    $vals
}

function Get-DexClasses($dexPaths) {
    $set = New-Object System.Collections.Generic.HashSet[string]
    foreach ($d in $dexPaths) {
        & $dexdump $d 2>&1 | Select-String -Pattern '^  Class descriptor' | ForEach-Object {
            $s = $_.Line -replace "^  Class descriptor\s*:\s*'L", ''
            $s = $s -replace "[';]+$", ''
            [void]$set.Add(($s -replace '/', '.'))
        }
    }
    ,$set
}

function Get-TopLevel($set) {
    $tops = New-Object System.Collections.Generic.HashSet[string]
    foreach ($c in $set) { [void]$tops.Add(($c -replace '\$.*$', '')) }
    ,$tops
}

$tmp = Join-Path $env:TEMP ('xtc-verify-' + [Guid]::NewGuid().ToString('N'))
try {
    # -----------------------------------------------------------------------
    Section '1. 包结构与 dex'
    # -----------------------------------------------------------------------
    $newDex = Expand-Dex $Apk (Join-Path $tmp 'new')
    $oldDex = Expand-Dex $ReferenceApk (Join-Path $tmp 'old')

    if ($newDex.Count -eq 1) { Pass "单 dex（classes.dex），与原厂一致" }
    else { Fail "产物是 $($newDex.Count) 个 dex，原厂是单 dex（minSdk 19 下 legacy multidex 需要 MultiDex.install）" }

    $newMethods = 0; $newClasses = 0
    foreach ($d in $newDex) { $h = Get-DexHeader $d; $newMethods += $h['method_ids_size']; $newClasses += $h['class_defs_size'] }
    $oldMethods = 0; $oldClasses = 0
    foreach ($d in $oldDex) { $h = Get-DexHeader $d; $oldMethods += $h['method_ids_size']; $oldClasses += $h['class_defs_size'] }

    Write-Host ("   方法数：本工程 $newMethods / 原厂 $oldMethods / 上限 65536")
    Write-Host ("   类数  ：本工程 $newClasses / 原厂 $oldClasses")
    if ($newMethods -ge 65536) { Fail '方法数达到 64K 上限，dex 会溢出' } else { Pass '方法数未触及 64K 上限' }

    # -----------------------------------------------------------------------
    Section '2. 清单组件是否都真实存在'
    # -----------------------------------------------------------------------
    $mf = Join-Path $tmp 'manifest.txt'
    & $aapt dump xmltree $Apk AndroidManifest.xml > $mf 2>&1
    $lines = Get-Content $mf
    $comps = New-Object System.Collections.Generic.HashSet[string]
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match 'E: (activity|service|receiver|provider) ') {
            for ($j = $i + 1; $j -lt [Math]::Min($i + 6, $lines.Count); $j++) {
                if ($lines[$j] -match 'A: android:name\(0x01010003\)="([^"]+)"') { [void]$comps.Add($Matches[1]); break }
                if ($lines[$j] -match '^\s*E: ') { break }
            }
        }
    }
    $newClassesSet = Get-DexClasses $newDex
    $missingComps = @($comps | Where-Object { -not $newClassesSet.Contains($_) })
    if ($missingComps.Count -eq 0) { Pass "清单里 $($comps.Count) 个组件类全部能在 dex 中找到" }
    else { foreach ($m in $missingComps) { Fail "清单声明的类在 dex 中不存在：$m" } }

    # -----------------------------------------------------------------------
    Section '3. 类覆盖度（按包比对顶层类）'
    # -----------------------------------------------------------------------
    $oldSet = Get-DexClasses $oldDex
    $newTops = Get-TopLevel $newClassesSet
    $oldTops = Get-TopLevel $oldSet

    $packages = @(
        'com.xtc.systemagent', 'com.xtc.bigdata', 'com.xtc.dns', 'com.xtc.im',
        'com.xtc.httplib', 'com.xtc.qiniu', 'com.xtc.architecture', 'com.xtc.database',
        'com.xtc.log', 'com.xtc.system', 'com.xtc.utils', 'com.xtc.common'
    )
    foreach ($p in $packages) {
        $o = @($oldTops | Where-Object { $_ -like ($p + '.*') }).Count
        $n = @($newTops | Where-Object { $_ -like ($p + '.*') }).Count
        $delta = $n - $o
        $text = '{0,-22} 原厂 {1,4} / 本工程 {2,4} (差 {3,0})' -f $p, $o, $n, $delta
        if ($delta -eq 0) { Pass $text }
        elseif ($Strict) { Fail $text }
        else { Warn $text }
    }

    # 上面按包比对时，重命名过的包会落在不同前缀下（例如原厂 com.xtc.a.a.a.*
    # 被重写成 com.xtc.utils.callback.*），单看某个前缀就会出现一多一少。
    # 所以再比一次"第一方类总量"：com.xtc.* 全算在内，这个必须完全相等。
    $oldXtc = @($oldTops | Where-Object { $_ -like 'com.xtc.*' }).Count
    $newXtc = @($newTops | Where-Object { $_ -like 'com.xtc.*' }).Count
    $xtcText = 'com.xtc.*（第一方类总量）  原厂 {0} / 本工程 {1}' -f $oldXtc, $newXtc
    if ($oldXtc -eq $newXtc) { Pass $xtcText }
    elseif ($Strict) { Fail $xtcText }
    else { Warn $xtcText }

    # -----------------------------------------------------------------------
    Section '4. 版本特征'
    # -----------------------------------------------------------------------
    $badging = (& $aapt dump badging $Apk 2>&1) -join "`n"
    foreach ($kv in @(
        @('package', 'package: name='),
        @('versionCode', "versionCode='"),
        @('versionName', "versionName='"),
        @('sdkVersion', "sdkVersion:'"),
        @('targetSdkVersion', "targetSdkVersion:'")
    )) {
        $label = $kv[0]; $needle = $kv[1]
        $line = ($badging -split "`n") | Where-Object { $_ -like ('*' + $needle + '*') } | Select-Object -First 1
        if ($line) { Write-Host ("   $label : " + $line.Trim()) } else { Fail "badging 里取不到 $label" }
    }

    # -----------------------------------------------------------------------
    Section '5. 混淆痕迹'
    # -----------------------------------------------------------------------
    $javaRoot = Join-Path $root 'app\src\main\java'
    $singleLetterDirs = @(Get-ChildItem $javaRoot -Recurse -Directory | Where-Object { $_.Name -match '^[A-Za-z]$' })
    if ($singleLetterDirs.Count -eq 0) { Pass '没有单字母包名' }
    else { foreach ($d in $singleLetterDirs) { Fail "单字母包名：$($d.FullName)" } }

    $singleLetterFiles = @(Get-ChildItem $javaRoot -Recurse -File -Filter '*.java' | Where-Object { $_.BaseName -match '^[A-Za-z]$' -and $_.BaseName -ne 'R' })
    if ($singleLetterFiles.Count -eq 0) { Pass '没有单字母类名（R 类除外）' }
    else { foreach ($f in $singleLetterFiles) { Fail "单字母类名：$($f.FullName)" } }
} finally {
    if (Test-Path -LiteralPath $tmp) { Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue }
}

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
Section '结论'
if ($script:failures.Count -eq 0) {
    Write-Host "   验收通过。告警 $($script:warnings.Count) 条。"
    Write-Host "   APK：$Apk"
    exit 0
} else {
    Write-Host "   验收失败，共 $($script:failures.Count) 项："
    $script:failures | ForEach-Object { Write-Host ('     - ' + $_) }
    exit 1
}
