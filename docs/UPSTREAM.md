# 上游与 QBox 的改动

QBox 是 [FongMi/TV](https://github.com/FongMi/TV) 的二次开发。这里记录 2.0.0 所对齐的上游版本、保留范围，以及 QBox 自己做了什么，方便后续维护和审计。

## 上游版本

| 层 | 上游仓库 | 版本 / 提交 | 说明 |
|---|---|---|---|
| TV App | [FongMi/TV](https://github.com/FongMi/TV) | **5.6.1 / `125468f0`**（2026-08-27） | QBox 2.0.0 的播放内核与 App 层对齐到这一提交 |
| 媒体层 | [FongMi/media](https://github.com/FongMi/media) | **`release-1.11.0-fongmi` / `3c2cbe8`** | media3 **1.11.0**，自编 18 个 `lib-*-release.aar`，QBox 不改这一层 |

### 为什么把 5.6.1 作为兼容天花板

`release-1.11.0-fongmi` 提供的是 media3 **1.11.0** 的 AAR。FongMi/TV 再往上的提交开始依赖这份 AAR 里不存在的 media3 类：

- `773cae11`（libass 相关）起，编译就会缺类；
- 5.7.8（`4de3f2d`）、5.8.4（`b1bab5b`）都在天花板之上。

因此 2.0.0 选择 **`125468f0`（5.6.1）** 作为 TV 源码的对齐点：它能拿到 FongMi 完整的 Controller / State / Request / Preloader / 播放守卫等 App 层健壮性，同时保持 media3 1.11.0 不动。

## QBox 做了什么

### 播放与交互

- 重接 `PlaybackActivity` / `VideoActivity` 到上游的 Controller、State、Request、Preloader 与 Host 契约；
- 起播位置按请求固定，切集/开播前写播放记录不再污染新集起点；
- 片尾跳集增加 `request == null`、`duration > 0` 守卫，避免首帧或切集时误跳下一集；
- 保留 QBox 的遥控器优先交互：点集预览、全屏控制栏、上下集、换源、清晰度/音轨/字幕/视轨、片头片尾、后台播放等；
- 选集网格保持 QBox 的两排布局、分组、焦点顺序与正序/倒序；
- 站源切换、Toast 浮层、收藏/历史等 UI 层继续使用 QBox 自己的实现。

### 壳子与工程

- `applicationId` 改为 `com.qbox.app`，label 为 `QBox`，使用 QBox 自己的发布签名与在线更新；
- 移除 Chaquopy / Python 爬虫，`com.fongmi.chaquo.Loader` 为空实现，py 站点回退 `SpiderNull`；
- release 不内置任何内容源；`QBOX_TEST_CONFIG` 只在 debug 注入，`qbox_default.json` 只在 `src/debug/assets`；
- 增加 QBox 品牌、首页/详情/播放器/设置等界面与截图；
- CI 增加单测、debug APK 的签名/包名/ABI 闸门，以及 720p 模拟器的无头启动截图；
- 更新检查指向本仓库的 GitHub `releases/latest`，按 ABI 下载 `QBox-<版本>-<abi>.apk`，使用系统 `PackageInstaller` 会话安装。

### Tier B：现在不做

以下功能依赖 FongMi/media 后续公开的新类，2.0.0 **不包含**，等上游 media 版本放开后再补：

- Disc（DiscMenu / 光盘菜单）
- Motion（动态背景/动效）
- 字幕偏移
- BDJ（蓝光菜单）

## 没有改的部分

- media3 仍为 **1.11.0**，`app/libs/*.aar` 不升级、不替换；
- 不接入统计、广告、账号 SDK；
- 不内置、不推荐任何内容源；
- 不修改上游的 GPL-3.0 许可与版权声明。

## 验证范围

2.0.0 发布前做了两层验证：

- JVM / Robolectric 单测与 CI 构建闸门；
- 真实电视盒子（Android 9 / 1080p / armeabi-v7a）验收：两条线路点集、切集、重启后续播、片尾/自动下一集、换源、媒体键、后台、720p 覆盖布局。

未覆盖：换清晰度（当前可达源只返回单 URL，网盘源需要扫码登录，未使用账号验证）、真正 720p 硬件、D16 的 30 集末组翻页边界、4K/DV 角标。它们不影响本次发版，后续有合适源/设备再补。

## 许可证

QBox 遵循 **GPL-3.0**，见仓库根目录 `LICENSE.md`。上游 FongMi/TV、FongMi/media 的版权归其原作者所有；QBox 的修改同样以 GPL-3.0 公开，源码在本仓库 `main` 分支可获取。
