# qbox-tv（QBox 源码，私有）

公开下载与安装包在 <https://github.com/zqs1qiwan/qbox> 的 Releases。
本仓库只放源码，**不公开**，每个 Release 附对应版本的源码 zip。

## 关键事实

- 基于 FongMi/TV **5.6.1**，`applicationId com.qbox.app`，label `QBox`
- 发布分支 `ui-cinema`，`main` 同步
- 当前版本在 `app/build.gradle`（写这份笔记时是 1.9.14 / 72）
- 媒体层不在 Maven：`app/libs/*.aar` 由 FongMi/media `release-1.11.0-fongmi` 自编
- Python 爬虫已移除：`chaquo/` 模块已删，`com.fongmi.chaquo.Loader` 是空实现
- release 不内置任何内容源：`QBOX_TEST_CONFIG` 仅在 debug 有值，`qbox_default.json` 只在 `src/debug/assets`

## 构建

```powershell
# 仓库外的构建脚本（会自动设置 JDK/SDK/GRADLE 环境变量）
..\.toolchain\build.ps1 -Abi all -Variant Release
..\.toolchain\build.ps1 -Abi v7a -Variant Debug
```

签名读根目录 `local.properties`（**不提交**）：

```properties
sdk.dir=...
storeFile=C:/path/to/keystore.jks
keyAlias=qbox
storePassword=...
keyPassword=...
```

当前签名密钥：`.toolchain/keys/qbox-release-v2.jks`（仓库外，2026-11 换的新 key；
旧 key 曾泄露，已停用）。

## 测试

```powershell
..\.toolchain\test.ps1
```

Robolectric 配置测试 + 直播解析测试；输出 `QBOX_TEST_OK sites=...`。

## 发布流程

1. 改 `app/build.gradle` 的 `versionCode` / `versionName`
2. 构建两个 ABI 的 release
3. `git archive --format=zip --prefix=QBox-<ver>-source/ HEAD -o dist/QBox-<ver>-source.zip`
4. 在 `qbox` 仓库发 Release：两个 APK + 源码 zip
5. 更新 `qbox` 的 README 版本说明与 `AGENTS.md`

## 绝不提交

- `local.properties`、`*.jks`、`keystore.properties`、`*.pass`
- `build/`、`Release/`、`.gradle/`、`*.log`
- 任何真实内容源配置 / 账号 / 内网地址

## 设备

- 测试机顶盒：`192.168.2.214:5555`（Android 9 / armeabi-v7a / 1280×720）
- 不要动 192.168.2.130 的电视
