# QBox UI/UX Review Log（统一记录文档）

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

### 切换类按钮的状态表达规则（N-28 落地，全站统一）

适用：循环、弹幕开关、倍速（取值显示）、缩放/解码/引擎（取值显示）、片头/片尾（取值显示）。

1. **开关型**（开/关）：按钮持久显示状态——开 = `qbox_accent_soft` 底衬 + 1dp `qbox_accent` 描边 + accent 文字；关 = 透明底 + #C9C9D1 灰白字。焦点态仍是实心白药丸，**焦点+开 = 白底 + accent 描边 + accent 字**（焦点环与状态互不吞掉）。不靠 toast 表达状态（toast 保留为操作反馈）。
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
| 二级设置页（播放/解码/预载/弹幕） | — | — | ❌ | 本轮未逐页进入（已有 N-18 修复记录，待补验） |
| 投屏 CastActivity | — | — | ⚠️ | 推送页已验证；独立 CastActivity 未单独进入 |
| 更新检查 | — | — | ❌ | 需真实网络到 GitHub，未触发 |
| 播放二级功能（倍速/音轨/字幕/选集/快进阶梯） | — | — | ⚠️ | 被 N-24 控制条焦点阻断，修复构建后补验 |
| 直播 EPG / 换台 / 加密分组 | — | — | ⚠️ | 频道列表/信息条已验证；EPG 节目单与加密分组未逐项 |

> 未覆盖项将在 N-24 修复构建 + `install -r` 后，用「播放键=呼出菜单」设置补齐播放页二级功能；EPG/加密分组依赖具体源配置。