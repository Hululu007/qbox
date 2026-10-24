# FongMi/TV 生态研究笔记

研究 TVBox 兼容性、协议规范、竞品行为时参考。数据经 GitHub API 与 raw 文件实测。

## 仓库结构

- **FongMi/TV**（源码，GPL-3.0，纯 Java，单分支 `fongmi`，无 tag）：模块 `app`（main 共用 + leanback/mobile 双 flavor UI）/ `catvod`（Spider 接口层）/ `chaquo`（Chaquopy Python3.10）/ `quickjs`。**开源源码滞后发布约 2 个小版本**（源码 5.6.3 vs 发布 5.7.8）：新版本特性（外挂 runtime 模块管理）在开源代码里找不到，别按源码推断最新行为。
- **FongMi/Release**（发布仓）：主版本 tag = leanback/mobile × arm64/armv7 四个 APK（各 ~43–49MB）；`module` tag = 6 个外挂运行时 APK + `catalog.json`。发版 = 手动建 Release + 上传资产（commit 历史即发布日志）。
- 配套 fork：`FongMi/media`（androidx/media fork，含 ffmpeg/libass/dav1d/mpegh/vpx/rtmp 扩展，**无 release 资产**）、`FongMi/mpv-android`（CI 只产 .so artifact 不发布）、`FongMi/CatVodSpider`（Java 爬虫）。

## 构建链

JDK21 + AGP 9.3.1 + compileSdk 37 + minSdk 24。**`app/libs/` 下 `lib-*.aar` 不在 git**，clone 后编不过；aar = media fork 自编译产物（NDK 编 ffmpeg/libass 全家桶，全项目最重一步，无现成下载渠道）。无 CI（仅 pages.yml 发文档站），官方纯本地 Windows 构建：`local.properties` 配 sdk+签名 → `gradlew assemble{Lean,Mobile}Release` → 自定义 `AbiApkPackaging`（buildSrc）做 ABI splits + zipalign/apksigner 后处理 → 输出 `Release/apk/` → 手动传发布仓。

## 外挂模块系统（5.7.x 新架构，可借鉴的设计）

catalog.json 每模块：`{id, packageName(com.fongmi.runtime.*), compatibilityId, versionCode, artifacts[{abi, minSdk, size, sha256, url}]}`。主包内置 catalog → 设置页按 packageName 探测已装模块、compatibilityId 判版本兼容 → 按需下载。模块清单：quickjs(2.3MB js 爬虫)/thunder(6.2MB 迅雷)/mpv(17MB 播放器)/python(23MB)/bdj(39MB 蓝光)/nodejs(47.6MB)。模式 = 主包瘦身 + 执行环境外挂。

## OK 影视渊源

FongMi（台湾）2023 因当地版权立法压力停更归档 → 2024 年初交棒"OK哥"，以 OK影视 名义沿用同一仓库体系续更，版本号连续。市面"FongMi OK版/Pro版"多为同一 APK 再分发。

## TVBox 配置生态

空壳 App + json 配置源（顶层键 spider/sites/lives/parses/danmu/flags/wallpaper/logo）；spider 三形态 jar / js(drpy) / py；spider 字段支持 `;md5;` 校验。配置仓库生态：饭太硬、gaotianliuyun/gao（其 0826.json = 饭太硬镜像）、qist/tvbox。协议规范文档 fongmi.github.io/TV（源码在 TV 仓 `website/` 目录）。

## 饭太硬配置实测样本（镜像 0826.json）

- 顶层 keys：lives / logo / sites / spider / wallpaper
- 51 个 sites 全部 type 3 + `api: csp_*`（FongMi 约定：csp_ 前缀 = jar 内置 Java spider）
- lives：type 0 m3u 订阅 + playerType 字段
- spider：`./jar/fan.txt;md5;<md5>` 相对路径，fan.txt 实为 zip（PK 头）内含 classes.dex
