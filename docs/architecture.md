# QBox 架构与二次开发指引

> 产品壳 = FongMi/TV 5.6.1 定制（源码仓 [qbox-tv](https://github.com/zqs1qiwan/qbox-tv)，本地 `fongmi-561/`）。
> 本文说明模块职责、数据流、以及"想改某处该动哪个文件"。

## 模块

| 模块 | 作用 |
|---|---|
| `app` | 应用本体：UI（leanback/mobile）、配置加载、播放器、解析、下载 |
| `catvod` | 爬虫运行时基础设施：`Spider`/`SpiderNull` 接口、OkHttp 封装、JSON/Path/Util 工具 |
| `quickjs` | JS 爬虫引擎（drpy 等 `api: http…js`） |
| `app/libs/*.aar` | media3 fork（自编 18 个模块）+ 第三方引擎（tvbus/thunder/forcetech/jianpian/hook） |

## 启动与配置数据流

```
App.onCreate
  └─ VodConfig.get().init()                 (app/.../api/config/VodConfig.java)
       ├─ 首次启动写入默认配置 Config(DEFAULT_CONFIG)   ← 品牌化改动点
       ├─ load(config)
       │    ├─ Decoder.getJson(url)          (app/.../api/Decoder.java)
       │    │    处理 302 / JPEG 伪装 + "**"base64 / AES CBC / 相对路径
       │    ├─ 失败且是默认源 → assets/qbox_default.json   ← QBox 兜底
       │    └─ checkJson → parseConfig
       │         ├─ BaseLoader.get().parseJar(spider, true)  下载/校验(;md5;)爬虫 jar
       │         └─ sites → Site.objectFrom(json, spider)
       ├─ LiveConfig.load()                  解析 lives 列表（m3u/txt）
       └─ WallConfig.init()
```

## 站点/爬虫加载

```
BaseLoader (app/.../api/loader/BaseLoader.java)
  ├─ csp_Xxx   → JarLoader   DexClassLoader(jar, cacheDir) → 反射 Init.init(context)（加固包在这里解壳）
  ├─ http…js   → JsLoader    QuickJS 加载 drpy 脚本
  ├─ http      → SiteApi     纯 HTTP JSON 接口站
  └─ 其它      → SpiderNull
```

- jar 缓存目录：`Path.jar()`；**自检对话框里的「jar 缓存」= 该目录文件数**。
- 老白源的 jar 是加固包（`BaseSpiderGuard` + `DexNative`），真实逻辑在运行时由 `libftyguard` 解出，所以脱离 Android 无法运行。

## 播放

```
VideoActivity → PlayerManager → Player（Ijk/Exo/MPV 由设置选择）
  ├─ exoplayer：media3 fork（app/libs/lib-exoplayer*.aar），支持 doh、选轨、字幕、弹幕
  ├─ mpv：lib-mpvplayer.aar（libmpv.so + libplayer.so）
  └─ 解析：WebView 嗅探 / JSON API / jar Json / Mix / 超级解析（app/.../api/parser、app/.../server）
```

## 品牌化改动点（都已改过，供后续维护）

| 想改什么 | 文件 |
|---|---|
| 应用名 | `app/src/main/res/values*/strings.xml` 的 `app_name` |
| 图标 / 自适应图标 | `res/mipmap-*`、`res/drawable/ic_launcher_foreground.xml`、`res/mipmap-anydpi-v26/ic_launcher*.xml` |
| 电视 banner | `app/src/leanback/res/drawable/ic_banner*.xml`、`res/drawable/ic_banner.png` |
| 首页/应用内 logo | `res/drawable-nodpi/ic_logo.png` |
| 主题色/底色 | `res/values/colors.xml` + `app/src/leanback/res/values/styles.xml`（`Theme.Base`） |
| 卡片圆角/焦点环 | `app/src/leanback/res/drawable/shape_vod_*.xml`、`res/values/styles.xml`（`Vod.*`） |
| 默认配置源 | `app/.../api/config/VodConfig.java` 的 `DEFAULT_CONFIG` |
| 离线兜底配置 | `assets/qbox_default.json` |
| 自检对话框 | `app/src/leanback/.../HomeActivity.java` 的 `showDiag()` |
| 包名/版本/签名 | `app/build.gradle` + `keystore.properties` |

## 构建与测试

```powershell
.\scripts\build.ps1                 # release，arm64 + v7a
.\scripts\build.ps1 -Variant Debug  # 快速编译验证
.\scripts\test.ps1                  # Robolectric：配置 + 直播
```

工具链在 `.toolchain/`（JDK 21 / SDK 36-37 / NDK 29 / Gradle 9.7.1），环境变量由脚本注入；
细节与踩过的坑见 [AGENTS.md](../AGENTS.md)。

## 与上游的关系

- 基于 FongMi/TV 5.6.1（GPL-3.0），分发需保留源码可获取（本仓库 + qbox-tv 已公开源码路径）。
- 上游 5.8.x 的 UI/播放改动依赖作者私有 media fork（libass/字幕转录/DecoderManager 等），
  公开源无法直接编译；如要跟进，需要先在 `media/` 对应分支补齐这些类。
