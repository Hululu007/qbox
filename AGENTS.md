# tvbox-shell


## Agent 知识（原 skill 迁移）

> 原 Hermes skill `tvbox-shell` 的全部内容，已并入本仓库；agent 端不再单独维护。

## 2026-10 现状（以此为准，覆盖下文旧描述）

- 产品线已切换为 **FongMi/TV 5.6.1 定制壳**（不再以纯 Kotlin 自研 M1 为主线）：
  - 源码仓 <https://github.com/zqs1qiwan/qbox-tv>（私有，`main`）；本地 worktree `fongmi-561/`（gitignore）。
  - `applicationId com.qbox.app`、`versionName 1.2.0`、`minSdk 24`、`targetSdk 37`、label `QBox`；图标、leanback banner、首页 `ic_logo` 均已换成自有设计；1.1.x 起焦点环 3dp、卡片 12dp 圆角、主色 QBox 蓝、窗口底色 #0B0B0F；1.2.0 调行间距/网格间距/海报标题。
  - Release `v1.2.0` 已上传：`QBox-1.2.0-arm64-v8a.apk`（44.6MB）、`QBox-1.2.0-armeabi-v7a.apk`（38.6MB），v2 签名；v1.0.x/1.1.x 保留。
- 本机（Windows）工具链在 `.toolchain/`：JDK 21 Temurin、SDK 36/37、build-tools 36/37、NDK 29.0.14206865、CMake 3.22.1、Gradle 9.7.1（TV）/9.1.0（media）。
  构建必须把 `JAVA_HOME/ANDROID_HOME/ANDROID_USER_HOME/GRADLE_USER_HOME/TMP/TEMP` 指到工作区内，否则 Gradle 与 adb 会写 `C:\Users\Q` 被沙箱拒绝。
- 媒体层不在 Maven 上：`media/`（FongMi/media 分支 `release-1.11.0-fongmi`）按仓库 `move.txt` 自编 18 个 `lib-*-release.aar`，放进 `fongmi-561/app/libs/`。**公开 TV 源码与公开 media 分支不同步**：5.7.8+ / 5.8 引用的 `androidx.media3.exoplayer.libass`、`SubtitleTranscript*`、`DecoderManager`、`DiscMenu*` 等类公开 media 仓没有，所以基准选 5.6.1（缺的只有 8 个音效/画质类，已在 `app/src/main/java/androidx/media3/**` 补桩）。
- Python 爬虫已裁掉：移除 `:chaquo` 依赖与 settings 入口，`com.fongmi.chaquo.Loader` 为占位；py 站点回退 `SpiderNull`。
- 默认配置：`VodConfig.init()` 首次启动写入 `https://tvbox.laobaitv.net`（→302→ `www.xn--sss604efuw.cc/tv`，JPEG+`**` base64 JSON；解码后 49 站点/6 路直播；spider 是伪装 jpg 的 zip，MD5 `b55978c4...`，dex 内 76 个 `com.github.catvod.spider.*Guard`）。
- 已知限制：FFMPEG 解码模式不可用（未编 `lib-decoder-ffmpeg`）；UI 结构仍是 FongMi leanback（已做品牌层 + 设计 token 精修，首页 hero/tab 未改）；**未完成真机播放验证**。模拟器已确认不可用：WHPX 与纯 TCG 都试过，qemu 启动即退出——沙箱禁止命名管道而 qemu 依赖它；`dex2jar` 想把爬虫 dex 转 JVM 类也被 `Files.isWritable` 恒 false 卡住。真机验收只能靠用户电视。
- **老白 jar 是加固包（重要）**：dex 里 76 个类全是 `*Guard` 薄壳，继承 `BaseSpiderGuard`，真实实现由 `DexNative` + `assets/ftyguard_v7|v8.so` 在运行时解出；`init()` 里 `delegate` 为空即 NPE。用 `enjarify`（Python 版，走 zipfile 不受 zipfs 限制，76/76 类转换成功）把 dex 转成 jar 后仍无法运行，证明该源只能真机验证。`enjarify` 用法：`PYTHONPATH=.toolchain/enjarify python -m enjarify.main <dex> -o out.jar -f`。
- 兜底配置：`assets/qbox_default.json`（12KB，49 站点）在默认源拉取失败时由 `VodConfig.load` 回退加载；对应单测 `bundledFallbackConfigParses` 输出 `QBOX_FALLBACK_OK sites=49`。
- 直播链路已用 `LiveParseTest`（`LiveParser.start`）验证：5 个源全部解析成功（2675 频道），首个频道线路 `cctv1.m3u8` 返回 `206 application/vnd.apple.mpegurl`。单测命令 `:app:testLeanbackArm64_v8aDebugUnitTest --tests "com.qbox.*"` 同时跑配置与直播两组。
- 本机没有 `/opt/data`、`/home/hermes`；下文这些旧路径与 M1 描述仅作历史参考。

### 本机构建坑（都实际踩过）

- `gradle/gradle-daemon-jvm.properties` 把 daemon 固定为 JBR：JBR 在本环境把项目目录判为不可写（`File.canWrite=false`），Gradle 9.7 直接拒绝配置项目 → 删除该文件，用 JAVA_HOME 的 Temurin。
- `5.8` 的 `tvbus-release.aar` 引用 `com.github.catvod.Runtime`，5.6.1 没有该类，R8 报 Missing class → 用 5.6.1 自带的 `app/libs/tvbus-release.aar`。
- `services.gradle.org` 的 wrapper 下载可能卡 0 字节：手动下载 distribution 放进 `wrapper/dists/<ver>/<hash>/` 并写 `.ok`。
- `adb.exe` 固定使用 `C:\Users\Q\.android`（忽略 ANDROID_USER_HOME/HOME），已在该路径建 junction 指向 `<repo>\.toolchain\android-home`；否则 adb 直接崩。
- Android 模拟器虽能通过 `-accel-check`（WHPX 可用），但在本沙箱内 qemu 启动即退出，无法用于验收；真机验收需用户电视。
- Robolectric 单测：默认会往 `~/.m2` 下 android-all jar，沙箱拒绝 → 已设 `robolectric.dependency.dir=<repo>/.toolchain/robolectric`，并预先下载 `android-all-instrumented-14-robolectric-10818077-i7.jar`（144MB，Maven Central）。测试命令：`:app:testLeanbackArm64_v8aDebugUnitTest --tests "com.qbox.ConfigPipelineTest"`；已通过，输出 `QBOX_TEST_OK sites=49 ...`。


# TVBox 自研壳（qbox）

## When to Use

用户提到 qbox / TVBox 壳 / 自研电视盒子客户端、FongMi/OK影视 生态、饭太硬 jar 兼容、TVBox 配置协议、或该项目的 UI/交互开发时使用。白果TV（红果短剧客户端）的工作不要用本技能，用 baiguotv。

## 项目事实

- 代码 `/home/hermes/projects/qbox`，项目名 **qbox**（用户定名，早期工作名 tvshell）。**新项目必须放 /home/hermes/projects/**——用户明确要求项目进 projects 目录；实测该目录可写，"挂载只读放不了"的判断是错的，先实测再下结论。
- git origin = https://github.com/zqs1qiwan/qbox.git（私有，已连接）；凭据走 git credential store（zqs1qiwan PAT），直接 git push。
- 两条交付线并存：**自研壳** applicationId `com.qbox.shell`（本仓库，纯 Kotlin 无 native，单一 universal APK）；**OKTV 改包版** applicationId `com.qbox.tv`（OKTV 5.1.6 换壳，83MB，发布在该仓库 GitHub Release tag `repack`）。包名不同可并存安装，对比测试互不干扰。
- 自研壳 minSdk 21 / targetSdk 28；v1+v2 双签名（`enableV1Signing = true` + `enableV2Signing = true`）。
- 签名密钥 `/opt/data/keys/qbox-release.jks`（密码 `/opt/data/keys/qbox-release.pass`），项目内 `app/qbox-release.jks` 为同一份副本；证书 SHA-256 `f7c4b32c67c17389945e45aa68ec44b009f224238023d0313f55548b2f63c62a`。改包版与自研壳统一此证书，以后升级不换密钥。
- 设计文档 `/opt/data/tvshell/DESIGN.md`（OKTV 逆向数据、M1–M3 路线、风险清单）；与本文件冲突时以本文件为准。

## 当前进度

- M1 已完成、可编译（release ~1.9MB）：配置加载（json/gzip/deflate/base64/zip 自动识别、`./` 相对路径解析、`;md5;` 校验）、饭太硬 jar 加载（DexClassLoader + 反射生命周期）、Home（站点网格 + 菜单键换配置地址并持久化）→ Vod（左分类栏 + 右海报网格、滚动到底自动翻页）→ Video（详情 + 内嵌 ExoPlayer，OkHttpDataSource 传请求头）。
- OKTV 式空壳交付：首次启动无配置 → 导入界面（地址输入框，预填默认地址 `https://tvbox.laobaitv.net` 老白TVBOX，已验证可用：spider 是伪装 jpg 的 jar 走 github.laobaitv.net 代理带 md5 校验）；保存后每次启动自动加载，菜单键随时更换。默认地址用 laobai 的（用户指定），不是 0826 镜像。
- M2 待做：QuickJS js spider、多站点搜索聚合、直播（m3u/txt）、历史/收藏。
- 注意：TVBox play_url 的 `$$$` 分隔符在 Kotlin 字符串字面量中必须写 `\$\$\$`（`$` 是模板前缀）。
- OKTV 改包版作用 = UI/交互验收基准（1:1 复刻竞品壳换包名/名字/签名），自研壳照此对齐；改包流程见 `references/apk-teardown.md`「改包重签」。

## 配置源与线上端点

- 默认配置地址 `https://tvbox.laobaitv.net` **不在任何代码里**：它是 laobaitv.net zone 的 Cloudflare Single Redirect（301 → `github.laobaitv.net/https://raw.githubusercontent.com/.../tvbox/laobaitvbox.json`）。改向不要在仓库里找，定位与修改流程见 laobai-infra skill 的 `references/cf-redirect-rules.md`。
- 第三方源（饭太硬系）常见**图片伪装配置**：HTTP 200 + `content-type: image/x-ms-bmp`，body 实际是 JPEG（`FF D8`）后面追加 base64，解码后才是 JSON（`spider`/`sites`）。验证时必须：
  1. 用非浏览器 UA 取（`-A "okhttp/3.15"`）——浏览器 UA 会被 301 回 `/`，光看 `curl -I` 或直接 `json.loads` 只会得到"不是配置"的假阴性；
  2. 按 `\xff\xd9`（JPEG EOI）切开取尾部，剔除 `**` 等非 base64 前缀与空白，补 padding 后 `base64.b64decode`。
- 这条也适用于自研壳的格式探测：M1 的自动识别（json/gzip/deflate/base64/zip）被子类"jpeg+尾部 base64"绕过，遇到这类源需要在壳里加同样的尾部剥壳逻辑，否则导入即解析失败。

## 构建

```
export JAVA_HOME=/opt/data/cache/jdk/jdk-17.0.20.1+1
cd /opt/data/qbox && ./gradlew :app:assembleRelease
```

Gradle 8.14.3 / AGP 8.11.0 / Kotlin 2.1.20（wrapper 与 baiguotv 同版本，复用已下载发行版）。local.properties → /opt/data/cache/android。无模拟器无 KVM，验证 = 单元测试 + 用户真机验收（与 baiguotv 流程一致）。patch/write_file 只能写 /opt/data 内——改 /home/hermes 项目代码一律走 execute_code。

## 构建坑（每个都实际踩过，各花掉一轮构建）

- sardine-android 传递引入 xpp3，与 Android 内置 XmlPullParser 冲突 → R8 直接失败。必须加 `configurations.configureEach { exclude(group = "xpp3", module = "xpp3") }`（FongMi 同款做法）。
- targetSdk 28 触发 ExpiredTargetSdkVersion 致命 lint。Kotlin DSL 写 `disable += "ExpiredTargetSdkVersion"`（disable 是 MutableSet），Groovy 风格 `disable "..."` 编译不过。
- ExoPlayer 实例没有 setMediaSourceFactory（那是 Builder 的方法）。换请求头用 `OkHttpDataSource.Factory.setDefaultRequestProperties()`——内部 DefaultRequestProperties 可变，对之后创建的 DataSource 生效，无需重建播放器。
- Activity 间传递的数据类必须实现 Serializable，否则 putExtra 编译不过。
- `context.getDir()` 自己保证目录存在，不要再包 `File(...).apply { mkdirs() }`——File(File) 构造在 K2 推断下报错。
- AGP 默认 minSdk ≥ 24 只生成 v2 签名 → Android 6 及以下电视装不上。老电视兼容 = minSdk 21 + `enableV1Signing = true` + `enableV2Signing = true`；排查"装不上"先 `apksigner verify --verbose` 看签名方案，再查 minSdk。
- 生态 APK（OKTV 系）只带 armeabi-v7a .so → x86_64 模拟器报 INSTALL_FAILED_NO_MATCHING_ABIS，不是"只有电视能装"；自研壳无 .so，模拟器可直接装验收。

## 用户拍板的方向（勿重新论证）

- 全新自研架构，不 fork FongMi/TV；投入重点是 UI/交互设计质量 + 代码完全自主可控。
- 操作结构对齐 OKTV：Home 顶部 [首页][直播][设置] 三 tab；Vod 左筛选栏+右海报网格；Video 详情与内嵌播放器一体（无独立播放页）；Live 全屏+左滑频道列表+数字选台。
- 视觉设计语言仿照 Apple TV+ / Netflix（用户点名，覆盖早期"扁平工业暗色"草案；M1 仍是草案风格，验收返工时重构）。
- jar 兼容聚焦饭太硬 jar，不承诺其他 jar；py 爬虫/迅雷/p2p/DLNA/手机版 UI 明确不做。
- 方向修正：先 1:1 复刻 OKTV 壳（二进制换包名/名字/签名，功能原样）作为可用基线，用户在其基础上提修改；自研壳并行推进保证自主可控。两线并存，勿只做其一、勿重新论证。
- 分发规则：APK 用 GitHub Release API 分发（创建 release + 上传 asset，83MB 实测成功）或 Telegram 直发（机器人上限 50MB，超限发送即失败，大包必须走 Release）；严禁起局域网 HTTP 服务器分发文件——用户明确拒绝并要求关闭清理。

## 饭太硬 jar 兼容面（fan.jar 实测，642 类，可直接依赖此结论）

- jar 自包含：`com.github.catvod.crawler.Spider`/`SpiderDebug` 基类在 jar 内 → 宿主无需提供接口类，DexClassLoader(parent=app classloader) 加载 + 反射调生命周期方法即可
- 类映射：配置 `api: csp_XXX` → jar 内 `com.github.catvod.spider.XXX`
- 宿主需内置依赖：okhttp3、com.google.gson、sardine-android(WebDAV)；cronet-okhttp 桥不提供=个别源失败，可接受；org.json/android.webkit 系统自带
- 加载器 ≈200 行 + 3 个 maven 依赖即可跑通

## 资产与参考

- 拆解产物在 `/opt/data/cache/scratch/tvbox-analysis/`（scratch 24h 清理；OK-TV.apk 可从 pan.laobaitv.net/TVBOX/OK-TV.apk 重新下载，饭太硬 jar/配置从 GitHub 镜像 gaotianliuyun/gao 取）。OKTV 改包产物（反编译目录 oktv-decoded、qbox-rebuilt/qbox-aligned）同在此处；改包发布版在该仓库 GitHub Release tag `repack`。
- FongMi 生态结构/构建链/外挂模块系统/配置生态：`references/fongmi-ecosystem.md`
- APK 拆解手法与具体命令：`references/apk-teardown.md`
