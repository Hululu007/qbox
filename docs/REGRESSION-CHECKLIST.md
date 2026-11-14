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

## C. 选集胶囊（GS-70/72/76/76b）— 固定大小 + 文字居中

> 2026-11-14 更新：用户拍板「按钮统一大小、文字居中、不按文字长短变尺寸、超长仍滚动」。
> 结构：root=match_parent×(固定 @dimen/qbox_episode_height=40dp)，text=match_parent×match_parent + gravity=center，
> 焦点态由 root→text 一层 `duplicateParentState` 直达（GS-16 合法）。padding 唯一来源 `applyPillPadding()`。
> 背景用全站通用 `selector_item`（含 state_activated），不再有选集专用 selector/shape。

| # | 检查项 | 验证方法 | 证据 | 状态 |
|---|---|---|---|---|
| C1 | **所有按钮等宽等高、不随文字长短变** | dump 各格 bounds 一致 | `fs1.xml`：1–14 全 `[...763][...843]`=238×80；`mq6.xml` 长标签源同样全 238×80 | [x] 2026-11-14 |
| C2 | **文字居中** | 聚焦/普通截图 | `fsS-crop.png`（1–14 数字居中）、`mqf-crop.png`（10.22 沉浸版 等居中） | [x] 2026-11-14 |
| C3 | 未聚焦项无大色块（透明底） | 同屏截图 | `fsS-crop.png` / `b3b-crop.png` | [x] 2026-11-14 |
| C4 | **焦点移动即时可见反馈**（不等选择） | 连按 → 截图白药丸跟随 | `fsS-crop.png`→`fs2-crop.png`（8→11）；`capC→capD` 旧版 | [x] 2026-11-14 |
| C5 | **聚焦文字反色可读**（白底深字） | 聚焦截图 | `fsS-crop.png` / `mqf-crop.png` 深色字清晰 | [x] 2026-11-14 |
| C6 | **当前播放集 activated 常亮灰底** | 焦点移走后原集灰底 | `fs2-crop.png`（第 1 格）、`b3-crop.png`（10.30 加更版）、`b3b-crop.png`（10.30 ·2 沉浸版） | [x] 2026-11-14 |
| C7 | **超长仍 marquee 滚动/省略** | 长标签聚焦 vs 未聚焦 | `mm1/mm2-crop.png`（聚焦显示全名 `10.30 ·3 沉浸版`）、`mqf-crop.png`（未聚焦 `沉浸...`）；4 个 marquee 属性+`setSelected` 保留 | [x] 2026-11-14 |
| C8 | padding 单一来源（无三处叠加） | 代码审查 | `adapter_episode.xml`（XML 不写 padding）+ `applyPillPadding()` 一次 setPadding | [x] 2026-11-14 |
| C9 | 焦点链 root→text 一层 duplicateParentState | 代码审查 | `adapter_episode.xml`（text 直接挂 root） | [x] 2026-11-14 |
| C10 | 角标嵌按钮右上、不撑大按钮 | 有 4K/DV 角标源聚焦截图 | 位置固定 `top\|end`；待测（当前源无角标） | [ ] |
| C11 | 复用通用 `selector_item`、无选集专用 drawable | 代码审查 | `selector_item.xml`（focused/selected/activated/default）、`selector_episode_item.xml` 已删 | [x] 2026-11-14 |
| C12 | 其它胶囊族（线路/画质/分组/排序）未被改坏 | 详情页上半部截图 | `b3b-crop.png`（分组条 1-10 聚焦白药丸+反色）、`capE-crop.png` | [x] 2026-11-14 |

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
| **选集布局 / selector / padding / dimen（任一）** | **C1–C12 全部 + D5 + E1–E3 + A1/A2/A3** | 本轮：修焦点把胶囊变大坨；回退 toast 又弄丢焦点 |
| 任何 `duplicateParentState` / 焦点 holder 结构 | C4/C5/C9 + 焦点态截图（聚焦+反色+移动三张） | 09d8740 焦点态断链 = 无反馈 |
| 通用 `selector_item` / `shape_item_*`（43 处共用） | **C12 + D4/D6/D8 全站胶囊抽查**：选集 activated 灰底 + 分组/线路 selected 聚焦态都要看 | 本轮：给 selector_item 加 state_activated，必须确认不串扰其它 43 处 |
| 颜色 token / 离散色收口 | 涉及页面各截一张聚焦态图，比对前后色值完全一致 | GS-63 |
| 删 drawable / 死代码 | A1/A2（build 兜住 0 悬挂引用）+ 相关页面焦点抽查 | 批次2：`CustomVerticalGridView` 删死块后验首页焦点 |
| `dispatchKeyEvent` / 焦点路由 | D1–D9 全链 | GS-75 |
| Toast / `targetSdk` / App 类加载 | B1–B3 + 播放一条线路确认没被预检拦 | GS-74 |

> 铁律：**每次只改一个关注点、单独提交、改完立刻按上表重跑对应项并打勾；绝不把多个不相关改动混在一次提交**（本轮教训：`git reset` 把焦点修复和胶囊尺寸一起回退，导致「无焦点」）。本轮 4 个提交：①胶囊固定大小 ②死代码清理 ③selector/颜色统一 ④文档，各自单独 build+test+真机验证。
