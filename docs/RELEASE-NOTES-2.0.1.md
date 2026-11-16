# QBox 2.0.1

发布日期：2026-11-16

这是 QBox 2.0.0 之后的**图标、品牌视觉与文档更新版**。播放内核与媒体层没有变化。

## 本次更新

- 沿用实际 APK 使用的 QBox 立方体 Q 标识：启动器、设置页、首页头部、启动图和 Android TV 横幅使用同一套视觉。旧的蓝红六边形标识废弃，不再使用。
- 保留实际 APK 使用的立方体 Q 图标资源：各密度启动图标、自适应图标与启动图保持一致；旧的蓝红六边形标识废弃，不再使用。
- 修复快进/拖动进度时暂停图标残留：seek 过程中播放器短暂 `isPlaying=false`，旧逻辑误判为暂停，自动恢复播放后中央暂停标志仍不消失；现在只在 `playWhenReady=false` 的用户主动暂停状态显示。
- `AndroidManifest.xml` 显式配置 `android:icon` / `android:roundIcon`，并在 `HomeActivity` 上声明图标，避免部分电视系统在 Toast、安装器或启动器中回退到默认 Android 机器人图标。
- 同时保留 `mipmap-anydpi-v26` 自适应图标与各密度 PNG，兼容不同 Android TV 版本的图标读取链路。
- 重写 README：补充项目性质、上游来源、截图、构建与发布说明。
- 更新全部界面截图为 2.0.1 实机截图。

## 项目性质

QBox 是 **FongMi/TV 的 UI / 交互重构版 TVBox 客户端**，面向 Android TV 与电视盒子。

- 本项目**不涉及视频源等信息**：不提供、不内置、不采集、不托管、不分发任何视频源、影视内容、直播源、账号或配置接口；
- 主要工作是基于个人使用习惯和审美，对界面布局、焦点逻辑、遥控器交互、播放控制与视觉样式做重构；
- 播放内核、媒体层与上游基础能力来自 FongMi/TV、FongMi/media，版权归其原作者所有；
- 与 FongMi 官方没有隶属关系，也不是 FongMi 官方发行版。

## 版本

- `versionCode 101` / `versionName 2.0.1`，高于 2.0.0 的 `100`。
- 包名 `com.qbox.app`，与 1.9.13+、2.0.0 使用同一签名，可直接覆盖安装。
- 上游不变：FongMi/TV 5.6.1（`125468f0`）；media 层保持 `release-1.11.0-fongmi` 的 media3 1.11.0。

## 升级方式

- 2.0.0 / 1.9.24 及之后版本：启动弹窗或「设置 → 版本」检查更新，点「更新」即可下载并覆盖安装。
- 老客户端若无法访问 GitHub，可手动下载本 Release 的 `QBox-2.0.1-armeabi-v7a.apk`（32 位盒子）或 `QBox-2.0.1-arm64-v8a.apk`（64 位盒子）。

## 安装包

| 文件 | 适用设备 |
|---|---|
| `QBox-2.0.1-armeabi-v7a.apk` | 32 位老盒子 / Android TV |
| `QBox-2.0.1-arm64-v8a.apk` | 64 位盒子 / Android TV |
| `QBox-2.0.1-source.zip` | 对应版本源码 |

## 说明

- QBox 只是播放器，不提供、不内置、不附带任何影视内容或配置源。
- 本版本遵循 GPL-3.0；上游版权归 FongMi/TV、FongMi/media 原作者所有。
