# 路线 B：基于 FongMi/TV 做自有壳

日期：2026-10-27　状态：媒体层 aar 已自建，应用编译中

## 结论（重要）

FongMi/TV 5.8.3 源码可以完整编译，**不需要降级播放器**。关键发现：

- 它依赖的不是一个 `media-1.8.0.aar`，而是 **20 个 `lib-*.aar`**（FongMi 自编 media3 fork）。
- 构建脚本就在 fork 仓库里：`FongMi/media` 分支 **`release-1.11.0-fongmi`** 的 `move.bat` + `move.txt` 列出了全部产物；
  每个模块产出 `lib-<name>-release.aar`，拷进 `TV/app/libs/` 即可。
- `move.txt` 清单：common、container、database、datasource、datasource-okhttp、datasource-rtmp、decoder、decoder-ffmpeg、decoder-mpegh、effect、exoplayer、exoplayer-dash/hls/rtsp/smoothstreaming、extractor、mpvplayer、session、ui、ui-danmaku。

## 构建步骤（已在本机验证）

1. `git -c http.sslBackend=openssl clone --depth 1 -b release-1.11.0-fongmi https://github.com/FongMi/media.git media`
2. 用 Gradle **9.1.0**（`-all` 包），JDK 21，SDK 36/37，NDK 29.0.14206865 + CMake 3.22.1（AGP 会自动装）。
3. `gradle -p media :lib-common:assembleRelease ...`（18 个 Java 模块；`lib-decoder-ffmpeg` 需要 NDK，可跳过）。
4. 把 `media/libraries/**/buildout/outputs/aar/lib-*-release.aar` 拷到 `fongmi/app/libs/`。
5. `fongmi` 里 `./gradlew :app:assembleLeanbackDebug`。

## 本项目对上游做的改动（fork 补丁）

| 文件 | 改动 | 原因 |
|---|---|---|
| `fongmi/gradle/gradle-daemon-jvm.properties` | 删除 | 上游把 daemon 固定为 JBR 21，JBR 在本机把目录判为不可写，Gradle 9.7 拒绝配置 |
| `fongmi/gradle.properties` | `-Duser.home=.toolchain/home`、关配置缓存 | 沙箱禁止写 `~/.android`、`~/.gradle` |
| `media/gradle.properties` | 同上 | 同上 |
| `media/libraries/mpvplayer/build.gradle.kts` | 去掉 `:lib-decoder-ffmpeg` 依赖 | 先不编原生解码器；MPV 运行时由外挂模块提供 |
| `fongmi/buildSrc/.../AbiApkPackaging.groovy` | 无 ffmpeg aar 时跳过 `verifyMpvNative` | 同上 |
| `fongmi/app/build.gradle` | applicationId `com.qbox.app`、版本 1.0.0 | 自有壳 |
| `fongmi/app/.../api/config/VodConfig.java` | 首次启动自动写入默认配置 `https://tvbox.laobaitv.net` | 开箱即用 |
| `fongmi/app/src/main/res/values*/strings.xml` | `app_name` → QBox | 品牌 |

## 环境（.toolchain/，已 gitignore）

- JDK：Temurin 21.0.12.1
- Android SDK：platform 36 + 37.0（`android-37` 为 junction）、build-tools 36/37、platform-tools、NDK 29、cmake 3.22.1
- Gradle：9.7.1（TV 构建）、9.1.0（media 构建）
- 下载器：`.toolchain/Dl.java`

### 必须的环境变量（每次构建）

```
JAVA_HOME=.toolchain/jdk-extract/jdk-21.0.12.1+1
ANDROID_HOME=.toolchain/android-sdk
ANDROID_USER_HOME=.toolchain/android-user-home
GRADLE_USER_HOME=.toolchain/gradle-home
TMP/TEMP=.toolchain/tmp
```

## 已知问题 / 待办

- MPV：`lib-mpvplayer.aar` 已含 `libmpv.so`/`libplayer.so`，但打包时被 `AbiApkPackaging` 排除（设计如此，运行时由 `runtime-mpv` 模块提供）；不装模块时 MPV 引擎不可用，EXO 正常。
- 原生解码器（ffmpeg/mpegh）暂未编：`decoderMode=FFMPEG` 不可用。
- 本机未验证真机：需装 APK 或用模拟器。
- UI/UX 仍是 FongMi 原版 leanback 布局，后续再按 QBox 设计语言改。
