# QBox 颜色清单与合并方案（第 37 轮 P0-2）

> 扫描范围：`fongmi-561/app/src` 下 leanback 资源（colors.xml、drawable、layout、color state list）+ Java 色常量（`0xAARRGGBB`、`Color.parseColor`）。
> 类别：**文**=文字色 / **底**=背景·底衬 / **边**=描边 / **调**=强调·功能色。
> 本文件是后续迭代的合并依据；改动需与 GOLDEN-STANDARDS GS-01 一致（交互态中性色承担，品牌色唯一）。

## 1. 现有颜色总表

### 1.1 文字色（去重后 13 个，是「颜色太多」的重灾区）

| 色值 | 出处 | 用在哪 | 类别 | 合并建议 | 风险 |
|---|---|---|---|---|---|
| `#FFFFFF`（qbox_text） | colors.xml；全站 | 主文字、tab 未选（hero_tab_text）、焦点药丸底 | 文/底 | **保留 = L1 主文字** | 无 |
| `#A6A6B0`（qbox_text_dim） | colors.xml；副标题、tab 选中（hero_tab_text）、时钟 | 次级文字 | 文 | **并入 L2 `#C9C9D1`**（tab/chip/时钟）；面板内副标题保留为 L3 场景见 §3 | 低：面板副标题变亮一点 |
| `#8C8C98`（qbox_text_mute） | colors.xml；搜索 hint、说明文字 | 三级说明 | 文 | **保留 = L3 辅助说明**（仅深色面板上） | 无 |
| `#C9C9D1` | color/text.xml、control_toggle_text.xml 普通态 | 列表项、开关按钮普通态 | 文 | **保留 = L2 次文字** | 无 |
| `#B3FFFFFF` | color/text_dim.xml 普通态 | 直播分组/频道次级 | 文 | 并入 L2 `#C9C9D1`（近似，差 3%） | 低 |
| `#A9A9B6` | styles.xml:38（Control 风格默认） | 播放控制按钮普通态 | 文 | 并入 L2 `#C9C9D1`（近似） | 低 |
| `#FF9A9AA6` | activity_setting.xml 副标题 | 设置副标题 | 文 | 并入 L3 `#8C8C98`（近似） | 低 |
| `#E7E7EE` | adapter_history、adapter_vod_rect 卡片名 | 卡片标题 | 文 | 并入 L1 `#FFFFFF`（近似，差 3%） | 低 |
| `#DCDCE4` | adapter_history 备注 | 卡片备注 | 文 | 并入 L2 `#C9C9D1`（近似） | 低 |
| `#E8E8EC` | adapter_vod 卡片名 | 卡片标题 | 文 | 并入 L1 `#FFFFFF`（近似） | 低 |
| `#0B0B0F`（qbox_focus_text） | colors.xml；焦点白药丸上的反色字 | 焦点态反色 | 文 | **保留 = 焦点反色**（不是彩色，是形态必需） | 无 |
| `#66FFFFFF`/`#80FFFFFF`/`#99FFFFFF`/`#E6FFFFFF`（white_40/50/60/90） | colors.xml + 直播/统计 | 直播次级、统计、时钟（新） | 文 | 保留 white_50 一档给「降存在感的文字」（时钟），其余并入 L2/L3 | 低 |
| `#4C8DFF` | VideoActivity.java 常量 | 字幕/CC 或残留强调 | 调 | **并入 `#5B8CFF`**（GS-01 曾点名它） | 中：需核对控件身份后替换 |

### 1.2 背景/底衬色

| 色值 | 出处 | 用在哪 | 类别 | 合并建议 | 风险 |
|---|---|---|---|---|---|
| `#000000`（qbox_bg） | colors.xml、bg_app | 全局背景 | 底 | 保留 | 无 |
| `#0B0B0E`（qbox_bg_soft） | colors.xml | 软背景 | 底 | 保留 | 无 |
| `#17171C`（qbox_surface） | colors.xml、GS-06 弹层 | 弹层/面板底 | 底 | 保留 | 无 |
| `#232329`（qbox_surface_hi） | colors.xml | 面板高亮层 | 底 | 保留 | 无 |
| `#2B2B33`（qbox_line） | colors.xml | 分隔线 | 边 | 保留 | 无 |
| `#B8000000`（qbox_panel，72% 黑） | colors.xml；shape_controller 中档、导航栏主体 | 播放面板材质中档、导航「栏」 | 底 | **保留 = 面板材质唯一 token**；P1-3 各设置/轨道层背景统一用它 | 无 |
| `#7A/#E6000000` | shape_controller 渐变两端 | 控制条面板上下缘 | 底 | 保留（面板渐变组成部分） | 无 |
| `#F0090A0D`（qbox_header_scrim） | colors.xml | 顶栏实底备用 | 底 | 与 qbox_panel 并存；建议下轮评估是否并掉 | 低 |
| `#090A0D` 系（导航渐变带 19 切片、氛围收边 shape_backdrop_scrim_*） | shape_nav_scrim、shape_backdrop_scrim_a-d3、shape_detail_scrim* | 导航渐变带、氛围图收边 | 底 | 保留为「氛围层」专用基色；不与面板 token 混用 | 无 |
| `#00000000` 透明 | 各处 selector | 普通态透明底 | 底 | 保留 | 无 |
| `#12141B` | shape_settings_panel | 设置面板底 | 底 | 并入 qbox_surface `#17171C`（近似） | 低 |
| `#05060A`/`#0D0F15` | bg_app 渐变 | 全局背景渐变 | 底 | 保留（背景渐变组成） | 无 |
| `#08080B`/`#101014`/`#17171D` | shape_wall_premium | 壁纸页 | 底 | 下轮并入 surface 系 | 低 |
| `#4D101014` | shape_hero_tab_normal（tab 普通态底） | tab 普通态底衬 | 底 | **删除**：tab 普通态恢复透明（用户已否底衬类），选中/焦点交给白药丸 | 中：静态 tab 在亮海报只靠文字阴影（现状即如此） |
| `#1E2B4D`（qbox_accent_soft） | colors.xml、shape_chip_activated 等 | 激活 chip 底（蓝黑） | 底 | 保留为「激活底」唯一用法，或并入白系激活底（见 §3 风险注） | 中：涉及 N-28 开关态规范 |

### 1.3 白 alpha 底衬/描边（selector 层，归并为 4 档）

| 现值 | 用在哪 | 类别 | 合并为 |
|---|---|---|---|
| `#14FFFFFF`（hero_tab 普通底、selector_action_chip 等） | tab/chip 普通底 | 底 | **白衬档 1 = 8% `#14FFFFFF`** |
| `#1FFFFFFF`（shape_chip_activated、selector_seek_focus、shape_item_activated 等 9 处） | 激活/焦点底 | 底 | **白衬档 2 = 12% `#1FFFFFFF`** |
| `#24FFFFFF`（filter chip 选中底）/ `#26FFFFFF`（bg_app、toggle_on、stats 等 7 处） | 选中底/卡片底 | 底 | **白衬档 3 = 15% `#26FFFFFF`** |
| `#33FFFFFF`（描边、删除 hint 等 5 处）/ `#4DFFFFFF`（filter 选中描边）/ `#66FFFFFF`（mic/seek 描边） | 描边 | 边 | **白描边档 = 30% `#4DFFFFFF`**（33→4D 略升，66→4D 略降，视觉无差） |

### 1.4 黑 alpha 遮罩（归并到 black 阶梯）

colors.xml 已有 black_05→black_90 阶梯。drawable 里的散值（`#0D/#1A/#26/#4D/#73/#99/#B3/#CC000000` 等）多为氛围收边与列表遮罩组成，**保持现状**（属氛围层，不是 UI 控件色）；新增控件遮罩一律取 black 阶梯 token。

### 1.5 强调/功能色

| 色值 | 出处 | 用在哪 | 类别 | 合并建议 | 风险 |
|---|---|---|---|---|---|
| `#5B8CFF`（qbox_accent） | colors.xml | logo、设置分区竖条、链接语义 | 调 | **保留 = 唯一强调色，兼作「标记色」**（进度条选中态、轨道选中标记） | 见 §3 对比度 |
| `#4C8DFF` | VideoActivity.java | 残留（N-37 点名过） | 调 | 并入 `#5B8CFF` | 中：先核对控件身份（GS-56） |
| `#1E2B4D`（qbox_accent_soft） | colors.xml | 激活 chip 蓝黑底 | 底/调 | 保留（激活底）或下轮评估改白系 | 中 |
| `#404040/#40C090/#4870E0/#48B0C0` | CustomWallView.java | 壁纸缩略预览图色块 | 调 | 不属 UI 控件色（内容图），不动 | 无 |
| `#D9000000` | shape_delete_hint | 删除提示描边 | 调 | 并入 black_85 | 低 |

## 2. 分类归并结论

- **近似重复可直接删除**：`#B3FFFFFF`、`#A9A9B6`、`#FF9A9AA6`、`#E7E7EE`、`#E8E8EC`、`#DCDCE4`、`#4C8DFF`、`#12141B`、`#4D101014`（9 个）。
- **文字色**：13 → **4 档**（L1 白 / L2 `#C9C9D1` / L3 `#8C8C98` / 焦点反色 `#0B0B0F`）+ white_50 专用降档（时钟）。
- **白 alpha 衬/边**：10+ 散值 → **4 档**（8%/12%/15% 底衬 + 30% 描边）。
- **品牌色总数（合并后）**：**17 个** =
  文字 5（L1/L2/L3/反色/white_50）+ 底色 6（bg/bg_soft/surface/surface_hi/line/panel）+ 强调 2（accent/accent_soft）+ 白衬描边 4（8%/12%/15%/30%）。
  氛围层（导航渐变带、海报收边、列表遮罩的 `#090A0D` 系与 black alpha 阶梯）为内容适配层，不计入品牌色、不新增 token。

## 3. 标记色选定（进度条选中态 / 轨道选中标记）

**选定 `qbox_accent #5B8CFF`**，理由：
1. 合并后唯一强调色——「标记」语义天然归它，不需要再发明一支浅蓝（发明新色=品牌色 +1，与收敛目标相悖）。
2. 对比度实测（WCAG）：`#5B8CFF` 相对亮度 0.280；对 qbox_surface `#17171C`（0.011）= **5.4:1**，对纯黑 = 6.6:1，对 qbox_panel 72% 黑叠亮海报最坏合成（≈0.065）= **4.0:1**——进度条是大色块（非正文），≥3:1 即清晰可辨。
3. GS-01 原限制「蓝仅留 logo/竖条/链接」正是为「强调唯一」服务；进度条选中与轨道选中属于同一强调语义，不违反 GS-01，需在 GS-01 补一条合法用法登记。
4. 白色在选中态已被「焦点白药丸」占用；用白做标记色会与焦点态混淆——蓝色把「选中」与「焦点」两个通道分开。

风险注：机顶盒 RGB 渲染下 `#5B8CFF` 在 72% 黑上偏暗是可接受的（大色块）；若用户实机嫌暗，备选 `#7AA7FF`（同族提亮，对比度 6.8:1），但默认先用合并色板内的 `#5B8CFF` 不新增色。

## 4. 落地结论

**本轮就改**：
- tab/chip/控制按钮/卡片文字 → L1/L2 两档白（hero_tab_text 语义修正：选中=白、未选=L2，与 tab_text 对齐）。
- 时钟 → white_50 + 14sp（保留颜色、降存在感）。
- 进度条选中态 → `#5B8CFF` 变色（撤销缩放与白框，P0-3）。
- `#4C8DFF` 残留 → `#5B8CFF`（先核对控件身份）。
- 各设置/轨道层背景 → qbox_panel（P1-3）；轨道选中标记 → `#5B8CFF`。

**下一轮再动**：qbox_accent_soft 激活底是否改白系（涉 N-28 规范）、qbox_header_scrim 是否并掉、壁纸页底色并 surface 系、`#1E2B4D` 激活 chip 底换标记色系。

**替换映射表（本轮执行部分）**：

| 旧值 | 新值 | 范围 | 进度（第 38 轮） |
|---|---|---|---|
| `#A6A6B0`（tab/chip/时钟场景） | `#C9C9D1`（L2）/ 时钟 white_50 | hero_tab_text、tab_text、activity_home/vod 时钟 | ✅ 已替换 |
| `#B3FFFFFF` | `#C9C9D1` | color/text_dim.xml | ✅ 已替换 |
| `#A9A9B6` | `#C9C9D1` | styles.xml 节标题 | ✅ 已替换 |
| `#E7E7EE` / `#E8E8EC` | `#FFFFFF` | adapter_history、adapter_vod、adapter_vod_rect | ✅ 已替换 |
| `#DCDCE4` | `#C9C9D1` | adapter_history 备注 | ✅ 已替换 |
| `#4C8DFF` | `#5B8CFF` | VideoActivity | ✅ 核查：仅存在于注释，无代码引用 |
| `#4D101014` | 删除（tab 普通态透明） | shape_hero_tab_normal.xml | ✅ 已删文件（无引用） |
| `#FF9A9AA6` / `#9A9AA6` | `#8C8C98`（L3） | activity_setting.xml 副标题/图标 tint ×7 | ✅ 已替换 |
| `#12141B` | `@color/qbox_surface` | shape_settings_panel.xml | ✅ 已替换 |
| `#D9000000` / `#33FFFFFF`（delete_hint） | `#CC000000` / `#4DFFFFFF` | shape_delete_hint.xml | ✅ 已替换 |
| `#24FFFFFF`（filter chip 选中底） | `#26FFFFFF`（档 3） | selector_filter_chip.xml | ✅ 已替换 |
| `#29FFFFFF`（track chip 描边） | `#26FFFFFF` | shape_track_chip.xml | ✅ 已替换 |
| `#33FFFFFF`（filter chip 常态描边） | `#4DFFFFFF`（描边档） | selector_filter_chip.xml | ✅ 已替换 |
| `#1E2B4D`（accent_soft 激活底） | 保留（用户确认强调色留 2 个） | — | ⏸ 不动 |
| 壁纸页 `#08080B/#101014/#17171D` | 下轮并 surface 系 | shape_wall_premium | ⏭ 下轮 |
| `qbox_header_scrim` | 下轮评估并掉 | colors.xml | ⏭ 下轮 |
| 进度条已播放/圆球（选中态） | `#5B8CFF` 标记色 | VideoActivity styleSeekBar/focus 监听 | ✅ 已落地（P0-3） |

替换后验证：首页（`r38_home.png`）、分类筛选页（`r38_filter_open.png`）实测无掉对比度、无死白色块；tab 两档白（选中 L1/未选 L2）在实色栏上清晰。
