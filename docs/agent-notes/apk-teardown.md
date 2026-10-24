# Android APK 拆解手法

适用：分析竞品/兼容目标 APK 的包名版本、原生库构成、页面结构、jar 依赖面。工具 = Android SDK build-tools 的 aapt/dexdump + Python zipfile 标准库。本机 build-tools：`/opt/data/cache/android/build-tools/35.0.0/`。

## 步骤

1. 下载 APK 到 scratch，记录体积。
2. `aapt dump badging <apk>`：package / versionCode / versionName / targetSdk / uses-permission / native-code / application-label。一次拿到兼容基线。
3. Python `zipfile`：`lib/<abi>/*.so` 按目录分组列大小 → 原生运行时构成（播放器/爬虫引擎/下载器）；`*.dex` 大小 → 业务代码体量；`assets/` 顶层与 js/py 库文件 → 内置运行库。
4. `aapt list <apk>` 看 res 分布：**layout 数量为 0 = UI 纯代码构建**（res 极小，只剩 color 与少量图形）。不要用 `aapt dump resources` 正则找 layout——可能匹配不到，用 list 直接看文件名。
5. `aapt dump xmltree <apk> AndroidManifest.xml`：提取 Activity/Service/Provider/Receiver → 信息架构（页面构成）。坑：R8 混淆后 dex 类名全变，但 **manifest 注册的组件名不混淆**（keep 规则），是还原页面结构唯一可靠来源。
6. `dexdump <dex>` + 正则 `Class descriptor\s+: 'L([\w/$]+);'` 提取类列表 → 包分布、入口类。
7. jar 依赖面（DexClassLoader 兼容设计的关键输入）：对 dex 二进制扫类型描述符 `re.findall(rb'L[a-zA-Z_$][\w/$]*;', data)`，排除 jar 自身包前缀，按外部包分组 → 宿主必须提供的依赖库清单。
8. 竞品 jar 常伪装：`.txt` 可能是 zip（看 PK 头）。jar 自带 Spider 基类时宿主无需提供接口，只需依赖库 + 反射调生命周期。

## 已实测结论（可直接引用）

- OKTV 5.1.6：全内置架构；UI 纯代码（0 layout XML，res 2MB）；ijkplayer(12MB)+media3-ffmpeg(5.8MB) 双播放器；Python3.8/Chaquopy(13MB)+QuickJS+迅雷SDK+p2p~p9p 全内置 → 82.8MB 单 abi。页面构成见 tvbox-shell SKILL.md 与设计文档。
- 饭太硬 fan.jar：642 类全在 `com.github.catvod`，自包含 `crawler.Spider`/`SpiderDebug` 基类；外部依赖仅 okhttp3 / gson / sardine-android / cronet-okhttp-transport。

## 改包重签（1:1 复刻竞品壳）

适用：把生态 APK（OKTV 系）换包名/应用名/签名变成自己的壳，功能原样保留。工具 = apktool jar（`java -jar apktool.jar`，放 scratch）+ build-tools 的 zipalign/apksigner。

1. `apktool d -f -o <dir> <apk>` 反编译；minSdk/版本信息在 apktool.yml。
2. 只改四类字符串，其余一律不动：
   - AndroidManifest `package="old.*"` 属性
   - provider `authorities="old.*"`（全部）
   - 自定义 permission 名（`old.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 这类，避免与原包并存冲突）
   - `res/values*/strings.xml` 的 app_name（多语言各一份）
3. 严禁改动（改了必崩或必不匹配）：manifest 组件类名（dex 类名未变）、`<action>` 字符串（与 smali 生成的字符串必须一致——smali 里对应字符串也不动最稳，如 P2P stop action）、meta-data 的 Initializer 类名、layout 里自定义 view 类名。smali 类路径与 applicationId 无关，不用动。
4. `apktool b <dir> -o out.apk` → `zipalign -f -p 4` → `apksigner sign --ks <jks>`（v1+v2）。验证：`apksigner verify --verbose`（v1/v2 均 true）+ `aapt dump badging`（新包名/新 label）。
5. 原包带 armv7-only .so → 改包版同样装不上 x86_64 模拟器，验收走 ARM 真机；与原包并存的前提是包名+签名都不同。
