# 构建 QBox APK（Windows，PowerShell 5.1+）
#
# 用法：
#   .\scripts\build.ps1                      # 打 arm64 + v7a 的 release
#   .\scripts\build.ps1 -Abi arm64           # 只打 arm64
#   .\scripts\build.ps1 -Variant Debug       # 打 debug（快，用于验证编译）
#
# 说明：所有 Gradle/Android 环境变量都指到仓库内 .toolchain/，避免沙箱/权限问题。

param(
    [ValidateSet("arm64", "v7a", "all")]
    [string]$Abi = "all",
    [ValidateSet("Release", "Debug")]
    [string]$Variant = "Release"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$toolchain = Join-Path $root ".toolchain"

$env:JAVA_HOME = Join-Path $toolchain "jdk-extract\jdk-21.0.12.1+1"
$env:ANDROID_HOME = Join-Path $toolchain "android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $toolchain "android-user-home"
$env:GRADLE_USER_HOME = Join-Path $toolchain "gradle-home"
$env:TMP = Join-Path $toolchain "tmp"
$env:TEMP = $env:TMP
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

foreach ($dir in @($env:ANDROID_USER_HOME, $env:TMP)) {
    New-Item -ItemType Directory -Force $dir | Out-Null
}

$tasks = @()
if ($Abi -in @("arm64", "all")) { $tasks += ":app:assembleLeanbackArm64_v8a$Variant" }
if ($Abi -in @("v7a", "all")) { $tasks += ":app:assembleLeanbackArmeabi_v7a$Variant" }

Write-Host "==> $($tasks -join ' ')" -ForegroundColor Cyan
Push-Location (Join-Path $root "fongmi-561")
try {
    & ".\gradlew.bat" @tasks --console=plain --no-watch-fs
    if ($LASTEXITCODE -ne 0) { throw "gradle failed: $LASTEXITCODE" }
} finally {
    Pop-Location
}

$out = Join-Path $root "fongmi-561\app\build\outputs\apk"
Get-ChildItem $out -Recurse -Filter *.apk |
    Where-Object { $_.FullName -match [regex]::Escape($Variant.ToLower()) -and ($Abi -eq "all" -or $_.FullName -match $(if ($Abi -eq "arm64") { "arm64" } else { "armeabi" })) } |
    ForEach-Object { Write-Host ("APK  " + $_.FullName + "  " + [math]::Round($_.Length / 1MB, 1) + " MB") -ForegroundColor Green }
