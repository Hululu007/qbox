# QBox 交互回归清单（每次改动后逐项打勾，禁止空勾）

> 目的：杜绝「修一个功能、产生一个新 bug」。**任何涉及 UI / 焦点 / 选集 / Toast / drawable / 布局的改动，提交前必须把本清单相关项全部重跑一遍并勾选，未验或失败的不许提交。**
> 证据 = 192.168.2.132:5555 真机 uiautomator 焦点 bounds / 截图 / logcat / 单测输出，路径记在「证据」列。
> 勾选规范：`[x]` 已实测通过（附证据+日期）｜`[ ]` 未验｜`[!]` 已知失败待修。改了 A 必须按文末「改动→必验」映射表连带验证 B。

维护：每次修复新增一条规则到 `GOLDEN-STANDARDS.md`（GS-xx）并在此登记；过程细节进 `uiux-review-log.md`。

---

## A. 构建 / 安装 / 环境门禁（每次必过）

| # | 检查项 | 验证方法 | 证据 | 状态 |
|---|---|---|---|---|
| A1 | 单元测试全绿 | `.toolchain\test.ps1` → `QBOX_TEST_OK sites=49` | `test-final.log` | [x] 2026-11-14 |
| A2 | v7a debug 构建成功 | `.toolchain\build.ps1 -Abi v7a -Variant Debug` → `BUILD SUCCESSFUL` | `build-final.log`（49s） | [x] 2026-11-14 |
| A3 | 装到指定设备（只用 192.168.2.132，不用电视） | `adb -s 192.168.2.132:5555 install -r -d ...apk` → Success | 安装日志 | [x] 2026-11-14 |
| A4 | 版本/签名正确 | `dumpsys package com.qbox.app` → versionCode/targetSdk | versionCode=81 / targetSdk=37 | [x] 2026-11-14 |
| A5 | logcat 无新增 FATAL / ANR | `logcat -d \| grep FATAL\|ANR` | — | [x] 2026-11-14 |

## B. Toast（GS-74：不干预，已全部回退）

| # | 检查项 | 验证方法 | 证据 | 状态 |
|---|---|---|---|---|
| B1 | 无 Toast 干预代码 | `git log` 确认回退 `09d8740`；全仓无 `ToastShadow`/`Runtime` 桥/`targetSdk 30`/网盘预检 | 回退 commit + 代码检索 | [x] 2026-11-14 |
| B2 | 设备无干预残留 | logcat `QBoxToastShadow` 计数 = 0 | logcat 输出 `0` | [x] 2026-11-14 |
| B3 | 系统默认行为 | spider 提示按系统默认渲染（接受带图标） | 用户拍板，不再干预 | [x] 2026-11-14 |

## C. 选集胶囊（GS-70/72/76）— 本轮重点

| # | 检查项 | 验证方法 | 证据 | 状态 |
|---|---|---|---|---|
| C1 | **聚焦胶囊是紧贴文字的圆角白药丸，不是整列大坨** | 选集行聚焦截图：白药丸宽度 = 文字+内边距，不横贯整列 | `capC-full.png`（`10.23 ·2 沉浸版` 白药丸约 240px） | [x] 2026-11-14 |
| C2 | **未聚焦项无大色块** | 同屏其余集为纯文字/透明底 | `capC-full.png` / `capB-crop.png` | [x] 2026-11-14 |
| C3 | **焦点移动即时可见反馈**（不等选择） | 连按 → 截图：白药丸跟随移动 | `capC-full.png`→`capD-full.png`（白药丸从 `10.23 ·2` 移到 `10.29 ·2 上`） | [x] 2026-11-14 |
| C4 | **聚焦文字反色可读**（白底深字） | 聚焦截图文字对比 | `capC-full.png` 深色字清晰 | [x] 2026-11-14 |
| C5 | **当前播放集 activated 常亮灰底** | 焦点移走后原集呈灰底胶囊 | `capD-full.png`（`10.23 ·2 沉浸版` 灰底） | [x] 2026-11-14 |
| C6 | padding 单一来源（无三处叠加） | 代码审查：仅 `applyPillPadding()` 一次 `setPadding`；XML 不写 padding；shape padding 被覆盖 | `adapter_episode.xml` / `EpisodeAdapter.java` | [x] 2026-11-14 |
| C7 | 胶囊背景在 wrap_content 的 text + box/root 链传焦点态 | 代码审查 `duplicateParentState` 链 root→box→text | `adapter_episode.xml` | [x] 2026-11-14 |
| C8 | 角标嵌胶囊右上、不撑大胶囊 | 有 4K/DV 角标的源聚焦截图 | 待测（当前源无角标） | [ ] |
| C9 | 长名原名 + marquee 滚动 | 长名源聚焦，观察滚动 | 待测 | [ ] |
| C10 | 其它胶囊族（线路/画质/分组/排序）未被改坏 | 详情页上半部截图 | `capE-crop.png`（通用胶囊均为紧凑胶囊） | [x] 2026-11-14 |

## D. 焦点导航（灾难性 review 项）

| # | 检查项 | 验证方法 | 证据 | 状态 |
|---|---|---|---|---|
| D1 | 聚合搜索页：海报首行 ↑ → 源 tab 行 | uiautomator 焦点 bounds | `[826,156][1094,240]`（GS-75） | [x] 2026-11-14 |
| D2 | 工具栏/返回 ↓ → tab 行 | 同上 | tab 行 bounds | [x] 2026-11-14 |
| D3 | 海报第 2 行起 ↑/↓ 逐行保留 | QBoxGrid sel 0→1→2→3 日志 | logcat | [x] 2026-11-14 |
| D4 | 详情页 video↓flag↓group↓episode↓part 及反向 ↑ | 逐键焦点 bounds | 3×UP 从选集回到 video `[56,56]` | [x] 2026-11-14 |
| D5 | 选集 ←/→ 逐格、↓ 跨行同列 | 逐键 bounds | `capC`→`capD` 右移两格 | [x] 2026-11-14 |
| D6 | 多线路横向 ←/→、画质行 ←/→ | 逐键 bounds | B站/音轨、480P/360P | [x] 2026-11-14 |
| D7 | 全屏控制栏 15 按钮 ←/→ 可达 | 控制栏截图逐格 | 下集→…→详情 | [x] 2026-11-14 |
| D8 | 首页 header/content 行、搜索页、设置页 | 逐键 bounds | 各页焦点落点记录 | [x] 2026-11-14 |
| D9 | 首页返回退出、长按返回、熄屏 30 分钟退出 | dumpsys + 广播入口 | GS-71 | [x] 2026-11-14 |

## E. 选集数据（EpisodeInfo 单测 + 真机）

| # | 检查项 | 验证方法 | 证据 | 状态 |
|---|---|---|---|---|
| E1 | 排序 1→100、时间>集数>源顺序、混合命名 | `EpisodeInfoTest` | `QBOX_TEST_OK sites=49` | [x] 2026-11-14 |
| E2 | 时间/期/回/裸数字命名解析 | 同上 | 46+ 断言 | [x] 2026-11-14 |
| E3 | 稀疏宽格（≤4 条 2 列等） | 同上 | 断言 | [x] 2026-11-14 |

---

## 「改动 → 必验」映射表（防止修 A 坏 B）

| 改了什么 | 必验清单项 | 教训来源 |
|---|---|---|
| **选集布局 / selector / padding（任一）** | **C1–C10 全部 + D5 + E1–E3 + A1/A2/A3** | 本轮：修焦点把胶囊变大坨；回退 toast 又弄丢焦点 |
| 任何 `duplicateParentState` / 焦点 holder 结构 | C3/C4/C7 + 焦点态截图（聚焦+反色+移动三张） | 09d8740 焦点态断链 = 无反馈 |
| 通用 `shape_item_*` / `selector_item`（43 处共用） | **D4/D6/D8 全站胶囊抽查**，不可只看选集 | 全局审查：一处改动波及 43 个胶囊 |
| `dispatchKeyEvent` / 焦点路由 | D1–D9 全链 | GS-75 |
| Toast / `targetSdk` / App 类加载 | B1–B3 + 播放一条线路确认没被预检拦 | GS-74 |
| drawable / colors token | 涉及页面各截一张聚焦态图，比对前后 | GS-63 |

> 铁律：**每次只改一个关注点，改完立刻按上表重跑对应项并打勾；绝不把多个不相关改动混在一次提交**（本轮教训：`git reset` 把焦点修复和胶囊尺寸一起回退，导致「无焦点」）。
