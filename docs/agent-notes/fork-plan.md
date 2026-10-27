# 路线 B 决策记录：基于 FongMi/TV 做自有壳

日期：2026-10-27（最后更新：1.3.2 发布后）　状态：**已定稿**

## 结论：基线选 5.6.1，不是 5.8.x

曾经尝试直接用 5.8.3 源码编译，失败原因（重要，避免重复踩）：

- 5.8.3 的 `app` 引用了大量 **私有 media fork 类**：`androidx.media3.exoplayer.libass.*`、
  `SubtitleTranscript*`、`DecoderManager`、`DiscMenu*`、`Effect*` 等；公开的 `FongMi/media`
  分支（`release-1.11.0-fongmi`）里**没有**这些类——作者本地另有未公开改动。
  实测缺 45 个类，无法用公开源码补齐。
- 5.6.1 只缺 8 个音效/画质类（`AudioChannelMode`、`AudioChannelMix`、`AudioEffectParameters`、
  `AudioEffectProcessor`、`AudioEqualizerController`、`VideoAdjustment`、`ColorToneAdjustEffect`、
  `DetailAdjustEffect`），已用兼容桩补齐（`app/src/main/java/androidx/media3/**`），功能不受影响。

## 媒体层构建（已验证）

1. `git -c http.sslBackend=openssl clone --depth 1 -b release-1.11.0-fongmi https://github.com/FongMi/media.git media`
2. Gradle **9.1.0**、JDK 21、SDK 36/37、NDK 29.0.14206865、CMake 3.22.1。
3. 按仓库 `move.txt` 构建 18 个 Java 模块：`gradle -p media :lib-common:assembleRelease ...`
   （`lib-decoder-ffmpeg` 需要 NDK 原生编译，本机沙箱内 CMake/ninja 阻塞，跳过；见 AGENTS.md）
4. 产物 `media/libraries/**/buildout/outputs/aar/lib-*-release.aar` → `fongmi-561/app/libs/`

## 对上游（5.6.1）的改动清单

| 文件 | 改动 | 原因 |
|---|---|---|
| `gradle/gradle-daemon-jvm.properties` | 删除 | 上游固定 daemon 为 JBR，本机 JBR 判目录不可写，Gradle 9.7 拒绝配置 |
| `gradle.properties` | `-Duser.home=.toolchain/home` | 沙箱禁止写 `C:\Users\Q\.android`、`.gradle` |
| `settings.gradle` / `app/build.gradle` | 移除 `:chaquo`，stub `com.fongmi.chaquo.Loader` | Python 运行时需要宿主 Python 3.10，构建环境没有 |
| `app/build.gradle` | applicationId `com.qbox.app`、version 1.3.2、新增 x86_64 flavor | 自有壳 + 模拟器/CI 预留 |
| `app/.../api/config/VodConfig.java` | `DEFAULT_CONFIG` 首次启动写入；拉取失败回退 `assets/qbox_default.json` | 开箱即用 + 断网兜底 |
| `app/.../Updater.java` | 关闭 FongMi 官方源自更新 | 否则会把用户引到 FongMi 官方包 |
| `app/.../HomeActivity.java` | 标题长按 = QBox 自检 | 电视端排障 |
| `res/values*/strings.xml` | `app_name` → QBox | 品牌 |
| `res/mipmap-*`、`drawable/ic_launcher_foreground.xml`、`drawable-nodpi/ic_logo.png`、leanback banner | 自有图标/横幅 | 品牌 |
| `app/src/main/java/androidx/media3/**` | 8 个兼容桩类 | 5.6.1 与公开 media 分支的差集 |

## 环境（`.toolchain/`，已 gitignore）

- JDK：Temurin 21.0.12.1（**不要用 JBR**）
- Android SDK：platform 36 + 37.0（`android-37` junction）、build-tools 36/37、platform-tools、NDK 29、CMake 3.22.1
- Gradle：9.7.1（TV）、9.1.0（media）
- 下载器：`.toolchain/Dl.java`、`.toolchain/Fetch.java`（Java 网络，绕开沙箱 schannel 问题）
- 构建入口：`scripts/build.cmd`；测试入口：`scripts/test.cmd`

## 结果

- 1.3.2 已发布（arm64-v8a / armeabi-v7a），`applicationId com.qbox.app`，v2 签名。
- 离线可验证的链路都已验证：配置拉取/解码（含兜底）、直播解析（5 源 2675 频道 + 真实 HLS）。
- 点播播放无法离线验证：老白 jar 是加固包（`BaseSpiderGuard` + `DexNative` 原生解壳），只能真机。
