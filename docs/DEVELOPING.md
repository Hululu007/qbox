# 开发、构建与发布

面向参与 QBox 开发的贡献者；普通用户请看 [README](../README.md)。

## 仓库与版本

- 源码与发布：<https://github.com/zqs1qiwan/qbox>，长期分支 `main`。
- 当前版本以 `app/build.gradle` 为准（2.0.4 / 104）。
- TV 源码对齐 FongMi/TV 5.6.1（`125468f0`）；媒体层保持 FongMi/media `release-1.11.0-fongmi` 的 media3 1.11.0，不升级 media3。
- 详细的上游同步记录见 [UPSTREAM.md](UPSTREAM.md)。

## 构建

1. JDK 21、Android SDK（compileSdk 37）、仓库自带 Gradle Wrapper（9.6.1）。
2. 自编媒体 AAR：使用 FongMi/media 的 `release-1.11.0-fongmi` 分支，按 `move.txt` 生成 `lib-*-release.aar`，放进 `app/libs/`。
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

Windows 下用 `gradlew.bat` 执行同样两个任务。

## 发布

1. 修改 `app/build.gradle` 的 `versionCode` / `versionName`。
2. 更新 `docs/RELEASE-NOTES-<版本>.md`。
3. 构建两个 ABI 的 release APK，并验证包名、版本、签名和 ABI 载荷。
4. `git archive` 导出源码包 `QBox-<版本>-source.zip`。
5. 提交并打 `v<版本>` tag，CI 自动创建 GitHub Release。
6. 资产命名：`QBox-<版本>-arm64-v8a.apk`、`QBox-<版本>-armeabi-v7a.apk`、`QBox-<版本>-source.zip`；`QBox-latest-*.apk` 是指向最新版的固定链接副本。

## 约束

- release 包不包含任何内容源；`QBOX_TEST_CONFIG` 只在 debug 构建注入，`qbox_default.json` 只放在 `src/debug/assets`。
- 不接入统计、广告或账号 SDK。
- 提交前确认没有把 `local.properties`、`*.jks`、`build/`、`Release/`、真实配置、账号、token、设备测试记录带进仓库。
- CI 与无头测试细节见 [HEADLESS.md](HEADLESS.md)。
