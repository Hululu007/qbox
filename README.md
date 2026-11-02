# QBox 源码

QBox 是面向 Android TV / 电视盒子的播放器，基于 [FongMi/TV 5.6.1](https://github.com/FongMi/TV) 定制。
**QBox 不提供、不内置任何影视内容或配置源**，所有内容由用户自行导入配置后显示。

- 应用 ID：`com.qbox.app`
- 分支：`ui-cinema`（发布分支）、`main`（跟随发布）
- 许可：GPL-3.0（见 `LICENSE.md`）

## 构建

1. 安装 JDK 21、Android SDK（compileSdk 37）与 Gradle 9.7.1
2. 自编 media AAR：使用 [FongMi/media](https://github.com/FongMi/media) 的
   `release-1.11.0-fongmi` 分支，产物放到 `app/libs/`
3. 在仓库根目录创建 `local.properties`（不要提交）：

```properties
sdk.dir=/path/to/android-sdk
storeFile=/path/to/qbox-release.jks
keyAlias=qbox
storePassword=******
keyPassword=******
```

4. 构建：

```bash
./gradlew :app:assembleLeanbackArmeabi_v7aRelease
./gradlew :app:assembleLeanbackArm64_v8aRelease
```

产物在 `app/build/outputs/apk/leanback*/release/`。

## 说明

- 默认配置只在 debug 构建里注入，release 包不包含任何内容源
- 应用内更新检查 GitHub Release 的 latest tag，按 ABI 下载对应安装包
- 源码中的 `app/src/debug/assets/qbox_default.json` 仅用于本地调试与单测
