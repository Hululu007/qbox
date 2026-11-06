# QBox UI/UX Review Log（统一记录文档）

> **规则以 [GOLDEN-STANDARDS.md](GOLDEN-STANDARDS.md) 为准**；本文件只记过程与台账。

> 本文件是 QBox TV 端 UI/UX 整改的**统一记录**，结构固定为四段：问题清单 → 迭代记录 → 历史遗留与易回归 → 回归检查清单。
> 详细的分析台账见 [ui-ux-audit.md](ui-ux-audit.md)（R/N/H 编号寄存器），两者一致、本文件为终态索引。
> 每轮开始先读本文件与 ui-ux-audit.md，确认没有把已修问题改回去；每轮结束更新本文件并提交 git。

- 设备：`192.168.2.214:5555`（Hisense IP810N_48UB1 / Android 9 / 1920×1080 @240dpi density=2.0 / armeabi-v7a）
- 包名 / 版本：`com.qbox.app`，1.9.19 / versionCode 77，targetSdk 37
- 源码 worktree：`fongmi-561/`（git 分支 `ui-cinema`）；文档仓库：本仓库 `main`
- 设计规范：项目内技能 `.dsh/skills/tvos-design-guidelines/SKILL.md` + `.dsh/skills/android-design-guidelines/SKILL.md`（均已读取，见 §7）

---

## 1. 问题清单（终态索引）

> 严重级别：阻断 / 高 / 中 / 低。状态：open / fixed / accepted。
> 每条对应 ui-ux-audit.md 里的 R-xx / N-xx / H-xx 编号。下表为**当前 open 与最近修复**的索引；
> 已销账的完整历史见 ui-ux-audit.md 各轮章节。

### 1.1 当前 Open 项

| ID | 页面 | 级别 | 状态 | 说明 | 证据 / 关联 |
|---|---|---|---|---|---|
| P1-RES | 设置→恢复 | 高 | open | 恢复对话框弹出后焦点仍留在设置页，→/↓ 无反应；`onStart` + `post requestFocus` 无效（弹窗窗口未拿到焦点），需查 `BaseAlertDialog` 的 window focus | handoff §2 |
| N-12 | 全局·启动 | 高 | open（已缓解） | 冷启动 debug 包 ~11.5s 首帧 / ~24s 内容，仅剩启动骨架屏未做；直播/壁纸配置已移出主线程 | handoff §2 |
| R25-01 | 全局·顶栏 | 低 | open（待取舍） | 顶栏胶囊聚焦展开会把左侧搜索/直播图标推左 70–93px；方案待拍板（固定槽位/容器左对齐/保持现状） | handoff §2 |
| N-13 | 搜索·站点 tab | 低 | open（回滚待重做） | 站点 tab 行右缘渐隐：上一版 FrameLayout 包裹破坏 `setHeader` 导致搜索页空白，已 revert；需非侵入 overlay 方案 | handoff §2 |
| N-14 | 首页/分类·海报 | 低 | open（源侧，观察） | 个别源返回占位图（深色块），非加载器问题 | handoff §2 |
| N-47 | 详情页 | 高 | fixed（第 19 轮三档实测） | fontScale 1.3 下「主演」长文与播放/简介/收藏/换源按钮行重叠（TV-33 fail） | §18.3，fs13_detail.png |
| N-48 | 首页·更新推荐 | 低 | fixed（第 19 轮 dump 实测） | 卡片纯海报无文本无 contentDescription，TalkBack 静默（TV-80 fail） | §18.3 |
| N-49 | 播放页·媒体键 | 高 | fixed（第 19 轮状态机实测） | 遥控器 PLAY/PAUSE 键全无效：`KeyUtil.isPlayPause` 死代码无调用方（TV-12 fail） | §18.3 |

### 1.2 待补实机截图（功能已实现，验证挂账）

| 项 | 关联 commit |
|---|---|
| 倍速点按 toast「倍速 1.5x」 | `afc7d9a` |
| 循环 toast「循环：开/关」 | `afc7d9a` |
| 「更多」→「倍速设置」弹窗 | `1dd6d58` |
| 单音轨 / 视轨 toast（不再把唯一轨道置空） | `afc7d9a` |
| 单片时「章节」按钮隐藏 | `afc7d9a` |
| N-21 直播分组/频道加粗（↑/↓ 直接切台，几乎不可见） | `39e91a1` |

### 1.3 本轮新增（2026-11-04 全量功能检验 + UI/UX 重构）

> 本节在每轮真机验证后持续填充；新发现的问题先在此登记、再排期修复。

| ID | 页面 | 级别 | 状态 | 复现步骤 | 证据 | 修复说明 / commit | 回归风险 |
|---|---|---|---|---|---|---|---|
| N-24 | 播放页控制条 | 高 | fixed | 全屏播放按 ↓/↑ 或 MENU 呼出控制条后，焦点仍留在视频面（`video`），→/← 被当成快进（position 跳变 +12s），控制条 14 个按钮全无法点按 | 修复前 `shots/round-2604/ctrl-*.png`；修复后 `r11-ctrl-pause.png`（焦点在「暂停」白药丸）、`r11-ctrl-right2.png`（→ 移到「上集」）、QBoxCtrl 日志（`requestFocus=true nowFocus=playPause` → `focus=prev panelFocused=true`） | 根因：`showControl` 单次 `post` 时按钮未 layout（`isShown()==false`）requestFocus 失败；且 `mFocus2` 被污染成 `video`。改 A：`showControl` 改用 `OnGlobalLayoutListener` 等布局完成再聚焦（`requestControlFocus`）；改 B：`mFocus2` 仅 `panelFocused` 时赋值 | 控制条 5s 无操作自动收起后焦点回 video 属预期；按钮双击/长按、收起后 ←/→ 快进仍需遥控器复验 |
| N-25 | 详情页简介 | 中 | fixed | 简介文本以原始 `&nbsp; ` 开头显示（未解码 HTML 实体） | 修复前 `detail-loaded.png`（`&nbsp;《伦敦合伙人》…`）；修复后 `r11-london-detail2.png`（「合伙人们实现了从"开起来"…」干净显示）；单测 `UtilCleanTest` 5/5 通过 | 根因：`Util.clean()` 对不含 `<` 的文本直接 return。改：无 `<` 分支也替换 `&nbsp;/&#160;/\u00A0/\u3000`；单测覆盖实体变体、中英文标点/emoji/连字符不误伤、`<p>` 标签分支不破坏 | 简介截断/换行、HTML 富文本源显示需在发版前再抽 2-3 部片目检 |
| N-26 | 搜索页键盘 | 低 | open | 搜索框已有长文本时，⌫ 键**长按不能清空**（`--longpress 23` 实测只删 1 字），长词需逐字删除，10-foot 场景费劲 | `r11-cleared2.png`（长按后「小城良方LL」→「小城良方L」） | 待修：⌫ 长按应清空（或循环加速删除）；需看 `SearchActivity` 键盘 `onLongClick` 是否实现 | 低：仅影响长词删除效率 |
| N-27 | 播放页·字幕面板 | 低 | open | 无字幕轨道的流点「字幕」弹出空面板，无「暂无可用字幕」占位文案 | `r12-subtitle.png`（面板标题+图标，列表区空） | 待修：空列表加占位文案（同 H-23 三态收口一起做） | 低 |
| N-28 | 播放页·循环按钮 | 中 | fixed | 循环开/关只弹 toast，按钮本身无状态（`shape_chip_activated`=#1FFFFFFF 12% 白底≈不可见）；且 `setRepeatOne` 只写 ExoPlayer 运行时，重启/换片后丢失 | 修复前 `r12-loop.png`（循环开时按钮与普通按钮无区别）；修复后验收截图见第 11 轮记录 | ① 新增切换类按钮专用资源：`selector_chip_toggle`（开=accent_soft 底+1dp accent 描边+蓝字；开+焦点=白底+1.5dp accent 描边+蓝字；焦点=白药丸黑字；关=透明灰白字）；② `Setting.isRepeatOne/putRepeatOne` 持久化；③ `renderHistory` 恢复循环与按钮态；④ 倍速按钮非 1x 时直接显示当前值（`updateSpeedButton`，呼出/点按/恢复三处刷新）；⑤ mobile 端同步持久化逻辑（视觉规范台账挂账） | 循环按钮宽度不变（文字不切换）；倍速按钮文字随值变化（「倍速」↔「1.5x」）会微调行宽，需回归控制条排布 |
| N-29 | 播放页·章节面板 | 低 | blocked | 「选择章节」面板弹出后列表为空（伦敦合伙人有 38 期，理论上应有章节/选集数据） | `r12-chapter.png`/`r12-chapter2.png` | 待查：是这路流无 chapter 数据，还是面板加载失败；先看 `haveChapter()` 判定与 adapter 数据源 | 低 |
| N-30 | 直播播放 | 低 | open | 异常流（006 CCTV5 线路2，txiptv 酒店源）播放到 ENDED 后停在 00:00/00:00 暂停态：`playNextProgram` 因无 catchup 空转，不自动换线/换台；此时 `isLive()=false`，←/→ 落入 seek 分支失效，只能手动换台 | `r15-line.png`（00:00/00:00 暂停态）、`r15-line1.png`（连接超时） | 待修：ENDED 且无 catchup 可播时应回退 nextLine/nextChannel，或给出「重试/换台」引导而非死停 | 低 |
| N-31 | 直播·频道收藏 | 中 | fixed | TV 端长按 OK 被 `dispatchKeyEvent` 全局拦截开控制条（ACTION_DOWN repeatCount>0 直接消费），频道项 `OnLongClick`（加/删收藏）遥控器永远不可达，「收藏」分组永远为空 | Keep 表无任何 type=1 记录；代码路径 `LiveActivity.dispatchKeyEvent`（leanback）；焦点实测落在频道项（long-clickable=true）；**用户真机确认长按收藏可用（2026-11-05）** | 修复：`dispatchKeyEvent` 加守卫——频道列表（recycler）可见时长按 OK 交还列表项；列表收起才开控制条（commit `4d692ad`） | 列表收起时长按开控制条手势不变；列表内长按不再开控制条（改用 MENU 键） |
| N-32 | 直播·频道列表 | 高 | fixed（已真机复验，证据见第 15 轮） | 列表左上角一块黑色覆盖看不清首行内容 | `r15-live-list.png`；根因：`listTopScrim` 用圆心在左上角的径向渐变（半径 380dp≫视图高 120dp），叠在列表 94% 黑底上双重压黑成块 | `shape_live_top_glow` 改垂直线性渐变（顶 45% 黑向下淡出）（`8d153fe`） | 低：纯视觉 |
| N-33 | 直播·分组/频道行 | 中 | fixed（已真机复验，证据见第 15 轮） | 当前分组/频道无选中态标记（selector_live 只有焦点态）；且只有聚焦行有内边距，聚焦时文字右移、行高跳动 | 代码审计：`selector_live` 无 selected 分支；`shape_live_focused` 独占 padding | 新增 `shape_live_selected`（12% 白底，对齐分类 tab 规范）；padding 上移到三个列表项布局（`8d153fe`） | 低 |
| N-34 | 直播配置/站点对话框 | 中 | fixed（已真机复验，证据见第 15 轮） | 「综合直播」等配置对话框打开后无可见焦点，用户不知道当前选中项、↓ 不知从哪开始 | 代码审计：`LiveDialog`/`SiteDialog` 只 scrollToPosition 无 requestFocus（P1 同族） | 打开后焦点落到当前选中项（`8d153fe`） | 低 |
| N-35 | 全局·站点名 emoji | 低 | fixed（已真机复验，证据见第 15 轮） | BMP 符号区（♥⭐▶ 0x2190–0x2BFF）在精简字体盒子上会豆腐块但未被拦截 | 代码审计：`Util.cleanName` 只查 >0xFFFF | 渲染检查扩展到 BMP 符号区，CJK/常用标点不受影响（`8d153fe`） | 低 |
| N-36 | 播放中·轨道/倍速弹层 | 高 | fixed（已真机复验，证据见第 15 轮） | 音轨/画质/字幕/倍速设置走 Material BottomSheet 默认主题（紫灰叠色+蓝色控件），与全站面板对话框两套 UI | 代码审计：`TrackDialog` 继承 `BaseBottomSheetDialog`，主题未覆盖 | `Theme.Base` 增加 `bottomSheetDialogTheme=ThemeOverlay.QBox.Sheet`（qbox_surface + 关叠色）（`79753d3`） | 中：所有底部弹层外观变化，需回归 |
| N-37 | 全局·色彩 | 高 | fixed（已真机复验，证据见第 15 轮） | 超过 3 色的「奇怪蓝色」：背景左上蓝紫柔光 `#332B4A74`、进度条蓝 `0xFF4C8DFF`、主按钮焦点蓝描边、VOD 选中蓝描边、内容对话框链接黄、Material 控件 colorPrimary 蓝 | 全仓 `#hex`/`qbox_accent` 扫描清单 | 收敛为 ≤3 色：**中性（黑/白/灰阶）承担全部交互态；品牌蓝仅留 logo/设置分区竖条/链接语义**；`colorPrimary` 改白（驱动滑杆/勾选框/开关）（`79753d3`） | 中：所有 Material 控件变色，需回归 |
| N-38 | 播放页·循环按钮 | 中 | fixed（已真机复验，证据见第 15 轮） | 激活态蓝色亮底不协调 | 用户反馈 | 激活态改白色系：开=白描边+15% 白底+白字；开+焦点=实白+内深色细环（`79753d3`） | 低 |
| N-39 | 首页/分类/详情·氛围底图 | 中 | fixed（已真机复验，证据见第 15 轮） | 背景模糊海报从 40×40 中心方块 48× 放大，只剩局部色块、信息量低 | 代码审计：三处 `override(40,40).centerCrop()` | 改源海报中心 16:9 横带 `override(160,90)`，~12× 适度放大（`79753d3`） | 低 |
| N-40 | 详情页顶部 | 低 | fixed（已真机复验，证据见第 15 轮） | 亮海报顶边硬切，无渐隐入黑 | `shape_backdrop_scrim` 顶部仅 22% 压暗 | 顶部加深到 55% 平滑过渡（共享 scrim，首页/分类/详情同受益）（`79753d3`） | 低 |
| N-41 | 首页·最近观看 | 中 | fixed（已真机复验，证据见第 15 轮） | 焦点框下半圈被卡片的底部压暗遮罩吃掉（环是 image 的 foreground，遮罩兄弟节点画在它之上） | 代码审计：`adapter_history.xml` 层叠顺序 | 焦点环移到独立覆盖层（遮罩之上），白色实环全程完整（`79753d3`） | 低 |
| N-42 | 首页·回到最近观看 | 低 | blocked（需真机） | 从更新推荐 ↑ 回最近观看缺滚动效果 | 设备待机无法观察；代码侧 `getHistory` 首行插入未见显式滚动调用 | 待真机确认现象（Leanback 行内滚动 vs 页面跳变）再定方案 | 低 |
| N-43 | 播放页·快进手感 | 高 | fixed（已真机复验，证据见第 15 轮） | 一按方向键很快进入大步跳：阶梯按 repeat 计数分档，但 repeat 事件 ~50ms 一个、远快于 300ms 节流后的实际步进，按住约 0.2s 就进 20s 档 | 代码审计：`CustomKeyDownVod.stepFor(repeat)` / `VideoActivity.seekStep` | 改按**按住时长**分档：前 1.5s 每步 10s（秒级细调窗口）→3.5s 内 20s→6s 内 30s→9s 内 60s→13s 内 120s→17s 内 180s→300s；理论对比（同样按住 3s）：旧 ≈+8:20，新 ≈+2:10（`d5bbae2` + `79753d3` 同文件部分） | 中：快进体感变化，需真人手感复验 |
| N-44 | 网盘管理 | 中 | fixed（复验通过） | 「我的云盘┃配置」站点点击无反应 | 站点为加固 spider（`csp_MyDriveGuard`） | 真机复验：点击「云盘配置」tab 触发 spider 请求（logcat `TV-category: key=MDrive,tid=peizhi`）且 VodActivity 正常打开，网盘管理网格完整渲染（登入网盘/清除四端 Cookie/自动排序/线路排序/手动推送授权）——当前构建下点击有响应，原反馈场景未复现 | 低 |
| N-45 | 播放页·自动换源 | 高 | fixed（已真机复验，证据见第 15 轮） | 未登录网盘连续报错触发自动换源连跳，焦点一直被抢、手动抢不过 | 代码审计：`VodFallbackPolicy` 无上限连跳；`renderSources` 反复刷新 | 自动换源连续上限 3 次（手动换源不受限；手动选择本就 `setAutoFallback(false)` 已确认）；超限后界面稳定停在当前源（`f5cc81e`） | 中：正常换源链路需回归 |
| N-46 | 配置加载 | 中 | fixed（已真机复验，证据见第 15 轮） | 「获取失败 xiuxiu pro」：用户配置源失效后卡在报错页（默认配置无此站点，非默认源问题） | 线上配置探测：tvbox.laobaitv.net 无 xiuxiu；`VodConfig.load` 只对默认源兜底 | 首载失败（内存与缓存都无站点）时回退内置快照（仅 debug；release 无快照资产则维持原报错）（`f5cc81e`） | 低 |

### 切换类按钮的状态表达规则（N-28 落地，全站统一）

适用：循环、弹幕开关、倍速（取值显示）、缩放/解码/引擎（取值显示）、片头/片尾（取值显示）。

1. **开关型**（开/关）：按钮持久显示状态——**开 = 白描边 + 15% 白底衬 + 白字；开+焦点 = 实白药丸 + 内侧 1.5dp 深色细环 + 深色字**；关 = 透明底 + #C9C9D1 灰白字。焦点态仍是实心白药丸，三态（开·非焦点 / 纯焦点 / 开+焦点）两两可区分。不靠 toast 表达状态（toast 保留为操作反馈）。（2026-11-05 B7：激活态由品牌蓝收敛为白色系，蓝色只留品牌位/链接语义。）
2. **取值型**（倍速/缩放/解码/片头尾）：按钮文字直接显示当前值（如 `1.5x`），默认值时回到简洁文案（如「倍速」）。
3. **面板选择型**（音轨/字幕/视轨/章节/画质）：按钮本身不带状态，选中项在弹层面板内标记；空面板必须有占位文案（N-27 待修）。
4. 双通道原则：状态表达必须同时有形态差异（描边/底衬/文字内容）与颜色差异，禁止只靠颜色（色觉障碍 + 10 英尺距离）。
5. 持久化：开关型与取值型状态一律写 `Prefers`（Setting/SpeedSetting），重进播放页与重启应用后恢复。
6. mobile 端：持久化逻辑已同步；视觉规范（Control 样式 selected 态）待单独收口（手机形态不走焦点模型）。

---

## 2. 迭代记录

### 第 1 轮（2026-11-03）— 结构性 bug（焦点/抖动/裁切）修复

范围：R1-01 ~ R1-11（分组标题压字、列表半截海报死循环、换行回弹、焦点守护日志、详情页线路↓、搜索历史↑陷阱、配置弹窗无焦点、控制条压亮画面、卡片贴边、历史卡重叠、筛选与贴顶互斥）。

结果：见 ui-ux-audit.md §1、§5；构建 1.9.16 (74)，单测全绿。

### 第 2 轮（2026-11-03 晚）— 间距 / 基础视觉

范围：R2-01 ~ R2-03（内容区起点统一 32dp、历史卡与海报卡同一套网格、投屏页焦点可见）。

结果：见 ui-ux-audit.md §6。

### 第 3 轮 — 焦点顺序 / 文案本地化 / 加载态

范围：R3-01 ~ R3-03（详情页右跳简介、播放错误中文、加载态文案）。

### 第 4 轮 — 用户实测（播放键慢一拍 / 首页↑跳级 / 速度不变 / 设置太黑）

范围：R4-01 ~ R4-05。

### 第 5~7 轮 — 直播/搜索/收藏/设置补丁

范围：N-10 ~ N-23、R16-01、R18-01/02、R21-01 ~ R27、R30-32。

### 第 8 轮 — 1.9.19 播放控制条焦点真因修复（本轮前最后一轮）

范围：R33-01 ~ R35-01。核心是 `ea3947a`：`showControl()` 里同步 `requestFocus` 时按钮未布局完成→焦点留在画面→→/← 被当快进；改为布局完成后 `post` 再请求焦点。

结果（本轮会话提交）：`344797a` → `ea3947a`（共 22 个 app 提交领先 origin）。

### 第 9 轮（2026-11-04）— 全量功能检验 + 两项修复

范围：全量真机走查（首页/设置/恢复/详情/直播/收藏/推送/搜索/分类/播放全屏）+ 修复 N-24（控制条按钮无法聚焦）、N-25（简介 `&nbsp;` 实体）。

结果：
- 功能验证矩阵见 §8；核心结论：首页/顶栏/分类/搜索/直播/收藏/推送/设置/详情/播放全链路焦点无 crash、无 ANR。
- 新发现并修复 N-24（`VideoActivity`：`showControl` 改用 `OnGlobalLayoutListener` 等布局完成再聚焦；`mFocus2` 仅 `panelFocused` 时赋值）、N-25（`Util.clean` 无 `<` 分支补 `&nbsp;/&#160;/\u00A0/\u3000` 替换）。
- 另确认「恢复对话框无焦点」（handoff §2 P1）已不复现。
- **待补**：N-24/N-25 已构建（`BUILD SUCCESSFUL`，dex 已含新字符串常量）并 `install -r` 安装成功（lastUpdateTime 更新），但真机回归验证**受阻**——本轮误 `uninstall` 清了数据（已由 debug 自动配置源恢复），且首页数据源变化后难以稳定进入含简介且可播的样例；uiautomator 对 SurfaceView 焦点的 `focused` 属性不可靠，需下一轮改用**截图视觉 + 按键行为（position 是否跳变）**两路交叉验证才能定论。

### 第 10 轮（2026-11-04 深夜）— N-24/N-25 回归闭环

范围：按用户要求先闭环 N-24/N-25 验证，再谈后续。

过程与结论：
- N-24 初验失败（→ 后 position +18.5s）→ 按要求**落日志定位**（临时 `QBoxCtrl` tag），时序证据：`showControl → onGlobalLayout pick=playPause shown=true requestFocus=true nowFocus=playPause`，证明修复生效；之前验证失败是 adb 命令链间隔超过控制条 5s 自动收起窗口、焦点被系统收回 video 所致（属预期行为）。
- 改用单条 `adb shell "input keyevent 20; sleep 1; input keyevent 22; sleep 1; input keyevent 22"` 紧凑序列，截到焦点白药丸落在「下集」（`r12-ctrl-tight.png`），→ 为行内移动、无快进跳变。**N-24 = fixed（证据闭环）**，临时日志已移除（工作区回到 b9c8590 提交态）。
- N-25：新增 `UtilCleanTest`（Robolectric，5/5 通过：实体变体剥除、正常中英文标点/emoji/连字符不误伤、`<p>` 分支不破坏、空串边界）；真机「伦敦合伙人」详情页简介干净显示（`r11-london-detail2.png`）。**N-25 = fixed（单测+真机双证据）**。
- 新发现 N-26（低）：搜索键盘 ⌫ 长按无清空功能。
- 搜索页确认拼音首字母联想可用（「LD」→「伦敦合伙人」）。
- commit：`c5512cc`（UtilCleanTest）。N-24/N-25 代码修复在此前 `b9c8590`、`99349e5`。

遗留方法学教训：验证 5s 自动收起 UI 的操作序列必须放在**单条 adb shell** 内执行，消除命令往返开销；`dumpsys media_session` 的 position 快照有延迟，不能单独用作精确计时判据；uiautomator 对 SurfaceView/Leanback 列表的 `focused` 属性与 bounds 常返回旧值，**以截图视觉为准**。

### 第 11 轮（2026-11-04 深夜）— N-28 循环按钮状态可见 + 持久化

范围：用户实测反馈「循环按钮只有 toast、按钮本身无状态」，按 8 条要求处理并扩展为「切换类按钮状态表达规则」。

改动：
- 新增 `selector_chip_toggle` / `shape_chip_toggle_on` / `shape_chip_toggle_on_focused` / `control_toggle_text`（leanback），循环按钮换用；
- `Setting.isRepeatOne/putRepeatOne` 持久化；leanback + mobile 两端 `onRepeat` 写入、`renderHistory` 恢复；
- 倍速按钮非 1x 直接显示当前值（`updateSpeedButton`，呼出/点按/恢复三处刷新）。

真机验收（全过）：
- 关态：`r13-loop-off.png`（循环=透明灰白字，倍速=1.5x 已显示）；
- 焦点+开：`r13-loop-on-focused.png`（白底+蓝描边+蓝字，toast「循环：开」辅助仍在）；
- 非焦点开：`r13-loop-on-unfocused.png`、`r13-loop-persist2.png`（蓝底衬+蓝描边+蓝字，与灰白按钮一眼可辨）；
- 持久化：重进播放页恢复（`r13-loop-persist2.png`）；**force-stop 重启后仍恢复**（`r13-loop-restart.png`）。

顺带验证：网盘源（盘她|夸父）详情页 QR 授权弹窗存在（登录/授权场景确认）；「连接超时」「播放地址加载失败」错误态显示正常；收藏 toast 正常。

commit：`aa566e8`（fongmi-561）。N-27（字幕空面板占位）、N-29（章节面板空）仍 open/blocked。

### 第 12 轮（2026-11-05 凌晨）— 播放页二级功能全量验证

范围：播放控制条全部 14 按钮 + 更多二级行 + 倍速设置弹窗。

| 功能 | 结果 | 证据 |
|---|---|---|
| 暂停/播放 | ✅ | 各控制条截图 |
| 倍速点按 | ✅ toast + 按钮显示「1.5x」（N-28 取值型） | `r13-loop-off.png` |
| 倍速设置弹窗 | ✅ slider(1.5x)/长按倍速/快速选择 chips/跳过无声/重置 | `r13-speed-dialog2.png` |
| 音轨 | ✅ 弹「选择音轨」（立体声 AAC-LC） | `r12-audio.png` |
| 字幕 | ⚠️ 空面板无占位文案 → N-27 open | `r12-subtitle.png` |
| 视轨 | ✅ 弹「选择视轨」（1920×1080 H.264，单轨不置空） | `r13-videotrack.png` |
| 片头 | ✅ OK 设当前位置，按钮显示「09:12」 | `r13-opening.png` |
| 片尾 | ✅ 同机制（代码同路径，未单独点） | — |
| 循环 | ✅ N-28 全态+持久化 | 见第 11 轮 |
| 章节 | ❓ 面板空 → N-29 blocked（待查数据源） | `r12-chapter2.png` |
| 重播 | ✅ 回片头点（09:12）重播 | `r13-replay.png` |
| 详情 | ✅ 退全屏回详情视图（内嵌播放+字幕正常） | `r13-detail-btn2.png` |
| 更多 | ✅ 展开 extra（原始/硬解/EXO 显示当前值，「更多」↔「收起」） | `r13-more.png` |

顺带确认：续播位置恢复（position=42 分钟处）、倍速跨重启恢复（speed=1.5）、循环图标随状态切换（media_session mIcon 变化）、内嵌字幕渲染正常、网盘源 QR 授权弹窗存在。logcat 无新增 FATAL/ANR。

遗留：片头/片尾的 ↑/↓ 微调与长按重置未逐项（机制已由 OK 设值覆盖验证，微调走 CustomUpDownView 既有逻辑）；「上集/下集」单集 toast 为历史挂账项（`afc7d9a`）未复测。

### 第 13 轮（2026-11-05 凌晨）— 二级设置页全量验证 + 测试机配置恢复

范围：SettingActivity 全部二级页与对话框逐项进入（每页截图）。

结果：播放设置 9 项页（`r14-setting-player.png`）、解码设置 6 项页（隧道模式切换+切回，`r14-decode*.png`）、预载页（单开关，`r14-preload*.png`）、UA 对话框（QR+输入）、播放键切换（暂停/播放↔呼出菜单）、备份（toast 备份成功）、版本（toast 已是最新版本）、DoH 选择对话框（系统/腾讯/阿里/360）、无痕切换（已切回关）、点播配置对话框（QR+地址+选择/确定/取消，`r14-vod-dialog*.png`）、缓存清理（20MB→无，`r14-cache*.png`）。

配置恢复（不留验证痕迹）：图片尺寸循环 [小/中/大/特大] 验证后从「小」OK×2 经「中」恢复原值「大」（uiautomator sizeText=「大」确认）；隧道模式、无痕模式均已切回原值。

顺带确认：弹幕设置入口为产品决策隐藏（`SettingActivity.java:78` 注释「QBox: 去掉弹幕相关入口，保持设置精简」，非缺陷）；预载页单开关信息密度低（UX 观察，不单独立项）。全程无 crash/ANR。

### 第 14 轮（2026-11-05 凌晨）— 直播 EPG/换台/加密分组验证 + N-31 候选修复

验证项与结果：

| 项 | 结果 | 证据 |
|---|---|---|
| 频道列表呼出/分组切换 | ✅ OK 呼出；左列分组（收藏/央视/卫视/睛彩/咪视界/历年春晚/轮播）↓ 选中即刷新频道列（焦点驱动、**播放不中断**）；组内 OK 进频道列、↓ 选台、OK 换台 | `r15-group2.png`（卫视 pill + 030~038 频道列）、`r15-group-switch.png` |
| ↑/↓ 直换台 | ✅ CCTV2→CCTV4→006 CCTV5 等；信息条实时更新频道号/台标/名称/**EPG 节目名+时段**/线路 | `r15-live-list.png`、`r15-line1.png`（「006 CCTV5 01:06~02:36 国际足球赛场…利物浦-富勒姆(4K)」） |
| EPG 面板 | ✅ 源数据 36/141 频道有 EPG（logcat `EpgParser: bindResultsToLive with-epg=36 without-epg=105`，源 `gitee.com/taksssss/tv/.../112114.xml.gz`）；CCTV2/CCTV4 面板：当前节目置顶高亮、↓ 列表内焦点移动正常、节目与实播一致（中国经济大讲堂 / 爱在青山绿水间）；点击非当前节目在无 catchup 源下按设计无操作（`LivePlaybackController.selectEpg` return false） | `r15-epg-panel.png`、`r15-epg-nav2.png` |
| CCTV1 无 EPG | ✅ 数据事实非 bug：EPG XML 频道表从 CCTV10 起、无 CCTV1 条目（已拉源文件核对）；点击 CCTV1 正确落入换台分支 | 本地 `epg.xml` 核对记录 |
| 错误态 | ✅ 卡流 spinner「正在加载」；超时后 ⚠「连接超时」图标+文字；自动换线逻辑代码确认（`fallbackAfterError`：`LiveSetting.isChange` 开且非最后线路才自动换线；当前源最后线路超时后停留报错属预期） | `r15-line1.png`、`r15-cctv2.png` |
| 加密分组 | ⛔ blocked（源未配置）：`Live.pass=0`（数据库实测）；连点「收藏」分组 5 次无 PassDialog 属预期 | tv.db `Live` 行 |
| 手动切线（控制条「线路」按钮） | ⛔ blocked（源服务不稳定）：按钮正确显示当前值（线路 2）、点击触发 requestLive（media_session state 6→0 重载尝试），但 m3u 服务器 502、两线路均超时，端到端「切线后播放恢复」未证实 | media_session 快照、`r15-line1.png` |
| 数字选台 | ⛔ blocked（未实测）：代码路径存在（`CustomKeyDownLive` 数字键→onShow/onFind），本轮未执行 | — |
| 长按收藏（N-31） | ✅ fixed：拦截根因修复（`dispatchKeyEvent` 列表可见时放行长按），**用户真机确认长按 OK 收藏可用** | §1.3 N-31 行 |

新发现登记：**N-30**（低，open）异常流 ENDED 死停 00:00 暂停态；**N-31**（中）TV 端长按收藏不可达——候选修复当轮提交，**用户随后真机确认可用，已定案 fixed**。

新发现待查项（N-30 关联观察）：CCTV5 线路2（`139.227.221.147:9901/tsfile/...` txiptv 酒店源）流卡在 00:00 且 `isLive()=false` 时，→ 键落入 `onKeyRight` 的 seek 分支（`LiveActivity.java:1061-1063`）而非 `nextLine`——切线路在异常流态下静默失效；另 `LivePlaybackController.switchLine`（`LivePlaybackController.java:149-155`）无 buffering/错误态守卫，异常中切线表现依赖上层。复现路径：直播 → ↓ 换至 006 CCTV5（若默认线路2）→ 等流 ENDED 卡 00:00 → 按 →。

**N-24 状态重申：已闭环 fixed**（第 10/12 轮交叉证据：`r12-ctrl-pause.png` 焦点落「暂停」、QBoxCtrl 日志 `focus=playPause → focus=prev panelFocused=true`、紧凑序列 →/→ 行内移动不快进）。本轮直播侧控制条焦点行为同样受 5s 自动收起约束，验证序列必须单条 adb shell 内完成（§2 方法学教训已记）。

### 第 15 轮（2026-11-05 上午）— 用户反馈 A→E 批次 + P1 族焦点根因修复

真机环境：设备夜间待机后 adb 连接周期性断开（daemon 需每条命令内置 connect）；直播源当晚整体抖动（m3u 502、多线路超时）。

**已修复并真机复验（证据截图均在 `shots/round-2604/`）**：

| 项 | 真机证据 | 结论 |
|---|---|---|
| N-32 直播列表左上黑块 | `n15-live-list.png` | 首行分组/频道完全可读，顶部渐隐柔和 ✅ |
| N-33 选中态/行对齐 | `n15-live-list.png` | 「卫视频道」12% 白底选中态、焦点白药丸（031）、行对齐无跳动 ✅ |
| N-34/P1 族 对话框焦点 | `n15-livedlg7.png` | 「综合直播」实白焦点 pill 一眼可见 ✅；**附加根因修复**：`adapter_live/adapter_config/adapter_restore/dialog_player_engine` 的 `duplicateParentState` 误用导致焦点在但白 pill 不渲染（P1 族共同根因），已移除（commit 见下） |
| N-35 emoji 防护 | `n15-home2.png` | 站点名 🗂 被剥离而非豆腐块（本机字体缺该字形→降级策略生效）✅ |
| N-36 底部弹层统一 | `n15-track2.png` | 「选择音轨」弹层为 qbox_surface 深色面板+白色选中 pill，无 Material 紫灰叠色/蓝控件 ✅ |
| N-37 色彩收敛 | `n15-ctrl.png`/`n15-seeking.png` | 进度条实白、无蓝色控件；品牌蓝仅剩 logo/设置分区竖条/链接语义 ✅ |
| N-38 循环白色激活 | `n15-loop-on-uf.png`、`n15-seeking.png` | 开·非焦点=白描边+白底衬+白字；**且重启后开态保持（N-28 持久化再实证）**；单曲循环在 19:02 播到 20:49 片尾后无缝重播至 02:04（media_session/截图双重佐证）✅ |
| N-39 底图中心截取 | `n15-home2.png`/`n15-detail2.png` | 底图从色块变为可辨识画面内容（16:9 中心横带、~12× 放大）✅ |
| N-40 顶部渐隐 | `n15-detail2.png` | 详情页顶部平滑入黑无硬边 ✅ |
| N-41 最近观看焦点环 | `n15-home2.png` | 走马上任卡片白色焦点环四边完整（含下沿）✅ |
| N-43 快进阶梯 | `n15-seeking.png` + 位置推算 | 按住 ~4.3s 位移约 +250s（10s 细调窗→20s→30s 渐进）；旧阶梯同等时长约 +21:40 直接飞穿 20:49 片尾——对比成立 ✅ |
| N-44 网盘管理 | `n15-drive2.png` + logcat | 点击有响应、网盘管理网格完整渲染（原反馈未复现）✅ |

**代码修复待真人手感/场景复验**：N-45 自动换源上限（需未登录网盘连续报错场景）；N-46 配置兜底（需失效配置首载场景）。

**blocked 不变**：N-26（搜索 ⌫ 长按清空，待修）、N-27（字幕空面板占位，待修）、N-29（章节面板空，待查源）、N-30（异常流 ENDED 死停）、N-42（回到最近观看滚动效果，现象待真机确认）。

**本轮 commit**：`8d153fe`(A) `79753d3`(B+D，含同文件 C9 部分) `d5bbae2`(C) `f5cc81e`(E) + P1 族 `duplicateParentState` 修复（见 git log）。

**过程教训**：① Set-Content 写 XML 带 BOM 导致资源合并失败一次（dialog_player_engine.xml），已用 Write 重写修复——XML 编辑一律走专用工具；② 设置页导航误触图片尺寸（大→特大），已按循环 [小/中/大/特大] OK×3 恢复「大」并截图确认；③ BACK 连按会把应用退到 launcher，导航序列前先确认当前窗口。

---

### 第 16 轮（2026-11-05 白天）— 按钮固定槽位三态实证 + 全站弹层面板化统一

**范围**：用户四要求——①按钮尺寸不随内容位移（高优先）②去 AI 味 ③所有菜单统一一套组件 ④验收记录。长期规则已抽入 [GOLDEN-STANDARDS.md](GOLDEN-STANDARDS.md)（本轮新增 GS-32），本节只记过程与证据。

#### 16.1 按钮固定槽位（片头/片尾/倍速等取值型按钮）

改动：`styles.xml` 新增 `Control.Value`（固定 76dp 槽位 + 内容居中 + `tnum` 等宽数字 + 单行 ellipsize）；`Control` 内边距 14→11dp（960dp 屏 14 按钮行宽回收——88dp 槽位曾致「更多」溢出屏外、控制条无焦点的回归）；`view_control_vod_action.xml`（speed/opening/ending + extra 行 scale/decode/player）与 `view_control_live_action.xml`（line）换用 `Control.Value`；`VideoActivity.requestControlFocus` 加 playPause 末位保险。

**三态 bounds 实测**（1920×1080 像素，白色焦点药丸像素测量法 `shots/round-2604/`）：

| 按钮 | 无秒数 | 有秒数 | 秒数变化 | 结论 |
|---|---|---|---|---|
| 片头 | `[908,977][1051,1043]`（sA3/reset，「片头」） | `[908,977][1051,1043]`（sB_open，「00:01」） | `[908,977][1051,1043]`（sC4「00:05」；sAe「00:04」同） | **完全一致** ✅ |
| 片尾 | `[1068,977][1211,1043]`（sAe5） | —（片尾文案不变） | `[1068,977][1211,1043]`（sCe2，片头=00:05 时） | **完全一致** ✅ |
| 详情（右邻位移检测） | x=[1575..1676]（sA_open） | `[1576,980][1675,1040]`（sB_end/w2/w3） | — | 不动 ✅ |
| 更多（行尾溢出检测） | `[1692,980][1791,1040]`（fix2/probe） | `[1692,980][1791,1040]`（w1） | — | 不动 ✅ |

连续操作截图序列：`fix2`（14 键无溢出）→ `sA3` → `sB_open` → `sC4` → `sAe` → `reset`（片头归零）→ `sAe5`，全行逐帧同位。

#### 16.2 全站弹层面板化（菜单统一组件）

- `shape_panel.xml`（新）：qbox_surface 实底 + 28dp 圆角，全站面板共用背景。
- `BaseBottomSheetDialog.panelize`（TV 端）：窗口 560dp 居中、窗底透明、sheet 用 shape_panel、动画 `Animation_Dialog` 淡入淡出。覆盖 14 个 BottomSheet 弹层（音轨/视轨/字幕/字幕搜索/倍速设置/引擎/音视频设置/弹幕×3/章节/选季/解析/Pass）。
- `BaseAlertDialog`：TV 端统一 560dp（原 0.4/0.55/0.6 比例各处不一）；`onStart` 兜底不设宽的面板（简介等）；窗底透明 + decorView 贴 shape_panel（Material 默认背景自带 ~25dp 阴影内缩，可见面板曾比窗口窄 50px）。覆盖 简介/直播配置/站点/历史/恢复/DoH/UA/弹幕 API/mpv/预载/更新 等 13 个。
- `Theme.Base`：`bottomSheetDialogTheme=ThemeOverlay.QBox.Sheet`（+`QBox.BottomSheet` 关 `margin*SystemWindowInsets`）。

**本机特有坑（→ GS-32）**：该盒子上报假左侧系统 inset（320px），Material 把它转成 sheet 的 marginLeft → 面板偏右、右缘被裁（修复前 dump：sheet=[720..1520] vs 窗口=[400..1520]，panel4.png）。修复 = sheet inset 分发吃掉 + 常驻布局监听每次布局强制 margin 归零 + 窗口 `layoutInDisplayCutoutMode=ALWAYS`。代码里留 `QBoxSheet` Log.e 作哨兵（left>0 即报警）。

**面板一致性实测（两族同参）**：

| 属性 | 音轨面板（BottomSheet，panel8） | 简介面板（AlertDialog，intro3） |
|---|---|---|
| 窗口/可见宽度 | 1120px=560dp（[400..1520]） | 1120px=560dp（[399..1519]） |
| 位置 | 居中 | 居中 |
| 圆角/背景 | 28dp / qbox_surface（shape_panel） | 28dp / qbox_surface（shape_panel） |
| 动画 | Animation_Dialog 淡入淡出 | 主题对话框淡入淡出 |
| BACK 关闭 | 不变（BackCallback 原有行为） | 不变 |

直播配置/设置弹窗与简介同基类（BaseAlertDialog），同参生效，下轮补截图挂账。

#### 16.3 去 AI 味进展

面板已收敛为：实底 surface、无阴影（窗底透明去默认阴影内缩）、28dp 统一圆角、白/灰阶交互色、单一淡入淡出。字号阶梯/图标线宽全量审计挂账下轮（本轮优先级：按钮固定 > 菜单统一 > 质感细节）。

#### 16.4 验证方法学教训（本轮新增）

- **adb input 注入在本机会随机丢键**（0.12~0.7s 间隔均发生，丢 50%~100%）：多键链不可信；可靠手法 = 每条命令自带 82 呼出 + tap 聚焦（tap 前不能有 screencap，否则 tap 失效）+ 尾cap + 像素测量闭环，逐命令验证落点。
- tap 不重置控制条 5s 收起计时器（dispatchKeyEvent 才重置）；命令间隔 >5s 后第一键必丢进 video（↑ 会拉起进度条借用态）。
- 弹窗打开态下 uiautomator dump 稳定可用（视频播放面则不可靠）；弹窗 bounds 用 dump、控制条用截图像素测。

---

### 第 17 轮（2026-11-05 白天）— 控制条整行等分返工 + 倍速交互统一 + 快进阶梯定量复测

**性质**：第 16 轮「单控件固定 76dp」被用户打回（修一个坏一个：片头片尾比邻键宽一圈），本轮按用户规格改为整行等分；并补做防串键、章节入更多、SideSheet→面板统一。快进阶梯经 git+实测证明**无回归**。

#### 17.1 控制条整行等分（GS-33 落地）

- `Control` 样式重构：`layout_width=0dp + weight=1` + 居中 + singleLine + ellipsize + tnum；删除 `Control.Value`（76dp 方案废弃）。VOD 主行 13 键、直播行、Cast 行同吃一个样式；extra 行同规则。
- 章节入口从主行移入「更多」二级行（`chapter` 挪进 extra，显隐逻辑不变）。
- **改后实测**（1920×1080 像素，白色焦点药丸法）：暂停 `[52,979][175,1042]` w=124、片头 `[1038,979][1162,1042]` w=125、更多 `[1743,979][1867,1042]` w=125 —— 宽度差 ≤1px（取整误差），行右缘 1871≈容器 1872（与进度条同容器对齐），无滚动。`shots/round-2604/eq2/eq_s1/eq_s8/eq_extra.png`。

#### 17.2 倍速/枚举交互统一 + 防串键（GS-34 落地）

- 可调值类（倍速/片头/片尾）：↑/↓ 就地调档（倍速沿 `SpeedSetting.PRESETS`，新增 `prev()`，到顶到底钉住不环绕）；OK 开面板（倍速点击由循环改为开 `SpeedSettingDialog`，删除 SPEED_STEPS 点按循环与冗余长按）。
- 选择类（缩放/解码/引擎/线路/章节/音轨/字幕/视轨）：`onControlKey` 对非 CustomUpDownView 焦点 ↑/↓ 直接 consume 无副作用（不再跳进度条）；唯一例外 extra 行内 ↓ 回主行「更多」（否则焦点困死）。
- **真机防串键证据**：暂停态聚焦倍速 ↑×2 → 1.0x→1.5x，position 07:30 纹丝不动、焦点不动（sp1.png）；聚焦音轨 ↑/↓ → 值不变、焦点不动、无 seek（sel1.png）。

#### 17.3 快进阶梯定量复测（用户指控回归 → 证据：无回归）

- git：`git diff d5bbae2 HEAD -- CustomKeyDownVod.java` 为空；`Constant.INTERVAL_SEEK` 自 ab17e53 未动；VideoActivity 自 d5bbae2 唯一 diff 是焦点兜底（与 seek 无关）。**无任何 commit 覆盖分档逻辑**。
- 真机量化（sendevent 合成 down+autorepeat，临时 QBoxSeek 日志逐步记录，测后已移除日志）：holdMs→累计位移：0→+10s、329→+20s、760→+30s、1089→+40s、1487→+50s、1793→+70s、2246→+90s、2658→+110s、3125→+130s、3571→+160s、4447→+220s、5405→+280s。插值 0.5s≈+20s / 1s≈+40s / 2s≈+90s / 4.3s≈+220s，与 d5bbae2 验收值（4.3s≈+250s）同量级（重复节奏差异）。
- 结论：阶梯完好；「一上来就 10s」是设计首步（用户给的验收表 0.5s≈+10s 即此）。按用户最新指令**保留阶梯为有意偏离**（建议：如需更丝滑可在加速前再延长细调窗口，本轮不改）。

#### 17.4 弹层统一收尾（SideSheet→面板）

- 倍速/视频/字幕/音频/弹幕 5 个设置弹窗 TV 端从右侧抽屉（SideSheet 420dp）改走 BottomSheet → 经 panelize 统一为 560dp 居中控件；手机横屏保留抽屉。
- panelize 完善：sheet 高度内容决定（上限 85% 屏高），垂直居中用常驻布局监听 + translationY（BottomSheetBehavior 恒贴底，wrap 窗口高度测量有环）；实测倍速设置面板双居中、内容完整无裁切（spdone12/spd_ok.png）。
- 回归发现：6 个主行按钮变 13 个等分后，extra 行也同步等分（eq_extra.png）。

#### 17.5 pass/fail 表（本轮交付前）

| 检查项 | 结果 | 证据 |
|---|---|---|
| 主行 13 键等分、宽度差 ≤1px | pass | eq_s1/eq_s8/eq2 实测 124/125/125 |
| 行铺满无滚动、右缘对齐进度条容器 | pass | 1871≈1872 |
| 章节入更多、更多恒最后 | pass | eq_extra |
| 倍速 ↑/↓ 就地调档、OK 开面板 | pass | sp1/spdone12 |
| 防串键：调档时 position 不变 | pass | sp1（暂停态） |
| 选择类 ↑/↓ 无副作用 | pass | sel1 |
| extra→主行 ↓ 逃逸 | code-reviewed（真机未截到，记半绿） | onControlKey DOWN 分支 |
| 快进阶梯分档 | pass（无回归，git+日志双证） | 17.3 |
| 五个设置弹窗面板化 | pass（倍速实测；其余同基类路由） | spdone12 |
| 色彩 ≤3、无新增彩色 | pass | 全轮截图目检 |
| 片头/片尾内容变宽不变 | pass（等分天然免疫） | eq 系列 |

#### 17.6 误操作记录（本轮新增，已恢复）

- 验证过程中误点缩放：16:9→原始，已循环恢复 16:9（scale_back.png）；误把倍速调到 2.0x，已面板内点 1.0 chip 恢复（spd_ok.png）；片头曾设 00:0x 测试值，已归零。
- 发现该盒子会叠出**两个 VideoActivity 窗口**（双击导航竞态），导致按键落到非顶窗口、焦点假象——本轮后段大量"丢键"实为此因；force-stop 清场后 sendevent 链一次成功。教训：验证前先 `dumpsys window windows | grep -c` 确认单实例。

---

### 第 18 轮（2026-11-05 白天）— 外部 TV 标准审计（tv-design-standard）

**性质**：纯审计+规则并入，不改产品代码。标准文件 `tv-design-standard/{STANDARD,CHECKLIST,GAP-ANALYSIS}.md`（52 条 TV-01~TV-88）。

#### 18.1 旧 skill 处理（GAP-ANALYSIS 建议已执行）

- `tvos-design-guidelines` 重写为 v2.0.0：29pt 更正为默认值（最小 23pt）；删除无出处的 48pt 标题/60pt 安全区/3–7 tab/自制字号表；250×150pt 卡片与 TAB-06 标注「社区经验值」；4.5:1 标注出自 WCAG。全部条目对齐 TV-xx 编号与级别。
- `android-design-guidelines` v1.1.0：加「手机/平板 MD3，TV 不适用」声明 + 5 处冲突表（动态取色→TV-41、底部导航→TV-53、48dp 热区→TV-03/04、禁纯黑→TV-40、预测性返回→TV-10），保留不删。
- 注意：`.dsh/` 在 .gitignore 内（本地技能），skill 改动只落盘不入仓。

#### 18.2 金标准并入

GS-35~GS-45 共 11 条并入 `docs/GOLDEN-STANDARDS.md`（引用 TV 编号，冲突写明取舍；文件 144 行 ≤150）。commit `d3f1086`（docs 仓，未 push）。

#### 18.3 真机审计 pass/fail（重点 5 项，逐项带取证）

| TV 项 | 检查点 | 结果 | 取证 |
|---|---|---|---|
| TV-22 过扫安全区 | 关键内容不入 5% 过扫区 | **pass（本机）** | 1080p 实测无过扫裁切：topbar 左缘 34.5dp、焦点环 36dp；对照 5% 建议值 48dp 未达，记观察项（720p 盒子有裁切史，TV-23 三档复测挂账） |
| TV-34 对比度 | 正文 ≥4.5:1 实测非目测 | **pass** | 面板白字/qbox_surface 实测 15.6:1 |
| TV-33 fontScale 1.3 | 无截断无出屏 | **部分 fail** | 首页 pass（tab 行不折行、长标题正常 ellipsize，fs13_home.png）；控制条 pass（13 键等分不截断不滚动，fs13_ctrl.png）；**详情页 fail：主演长文与「播放/简介/收藏/换源」按钮行重叠**（fs13_detail.png）→ 登记 N-47 open |
| TV-80 contentDescription | 图标按钮/无文本焦点项有标签 | **部分 fail** | 首页 dump：26 个无自文本焦点节点中 7 有 desc；**「更新推荐」5 张卡片无文本无 desc**（纯海报布局）→ 登记 N-48 open；控制条按钮均有文字=有标签 |
| TV-12 播放键语义 | 中键暂停/呼出控制条；媒体键 PLAY_PAUSE | **部分 fail** | 中键 pass（历轮实证）；**媒体键 fail：sendevent key 85 播放无反应；`KeyUtil.isPlayPause` 为死代码无调用方** → 登记 N-49 open |
| TV-13 快进语义 | 左右固定秒数/长按拖拽 | **有意偏离** | 阶梯 10→300s（17.3 定量曲线），见 GS-42，不改；建议方向=加速前延长首档细调窗口 |

#### 18.4 Top 高危项（本轮新登记）

- **N-47**：详情页 fontScale 1.3 主演文字与按钮行重叠（可读性崩，fs13_detail.png）。
- **N-49**：遥控器媒体键 PLAY/PAUSE 全无效（isPlayPause 死代码；需接 dispatchKeyEvent 或 MediaSession 媒体键路由）。
- **N-48**：更新推荐卡片无障碍标签缺失（TalkBack 静默）。

#### 18.5 有意偏离清单（不改，拍板留档）

| 项 | 偏离 | 理由 | 记录 |
|---|---|---|---|
| 快进阶梯 | TV-13 官方建议固定秒数，本项目 10→300s 阶梯加速 | 长片盲操效率 + 进度实时可见 | GS-42 |
| 海报比例 | 标准 2:3/16:9，本项目竖海报 3:4 | FongMi 片源比例现实 | GS-39 |
| 对比度来源 | 4.5:1 出自 WCAG 非 TV 官方 | 标注溯源，仍按 4.5:1 执行 | GS-38 |

---

### 第 19 轮（2026-11-05 傍晚）— N-49 媒体键接线 + N-47 fontScale 修复 + N-48 卡片 desc + 阶梯细调窗口延长

**性质**：修复第 18 轮审计登记的三项 open（N-49→N-47→N-48），并采纳 GS-42 建议把快进首档细调窗口 1.5s→2.5s。全部真机取证。

#### 19.1 N-49 媒体键 PLAY_PAUSE（最高优先）

- **根因链（三层）**：① 系统把媒体键路由给 MediaSession，不进 `Activity.dispatchKeyEvent`（logcat：`MediaSessionService: Sending KeyEvent... to com.qbox.app/androidx.media3.session.id`），所以 `KeyUtil.isPlayPause` 挂了也没人调；② media3 默认处理在本机实测无效；③ 自写接管后先后踩两个坑——UP 事件漏给 media3 默认实现会二次切换（DOWN 暂停→UP 又播放，实测 state 6→3）；UP 实际到达 Activity 而我加的 dispatchKeyEvent 兜底再次切换（EventLogger：pause 后 100ms 被 play 拉回，USER_REQUEST）。
- **修法**：只在 `PlaybackService.onMediaButtonEvent` 接管播放键族（PLAY_PAUSE 切换 / PLAY 只播 / PAUSE 只停），DOWN/UP 全消费；VideoActivity 不留任何媒体键兜底（FF/REW 也移除，否则 session DOWN + Activity UP 双重 seek）。NEXT/PREV/STOP 走 media3 默认 → `wrap()` 已映射 dispatchNext/Prev/Stop（换集/停播语义保持）。
- **实测**：播放中 `input keyevent 85` → `dumpsys media_session` state 3→**2**（position 冻结，两次查询同值），再按 → **3**；最终无日志构建复测 3→2→3。FF（90）= **+15s** 单次（EventLogger SEEK +15000 量级），REW（89）= **-5s**（EventLogger：pos 2380867→2375858）。NEXT/PREV/STOP 代码路径已在 wrap() 接线，未实测（会换集/退出，语义符合预期，记录为「接线存在、未逐项真机」）。
- **证据**：`shots/round-2604/n49_fixed2_pause.png`、logcat QBoxKey/EventLogger 序列。

#### 19.2 N-47 fontScale 详情页重叠

- **根因**：详情页文本栈（片名/备注/信息行/导演/主演）从顶向下长，按钮行 `layout_alignBottom=video` 从底向上长，中间无硬约束——fontScale 1.3 时两端对撞重叠。
- **修法**：`actor` 改单行 ellipsize + `layout_above=@id/row2` 钉住按钮行上缘（+8dp margin）；`name` 顺带收为单行（2 行 32sp 在 1.3 下独占 110dp，是主要溢出源）。无负 margin。
- **三档实测**（uiautomator bounds，actor 底 vs row2 顶）：1.0 → 399 vs 415（gap 16px）；1.15 → 391 vs 407（16px）；1.3 → 385 vs 401（16px）。三档无交叠；1.3 截图目检演员单行省略、按钮行完整（`n47_13.png`）。测完 fontScale 已恢复 1.0。

#### 19.3 N-48 更新推荐卡片无障碍名

- **修法**：`VodRectHolder`/`VodOvalHolder` 根节点补 `contentDescription`（标题→备注→站点名兜底）。收藏/最近观看卡片有可见文本，天然有标签，不缺。
- **实测**：首页 dump 26 个无自文本焦点节点：7 图标 + 5 推荐卡有 desc、**0 缺失**（修复前 5 缺失）。TalkBack 朗读验证 **blocked**（本机无 TalkBack 服务，`enabled_accessibility_services=null`），以 dump 存在性替代。

#### 19.4 快进阶梯：首档细调窗口 1.5s→2.5s（GS-42 建议落地）

- `stepFor`：10s 档 0–2.5s → 20s 档 2.5–4.5s → 30s 档 4.5–7s → 60s 档 7–10s → 120s 档 10–14s → 180s 档 14–18s → 300s。
- **实测曲线**（sendevent 合成按住 + 临时 QBoxSeek 日志，测后已删）：hold 0→+10s、435ms→+20、1001→+30、1539→+40、2069→+50、2586→+70（2.5s 准点升档）、4006→+130、4523→+160（4.5s 准点升档）。
- **位移表**：0.5s≈+20s / 1s≈+30s / 2s≈+50s / 4s≈+130s（旧窗口为 +20/+40/+90/+200）——2s 处不再过早冲进 20s 档。

#### 19.5 误操作 / 环境事件

- 验证期间另一会话把 `com.baiguotv.tv.debug` 推到 192.168.2.214 前台一次（白果TV 调试包）；已把 qbox 拉回前台继续。提醒：该盒为多会话共用，验证前先看 `mCurrentFocus`。
- 设备 fontScale 已恢复 1.0；播放进度为正常观看记录，无测试残留值。

---

### 第 20 轮（2026-11-05 深夜）— 快进斜坡重做（起步 1s 级）+ 海报背景/顶栏过渡 + 筛选行紧凑化

**背景**：19 轮把首档 10s 窗口延长被用户判"不达标"——实测手感仍是 10s 起步（19 轮验收口径只看 2s 位移变小，错）。20 轮按用户口径重做：前 3 秒全程 1s 级细调。另两组：海报背景马赛克/顶栏硬分界线、筛选子列表松散。

#### 20.1 快进/快退斜坡重做（commit 见代码仓）

- **新斜坡**（`CustomKeyDownVod.RAMP` = {阶段上限, 步长, 节拍}）：0–3s：±1s/130ms → 3–5s：±10s/500ms → 5–7s：±30s/900ms → 7–10s：±60s/700ms → 10–14s：±120s → 14–18s：±180s → 300s。按下首 tick 即 +1s，松手即停。
- **FF/REW 统一**：媒体键不再走 media3 固定 ±15s/-5s；`PlaybackService.onMediaButtonEvent` 把 FF/REW 转发给前台 Activity（`NavigationCallback.onMediaSeek`），复用同一斜坡+同一条控制条 seek UI。
- **实测采样表**（sendevent 合成按住 + QBoxSeek 日志，注入节拍 ~190ms 为下限瓶颈，真遥控器 50ms autorepeat 会更密）：

| 按住 | 位移 | 阶段 | 当次步长 |
|---|---|---|---|
| 0.3s | +2s | 1 | 1s |
| 0.5s | +3s | 1 | 1s |
| 1s | +6s | 1 | 1s |
| 1.5s | +8s | 1 | 1s |
| 2s | +11s | 1 | 1s |
| 2.5s | +14s | 1 | 1s |
| 3s | +16s | 1 | 1s |
| 4s | +26s | 2 | 10s |
| 5s | +46s | 2 | 10s |
| 6s | +76s | 3 | 30s |
| 8s | +166s | 4 | 60s |

  左键对称（1.2s 按住 −1s×6）。FF/REW 单击 = ±1s（QBoxSeek：hold=0 step=1000 acc=±1000）。位移曲线图 `shots/round-2604/ramp_curve.png`（前段平缓、3s 抬升肉眼可见）。
- **验证坑位**：`dumpsys media_session` 的 position 在广告过滤片源上会与 EventLogger mediaPos 差 ~45s（过滤段重映射），读 position 判位移会误判成"倒退"——判位移以 EventLogger 的 SEEK pos 对为准（最终包 2s 按住 = 7×+1000ms 精确步进）。
- FF/REW 长按的 repeat 注入无法用 sendevent（媒体键不路由）/input（无 repeat）模拟，按住升档路径与方向键同一函数，构造上同源；真机长按住留待用户手感确认。

#### 20.2 海报背景与顶栏过渡（首页 + 分类页 + 详情页）

- **底图**：`override(160,90)`→`override(480,270)`（最终放大 12×→4×），alpha 0.62→0.72——马赛克收敛、内容可辨识。
- **顶栏**：`shape_top_scrim` 重写为 200dp 三段渐变（#000→#0C1116@36%→透明），替代旧 124dp 40%→0（那条在 y=248px 产生可辨认分界带）；`qbox_header_mask` 124dp→200dp（顶栏 70dp 的 2.9 倍，满足 ≥1.5×）。
- **整屏 scrim**：顶部 55%→28%（顶栏压暗由 top_scrim 承担，不再双层叠黑），中部 35%→30%，底部 85% 不变。
- **左右缘**：各 96dp `shape_edge_fade_start/end` 渐隐到黑。
- **实测**：顶部 200dp 放大图无分界线/色带（`g2_top200_zoom.png`）；竖带亮度剖面相邻行最大跳变 20.18/255（改前 60.27，且改后峰值位于海报内容本身非 UI 边带）；改前改后同海报对比 `n49_home2.png` vs `g2_home_new2.png`。GS-20 已改写（与旧"160×90/12×"设定冲突，以本轮为准）。
- **详情页**：同套底图/scrim 生效，演员单行无重叠保持（N-47 不回退）。注意：用户随后提出详情页背景仍不满意（渐变太急、黑色占比大），已立项下一轮做 Netflix/Apple TV+ 对标研究 + 2~3 版对比实现，本轮参数可能再变。

#### 20.3 筛选子列表紧凑化

- **行距**：行间 40dp→16dp（面板展开期间 `setVerticalSpacing(8dp)` + 行内 grid padding 8dp→4dp；收起时 applyRowMetrics 恢复海报行距；applyRowMetrics 在面板展开态保持 8dp 不被覆盖）。
- **字号**：14sp→13sp（chip 高 34dp）。
- **选中态**：`shape_item_round_activated`（selector 的 state_selected 映射）补 1.5dp 白描边 + 15% 白底——验证过程中发现 dump `selected` 对默认项为 false（未点选时无选中项），实选「国产剧」后 sel=true，焦点移开截图确认描边在（`g3_sel4.png`）。
- **对齐**：chip 左缘 x=64px=tab 左缘 64px ✓（`g3_f2.xml` bounds）。
- **bounds 表**：热度 [64,290][180,358] / 全部 [64,390][180,458]，chip 高 68px=34dp，行距 32px=16dp。

#### 20.4 误操作 / 环境事件

- 误点首页源名胶囊把站点切成「我的云盘|配置」，翻站点对话框恢复「豆豆丨片单」；期间误入一次搜索页（黑球）。设备最终态已核对：站点=豆豆丨片单、首页结构正常。
- 验证期间设备多次被另一会话（白果TV debug/release、当贝桌面）抢占前台；每次注入前查 `mCurrentFocus`，不在前台时暂停等待，未在他人前台期间注入。
- 教训：筛选 chip 选中态靠 `state_selected`（setSelected），不是 state_activated——selector 命名有误导性（`_activated.xml` 文件实际服务 selected 态）；改 drawable 前先确认状态映射，差点把选中描边改没了（已补救重建）。

---

### 第 21 轮（2026-11-06 凌晨）— 详情页海报背景对标重做（Netflix / Apple TV+ 研究 + A/B/C 三版对比）

**起因**：用户对详情页背景不满——「渐变太急、一下就进纯黑；黑色占比过大」。给出两个方向（顶部纯黑区只盖 tab 行 / 干脆铺满海报），要求先研究 Netflix / Apple TV+ 再动手。

#### 21.1 研究结论（来源已分级）

- **Netflix**：TV 详情页背景用 16:9 key art 原图全幅铺满，**不模糊**；底部一条 black→#141414 渐变接入内容区；顶部靠 0–12% 屏高的短黑渐变（a 0.6→0）护导航，**无纯黑条**。（来源：Curio 设计分析 + 截图观察推断；Netflix TV UI 闭源，数值为推断）
- **Apple TV+**：整屏 full-bleed 海报 + 轻遮罩（**非**「顶部黑区+内容区」）；官方 SwiftUI 示例给出底部 material 渐变 stop（自下而上 black@25%、a0.3@37.5%、a0@50%）；导航是半透明模糊悬浮条，不是黑区。（来源：Apple 官方示例代码 / HIG Materials）
- 两家的共同点：**都不做整屏均匀压暗**，暗化只发生在「顶部护导航的一小段」和「底部接内容的一段」，中段海报尽量裸露——我们旧版正是整屏 28–30% 均匀压暗 + 85% 底，黑得又早又多。

#### 21.2 三版实现（DEBUG 广播 `com.qbox.app.DEBUG_BACKDROP --ei v 0/1/2` 运行时切换）

| 参数 | A（Netflix 风，**默认**） | B（顶部实底只盖头部行） | C（Apple 风轻遮罩） |
|---|---|---|---|
| 顶部 | 130dp 渐变 60%→0 | 70dp 实底 #F20C1116 + 210dp 长渐隐 | 100dp 渐变 45%→0 |
| 中段 | 全透到 45% 屏高 | 全透到 55% | 15%→20% 轻压 |
| 底部 | 45%→100% 升到 85% | 55%→100% 升到 85% | 55%→100% 升到 75% |
| 右侧文字 scrim | 原 detail_scrim（70%/90%） | 同 A | 弱一档（55%/82%） |
| 底图 alpha | 0.80 | 0.72 | 0.85 |

实测（嫁金钗，右半屏）：纯黑区占比 A 0.2% / B 0.3% / C 0.1%；右下象限亮度 A 30.9 / B 31.0 / C 33.2；顶部条带亮度 A 38.9 / B 34.4 / C 44.7。截图：`j_a/j_b/j_c.png`（亮海报）、`p_a/p_b/p_c.png`（暗海报醉梦）。
亮海报（后会友期）A 版文字可读性验证通过（`j_bright2.png`）；播放中返回详情页状态一致（`j_return.png`）。
**默认 A**：Netflix 同款结构，护文字与海报裸露最均衡；C 最贴近「铺满海报」诉求但文字保护最弱，B 黑区最重。用户可广播切换实机对比后拍板，拍板后把 layout 默认 drawable 换掉即可。

#### 21.3 记录

- 照抄：Netflix「顶部短渐变 + 底部接内容」结构、Apple 官方示例的底部渐变节奏思路。
- 改编：底部 stop 位置按我们页面内容起点（选集行 ~55% 屏高）对齐，没用 Apple 的 25/37.5/50 绝对值；右侧文字区 scrim 是我们自有结构，保留并按 C 版弱化。
- GS-20 已补「详情页例外」段。
- 遗留：DEBUG 广播切换器只在 debug 包注册；若用户拍板非 A 版，改 `activity_video.xml` 的 `backdrop_scrim` 默认 drawable + 详情页 alpha 一行即可。

### 第 22 轮（2026-11-06 上午）— C 版装机为默认 + D 版「铺满 + 四边轻收边」

**起因**：用户看过 A 版后否掉「为导航做渐变黑边」的方向——黑边不好看，海报要直接铺满，只在四边做一点点收边与电视边框融合，"不要用力"；同时要求默认改成 C 推到 .214 实机对比。

#### 22.1 变体系统升级（A/B/C/D 四版 + 持久化 + 默认 C）

- `VideoActivity`：变体数组扩到 4 版，`applyBackdropVariant` 写入 Prefers `debug_backdrop_variant`（DEBUG），initView 读回；`BACKDROP_VARIANT_DEFAULT = 2`（C）。layout 静态默认三件套同步改 C（`shape_backdrop_scrim_c` + `shape_detail_scrim_light` + alpha 0.85）——两处必须同步，GS-20 已写明。
- 切换方式（debug 包）：`adb shell am broadcast -a com.qbox.app.DEBUG_BACKDROP --ei v <0/1/2/3>`（A/B/C/D），即时生效且持久化，重进详情页保持。**用户侧只有遥控器无法自切**——需要电脑/手机 adb 执行一次；release 包不注册该广播。
- 装机验证：全新 install 后首开详情页即 C（`d-jinchai-default.png`）。

#### 22.2 D 版参数与实测（一次实测驱动的校准）

- 结构：无顶部黑带、无整屏压暗；四边收边 上/左/右 56dp ≤24%，**底边 120dp ≤40%**（实测后加强）；右侧文字区横向 scrim 10%→45%@50%→75%。
- **初版被实测打回**：四边 56dp/24% + 右侧 30%@0.55/65% 在亮海报（后会友期）上演员行/选集行被洗白（区域中位对比度：底左剧名 2.45:1）→ 按用户指定的「极轻局部 scrim」路径加强（非黑条），校准后复测：
- **校准后亮海报对比度**（区域中位相对亮度 vs #F5F5F7 文字）：标题 6.20 / 更新行 5.87 / 演员行 7.14 / 选集行 8.28 / 底左剧名 3.65——全部 ≥3:1（WCAG 大字），剧名 3.65 略低于 4.5 但它是标题的冗余重复，接受。
- **「不要用力」量化**（嫁金钗同海报 A/C/D，屏蔽内嵌视频框）：纯黑占比 A 0.01% / C 0.76% / D 0.00%（均≈0，无黑块）；四角相对中心压暗 A 76.4% / C 88.2% / **D 68.5%**（含海报自身内容）；顶部条带亮度 A 0.0395 / C 0.0439 / **D 0.0550**（D 无黑带的直接证据，比 A 亮 39%）。
- 截图：亮海报 D `d-houhui-D2.png`、暗海报 D `d-zuimeng-D.png`、中调 A/C/D 同页 `d-jinchai-A/default/D3.png`。
- **取舍说明**：D 在暗/中调海报上完胜（海报最裸露、文字全可读）；亮海报上底左剧名是天花板（3.65:1），再加 scrim 就回到黑带——若用户在意，候选是给剧名单独加底色 chip（本轮未做，等拍板）。
- 教训（导航）：首页「最近观看」返回后**焦点留在最后聚焦的卡片**，不是第一张——本轮因按"焦点回第一张"假设连按导航，误进后会友期详情还把 D 截图/A 版量化测串了海报（已重测修正）。同类脚本化导航必须先截图确认焦点再发键。

### 第 23 轮（2026-11-06 上午）— 快进斜坡放缓 + 跨档速率爬升

**起因**：用户实测 86c6176：前 3 秒细调对了（不动），但 2s 后加速度步子偏大，且档位切换突变，要平滑爬升。

- **实现**（`CustomKeyDownVod`，commit 见代码仓）：档位 {上限, 步长, 节拍} = {3s: 1s/130ms（定稿不动）} → {5s: 5s/600ms} → {7s: 10s/700ms} → {10s: 25s/800ms} → {14s: 50s/700ms} → {18s: 100s/500ms} → {∞: 180s/400ms}；每次跨档后 **800ms 速率线性爬升**（step = lerp（上档速率， 本档速率） × 实际间隔，窗内 150ms 密集采样）。
- **失败的第一版**（自查发现，未交付）：步长插值 + 过渡窗 min 节拍——3-5s 段位移反增到 +44s（旧 +26s），与"更平缓"目标相反；根因是密节拍 × 中步长使瞬时速率冲过本档。改为速率插值后解决（教训：斜坡过渡要对**速率**做文章，步长×节拍的组合变化会隐性改变速率）。
- **改前/改后实测对照表**（sendevent 合成按住 + 临时 QBoxSeek 日志逐 tick 插值，测后日志已删；注入节拍 ~150-200ms，真遥控器 50ms autorepeat 同函数同源）：

| 按住 | 改前（86c6176） | 改后 | 变化 |
|---|---|---|---|
| 0.5s | +3s | +3.3s | ≈ |
| 1s | +6s | +6.0s | = |
| 1.5s | +8s | +8.8s | ≈ |
| 2s | +11s | +11.7s | ≈ |
| 3s | +16s | +17.5s | ≈ |
| 4s | +26s | +24.9s | −4% |
| 5s | +46s | +32.6s | **−29%** |
| 6s | +76s | +44.8s | **−41%** |
| 8s | +166s | +83.5s | **−50%** |

- **各档速率**：7.7（不变）→ 8.3 → 14.3 → 31.3 → 71 → 200 → 450 s/s（旧 7.7→20→33→86→400→600→1000），档间过渡 800ms。
- **爬升实证**（F5.0 跨档段）：步长序列 1000→1416→1338→1434→1460→5000→5353→2283→2364→2855→10000——过渡窗内逐 tick 渐进，无台阶跳变。
- **快退对称**：|F+B| 在 0.5/1/1.5/2/3/4/5s 各点 ≤404ms（注入速率抖动量级，代码同函数仅取负）。
- **播放状态/文案/进度条**：长按期间控制条借出显示「目标时间/总时长 + 进度条」（`mid-hold.png`：12:19 / 1:39:46），进度条逐 tick 跟随；seek 中 state=6 缓冲、松手即 state=3 续播（position 与目标一致）。
- GS-42 已按新档位 + 爬升规则改写（前 3 秒段标记"定稿不得再动"）。
- 注入模拟不到：FF/REW 媒体键长按 repeat（sendevent 不路由 MediaSession、input 无 repeat）——方向键与 FF/REW 共用 `seek()` 同一函数，构造同源；真机长按手感留待用户确认。

---

## 3. 历史遗留与易回归

> 这些是此前轮次记录、易在回归中复发的问题，改代码前必须对照。

### 3.1 H 系列台账（详见 ui-ux-audit.md §2）

- H-01/H-02/H-03（播放页进度条 BACK 吞键、快进后菜单键点不到、控制条收起后第一下误触快进）→ **易回归**，本轮需在播放页逐一复验。
- H-07/H-08/H-09（分类页切分类回顶、收起筛选焦点悬空、展开筛选焦点甩网格）→ 筛选链路易回归。
- H-22（返回键层级不统一）、H-23（空态/错误态/加载态各写各的）、H-27（焦点视觉三种混用）→ 一致性专项，需在统一组件规范落地后收口。
- H-25（剧集行右侧溢出）、H-24（倒序提示撑黑块）。

### 3.2 曾引入回归的改动（务必注意）

| 改动 | 回归 | 结论 |
|---|---|---|
| `5f1b1c7` 站点 tab 右缘渐隐（FrameLayout 包裹） | 搜索页整页空白（N-19） | 已 revert（`5fd8f55`）；N-13 需非侵入方案 |
| 给 Leanback RecyclerView 加装饰层改父层级 | `setHeader` 依赖直接父子关系断裂 | 禁止改 recycler 父层级 |

### 3.3 硬约束（改代码前必读）

- 不要用 PowerShell `Set-Content -Encoding UTF8` 改 Java（会写 BOM，javac 报 `\ufeff`）；用编辑工具或 UTF-8 无 BOM。
- 改 `strings.xml` 注意 zh-rCN 与 values 两处。
- 不重写业务逻辑与网络层，只改交互与视觉。

### 3.4 工作纪律（每轮自查）

1. **禁止 `adb uninstall`**，只能 `install -r`；如需重装必须先说明影响并等待用户确认。
2. 每次 adb 操作前确认设备序列号，`-s 192.168.2.214:5555` 必须显式指定；**绝不碰 192.168.2.132**（另一路会话正在使用的设备）。
3. 任何「无法验证」的项一律标 **blocked** 并写明原因，**不许标 fixed**。
4. 每轮结束把本轮误操作/数据安全事件记入下表，提醒后续轮次。

### 3.5 误操作 / 数据安全事件记录

| 日期 | 事件 | 影响 | 教训 |
|---|---|---|---|
| 2026-11-04 | 误用 `adb uninstall com.qbox.app` 代替 `install -r` | 测试机本地数据清空（播放历史、收藏、配置丢失）；后由 debug 包自动配置源恢复到「豆豆\|片单」 | 安装只允许 `install -r`；卸载前必须停下确认 |

---

## 4. 回归检查清单（每轮提交前必跑）

> 用 adb 逐键操作，每步截图 + 对照焦点框；不依赖 uiautomator bounds 判断焦点（Leanback 列表返回旧坐标）。
> 关键 logcat：`adb logcat -s QBoxSnap QBoxGrid QBoxFocus`。

### 4.1 首页
1. 冷启动：首焦点落在第一张内容卡（有历史→历史卡）。
2. ↑ 分类条 → ↑ 源名 → ↑ 停住；←/→ 焦点移动 y 不变；OK 进分类页。
3. 源名右移：搜索/直播/收藏/推送/设置依次展开 icon+文字，其它图标不位移。
4. 内容区 ↓×N 每排贴顶、无上一排残影；↑×N 回第一排再进分类条。
5. 海报行 ←/→ 整排 y 不变；最左/最右完整。
6. 断网/空数据：不白屏、有提示。

### 4.2 分类页
1. 进页 tab 与首页同 y 同高；首焦点第一排海报。
2. 筛选展开→落第一 chip→点选刷新焦点回该排→收起焦点回网格。
3. 换分类再回来列表位置不重置。
4. 滚到底自动翻页、翻页后焦点不丢。

### 4.3 搜索 / 聚合
1. 进页首焦点搜索框；↓ 进键盘；旧请求不覆盖新请求。
2. 热词首行 ↑ 回搜索框；BACK 直接退出。
3. 聚合：loading/空结果/有结果三态；站点 tab 高亮当前页；↓ 进当前页网格。

### 4.4 设置 / 收藏 / 投屏 / 更新
1. 设置每行可聚焦、二级页有返回、弹窗打开焦点不被抢。
2. 收藏首焦点第一项、删到空有 empty 态。
3. 投屏进页有焦点、二维码可聚焦。
4. 更新下载中可返回、禁用按钮不接焦点。

### 4.5 详情 / 播放 / 直播
1. 详情：播放/简介/收藏/换源可聚焦；剧集行不溢出；倒序提示不遮半屏。
2. 播放：↑ 呼出控制条（首焦点播放/暂停）；进度条 ←/→ seek；控制条按钮可逐项点按（修复 `ea3947a`）；倍速/音轨/字幕/选集正确。
3. 直播：频道列表/信息条/返回层级正确；换台不丢焦点。

---

## 5. 统一组件 / 交互规范（落地后回填）

> 目标：焦点态、按钮/卡片/列表/对话框/加载态/空状态/错误态一套参数全站复用，禁止每页各写一套。
> 本节在规范确立后填写具体 token（焦点环宽度、圆角、间距、动效时长、三态文案等）。

| 组件 | 现状问题 | 目标规范 | 状态 |
|---|---|---|---|
| 焦点态 | H-27：白药丸/白描边/半透明白底三种混用 | 全站统一（拟：卡片白药丸 3dp、列表整行白药丸、图标圆形描边） | 待收口 |
| 间距/圆角 | 已统一到 `dimens.xml`（32dp 内容区、16dp 卡片间距、24dp 行距） | 维持单一来源 | 已落地 |
| 动效 | 焦点放大 ZOOM_FACTOR_SMALL、150–250ms | 统一时长 | 待确认 |
| 三态 | H-23：各页各写 | 统一 loading/空态/错误态组件 + 文案 | 待收口 |
| **弹层面板（第 16 轮落地）** | 曾两套 UI（BottomSheet 默认紫灰 vs 面板对话框） | **两族基类收敛**：`BaseBottomSheetDialog.panelize` + `BaseAlertDialog.setWidth`；560dp 居中、shape_panel（qbox_surface+28dp）、淡入淡出、BACK 不变 | **已落地**（音轨/简介实测同参，见 §16.2） |
| **取值型按钮（第 17 轮重做）** | 片头/片尾随秒数变宽、整行跳动；16 轮 76dp 定宽被用户打回（修一个坏一个） | **整行等分**：`0dp+weight=1` + 居中 + tnum + ellipsize，宽度由等分决定不由文案决定（GS-33；禁单控件定 dp） | **已落地**（各键宽度差 ≤1px、行铺满无滚动，见 §17.1） |

---

## 6. 构建 / 测试 / 安装（复用命令）

```powershell
# 构建 debug（armeabi-v7a）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .toolchain\build.ps1 -Abi v7a -Variant Debug
# APK
fongmi-561\app\build\outputs\apk\leanbackArmeabi_v7a\debug\app-leanback-armeabi_v7a-debug.apk
# 安装（覆盖，保数据）
.toolchain\android-sdk\platform-tools\adb.exe -s 192.168.2.214:5555 install -r <apk>
# 单测（Robolectric：配置 + 直播 + 首页布局）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .toolchain\test.ps1
```

**注意**：build.ps1 常在 APK 产出后不退出（Gradle daemon 挂起，退出码 1）；判断成功用 APK 的 LastWriteTime，然后 kill 任务手动安装。

---

## 7. 设计规范依据

项目内存在 `.dsh/skills/tvos-design-guidelines/SKILL.md`（Apple HIG for Apple TV）与
`.dsh/skills/android-design-guidelines/SKILL.md`（Material 3 + Android 平台规范），本记录按其执行。
QBox 是 Android TV / TV 盒子产品，故以 Android TV 实际规范为主、tvOS HIG 的 10-foot UI / 焦点驱动原则为辅：
- 焦点驱动、每个可交互元素必须有清晰焦点态；焦点移动符合空间排布；无焦点陷阱；保留焦点记忆。
- 10-foot UI：正文 ≥29pt（Android TV 用 sp 折算）、高对比度（WCAG AA ≥4.5:1）、少字、大热区（≥48dp）、TV 安全区（overscan 预留）、避免细字/发丝边框。
- 遥控器：Menu/BACK 每个层级可返回；播放/暂停键全局控制媒体；不做多指手势；文字输入提供语音/预填替代。
- 组件状态（加载/空/错误）一致；动效 150–250ms 反馈；不滥用渐变发光与过度圆角。

---

## 8. 功能验证矩阵（第 9 轮真机走查）

> 状态：✅ 通过 / ⚠️ 部分（标注原因）/ ❌ 未覆盖。

| 功能 | 正向路径 | 异常/边界 | 状态 | 说明 |
|---|---|---|---|---|
| 首页 | 推荐位、最近观看（16:9+进度条）、分类条、顶栏 5 图标 | 顶栏焦点链 ↑/↓、图标展开 | ✅ | 焦点链 源名→搜索→直播→收藏→推送→设置 正常 |
| 分类页 VodActivity | 分类 tab 切换、海报网格、筛选按钮可见 | 焦点 tab↔内容 | ✅ | 焦点热播电影 tab；↓ 到第一张海报；→ 切 tab 正常 |
| 搜索 SearchActivity | 首焦点搜索框、7×7 键盘（含中文标点）、历史/热搜 chip | 键盘焦点、BACK 退出 | ✅ | 键盘 49 键、焦点进「A」键正常 |
| 详情 VideoActivity | 海报/简介/元数据/动作行/剧集/倒序分页；焦点链 | 简介 `&nbsp;`（N-25 已修） | ✅ | 焦点链 播放器→(→)动作行; ↓线路→剧集→倒序 |
| 播放（内嵌+全屏） | 起播（media_session state=3）、全屏、进度条显示 | 控制条焦点（N-24 已修） | ⚠️ | 起播/全屏/进度条 ✅；控制条 14 按钮点按因 N-24 待复验 |
| 直播 LiveActivity | CCTV17 起播、频道列表（分组+台标）、信息条 | 频道列表呼出/收起 | ✅ | OK 呼出列表、当前频道白药丸、半透明遮罩可读 |
| 收藏 KeepActivity | 空态 3D 盒子 + 引导文案 | 空态焦点落返回键 | ✅ | 顶栏返回+标题、空盒图示正常 |
| 推送 PushActivity | QR 码（白点黑底可扫）、剪贴板、URL | — | ✅ | 标准顶栏（R23-01）正常 |
| 设置 SettingActivity | 两列面板（内容源/播放｜系统/维护）、焦点导航 | 弹窗/二级页 | ✅ | 12 个可聚焦行、焦点上下右移动正常 |
| 恢复对话框 | 备份列表、焦点移动、删除图标 | P1「无焦点」复验 | ✅ | 焦点正常落列表条目，DOWN/RIGHT 可移动（未复现 P1） |
| 二级设置页（播放/解码/预载/弹幕） | 播放设置 9 项、解码 6 项（隧道切换+切回）、预载开关、UA 对话框、播放键、备份、版本、DoH、无痕、点播配置、缓存清理 | 弹幕入口为产品决策隐藏（代码注释确认）；部分循环项（引擎/渲染/缩放/后台播放/缓冲）只读值未逐项切换 | ✅ | 第 13 轮 |
| 投屏 CastActivity | — | — | ⚠️ | 推送页已验证；独立 CastActivity 未单独进入 |
| 更新检查 | — | — | ❌ | 需真实网络到 GitHub，未触发 |
| 播放二级功能（倍速/音轨/字幕/选集/快进阶梯） | — | — | ⚠️ | 被 N-24 控制条焦点阻断，修复构建后补验 |
| 直播 EPG / 换台 / 加密分组 | EPG 面板（36/141 频道有数据、当前节目高亮、列表内焦点）、↑↓直换台、组内选台换台、错误态（加载/超时）、分组切换播放不中断 | 加密分组 blocked（源 pass=0）、手动切线端到端 blocked（源 502/超时）、数字选台 blocked（未实测）、N-31 长按收藏 blocked（注入局限，候选修复已提交） | ⚠️ | 第 14 轮 |

> 未覆盖项将在 N-24 修复构建 + `install -r` 后，用「播放键=呼出菜单」设置补齐播放页二级功能；EPG/加密分组依赖具体源配置。