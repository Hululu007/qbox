#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Build, smoke-test and capture QBox screenshots without a real TV.
.DESCRIPTION
  Runs on Windows PowerShell or pwsh. It expects a TV/720p Android emulator to be
  reachable through adb, then builds the x86_64 debug APK, injects the offline
  mock config, launches HomeActivity and saves the resulting screenshot.
.EXAMPLE
  .\smoke\mock-server.ps1
  .\smoke\headless-screenshot.ps1 -StartEmulator -Avd qbox-tv720 -ClearData -UpdateBaseline
#>

[CmdletBinding()]
param(
  [string]$Avd = 'qbox-tv720',
  [string]$Package = 'com.qbox.app',
  [int]$Port = 8765,
  # Forward slashes + [IO.Path]::Combine below so this works on Windows and on Linux CI runners.
  [string]$OutputDir = 'artifacts/headless-screenshots',
  [string]$BaselineDir = 'smoke/baselines',
  [int]$WaitSeconds = 10,
  [switch]$StartEmulator,
  [switch]$SkipBuild,
  [switch]$ClearData,
  [switch]$UpdateBaseline
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { [IO.Path]::Combine((Split-Path -Parent $root), '.toolchain', 'android-sdk') }
# adb/emulator live at the SDK root on all platforms; on Windows they carry .exe, on Linux they do not.
$adb = [IO.Path]::Combine($sdk, 'platform-tools', 'adb')
if ($env:OS -eq 'Windows_NT') { $adb += '.exe' }
if (-not (Test-Path -LiteralPath $adb)) { $adb = (Get-Command adb -ErrorAction Stop).Source }
$emulator = [IO.Path]::Combine($sdk, 'emulator', 'emulator')
if ($env:OS -eq 'Windows_NT') { $emulator += '.exe' }
if (-not (Test-Path -LiteralPath $emulator)) { $emulator = (Get-Command emulator -ErrorAction Stop).Source }
$gradlew = if ($env:OS -eq 'Windows_NT' -or (Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))) { Join-Path $root 'gradlew.bat' } else { Join-Path $root 'gradlew' }
$mock = Join-Path $PSScriptRoot 'mock-server.ps1'
$output = [IO.Path]::Combine($root, $OutputDir.Replace('\', '/'))
$baseline = [IO.Path]::Combine($root, $BaselineDir.Replace('\', '/'))
New-Item -ItemType Directory -Force -Path $output | Out-Null

function Invoke-AdbBinary {
  param([string[]]$Arguments, [string]$Destination)
  $psi = [Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = $adb
  $psi.Arguments = ($Arguments | ForEach-Object { if ($_ -match '[\s"]') { '"' + $_.Replace('"','\"') + '"' } else { $_ } }) -join ' '
  $psi.UseShellExecute = $false
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $process = [Diagnostics.Process]::Start($psi)
  $buffer = [IO.MemoryStream]::new()
  try {
    $process.StandardOutput.BaseStream.CopyTo($buffer)
    $errorText = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "adb failed: $errorText" }
    [IO.File]::WriteAllBytes($Destination, $buffer.ToArray())
  } finally {
    $buffer.Dispose()
  }
}

function Invoke-AdbText {
  param([string[]]$Arguments)
  $out = & $adb @Arguments 2>&1
  if ($LASTEXITCODE -ne 0) { throw "adb failed ($LASTEXITCODE): $($Arguments -join ' ')`n$out" }
  return ($out | Out-String).Trim()
}

function Install-Apk {
  param([string]$Path)
  # 不再吞掉 adb 输出：Failure/Error 必须让整个冒烟失败，否则会出现「截图成功但没装进去」的假绿。
  $out = & $adb install -r $Path 2>&1
  $text = ($out | Out-String).Trim()
  Write-Host $text
  if ($LASTEXITCODE -ne 0 -or $text -match 'Failure|Error|Exception') {
    throw "apk install failed: $text"
  }
}

function Assert-PackageInstalled {
  param([string]$Package)
  $path = Invoke-AdbText @('shell','pm','path',$Package)
  if ($path -notmatch 'package:') { throw "$Package is not installed (pm path returned: $path)" }
  Write-Host "installed: $path"
}

function Resolve-LauncherComponent {
  param([string]$Package)
  # namespace (com.fongmi.android.tv) != applicationId (com.qbox.app)，所以不能硬编码 .ui.activity.HomeActivity。
  foreach ($category in @('android.intent.category.LEANBACK_LAUNCHER','android.intent.category.LAUNCHER')) {
    $out = & $adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c $category $Package 2>&1
    $component = ($out | Where-Object { $_ -match '/' } | Select-Object -Last 1)
    if ($component) { return $component.Trim() }
  }
  return ''
}

function Assert-ForegroundPackage {
  param([string]$Package, [int]$TimeoutSeconds = 60)
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  $focus = $null
  while ((Get-Date) -lt $deadline) {
    $focus = & $adb shell dumpsys activity activities 2>&1 | Select-String -Pattern 'mResumedActivity|topResumedActivity' | Select-Object -First 1
    if ($focus -and $focus.ToString() -match [regex]::Escape($Package)) {
      Write-Host "foreground OK: $($focus.ToString().Trim())"
      return
    }
    Start-Sleep -Seconds 2
  }
  throw "foreground activity is not $Package after $TimeoutSeconds s: $focus"
}

function Test-Mock {
  try { return (Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 "http://127.0.0.1:$Port/health").Content.Trim() -eq 'ok' } catch { return $false }
}

function Save-Logcat {
  param([string]$Destination)
  # 失败时把 logcat 一并留在 artifacts 里，否则只能靠猜。
  & $adb logcat -d -v time 2>&1 | Set-Content -LiteralPath $Destination -Encoding UTF8
  Write-Host "logcat: $Destination"
}

function Dismiss-SystemDialog {
  param([string]$Ui)
  # 无 KVM 的软件模拟下 SystemUI / 系统进程容易 ANR，弹窗会盖住应用。
  if ($Ui -notmatch "isn't responding|keeps stopping|Not responding") { return $false }
  # 只用 ASCII 字面量：Windows PowerShell 5.1 会按 ANSI 读取无 BOM 的 UTF-8 文件，
  # 中文字符串字面量可能被误解码成引号，导致整个脚本语法错误。
  # 只点 Wait：Close app 会直接杀掉被测应用（实测 ANR 后被 force finish）。
  $m = [regex]::Match($Ui, 'text="(Wait)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
  if (-not $m.Success) { return $false }
  $x = [int](([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2)
  $y = [int](([int]$m.Groups[3].Value + [int]$m.Groups[5].Value) / 2)
  Write-Host "dismissing system dialog via '$($m.Groups[1].Value)' at $x,$y"
  Invoke-AdbText @('shell','input','tap',[string]$x,[string]$y) | Out-Null
  Start-Sleep -Seconds 2
  return $true
}

function Test-RawNotBlack {
  param([string]$Path)
  # 直接分析 `adb exec-out screencap` 的原始 RGBA：不依赖 System.Drawing
  # （System.Drawing.Common 在 Linux 的 pwsh 上会抛 PlatformNotSupportedException）。
  $bytes = [IO.File]::ReadAllBytes($Path)
  if ($bytes.Length -lt 16) { return $false }
  $width = [BitConverter]::ToInt32($bytes, 0)
  $height = [BitConverter]::ToInt32($bytes, 4)
  $pixels = $width * $height * 4
  if ($width -le 0 -or $height -le 0 -or $bytes.Length -lt $pixels) { return $false }
  $offset = $bytes.Length - $pixels
  $seen = @{}
  $stepX = [Math]::Max(1, [int]($width / 16))
  $stepY = [Math]::Max(1, [int]($height / 9))
  for ($y = 0; $y -lt $height; $y += $stepY) {
    for ($x = 0; $x -lt $width; $x += $stepX) {
      $i = $offset + (($y * $width) + $x) * 4
      $key = "$($bytes[$i]),$($bytes[$i+1]),$($bytes[$i+2])"
      $seen[$key] = 1
    }
  }
  return ($seen.Count -gt 1)
}

if (-not $SkipBuild) {
  $mockConfig = "http://10.0.2.2:$Port/config.json"
  Push-Location $root
  try { & $gradlew ':app:assembleLeanbackX86_64Debug' "-PqboxTestConfig=$mockConfig" } finally { Pop-Location }
  if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
}

if (-not (Test-Mock)) {
  $hostExe = if (Get-Command pwsh -ErrorAction SilentlyContinue) { 'pwsh' } else { 'powershell.exe' }
  Write-Host "Starting mock on 127.0.0.1:$Port"
  if ($env:OS -eq 'Windows_NT') {
    Start-Process $hostExe -ArgumentList @('-NoProfile','-File',$mock,'-Port',[string]$Port,'-PublicBaseUrl',"http://10.0.2.2:$Port") -WindowStyle Hidden | Out-Null
  } else {
    Start-Job -ScriptBlock { param($f,$p,$h) & $h -NoProfile -File $f -Port $p -PublicBaseUrl "http://10.0.2.2:$p" } -ArgumentList $mock,$Port,$hostExe | Out-Null
  }
  for ($i = 0; $i -lt 20 -and -not (Test-Mock); $i++) { Start-Sleep -Milliseconds 250 }
}
if (-not (Test-Mock)) { throw "mock server did not start on port $Port" }

if ($StartEmulator) {
  Write-Host "Starting emulator $Avd (headless)"
  Start-Process $emulator -ArgumentList @('-avd',$Avd,'-no-window','-no-audio','-no-boot-anim','-gpu','swiftshader_indirect') | Out-Null
}
Invoke-AdbText @('wait-for-device')
Invoke-AdbText @('shell','settings','put','global','window_animation_scale','0')
Invoke-AdbText @('shell','settings','put','global','transition_animation_scale','0')
Invoke-AdbText @('shell','settings','put','global','animator_duration_scale','0')
# ATD/TV 镜像启动后屏幕可能是熄的：uiautomator 仍能读到层级，但 screencap 全黑。
Invoke-AdbText @('shell','svc','power','stayon','true') | Out-Null
Invoke-AdbText @('shell','settings','put','system','screen_off_timeout','2147483647') | Out-Null
Invoke-AdbText @('shell','input','keyevent','KEYCODE_WAKEUP') | Out-Null
Invoke-AdbText @('shell','wm','dismiss-keyguard') | Out-Null
# 无 KVM 的软件模拟很慢，SystemUI 容易 ANR；关掉错误弹窗，避免截图被弹窗盖住。
Invoke-AdbText @('shell','settings','put','global','hide_error_dialogs','1') | Out-Null
Invoke-AdbText @('shell','settings','put','secure','anr_show_background','0') | Out-Null
$apkDir = [IO.Path]::Combine($root, 'app', 'build', 'outputs', 'apk', 'leanbackX86_64', 'debug')
$apk = Get-ChildItem -LiteralPath $apkDir -Filter '*.apk' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { throw "no x86_64 debug APK found in $apkDir" }
Write-Host "Installing $($apk.FullName)"
Install-Apk $apk.FullName
if ($ClearData) { Invoke-AdbText @('shell','pm','clear',$Package) | Out-Null }
Assert-PackageInstalled $Package

# 无 KVM 的软件模拟下，首次启动的类校验/解释执行会把主线程拖到 ANR
# （实测 nightly 42961634681：ANR in com.qbox.app -> force finish -> 截图变成桌面）。
# 先把 dex 做 AOT 编译（speed），启动耗时能降一个数量级。
Write-Host "AOT compiling $Package (mode=speed)"
Invoke-AdbText @('shell','cmd','package','compile','-m','speed','-f',$Package) | Out-Null

$component = Resolve-LauncherComponent $Package
if (-not $component) { throw "cannot resolve launcher activity for $Package" }
function Start-QBox {
  Write-Host "Launching $component"
  Invoke-AdbText @('shell','am','start','-W','-n',$component) | Out-Null
}
Start-QBox
Start-Sleep -Seconds 10

$screenshot = Join-Path $output 'qbox-home.png'
$uiPath = Join-Path $output 'qbox-home.xml'
$logPath = Join-Path $output 'logcat.txt'

# 轮询：等前台是 QBox，并且 UI 里真的出现了 mock 内容。
# 只等固定秒数会截到「还在加载」的黑屏/空列表，之前 nightly 就是这样假绿的。
$deadline = (Get-Date).AddSeconds([Math]::Max($WaitSeconds, 180))
$ui = ''
$foregroundOk = $false
$relaunches = 0
while ((Get-Date) -lt $deadline) {
  $focus = & $adb shell dumpsys activity activities 2>&1 | Select-String -Pattern 'mResumedActivity|topResumedActivity' | Select-Object -First 1
  $isForeground = [bool]($focus -and $focus.ToString() -match [regex]::Escape($Package))
  if ($isForeground) {
    if (-not $foregroundOk) { Write-Host "foreground OK: $($focus.ToString().Trim())" }
    $foregroundOk = $true
  } elseif ($foregroundOk -or $relaunches -lt 3) {
    # 应用被 ANR 杀掉或掉到后台时重新拉起，而不是直接判失败。
    $relaunches++
    Write-Host "app is not foreground (attempt $relaunches), relaunching"
    Start-QBox
    Start-Sleep -Seconds 8
    continue
  }
  Invoke-AdbText @('shell','uiautomator','dump','/sdcard/qbox-ui.xml') | Out-Null
  Invoke-AdbBinary @('exec-out','cat','/sdcard/qbox-ui.xml') $uiPath
  $ui = Get-Content -LiteralPath $uiPath -Raw
  if (Dismiss-SystemDialog $ui) { continue }
  if ($foregroundOk -and $ui -match 'Mock Moon|Movie|Series|Documentary' -and $ui -match [regex]::Escape($Package)) { break }
  Start-Sleep -Seconds 3
}

if (-not $foregroundOk) {
  Save-Logcat $logPath
  throw "foreground activity is not $Package within the wait window"
}
if ($ui -notmatch [regex]::Escape($Package)) {
  Save-Logcat $logPath
  throw "UI dump does not contain $Package; screenshot is not the QBox home screen"
}
if ($ui -notmatch 'Mock Moon|Movie|Series|Documentary') {
  Save-Logcat $logPath
  throw "mock config did not render (no mock category/film in UI dump); see $logPath"
}

# 截图：ATD 镜像可能不合成硬件层，必要时关掉 hw overlays 再截一次。
# 先用原始 RGBA 判断是否全黑（跨平台），确认有画面后再存 PNG 证据。
$rawPath = Join-Path $output 'qbox-home.raw'
Invoke-AdbBinary @('exec-out','screencap') $rawPath
$hasPicture = Test-RawNotBlack $rawPath
if (-not $hasPicture) {
  Write-Host "screenshot is black, retrying with hardware overlays disabled"
  Invoke-AdbText @('shell','service','call','SurfaceFlinger','1008','i32','1') | Out-Null
  Start-Sleep -Seconds 2
  Invoke-AdbBinary @('exec-out','screencap') $rawPath
  $hasPicture = Test-RawNotBlack $rawPath
}
if (-not $hasPicture) {
  Save-Logcat $logPath
  throw "screenshot is still uniformly black; see $logPath"
}
Invoke-AdbBinary @('exec-out','screencap','-p') $screenshot
Remove-Item -LiteralPath $rawPath -Force -ErrorAction SilentlyContinue
Save-Logcat $logPath
New-Item -ItemType Directory -Force -Path $baseline | Out-Null
if ($UpdateBaseline) { Copy-Item -LiteralPath $screenshot -Destination (Join-Path $baseline 'qbox-home.png') -Force }
Write-Host "Screenshot: $screenshot"
Write-Host "View dump: $uiPath"
if ($UpdateBaseline) { Write-Host "Updated baseline: $BaselineDir/qbox-home.png" }
