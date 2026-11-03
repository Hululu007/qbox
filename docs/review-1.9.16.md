# QBox 交互 / UI 全量 Review（1.9.15 → 1.9.16）

> 审查方式：真机 adb 操作 + UI 层级导出 + logcat 打点 + 全量源码走查。
> 测试机：JmGO 投影（Android 9 / 1920×1080 / density 2.0 / armeabi-v7a，`192.168.2.132`）、
> IP810N 盒子（Android 9 / 1280×720 / density 1.0，`192.168.2.214`）。
> 结论：**没有"大 bug"，但有一批同源的小问题** —— 焦点路由靠 Leanback 的 `focusSearch` 猜、
> 而不是显式指定；面板收起后不交还焦点；`dispatchKeyEvent` 里 `return true` 吞键。
> 这三类占全部问题的 70%。

---

## 一、P0：遥控器会"卡死"或功能完全不可达（本轮已全部修复）

| # | 页面 | 现象 | 根因 | 修复 |
|---|---|---|---|---|
| 1 | 播放页 | 焦点移到进度条后按返回，**没有任何反应**，退不出控制条也退不出全屏 | `VideoActivity.onSeekBarKey()` 的 `default: return true` 把返回键吞掉，`onBackInvoked` 永远收不到 | default 改 `return false`，只处理 ←/→/↓/OK |
| 2 | 播放页 | 快进过一次后，按菜单键呼出控制条，**一个按钮都点不到**（焦点在画面上） | `getFocus2()` 只校验 `isShown()`，快进时 `mFocus2` 被记成 `video`（画面层），于是恢复目标变成画面 | `getFocus2()` 增加 `isInside(control.getRoot(), mFocus2)` 校验；`mFocus2` 只在 `panelFocused` 时记录 |
| 3 | 播放页 | 控制条 5 秒自动收起后，**第一下 ←/→ 会直接快进 10s、第一下 OK 直接暂停**，要先按一次唤醒 | `hideControl()` 只把面板置 GONE，焦点被系统回收，`getCurrentFocus()` 变 null | `hideControl()` 末尾把焦点交还画面层 |
| 4 | 直播页 | 加密分组（`mHides` 非空）**永远解不开**，连按 100 次也不弹密码框 | `onChildSelected()` 每次选中都调 `resetPass()`，把 `onItemClick` 刚 `++` 的计数清零，计数永远等于 1 | 只在「分组真的变了」时清零（新增 `mLastGroup`） |
| 5 | 投屏页 | 进页面后**遥控器完全失效**：没有焦点框，OK 没反应，只能返回 | `activity_push.xml` 里二维码和"推送剪贴板"都没 `focusable`，整棵树没有可聚焦控件 | 两个控件补 `focusable` + 焦点态背景，进页显式 `requestFocus` |
| 6 | 首页 | ①内容区第一排按 ↑ 出不去 ②顶栏按 ↓ 直接跳过分类条掉进内容区 ③上下方向错乱 | `dispatchKeyEvent` 用 `recycler.hasFocus() && getSelectedPosition() <= 0` 判断"第一行"，但选中位置 0 是不可聚焦的分组标题、1 才是第一行，条件永远不成立；`setOnKeyListener` 在焦点位于子 View 时根本不触发 | 改为「谁持有焦点 → 显式指定下一个目标」，判据改成 `selectedPosition <= 1`；删掉失效的 OnKeyListener |

**首页焦点链路修复后的实测（132，logcat 佐证）**

```
key=20 src=true  tabs=false  → 进分类条
key=20 tabs=true content=false → 进内容区（sel 1）
key=19 content=true top=false → 内容区内上移
key=19 content=true top=false → 回分类条
key=19 tabs=true → 回顶栏
```

---

## 二、P1：明显影响体验（按页面）

### 首页

| 现象 | 根因 | 建议 |
|---|---|---|
| 启动后首焦点**不稳定**：有时在顶栏源名、有时在内容区第一张 | `initConfig()` 400ms 的 `source.requestFocus()` 与 `showContent()` 300ms 的 `recycler.requestFocus()` 竞争，且列表可能还没布局完 | 已给 `setFocus()` 加一次 200ms 重试；建议再统一成"只在内容就绪后聚焦一次" |
| 氛围底图轮询可能**叠加多个 600ms 循环** | `onResume` 每次都 `App.post(mBackdropTask, 700)`，同一个 Runnable 被 post 多次会排队，每轮又自续 | `onResume` 里先 `App.removeCallbacks(mBackdropTask)` |
| 分类条数据未到（`typeSize=0`）时按 ↓ 直接进内容区，之后用户找不到分类条 | 加载时序；兜底逻辑本身没错 | 空分类时不保留 42dp 空白，或给"加载中"占位 |
| 有「最近观看」时，从推荐第一排按 ↑ 先到历史行、再到分类条 | 数据结构如此（历史行在推荐上方） | 符合直觉，无需改 |

### 分类页（VodActivity / TypeFragment）

| 现象 | 根因 | 建议 |
|---|---|---|
| 切到别的分类再切回来，**列表被强制滚回顶部**，位置丢失 | `TypeFragment.setUserVisibleHint()` 每次可见都 `moveToTop()`（已废弃 API） | 只在首次显示时回顶；或完全保留位置 |
| 收起筛选后**焦点悬空**，方向键要按两次才回来 | `updateFilter(false)` 不恢复焦点，而筛选项行已被 `removeItems` | 收起后 `mBinding.typeRecycler.requestFocus()` |
| 展开筛选后焦点被**强制甩到网格**，用户只想展开看看也不行 | `updateFilter(true)` 里 `App.post(this::focusGrid, 90)` | 只在用户从筛选行按下时才跳 |
| 所有分类 Fragment **从不销毁** | `PageAdapter.destroyItem()` 空实现 | 恢复 `super.destroyItem()`，或改 `setOffscreenPageLimit(1)` + 真正销毁 |
| 首焦点被请求两次 | `setRecyclerView()` 里同步 `requestFocus()` + `post(200ms)` 再来一次 | 删掉同步那次 |
| `getFragment()` 用 `instantiateItem` 强取，可能创建多余 Fragment | 每次调用都可能实例化 | 用 `findFragmentByTag` / `getItem` |

### 搜索 / 聚合

| 现象 | 根因 | 建议 |
|---|---|---|
| 热词区首行按 ↑ **出不去**，返回键直接退页面 | `handleWordKey` 在首行且无记录区时 `return true` 吞键 | 首行 ↑ 回搜索框 |
| 快速输入时，**旧建议覆盖新建议** | `getSuggest` 回调无请求序号，晚到的旧响应直接 setAdapter | 加 `int seq` 校验 |
| 刚进页面按 ↓ 进键盘**第一次没反应** | `focusFirst` 取 `getChildAt(0)` 时列表还没布局 | 先吃键，`post` 到下一帧再聚焦 |
| 焦点路过麦克风就**弹录音权限/拉起语音页** | `CustomMic.onFocusChanged(gainFocus) → start()` | 只在 OK 键点击时启动录音 |
| 聚合搜索**整页纯黑**：没 loading、没"无结果" | `CollectFragment` 有 `ProgressLayout` 但从不调用 `showProgress/showEmpty` | 补状态机 |
| 站点 tab 上按 ↓，焦点跑到**屏幕外的第 0 页** | `focusGrid()` 里 `getFragment()` 写死第 0 页 | 用 `pager.getCurrentItem()` |
| 切 tab 时页面**连闪**、退出后还有一次延迟跳页 | `App.post(mRunnable, 100)` 未在 `onDestroy` 移除 | 移除 + 改平滑 `setCurrentItem(pos, true)` |
| 聚合页 tab 看不出"当前在哪个站点" | `CollectAdapter` 只处理焦点态，没有 selected 态 | 补 `setSelected` |

### 设置页

| 现象 | 根因 | 建议 |
|---|---|---|
| 播放器里切了 MPV，进设置页**还显示 Exo 专属项** | `SettingPlayerActivity.onResume` 只刷新文案，不调 `setVisible()` | onResume 补 `setVisible()` |
| MPV 下仍显示「杜比视界输出」，改了没效果 | `setVisible()` 漏了这个控件 | 一并 GONE |
| 关掉某个总开关后，下面几行消失，**焦点悬空** | 多处 `setVisible()` 无焦点兜底 | 统一封装：隐藏后若焦点在被隐藏行则回退首行 |
| 设置页看不到当前内核 | `player` 行没有 value TextView | 补 `playerText` |
| 「壁纸·默认」点了页面文字不变 | 只 `Setting.putWall()`，没刷新文案 | 立即更新或发 `ConfigEvent` |
| `getDohList()[0]` 可能数组越界崩溃 | 空列表时直接取下标 | 判空 |

### 列表与对话框

| 现象 | 根因 | 建议 |
|---|---|---|
| 收藏页删到最后一条：**整页空白、蒙层也不见了**，焦点每次删除都丢 | `onItemDelete` 只处理 `count==0 → setDelete(false)`，无空态、无焦点兜底 | 补 `showEmpty` + `requestFocus` |
| 收藏页进页面首焦点在**返回图标**上，按 OK 直接退出 | 没有 `requestFocus` | 列表就绪后聚焦第一项 |
| 历史对话框删除后焦点掉到 RecyclerView 容器（无焦点框） | `notifyItemRemoved` 后没有转移焦点 | 焦点交给相邻项 |
| 更新对话框下载中**返回键无效**，按钮被禁用后焦点留在禁用控件上 | `setCancelable(false)` + `setEnabled(false)` | 允许取消；禁用后焦点移到 cancel |
| 换源对话框「全部」模式下，焦点能落到被禁用的「全选」上 | 只 `setClickable(false)` 没 `setFocusable(false)` | 同步设置 |
| 文件页拒绝存储权限后**不提示，直接进空目录**，且不显示当前路径 | lambda 忽略 `allGranted` | 空态提示 + 顶部路径文本 |

### 播放页（剩余项）

| 现象 | 根因 | 建议 |
|---|---|---|
| 遥控器 ⏩/⏪ 媒体键**完全没反应** | `controller().seekForward()` 依赖 `setSeekForwardIncrement`，全仓库没设置过，ExoPlayer 直接 return | 改成 `seekBy(±10000)` |
| 连按快进时控制条会在**按住过程中**闪一下再出现，位置多跳十几秒 | `mR1` 与 `mHideControl` 两个 Runnable 指向同一个 `hideControl`，互不 remove；`onSeekEnd` 的 1.8s 定时不会被后续按键取消 | 合并成一个 Runnable + `hideControl` 里判断 `seeking`（已加保护） |
| 片尾 ENDED 控制条自动收起，5 秒后焦点丢失 | `showControl(replay)` 后照常 `setR1Callback()` | ENDED 常驻不自动收 |
| 直播/直链（duration=0）进度条仍能聚焦、能 seek，导致反复缓冲 | `seekBy` 里 `duration<=0` 时不做上限，`seekBox` 也没按 `isVod()` 禁用 | duration<=0 时 `seekBox.setFocusable(false)` |
| 「更多」展开指针错乱：快进借出控制条后第一次点「更多」是"收起" | `enterSeekMode()` 直接 `extra.setVisibility(GONE)`，绕过 `setExtra` 的文案更新 | 统一走 `setExtra` |
| 调完倍速/音轨/选集，返回后控制条是关的、焦点也没了 | 每个 `onXxx()` 都是 `dialog.show()` 后立刻 `hideControl()` | 去掉 `hideControl()`，或 `setOnDismissListener(() -> showControl(原按钮))` |
| 暂停时长按 ↑ 呼不出控制条，还顺手把倍速重置了 | `onSpeedUp()` 里 `if (!isPlaying) return`，但 `changeSpeed` 已置 true | `onSpeedUp` 返回 boolean 决定是否进入加速态 |

### 直播页（剩余项）

| 现象 | 根因 | 建议 |
|---|---|---|
| 长按 OK 想收藏频道，结果**控制条弹出、焦点跳到设置** | `dispatchKeyEvent` 对 `repeatCount>0` 无条件 `showControl()` 并吞掉后续 UP | 只在"列表和控制条都不可见"时才当长按呼出 |
| 打开频道列表→切台→按返回，只关信息条，**要按两次**才关列表 | `onBackInvoked` 判断顺序 control → bottom → recycler | recycler 提到 bottom 之前 |
| 在频道列表对**非当前播放**频道按 →，节目单打不开且无提示 | `showEpg()` 要求 `item.isSelected() && equals(mChannel)` | 改为按"焦点所在频道"渲染，空数据给 toast |
| 「更多」收起后焦点环消失 | `setExtra(false)` 无焦点兜底 | 收起后聚焦 `more` |
| 只有 1 个分组时，**不恢复上次频道**，每次从第 1 个开始 | `setPosition(int[])` 在 `size==1` 时整体 return | 只跳过 `setSelectedPosition` |

---

## 三、P2：打磨项（不影响功能，影响"精致感"）

1. **两套品牌蓝**：进度条/seekbar 写死 `0xFF4C8DFF`，token `qbox_accent` 是 `#5B8CFF`；设置页 8 处 `app:tint="#6E6E78"`，token `qbox_text_mute` 是 `#8C8C98`。
2. **两套白**：`adapter_vod.xml` 标题 `#E8E8EC`、`adapter_history.xml` `#E7E7EE`。
3. **空态文案硬编码中文**：「换个分类，或按返回上一级」写死在 `view_empty.xml`，所有场景复用。
4. **搜索框焦点态实际不生效**：`selector_search_field` 依赖子 View 状态，`field` 没有 `addStatesFromChildren`，代码里手动切背景代替。
5. **滚上去的行被硬裁成"半截海报"**：`clipTop=114dp` 直接 `clipRect`，上一行滚到分类条下面时齐刷刷切断（720p 实测整排只剩下半截）。建议改成 8~16dp 的渐变遮罩（`LinearGradient` shader 或叠一层 `shape_top_fade`），视觉上是"淡出"而不是"切断"。
6. **历史行卡片过小**（720p 实测约 160×90），与下方 7 列海报视觉权重失衡；且只有 1 条历史时整行很空。
7. **720p 7 列 / 1080p 5 列**：列数只按宽度算，没考虑观看距离与卡片可读性；建议按 `smallestScreenWidthDp` 分档，720p 保持 6 列（卡片约 180dp 宽）。
8. **详情页信息层级**：「倒序 → 1-7」这行结构含混（两个语义挤一行）；元信息全灰，主次不分；小窗播放器无圆角直接黑块压在海报上。
9. **播放页动作行 14 个按钮**：720p 已经挤到屏幕边缘，文案再长就会溢出（`findFocusable` 用 `isShown()` 判断，被裁掉的按钮仍可聚焦，焦点会走到屏幕外）。建议：常用 8 个 + 「更多」二级。
10. **无障碍**：投屏页二维码、搜索页语音按钮、设置页图标已有部分 `contentDescription`，但列表卡片、播放页按钮普遍缺失；TalkBack 下焦点顺序未验证。
11. **启动页无反馈**：实测 214 上冷启动出现过 20 秒纯 logo 黑屏（配置拉取慢），期间没有任何"正在加载配置"的提示。

---

## 四、UI/UX 优化建议（10-foot 视角，按投入产出排序）

| 优先级 | 事项 | 理由 |
|---|---|---|
| ★★★ | 焦点态统一成一套：**整块白药丸**（顶栏/设置/按钮）与**白描边**（海报卡片）各司其职，不要出现第三种（半透明白底） | 现在三种焦点视觉混用，用户分不清"这个能不能点" |
| ★★★ | 列表顶部边缘改成**渐隐**而不是硬裁 | 硬裁让滚动的第一行看起来像"渲染错误"，这是最容易被感知的"廉价感"来源 |
| ★★☆ | 顶栏 chip 已能"聚焦展开、不推挤"（实测 46dp→184dp，向左长），可复用到其它页面做一致性 | 现状是对的，别改坏 |
| ★★☆ | 播放页控制条分级：一级 `播放/上下集/选集/倍速/字幕/更多`，二级收 `音轨/视轨/片头/片尾/循环/详情/统计` | 720p 14 个按钮已到极限 |
| ★★☆ | 统一返回键层级规范并在所有页面落地：`面板 → 列表顶 → tab → 顶栏 → 退出` | 现在的返回行为各页不一致，是"用久了很烦"的主因 |
| ★☆☆ | 历史行卡片放大到 16:9 的 240×135dp，与海报行共享行高节奏 | 提升首页信息层级 |
| ★☆☆ | 空态/错误态/加载态统一组件（图标 + 主文案 + 副文案 + 重试） | 现在各页面各写各的 |

---

## 五、本轮代码改动（已编译通过，132/214 已装机验证）

| 文件 | 改动 |
|---|---|
| `HomeActivity.java` | 焦点路由重写（顶栏/分类条/内容区显式指定）、`isRecyclerAtTop` 判据改 `selectedPosition<=1`、删除失效 OnKeyListener、首焦点加重试 |
| `VideoActivity.java` | `onSeekBarKey` 不再吞返回键、`getFocus2` 校验面板归属、`hideControl` 交还焦点 + `seeking` 保护 + `force` 重载 |
| `LiveActivity.java` | 加密分组计数只在换组时清零 |
| `PushActivity.java` + `activity_push.xml` + 3 个 strings | 二维码/剪贴板按钮可聚焦、进页 requestFocus、二维码与文案地址一致、无浏览器时不闪退 |
| `selector_qr_code.xml`（新增） | 二维码焦点描边 |

**已撤回的实验改动**：`CustomVerticalGridView` 的 `setWindowAlignment(WINDOW_ALIGN_HIGH_EDGE)` 与 `snapRow()`。
两者都会把聚焦行硬贴到内容区顶部，实测把第一行滚到 `y=29px`（越过 `paddingTop=144dp`），上下键行为紊乱。
"整行吸附"这个诉求本身合理，但必须用**滚动阶段的轻量补偿**实现，不能改 windowAlignment，也不能在 `onFocusChanged` 里 `scrollBy`。

---

## 五之二、实测通过（供回归对照）

在 132（1080p）与 214（720p）上用 adb 逐键验证：

- 首页：`顶栏 ↓ 分类条 ↓ 内容 ↑ 历史行 ↑ 分类条 ↑ 顶栏` 全部逐级命中，分类条焦点是白药丸、海报是白描边。
- 首页：内容区 ↓ 能逐行下移并滚动，↑ 能逐行上移。
- 投屏页：进页面即有焦点框，↑↓ 可在二维码 / 剪贴板之间切换。
- 播放页：`↑` 呼出控制条 → `↑` 焦点进进度条（白色矩形框）→ `←/→` 正常 seek（30:20 → +20s）→ `BACK` 不再卡死。
- 直播页：分组焦点移动不再把密码计数清零（需真实加密源回归）。

**注意**：`uiautomator dump` 的 `bounds` 在 Leanback 列表上不可靠（同一焦点会返回旧坐标），
本轮的焦点判断以**截图里的焦点框**和**代码内打点日志**为准，不要拿 dump 的坐标下结论。


---

## 六、建议的下一轮顺序

1. **回归播放页**：`screencap` 逐帧确认"进度条聚焦 → 返回"、"快进后菜单键"、"暂停时长按上"三条（改动已在，缺完整回归）。
2. **列表框架收口**（见第七节）：这是"海报截断 / 焦点锁死 / tab 乱跳"的唯一根治办法，优先于任何局部修补。
3. **搜索页三连**：首行 ↑ 回搜索框、建议词请求序号、麦克风改点击触发。
4. **设置页显隐/焦点兜底**统一封装。
5. **返回键层级**统一（写一个 `BackDispatcher` 工具类，各页只声明自己的层级）。

---

## 七、你列的那些症状，根因是同一个

你说的这些 —— 海报被截断、海报锁死不动、不知道选中了哪里、焦点框被 tab 截断、
tab 位置乱跳、海报展示逻辑不一致 —— **不是 6 个 bug，是 1 个架构问题**：
列表的「内容起点 / 裁切线 / 焦点放大 / 滚动吸附」四个变量分散在三个地方各自实现、互相耦合，
每修一个就要重调另外三个，所以永远在"好了这个、坏了那个"。

| 症状 | 直接原因 | 根因 |
|---|---|---|
| 海报被截断 | `CustomVerticalGridView.dispatchDraw()` 里 `canvas.clipRect(0, clipTop, ...)` 硬裁 | 用"裁绘制"解决"上一行露出半截"，裁切线又必须避开焦点环，两个需求打架 |
| 焦点框被 tab 截断 | 裁切线 `qbox_header_clip` 与放大后的焦点环顶边只差几 px（120→114→128 调过三次） | 同上：焦点环超出 item 边界，只能靠"裁切线刚好让开"来躲 |
| 海报锁死不动 / 不知道选中哪 | `snapRow()` 在 `onChildViewHolderSelected` 和 `onFocusChanged` 里 `scrollBy`，与 Leanback 自己的滚动抢控制权 | 两套滚动逻辑同时改 `scrollY`，焦点位置与视觉位置脱节 |
| 设置里改海报大小后展示错乱 | `Product.getColumn(style)` 改了列数/行高，但 `paddingTop`、裁切线、滚动位置都是常量 | 尺寸变了没有"重建列表 + 复位滚动"这一步 |
| 不同场景 tab 位置乱跳 | 首页 tab（toolbar 下固定 42dp）、分类页 tab（activity_vod）、搜索页站点 tab（pager 内 32dp inset）三套独立布局 | 没有共用的 header 组件，高度靠各处手写 |
| 海报展示逻辑不一致 | 首页 `addGrid`、分类页 `addGrid`、聚合页 `CollectFragment` 各写一份分页/列数逻辑 | 没有统一的"分页网格"封装 |

### 唯一能根治的做法

**1）把「内容起点」变成算出来的，不是写死的。**
现在 `dimens.xml` 里四个常量必须手工对齐：
`qbox_header_pad_top 22dp` + `qbox_header_content 46dp` + `qbox_tabs_gap 6dp` + `qbox_tabs_height 42dp` = 116dp，
而 `qbox_content_top` 写的是 144dp（多出的 28dp 是"给焦点放大留的呼吸位"），
`qbox_header_clip` 又是 114dp / 128dp（取决于是谁改的最后一版）。
→ 改成：一个 `QboxHeaderMetrics`，`contentTop = headerBottom + focusBleed`，`focusBleed = 卡片宽 × (zoom-1) / 2 + 环宽/2`，全部由代码算，不许手写常量。

**2）用「渐隐」代替「裁切」。**
把 `canvas.clipRect` 换成顶部 24dp 的 `LinearGradient` 遮罩（黑色 alpha 0 → 1）。
效果：滚上去的行是"淡出"，不是"齐刷刷切断"；焦点环再也不会被切，因为根本不再裁；
`qbox_header_clip` 这个常量可以直接删掉，连带那三次调参历史一起消失。

**3）去掉 `snapRow`，别跟 Leanback 抢滚动。**
`snapRow()` 现在的写法（`getFocusedChild().getTop() - paddingTop` → `scrollBy`）在三种情况下会打架：
跨行时 Leanback 正在做 rolling 动画、焦点放大改变了 item 的实际绘制高度、列表回收后 `getFocusedChild()` 返回的不是你以为的那一行。
→ 直接删掉，回到 Leanback 默认滚动 + 上面的渐隐遮罩。视觉上"上一行残留"会被淡出吃掉，不需要真的滚走。
（如果确实要"整行吸附"，正确做法是 `setWindowAlignment(WINDOW_ALIGN_LOW_EDGE)` +
`setWindowAlignmentOffsetPercent(0)`，让 Leanback 自己算，而不是事后 `scrollBy` 补一刀。）

**4）列数变化 = 重建列表。**
`Product.getColumn()` 变了之后：清空 adapter → `scrollToPosition(0)` → 重新 `addGrid`。
现在只改了 presenter 的 `size`，行高变了但滚动位置还停在旧坐标，所以"改大之后海报显示不全"。

**5）三个页面共用一套 header。**
把 toolbar + tabs 抽成 `view_qbox_header.xml`，首页/分类页/搜索页 `<include>` 同一份，
tab 高度、左边距（96dp）、字号（17sp）、焦点药丸全部只有一个来源 —— "tab 位置乱跳"会直接消失。

### 落地顺序（建议）

| 步骤 | 内容 | 风险 | 收益 |
|---|---|---|---|
| 1 | 删 `snapRow` + 裁切改渐隐 | 低（纯视觉层） | 一次消掉"锁死/截断/焦点环被切"三个症状 |
| 2 | 抽 `QboxHeaderMetrics` + 三个 header 合一份 | 中（动布局） | 消掉 tab 乱跳、尺寸联动 |
| 3 | 列数变更后重建列表 | 低 | 消掉"改海报大小后错乱" |
| 4 | 分页网格抽公共类 | 中 | 首页/分类/聚合三处展示逻辑一致 |

做完 1、2 两步，你列的 9 条症状里至少 7 条会消失，剩下的两条（功能按钮、UI 不统一）属于第二节的 P1/P2 清单。

