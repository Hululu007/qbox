# 元素×页面矩阵（GS-66 元素级品牌统一化）

> 第 43 轮用户拍板（Blocker 级）：同一语义元素在任意页面/任意模块必须完全一致——字体族、字号、字重、行高、文字颜色、焦点态、间距、图标规格；**只允许功能行为与文案不同**。
> 用法：新增/修改元素前先查本表既有规格；改完同步登记实测证据（截图/取色值）。状态：✅ 一致 / ⚠️ 不一致待处置 / — 不适用。

## 1. 文字

| 元素 | 出现页面 | 实现（共享 token/style） | 实测证据 | 状态 |
|---|---|---|---|---|
| L1 标题/交互文字（白） | 全部页面 | `@color/qbox_text` #FFFFFF | 42 轮：tab 18.2:1、分组标题 18.1:1 | ✅ |
| L2 副标题/时钟 | 首页/分类/详情/播放暂停/设置 | `@color/qbox_text_dim` #C9C9D1 | 42 轮：时钟 11.4:1、卡副标题 11.1:1 | ✅ |
| L3 备注/hint | 聚合角标/详情 meta/搜索 hint | `@color/qbox_text_mute` #8C8C98 | 39 轮定档；新底理论 5.5:1（AA 4.5 过） | ✅ |
| 焦点反色文字（白药丸内） | 全部白丸 | `@color/qbox_focus_text` #0B0B0F（经 `@color/text` 系 state list） | 各轮白丸深字截图 | ✅ |
| 弹窗标题/正文 | Alert 族全部 | `@color/text` state list | 43 轮配置弹窗标题 20.9:1 | ✅ |

## 2. 按钮

| 元素 | 出现页面 | 实现 | 实测证据 | 状态 |
|---|---|---|---|---|
| 菜单按钮（文字类） | 播放控制行（点播/直播/投屏） | `@style/Control`：`selector_chip` 底 + `@color/text` 字；`0dp+weight=1` 等分 | 43 轮 r43_*：白丸深字；GS-33 等分 | ✅ |
| 循环（开关型菜单钮） | 播放控制行（点播） | `selector_repeat_bg`（聚焦=同一块 shape_chip_focused 白丸，selected 无底衬）+ `control_toggle_text`（selected→accent / focused→focus_text / 默认 L1） | 43 轮四组合：关聚焦白丸深字、开聚焦白丸蓝字、开非聚焦蓝字无衬、关非聚焦白字无衬 | ✅ |
| 开关型菜单钮 ON 态 | 点播循环 vs 直播 invert/across/change | 点播=蓝字无衬（43 轮）；直播=`shape_chip_activated` 12% 白底+文字不变色 | **两者 ON 态表达不一致** | ⚠️ 待用户拍板（直播 ON 是否也改蓝字） |
| 弹窗按钮（确定/取消/选择） | Alert 族全部 | `QBoxDialogButton`/`selector_button` 白丸 + `@color/text` 深字 | 41 轮修白底白字；43 轮配置弹窗复验 | ✅ |
| 图标按钮（圆形） | 顶栏 5 图标/轨道层齿轮/详情返回 | `selector_action_icon`/`selector_icon_btn` 白圆 + 聚焦深色 icon（icon_tint） | 40 轮 r40_gear2；43 轮 r43_nav60b 白圈边缘可见 | ✅ |
| 详情页动作按钮（播放/简介/收藏/换源） | 详情页 | 白丸聚焦（与全站一致） | 42/43 轮详情页截图 | ✅ |

## 3. chip / tab

| 元素 | 出现页面 | 实现 | 实测证据 | 状态 |
|---|---|---|---|---|
| tab（分类行） | 首页/分类/榜单 | `@style/QBoxTabText`（17sp/medium/字距 0.01/`@color/tab_text`/阴影）+ `selector_hero_tab` 底衬 | 39 轮跨页取色一致；43 轮 18.2:1 | ✅ |
| filter chip | 分类页 | `chip_bed`（焦点白/默认 10% 白）+ L1 字 | 39 轮 r39_filter2 | ✅ |
| 弹窗 chip | 倍速/音视字幕弹幕五弹窗 | `QBoxDialogChip`（无 Material 描边） | 40 轮 r40_speed2 | ✅ |
| 搜索键盘/热词 chip | 搜索页 | 同上 token 族 | 39 轮 | ✅ |

## 4. 卡片与列表行

| 元素 | 出现页面 | 实现 | 实测证据 | 状态 |
|---|---|---|---|---|
| 卡片标题 | 首页/分类/聚合/收藏 | `@color/qbox_text`（38 轮收口硬编码） | 42 轮 18.1:1 | ✅ |
| 卡片副标题（历史进度） | 首页历史行 | `@color/qbox_text_dim` | 42 轮 11.1:1 | ✅ |
| 卡片角标备注 | 聚合/首页 | L3 `@color/qbox_text_mute` | 39 轮 | ✅ |
| 卡片焦点环 | 全部卡片 | 3dp 白环只圈海报、ZOOM_FACTOR_SMALL | 37-39 轮 | ✅ |
| 设置行 | 设置/四个二级设置页 | `@style/SettingRow`（selector_item 白丸，43 轮行高 8→12dp）+ SettingLabel/SettingValue（`@color/text` 17sp）；**主设置页 44 轮起行直落 qbox_bg 底（方案 B，用户选定）** | 42/43 轮设置页截图；44 轮 B 合入 | ✅ |
| 轨道行（底浮 sheet） | 音/视/字轨/章节 | `track_text`：焦点反色/选中 accent/其余 L1 | 39 轮蓝字；43 轮 sheet 标题 12.9:1 | ✅ |

## 5. 面板与浮层

| 元素 | 出现页面 | 实现 | 实测证据 | 状态 |
|---|---|---|---|---|
| 播放控制条 | 播放 | `shape_controller` 48→72→90 黑渐变（基准） | 40 轮 | ✅ |
| Alert 面板 | 全部弹窗 | `shape_panel`=`qbox_panel` 28dp 圆角 | 42 轮弹窗标题 20.9:1 | ✅ |
| 底浮 sheet | 轨道/章节 | `shape_sheet` 同菜单 48→72→90 渐变，顶角 20dp | 41 轮反解 α；43 轮复验 | ✅ |
| 设置分组块 | **仅二级设置页**（播放/解码/预载/弹幕） | `shape_settings_panel`=`qbox_panel` 20dp（42 轮撤豁免）；**主设置页 44 轮方案 B 去卡片，无块** | 42 轮块合成 #08080A | ⚠️ 二级页块与主页 B 版不一致，待用户看二级页后拍板统一 |
| 导航带/栏 | 首页/分类 | `shape_nav_scrim` 峰值 60%（43 轮 72%→60%）/ `qbox_nav_bar` 60% 栏 | 43 轮：合成 #161212（改前 #0F0D0D），图标白圈边缘可见 | ✅ |
| HUD（widget/stats/删除提示） | 播放/首页 | `qbox_panel` | 40 轮 | ✅ |
| toast | 全局 | 系统 Toast（GS-65 豁免：非面板） | 43 轮「循环：开」目验 | ✅ 豁免 |

## 6. 控件

| 元素 | 出现页面 | 实现 | 实测证据 | 状态 |
|---|---|---|---|---|
| switch 开关 | 高级设置五弹窗 | `switch_thumb`/`switch_track`：开=accent/accent_soft、关=white_60/white_20 | 40 轮 | ✅ |
| slider 滑轨 | 倍速/弹幕/字幕 | 白轨 + halo `white_20`（41 轮） | 41 轮 | ✅ |
| 播放进度条 | 播放 | 已播+圆球：默认白、选中 `qbox_accent`（GS-62 只变色） | 37/39 轮 | ✅ |
| 历史卡进度条 | 首页 | `progress_history` 白+20% 轨道 | 39 轮 | ✅ |
| 空态 | 收藏/搜索/站点列表 | 3D 盒子+引导文案 | 第 13 轮起 | ✅ |

## 7. 字体与渲染

| 元素 | 出现页面 | 实现 | 实测证据 | 状态 |
|---|---|---|---|---|
| 字体族 | 全局 | 系统默认 sans-serif（Roboto 系；中文走系统回退） | — | ✅ |
| emoji 渲染 | 站名/线路名/卡角标 | 43 轮：`Util.cleanName` 保留 emoji+ZWJ/VS、只删私有区；`androidx.emoji2:emoji2(-bundled) 1.5.0` 内嵌 NotoColorEmoji 全局替换 | 43 轮：改前豆腐块（crop_toast）、改后见台账 | ✅ |
| 页面底色 | 全局 | `qbox_bg` #12141B + `bg_app` 渐变+柔光 | 42 轮 §8 | ✅ |

## 处置记录（43 轮）

1. ⚠️→✅ **循环按钮聚焦态**：41 轮误改「无底衬+加粗」→ 43 轮恢复与全站同一块白药丸（selector_repeat_bg 复用 shape_chip_focused）；去掉的是 selected 12% 白底（shape_chip_activated 对循环不生效），ON 只蓝字。
2. ⚠️ 登记中 **直播 ON 态**（invert/across/change）：仍走 shape_chip_activated 12% 白底，与点播循环「蓝字」不一致——待用户拍板是否统一为蓝字（涉及 LiveActivity 三钮，直播路径未实测，不敢擅改）。
3. ✅ **导航带/栏透明度**：72%→60%（qbox_nav_bar），解决「图标焦点圆边看不见」。

## 处置记录（44 轮）

4. ✅ **设置页方案 B 选定合入主线**（用户：「还是 b 好看一点 选 b 吧」）：主设置页 = 分组标题+行直落 qbox_bg 底、无分组块；方案 A（标题外置+4 块）未采用，留档 `shots/settings-A-main.png`。二级设置页（播放/解码/预载/弹幕）仍为 qbox_panel 分组块，与主页不一致——登记待用户看二级页后拍板是否同步去卡片。
