<p align="center">
  <img src="app/src/main/assets/qbox-logo.png" width="132" alt="QBox logo">
</p>

<h1 align="center">QBox</h1>

**为 Android TV 和电视盒子打造的清爽播放器。** 无广告、无捆绑、不强制登录，导入自己的 TVBox 兼容配置即可使用。

> **QBox 只是播放器，不提供、不内置、不附带任何影视内容、播放源或配置源。** 界面上的分类、海报、搜索结果和直播频道，全部来自你导入的配置源；内容是否合法、如何使用，由你自行判断并承担全部责任。

<p align="center">
  <img src="docs/screenshots/ui-cinema/v203-home.jpg" width="100%" alt="首页">
  <img src="docs/screenshots/ui-cinema/v203-filter.jpg" width="49%" alt="分类与筛选">
  <img src="docs/screenshots/ui-cinema/v203-detail.jpg" width="49%" alt="详情页">
  <img src="docs/screenshots/ui-cinema/v203-episodes.jpg" width="100%" alt="剧集列表与长名称滚动">
  <img src="docs/screenshots/ui-cinema/v203-player.jpg" width="49%" alt="播放控制条">
  <img src="docs/screenshots/ui-cinema/v203-pause.jpg" width="49%" alt="暂停精简态">
</p>

> 界面截图来自 QBox 2.0.3 实机；图中片名、海报、配置名均来自测试配置，仅用于展示界面。QBox 不提供任何内容源。

## 功能

- 海报墙、分类浏览、筛选与聚合搜索
- 自动识别剧集名：从资源文件名提取集数/期数，左侧显示固定前缀，右侧保留完整原名并滚动展示
- 遥控器优先：焦点清楚、层级简单，老人小孩也能上手
- 播放记录、收藏、断点续播，数据只保存在本机
- 多音轨、字幕、倍速、片头片尾、循环播放
- 左右快进阶梯加速：轻按 10 秒，按住可加速到 5 分钟一步
- 支持 m3u / txt 直播源，分组选台
- 适配 720p / 1080p / 4K 电视，支持 32 位和 64 位设备
- 无开屏广告、无弹窗、无后台统计，也不需要注册账号
- 内置检查更新，发现新版本可直接下载安装

## 下载

前往 **[GitHub Releases](https://github.com/zqs1qiwan/qbox/releases/latest)** 下载最新版本：

- [arm64-v8a](https://github.com/zqs1qiwan/qbox/releases/latest/download/QBox-latest-arm64-v8a.apk)：大多数 2019 年后的电视 / 盒子
- [armeabi-v7a](https://github.com/zqs1qiwan/qbox/releases/latest/download/QBox-latest-armeabi-v7a.apk)：较老的 32 位设备

支持 Android 7.0 及以上。两个安装包使用相同签名，可直接覆盖安装，数据不会丢失。装过 1.9.12 或更早版本的用户需要先卸载旧版；1.9.13 及之后可以直接覆盖。

## 安装

1. 下载与设备架构匹配的 APK
2. 用 U 盘、电视文件管理器或手机传输工具安装（首次安装需允许「未知来源」）
3. 打开 QBox，点击「添加配置源」，粘贴配置地址即可使用

## 配置源

QBox **不内置、不提供、不推荐任何影视内容或配置源**。配置由你自行准备，支持网络地址、本地文件等常见 TVBox 格式；设置页可以分别管理点播和直播配置。

## 隐私

- 不收集个人信息，不接入统计或广告 SDK
- 不需要账号，不会上传播放记录
- 配置、历史、收藏均保存在本机，卸载即清除

QBox 基于 [FongMi/TV](https://github.com/FongMi/TV) 二次开发，遵循 **GPL-3.0**，仅做界面与交互重构，不涉及任何视频源、账号或配置接口。
