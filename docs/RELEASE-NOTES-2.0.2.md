# QBox 2.0.2

发布日期：2026-11-16

这是 QBox 2.0.1 之后的**选集长名称阅读与最近观看线路恢复修复版**。播放内核、媒体层和界面结构没有变化。

## 本次更新

### 1. 选集长名称可滚动、可看全

问题有两个叠加原因，缺一不可：

- 选集格里的 `text` 原来带 `android:duplicateParentState="true"`，父容器状态会覆盖 `EpisodeAdapter` 程序化设置的 `selected=true`，`ellipsize=marquee` 不启动；
- 更关键的是，`EpisodeInfo` 对「逃出布迪秀 Buddy 2026 … 佬友 LY-96 … .mkv」这类没有日期/集数标记的资源名，会走「识别不出集数/日期」分支：`label` 等于整段清理后的文件名，`rest` 也为空，随后 `rest.equals(label)` 又把 `rest` 清空。`EpisodeAdapter` 因此把整段长名塞进固定不滚动的 `prefix`，`text` 为空，焦点格自然看不到滚动。

2.0.2 的处理：

- `text` 去掉 `duplicateParentState`，改用独立的 `episode_text` 颜色选择器，聚焦时只由自身的 `selected` 驱动 marquee；
- `EpisodeAdapter` 对 `rest` 为空的条目改用**原始资源名**填充可滚动的 `text`，固定前缀只保留源提供的 `desc`（通常为空或集号）；
- 有日期/集数结构的条目保持原策略：前缀固定显示日期/集数，`text` 滚动原资源名；清晰度、动态范围与声道角标位置不变。

### 2. 从「最近观看」恢复上次线路

- 首次打开详情时，以 `History.vodFlag` / `vodRemarks` / `episodeUrl` 为权威，先恢复历史线路与集数，再考虑可能已过期的播放请求；刷新、换源仍优先当前播放请求。
- `History.findEpisode` 在匹配到同名历史里的集之前，不再把第一路 Flag 写回当前历史；只有确实找不到可匹配集时才回落到第一路。
- Flag 列表更新后重新渲染当前选中线路，避免 `FlagAdapter.addAll` 把选中位置落回第一项。
- 线路名增加首尾空格容错；恢复集数时优先按历史里的 `episodeUrl` 精确匹配，再按集名严格/宽松匹配。

## 项目性质

QBox 是 **FongMi/TV 的 UI / 交互重构版 TVBox 客户端**，面向 Android TV 与电视盒子。

- 本项目**不涉及视频源等信息**：不提供、不内置、不采集、不托管、不分发任何视频源、影视内容、直播源、账号或配置接口；
- 主要工作是基于个人使用习惯和审美，对界面布局、焦点逻辑、遥控器交互、播放控制与视觉样式做重构；
- 播放内核、媒体层与上游基础能力来自 FongMi/TV、FongMi/media，版权归其原作者所有；
- 与 FongMi 官方没有隶属关系，也不是 FongMi 官方发行版。

## 版本

- `versionCode 102` / `versionName 2.0.2`，高于 2.0.1 的 `101`。
- 包名 `com.qbox.app`，与 1.9.13+、2.0.0、2.0.1 使用同一签名，可直接覆盖安装。
- 上游不变：FongMi/TV 5.6.1（`125468f0`）；media 层保持 `release-1.11.0-fongmi` 的 media3 1.11.0。

## 升级方式

- 2.0.0 / 2.0.1 / 1.9.24 及之后版本：启动弹窗或「设置 → 版本」检查更新，点「更新」即可下载并覆盖安装。
- 老客户端若无法访问 GitHub，可手动下载本 Release 的 `QBox-2.0.2-armeabi-v7a.apk`（32 位盒子）或 `QBox-2.0.2-arm64-v8a.apk`（64 位盒子）。

## 安装包

| 文件 | 适用设备 |
|---|---|
| `QBox-2.0.2-armeabi-v7a.apk` | 32 位老盒子 / Android TV |
| `QBox-2.0.2-arm64-v8a.apk` | 64 位盒子 / Android TV |
| `QBox-2.0.2-source.zip` | 对应版本源码 |

## 验证记录

- 测试机：`192.168.2.245:5555`，Android 9，1920×1080 / density 240 / armeabi-v7a。
- 覆盖安装 2.0.2/102 后原有配置与「最近观看」数据保留，首页正常。
- 从「最近观看」进入「逃出布迪秀 Buddy 2026(1)」：详情默认选中「夸父智」，UI dump 中「夸父智」`selected=true`、「夸父原」`selected=false`；历史数据库里 `vodFlag` 为「夸父智」。
- 聚焦长名选集格时，UI dump 显示完整资源名位于 `text`（不再是 `prefix`），间隔截图可见文字滚动；不再只显示被截断的固定前缀。
- 手动在「夸父原 / 夸父智」之间切换、退出重进、切到其他集再切回，均按当前选择保存。

## 说明

- QBox 只是播放器，不提供、不内置、不附带任何影视内容或配置源。
- 本版本遵循 GPL-3.0；上游版权归 FongMi/TV、FongMi/media 原作者所有。
- 自动更新的「下载并覆盖安装」仍依赖系统 `PackageInstaller` 的安装确认；本版本只验证了同签名覆盖安装和版本弹窗识别，不宣称所有电视系统都能静默安装。
