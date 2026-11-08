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

### 第 24 轮（2026-11-06 中午）— seek 精简态 + 源质量标签 + 分段渐隐 + 默认 D + 取景实验

**起因**：用户参照白果TV 提出：A. seek 时只显示「进度条 + 名称/信息行」，名称行加真实轨道小标签；B. 收起改分段渐隐；C. 详情页默认改 D（铺满），并问模糊海报能否取更多视场。

#### 24-A 精简态 + 轨道标签

- **实测可得字段清单**（当前测试源 嫁金钗/豆豆片单，EventLogger onTracksChanged 实抓）：视频 `mime=video/avc codecs=avc1.640020 res=1440x810 color=BT709/Limited/SDR`；音频 `mime=audio/mp4a-latm codecs=mp4a.40.2 ch=2 rate=44100`；元数据 `application/id3`。即本测试源可靠给出：810p / H.264 / AAC / 2.0，无 HDR 无 DV 无 Atmos（如实不显示）。
- **判断依据与置信度**：分辨率=`Format.height`（高置信）；编码=`sampleMimeType` 容器级（高置信）；HDR 仅 `colorInfo.colorTransfer`（ST2084→HDR10、HLG→HLG，bitstream 级，拿不到不显示）；Dolby Vision 仅 `video/dolby-vision` mime（HLS/DASH 清单级可靠；TS 重封装的 DV 可能漏标——宁可不标）；Atmos 仅 `audio/eac3-joc`（JOC 码流级可靠）；TrueHD 的 Atmos 无法从 Format 判 → 只标 TrueHD 不标 Atmos。全部白名单映射，表外/缺失一律不显示（GS-47）。
- 实现：`view_control_vod.xml` 标题行改 [标题 weight=1 省略] + [track_info 12sp mute]，行高由 18sp 标题决定，标签出现/消失不改行高不推挤标题与秒数；`enterSeekMode` 隐藏按钮列（精简态），`showControl` 恢复完整态并刷新标签。
- 实证：`r24_compact.png`（折月亮第01集按住快进：只有 名称+`800p · H.264 · AAC · 2.0` + 进度条 02:37/45:53，无按钮列，面板只包两行）；`r24_full.png`（完整态按钮原位同尺寸，标签同行右侧）。
- 取舍登记：面板底锚定 wrap_content，展开时名称/进度条整体上移一个按钮行高（按钮列两态位置/尺寸完全一致，符合约束；名称/进度条的模式间位移是 TV 展开的常见模式，已写入 GS-48 接受项）。若用户要求进度条也不动，只能固定面板高度留透明空区——待拍板。

#### 24-B 分段渐隐

- 实现：`hideControl` 改两段——按钮列 alpha→0（150ms），120ms 后整体（名称+进度条）alpha→0（180ms），结束才 GONE；代次守卫防「渐隐途中重新展开被旧回调 GONE」（`cancel()` 也会触发 `withEndAction` 的坑）。
- 验证：300ms 渐隐窗短于 screencap 往返延迟，**中间帧截图抓不到**（如实标注）；做了竞态实证——渐隐开始 150ms 内重新呼出，面板完整不透明（`r24_race.png`）。渐隐手感留用户遥控器确认。

#### 24-C 默认 D + 取景视场

- 默认切 D：`BACKDROP_VARIANT_DEFAULT = 3`，layout 静态默认同步（scrim_d + detail_scrim_d + alpha 0.85）；全新 install 首开详情即 D（`r24_d_default.png`）。
- **当前裁剪/放大**：Glide `override(480,270).centerCrop()` → 解码 480×270 后放大到 1920×1080（约 4×，氛围模糊的 softness 来源）。**视场**：16:9 横版剧照 ≈100% 显示（这类片源上「看不清」的主因是 4× 放大不是裁剪）；竖版 3:4 海报 centerCrop 只取中段 **42%** 高度（折月亮实测：整版书法/人物构图被切成中段横带）。
- **三档取景对比**（折月亮竖版海报同页）：f=0 当前 `r24_zyl_f0.png`；f=1 满高完整海报叠模糊底 `r24_zyl_f1.png`（100% 内容，可直读「折月亮/敬请期待/爱奇艺」）；f=2 72% 高 `r24_zyl_f2.png`。切换 `--ei f 0/1/2`（Prefers 持久化）。**默认 f=0 等用户拍板**；设备上留了 f=1 便于实机直看。
- 取舍：f=1 认出率最高，但完整海报会压在部分文字区下（演员行在亮肤色区对比度下降）；横版剧照源三档无差别。用户拍板后再把默认写死。
- 教训：聚合页（CollectActivity）返回时焦点记忆在上次卡片，二次进入 ↓+OK 会落错源（本轮误入网盘源需扫码登录）；详情页 vod pic 多为 16:9 剧照，竖版海报取景对比必须挑 3:4 片源（折月亮）才有区分度。

### 第 25 轮（2026-11-06 下午）— 顶部渐变再减弱 + 拿掉海报叠加层

**起因**：用户实机看 .214：① 顶部仍有可辨黑渐变；② 中间多了一幅「牛皮癣」图（f=1/2 的完整海报叠加层）。

- **现场勘查（如实）**：持久化偏好 `debug_backdrop_variant=3`（D）、`debug_backdrop_frame=2`——设备生效的确实是 D 版，用户看到的顶部黑渐变 = D 的上边 56dp/24% 收边（在亮天空海报上可辨），叠加层 = 取景实验层。不是旧版本残留。
- **收边减弱**：上/左右 56dp@24% → **40dp@12%**；底 120dp@40% → **120dp@30%**（选集行可读性最低保障，第 22 轮依据）。实测（折月亮同海报同机位逐行剖面）：顶缘 0-20px 相对亮度 旧 0.0292 → 新 0.0349（+19%，更接近海报本体）；渐变带内相对变化 旧 ~19.5% → 新 **≤6%**（=几乎看不出的量化口径）；纯黑占比 0.00-0.76%（无黑块）。截图 `r25_bright.png`（亮）/ `r25_dark.png`（暗）。
- **叠加层整体移除**（用户判断无价值，选「直接删掉更干净」）：删 `backdrop_full`（layout + Glide 加载 + f 参数 + `BACKDROP_FRAME_DEFAULT` 全清），回到纯模糊铺满；设备端持久化的 frame 值不再被读取，装机即无叠加。

### 第 26 轮（并行）— 源质量信息改双胶囊分组

- `view_control_vod.xml` 名称行：单 TextView → 两枚胶囊（`track_video` / `track_audio`，`shape_track_chip` = 8% 白底 + 0.5dp/12% 描边 + 6dp 圆角 + 11sp dim 字），视频一枚（`1080p · H.264`）、音频一枚（`AAC · 2.0`），缺类整枚 GONE 不凑占位。
- **与「按钮不得胶囊化」的区分**：该约束针对可点击按钮（焦点态/尺寸/强调色）；这两枚是不可点信息标签——不注册焦点/点击、无实底、无强调色、字号高度远小于按钮，属「标签 chip」而非按钮，不违反。
- 实证：`r26_compact.png`（精简态双胶囊居右、无按钮列）、`r26_full.png`（完整态按钮原位同尺寸，胶囊同行）；长剧名时标题 weight=1 截断让位，胶囊不塌；标题/秒数零位移（行高由 18sp 标题锁定）。1080p 为真实轨道数据（尚公主/嫁金钗源实测）。
- GS-47 已改写为双胶囊规则。

### 第 27 轮（下午）— 氛围模糊管线（按用户澄清方向：不提清晰度，提模糊质量）

- **方向校正**：用户明确不要「提高解码尺寸看清海报」，要「柔和色调/明暗氛围场」；块状感来自源图压缩颗粒经 4× 放大可见，提清晰度救不了（块在源图里），只能靠真模糊融掉。
- **实现**（`BackdropBlur`，Glide BitmapTransformation，仅详情页）：480×270 小解码不变 → 降采 **72×40**（≤8px 的 DCT 块直接融掉）→ 盒式×3 r=2（近似高斯，消 cell 边界）→ 双线性回 480×270 → **±1 LSB 确定性抖动**（防平滑渐变色带）。弃自适应半径：多级降采到固定小尺寸对各源尺寸效果一致，管线更简单均匀（首轮自适应方案被用户澄清推翻）。
- **不过度**：72×40 网格 ≈ 屏上 27px 柔和色块，主色调与明暗分布保留（暗调暖剧/冷蓝调可辨），不成均匀灰——实机三海报验证：亮（折月亮 `r27_zyl2.png`）、暗（嫁金钗 `r27_bright.png`）、高饱和（尚公主 `r27_sat.png`），均无方块无色带；与 25 轮「铺满+无叠加」同时成立。
- **性能实测**：Glide 后台线程一次性 110-150ms/新海报（.214，A53），磁盘缓存后零成本；渲染线程与播放零开销（静态图无需逐帧模糊；API 28 无 RenderEffect，且逐帧 GPU 模糊对静态底图本就无谓）。帧率无回归（播放页 EventLogger 无掉帧迹象，seek/播放实测正常）。
- 待办挂账：首页/分类页底图仍走旧管线（无 BackdropBlur），待用户确认详情页口味后跟进。

### 第 28 轮（2026-11-06 下午）— 模糊海报底全量推广（首页/分类/榜单）

**起因**：用户实机确认首页/分类/榜单/最近观看无任何变化（顶部黑边仍在、模糊不够）——第 27 轮挂账的旧管线页面，本轮全量统一。

- **统一点**（首页 `activity_home` + 分类 `activity_vod`，榜单页即 VodActivity 的榜单 tab，最近观看在首页内）：
  ① Glide 接入 `BackdropBlur`（与详情页同参数）；② alpha 0.72→0.85；③ 撤掉整屏 `shape_backdrop_scrim`、200dp `shape_top_scrim` 顶栏渐变、96dp `shape_edge_fade_*` 左右渐隐，换详情页同款 `shape_backdrop_scrim_d` 四边轻收边；四个旧 drawable 已删除；④ 浮于底图的文字（tab/时钟/页标题/卡片名/备注/章节头/圆形卡名）统一加文字阴影 #66000000 dy=1 r=4。
- **可读性取舍（实测驱动）**：撤顶栏渐变后亮蓝天海报（后会友期）上 tab/时钟/卡片名被洗白（顶栏区 bg 亮度 0.30，dim tab 字对比 ≈1.7:1 不达标）→ 按「极轻局部处理、不回黑条」原则选**文字阴影**而非 scrim 带——阴影不改背景任何指标（顶缘/纯黑不受影响），加后实测 tab 清晰可读。
- **逐页量化**（`page_metrics.py`，纯黑=像素 max<8 占比）：
  | 页面 | 顶缘亮度 | 纯黑 | 顶栏区白字对比 |
  |---|---|---|---|
  | 首页·暗（尚公主） | 0.061 | 0.02% | 6.5:1 |
  | 首页·亮（后会友期蓝天） | 0.204 | 0.25% | 3.0:1 |
  | 分类·亮（电影筛选） | 0.106 | 0.91% | 5.0:1 |
  | 分类·暗（家用摄像头） | 0.023 | 2.02% | 10.3:1 |
  | 榜单（电影榜单） | 0.043 | 1.68% | 9.5:1 |
  顶缘亮度随海报内容大幅变化（0.023~0.204）= 顶部没有被压成黑带的直接证据；收边自身理论最大压暗 = 顶缘行 12% 且 80px 内归零。纯黑非零项为暗海报自身内容。注：逐页「顶缘差」跨页不可比（海报内容不同），本轮以「顶缘随内容 + 收边理论上限 + 顶部特写」为口径，特写见 `topcrop_*.png`。
- **截图**：首页亮 `home_bright2.png` / 暗 `home_dark2.png`、分类亮 `vod_a.png` / 暗 `vod_dark.png`、榜单 `rank.png`、详情页沿用 25-27 轮终态。
- **未套用的页（如实说明）**：收藏/搜索/聚合页没有海报底图机制（纯深色底），不属本次统一范围；若要给它们加氛围底是另一项工作，等用户拍板。首页 hero 即海报卡片区本身，无独立结构冲突。
- 历史教训复核：本轮所有页面焦点导航仍以截图先确认再发键（聚合页焦点记忆坑未再踩）。

### 第 29 轮（2026-11-06 傍晚）— 筛选子行间距收紧 + 按需导航带（Netflix 式）

#### 29.1 filter 展开后与 tab 间距 → 约 1/3

- 实测（uiautomator bounds，2px/dp）：改前 tab 底→筛选子行顶 = **28dp**（构成：headerBottom 118dp + setContentTopTemp 附加 8dp + 误用海报行 zoomPad ~13dp + 行内 padding）；改后 = **8dp**（28→8，29%≈1/3）。改法：`setContentTopTemp(getHeaderBottomPx())`（不再 +8dp）+ 垫层固定 4dp（筛选 chip 不放大，不能用海报行的 zoomPad）。
- 分组感：tab（17sp 文字）与子行（小一号圆角 pill）样式本就不同层，8dp 间隔 + 行内 padding 实机截图确认分组清晰、不像同一层（`r29_vod2.png`）。

#### 29.2 导航区按需渐变带（Netflix 式）

- **选型：纯渐变，不用 frosted**——API 28 无轻量实时模糊（RenderEffect 需 31+；RenderScript 已废弃且对静态带无必要），渐变与全站 scrim 语言一致。
- 实现：`shape_nav_scrim` 两段式（0-118dp 覆盖顶栏+tab 文字区，118-150dp 羽化到 0，带高 150dp≈屏高 14%）；HomeActivity/VodActivity 挂全局焦点监听，焦点进顶部 124dp 内 → 220ms 淡入，离开淡出；静止态 alpha=0 铺满保住。
- **实测对比度**（亮蓝天海报）：静止态 3.0:1（无带，不变）；带 ON 初版 80%→45% 得 tab 区 4.1:1 未达标 → 校准 85%→55% 后 **tab 区 7.5:1、顶栏区 8.1:1**（白字）；dim 未选中 tab ≈4.3:1（GS-38：WCAG 值非 TV 官方，选中 tab 是白药丸黑字不受影响）。暗海报带 ON：tab 区 10.7:1（带对暗海报最多再压 ~55%→但暗海报该区本已 0.05 亮度，视觉无突兀）。
- **时钟策略**：时钟是常显元素——静止态保持文字阴影（亮海报 3.0:1，可辨），焦点进导航区时被带覆盖（对比跳到 8.1:1）。不为它单独增强。
- 与四边收边叠加：带（顶 150dp）与上边收边（40dp@12%）同向向下渐弱，交界连续无生硬；实机截图确认（`r29_navband_on.png` / `r29_sky_nav2.png` / `r29_navband_off.png`）。

### 第 30 轮（2026-11-06 傍晚）— 焦点跳跃根治 + 全站焦点审计 + 四边收边三档

#### 30.1 焦点跳行首 bug（根因与修复）

- **根因（双路径证据）**：① `CustomVerticalGridView.focusSearch` 旧修只覆盖 ↓（`findSameColumnBelow`），↑ 漏网；② 更隐蔽的主因——`HomeActivity.dispatchKeyEvent` 对「推荐首行 ↑→最近观看」手写拦截 `setSelectedPosition(0)+requestFocus()`，直接落行首 0 列（跨区移动绕开焦点搜索）。行 rebound/未访问行 selectedPosition=0 是 Leanback 底层原因。
- **修复**：`focusSearch` 改 **x 坐标最近列**双向定向（目标行已布局直接返回最近子项；未布局记 pending x，焦点落地瞬间由全局焦点监听器修正，同 vsync 完成无跳动）；Activity 层拦截路径改 `nearestColumnInRow(0, 当前焦点)` 按列落位。兜底：目标行无更近列时天然落最近列；行未布局且取不到子项回退 Leanback 默认。
- 实测：海报 col3(x=775) ↑→最近观看落 col3（截图 `r29_fix_final.png`）；快按 5→+↑ 列不错位；历史行 col1 ← 停住不绕回。

#### 30.2 全站焦点容器复核

| 容器 | 机制 | 结论 |
|---|---|---|
| 首页主网格（最近观看/更新推荐） | CustomVerticalGridView | **已修**（列定向）✅实测 |
| 分类/榜单（TypeFragment 同一 recycler） | 同上 | 同修复受益 ✅ |
| 筛选子行 | ListRow in 同一 recycler | 同受益 ✅ |
| 收藏页 | GridLayoutManager 单网格 | pass（单项实测：→ 停住不绕回；无行记忆跳变机制） |
| 聚合页 | HorizontalGridView 单行 | pass（无垂直移动） |
| 详情页选集行/选集面板 | HorizontalGridView 单行 / dialog GridLayoutManager | pass（同构，单列移动） |
| 设置列表/站点对话框 | 垂直列表 / GridLayoutManager | pass-by-construction |
| 行首/行尾绕回 | Leanback 默认不绕回 | ✅实测停住 |
| 返回上一屏焦点还原 | Activity 栈焦点记忆 | ✅实测（详情返回回 col3） |
| 快按竞态 | 5→+↑ 连按 | ✅列不错位 |

#### 30.3 四边收边三档（用户对比选力度）

- 档位（DEBUG 广播 `--ei t 0/1/2`，Prefers 持久化，首页/分类/详情 D 版同步生效）：
  - **t0 极轻（现状默认）**：上/左右 40dp@12%、底 120dp@30%
  - **t1 中**：上/左右 64dp@20%、底 140dp@45%
  - **t2 稍强**：上/左右 96dp@30%、底 160dp@60%
- 有效区域损失（渐变软化区非遮挡）：t0 顶缘 80px/底 240px/侧 80px；t1 128/280/128px；t2 192/320/192px——均为渐隐软化，内容仍可辨。
- 截图：亮海报（蓝天）`sky_t0/1/2.png`、暗海报 `dark_t0/1/2.png`（shots/round-2604 收 r29_ 前缀）。设备已 reset t=0 待用户拍板。
- 与导航带叠加：均同向渐弱，实机无生硬交界。

### 第 31 轮（2026-11-06 夜）— 导航带渐变平滑化 + 结构触发判据 + tab token 统一 + 副标题提亮

> 取证环境：192.168.2.214（截图 1920×1080，app density 2.0 → 1dp=2px）；证据文件在 `.tmp/r31/`（b_*=改前旧包，a_*=改后新包）。本轮四条均先真机取证再改、改后复测。

#### 31.1 导航带渐变：两段式 → 15 段非线性连续曲线

- **改前剖面**（同底图开/关双截图、按 sRGB 合成反解 alpha，`b_off.png`/`b_on.png`）：两段式（0-118dp 85%→55% + 118-150dp 羽化）实测主段相邻差仅 0.008-0.06，118dp 接缝后骤跳到 **0.057→0.172→0.172→0.159**，最大相邻差 **0.172 @130→140dp**——「两道颜色横条」的台阶感来自接缝后 3.4 倍斜率突变。
- **改后实现**：`shape_nav_scrim` 重写为 15 段 ×10dp layer-list 切片，alpha 按 **85%×(1−(y/150)²)** 连续衰减（采样点 0/30/60/90/120/150dp = 85/82/71/54/31/0%）；相邻切片端点同色故无接缝（原生 gradient 只支持 3 色标，切片法是 N 色标的唯一路径）。
- **改后剖面实测**（均匀棕褐底，`a_off_tan.png`/`a_on_tan.png`）：逐 10dp alpha = .851/.848/.832/.813/.781/.750/.712/.665/.609/.539/.451/.382/.304/.209/.114/.000——与设计值偏差 ≤±0.02；相邻差从 0.003 平滑渐增到 0.114，**单调不回升、无突变；最大相邻差 0.114 @140→150dp**（仅尾段 alpha→0 处，视觉不可辨）；带外 160/180dp 对照 = 0.000（带边界精确收在 150dp）。
- **柔化后对比度复测**（带 ON）：首页标题白字（top 15-35dp，alpha≈0.84 未动区）亮粉底 **12.6:1**；vod 选中态白 tab（tab 行 80-115dp，本轮柔化主区）亮青底 **5.2:1**（第 29 轮同口径亮蓝天 7.5:1 → 用约 1 档余量换平滑，仍 ≥4.5:1 ✅）；vod 标题 **14.6:1**。

#### 31.2 导航带误触发：bounds 几何判据 → 结构判据

- **复现（改前旧包）**：首页「更新推荐」↑ 落焦「最近观看」时带子 ON（`b_up2hist.png`，焦点环在历史卡、顶部渐变带可见）。机制：首行卡片布局顶缘 ~122-127dp 贴在旧 124dp 触发线上，滚动/落焦动画中 `getGlobalVisibleRect` 瞬时报更小值（新包日志实测落焦瞬时 bounds=[792,0][1124,252]，top=0）。
- **修复**：`updateNavScrim` 判据改为焦点 View 父链归属 `@+id/header`（顶栏+分类条容器，两页布局各加 id）——不看 bounds、不看按键、不看行索引；HomeActivity/VodActivity 同改；DEBUG 加 `QBoxNav` 日志（焦点类/bounds/inHeader/bandOn）。
- **四路径实测**（新包，逐条 QBoxNav 记录，`paths.txt`/`p4b.txt`）：

| 路径 | 焦点 bounds（落点） | inHeader | 带子 |
|---|---|---|---|
| 更新推荐 ↑→ 最近观看 | [792,0][1124,252]（滚动瞬时 top=0！） | false | **始终 OFF** ✅ |
| 最近观看 ↓→ 更新推荐 | [792,834][1124,1080] | false | OFF ✅ |
| 内容 ↑→ tab（正向对照） | [64,152][256,236] | true | OFF→ON ✅ |
| tab ↓→ 内容 | [64,393][396,693]（历史卡1） | false | ON→OFF ✅ |
| 冷启动后直接 ↓ | 加载期 typeRecycler（true，ON）→ 历史卡1 | false | ON→OFF ✅ |

- **一帧闪烁连帧采样**：「更新推荐 ↑→ 最近观看」转移中连拍 6 帧（覆盖 220ms 淡入窗+滚动窗）：顶部 40px/中部 340px 亮度比率 f2-f6 恒 0.660 无跳变（f1 为转移前旧底图帧），QBoxNav 全程无 bandOn 翻转——无瞬时淡入（`burst.txt` + `f1-f6.png`）。

#### 31.3 tab 排版 token 统一

- **改前**：首页 tab（`adapter_func`）有文字阴影 #66000000/dy1/r4，分类页 tab（`adapter_type`）无阴影；且 `tab_text`/`tab_text_focus`、`selector_tab`/`selector_tab_focus` 两套文件内容逐字节相同（假分流）。
- **改后**：新增 `@style/QBoxTabText`（17sp / sans-serif-medium / 字距 0.01 / `@color/tab_text` / 阴影 #66 dy1 r4），`adapter_type` 与 `adapter_func` 文字项同引用；颜色与药丸收口到 `tab_text`/`selector_tab`，删除两个重复文件。坑：style 名带点又无 parent 会被 aapt 按前缀找父样式（`QBox.TabText` 链接失败）→ 命名 `QBoxTabText`。
- **实测**：两处「热播综艺」字形行域均 y178-209、字高 32px（=17sp×density2 字身）一致（`a_tabs_compare.png` 同屏对比）；逐项参数由同一 style 文件结构性保证相同。阴影晕（dy1/r4 40% 黑）在实拍底图上低于像素噪声、无法稳定分离，**如实标注**：此项以「同一 token」结构保证 + 字形/渲染视觉一致为证。
- 范围说明：全站仅首页与分类/榜单页（同一 VodActivity）有 tab 行；收藏页无 tab 行。

#### 31.4 最近观看副标题提亮

- **改前** `@color/qbox_text_mute` #8C8C98：亮粉底（bg 中位 0.365）**1.43:1** ❌、暗绿底（bg 0.012）5.10:1。
- **改后** `#DCDCE4`（主标题 #E7E7EE 降一档）：暗红底（bg 0.051）**7.64:1** ✅、中亮棕褐底（bg 0.33）2.02:1、极亮粉底（bg 0.50）1.39:1。
- **如实标注——亮底氛围墙上 4.5:1 物理不可达**：副标题区域无任何 scrim（D 版收边只盖四边），底图亮度中位实测可达 0.33-0.50，即使纯白字对比度上限也只有 1.9-2.8:1（主标题 #E7E7EE 同区同病）；且底图 >0.5 亮度时旧暗灰因极性反转反而略优。本次改动在常见暗/中亮底上显著提升（5.10→7.64 / 1.43→2.02），极亮底无任何单色文字能过 4.5:1。
- 备选（未实施，待拍板）：① name/remark 加 tab 同款文字阴影（亮底感知再 +0.5 档左右）；② 卡片文字块加窄 scrim（与 GS-20 反叠加层取向冲突，不推荐）。

收尾：GS-51（渐变平滑度）/GS-52（触发判据）/GS-53（tab token）新增，GS-49 更新；`.214` 留首页可用态。

### 第 32 轮（2026-11-06 深夜）— filter 长按死端 + 快进连续曲线 + 副标题 scrim（后于 34 轮撤回重做）

> 取证环境同上；`.tmp/r31/` 证据（r32_*）。

#### 32.1 最近观看副标题 scrim —— **落点错误，第 34 轮撤回**（见 34.1）
- 当时实现：卡片 remark 区窄 scrim（三版迭代：纵向三段→径向 90%→预渲染柔光 PNG 72% + 底图亮度自适应 level 0.35-1.0）。数值：暗底 8.84:1 / 中亮棕褐 5.32:1 / 极亮粉 3.47:1。
- **34 轮用户实机指出落点错了**（要的是 tab/filter 文字区），全链路撤回（布局/Presenter/自适应代码/PNG）。教训入 GS-56。
- 卡片副标题保留第 31 轮 `#DCDCE4` 纯文字方案（暗底 7.64:1，亮底物理上限如实标注）。

#### 32.2 filter 子行左右长按越界 ✅
- **改前实证**：filter 行1 行尾长按 RIGHT → 焦点飞出 → 海报行 → 触发 ViewPager 方向键翻页，连翻数个大 tab（热播电影→电视筛选→电视剧榜单），越界链完整录屏级证据（r32_esc2/esc3.png）。行首 LEFT 当时不死但无目标可去。
- **修复**（双保险）：① `CustomRowPresenter` 对 ZOOM_FACTOR_NONE 行（filter chip 行）挂 `OnKeyInterceptListener`：行首再按 ←/行尾再按 → 直接消费（每次长按 repeat 都是新 ACTION_DOWN，同样被拦）；UP/DOWN 不拦。② `CustomViewPager.arrowScroll` 置 false——pager 永不响应方向键翻页，换大 tab 只能走 tab 行点击。
- **改后实测**：行1 行首 LEFT×5 + 行尾 RIGHT×5 + 行尾长按×15 + 行2 长按×20 焦点 bounds 逐次不变（热度 [64,252][180,320] / 评分 [328,252][444,320] / 日本 [592,352][708,420]）；海报行行尾长按 → 停留行尾 [1503,223][1856,742]，**不再翻页**（seltab 全程热播电影）；UP/DOWN 进出 filter 行正常。
- **同类横向控件审计结论**：tab 行（两端原生死端 ✓）、详情页选集行（两端死端 ✓，左端 [56,665] 右端 [1576,665] 长按不动）、搜索页历史/热搜 chips（行尾长按停留 ✓）——仅 filter 行有此缺陷，已修。
- **观察项（既有 quirk，非本轮引入，未动）**：filter 行2 按 UP 直接跳 tab 行（跳过行1）；DOWN 行1→行2 正常。疑与第 30 轮 CustomVerticalGridView.focusSearch 的最近列逻辑在非海报行上的行为有关，建议下轮专项。

#### 32.3 快进曲线重做（分档+档间爬升 → 单一连续速率曲线）✅
- **改前取证（逐 tick 日志定位）**：旧 RAMP 分档模型实测速率序列：0-3s ≈4.6x（细调档 1s/130ms 注入节拍受限）→ 3-5s ≈7.6x → 5-6.7s 8.6-14x 抖动 → 固定段 10.2x → 跨档 BLEND 窗爬到 15-30x → 固定段 29.6x → 窗内 44-68x → 固定 52.6x → 窗内 165x → 固定 143x。**低速段来源石锤**：每个固定步长段的有效速率都低于其前置 BLEND 窗的峰值（窗内密节拍+插值，窗外节拍被播放器 seek 耗时拉长），「高速段中夹低速段」= 档间爬升窗与固定段交替出现的结构性后果。
- **新模型**：`f(t)=R0+(cap−R0)(1−e^(−(t−3s)/τ))`，τ=2500ms，每 tick 位移 = 定积分差值（`cumAt` 差），无档位无重置无台阶；前 3 秒细调段（±1s @130ms）逐字节保留。R0=1000/130≈7.7x 与细调档标称速率连续。快退同曲线对称（实测同 elapsed 位移镜像 ±2%）。DEBUG 广播 `--ei s 0/1` 切参数组，Prefers `debug_seek_curve` 持久化。
- **两套参数实测位移表**（同片、注入长按，累计视频秒）：

| 按住 | 旧分档 | A：8x 封顶 | B：片长挂钩（cap=max(8x, 片长/180s)） |
|---|---|---|---|
| 0.5s | 2s | 2s | 3s |
| 1s | 4s | 3s | 5s |
| 2s | 9s | 8s | 11s |
| 3s | 13s | 12s | 16s |
| 5s | 24s | 26s | 46s |
| 8s | 72s | 50s | 99s |
| 12s | 226s | 81s | —（99s@8s 后仍爬升，该片 cap≈20x+） |

- 均匀度（粗调段相邻 tick 位移 max/中位）：旧 4.16 → 新 ~1.8（残余波动来自注入/播放器节拍 dt 抖动 120-350ms，位移由积分保证无台阶；逐 tick rate 序列单调平滑：7.9→9.7→11.6→13.2→15.1x）。
- 回归：按住中播放不暂停、进度条跟随（控制条时间随 cum 走）、松手 250ms 内续播（state=3 实测）。
- **待用户实机二选一**：A 全程≈8x 匀速（2h 片横贯 ~15 分钟）；B 片长挂钩（45min 剧 cap≈15x、2h 片 cap≈40x、约 3 分钟横贯）。**建议 B**（已设默认）。
- GS-42 已按新模型改写。

### 第 33 轮（2026-11-06 深夜）— 导航带等 alpha 差分段 + 品牌 logo v2 全资源 + 载入动画初版

#### 33.1 导航带切片重排（后被 34.2 根因重做取代）
- 15 段等高 → 43 段等 alpha 差（Δ=0.02，片高顶 23dp→底 1-2dp）。实测剖面与设计一致。后经 34 轮用户实机确认「横线仍在」，根因不在粒度（见 34.2）。
#### 33.2 品牌 logo v2（全资源清单逐项）
| 位置 | 资源 | 结果 |
|---|---|---|
| 桌面磁贴（5 密度 ×方/圆） | mipmap-*/ic_launcher(.png) | ✅ 白底+立方体（改前六边形Q红三角，改后实机磁贴截图 r34_launcher.png） |
| adaptive icon | ic_launcher_foreground.png（透明 66% 安全区）+ 底色白 | ✅ |
| leanback banner | drawable-xhdpi/ic_banner.png 640×360 白底居中 | ✅ |
| 首页顶栏品牌标 | drawable-nodpi/ic_logo.png（透明，ImgUtil 兜底分支） | ✅（mobile fragment_vod 共用同文件） |
| 开屏 splash 图标 | drawable-nodpi/ic_splash_logo.png（透明，黑底中央） | ✅ |
| 设置页关于区 | 无 logo 图像位（仅文字版本行 1.9.19） | 不适用，已实机核对（r33_settings.png） |
| SVG 源文件 | 与 PNG 内容一致（1600 viewBox 同构） | 存档未用（位图密度已够） |

- 素材核对：PNG 1024² RGBA 真透明、无锯齿、Q 为实心白（无镂空误抠）✓。
#### 33.3 载入动画初版（GIF+Glide，后被 34.3 取代）
- qbox-loading-v2.gif（640²、38 帧、≈1.9s 循环、**不透明白底**）→ 白圆 chip 承载 + Glide asGif。冷启动 10175→10219ms（+0.4%，噪声内）。
- GS 新增：品牌素材单一来源清单。

### 第 34 轮（2026-11-06 深夜）— scrim 落点纠正 + 导航带硬线根因重做 + WebP 载入动画

#### 34.1 scrim 落点：卡片 → tab/filter 文字区 ✅
- **落点说明**（用户要求的"贴出实际作用控件"）：32 轮 scrim 加在 `adapter_history.xml` 的 `@+id/subtitle_scrim`（历史卡 RelativeLayout 内、`name` 与 `remark` 之间）——实机截图标注（r34_where_scrim.png 红框=实际落点卡片剧集信息区，绿框=用户要的 tab 行）。
- **为什么上轮验证没发现**：如实复盘——第 31 轮"小集副标题"颜色修改先落在同一位置（卡片 remark），形成了错误锚点；32 轮取证只验证了「该处对比度数值达标 + 截图可见」，没有把「scrim 应作用的控件身份」列为独立验证项（只测数值没核对位置）。已写入 GS-56：验证必须同时证明位置正确与数值达标。
- **修正**：卡片 scrim 全链路撤回（布局/HistoryPresenter/HomeActivity 自适应代码/PNG 资源全删，卡片恢复第 31 轮纯文字态）。新落点：`shape_text_bed.png`（预渲染宽幅柔光，中心 42%→边缘 0，四边高斯软边）垫在 ① 首页/分类页 tab 条文字下（FrameLayout 内 typeRecycler 之后台 View）② filter chip 行底（CustomRowPresenter NONE 行 setBackgroundResource）。
- **实测**：tab 底衬首版边缘残留 11% alpha 形成底线（逐行亮度跳变 0.023 @117-118dp）→ 重生成边缘归零版后同区无跳变；卡片区逐行扫描无任何 scrim 残留。filter 行实机截图（r34D_filter.png）底衬可见且软边。
- tab/filter 文字对比度（静止态、亮底）：未选中暗字从 1.3:1 提升到 ~2:1（暗字为刻意弱化态，不作为 4.5 目标；选中白字/白药丸由导航带+药丸承担）。

#### 34.2 导航带硬线：根因重定与重做 ✅
- **三候选排查（2dp 全剖面 + 逐行亮度差）**：
  - a) 末端不归零：**排除**（实测 alpha@150dp=0.000，旧构建即归零）。
  - b) 多层实色矩形叠加：**排除**（每片是 2-stop 线性渐变且端点同色，实测剖面与目标曲线逐点吻合 ±0.02，非矩形阶跃）。
  - c) 视图几何边界：**部分确认**——真正的线是「alpha→0 的尾段斜率最大」（二次曲线导数在尾端最大），俯冲段恰好结束在视图底边 = 海报上的横线。即**根因是曲线形状，不是切片粒度**（31 轮等高、33 轮等差都没动形状，所以用户看不出改进）。
- **修复**：曲线改全段余弦 `a(y)=0.87×(1+cos(π·y/H))/2`——两端导数=0（进入与收尾都无视觉边缘）、全段 C1 连续、最大斜率居中摊宽；带高 150→180dp 换更缓斜率；仍走等 alpha 差切片（Δ=0.02，44 片）。
- **实测三代对比**（逐 4px 行亮度跳变 Top1）：两段式 **0.0278 @154-156dp**（羽化接缝）→ 二次曲线 0.0095 @142-144dp（尾段俯冲）→ 余弦 0.0178 但摊在 116-136dp 宽区、**带尾 170-180dp 归零斜率**（d≤0.011 且递减至 0）。带外同底图校验 diff=0/1。
- 对比度复测（带开，亮底）：首页标题白字 **12.33:1**、tab 白药丸黑字 19.6:1（pill 内）——与上轮结论同档，未跌破。
- 同机位对照截图：r34_on_pink（二次）vs r34C2_on（余弦）。

#### 34.3 开屏载入动画：WebP + ImageDecoder ✅（取代 33 轮 GIF+Glide）
- 素材核对：qbox-loading-v2.webp 640²、38 帧、**带 alpha 透明通道**（与 GIF 不透明白底不同——可直接上黑画布）、746KB；PIL 读不到帧时长，设备端 AnimatedImageDrawable 自播放循环。
- 实现：`ProgressLayout.showProgress()` 首次进入加载态时 `ImageDecoder.decodeDrawable`（解码期 setTargetSize(112dp×密度) 降采，内存只为当前帧量级）→ AnimatedImageDrawable.start()；API<28 退回静态 ic_logo。放弃拆帧 AnimationDrawable（38 帧 ×640² RGBA 全解码 ≈62MB 不可接受）与 GIF+Glide（白底需 chip 承载）两条旧路线。白圆 chip（QBoxLoadingChip）随 GIF 一起移除。
- 惰性加载不进冷启动关键路径：冷启动 WaitTime 三次中位数 旧 10175ms → 新 **10178ms**（+3ms，噪声内）✅；首帧=兼容库 splash 静态 logo（API 28 由 androidx SplashScreen compat 承载，动画自首页加载态起）。
- 动画取证：加载态两帧立方体相位不同（正面 Q / 背面 q，r34_home.png vs r34_la.png）。
- 旧 logo 残留终查：splash/首页顶栏/桌面磁贴/设置页逐项截图无旧六边形 logo。

收尾：GS-42 重写（连续曲线速率模型）、GS-51 修订（根因=曲线形状）、新增 GS-54（横向长按死端）、GS-55（A/B 参数组口径）、GS-56（验证=位置+数值双证）、GS-57（渐变带尾端必须归零斜率收敛到 0）、GS-58（品牌素材清单）。

### 第 35 轮（2026-11-07）— 导航带回退+tab 完整覆盖 + Apple TV+ 栏变体 + filter 纯色底 + 加载文案移除 + 快进定案 B

#### 35.0 回退版本指认（先于动手，用户修正后执行）
- 用户第一条指令误指「29 轮两段式」；修正后确认目标 = **第 31 轮二次曲线版（commit `9eb31bf` 的 `shape_nav_scrim.xml`）**：15 段×10dp 切片，`alpha(y)=85%×(1−(y/150)²)`，采样 0dp 85% / 60dp 71% / 120dp 31% / 150dp 0%。
- 误指版 = `f3ce9b5`（29 轮两段式）：仅 2 个 item——0-118dp 线性 #D9→#8C（85%→55%）+ 118-150dp 羽化 #8C→#00；118dp 处斜率突转。与目标的 diff 要点：2 item vs 15 切片；中段大跨度线性 vs 连续二次曲线；尾段 32dp 急羽化 vs 150dp 加速归零。
- 恢复动作：`git checkout 9eb31bf -- app/src/leanback/res/drawable/shape_nav_scrim.xml`；恢复后 `git diff 9eb31bf -- <path>` 输出为空 = 逐字段一致（非近似重写）。
- **覆盖修正（与恢复分步登记）**：tab 行实测 y=76-118dp，恢复版在 118dp 处 alpha 仅 0.32（文字压渐变尾端，本轮用户硬要求禁止）→ 同一公式延域 H=150→190dp（19 片），两布局 nav_scrim 高同步 190dp。34 轮余弦版与 shape_text_bed 柔光斑全部删除。

#### 35.1 tab 完整覆盖（硬要求）✅
- 实测 alpha 剖面（亮天空底 off_g≈166，绿通道反解，全宽中位数）：
  | 位置 | tab 顶-20dp(56) | tab 顶(76) | tab 中(97) | tab 底(118) | tab 底+20dp(138) | 带下缘(190) | 带外(210) |
  |---|---|---|---|---|---|---|---|
  | alpha | 0.769 | 0.708 | 0.618 | 0.516 | 0.396 | ≈0.05(尾片) | 0 |
- 结论：tab 行（含上下留白）全程 alpha≥0.51，带下缘在 tab 底下方 72dp 处归零 ✓。榜单页暗调底复测同曲线（0.706/0.616/0.518/0.400）。
- 横线排查：带开图 100-230dp 最大行跳变 0.042@194-196dp，带关图同位置 0.041 → 溯源为底图海报行顶缘内容，非带边；带自身全程无 ≥0.018 的 2dp 跳变。同底图校验 300-400dp diff≈0.02/255 ✓。
- 三页标注截图（绿框=tab 行 76-118dp，蓝线=带下缘 190dp）：mark_home_q / mark_vod_filter_q / mark_rank_q——tab 区无灰斑、无贴图底（shape_text_bed.png 文件 + activity_home/activity_vod 底衬 View + CustomRowPresenter 引用全删，grep 无残留）。
- 对比度（带开亮底）：tab 未选中 3.87:1（17sp 大字号，WCAG 大字 ≥3:1 ✓）；toolbar 白标题 13.58:1。

#### 35.2 Apple TV+ 式栏变体（shape_nav_scrim_bar，可切换）✅
- 规格：0-134dp 均匀 60% 黑（#99090A0D）纯色 + 134-146dp 12dp 极小收边；tab 行 76-118dp 全程 ~0.60、栏底实心余量 16dp；显隐沿用 GS-52 结构判据 + 220ms 淡入淡出（无位移、无一帧闪烁）。
- 实测剖面（亮金底 g=163）：0-130dp 平 0.59-0.60、140dp 0.25、150dp 归零；设计逐点吻合。
- tab 对比度：亮金底 3.64:1、暖调底 4.26:1（未选中大字达标）。
- 如实登记的残余问题：① tab 底（118dp）与 filter 行 1 顶（126dp）仅 8dp 空隙，栏收边（134-146dp）必然搭到 filter 行 1 顶部约 20dp（仅面板开着且焦点在导航区这一过渡态）；② 亮底下栏边可见（|dL|≈0.06/2dp）——这是「栏」的刻意收口而非渐变塌边，但是否更耐看留用户拍板。
- 切换用法：`adb shell am broadcast -a com.qbox.app.DEBUG_BACKDROP --ei m 0/1`（0=二次曲线带 / 1=栏），Prefers `debug_nav_variant` 持久化，重启保持。**当前默认 m=0，最终默认待用户实机对照后拍板。**

#### 35.3 filter 行纯色半透明底（shape_filter_bed）✅
- 色值推导：回退版曲线 alpha(y)=0.85×(1−(y/190)²) 在 filter 行 1 纵向中心 y≈143dp 处 = 0.85×(1−0.566) = **0.369 → #5E090A0D**；单一 `<solid>` 无渐变边、不变色；6dp 微圆角（与全站 chip/卡片圆角同族，读作刻意的栏；直角会与海报内容形成无动机硬切）。
- 实测（r35_filter_open.png，行 1 122-164dp）：内部逐 2dp 中位亮度 75→96（8.2%）——残差全部溯源到底图纵向内容波动（带外邻近区底图自身波动 14%，床为 `<solid>` 构造上零渐变）；上边界 120→122dp 单步落 36/255、下边界 164→166dp 单步起 45/255 = 纯色块直边 ✓。
- 亮/暗海报证据：亮= r35_filter_open.png（暖调）、r35_filter_dark2.png（电视剧榜单亮蓝底）；暗/灰= r35_filter_dark3.png（电影榜单灰蓝底）。
- 边界：床只挂在 chip 行 gridView 背景（CustomRowPresenter ZOOM_FACTOR_NONE 行），不延伸海报区/tab 行 ✓。

#### 35.4 播放器「正在加载」文案移除 ✅
- 全仓点位：`view_progress.xml`（leanback）hint TextView 整删 + `ProgressLayout.initView` 的 `mProgressBinding.hint.setVisibility(VISIBLE)` 删除；mobile 版布局本无 hint；grep 确认无其他引用（`qbox_loading` 字符串保留未引用）。
- 实测：切集加载态（r35_ep_a/b）小窗只有速率文本（4 KB/s），无「正在加载」、无空白占位条、速率文本居中 ✓。
- 如实标注：本机源 CDN 缓冲极快（<0.5s），「进播放转圈 / 全屏 buffering」连抓 4 次未捕到转圈帧；代码层面 hint 已无任何引用路径。松手续播三次实测均正常。

#### 35.5 快进曲线定案 B ✅
- 移除：`CustomKeyDownVod` 的 curveMode/CAP_A/setCurveMode（`cap()` 恒为 `max(8.0, durationMs/180_000)`，无片长 8x 兜底）；`VideoActivity` initView 的 setCurveMode 与广播 `--ei s` 分支；QBoxSeek 日志去 mode 字段。
- 参数位置：`CustomKeyDownVod.java` FINE_MS=3000 / FINE_STEP=1000 / FINE_TICK=130（前 3 秒细调定稿不动）/ COARSE_TICK=120 / R0≈7.69 / TAU=2500 / CAP_MIN=8.0 / `cap()` / `cumAt()`；片长注入 = `VideoActivity.onSeeking` 每 tick `setDuration`。
- 实测位移表（后会友期加更版，片长 38.75min → cap=12.9）：
  | 按住 | 0.5s | 1s | 2s | 3s | 5s | 8s |
  |---|---|---|---|---|---|---|
  | 位移 | 3s | 6s | 12s | 17s | 36.6s | 71.6s |
  模型点验：elapsed=5087ms 处 cum=36559 ≈ 17000+G(2087)=19545 ✓；长片（沉浸版 99.8min → cap=33.3）8s→128s、顶速渐近 33.2x ✓。
- 快退对称：REW 同 elapsed 速率序列一致（7.69 细调段 → 9.62@3196ms…）、cum 负向镜像 ✓；松手 250ms onSeekEnd 续播 ✓。

#### 35.6 自测与审美自评（用户硬性要求）
- 覆盖矩阵：亮（亮天空 home / 亮金 home g=163 / 亮蓝电视剧榜单）× 暗灰（电影榜单灰蓝 / 电视筛选灰调）× 高饱和红（尚公主底图）；首页/分类/榜单/详情页；激活+静止态。
- 回退版自评：渐变尾段溶进底图无可见线（tail 放大裁图确认）；tab 行干净无斑；带尾下方标题可读。**达标。**
- 栏版自评：栏体均匀、tab 全程压 60% 区、收边在亮底下可见但读作「一条栏」。**我的倾向：栏版更符合「刻意设计」美学**——边界是明确几何收口而非渐变的物理衰减（与 Apple TV+ 同构）；回退版的尾巴在物理上仍是「未收敛的边」，只是衰减足够慢。两版都已装设备（默认 m=0，`--ei m 1` 切栏版），请用户实机拍板。
- 已知取舍登记：静止态（无带）tab 在极浅底图上仅靠 #66 文字阴影（常显底衬方案已被用户否决）；栏版收边与 filter 行 1 顶部的过渡态交叠。
- 迭代轮次：设计参数一轮通过、无美学返工；构建期一次返工（XML 注释含 `--` 非法字符 → 修复后构建通过）。

收尾：GS 更新 = GS-42 定案 B、GS-49 带高 190dp、GS-51 根因再修订（文字不得压渐变尾端）、GS-55 补「拍板后清落选入口」、GS-57 撤回标记；新增 GS-59（回退先指认 commit 并 diff 核对）、GS-60（纯色半透明优先 + 硬边栏收口）。两仓本地 commit（不 push）。

### 第 36 轮（2026-11-07）— 栏版推设备 + 快进改「统一曲线+片长封顶」

#### 36.1 栏版（m=1）推 .214 供实机对照 ✅
- 设备休眠后 `adb connect` 一次连上（无需反复重试）；新构建安装后 `--ei m 1` 推送，停 HomeActivity、**保持唤醒**（本轮不休眠，用户遥控器自验），Prefers `debug_nav_variant=1` 持久化。
- 对照图（同机位，35 轮已取，视觉未变仍有效）：亮海报 激活态 on_q_bright2（渐变带）vs on_bar_bright2（栏）；暗/灰调 榜单页 rank_on_q_dark vs rank_on_bar_dark；高饱和红底 on_q_sat vs on_bar_sat。差异要点：渐变带=从顶到底缓慢衰减、无可见边；栏=tab 行全程均匀 60%、下缘一条明确的边（12dp 收边）——「栏」的收口是几何刻意边，渐变带的尾巴是物理衰减。

#### 36.2 快进：统一曲线 + 片长封顶（用户拍板重写）✅
- **核实旧 B 的成因**：35 轮模型 `rate=R0+(cap−R0)(1−e^(−u/τ))` 整条 ramp 随 cap 线性缩放——短片 cap 小，整条曲线被压扁（同按住时长倍率更低），正是用户抱怨的形态。
- **新模型（GS-42 改写）**：所有片长共用基础曲线 `f(t)=R0+(64−R0)(1−e^(−(t−3s)/τ))`（渐近 64x）；`cap=clamp(片长/60s, 8x, 60x)` 只做截顶（`cumAt` 分段：u≤u* 积分，u>u* 按 cap 线性）。下限 8x 保细调段连续（R0≈7.69x）；上限 60x=1 小时片 60s 整穿越，再高单 tick ≥7.2s 过冲风险大于收益。前 3 秒细调 ±1s@130ms 不变；快退镜像；无模式无开关（A/B 之争闭环）。
- **设备实测**（后会友期 113.9min → cap=60 clamp 命中）：
  - 统一 ramp 逐 tick 与模型吻合（elapsed 8458ms 处 rate 57.66 = 理论 57.7；位移 240.4s ≈ 理论 241.4s，抖动 <0.5%）；
  - 截顶精确：rate 爬升至 59.74 后从 elapsed≈9638ms 起**精确平在 60.00**（理论 u*=6.6s → t≈9.6s ✓）；
  - REW 同 elapsed 速率镜像（12.33@3215 vs FF 12.15@3206…），cum 负向对称 ✓；松手续播正常。
- **位移表**（理论值，设备点验见上；细调段各片长一致）：
  | 片长（cap） | 0.5s | 1s | 2s | 3s | 5s | 8s | 穿越全长按住 |
  |---|---|---|---|---|---|---|---|
  | 5min（8x） | 3s | 6s | 12s | 17s | 33.0s | 57.0s | ≈38s |
  | 20min（20x） | 3s | 6s | 12s | 17s | 53.4s | 113.4s | ≈62s |
  | 40min（40x） | 3s | 6s | 12s | 17s | 67.5s | 187.4s | ≈63s |
  | 100min（60x 封顶） | 3s | 6s | 12s | 17s | 67.5s | 215.3s | ≈105s |
  读法：同按住时长同倍率直到各片 cap 截顶（5s 处 40min 与 100min 相同=67.5s）；短片不被压慢（5min 与旧版一致起步），40min 片约 1 分钟穿越 ✓。
- 参数位置：`CustomKeyDownVod` FINE_MS/FINE_STEP/FINE_TICK（细调定稿）、COARSE_TICK=120、R0≈7.69、TAU=2500、BASE_MAX=64、CAP_MIN=8、CAP_MAX=60、`cap()/uStar()/baseCum()/cumAt()`；片长注入=VideoActivity.onSeeking 每 tick setDuration。

收尾：GS-42 改写（统一曲线+片长封顶口径，防「按比例压扁」再误解）；台账本节。两仓本地 commit（不 push）。待拍板：默认导航变体（m=0/m=1）、filter 底衬强度（#5E）。

### 第 37 轮（2026-11-07）— 栏改实色+极细收边 / filter 延展 / 进度条可聚焦拖动 / 颜色清单

#### 37.1 栏边缘：均匀实色 + 极细收边（材质对齐控制条面板）
- 结构：`shape_nav_scrim_bar`（渐变尾）废，拆「实色主体 nav_scrim(136dp) + 4dp 收边 nav_scrim_edge」；主体色 = 新 token **`@color/qbox_panel #B8000000`**（= shape_controller 48/72/90 渐变的中档 72%，面板渐变中档也改指该 token，两处同源）。实测主体 alpha 0.716-0.724 全平，收边 136→140dp 单调归零（0.54→0.18→0）；亮/暗底边缘放大裁图各一（r37_crop_edge_bright / r37_crop_edge_dark2）。
#### 37.2 filter：单独 scrim 删除，栏向下延展
- `shape_filter_bed` 全删；栏版 filter 展开时主体 240ms 动画延展到最后一行 chip 底沿（TypeFragment.getFilterBottomPx + 8dp），收起归位；显隐 = 焦点在导航区 或 filter 展开。实测 bounds 644px=322dp 贴最后一行，收起无残影。
- 业界对照（写入 GS-62 背景）：Netflix/Apple TV+/YouTube TV 进度条均可聚焦，选中=条加粗/球放大+变色；国产左右固定步长。
#### 37.3 进度条可聚焦拖动
- ↑ 从按钮行进进度条（UpDown 按钮 ↑ 仍调档），选中态=白框+提亮+1.35×（**后被 38 轮 P0-3 否决重做**）；↓/BACK 归还进入前按钮；按住走统一曲线；单点 ±1% 钳 5-30s（scrubTap step=30000 实测）。
#### 37.4 颜色清单 docs/color-audit.md
- 全仓扫描：文字色 13 个、白 alpha 散值 10+；合并后**品牌色 17 个**（文字 5 + 底色 6 + 强调 2 + 白衬描边 4）；标记色选定 `#5B8CFF`（面板底 5.4:1，理由与备选见文档 §3）。文字收敛落地：tab/chip 白系两档（选中 #FFFFFF/未选 #C9C9D1），hero_tab_text 反相 bug 修正；时钟降 white_50+14sp。

### 第 38 轮（2026-11-07）— P0 打回修复：层级/焦点/原生快进/颜色落地/转圈

#### 38.1 栏层级 bug（filter 文字被盖）✅
- 根因：nav_band 在布局里位于 pager 之后（z 最上），72% 实色盖在 chip 文字上。修：两个布局把 nav_band 移到 pager 之下（backdrop 之上），tab/chip 文字天然在栏上。放大裁图 `r38_crop_chips.png`：文字完整无压暗无裁切。新增 GS-61。
#### 38.2 海报按 ↑ 回 filter 落点 ✅
- 根因：`mLastFilterRow` 只在「点击 chip」时记录，纯移动焦点不更新；且 `upToFilter` 用 firstFocusable 一律落行首。修：① OnChildViewHolderSelectedListener 持续跟踪焦点所在 chip 行；② upToFilter 恢复时取行内 HorizontalGridView 自己记忆的选中位（列也还原）。实测：地区行「华语」→海报→↑ 回「华语」；平台行「爱奇艺」→海报→↑ 回「爱奇艺」（行/列双还原，截图 r38_after_up3）。
#### 38.3 fongmi 原生对齐（P1-1）✅
- 对照上游 `ab17e53` 原版 CustomKeyDownVod：原生 = 每按键事件（含 repeat）固定 ±INTERVAL_SEEK(10s)，无曲线。QBox 自造三代模型（阶梯/连续曲线/统一曲线+片长封顶）**全部移除**，GS-42 重写为原生口径并立「禁止再自造变速曲线」。实测：全局每 tick step 恒 10000ms、cum 线性；拖动模式 = max(2×INTERVAL_SEEK, 1%片长)/repeat（38.75min 片实测 step=23251ms 恒等，2.3× 全局）。单点仍 ±1% 钳 5-30s。
#### 38.4 颜色合并全落地 ✅
- color-audit.md 映射表 13 项全部执行（进度表在文档内）：文字近似色并入 L1/L2/L3、白衬 24/29→26、描边 33→4D、shape_hero_tab_normal 删除、#4C8DFF 核查仅注释残留。首页/筛选页截图无掉对比度无死白块。
#### 38.5 播放器转圈补挂 ✅
- 现状结论：activity_video.xml 里 `<include view_progress>` 的 loading_cube **从未被喂图**（WebP 解码发生在 ProgressLayout 自己的 mProgressBinding 上，是另一实例）——不是丢了也不是被遮，是「另一份拷贝」。修：VideoActivity.showProgress 惰性解码同一 WebP 喂给 include 实例（API<28 退 ic_logo）。显隐沿用 STATE_BUFFERING→show / READY→hide。实测起播 `r38_load.png`：立方体动画 + 3 KB/s 垂直叠放；播放中无转圈；seek 后缓冲沿用同一路径（本机 CDN 过快未捕到帧，如实标注）。

收尾：GS 更新（GS-01 标记色登记、GS-34 ↑出口修订、GS-42 原生重写、新增 GS-61/62）；color-audit.md 进度表。两仓本地 commit（不 push）。P1-2（播放菜单重排）/P1-3（轨道层半透明+底浮选择 UI）排入下一轮。

### 第 39 轮（2026-11-07）— 颜色分层硬规则 + 跨页一致性 / 播放菜单 fongmi 化 / 轨道选择底浮式

> 设备 1920×1080 截图（密度 2.0，960dp 宽）；取色 = 区域最亮 10% 像素中位法（.tmp/colorsample.py）。

#### 39.1 颜色分层 + 跨页同色 ✅
- 硬规则落地：标题性/交互文字一律 L1 `#FFFFFF`；**L2 `#C9C9D1` 只给副标题+时钟**；L3 只给备注/hint。tab 三份 color state list（hero_tab_text/tab_text/text.xml）统一为「焦点反色、其余 L1」，选中/未选区分移交底衬（selector_hero_tab：focused 白药丸 / selected 15% 白 / normal 8% 白）；时钟两布局 `@color/qbox_text_dim` 14sp。
- 跨页根因：首页 tab 灰白 = FuncPresenter 不设 selected（TypeAdapter 有）→ 同一 tab 两页两色；本轮改「一律 L1」从根上消灭对 selected 的依赖。
- **跨页逐元素取色核对表**（color-audit.md §5.2，11 行）：首页/分类页/聚合页实测全一致（标题/tab/卡片标题 `#FFFFFF`，时钟/历史卡副标题 `#C9C9D1`，聚合卡备注 `#8C8C98`=L3 备注角色）；详情页亮底图 AA 污染读数，改 token 直查（name=qbox_text / remark=qbox_text_dim / meta=qbox_text_mute）。
- 38 轮「已落地」回验抓出 5 处漏改：adapter_history 标题+备注、adapter_vod、adapter_vod_rect、view_stats 硬编码 `#FFFFFF/#E6FFFFFF/#C9C9D1` → 全部收口 token。教训写入 GS-63（回验必须实机取色不看 diff；同值硬编码视同漏改）。
- 品牌色总数维持 17：shape_sheet 复用 qbox_panel，track_text/toggle 复用 L1/accent/反色，无新增色值。

#### 39.2 播放菜单 fongmi 化（P1-2）✅
- 对照 stock `ab17e53`：原生控制条**本来就没有播放/暂停按钮**（OK 直接暂停 + hideControl）。布局重写为 fongmi 单行序列：下集/上集/[解析 gone]/EXO/硬解/刷新/重播/循环/倍速/比例/字幕/音轨/视轨/[弹幕/版本 gone]/章节/片头/片尾/**详情**（行尾，顶替上游「指令」位）；等宽权重铺满不滚动（GS-33 机制天然满足）。「更多」二级行、speedExtra、播放键设置项（PlayerSetting.isOkMenu + SettingActivity 行 + 三处 strings）全删——fongmi 没有的一律不留。
- VideoActivity：`onKeyCenter` = `wasPlaying = !isEnded() && isPlayWhenReady()` → onPlayPause → 暂停则 `enterSeekMode()`（GS-48 精简态：名称/质量胶囊/进度条/时间，按钮行 GONE），恢复则 hideControl；mPlayIntent/showPlayIntent/updatePlayPause 整套按钮文字机制随按钮删除（12 处调用点清理）；getFocus2/showControl/exitScrubFocus/requestControlFocus 兜底全部改「下集」（fongmi 默认落点）。
- 实测（r39_*）：OK 暂停 state=2 出精简时间状态（顶部标题条+底部时间/进度，无菜单）；OK 恢复 state=3 即收；菜单 16 键（本片有章节）铺满无滚动；循环 ON = 标记色描边+蓝字（OFF 回 L1），toast「循环：开」；倍速 ↑ 就地 1.0→1.2x（按钮即时显示 1.2x），OK 开倍速设置面板（slider 带入 1.2x）。

#### 39.3 轨道选择底浮式（P1-3）✅
- `BaseBottomSheetDialog` 加 `isSheet()` 分支：TrackDialog 回 fongmi 底浮式（底部浮起、全宽、高度随内容上限 85%），其余设置弹窗保持居中面板不动。层背景新 shape_sheet = `@color/qbox_panel`（72% 黑，顶圆角 20dp，GS-62 同 token 不另起色值）；假 inset 防御（consume + 常驻 margin 归零）与面板化同套。
- 选中轨道颜色标志：新 track_text.xml（焦点反色 / **选中=qbox_accent** / 其余 L1），实测音轨层「0.19 Mbps, AAC-LC」非焦点态蓝字+浅底衬（`r39_track3`）。字幕/视轨同一 TrackDialog 类同码路径，一并生效。
- GS 更新：GS-01 登记扩展（开关型 ON 态）、GS-34 删 extra 例外、GS-48 扩暂停复用、新增 GS-63（分层+跨页同色）、GS-64（播放流程完全对齐 fongmi）。

#### 39.4 P0 回归 ✅
- 进度条选中态：菜单 ↑ 进 scrub，已播段+圆球标记色，无变形无白框（`r39_scrub`）；循环蓝态在多帧间保持。
- filter 层级：filter 展开后栏延展覆盖到最后一行底沿，chip 文字完整在栏上（`r39_filter2`）；焦点记忆：华语→海报→↑ 回「华语」行列双还原（`r39_fmem`）。
- 历史行新增「不存在的罪人」带进度条卡片（播放位置记忆正常）。
- 已知未验项（如实标注）：视轨/字幕层仅同码路径推断未逐页截图（音轨层已验）；seek 后缓冲帧本机 CDN 过快仍未捕到（沿 38 轮标注）。

### 第 40 轮（2026-11-07，v1.9.20 发版）— 循环聚焦去双层 / 暂停去顶标 / 来源弹窗回退 / 面板材质全局统一 / 发版 GitHub

#### 40.1 循环聚焦去双层表达 ✅
- 聚焦+开 = 白药丸 + **标记色文字**（shape_chip_toggle_on_focused 去 1.5dp 内圈描边；control_toggle_text 的 selected 提前到 focused 之前判定）。实测 `r40_repeat`：白丸上蓝字，无内圈。OFF 聚焦 = 白丸深字（与全站焦点一致）。

#### 40.2 暂停去顶部标题 ✅
- onPlayingChanged 暂停分支 showInfo→hideInfo（showInfo 成死码已删）。实测 `r40_pf`：暂停只出底部精简态（标题/质量胶囊/进度/时间），顶部无重复标题条。

#### 40.3 来源弹窗回退紧凑+半透明 ✅
- 根因：44ce5d6（第 16 轮统一弹层）把 BaseAlertDialog 全部钉死 560dp——SiteDialog 原 0.4 屏宽被波及。修：BaseAlertDialog 加 `isCompact()` 钩子，SiteDialog 回退 0.4/0.6/0.8 屏宽（列表 384dp/网格 576/768dp）；shape_panel 实心 surface→qbox_panel。实测 `r40_site5`：弹窗内边 759px≈380dp、透出首页底图。改前/改后：560dp 实心 → 384dp 半透明。

#### 40.4 轨道层焦点强化 ✅
- 齿轮/搜索/选择文件三个图标按钮：borderless ripple（TV 上聚焦不可见）→ selector_icon_btn（聚焦白圆）+ icon_tint（聚焦深色）。实测 `r40_gear2`：齿轮白圆一目了然；选中轨道「立体声, AAC-LC」蓝字保持。

#### 40.5 高级设置品牌化 + 全局面板统一（GS-65）✅
- shape_panel → qbox_panel（一处改，Alert+面板化 sheet 全半透明）；shape_widget/stats/delete_hint 散黑并入；新增 GS-65 单一标准（纯黑、qbox_panel 基准、无描边、整页豁免）。
- 五弹窗（倍速/音频/视频/字幕/弹幕）控件品牌化：QBoxDialogChip（无 Material 描边、10% 白衬、焦点白丸）、QBoxDialogButton、switch 开=标记色、slider 白轨无彩晕。实测 `r40_speed2`：倍速设置无紫色残留、面板透视频。

#### 40.6 导航带与播放菜单对齐 ✅
- 实测对照：菜单渐变 48→72→90（平均 70%）；导航带原 85% 峰 #090A0D——「太深色」实锤。对齐：纯黑化 + 峰值 72%（=qbox_panel），文字区 72→48% 缓降、尾部二次归零（几何不动）。带开/关同机位反解：顶部 alpha 实测 0.658≈设计 0.66；合成亮度 13→26（菜单同区 ~23，差 ≤12%）。亮海报（紫蓝底）tab/时钟/标题全清晰（`r40_bright3`）；tab 底缘 48% 处 L1 5.0:1、时钟 L2 7.0:1。

#### 40.7 发版 v1.9.20 ✅
- versionCode 77→78 / versionName 1.9.19→1.9.20；commit 6920237；`git push origin ui-cinema` + `ui-cinema:main`（均快进）；annotated tag v1.9.20（--cleanup=verbatim，说明完整）已推送 qbox-tv。
- GitHub Release（zqs1qiwan/qbox，latest）：QBox-1.9.20-armeabi-v7a.apk + QBox-1.9.20-arm64-v8a.apk + QBox-1.9.20-source.zip。
- app 内更新验证：API `repos/zqs1qiwan/qbox/releases/latest` 返回 tag_name=v1.9.20、assets 双 APK 命名匹配 `QBox-<cleanTag>-<abi>.apk`；`releases/latest` 302 → /tag/v1.9.20（Updater 兜底路径）；设备装 1.9.20 手动检查更新 = 「已是最新」（证明客户端读取+解析+比较链路通）。

#### 40.8 P0 回归 ✅
- 进度条选中态标记色（`r40_scrub` 沿 39 轮路径复验）、循环蓝态跨帧保持、跨页同色未动、filter 层级/落点未动（本轮无相关代码变更）；导航带几何（190dp 尾归零、焦点进出显隐）实测未回归。
- 备注：测试机装有 baiguotv 包（他项目），一次 BACK 链过长误入其前台已立即 am start 拉回，未做任何交互。

### 第 41 轮（2026-11-08）— 实机复核打回修复：循环无圈 / 暂停留时钟 / sheet 渐变对齐 / 弹窗按钮白字 bug / 推送简体化

#### 41.0 设备构建核对（用户要求先查证）
- `.214` 装机 = 1.9.20/78，lastUpdateTime 23:21:04 = commit `6920237` 内容的 debug 包（APK 22:35:22 构建，aapt badging 复核）。用户复核机上的「没改」= 实现与用户预期有出入（见下），非装错包。

#### 41.1 循环聚焦圈 ✅（最终形态）
- 用户要的不是「去掉内圈描边」，是**全态无圈**：开关靠文字变色（OFF 白 / ON 标记色），聚焦靠**加粗字重**，双通道都不画圈。
- 删 selector_chip_toggle/shape_chip_toggle_on(_focused) 三个 drawable；repeat 背景 transparent；control_toggle_text 简化为 selected→accent/默认 L1；VideoActivity 加 OnFocusChangeListener 切 BOLD。
- 实测：OFF 聚焦 = 白粗体（`r41_t2`）、ON 聚焦 = 蓝粗体（`r41_f`）、OK 正常 toggle（prefs repeat_one 翻转 + state 不变）。

#### 41.2 暂停去标题留时钟 ✅
- 40 轮误删整条 widget.top（标题+时钟一锅端）。本轮：暂停 top 只亮时钟（title/size GONE），时钟样式降为 L2 #C9C9D1 16sp。实测 `r41_pause`：右上角 00:07:41 在、顶部无标题、底部精简态如常。

#### 41.3 音轨/章节/视轨层「比菜单黑太多」✅
- 根因：39 轮 shape_sheet 用 flat `qbox_panel` 72%，而菜单是 48→72→90 渐变、顶部视线区更透——同帧反解旧 sheet 顶部 α≈0.72。
- 修：shape_sheet 直接套用菜单同款 48→72→90 纵向渐变（顶圆角 20dp）；ChapterDialog 并入 isSheet 底浮族。
- 实测（`r41_sheet2` 同帧邻域反解）：sheet 顶部 α 0.42→0.52（设计 0.48→0.72 上行 ✓）；顶部合成亮度 73 vs 旧值 35（透一倍多）。

#### 41.4 全局「未统一」盘点（实机核对版）
| 面板/层 | 实现 | 实测证据 | 合规 |
|---|---|---|---|
| 播放控制条 | shape_controller 48/72/90 | 基准 | ✅ |
| 底浮层（音/视/字轨、章节） | shape_sheet 同菜单渐变 | r41_sheet2 反解 | ✅ |
| Alert 弹窗族（来源/更新/配置/UA/弹幕API/简介/密码/崩溃/MPV/Live） | BaseAlertDialog→shape_panel=qbox_panel | r41_cfg3/r40_site5 透视 | ✅ |
| 面板化 sheet（倍速/音频/视频/字幕/弹幕设置、解析/版本/弹幕列表/投屏） | panelize→shape_panel | r40_speed2 | ✅ |
| 导航带 | shape_nav_scrim 72→0 纯黑渐变 | r40 反解 0.658 | ✅ |
| HUD（error/message/center）/stats/删除提示 | qbox_panel | 同 token | ✅ |
| Notify toast | 系统 Toast | — | 豁免（非面板） |
| 设置页等整页 | qbox_surface 实底 | — | 豁免（整页） |

#### 41.5 扫码推送全简体 ✅
- 繁体源 = 手机端网页资产（TV 侧 strings 本来就简体）：assets/index.html（lang zh-Hant、全篇繁体）+ js/script.js（7 处提示繁体）+ parse.html（lang zh-TW）。
- index.html 全量重写简体（SVG 路径经 git 校验零漂移）；script.js 7 处替换；parse.html lang→zh-CN。
- 实测：`curl http://192.168.2.214:9978/?tab=2` 返回 lang="zh-Hans"、全简体；TV 侧 push_info 简体（`r41_cfg3`）。

#### 41.6 更新弹窗按钮白底白字 ✅ + 全局按钮聚焦排查
- 根因：selector_button 聚焦=白药丸，而 activity_crash/dialog_pass/dialog_update 文字硬编码 `@color/white` → 聚焦白底白字。dialog_config/mpv_conf/ua 用的是 @color/text（state list）所以没事。
- 修：3 处 5 个按钮改 @color/text。实测同构渲染：`r41_cfg3` 配置弹窗「确定」= 白丸深字清晰可读（同 selector_button+@color/text 组合）。
- 排查清单（聚焦态表达，深/亮底各验过）：Control 菜单钮（白丸深字）、循环（变色+加粗）、QBoxDialogChip/Button（白丸深字）、selector_icon_btn 齿轮（白圆深 icon）、selector_action_icon 返回（白圆）、SettingRow 设置行（白丸）、来源/轨道/章节行（白丸）、详情页按钮（白丸）、filter/tab chip（白丸）、slider（本轮补 white_20 halo）、更新/崩溃/密码弹窗钮（本轮修复）。未发现其他白底白字或对比不足项。

#### 41.7 备注
- 过程发现一条操作纪律：菜单链内禁止插 screencap（约 1s 无键空窗会让 5s 自动收起抢跑，后续方向键变全局快进）——与 GS-09 同因，执行时再犯一次，已纠正并重测。
- P0 行为（快进/焦点/栏随滚动/跨页同色）未触碰，无回归。

### 第 42 轮（2026-11-08）— 默认底色浅黑灰化 + 设置整页分组半透明块

> 用户：「默认的黑色太黑了……改为浅黑灰稍微有点对比色的黑吧，这样黑色的半透明设置栏目也看得出；整个设置页面模块也是要做的半透明背景块稍微好看一点」。根源：纯黑底上叠 72% 黑面板合成仍是纯黑，面板与底零反差。

#### 42.1 默认底色 #000000 → #12141B ✅
- `qbox_bg` 改 `#12141B`（冷调深灰微蓝，与标记蓝同族）；连带同步：`bg_app` 渐变 `#0D0F15→#05060A` → `#151823→#0F1117`（顶部柔光保留）、`qbox_header_scrim` `#F0090A0D`→`#F012141B`、scrim 一族 ×9 末端 `#090A0D`→`#12141B`（alpha 曲线不动，海报边缘融合无接缝）。
- 选值理由：72% 黑面板叠上合成 ≈`#050608`，比底色暗 ~3 档——**面板靠明度差自然沉出，不靠描边**（GS-65 ④ 不变）。
- 改前/改后同机位：首页改前（纯黑系，`r27_home3` 同机位参照）vs 改后 `r42_home`——海报与底色间出现可感知明度差，卡片浮起；文字无掉对比度。

#### 42.2 设置整页 = 分组半透明背景块 ✅（撤销 41 轮「整页实底豁免」）
- `shape_settings_panel`：`qbox_surface` 实心 + `#1FFFFFFF` 描边 → **`@color/qbox_panel` 无描边**，20dp 圆角不动。GS-65 ⑤ 豁免项删除，改写为「页面底色 = 浅黑灰 qbox_bg #12141B；整页 Activity 不再是豁免项，设置页分组块同走 qbox_panel」。
- 实测 `r42_settings`：两个分组块（内容源+播放｜系统+维护）明显暗于页面底色、层次清晰，分组小标题蓝条 accent 保持；块合成色 `#08080A`。

#### 42.3 连带抽测（底色一改，反解 alpha 全变；重新抽测合成观感）✅
- **文字对比度实测**（1920×1080，区域最亮 10%/最暗 30% 中位；改前=纯黑理论值）：tab 选中 21→**18.2:1**、时钟 L2 12.8→**11.4:1**、分组标题 **18.1:1**、卡副标题 L2 **11.1:1**、设置块内文字 **19.9:1**、设置值 **20.3:1**、弹窗标题 **20.9:1**——全部 ≥10:1 远超 AAA；L3 理论 6.3→5.5:1 仍过 AA 4.5。
- **各层合成色**（详见 color-audit.md §8.3 表）：设置块 `#08080A` ≈ 导航带 `#08090C`（同 qbox_panel 72% 黑在新底上的合成，±1/255）；Alert 叠 dim ≈`#010102`；播放菜单/底浮轨道层叠视频画面不经页面底色，材质未变。**不再出现「比菜单黑」。**
- 焦点药丸复查：首页「豆豆|片单」白药丸（`r42_nav`）、设置行白药丸、播放菜单「下集」白丸、弹窗「确定」白丸深字（`r42_alert2`）——新底色下反色焦点全部清晰，无白底白字。
- 暂停态时钟（41 轮修复项）在新底色下保持（`r42_sheet` 右上角 00:49:38 可见）。

#### 42.4 备注
- 41 轮截图（r41_h 等）在 `.tmp` 已被清理，改前对比用 27 轮同机位纯黑首页（`r27_home3`）替代，如实登记。
- 误操作：BACK×3 退出设置时链过长把 QBox 退到系统桌面（`com.huanxi.tvhome` 前台），立即 monkey 拉回 QBox HomeActivity，未与桌面做任何交互；`.214` 保持唤醒。
- 构建：debug v7a 构建成功（APK 00:33:08），`install -r` 装机；versionCode/Name 未动（发版等用户确认）。

### 第 43 轮（2026-11-08）— 循环聚焦统一 / 菜单巡回 / 导航带再透一档 / 设置页 A·B 方案 / emoji 支持 + GS-66 品牌统一化 + 全量回归

> 用户：「为什么光标聚焦到循环上跟其他菜单光标不一样呢？让你改的是点击后的效果取消边框」「播放菜单这一行可以做巡回」「tab 的下拉黑色透明背景有点太黑了……黑的导致那几个搜索设置等按钮都看不到圆圈边缘了」「设置页面还是有点不好看……抄下 Android tv 设计标准和 apple tv 设计吧」「还是要支持 emoji 的源」；随后下达**元素级品牌统一化金标准 + 全量回归 + 测完 adb 关机**。

#### 43.1 循环聚焦态恢复统一白药丸（纠正 41 轮误改）✅
- 根因：40 轮用户说「聚焦不要圈」被误执行为「取消聚焦样式」（透明底+加粗），用户真实意图是**「点击开启后不要额外那层底衬」**——selected 态的 `shape_chip_activated`（12% 白底丸）才是要去掉的"圈"，聚焦白药丸必须保留且与全站一致。
- 修复：`selector_repeat_bg`（聚焦=`shape_chip_focused` 同一块白丸，selected 无底衬）+ `control_toggle_text`（selected→accent 最优先 / focused→深字 / 默认 L1）；删除 41 轮 BOLD hack。
- 四组合同帧实测：关+聚焦=白丸深字（与邻钮一致）、开+聚焦=白丸**蓝**字、开+非聚焦=蓝字无衬、关+非聚焦=白字无衬（r43_rep_focus_off/r43_b_focus/r43_b_unfocus）。
- **GS-66 判例**写进金标准（Blocker 级，见 43.6）。

#### 43.2 播放菜单行左右巡回 ✅
- `onControlKey` LEFT/RIGHT：行尾 `findFocusable` 返回 null 时 wrap 到 `findEdgeFocusable`（另一端第一个可见可聚焦）；仅本动作行，filter 行等其它行锁定行为（GS-54）不动。
- 实测：详情（行尾）+RIGHT→下集（行首）；下集+LEFT→详情（r43_wrap1/r43_wrap2）。

#### 43.3 导航带/栏再透一档（72%→60%）✅
- 新 token `qbox_nav_bar` #99000000；栏版主体+收边改用它（qbox_panel 基准不动）；渐变带峰值 #B8→#99 全曲线 ×0.833。
- 改前/改后同机位：r42_nav（72%）vs r43_nav60b（60%）——栏合成 `#0F0D0D`→`#161212`，透出海报柔光；**搜索/设置等图标非聚焦圆底边缘清晰可见**，搜索白丸焦点对比更强。
- 亮海报预算：L1 ≈8.1:1、L2 ≈4.9:1（均过 AA）。GS-65 ② 已修订。

#### 43.4 设置页视觉重做：A/B 两方案实机同机位（**第 44 轮用户已选定 B**）
- 参考：tvOS HIG（标题外置、内容成组、组间大留白）+ Android TV 原生设置（无卡片、小标题+行列表）；行高统一 8→12dp（两案共用）。
- **方案 A（Apple TV 式，未采用）**：分组标题挪出块外上方，4 个独立 `qbox_panel` 分组块——`r43_setA`，留档 `shots/settings-A-main.png`。
- **方案 B（Android TV 式，✅ 第 44 轮用户选定并合入主线 `907920c`）**：去卡片，行直落 `#12141B` 底，分组靠标题+间距——`r43_setB2`。（注：初版 B commit 曾基于 42 轮代码，已重建到 43 轮主线最新之上，确保 B 包同样含循环/巡回/导航带/emoji 全部修复。）
- 两版同机位（设置首屏、焦点"点播"）供用户挑；落选方案 revert/弃分支即可。

#### 43.5 源站名 emoji 支持 ✅
- 实测：设备有 `NotoColorEmoji.ttf` 但固件字体链不挂（TextView 默认链渲染 ▯，43 轮初版截图 crop_toast 实证）。
- 方案：`androidx.emoji2:emoji2(-bundled) 1.5.0` 内嵌字体（APK 55.7→62.8MB）；`App.onCreate` EmojiCompat.init；`Util.cleanName` 重做（emoji/ZWJ/VS 全保留，只删私有区）；系统 Toast 显式 `EmojiCompat.process`（Notify.makeText——实测 toast 豆腐块后补的刀）。
- 实测：详情页站名 🐻剧圈、聚合卡 🧽海绵🌸茉莉👀瓜子、历史卡角标 🐻、换源 toast 👽玩偶哥哥——彩色 emoji 全显，无豆腐块。

#### 43.6 GS-66 元素级品牌统一化（新 Blocker 金标准）✅
- GS-66 已写入 GOLDEN-STANDARDS.md（同元素跨页完全一致：字体族/字号/字重/行高/颜色/焦点态/间距/图标规格；只允许功能行为与文案不同；一律引用共享 token/style，禁止页面级各自定义）。
- 新建 `docs/element-matrix.md`：元素×页面矩阵 30+ 行逐项登记（实现 token + 实测证据 + 状态）；发现 1 处不一致：**直播 invert/across/change ON 态 = 12% 白底** vs 点播循环 ON = 蓝字——登记待用户拍板（直播路径未实测，不擅改）。

#### 43.7 全量回归（GS-01~66 + 历史遗留）

| GS | 结果 | 证据/说明 |
|---|---|---|
| 01 颜色分层 | ✅ | 42 轮 §8 全量对比度；43 轮未动文字 token |
| 02 尺寸 token 单点 | ✅ | 本轮 SettingRow 行高改 style 单点全站生效 |
| 03/04/05/06/07 交互工具与三态双通道 | ✅ | 循环=白丸+蓝字双通道；三态一致（43.1） |
| 08/09/10/11 工具纪律 | ✅ | 全程 install -r、Edit/Write、diff 证明未动父层级 |
| 12-18 弹窗家族/层级/度量/焦点/字体族 | ✅ | 42 轮弹窗实测沿用；43 轮配置弹窗复验（标题 20.9:1） |
| 19 禁遮羞条 | ✅ | 无新增 |
| 20 氛围底图 | ✅ | 首页海报柔光（r43_home4） |
| 21/22 详情系统 | ✅ | 详情页 emoji 站名复验（r43_nav_pos） |
| 23 防豆腐块 | ✅ | emoji2 bundled（43.5）；私有区仍删 |
| 24 占位规范 | ✅ | 起播 logo 占位（r42_play 沿用） |
| 25 图片管线 | ✅ | 未动 |
| 26 配置兜底 | ✅ | qbox_default.json 在包；启动正常 |
| 27/28 投屏/直播 | ➖ 不适用 | 本轮未触碰；直播 ON 态不一致已登记（43.6） |
| 29/30/31 芯片/状态持久化/系统返回 | ✅ | repeat_one prefs 翻转持久（false→true 实测） |
| 32 弹层 inset | ✅ | 未动 |
| 33 等分基准 | ✅ | 菜单 16 键等分铺满（r43_b_focus） |
| 34 防串键 | ✅ | 倍速 ↑ 调档 1.0→1.2x、进度连续无跳变（r43_speed4） |
| 35 D-pad/BACK | ✅ | 全程 D-pad 遍历；BACK 逐级（2 次链过长退桌面为操作问题非 app 缺陷） |
| 36-40 顶栏/过扫/字号/压图/环规则 | ✅ | 未动；42 轮实测沿用 |
| 41-48 暂停契约族 | ✅ | 暂停精简态有时钟无标题（r43_rep_focus_on）；恢复展开沿用 41 轮实测 |
| 49-53 导航带族 | ✅ | 焦点入区淡入（r43_nav60b）、静止无带（r43_home4）、来源名不闪、无灰带 |
| 54 行锁定 | ✅ | filter 行"综合"LEFT 锁定（r43_filter_lock）；菜单行按 43.2 改为巡回（用户拍板） |
| 55 实验参数登记 | ✅ | debug_nav_variant 双形态保留 |
| 56/57/58 组级路径/首行/聚集 | ✅ | 未动 |
| 59 回退纪律 | ✅ | 本轮无回退 |
| 60 快进隐藏控件 | ✅ | 控制层隐藏时 RIGHT=快进（43.7 过程实证） |
| 61 装饰层在内容下 | ✅ | filter 展开栏延展到 chip 行下（r43_filter） |
| 62 进度条变色 | ✅ | scrub 选中 accent 蓝只变色不变形；单点步幅 30s>全局 10s（r43_scrub5/6） |
| 63/63 补 | ✅ | 元素矩阵逐行核对（element-matrix.md） |
| 64 播放逻辑 | ✅ | OK 暂停精简态、菜单单行铺满、行尾详情、轨道底浮（43.1/43.7 各图） |
| 65 面板材质 | ✅ | 42 轮 §8.3 + 43 轮导航带 60%（②修订） |
| 66 品牌统一化 | ✅ | 本条新增；矩阵建立；循环判例闭环 |
| H 系列/3.2 遗留 | 见下 | ⑨搜索页不包=已知结构；⑩搜索区交叠=open；H1 行尾锁=open（43.2 巡回后该条语义变化：菜单行已按用户要求巡回，filter 等其它行仍锁定）；「测试」搜索历史=用户数据不主动清；导航带 m=0/m=1 最终拍板仍待用户（43.3 后 m=1 栏在役） |

#### 43.8 备注
- 误操作登记：BACK 链过长 2 次把 QBox 退到系统桌面（com.huanxi.tvhome），均立即 monkey 拉回、未与桌面交互；UP 落点依赖当前焦点，盲链失败 2 次后以 dumpsys+截图逐步定位完成。
- scrub 位置基线在一次进入前后有跳变（疑自动换源/切集），scrub 行为本身（变色/步幅/缓冲）全部正常，不影响结论。
- 构建 3 次：主线+emoji2（62.8MB）、B 版（settings-plan-b，后于 44 轮前重建为 `907920c`）、最终版含 toast 修复（65.1MB，差异为 emoji2 字体打包方式）。
- versionCode/Name 未动；发版产物未准备 push/release（等用户确认）。

### 第 44 轮（2026-11-08）— 设置页方案 B 选定合入 + v1.9.21 发版

> 用户：「还是 b 好看一点 选 b 吧 更新线上版本」。

#### 44.1 B 合入主线 ✅
- `settings-plan-b`（`907920c`，已重建于 43 轮主线最新之上）fast-forward 合入 `ui-cinema`，分支删除。合入后逐项核查共存：B 版 4 处 transparent 块 + 循环 selector_repeat_bg + 巡回 findEdgeFocusable + 导航带 qbox_nav_bar + emoji2 依赖，全部在位。
- 文档同步：GS-65 ⑤ 修订（主设置页=行直落 qbox_bg 无块；二级设置页暂保留分组块，不一致登记待统一）；element-matrix §4/§5 + 处置记录；color-audit §9.4；43.4 标注「A 未采用留档」。

#### 44.2 v1.9.21 发版
- versionCode 78→**79**、versionName 1.9.20→**1.9.21**；内容 = 43 轮全部（循环聚焦统一/菜单巡回/导航带 60%/emoji 内嵌/toast emoji/A·B 设置页选定 B）。
- 双 ABI release + source zip，命名沿用 `QBox-1.9.21-<abi>.apk`；tag `v1.9.21`（`--cleanup=verbatim`）+ GitHub release 标 latest。
- 产物：armeabi-v7a 48.9MB / arm64-v8a 54.9MB（release R8 压缩后）/ source.zip 24.5MB（git archive HEAD）。
- 推送：开发仓 `qbox-tv`（ui-cinema + tag）与发布仓 `qbox`（tag；main 分支 push 被拒——远端有 40 轮后的额外提交，未强推，release 基于 tag 不受影响，main 分叉留用户决定）。

#### 44.3 发版验证（三重，全部通过）
- **API**：`repos/zqs1qiwan/qbox/releases/latest` → tag `v1.9.21`、name `QBox 1.9.21`、assets 三件（双 APK + source.zip）✓
- **网页 302**：`releases/latest` → 302 `/releases/tag/v1.9.21` ✓
- **Updater 解析**：设备 UA（okhttp/3.15）请求 API 200；asset 命名 `QBox-1.9.21-<abi>.apk` 匹配 Updater 规则（cleanTag=1.9.21、abi 下划线转横线）✓

#### 44.4 设备侧（待开机验证）
- `.214` 发版时仍处关机状态（adb device not found），以下项**待开机后验证，未执行**：① 装对应 ABI 的 1.9.21 release 包；② 设置页呈 B 样式（行直落背景无块）；③ 现机 1.9.20 debug 点「版本」应弹「发现新版本 v1.9.21」（正例），装 1.9.21 后应报「已是最新」。

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