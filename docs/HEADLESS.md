# 无头测试 / CI / 截图回归

目的：让另一个开发者或自动化代理在没有真电视的情况下快速确认安装、启动、焦点与基础页面，并把稳定画面沉淀为基线图。

**它不能替代真机验收。** 第三方加固包、真实解码、遥控器长按、720p 显示、HDMI/EDID、弱网等，仍需电视/盒子原样验证。

## 工作流总览

| 工作流 | 触发 | 用途 |
| --- | --- | --- |
| `qbox-ci` / `build-debug` | push（`main`）、PR、手动 | 18 个 media AAR、Robolectric 单测、x86_64 + ARM debug APK，包名/ABI 校验（配了 Secrets 再校验证书指纹） |
| `qbox-ci` / `smoke` | 依赖上一步（PR 跳过） | 干净 runner 上起 720p TV 模拟器，装机、启动、截图、断言 mock 内容已渲染 |
| `qbox-ci` / `build-release` | 手动勾 `build_release`、推 `v*` tag | 正式签名 release APK（arm64 + v7a）+ 源码 zip |
| `tv-nightly` / `build` | 每晚 18:00 UTC、手动 | 完整打包 + 单测 + APK 校验，产出 nightly APK |
| `tv-nightly` / `smoke` | 依赖上一步 | 干净 runner 上起 720p TV 模拟器，装机、启动、截图 |
| `cache-keepalive` | 每 5 天、手动 | 只 touch Gradle 缓存，防 7 天过期 |
| `prune-artifacts` | 每天 03:00 UTC、手动 | 删除 3 天前的 artifact，兜住 GitHub Free 的 500MB 制品额度 |

共享逻辑放在 `.github/actions/`，避免各 workflow 之间配置漂移（历史上 nightly 就因为 keystore 变量名写错而连续失败）：

- `android-env`：校验 Android SDK / 修 `gradle.properties` / 解出签名 keystore / **用 keytool 校验密码与指纹**（密码错会立刻报 `Keystore password/alias mismatch`，不再是打包阶段的 `KeytoolException`）/ 准备 Robolectric 144MB jar（带缓存 + curl 硬超时）。
- `media-aars`：从 FongMi/media `release-1.11.0-fongmi` 编 18 个 `lib-*-release.aar`（clone 和 gradle 各重试 3 次），并释放 media 源码目录腾磁盘。
- `verify-apk`：`apksigner verify --print-certs`、`aapt2 dump badging`、包名必须 `com.qbox.app`、ABI 载荷检查、输出 `SHA256SUMS`。**校验失败即构建失败**，不会出现“构建绿了但包是错的”。

## 打包闸门（fail = 不可交付）

- 证书指纹：
  - debug `c4844d81…96e0d4d`（CI 固定 debug key，可覆盖升级）
  - release `84071cd2…c0caa098`（**线上用户已安装包的证书，必须一致**）
- 包名 `com.qbox.app`，release 两 ABI 都产出
- ARM 包必须包含对应 `lib/<abi>/` 载荷（v7a 用 `armeabi-v7a`，arm64 用 `arm64-v8a`）
  - 注意：media fork 的 `lib-mpvplayer` 只有 arm `.so`，`x86_64` flavor 没有 native 载荷，故 ABI 检查对它跳过
- 每个 APK 产 `sha256`，随制品上传

## 签名（debug 与 release 分开）

- debug / release 的固定签名放 GitHub Secrets，不进仓库：
  - debug：`QBOX_DEBUG_KEYSTORE_B64`、`QBOX_DEBUG_KEYSTORE_PASSWORD`、`QBOX_DEBUG_KEY_ALIAS`（`qbox-ci`）
  - release：`QBOX_RELEASE_KEYSTORE_B64`（`qbox-release-v2.jks` base64）、`QBOX_RELEASE_STORE_PASSWORD`、`QBOX_RELEASE_KEY_ALIAS`（`qbox`）
- **debug 缺 Secrets 时**（公开仓默认 / 首次 fork）自动生成本次运行专用临时 keystore：仍校验包名与 ABI，跳过固定指纹；配齐 Secrets 后指纹校验照常。
- **release 不会回落到临时 key**：缺 Secret 直接失败，避免出现「每轮签名都变、装不上」的包。
- debug 与 release 共用 `com.qbox.app`，同一台设备只能装一个；要共存需给 debug 加 `applicationIdSuffix`（当前未做）。

## 制品

GitHub Free 的额度：**artifact 500MB**（与 Packages 共享）、**cache 10GB/仓库**（独立，不算进 500MB）。
所以上传策略按「这个 run 立刻要用 / 用户主动要」来分：

- `qbox-tv-debug-apk`：`leanback-armeabi_v7a.apk` + `leanback-arm64_v8a.apk` + `SHA256SUMS`，给电视/盒子。
  **仅 `workflow_dispatch` 与 `v*` tag 上传**（90MB/次）；push 只编译 + 校验、不留档。保留 2 天。
- `qbox-ci-headless`：x86_64 debug APK（模拟器用），只有 smoke 会下载；PR 不上传。保留 1 天。
- `qbox-ci-screenshots`：CI 模拟器截图 + UI dump + logcat，保留 2 天。
- `qbox-unit-test-reports`：Robolectric HTML 报告（失败也上传），保留 2 天。
- `qbox-release-apk`：正式签名 APK ×2 + `QBox-<版本>-source.zip` + `SHA256SUMS`，保留 3 天。
- `qbox-nightly-apk` / `qbox-nightly-screenshots`：夜间包与模拟器截图，保留 2 天。

历史上 push 每轮都传 90MB 的 ARM 包 + 14 天保留，一天 8 轮就把 500MB 额度撑到 1.4GB
（报错后 Actions 拒绝上传）。改策略时不要恢复成「每轮全传」。

## 真机流程

agent 从 Actions 页下载 `qbox-tv-debug-apk`，`adb -s <测试盒子>:5555 install -r` 后做真机验收（第三方 spider、硬解、遥控器长按等只能到此完成）。签名固定，正常可直接覆盖升级。

## 相关文件

- `smoke/mock-server.ps1`：离线 mock 配置 / 列表 / 海报，不含任何真实站点、账号或直播源。
- `smoke/headless-screenshot.ps1`：构建（可跳过）、等待模拟器、清理数据、启动首页、截图与导出 UI dump（Windows / Linux 通用）。
- `smoke/baselines/`：已人工确认的基线截图；空目录表示尚未建立基线。
- `.github/workflows/*.yml` + `.github/actions/*`：流水线编排与共享步骤。

## 本地运行

需要一台有足够内存、支持 x86_64 加速的机器（Linux + KVM 最稳；Windows 需 WHPX/Hyper-V 且空闲内存充足）。AVD 建议使用 `tv_720p`，镜像可选 `android-tv`（如 SDK 提供）或同 ABI 的 AOSP 镜像；UI 行为以 TV 镜像为准。

PowerShell：

```powershell
# 先启动假内容服务（另一个窗口）
.\smoke\mock-server.ps1

# 再构建 x86_64 debug 包并截图（有 AVD 名称时传 -Avd）
.\smoke\headless-screenshot.ps1 -StartEmulator -Avd qbox-tv720 -ClearData -UpdateBaseline
```

直接以 mock 启动 Debug 配置：

```powershell
.\gradlew :app:assembleLeanbackX86_64Debug -PqboxTestConfig=http://10.0.2.2:8765/config.json
```

`-PqboxTestConfig=<url>` 只改 debug 构建配置；默认不传时仍使用内置测试源，release 不受影响。

> Linux 上的 `HttpListener` 会按 `Host` 头匹配注册前缀。模拟器访问的是 `http://10.0.2.2:8765/`，
> 所以 CI 必须用 `-HostAddress '*'` 启动 mock；只绑 `127.0.0.1` 会被 HttpListener 直接回 404，
> 应用侧表现为配置 JSON 解析失败（`MalformedJsonException ... column 10`）。

## 基线管理

- 只有人工确认「这一屏就是预期」后才 `-UpdateBaseline`。
- 基线必须与同一 AVD、同一分辨率/密度、同一 mock、冻结动画一起更新；不要把失败或半成品截图设为基线。
- 更新基线的提交说明里写明：改了什么、为何布局/焦点变化、在哪台 TV profile 验证。
- 下载 CI 制品看 `qbox-home.png` 与 `qbox-home.xml`；两张图不一致时以逐像素/肉眼可见差异为准，不只看绿色勾。

## 冻结清单

- 模拟器：720p、固定语言/时区、关闭系统动画。
- 数据：`pm clear com.qbox.app`，不要沿用本地第三方源历史/收藏。
- 配置：只用 `smoke/mock-server.ps1`；不要在 CI 里打线上配置。
- 内容：mock 里的影片均为合成条目与占位图。

## 当前限制

- x86_64 只能做安装/启动/焦点/截图；第三方 native 加固 spider 不能在模拟器上验收。
- 无网络、无真实直播、无真实播放器解码；播放级验收仍走电视。
- GitHub 托管 runner 无 KVM 时模拟器走软件模拟，启动慢（约 4～6 分钟），故模拟器必须**单独成 job**：
  与构建放同一个 job 时，runner 的磁盘/CPU 已被 media 源码 + Gradle 缓存占满，模拟器 30 分钟都起不来
  （曾出现 `Timeout waiting for emulator to boot`）。现在 `qbox-ci/smoke` 与 `tv-nightly/smoke` 都是干净 runner。
- 无头模拟器的三个坑（都已在脚本里处理，改动时别回退）：
  - `aosp_atd` 镜像没有正常合成管线，`screencap` 全黑（实测 0/921600 非黑像素），必须用 `target: default`；
  - `.NET HttpListener` 按 `Host` 头匹配前缀，mock 必须用 `-HostAddress '*'`，否则配置请求拿到 404；
  - 首次启动的类校验会把主线程拖到 ANR，装完先 `cmd package compile -m speed -f <pkg>` 做 AOT。
- 正式发布仍需人工决定版本号与公开仓 Release 内容；CI 只负责产出已校验的 APK + 源码 zip。
