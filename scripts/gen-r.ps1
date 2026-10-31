
$pub = 'D:\CaremiumWorkspace\Research\SystemApps\XTCSysAgent\smali\res\values\public.xml'
$dst = 'D:\CaremiumWorkspace\Sources\XTCSysAgent\app\src\main\res\values'
$out = 'D:\CaremiumWorkspace\Sources\XTCSysAgent\app\src\main\java\com\xtc\systemagent\R.java'

$types = [ordered]@{}
Get-Content $pub | ForEach-Object {
  if ($_ -match '<public type="(\w+)" name="([^"]+)" id="(0x[0-9a-fA-F]+)"') {
    $t = $Matches[1]; $n = $Matches[2]; $id = $Matches[3]
    if (-not $types.Contains($t)) { $types[$t] = New-Object System.Collections.Generic.List[object] }
    $types[$t].Add([pscustomobject]@{ Name = $n; Id = $id })
  }
}

# 生成 res/values/public.xml（public 标记，让 AAPT2 保留原厂资源 id）
$pubOut = New-Object System.Collections.Generic.List[string]
$pubOut.Add('<?xml version="1.0" encoding="utf-8"?>')
$pubOut.Add('<resources>')
foreach ($t in $types.Keys) {
  foreach ($e in $types[$t]) {
    $pubOut.Add('    <public type="' + $t + '" name="' + $e.Name + '" id="' + $e.Id + '" />')
  }
}
$pubOut.Add('</resources>')
Set-Content -Path (Join-Path $dst 'public.xml') -Value ($pubOut -join [Environment]::NewLine) -Encoding UTF8

# 生成 R.java
$sb = New-Object System.Collections.Generic.List[string]
$sb.Add('package com.xtc.systemagent;')
$sb.Add('')
$sb.Add('/**')
$sb.Add(' * 资源索引类。字段与资源 id 与原厂 APK 完全一致（见 res/values/public.xml），')
$sb.Add(' * 因此任何通过 R 常量或资源 id 进行的比较/持久化都保持原样。')
$sb.Add(' */')
$sb.Add('public final class R {')
foreach ($t in $types.Keys) {
  $cls = 'R$' + $t
  $sb.Add('    public static final class ' + $t + ' {')
  foreach ($e in $types[$t]) {
    $v = [System.Convert]::ToInt64($e.Id, 16)
    $sb.Add('        public static final int ' + $e.Name + ' = ' + $v + ';')
  }
  $sb.Add('        private ' + $t + '() {')
  $sb.Add('        }')
  $sb.Add('    }')
  $sb.Add('')
}
$sb.Add('    private R() {')
$sb.Add('    }')
$sb.Add('}')
Set-Content -Path $out -Value ($sb -join [Environment]::NewLine) -Encoding UTF8
"types: $($types.Keys -join ', ')"
foreach ($t in $types.Keys) { "  $t : $($types[$t].Count)" }
