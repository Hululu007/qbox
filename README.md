# QBox

自研 TVBox 客户端（Android TV）。对标 OKTV（Fong/OK 系）的操作结构，全新代码库。

## 特性

- 纯代码构建 UI（零 XML 布局），扁平克制暗色风格
- TVBox 配置协议：json 配置、多站点、jar/js spider
- 饭太硬 jar 兼容：DexClassLoader 动态加载（jar 自包含 Spider 基类，宿主仅需提供 okhttp/gson/sardine）
- ExoPlayer（media3 官方 maven），支持请求头
- 体积约 1.8 MB

## 构建

```
./gradlew :app:assembleRelease
```

依赖：JDK 17、Android SDK（compileSdk 36）。签名密钥 `app/qbox-release.jks` + `keystore.properties`（已入库）。

## 当前状态（M1）

- [x] 配置加载/解析（json/gzip/base64/zip 自动识别）
- [x] jar spider 动态加载与反射调用
- [x] 首页（站点网格）/ 点播页（分类+海报网格+翻页）/ 详情页（选集+播放）
- [x] ExoPlayer 播放（含请求头）
- [ ] js spider（QuickJS）
- [ ] 搜索聚合
- [ ] 直播
- [ ] 历史/收藏
- [ ] 设置页

## 结构

```
app/src/main/java/com/qbox/tv/
  config/   配置协议
  spider/   jar 加载器 + 统一运行时
  model/    内容数据模型 + 播放地址解析
  player/   ExoPlayer 封装
  ui/       页面与控件（纯代码）
```
