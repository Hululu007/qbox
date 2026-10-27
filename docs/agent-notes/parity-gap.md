# 与 OK影视（FongMi 系）对齐的差距分析

日期：2026-10-26　依据：本仓库全部源码（1,349 行 Kotlin）+ FongMi/TV `fongmi` 分支源码（5.8.2，2026-10-26 仍在推送）。

## 0. 结论

1. 现在缺的不是"效果"，是**壳内的整个后端运行时**：站点分派、jar 运行时契约、本地代理、解析（parse）引擎、直播/EPG、数据层、播放引擎抽象。qbox 目前只有其中最薄的一层（反射调 jar + ExoPlayer）。
2. 代码里有 **3 个"必然失败"级缺陷**，导致连 jar 源的主链路都跑不通——不是体验差，是分类页/详情页打不开。见 §2 的 B1/B2/B3。
3. 用户默认源（老白 `tvbox.laobaitv.net` → 饭太硬系 jar）是 **csp_ jar**，所以 jar 契约 + 解析层做对，就能覆盖日常使用的大头；JS/Py/Node 可以后置。
4. "后端源码来自 fongmi" 的说法要精确化：TVBox **没有服务端**，所谓后端 = 壳内的 spider 运行时 + 解析器。OK影视 就是 FongMi/TV 的续更版，两者源码同源。所以"搬后端"=把 FongMi 的 catvod 运行时语义搬进壳里，不是调什么接口。

## 1. 现状盘点

| 模块 | 文件 | 行数 | 实际能力 |
|---|---|---|---|
| 配置 | `config/Config.kt` `ConfigRepository.kt` `SiteParcel.kt` | ~230 | json/gzip/zip/base64 探测、`./` 相对路径、spider md5 |
| Spider | `spider/JarLoader.kt` `SpiderRuntime.kt` | ~200 | DexClassLoader + 反射调 6 个方法 |
| 模型 | `model/Content.kt` `PlayParser.kt` | ~115 | Gson 映射 + `vod_play_url` 解析 |
| 播放 | `player/QPlayer.kt` | 44 | ExoPlayer 单实例 + OkHttp 请求头 |
| UI | `ui/*.kt` | ~780 | 三屏（站点墙 / 分类+海报 / 详情+内嵌播放） |
| 测试 | `PlayParserTest` | 少量 | 只测 `vod_play_url` 解析 |

对比量级：FongMi/TV 的 Java 源码 3.32 MB（约 7–8 万行），其中 `PlaybackActivity` 48 KB、`PlayerManager` 43 KB、`SpiderTestSession` 20 KB、`SiteApi` 16 KB、`EpgParser` 15 KB、`ParseJob` 10 KB。qbox 是它的 ~1.7%。

## 2. 硬缺陷（按严重度，均可在代码里定位）

### B1 `homeContent` 的 `class` 永远解析不到 → 点播页必然空白

`model/Content.kt:49-53`：

```kotlin
data class HomeResult(
    val class_: List<ClassInfo>? = null,   // ← Gson 默认 FieldNamingPolicy.IDENTITY
    ...
```

Gson 用字段名做 JSON key，字段叫 `class_`，而 TVBox 协议 `homeContent` 返回的是 `"class"`。
→ `VodActivity.kt:108` 的 `home.class_.orEmpty()` 恒为空 → `cats` 空 → `currentTid=""` → `loadPage` 直接 return，网格永远空。

修法：`@SerializedName("class") val categories: List<ClassInfo>?`。

### B2 `categoryContent` 反射传错集合类型 → 100% 抛异常

FongMi 契约（`Spider.java`）：`categoryContent(String tid, String pg, boolean filter, HashMap<String,String> extend)`。
`SpiderRuntime.kt:47-51` 传的是 `Map<String,String>`，`VodActivity` 实参是 Kotlin `emptyMap()`（`kotlin.collections.EmptyMap`，不是 `HashMap`）。

`Method.invoke` 做参数可赋值性检查 → `IllegalArgumentException` → 被 `runCatching` 吞成"翻页失败/加载失败"。

修法：构造 `HashMap<String,String>`；并修正 `searchContent(key,false,1)` 传 Int 给 `String pg` 的问题（`SpiderRuntime.kt:59-71` 目前靠 catch 回退）。

### B3 jar 运行时契约缺失 → 大量源"看起来能进，实际播不了"

`JarLoader.kt` 只做了三件事：下载、`DexClassLoader`、反射。FongMi 的 `JarLoader` 还要求：

| 契约 | FongMi 行为 | qbox |
|---|---|---|
| `com.github.catvod.spider.Init.init(Context)` | 加载 jar 后先调，很多爬虫的静态上下文靠它 | 未调 |
| `spider.siteKey = key` | proxy URL / 弹幕 URL / 多站隔离 | 未设（null） |
| `spider.getProxyUrl()` → `http://127.0.0.1:9978/proxy?do=jar&siteKey=...` | 宿主跑 NanoHTTPD 本地代理，把 `proxy://` 落地 | 无本地服务 → 图片/播放地址 404 |
| jar 内 `Proxy.proxy(Map)` 注册 | 加载时反射注册，按 jar 分派 | 无 |
| jar 内 `Json*` / `Mix*` 解析类 | `jsonExt` / `jsonExtMix` 反射调用（parse 类型 2/3） | 无 |
| `Spider.net` / `local` / `js` / `dialog` 注入 | 新 jar 依赖这些宿主服务 | 无 |

后果：凡是返回 `spider.getProxyUrl(...)`、`proxy://`、`json:` 前缀的源，全部静默失败。

### B4 站点分派只看 `type` + `api` 非空 → 非 csp_ 站点全废

`HomeActivity.kt:205-207` 过滤 `type==0||type==3` 且 `api` 非空，然后一律走 jar。FongMi 的分派完全按 api 前缀（`BaseLoader.getSpider`）：

```
node:xxx → NodeLoader      *.py → PyLoader      *.js → JsLoader
csp_xxx  → JarLoader       其它   → SpiderNull（xml api 站由 SiteApi 走 HTTP 聚合接口）
```

`api: https://xxx/api.php/provide/vod/` 这种"采集站"（配置里占比很高）在 qbox 会被当成 jar 类名加载，必抛 `ClassNotFound`。

### B5 解析（parse）层完全缺失 → "该集无直链"就是终局

`VideoActivity.kt:184-202` 只认 `PlayerResult.realUrl()`，没有直链就提示"M2 暂不支持"，`parse` 字段（`Content.kt:70-80`）读进来后丢弃。

FongMi 的 `ParseJob` 按 `parse.type` 分 5 路：

| type | 实现 | 依赖 |
|---|---|---|
| 0 | `ParseWebView` 注入嗅探脚本（含 click 流程） | WebView + 嗅探 JS |
| 1 | JSON 解析 API：`url + webUrl`，读回 `{url, headers}` | OkHttp |
| 2 | jar 内 `com.github.catvod.parser.Json<名字>` | 反射，无需 JS |
| 3 | jar 内 `Mix<名字>` 聚合解析 | 反射 |
| 4 | 超级解析：并行跑所有 type0/1 | 组合 |

另有 `playUrl` 前缀 `json:`（临时 JSON 解析）与 `parse:<名字>`（指定解析器）。qbox 一个都没有，所以"能播的源"只剩极少数直链站。

### B6 配置层协议面不足

- UA 用了 Mozilla（`ConfigRepository.kt:69`）→ 老白镜像会 301 回首页（AGENTS.md 已记录"必须用 okhttp UA"，代码没照做）。
- 解码（`ConfigRepository.kt:79-118`）不支持"JPEG 尾挂 base64"（饭太硬系常见），不支持纯 base64（无压缩），也没有 `;md5;` 校验；`InflaterInputStream` 在非 deflate 数据上可能吞掉异常后返回乱码。
- 顶层键只认 `spider/sites/lives/parses/wallpaper/logo/flags/danmu`；FongMi 5.8 还认 `headers/proxy/rules/doh/hosts/ads/notice/assrt/urls(depot)`，现代配置越来越多带这些。
- 站点字段缺 `lang/hide/changeable/danmaku/style`；`ext` 只取 String（`SiteParcel.kt:19` 的 `as? String`），对象型 ext 直接丢；站点级 `jar`、`ext` 的相对路径没做解析。

### B7 稳定性与交互

- `JarLoader.call` 用**单线程** executor + `future.get(timeout)`，超时不取消任务 → 一个卡死站点会永久占住这条线程，之后所有站点的调用全部排队阻塞（`JarLoader.kt:90-94`）。
- 反射选方法只看"名字 + 参数个数"（`JarLoader.kt:96-107`），遇到重载靠 `methods` 返回顺序碰运气。
- 所有网络/爬虫回调都是 `thread {}` + `Handler`，无生命周期取消，Activity 销毁后仍 `post`。
- 无日志/调试面板（真机调试只能靠 logcat，电视上不可行）。
- `PosterCard` 固定 150×225dp（`PosterCard.kt:36-37`）塞进 `MATCH_PARENT` 网格项；网格固定 4 列（`VodActivity.kt:84`）→ 大屏海报比例被拉伸，列数不随分辨率变。
- 无 banner（`AndroidManifest.xml` 的 launcher activity 没有 `android:banner`），电视桌面上是矮图标。
- 播放层只有 ExoPlayer，无引擎回退、无续播、无倍速/选轨/字幕、无下一集自动播放；`PlayerView` 用默认控制器，遥控器焦点行为未适配。

## 3. 架构级缺口对照

| 子系统 | FongMi 5.8.2 | qbox | 不做会怎样 |
|---|---|---|---|
| 站点分派 | `BaseLoader` 四引擎 + `SiteApi`（xml api 站） | 单 jar 路径 | 一半配置源不可用 |
| jar 运行时 | Init/proxy 注册/siteKey/jsonExt/Mix/服务注入 | 仅反射 6 方法 | 大量源 proxy 地址 404 |
| 本地代理 | NanoHTTPD `/proxy?do=jar\|js\|py` | 无 | `proxy://`、图片代理全断 |
| 解析 | `ParseJob` 5 型 + `ParseWebView` 嗅探 | 无 | 绝大多数源无法播放 |
| 直播 | `LiveConfig` + `LiveParser`(m3u/txt) + `EpgParser`(xmltv) + timeshift | 无 | 缺 OK影视 一大主 tab |
| 播放 | `PlayerEngine` 抽象（EXO/MPV）+ 多轨/字幕/弹幕/续播/倍速 | Exo 单实例 | 部分片源花屏/无音轨 |
| 数据 | Room（历史/收藏/配置/站点设置） | 仅一个 `SharedPreferences` URL | 无续播、无收藏、无多配置 |
| 规则 | `rules` 广告过滤 / `hosts` / `DoH` / `headers` / `proxy` | 无 | 广告、部分域名不可达 |
| UI | leanback 焦点体系、站点墙、搜索页、设置页、播放 OSD | 三屏手写布局 | "不像 OK影视" |

## 4. 三条路线

**A. 移植式自研（推荐）**：保留 Kotlin 壳与 UI 骨架，按 FongMi 的**行为契约**重写兼容层（约 8–12k 行 Kotlin）。只参考协议与行为，不复制 GPL Java 代码 → 继续自主可控，可闭源分发。

**B. 直接 fork FongMi/TV**：最快 1:1（1–2 周出可用包），但 GPL-3.0 传染（分发 APK 即须开源衍生代码），且 `app/libs/lib-*.aar`（media fork：ffmpeg/libass/dav1d/mpv）不在 git，NDK 自编是全项目最重一步；UI 改造要在 Java + leanback 里做。

**C. 双线**：OKTV 改包版（已有 `repack` tag）当"能用"基线，自研壳按 A 慢慢追。

## 5. 路线 A 的阶段与验收

| 阶段 | 内容 | 验收（真机） |
|---|---|---|
| M2 保命（1–2 天） | B1/B2/B3(Init+siteKey)/B6(UA、jpeg 尾 base64、纯 base64)、反射按类型选方法、超时可取消（线程池 ≥4）、错误可见 | 老白默认配置：进站点 → 分类出列表 → 详情 → 直链可播 |
| M3 兼容层 | jar 契约补齐（proxy 注册、jsonExt/Mix、服务注入）；本地代理（NanoHTTPD，127.0.0.1:9978）；xml api 站点；过滤器 UI；多站搜索聚合；`flags`/`ext` 透传 | fan.jar 51 站点 ≥90% 能出列表/详情；带代理的图片/地址能出图、能播 |
| M4 解析层 | Parse 0/1/4（WebView 嗅探 + JSON API + 超级解析），再 2/3（纯反射）；`json:`/`parse:` 前缀；解析器选择与手动切换 | parse 类源可播；失败可切换解析器重试 |
| M5 体验层 | QuickJS（js 源）、Room 历史/收藏/续播、直播 m3u/txt + EPG、播放 OSD（选集/倍速/选轨/字幕/下一集）、设置页（多配置、引擎、UA、DoH、规则、清缓存）、调试日志页 | 与 OK影视 同配置源并排对比，功能对齐 |

体积预期：M2–M4 约 3–4 MB；加 QuickJS 约 +5 MB（universal APK，四 ABI）；再加 MPV/IJK 会到 20 MB+，建议按需。

## 6. 风险

- **协议漂移**：FongMi 5.8 引入 `headers/proxy/rules/doh/hosts`、外挂 runtime 模块、`NodeRuntime`；照 5.1.x 实现的壳会逐步错位，需以"行为对齐 + 兼容旧"的方式写。
- **GPL**：路线 B 的代码不能进闭源分发；路线 A 只借鉴行为，安全。
- **WebView 嗅探**：低端电视 WebView 版本老，嗅探脚本要兜底（`ParseJob` 里也做了 `WebViewUtil.support()` 判断）。
- **环境**：本机（Windows）无 JDK、无 Android SDK、无 `~/.gradle`，`local.properties` 缺失 → **当前无法编译验证**。要落地必须先装 JDK 17 + cmdline-tools + platform 36 + build-tools（约 1.5 GB），或回到原 Linux 构建机。AGENTS.md 里 `/opt/data/...` 的路径在本机不存在。

## 7. 建议的第一步

先做 M2（修 B1/B2 + Init/siteKey + UA/解码 + 线程池），因为它把"能不能用"变成"能用"，后续每个阶段才有真机反馈回路。同时并装构建工具链，否则改完无法验证。
