
$src = 'D:\CaremiumWorkspace\Research\SystemApps\XTCSysAgent\smali\res'
$dst = 'D:\CaremiumWorkspace\Sources\XTCSysAgent\app\src\main\res'

$libNames = New-Object System.Collections.Generic.HashSet[string]
Get-ChildItem -Path "$dst\values" -File -Filter *.xml | ForEach-Object {
  Get-Content $_.FullName | ForEach-Object {
    if ($_ -match '^\s*(?:<(\w+)|<item\s+type="\w+")\s+name="([^"]+)"') { [void]$libNames.Add($Matches[2]) }
  }
}
Write-Output ("libNames count: " + $libNames.Count)

# --- 拷贝 drawable / mipmap / anim / color / layout 中没有被过滤掉的资源 ---
$copied = 0
$skipped = 0
Get-ChildItem -Path $src -Directory | Where-Object { $_.Name -notlike 'values*' } | ForEach-Object {
  $dirName = $_.Name
  Get-ChildItem -Path $_.FullName -File -Recurse | ForEach-Object {
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
    if ($libNames.Contains($baseName)) { $script:skipped++; return }
    $rel = $_.FullName.Substring($src.Length).TrimStart('\')
    $target = Join-Path $dst $rel
    New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
    Copy-Item -Path $_.FullName -Destination $target -Force
    $script:copied++
  }
}
Write-Output ("copied: $copied, skipped(lib): $skipped")

# --- 未被引用的 lib 名字也清理出 values ---
Get-ChildItem -Path "$dst\values" -File -Filter *.xml | ForEach-Object {
  $content = Get-Content $_.FullName -Raw
  Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
}
