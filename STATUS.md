# QBox 当前状态

**已达成：环境可复现、壳可构建、测试源可载入、可点播播放、可直播、UI 品牌化 + 首页改版；
真机（Sony BRAVIA 4K / Android 12）实测通过。**

---

## 交付物

| 包 | 说明 |
|---|---|
| `QBox-1.5.0-arm64-v8a.apk` | 当前稳定版（UI v2 首批） |
| `QBox-1.5.0-armeabi-v7a.apk` | 当前稳定版（32 位老电视，本次测试机就是这种） |
| Release | https://github.com/zqs1qiwan/qbox-tv/releases/tag/v1.5.0 |

- `applicationId com.qbox.app`，minSdk 24，v2 签名（`f7c4b32c…f63c62a`）。
- 默认配置 `https://tvbox.laobaitv.net`，首启自动载入；失败回退内置 `assets/qbox_default.json`。

## 真机实测（2026-10-28，Sony BRAVIA 4K VH2 / Android 12 / 1920×1080）

通过 adb 连接电视（`192.168.2.130:5555`）实际操作，截图见 [`docs/screenshots/`](screenshots)。

| 项目 | 结果 |
|---|---|
| 安装启动 | ✅ 桌面图标/名称 QBox，首页正常 |
| 配置载入 | ✅ 自检显示 站点 **49**、直播 **6**、jar 缓存 2、spider md5 匹配 |
| 点播浏览 | ✅ 站点列表 → 分类/海报网格（含评分、筛选） |
| 详情 | ✅ 简介/线路/剧集，海报与线路正常 |
| **播放** | ✅ `media_session` `state=3`，position 31s→58s 递增，画面推进 |
| **直播** | ✅ CCTV1 起播，频道号 001、线路 1 |
| 自检 | ✅ 长按「首页」tab 弹出，版本 1.4.0-hero/1.4.0 正常 |
| hero/tab 焦点 | ✅ 上键入 tab（白色药丸），左右切换，OK 生效（直播 tab 进 LiveActivity） |

已知小问题（不影响使用）：
- 部分网盘源（玩偶哥哥等）会弹「网盘源需使用夸克浏览器APP扫码登录」，属源本身限制；
- 详情页偶发「弹幕服务启动失败」Toast，弹幕不可用，不影响播放；
- 首页标题显示的是当前"首页站点"名（如「豆豆｜片单」），不是配置名。

## 离线可复现的验证

```powershell
.\scripts\test.cmd
  QBOX_TEST_OK      sites=49 spider=…jpg;md5;b55978c4…
  QBOX_FALLBACK_OK  sites=49
  QBOX_LIVE_SUMMARY parsed=5 channels=2675
  QBOX_LIVE_STREAM  …/cctv1.m3u8 -> 206 application/vnd.apple.mpegurl
  QBOX_UI_OK        hero=174px title=41px
```

## 构建/维护

```powershell
.\scripts\build-media-aars.cmd   # 新克隆必跑一次（18 个 media3 aar → app/libs）
.\scripts\build.cmd              # release，arm64 + v7a
.\scripts\test.cmd               # 配置 / 直播 / 首页布局
```

- 结构、数据流、改动点对照：[docs/architecture.md](architecture.md)
- 为什么基于 5.6.1、媒体层怎么编：[docs/agent-notes/fork-plan.md](agent-notes/fork-plan.md)
- 电视验收步骤：[docs/acceptance.md](acceptance.md)
- UI 方向与后续：[docs/design/ui-plan.md](design/ui-plan.md)

## 已知限制

- Python 爬虫已裁掉（Chaquopy 需宿主 Python 3.10）；
- FFMPEG 解码模式不可用（原生编译在沙箱内卡死，已记录）；
- 在线更新关闭（原指向 FongMi 官方源），更新走 GitHub Release 手动下载；
- 弹幕服务在当前构建下启动失败（待查）。
