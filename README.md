# QBox 源码

QBox 是面向 Android TV / 电视盒子的播放器，基于 [FongMi/TV 5.6.1](https://github.com/FongMi/TV) 定制。
**QBox 只提供播放器本身，不提供、不内置、不附带任何影视内容或配置源**，所有内容由用户自行导入配置后显示。

- 应用 ID：`com.qbox.app`
- 发布分支：`ui-cinema`（`main` 跟随发布）
- 许可：GPL-3.0，见 `LICENSE.md`

## 目录

| 路径 | 说明 |
|---|---|
| `app/` | Leanback 主模块（`src/leanback` 为电视界面，`src/main` 为公共代码） |
| `catvod/` | 配置解析 / 爬虫运行时 |
| `quickjs/` | JS 爬虫引擎 |
| `app/libs/*.aar` | 媒体层 AAR（ExoPlayer / MPV 等），源码来自 FongMi/media |
| `thunder/` `tvbus/` `hook/` `jianpian/` `forcetech/` `zlive/` | 对应 AAR 的源码模块，不参与主工程构建 |
| `docs/` | 上游配置与使用文档 |

Python 爬虫（Chaquopy）已移除，`com.fongmi.chaquo.Loader` 是空实现，py 站点回退到 `SpiderNull`。

## 构建

1. JDK 21、Android SDK（compileSdk 37）、Gradle 9.7.1
2. 自编媒体 AAR：使用 [FongMi/media](https://github.com/FongMi/media) 的 `release-1.11.0-fongmi` 分支，
   按其中的 `move.txt` 生成 `lib-*-release.aar`，放进 `app/libs/`
3. 在仓库根目录创建 `local.properties`（**不要提交**）：

```properties
sdk.dir=/path/to/android-sdk
storeFile=/path/to/keystore.jks
keyAlias=your-alias
storePassword=******
keyPassword=******
```

4. 构建：

```bash
./gradlew :app:assembleLeanbackArmeabi_v7aRelease
./gradlew :app:assembleLeanbackArm64_v8aRelease
```

产物：`app/build/outputs/apk/leanback*/release/`。

Windows 下可以用 `.toolchain/build.ps1 -Abi all -Variant Release`（脚本在仓库外，自动注入工具链环境变量）。

## 签名

- 签名密钥保存在仓库外，**绝不提交** `*.jks`、`keystore.properties`、`local.properties`
- release 与 debug 使用同一签名时，可以互相覆盖安装而不丢数据
- 更换签名后，老版本必须先卸载再安装

## 发布

1. 修改 `app/build.gradle` 的 `versionCode` / `versionName`
2. 构建两个 ABI 的 release APK
3. `git archive` 导出源码包 `QBox-<版本>-source.zip`
4. 上传到公开仓库 `zqs1qiwan/qbox` 的 GitHub Release（安装包 + 源码包）

## 约束

- **release 包不包含任何内容源**：`QBOX_TEST_CONFIG` 只在 debug 构建注入，`qbox_default.json` 只放在 `src/debug/assets`
- 不接入统计、广告或账号 SDK
- 提交前确认没有把 `local.properties`、`*.jks`、`build/`、`Release/` 带进仓库
