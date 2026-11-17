- 修复 Android 13+ 原生 Toast 应用图标回退为系统 Android 机器人：移除 2.0.4 新增的 `mipmap-anydpi-v33` 单色图标层，回到 `mipmap-anydpi-v26` 自适应图标。
- Toast 继续使用 Android 原生 `Toast.makeText`，不接管系统提示，不引入自定义 Toast 浮层。
- Launcher 图标保持莫兰迪蓝 `#5C8AAB` 的立方体 Q，传统密度图标与自适应图标同步保留。
- 版本：`versionCode 105` / `versionName 2.0.5`。

升级方式：在应用内点「更新」即可覆盖安装，配置和播放记录保留。
