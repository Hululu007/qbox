# QBox 1.9.19 UI/UX 优化 — 交接状态

> 生成时间：2026-11-04 20:25（Asia/Hong_Kong）
> 当前版本：**1.9.19 / versionCode 77**，已装测试机 `192.168.2.214:5555`
> 详细问题台账：`docs/ui-ux-audit.md`（第 1–9 节 + N/R 编号寄存器）

---

## 0. 一句话现状

1.9.15 之后暴露的 UI/UX 问题已系统性修完两轮：**结构性 bug（焦点逃逸、黑屏、图标位移、列表裁切、删除无提示、搜索 BACK、控制条按钮点不到）全部有提交和实机截图**；
剩余主要是**等待实机截图验证**的几项、以及两个需要设计取舍的 P3、一个启动耗时 P1。

---

## 1. 交付物（已提交、已实机验证）

### 1.1 播放页 / 控制条（本轮最关键的修复）

| 提交 | 内容 | 证据 |
|---|---|---|
| `ea3947a` | **控制条按钮无法聚焦的真因修复**：`showControl()` 同步 `requestFocus` 时按钮还没布局，焦点留在画面，→/← 被当成快进；改为布局完成后 `post` 再请求焦点 | `shots/ctrl-focus1.png`（↓ 后焦点在「暂停」）、`shots/ctrl-focus2.png`（→ 后焦点到「上集」，时间正常前进） |
| `afc7d9a` | 倍速点按循环 `1→1.5→1.75→2→3→0.75→1` + OSD；循环开关 toast「循环：开/关」；「选集」改「章节」且单集自动隐藏；单音轨/视轨不再把唯一轨道置空（黑屏/静音） | 代码已构建；toast 截图待补 |
| `1dd6d58` | 精细倍速收进「更多」二级行（`speedExtra` → `SpeedSettingDialog`），长按主行「倍速」也可进 | `shots/v-more-open.png`（更多展开行：原始/硬解/EXO/**倍速设置**/刷新） |

### 1.2 首页 / 海报墙

| 提交 | 内容 | 证据 |
|---|---|---|
| `afc7d9a` | **海报墙 ↓ 保持列**：`CustomVerticalGridView.focusSearch` 找下一行同列子 View 直接返回（之前 ↓ 跳下一行第 1 个） | `shots/home-3rd-down.png`（第 3 列 → 下一行第 3 列） |
| `97221ec` | 顶栏「直播」胶囊无直播时用 `INVISIBLE` 保留占位，换配置源/异步加载时右侧图标不再整体左移 | `shots/home-stable-bar.png` |
| `344797a` | 首页 ↑ 焦点链（更新推荐 → 最近观看 → 分类条）、暂停/播放文案即时反馈、直播带宽刷新、设置页视觉 | 台账 R1 |

### 1.3 直播页

| 提交 | 内容 | 证据 |
|---|---|---|
| `def72fa` | 错误态底部信息条常驻（「016 CCTV14 … 线路 2」），不再 5s 自动收起 | `shots/live-error-bar2.png` |
| `48a2137` | 列表顶部径向渐隐（滚出顶边的行柔和淡出，不被硬裁）+ 频道列压暗加强（亮色画面下频道名可读） | `shots/live-list-scrim2.png` |
| `39e91a1` | 当前分组/频道加粗标记（Leanback selected 会跟焦点走，不能只靠背景） | 代码已构建；列表截图已确认分组加粗，频道列因"↓/↑ 直接切台"而基本看不到 |
| `be78af6` | 错误态信息条 + 本地化错误文案 | `shots/live-error*.png` |

### 1.4 搜索页

| 提交 | 内容 | 证据 |
|---|---|---|
| `e2f36cf` | 搜索页 BACK 直接退出（之前被输入法/编辑框吞掉，表现为"按 BACK 不退出、偶尔删字"） | 实测一次 BACK 回首页 |
| `e1ba234` | 搜索键盘补 6 个标点键（43→49 = 7×7），不再出现孤零零的「9」 | `shots/search-keyboard*.png` |

### 1.5 收藏页

| 提交 | 内容 | 证据 |
|---|---|---|
| `0bf6147` | 收藏页进页聚焦第一张卡片（不再聚焦返回键）、顶部留白 12→32dp | `shots/favorites*.png` |
| `7a46bb0` | 长按删除模式常驻提示「按 OK 删除当前收藏 · 按 BACK 取消」；删空后回到 3D 空盒子空态（之前留整页黑） | `shots/keep-delete-hint.png`、`shots/keep-delete-empty.png` |

### 1.6 设置 / 推送 / 二级页

| 提交 | 内容 | 证据 |
|---|---|---|
| `210b3d0` | 播放/解码/预载/弹幕四个二级设置页统一圆角面板 + 蓝色竖条 | `shots/settings-sub*.png` |
| `344797a` | 设置主页两列面板、17sp、白药丸焦点 | `shots/settings-home2.png` |
| `3c9417c` | 推送页补标准顶栏（返回 + 「推送」标题），与收藏/设置/搜索一致 | `shots/push-header.png` |

### 1.7 其它已修

- `86b97fc`：详情页焦点顺序、播放错误文案本地化、加载提示。
- `211c5f0`：全站内容间距统一 32dp、推送页焦点视觉。
- `44ebacc`：详情/搜索/对话框焦点陷阱、收藏行距与贴顶。
- `470741e`：直播/壁纸配置移到后台线程加载，首页首帧不等配置。
- **已回滚**：`5f1b1c7` 站点 tab 右缘渐隐（FrameLayout 包裹破坏 `setHeader`，导致搜索页整页空白），`5fd8f55` revert；**N-13 需要非侵入方案**。

### 1.8 自动化回归

`.toolchain\test.ps1` 全绿：
```
QBOX_TEST_OK sites=49
QBOX_FALLBACK_OK sites=49
QBOX_UI_OK source=65px
QBOX_LIVE parsed=5 channels=2662
```

---

## 2. 进行中 / 未完成（按优先级）

### P1 — 需要架构/取舍

| 项 | 状态 | 说明 |
|---|---|---|
| **N-12 冷启动 ~11.5s 首帧 / ~24s 内容** | 已缓解 | 已把直播/壁纸配置移出主线程；剩余耗时是 debug 55MB APK 的类加载 + 复杂布局 inflation，需要启动骨架屏或 release 包验证 |
| **恢复对话框无焦点**（本轮发现） | 未修 | 设置 → 恢复 → 备份列表弹出后**焦点仍留在设置页**，→/↓ 无反应；试过 `onStart` 里 `post requestFocus` 无效。已排查：`BaseAlertDialog` 没有动 window 标志、`ThemeOverlay.QBox.Dialog` 只是配色覆盖；**现象指向弹窗窗口本身没有拿到 window focus**（设置页的「恢复」行还保持灰药丸）。下一步建议在 `RestoreDialog.onStart` 里 `getDialog().getWindow()` 主动 `setFocusable(true)`/`setFlags`，或改成 `BottomSheetDialogFragment`（参考 `TrackDialog`，它的列表焦点是正常的） |

### P2 — 偏源侧 / 低优先

| 项 | 状态 | 说明 |
|---|---|---|
| **N-14 个别源海报空白** | 观察 | 部分源返回占位图（夸父 logo），源侧问题；1.9.1 已修过百度代理坏图 |

### P3 — 设计取舍（需要拍板）

| 项 | 状态 | 说明 |
|---|---|---|
| **R25-01 顶栏胶囊展开左侧图标位移** | 待取舍 | 右对齐容器 + 胶囊展开 → 左侧搜索/直播图标左移 70–93px。方案：① 固定槽位（完全不动，图标间有固定空隙）② 容器左对齐（右侧图标右移）③ 保持现状 |
| **N-13 站点 tab 右缘渐隐** | 已回滚待重做 | 非侵入方案：在 Activity 根布局加 overlay，不动 recycler 父层级 |

### 待补实机截图（功能已实现，验证挂账）

- 倍速点按 toast「倍速 1.5x」
- 循环 toast「循环：开/关」
- 「更多」→「倍速设置」弹窗
- 单音轨/视轨 toast
- 单片（1 集）时「章节」按钮隐藏
- N-21 频道列加粗（因切台即换焦点，基本不可见）

---

## 3. 下一轮建议顺序

1. **补播放页 5 张验证图**：先 ↓ 呼出控制条（**不是 ↑**，↑ 是音量），→ 逐项点按；需要时把设置「播放键」切到「呼出菜单」用 OK 确认，验完切回。
2. **修恢复对话框焦点**（查 `BaseAlertDialog`/`MaterialAlertDialogBuilder` 的 window focus）。
3. **N-13 非侵入渐隐**（Activity 根布局 overlay）。
4. **R25-01 拍板后改顶栏**。
5. **N-12 启动骨架**（只影响首页加载态，不动配置架构）。

---

## 4. 构建 / 测试 / 安装（复用命令）

```powershell
# 构建 debug（armeabi-v7a）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .toolchain\build.ps1 -Abi v7a -Variant Debug
# APK
fongmi-561\app\build\outputs\apk\leanbackArmeabi_v7a\debug\app-leanback-armeabi_v7a-debug.apk
# 安装
.toolchain\android-sdk\platform-tools\adb.exe -s 192.168.2.214:5555 install -r <apk>
# 单测（Robolectric：配置 + 直播）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .toolchain\test.ps1
```

**注意**：build.ps1 经常在 APK 产出后不退出（Gradle daemon 挂着，退出码 1）。判断成功用 **APK 的 LastWriteTime**，然后 kill 任务、手动安装即可。

---

## 5. 测试方法与坑（很重要，接手先看）

### 5.1 遥控器按键模型（实测）

- **↓（KEYCODE 20）= 呼出控制条**（首焦点 播放/暂停）
- **↑（19）= 音量/长按倍速**，不是呼出控制条
- **OK（23）**：设置「播放键」= 暂停/播放（默认）时是播放/暂停并收起控制条；切到「呼出菜单」后是呼出/确认控制条
- **←/→**：控制条隐藏时是**快进/快退**（阶梯加速）；控制条可见且焦点在按钮上时是**行内移动焦点**（`ea3947a` 修复后）
- **BACK**：搜索页直接退出（`e2f36cf`）；全屏播放先回详情

### 5.2 adb 操作注意

- 顶栏胶囊聚焦时会展开、把左侧图标推左（R25-01），**不要用固定坐标点顶栏**，用 DPAD
- 控制条动作行用 →/← 移动，每次按键间隔 ≥500ms，焦点才会落位
- 长按删除：`adb input` 模拟不可靠（Android 9 无 `input motionevent`，`--longpress` 不触发 Leanback 长按）；删除模式提示已加，长按本身需真遥控器验证
- 盒子有时会被库存应用 `com.fongmi.android.tv` 抢前台，先 `am force-stop com.fongmi.android.tv`
- **不要动 192.168.2.130**（另一台电视）；`192.168.2.132` 不是我们的设备

### 5.3 改代码注意

- **不要用 PowerShell `Set-Content -Encoding UTF8` 改 Java**：会写 BOM，javac 报 `非法字符 '\ufeff'`（已踩过，`24 个错误`）；用编辑工具或 UTF-8 无 BOM
- 给 Leanback RecyclerView 加装饰层时**不要改它的父层级**：`setHeader` 依赖直接父子关系（N-13 的教训）
- 改 `strings.xml` 注意 zh-rCN 与 values 两处；`play_chapter` 现在是「章节」

### 5.4 测试机当前设置（2026-11-04 20:25）

- 播放键 = 暂停/播放（已从「呼出菜单」切回）
- 无痕模式 = 关
- 图片尺寸 = 特大
- 缓存 = 15.5 MB
- 首页配置源 = 豆豆｜片单（49 站点）
- 收藏页为空（收藏按配置隔离，切换配置源后原收藏不显示，属正常）

---

## 6. 版本与仓库

- **app**：`fongmi-561/`（私有 `qbox-tv`，分支 `ui-cinema`），1.9.19 / 77
- **docs**：本仓库，台账 `docs/ui-ux-audit.md`，本文件 `docs/handoff-status.md`
- 本轮会话 app 提交：`344797a` → `ea3947a`（见 `git log --oneline -20`）
- 未提交：无（`RestoreDialog.java` 的失败实验已回滚；`.gitignore/AGENTS.md/README.md/.github/` 是另一个 agent 的改动，勿动）
