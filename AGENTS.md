# qbox-tv（QBox 源码，私有）

公开下载与安装包在 <https://github.com/zqs1qiwan/qbox> 的 Releases。
本仓库只放源码，**不公开**，每个 Release 附对应版本的源码 zip。

## 开工必读

- 改任何 UI/交互代码前，先读 `..\docs\GOLDEN-STANDARDS.md`（项目金标准，GS-xx 编号）：Blocker 级规则违反直接打回；提交前过其 §六 回归清单与「改了 A 必须验 B」映射表。
- **每次修复若产生新教训，必须回流一条规则**到 GOLDEN-STANDARDS.md（新增/修改 GS-xx 并在其 §七 登记日期与编号）；过程细节记 `..\docs\uiux-review-log.md`，金标准只留规则。

## 关键事实

- 基于 FongMi/TV **5.6.1**，`applicationId com.qbox.app`，label `QBox`
- 发布分支 `ui-cinema`，`main` 同步
- 当前版本以 `app/build.gradle` 为准（检查时为 1.9.19 / 77）
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

## 无头测试 / CI

详情与限制见 `docs/HEADLESS.md`。要点：

- `.github/workflows/ci.yml` 在 Linux runner 上检出源码，从 FongMi/media `release-1.11.0-fongmi` 自编 18 个 `lib-*-release.aar`；
- CI 生成一次性临时 keystore（**不得**用于官方发布），跑 Robolectric 单测、打 `leanbackX86_64Debug`；
- `smoke/mock-server.ps1` 是纯合成占位内容（无真实站点/账号/直播源），`smoke/headless-screenshot.ps1` 启动 720p x86_64 无头 AVD、导入 mock、截 `qbox-home.png` 与 UI dump；
- `ci.yml` 通过 `-PqboxTestConfig=http://10.0.2.2:8765/config.json` 覆盖 debug 首启配置；不传时仍是老白测试源，release 仍不带任何源；
- `smoke/baselines/` 只放人工确认过的 720p 基线，其他截图在 `artifacts/`，不进版本库；
- 无头只适合安装/启动/焦点/按键/布局检查，**不能**代替真机验收老白 spider、硬解/DRM、遥控器长按、720p/HDMI；
- 更新基线或修改 mock 契约时，同步更新 `docs/HEADLESS.md` 与本文件。

## 发布流程

1. 改 `app/build.gradle` 的 `versionCode` / `versionName`
2. 构建两个 ABI 的 release
3. `git archive --format=zip --prefix=QBox-<ver>-source/ HEAD -o dist/QBox-<ver>-source.zip`
4. 在 `qbox` 仓库发 Release：两个 APK + 源码 zip
5. 更新 `qbox` 的 README 版本说明与 `AGENTS.md`

## 绝不提交

- `local.properties`、`*.jks`、`keystore.properties`、`*.pass`
- `build/`、`Release/`、`.gradle/`、`*.log`、`artifacts/`
- 任何真实内容源配置 / 账号 / 内网地址
- CI 一次性 keystore（只存在 runner /tmp，不得落盘提交）
- 未经人工确认的临时截图；`smoke/baselines/` 仅收录确认过的 720p 基线

## 设备

- 测试机顶盒：`192.168.2.214:5555`（Android 9 / armeabi-v7a / 1280×720）
- 不要动 192.168.2.130 的电视
