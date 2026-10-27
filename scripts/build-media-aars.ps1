# 构建 media3 fork 的 18 个 lib-*.aar 并拷进 fongmi-561/app/libs
#
# 背景：FongMi/TV 依赖的不是 Maven 上的 media3，而是 FongMi 自编的 fork
#       （仓库 FongMi/media，分支 release-1.11.0-fongmi）。
#       app/libs/*.aar 不入库，所以新克隆的 qbox-tv 需要先跑一次本脚本。
#
# 用法：
#   .\scripts\build-media-aars.cmd            # 增量构建（已存在则复用 media/）
#   .\scripts\build-media-aars.cmd -Force     # 重新拉取 media 并强制重建

param([switch]$Force)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
$toolchain = Join-Path $root ".toolchain"
$media = Join-Path $root "media"
$libs = Join-Path $root "fongmi-561\app\libs"

$env:JAVA_HOME = Join-Path $toolchain "jdk-extract\jdk-21.0.12.1+1"
$env:ANDROID_HOME = Join-Path $toolchain "android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $toolchain "android-user-home"
$env:GRADLE_USER_HOME = Join-Path $toolchain "gradle-home"
$env:TMP = Join-Path $toolchain "tmp"
$env:TEMP = $env:TMP
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$gradle = Join-Path $toolchain "gradle-9.1.0\gradle-9.1.0\bin\gradle.bat"
if (-not (Test-Path $gradle)) { throw "missing gradle 9.1.0 at $gradle" }

if ($Force -or -not (Test-Path (Join-Path $media "settings.gradle.kts"))) {
    if (Test-Path $media) { Remove-Item $media -Recurse -Force }
    & git -c http.sslBackend=openssl clone --depth 1 -b release-1.11.0-fongmi https://github.com/FongMi/media.git $media
    if ($LASTEXITCODE -ne 0) { throw "clone media failed" }
}

$modules = @(
    "lib-common", "lib-container", "lib-database", "lib-datasource", "lib-datasource-okhttp",
    "lib-datasource-rtmp", "lib-decoder", "lib-effect", "lib-exoplayer", "lib-exoplayer-dash",
    "lib-exoplayer-hls", "lib-exoplayer-rtsp", "lib-exoplayer-smoothstreaming", "lib-extractor",
    "lib-mpvplayer", "lib-session", "lib-ui", "lib-ui-danmaku"
)
$tasks = $modules | ForEach-Object { ":$($_)`:assembleRelease" }

Write-Host "==> building $($modules.Count) media modules" -ForegroundColor Cyan
& $gradle -p $media @tasks --console=plain --no-watch-fs
if ($LASTEXITCODE -ne 0) { throw "media build failed: $LASTEXITCODE" }

New-Item -ItemType Directory -Force $libs | Out-Null
$copied = 0
Get-ChildItem $media -Recurse -Filter "lib-*-release.aar" |
    Where-Object { $_.FullName -match "\\buildout\\outputs\\aar\\" } |
    ForEach-Object {
        Copy-Item $_.FullName (Join-Path $libs $_.Name) -Force
        $copied++
    }

Write-Host "==> copied $copied aars to app/libs" -ForegroundColor Green
Get-ChildItem $libs -Filter "lib-*.aar" | ForEach-Object {
    Write-Host ("     " + $_.Name + "  " + [math]::Round($_.Length / 1MB, 1) + " MB")
}

Write-Host "注意：lib-decoder-ffmpeg 需要 NDK 原生编译，未包含（见 AGENTS.md 已知限制）。" -ForegroundColor Yellow
