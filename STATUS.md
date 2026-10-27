# QBox 当前状态（一句话版）

**壳已可用、可复现构建、可发布；只剩「真机播放验证」和「UI 方向确认」两件事需要你。**

---

## 交付物

| 包 | 用途 | 位置 |
|---|---|---|
| `QBox-1.3.2-arm64-v8a.apk` / `-armeabi-v7a.apk` | **稳定版**，日常用这个 | `dist/` + [Release v1.3.2](https://github.com/zqs1qiwan/qbox-tv/releases/tag/v1.3.2) |
| `QBox-1.4.0-hero-arm64-v8a.apk` | 实验：首页 hero + 三 tab，仅供对比 | `dist/` + [Pre-release v1.4.0-hero](https://github.com/zqs1qiwan/qbox-tv/releases/tag/v1.4.0-hero) |

- 包名 `com.qbox.app`，minSdk 24，v2 签名（证书 SHA-256 `f7c4b32c…f63c62a`）。
- 默认配置 `https://tvbox.laobaitv.net`，首启自动载入；拉取失败回退内置 `assets/qbox_default.json`。

## 已经用代码证明的（可复现）

```
scripts\test.cmd
  QBOX_TEST_OK      sites=49 spider=…jpg;md5;b55978c4…
  QBOX_FALLBACK_OK  sites=49
  QBOX_LIVE_SUMMARY parsed=5 channels=2675
  QBOX_LIVE_STREAM  …/cctv1.m3u8 -> 206 application/vnd.apple.mpegurl
  QBOX_UI_OK        hero=174px title=41px        （仅 ui-hero 分支）
```

含义：配置下载/302 跳转/JPEG 伪装解码、内置兜底、直播源解析、首页布局 inflate 全部在壳的真实代码里跑通。

## 还没证明的（需要电视）

1. **点播播放**：老白 jar 是加固包（`BaseSpiderGuard` + `DexNative` 原生解壳），脱离 Android 无法运行；
   本机模拟器（WHPX/TCG）与原生编译在沙箱内都不可行，已记录在 AGENTS.md。
2. **UI 观感/焦点路径**：hero 布局能 inflate、tab 可聚焦，但好看与否、hero↔列表切换顺不顺只能真机看。

## 需要你做的三件事

1. 装 `1.3.2`，首页长按标题 → 自检截图（重点看 `站点：49`、`jar 缓存 ≥1`）；
2. 按 [docs/acceptance.md](acceptance.md) 走一遍点播/直播，卡住就把现象发我；
3. 有余力再装 `1.4.0-hero` 对比首页，说一句"留/不要/哪里别扭"。
   （装过 hero 后要回 1.3.2 必须先卸载——versionCode 不能降级覆盖。）

## 复现与维护

```powershell
.\scripts\build-media-aars.cmd   # 新克隆必跑一次（18 个 media3 aar）
.\scripts\build.cmd              # 出 release APK（arm64 + v7a）
.\scripts\test.cmd               # 配置/直播/布局测试
```

文档：`docs/architecture.md`（结构+改动点）、`docs/agent-notes/fork-plan.md`（为什么选 5.6.1）、
`docs/design/ui-plan.md`（UI 进度）、`AGENTS.md`（构建坑与环境事实）。

## 已知限制

- Python 爬虫已裁掉（Chaquopy 需宿主 Python 3.10）；
- FFMPEG 解码模式不可用（原生编译在沙箱内卡死）；
- 在线更新已关闭（原指向 FongMi 官方源），更新走 GitHub Release 手动下载。
