# QBox TV 端 UI/UX 整改记录（1.9.16）

> 设备：192.168.2.214:5555（1920×1080 / density 2.0 / 960×540dp），debug 包
> 方法：静态审查 + 逐键 adb 实测（uiautomator dump 取焦点 bounds，screencap 取画面）

---

## 一、根因：三个东西同时改滚动/裁切

上一轮留下的「海报截断 / 锁死 / 不知道选中哪」，不是三个 bug，是同一个结构性问题的三种表现：

```
CustomVerticalGridView
├── canvas.clipRect(0, clipTop, ...)   ← 硬裁，切海报也切焦点环
├── snapRow()  (选中行 post 后 scrollBy) ← 和 Leanback 抢滚动
└── dimens 里 4 个必须手工对齐的魔法数 (144/114/124/42)
```

`snapRow` 在 `onChildViewHolderSelected` 里 `scrollBy`，Leanback 自己的 `windowAlignment`
也在同一帧滚动 → 位移互相覆盖。表现就是「按了半天，列表不动 / 焦点不知道跑哪去了」。
`clipRect` 的裁切线又被压在 114dp，正好在聚焦放大的焦点环上 → 「焦点框被 tab 切掉」。

---

## 二、本轮的架构级改动

### 1. 删掉 snapRow，改 Leanback 原生滚动

`CustomVerticalGridView` 里 `snapRow()`、`onFocusChanged` 里的补位移全部移除，
`onChildViewHolderSelected` 只保留「换行时收起/展开头部」这一件事。

### 2. 硬裁 → DST_OUT 渐隐

```java
int layer = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
super.dispatchDraw(canvas);
fadePaint.setShader(fadeShader);          // fadeTop → fadeTop+fadeHeight，alpha 255→0
canvas.drawRect(0, 0, getWidth(), fadeTop + fadeHeight, fadePaint);
canvas.restoreToCount(layer);
```

擦除是真正的 alpha 过渡，不依赖背景色，氛围底图/模糊海报都能透出来。
硬约束写进了 `dimens.xml`：

```
qbox_fade_top + qbox_fade_height == qbox_content_top
134dp          + 10dp             = 144dp
```

含义：内容区起点处「擦除量 = 0」，所以**第一排卡片和它的焦点环永远完整**；
上一行标题（实测 131dp 附近）落在完全擦除区，不会在分类条下面留半行字。

### 3. 尺寸单一来源

`dimens.xml` 重写，头部总高一类常量只留一份：

| 常量 | 值 | 说明 |
|---|---|---|
| `qbox_header_total` | 118dp | 顶栏 70 + tabs 6+42，实测 tabs y=152px |
| `qbox_content_top` | 144dp | 第一排卡片起点 |
| `qbox_fade_top` / `_height` | 134 / 10dp | 二者之和 = content_top |
| `qbox_row_spacing` | 24dp | 原来首页 24、分类页 16、收藏页 16，全统一 |
| `qbox_tabs_height` | 42dp | 收藏页站点 tab 原来 wrap_content，现在固定 |

---

## 三、逐条对应你列的问题

| 你描述的现象 | 根因 | 修法 | 实测结果 |
|---|---|---|---|
| 海报展示被截断 | `clipRect` 硬裁 | 改 DST_OUT 渐隐，134/10dp | 滚动停止后第一排顶部完整、无残影 |
| 海报选择有问题、锁死不动 | `snapRow` 与 Leanback 抢滚动 | 删除 `snapRow` | 连续 DOWN×6 正常，focus bounds 逐帧跟随 |
| 之前有放大焦点 | 首页行被设成 `ZOOM_FACTOR_NONE` | 恢复 `ZOOM_FACTOR_SMALL` | 焦点卡明显放大，相邻卡不被覆盖 |
| 不知道选择了哪里 | 2dp 白边 + 首焦点落在不可聚焦的标题行 | 3dp 描边 + 6dp 柔光；`setSelectedPosition(1)` + 重试聚焦 | 冷启动即落第一张卡，白环一眼可见 |
| 功能按钮有问题 | 投屏页整页无可聚焦控件；二维码黑底白点 | 补 focusable + 首焦点；二维码改白底黑点 | 二维码可聚焦、白底黑点可扫 |
| 设置改海报大小影响展示 | 只刷数据不重建 ListRow，child 宽高仍是旧值 | `rebuildForSize()` 整体重建 + 滚动/焦点复位 | 「小→中」后 7 列变 6 列，卡片 228→272px，无重叠 |
| 焦点框被 tab 截断 | 裁切线 114dp 压在焦点环上 | 裁切线上移到 134dp，让开 144dp 内容区 | 分类页第一排焦点环四边完整 |
| UI 不够统一 | 各页 paddingTop / 行距 / 卡片尺寸各写各的 | 统一到 `dimens.xml` 单一来源 | 首页/点播/收藏 tabs 同在 y=152~236 |
| 不同场景 tab 位置乱跳 | 收藏页 tab 是 wrap_content + 8dp padding | 改固定 42dp + 6dp marginTop | 与首页分类条同 y 同高 |
| 海报展示逻辑不一致 | 历史行 64dp 高的小图 vs 竖版海报 | 历史卡宽取海报 1.6 倍（191dp，16:9） | 继续观看一行看清内容，与推荐行形成层级 |

---

## 四、其余修复（沿用上一轮）

- **播放页**：`onSeekBarKey` 不再吞返回键；`getFocus2()` 校验面板归属（快进后菜单键能点到按钮）；
  `hideControl(force)` + `seeking` 保护。
- **直播页**：加密分组计数只在换组时清零，不再永远解不开。
- **收藏/聚合搜索**：`getFragment(position)` 按当前 tab 取页，站点 tab 按 ↓ 直接进对应网格。
- **返回键层级**：首页不再 `scrollToPosition(0)`（0 是不可聚焦标题），改为回第一张卡再退出。

---

## 五、验证记录（214 / 720p）

```
冷启动         FOCUS RelativeLayout [64,361][521,720]   历史卡，有白环
UP             → 分类条 [64,152][256,236]
UP             → source  [160,51][424,129]
DOWN           → 分类条 → 内容区
滚动 DOWN×6    FOCUS [64,683][306,1064]                 无半截残留
BACK（列表中部）→ 回到第一张卡
一行 RIGHT×6   FOCUS [1570,321][1856,760]               最右完整可见
点播页          tabs [280,152][472,236]                 与首页同 y
设置改尺寸      sizeText 小→中，卡片 228→272px，焦点复位
投屏页          FOCUS code [710,150][1210,650]          可聚焦
```

---

## 六、仍然存在的（需要产品或数据侧决定）

1. **首页右侧留白**：源只给 7 条数据、当前 6 列，第二行只有 1 张。属数据量问题，不是布局 bug。
2. **播放页控制条未能在本机验证**：测试源全部「连接超时」，VideoActivity 停在详情页。
   控制条的改动在上一轮已在 132（1080p）验证过，本轮无法复测。
3. **KeepActivity 空态**：返回按钮默认聚焦呈纯白实心圆，在纯黑空态页上很抢眼。
   这是 `selector_action_icon` 的既定风格，改它会影响所有页面的返回按钮，未动。
