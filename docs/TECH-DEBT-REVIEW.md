# QBox 全项目技术债审查报告（2026-11-14）

> 触发：用户指出「改功能导致修一个坏一个、缝缝补补让可复用/全局调教被掩盖、变成冗余补丁状态；某些胶囊全 app 通用，变丑可能不止一处」。
> 方法：三个并行全仓只读审查（drawable/color 资源盘点、布局胶囊一致性、Java 冗余与死代码）+ 人工复核关键行号。
> 原则：**「修一个坏一个」的解药不是一次大改 40+ 处视觉，而是把「确定零风险」与「需拍板」严格分开、分批动、每批按 `REGRESSION-CHECKLIST.md` 打勾。**
> 路径基准：`fongmi-561/`。**本轮只落地第 1、2 节；第 3 节起全部列出供 review，未改一行。**

---

## 1. 本轮确定已修（零运行时行为变化 / 直接修复用户报的 bug）

| # | 问题 | 修复 | 验证 |
|---|---|---|---|
| F1 | **选集「一大坨而不是胶囊」**：`e014e96` 把药丸背景挂在 `match_parent` 根 → 整列宽大白块；且 shape(8/12) + XML(12/8) + `applyPillPadding()` 三处 padding 叠加 | `adapter_episode.xml` 收敛回「root 只管焦点命中/列宽、`wrap_content` 的 text 承载 selector、box 包角标」；给 box 补 `duplicateParentState` 接通 root→box→text 焦点链（09d8740 缺这行导致无焦点反馈）；padding 收敛到 `applyPillPadding()` 单一来源 | `capC-full.png`/`capD-full.png`：紧贴文字白药丸 + 反色 + 移动跟随 + activated 灰底 |
| F2 | 注释与代码矛盾（`applyPillPadding` 注释称"只做右侧增量"却重设四边；`onBind` 注释称"背景在根上"） | 改为如实描述；恢复「按角标实测宽度留白」的单一来源实现 | 代码复核 |
| F3 | 误建的孤立 `shape_episode_focused.xml`（又一个专用 drawable，正是冗余来源） | 删除 | 工作区确认未跟踪、已删 |
| F4 | 7 个 0 引用 import（HomeActivity×3、VideoActivity×2、VodActivity×2） | 删除（逐个先验证 non-import uses=0，再删；`Notify.java` 的 `ResUtil` 有 4 处使用，保留） | `BUILD SUCCESSFUL` |
| F5 | 新增可打勾回归清单 + 「改动→必验」映射表 | `docs/REGRESSION-CHECKLIST.md` | 本轮已按其打勾 |

**本轮（2026-11-14 续，用户「固定大小 + 按处理顺序修复」）新增：**

| # | 问题 | 修复 | 验证 | 提交 |
|---|---|---|---|---|
| F6 | 选集按钮跟着文字长短变大小（本末倒置） | 固定 `match_parent`×40dp + `gravity=center` + 去掉冗余 `box` 层改一层 `duplicateParentState` + marquee 保留 | `fsS/fs2/mqf-crop.png` | `e111a69` |
| F7 | 死代码 + 11 个孤儿 drawable（冗余补丁） | 带命中次数断言脚本删 0 引用方法/`fadeHeight` 死块/同包冗余 import；删前全仓 grep 13 项确认 0 引用 | build 3m13s + `QBOX_TEST_OK` + 首页真机 | `6793c27` |
| F8 | 胶囊 selector 孪生副本 + 硬编码颜色 | 删 `selector_episode_item`、`selector_item` 加 `state_activated`、11 文件 14 处精确等值颜色 token（跳过 `<gradient>`） | build+test+真机三态 | `9a9b5ca` |
| F9 | 文档与代码同步（GS-76b/77、清单 C 组、映射表、台账、本报告） | 全部更新 | 文档复核 | 根仓库 |

> 关键结论（反直觉）：**全站通用胶囊 `selector_item` + `shape_item_*` 没坏**。43 处复用（线路/画质/分组/设置行）都是健康的 `wrap_content` TextView 胶囊，真机 `capE-crop.png` 证实未被改坏。变丑的只有选集——它被三个历史补丁单独弄坏了。所以正确动作是「让选集收敛回通用体系」，**不是**改那 43 处。

---

## 2. 已确认的「胶囊/资源冗余」全貌（供你判断是否统一，本轮不动）

全局审查发现：详情页 6 个列表 + 排序/筛选/解析/设置行/tab，本应同一种「通用胶囊」，实际横跨 **9 种 selector、6 种圆角(10/16/999/8/28/6dp)、5 套 padding、3 种焦点态(state_selected / state_activated / 无)、5 种 textColor**，外加 MAIN 目录另一套 Material Chip。最典型的冗余：

- **孪生副本**：`selector_episode_item.xml` 与 `selector_item.xml` 仅差 `state_selected`↔`state_activated`，却共用同一套 `shape_item_focused/activated/normal`。`selector_item` 被 43 处引用，`selector_episode_item` 仅 1 处。
- **四套平行 3-state 体系**：`selector_chip` / `selector_chip_round` / `selector_item` / `selector_item_round` 结构完全相同，各配一整套 `*_normal/focused/activated`；另有 8 个 selector 把 `<shape>` 内联重定义同样的「焦点白 + 淡白 + 透明」三态。
- **硬编码色**：`#1FFFFFFF`(7处)、`#26FFFFFF`(7处) 等 60+ 行，而 `colors.xml` 已备好 `white_05…90`/`black_05…90`/`qbox_panel`；`shape_sheet.xml:10` 硬写 `#B8000000` 与 `qbox_panel` 全等、`styles.xml:38` 硬写 `#C9C9D1` 与 `qbox_text_dim` 全等。
- **孤儿资源**：约 10 个 0 引用 drawable（`shape_item_selected` / `shape_keyboard_*` / `shape_poster_scrim` / `shape_settings_panel` / `shape_clock_bg` / `shape_hero_tab_focused` / `selector_subtitle` 及其死链 / `selector_vod_name`）+ 3 个 0 引用 token（`qbox_bg_soft`/`qbox_surface_hi`/`qbox_header_scrim`，后者注释还指向不存在的 `shape_top_scrim`）。

**为什么本轮不统一**：一次改 43 处视觉 / 60+ 行颜色，正是下一轮「修一个坏一片」的来源；且需你先拍板「全站胶囊到底统一到哪一种圆角/焦点态」。建议按第 5 节分批。

---

## 3. 需你 review — 行为/视觉类（有回归风险，建议单独一轮）

| # | 位置 | 问题 | 建议 | 风险 |
|---|---|---|---|---|
| R1 | `layout/adapter_episode_group.xml:14` | 分组项文字色用 `@color/control_toggle_text`（循环按钮专用，选中变蓝），兄弟项都是 `@color/text` | 统一回 `@color/text` | 低，但改视觉需截图 |
| R2 | `drawable/selector_setting_chip.xml` | 只有 `state_focused`，缺 `state_selected`；排序标志 `setSelected` 对背景无反馈 | 补 `state_selected` 或改状态通道 | 中，涉焦点态 |
| R3 | `values/styles.xml:172-175`(Control 4/8/4/8) + `shape_chip_*:6`(8/14) | 控制条按钮双重 padding 叠加 | 只留一处 | 中，控制条布局 |
| R4 | `drawable/selector_button.xml` | normal 28dp 圆角 ↔ focused 999dp，聚焦瞬间圆角跳变 | 统一圆角 | 低 |
| R5 | `layout/adapter_chapter/edition/subtitle/track/danmaku/doh/site.xml` 等 | 背景挂 `match_parent` 根（列表行整行宽），与详情页胶囊族两种风格并存 | 需拍板是否统一为 wrap_content 胶囊 | 中，列表行布局 |
| R6 | `main/res/layout/dialog_*_setting.xml` 的 `MaterialChip` / `MaterialButtonToggleGroup` | 与 leanback 全手写 selector 完全独立的一套，不共享任何 token | 需拍板是否收敛 | 高，跨 flavour |
| R7 | `CustomVerticalGridView.java` nav-scrim 逻辑在 `HomeActivity`/`VodActivity` 各写一遍（B2） | 同职责两处实现、几何值一处常量一处字面量 | 抽公共基类/工具 | 中 |
| R8 | `LiveActivity:161` + `VideoActivity:411` 进度条 4 色硬编码两份；`CustomMic.java:149` 用 `MDColor.RED_500` 纯红 | 与 GS-01「色彩≤3」/GS-63 冲突 | 收口到 token | 低-中 |

## 4. 需你 review — 潜在 bug / 死代码（按 A=确定可删 / B=需确认 分级）

**A：确定零行为变化，可安全删（本轮未做，避免混入胶囊修复）**
- `ScreenGuard.java:40 isExitRequested()` / `:51 clear()` — 0 引用
- `CustomVerticalGridView.java:160 setClipTop()`、`:43 fadeHeight` 整块（`fadeHeight` 恒为 0，`:337-345` 永不执行）及随之死的 5 个 import（`LinearGradient`/`Paint`/`PorterDuff`/`PorterDuffXfermode`/`Shader`）
- `EpisodeAdapter.java:74 getPageStart()` / `:90 getNumRows()` — 0 引用
- 8 个未使用 import（HomeActivity×3 / VideoActivity×2 / VodActivity×2 已在本轮删；`Notify.java` 同包冗余 import 可删）
- DEBUG 专用纯日志：`QBoxEpisodes`/`QBoxEpisodesRaw`/`QBoxSnap`/`QBoxGrid`/`QBoxFlags`/`QBoxFocus`（`logFocus()` 及 3 调用点）。**保留** `QBoxSeek`(GS-42)/`QBoxNav`(GS-52) —— 它们是文档化验收手段。

**B：需确认（可能改行为/需拍板）**
- `EpisodeAdapter.getNext()/getPrev()` 空列表 `IndexOutOfBounds`（B9）
- `EpisodeGroupAdapter:122` 排序标志 ViewHolder 复用残留 `selected` 态（B10）
- `CollectActivity.getFragment()` 固定第 0 页仍被 `focusGrid()` 使用（B12）—— **与 GS-75 明令禁止的写法冲突，但本轮焦点已实测正常，需你决定是否清理**
- `setNextFocusXxx` 5 处一律 `notifyDataSetChanged()` 全量重建（B5），与 GS-72②「全量刷新丢焦点」相关
- `debug_nav_variant` 读取无 `BuildConfig.DEBUG` 守卫，装过 debug 覆盖到 release 会泄漏「Apple TV+ 栏」变体（B2 末条）
- 播放链路 `VodPlayback*` 一批 NPE 守卫（B18–B25）、`isRevPlay()` leanback 不可达残留（B26，GS-69 已定案应清）
- DEBUG 变体：`EdgeTiers.java` 整类、详情页 A/B/C/D 四变体/`BACKDROP_SCRIM_VARIANTS`（GS-55「拍板后清落选入口」—— 需你确认默认变体是否已定案）
- `CustomSearchView.isFocused()` 恒返回 true（B17）、`CustomKeyboard` 光标 -1（B15）等框架级问题
- `HomeActivity.showDiag()` 裸 Material 弹窗 + 全硬编码中文字串（B41）

## 5. 处理顺序与进度（每批单独提交 + 按清单打勾）

| 批次 | 内容 | 状态 | 提交 / 验证 |
|---|---|---|---|
| 1 | 胶囊结构收敛 + 死 import + 回归清单机制 | ✅ | `824258a`/`ad9831b`（上一轮） |
| 1b | **选集按钮固定大小 + 文字居中**（用户拍板） | ✅ | `e111a69`；build+test+真机三源 |
| 2 | 零风险清理：A 组死代码 + 11 个孤儿 drawable | ✅ | `6793c27`；build 3m13s + `QBOX_TEST_OK` + 首页真机 |
| 3 | **全站胶囊 selector 统一**（删孪生 `selector_episode_item`，`selector_item` 加 `state_activated`）+ **精确等值颜色 token 收口**（11 文件 14 处，跳过 `<gradient>` 色标） | ✅ | `9a9b5ca`；build+test+真机三态 |
| 4 | DEBUG 纯日志（`QBoxEpisodes`/`QBoxSnap`/`QBoxFlags`/`QBoxFocus`）| ⏸ **刻意未做** | release 被 R8 移除、零运行时收益，却要动焦点核心 `VideoActivity` 多处；`QBoxSeek`/`QBoxNav`/`QBoxGrid` 是验收手段本就不删。见下方取舍 |
| 5 | 潜在 bug（B9–B28）、DEBUG 变体（GS-55）、双轨逻辑、圆角强统一、跨 flavour/Material Chip、`<gradient>` 色标 | ⏸ **待拍板** | 涉及行为/需先定标准，逐项单独提交 |
| 6 | **选集焦点闪烁根治**（GS-79）：`EpisodeAdapter` 稳定 id 取代 GS-78③ 焦点守卫补丁，清除全部守卫代码 | ✅ | 待提交；build+test+真机（跨组翻页/点集/主动 UP/聚合搜索全回归） |

**仍需你 review 才能动的项**（第 4 节 R/B 组，理由 = 会改行为/视觉或需先定标准，擅自混入功能提交就是「修一个坏一个」）：
- 圆角强统一（R1–R6）、`selector_setting_chip` 缺 `state_selected`（R2）、Control 双重 padding（R3）、`selector_button` 圆角跳变（R4）、`match_parent` 列表行胶囊（R5）、MAIN 目录 Material Chip 另一套（R6）
- DEBUG 变体清理（GS-55 需确认默认变体是否定案）、`debug_nav_variant` release 泄漏（B2）、`CollectActivity.getFragment()` 固定第 0 页（B12，与 GS-75 冲突但焦点实测正常）
- 播放链路 NPE 守卫（B18–B25）、`isRevPlay` 残留（B26）、`setNextFocusXxx` 全量重建（B5）、`CustomSearchView.isFocused` 恒 true（B17）
- 硬编码颜色里**无精确等价 token** 的一批（`#1FFFFFFF`/`#14FFFFFF`/`#7A000000` 等）与全部 `<gradient>` 色标（需新增语义 token，属决策）
- 进度条 4 色硬编码两份（R8）、`CustomMic` 纯红（B32）

> 取舍说明（回应「杜绝修一个坏一个」）：本轮只自动落地了**确定零视觉/行为变化**的批次（2、3）——selector 状态合并、精确等值 token、删死代码/孤儿资源，每批单独 build+test+真机。凡需先定「统一到哪种标准」或改运行时行为的，一律列出等你拍板，不擅自大改 40+ 处共享视觉。

> 供你勾选：告诉我第 4–5 批里哪些先做、哪些暂缓。
