# QBox 1.9.15 UI/交互复审与修复（基于 v1.9.15 干净代码重做）

> 基线：`fongmi-561` 分支 `ui-cinema`，HEAD `575c3dd qbox 1.9.15: 搜索结果保留聚焦放大，顶部留白 32dp 防遮挡`。
> 上一轮 1.9.16 的改动已用 `git stash` 存为 `stash@{0}`，本轮从 1.9.15 重新开始。
> 测试机：IP810N 盒子 Android 9，`192.168.2.214:5555`，1920×1080 / density 2.0 / armeabi-v7a。

## 结论先说

用户点名的六个问题，根因只有两个：

1. **顶部几何是写死的常量**（`paddingTop=144dp`、裁剪线 `114dp`、行距 `24dp`）。
   只对「海报尺寸=大（5 列）」调好过；换成 6、7、4 列后行高变化，
   裁剪线和 paddingTop 之间露出 30dp 的缝，上一行的海报下半截/标题就从分类条下面钻出来。
2. **焦点依赖 Leanback 的隐式推断**。选中位置可能停在「最近观看 / 更新推荐」这类
   不可聚焦的分组标题上，`requestFocus()` 只把焦点交给列表容器 —— 屏幕上一个焦点框都没有，
   看起来就是「海报锁死不动，不知道选中了哪里」。

---

## 一、换行后第一行不干净（最高优先级）

**现象**：任何一行按上/下换行后，分类条下面总留着上一行的半截海报或半截标题。

**根因**：
- 裁剪线 `qbox_header_clip = 114dp(228px)`，内容区起点 `qbox_content_top = 144dp(288px)`，
  中间 60px 没有任何东西挡住；
- 列表选中行被 Leanback 垂直居中，上一行根本不会滚出内容区。

**修复（`CustomVerticalGridView`）：**

1. **裁剪线固定，不跟着选中行跳**：
   第一版做的是「裁到当前行顶边」，结果上一行**瞬间整行消失**（滚动动画被吃掉），
   而且筛选面板这种需要同时看多行的场景会被裁得只剩一行。
   现在裁剪线钉在内容区顶边（144dp），上一行由 Leanback 正常滚动送出去。
2. **滚动停下后把当前行贴到内容区顶部**：`smoothScrollBy` 补正（不是 `scrollBy` 瞬移），
   距离通常只有几十像素，看起来就是滚动动画多走一段。补正后画面稳定为
   `分类条 → 干净背景 → 当前这一行`。
3. **筛选行豁免贴顶**：`setSnapStartIndex(筛选行数)`。筛选面板一次要看三四行，
   贴顶会把它整个甩出屏幕；豁免后逐行下移只滚一小段，多行始终可见。
4. **裁剪线必须给放大让路**：卡片聚焦时上下各放大 5%（约行高 5.5%），
   裁剪线若压在行顶边上，放大后的海报/焦点环上半部分会被切掉。
5. `setGravity(Gravity.TOP)`：分类只有几条数据时，Leanback 默认把内容垂直居中，
   上面空一大片、海报贴着屏幕底部；统一从内容区顶部往下排。

**全局统一聚焦视觉**：首页之前用 `ZOOM_FACTOR_NONE`（不放大），
分类页/聚合搜索页用 `ZOOM_FACTOR_SMALL`（1.1 倍）——同一张海报在不同页面表现不一样。
现在全部改成 `ZOOM_FACTOR_SMALL`，卡片尺寸、行内间距 16dp、焦点描边、点击行为都走同一套。

> 尝试过但放弃的两条路（会"和 Leanback 抢滚动"，表现为按一下跳一下又弹回 / 海报锁死）：
> `scrollBy` 补偿、`offsetChildrenVertical` 平移。

## 二、海报尺寸切换后全部 UI 问题复发

**根因**：`paddingTop`、裁剪线、行距、行高全部是常量，只有 5 列时对得上；
切尺寸后 adapter 按新列数重排，但滚动位置和焦点还停在旧几何上。

**修复**：
- 新增 `applyContentTop(top, rowHeight, zoom)`：行距 = `max(24dp, 行高×7.5%)`，
  `paddingTop = 内容区顶边 + 放大余量`，随 `Product.getSpec(style)` 实时计算；
- 首页 `SIZE` 事件标记 `mResizePending`，数据回来时先 `scrollToPosition(0)`，
  再复位到第一个内容行；焦点只在「本来就在内容区或已丢失」时才抢，不把用户从顶栏拽下来；
- 分类页检测列数变化，清空旧分片行按新列数重排。

## 三、焦点丢失 / 不知道选中了哪里

| 场景 | 根因 | 修复 |
|---|---|---|
| 进内容区后屏幕没有焦点框 | 选中位置停在不可聚焦的分组标题上 | 新增 `focusContent()`：进内容区前校验选中行是不是 `ListRow`，不是就落到第一个内容行 |
| 顶栏按 ↓ 直接跳过分类条 | `bindAction` 的 DOWN 分支未判空分类 | 有分类条先给分类条 |
| 上/下键路由判断失效 | `recycler.hasFocus()` 在焦点位于卡片时为 false | 改用 `isInside(container, getCurrentFocus())` |
| 从设置页返回，遥控器完全没反应 | 系统把焦点回收，整棵树无 focused 节点 | `BaseActivity.onWindowFocusChanged` 全局兜底：谁都没焦点就找第一个可聚焦控件；首页 onResume 再兜底回顶栏源名 |
| 改海报尺寸后焦点悬空 | rebuild 后未重新聚焦 | `resetAfterResize()` 聚焦第一个内容行 |

## 四、搜索页

| 现象 | 根因 | 修复 |
|---|---|---|
| 快速输入时旧建议覆盖新建议 | 请求无序号 | 加 `mSuggestSeq`，只认最后一次请求 |
| 已经打字了，热词回来又把建议冲掉 | 热词回调不校验输入状态 | 热词回来时若输入框非空直接丢弃 |
| 建议词首行按 ↑ 出不去 | `handleWordKey` 直接 `return true` | 无记录区时回到搜索框 |
| 进页面第一次按 ↓ 没反应 | 列表还没布局，`getChildAt(0)` 为 null 时返回 false，按键被漏掉 | 先吃键，下一帧再聚焦 |
| 焦点路过麦克风就弹录音/权限 | `CustomMic.onFocusChanged(true)` 调 `start()` | 只在按 OK 时启动，离开时仍然收尾 |

## 五、播放页「暂停 / 播放」显示错误

**根因**：按钮文字只看 `isPlayWhenReady()`。
- 播完（ENDED）时 `playWhenReady` 仍是 `true`，按钮停在「暂停」，实际已经播完了；
- `pause()` 是跨进程调用，`onPlayPause()` 里立刻回读状态可能还没变。

**修复**：
- `updatePlayPause()` 把 ENDED 也算作可播放态（显示「播放」）；
- 覆写 `onPlayWhenReadyChanged()` 实时刷新，不再只等 `onIsPlayingChanged`；
- 播完按按钮走重播，而不是「暂停一个已经停住的画面」。

## 六、分类页

- 每次可见都 `moveToTop()` 强制回顶 → 改成只在首次进入回顶，切 tab 回来保留位置。

---

## 七、实测（214，720p 档）

| 场景 | 结果 |
|---|---|
| 7 列首屏 | 分组标题完整、第一行完整、焦点在历史卡 |
| 首页按 ↓ | 中间帧能看到上一行正在滚出（**有滚动动画**），落定后第一排贴顶、无残影 |
| 5 列按 ↓ | 焦点卡片明显放大（1.1 倍）+ 白色描边，**顶部完整不裁** |
| 分类页按 ↓ | 与首页完全一致：放大、完整、干净 |
| 筛选面板 | 字号 14sp 与分类条一致；第 1→2 排下移只轻微滚动，两排都可见；收起后恢复 |
| 聚合搜索页按 ↓ | 第一排贴顶、完整，无上一排遗留 |
| 改尺寸 → 返回首页 | 按新列数重排，分组标题正常，焦点复位 |
| 分类页（只有 3 条数据的榜单） | 内容从顶部排，不再垂直居中 |
| 搜索页 | 首焦点在搜索框；按一次 ↓ 即进键盘；输入后建议刷新 |
| 播放页 | 播放中按钮显示「暂停」→ 暂停后显示「播放」且时间码停住 |
| 设置页返回 | 焦点恢复到顶栏，方向键可用（修复前整页无焦点） |

> 三轮试错记录：
> ① 裁剪线只裁到行顶边 → 放大后上半截被切；
> ② 裁剪线跟着选中行跳 → 上一行瞬间消失、没有滚动动画，筛选面板被裁成一行；
> ③ 最终：裁剪线固定 + 滚动停下平滑贴顶 + 筛选行豁免。

## 八、改动文件

```
app/src/leanback/java/com/fongmi/android/tv/ui/custom/CustomVerticalGridView.java   动态裁剪 + 几何自适应 + 顶部对齐
app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java           focusContent / 尺寸切换重建 / onResume 兜底
app/src/leanback/java/com/fongmi/android/tv/ui/fragment/TypeFragment.java           几何自适应 / 列数变化重排 / 首次回顶
app/src/leanback/java/com/fongmi/android/tv/ui/base/BaseActivity.java               全局焦点兜底
app/src/leanback/java/com/fongmi/android/tv/ui/activity/SearchActivity.java         建议词序号 / ↑ 回搜索框 / 首次聚焦
app/src/leanback/java/com/fongmi/android/tv/ui/custom/CustomMic.java                焦点路过不再录音
app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java          播放/暂停状态显示
```

## 九、未完成 / 建议下一轮

1. `qbox_header_clip` 已无引用，可从 `dimens.xml` 删除（留着不影响）。
2. 「更多」二级面板、选集对话框的焦点回退还没统一。
3. 直播页控制条的返回层级（分组的加密计数已在上轮修过，本轮未回归）。
4. 启动加载 10~20 秒期间只有 logo + 小转圈，缺「正在加载配置」文案。
