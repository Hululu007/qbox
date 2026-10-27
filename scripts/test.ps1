# 跑 QBox 单元测试（JVM + Robolectric）
#
# 用法： .\scripts\test.ps1
#
# 覆盖：
#   ConfigPipelineTest  - 默认配置拉取/解码（线上 + 内置兜底）
#   LiveParseTest       - 直播 m3u/txt 解析（真实壳代码）

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
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

Push-Location (Join-Path $root "fongmi-561")
try {
    & ".\gradlew.bat" :app:testLeanbackArm64_v8aDebugUnitTest --tests "com.qbox.*" --console=plain --no-watch-fs
    if ($LASTEXITCODE -ne 0) { throw "tests failed: $LASTEXITCODE" }
} finally {
    Pop-Location
}
