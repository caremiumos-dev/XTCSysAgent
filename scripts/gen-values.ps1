
$src = 'D:\CaremiumWorkspace\Research\SystemApps\XTCSysAgent\smali\res'
$dst = 'D:\CaremiumWorkspace\Sources\XTCSysAgent\app\src\main\res'
New-Item -ItemType Directory -Force -Path $dst | Out-Null

function IsLibName([string]$n) {
  if ($n -match '^abc_') { return $true }
  if ($n -match '^notification_') { return $true }
  if ($n -match '^design_') { return $true }
  if ($n -match '^select_dialog_') { return $true }
  if ($n -match '^support_simple_') { return $true }
  if ($n -match '^switch_thumb_') { return $true }
  if ($n -match '^mtrl_') { return $true }
  return $false
}

$map = @('attrs.xml','bools.xml','colors.xml','dimens.xml','drawables.xml','ids.xml','integers.xml','strings.xml','styles.xml')
New-Item -ItemType Directory -Force -Path "$dst\values" | Out-Null
foreach ($k in $map) {
  $p = Join-Path "$src\values" $k
  if (-not (Test-Path $p)) { Write-Output "MISSING values/$k"; continue }
  $lines = Get-Content $p
  $out = New-Object System.Collections.Generic.List[string]
  foreach ($l in $lines) {
    if ($l -match '^\s*<(\w+)\s+name="([^"]+)"') {
      if (IsLibName $Matches[2]) { continue }
    }
    if ($l -match '^\s*<item\s+type="\w+"\s+name="([^"]+)"') {
      if (IsLibName $Matches[1]) { continue }
    }
    $out.Add($l)
  }
  Set-Content -Path (Join-Path "$dst\values" $k) -Value ($out -join [Environment]::NewLine) -Encoding UTF8
  Write-Output "values/$k -> $($out.Count) lines"
}

Get-ChildItem -Path $src -Directory | Where-Object { $_.Name -like 'values-*' } | ForEach-Object {
  $dirName = $_.Name
  $files = Get-ChildItem -Path $_.FullName -File -Filter *.xml
  $keep = @()
  foreach ($f in $files) {
    $lines = Get-Content $f.FullName
    $out = New-Object System.Collections.Generic.List[string]
    foreach ($l in $lines) {
      if ($l -match '^\s*<(\w+)\s+name="([^"]+)"') {
        if (IsLibName $Matches[2]) { continue }
      }
      if ($l -match '^\s*<item\s+type="\w+"\s+name="([^"]+)"') {
        if (IsLibName $Matches[1]) { continue }
      }
      $out.Add($l)
    }
    $bodyCount = ($out | Where-Object { $_ -match '^\s*<\w+\s+name=' }).Count
    if ($bodyCount -gt 0) {
      $d = Join-Path $dst $dirName
      New-Item -ItemType Directory -Force -Path $d | Out-Null
      Set-Content -Path (Join-Path $d $f.Name) -Value ($out -join [Environment]::NewLine) -Encoding UTF8
      $keep += ($f.Name + "(" + $bodyCount + ")")
    }
  }
  if ($keep.Count -gt 0) { Write-Output ($dirName + " : " + ($keep -join ',')) }
}
