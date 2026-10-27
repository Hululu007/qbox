# QBox

Android TV 的 TVBox 壳。当前产品线基于 **FongMi/TV 5.6.1** 源码定制（自有品牌、自有签名、默认配置、兼容层），
目标是与 OK影视（FongMi 系）同等的可用性，再叠加自有 UI/功能。

## 两条线

| 线 | 说明 | 位置 |
|---|---|---|
| **产品壳（主线）** | FongMi/TV 5.6.1 定制，applicationId `com.qbox.app` | 源码仓 [zqs1qiwan/qbox-tv](https://github.com/zqs1qiwan/qbox-tv)，本地构建目录 `fongmi-561/`（gitignore） |
| 自研 M1 壳 | 纯 Kotlin 三屏原型（配置/jar/ExoPlayer），已被主线取代，保留作参考 | 本仓库 `app/` |

## 下载

- https://github.com/zqs1qiwan/qbox-tv/releases/tag/v1.0.3
  - `QBox-1.0.3-arm64-v8a.apk`（44.6 MB，主流电视）
  - `QBox-1.0.3-armeabi-v7a.apk`（38.6 MB，老电视）
- `minSdk 24`（Android 7+），v2 签名，证书 SHA-256 `f7c4b32c...f63c62a`。

## 产品特性（v1.0.3）

- 默认配置拉取失败时回退到内置快照 `assets/qbox_default.json`（49 站点），断网/源跳转变更也能出站点

- TVBox 配置协议全量：sites / lives / parses / rules / hosts / flags / wallpaper / logo
- 4 种爬虫运行时：jar（csp_，内置 DexClassLoader）、QuickJS（js）、Python（已裁掉，见下）、HTTP API 站
- 解析：WebView 嗅探 / JSON API / jar Json / Mix / 超级解析
- 直播：m3u/txt 订阅、EPG、分组选台
- 播放：ExoPlayer + MPV 双引擎（media3 fork 由本项目自编）、多轨/字幕/倍速/续播
- 数据：Room（历史/收藏/配置/站点设置）
- 默认配置：`https://tvbox.laobaitv.net`（老白 TVBOX；实测 49 站点、6 路直播）
- 首次启动自动载入默认配置；设置页可随时更换

## 构建（Windows 已验证）

工具链全部在 `.toolchain/`（gitignore，约 8 GB）：

```
JDK 21 (Temurin 21.0.12.1)
Android SDK 36/37 + build-tools 36/37 + platform-tools
NDK 29.0.14206865 + CMake 3.22.1
Gradle 9.7.1（TV 5.6.1 构建）/ 9.1.0（media fork 构建）
```

1. **媒体层**（一次性）：`media/` 是 FongMi/media 的 `release-1.11.0-fongmi` 分支，
   构建 18 个 Java 模块的 `lib-*-release.aar`，拷进 `fongmi-561/app/libs/`。
2. **应用**：

```powershell
$env:JAVA_HOME="<repo>\.toolchain\jdk-extract\jdk-21.0.12.1+1"
$env:ANDROID_HOME="<repo>\.toolchain\android-sdk"
$env:ANDROID_USER_HOME="<repo>\.toolchain\android-user-home"
$env:GRADLE_USER_HOME="<repo>\.toolchain\gradle-home"
$env:TMP="$env:TEMP"<repo>\.toolchain\tmp"   # 必须指到工作区内
cd fongmi-561
.\gradlew.bat :app:assembleLeanbackArm64_v8aRelease :app:assembleLeanbackArmeabi_v7aRelease
```

产物：`fongmi-561/app/build/outputs/apk/leanback*/release/*.apk`。
签名密钥：`app/qbox-release.jks` + `keystore.properties`（本仓库，已入库）。

## 已验证

- **配置链路（壳内真实代码，可复现）**：Robolectric 测试 `ConfigPipelineTest` 直接调用
  `com.fongmi.android.tv.api.Decoder.getJson("https://tvbox.laobaitv.net")`，输出
  `QBOX_TEST_OK sites=49 spider=https://...jpg;md5;b55978c4...`。
  运行：`.\gradlew.bat :app:testLeanbackArm64_v8aDebugUnitTest --tests "com.qbox.ConfigPipelineTest"`
- 默认配置链路：302 → `www.xn--sss604efuw.cc/tv`，JPEG 伪装 + `**` 后接 base64 JSON；
  解码后 49 站点（46 `csp_` + 3 HTTP）、6 路直播。
- spider 包：`.../e05f...jpg` 实为 zip（`classes.dex`），MD5 与配置声明一致
  （`b55978c4fe6d12e2fea9075f17c8a67a`），dex 内 76 个 `com.github.catvod.spider.*Guard` 类。
- APK：`com.qbox.app` / label `QBox` / v1.0.3 / v2 签名通过 / arm64 44.6MB + armv7 38.6MB；
  图标、leanback banner、首页 logo 已换成 QBox 自有设计（深色圆角 + 环形播放标记）。
- 兜底：`QBOX_FALLBACK_OK sites=49`，内置 `qbox_default.json` 与线上配置同构，默认源不可达时仍能进首页。

## 已知限制

- **Python 爬虫已裁掉**（Chaquopy 需要宿主 Python 3.10 才能构建），py 类型站点会回退空实现。
- FFmpeg 解码器未编（`lib-decoder-ffmpeg` 需要 NDK 原生编译），`解码模式=FFMPEG` 不可用；MPV 引擎可用。
- 界面主体仍是 FongMi 原版 leanback 布局，只做了品牌层（图标/banner/logo/名称），首页 hero/tab 结构未重做。
- 未在真机/模拟器上做端到端播放验证：本机模拟器（WHPX 与纯 TCG 都试过）在沙箱内启动即退出——
  qemu 需要 Windows 命名管道，而沙箱禁止命名管道；真机验收需用户电视。
- **老白 spider jar 是加固包**：dex 里只有 `*Guard` 薄壳类，全部继承 `BaseSpiderGuard`，
  通过 `DexNative` + `assets/ftyguard_v7|v8.so` 在运行时解出真实实现。所以拿 dex 转 JVM 类也只能跑到
  `init()` 就 NPE（delegate 为空），播放链路无法脱离 Android 运行时验证。

## 仓库结构

```
app/                自研 M1 壳（Kotlin，历史）
docs/agent-notes/   研究笔记、构建手册、拆解方法
.toolchain/         本机工具链（gitignore）
fongmi-561/         产品源码 worktree（gitignore，推送到 qbox-tv）
media/              media3 fork 源码（gitignore，用于编 aar）
dist/               构建产物（gitignore）
```
