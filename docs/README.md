# 文档

安装与日常使用请先看根目录 [README](../README.md)。这里收录配置格式参考与维护资料；`HEADLESS.md`、`DEVELOPING.md` 面向贡献者，普通用户不需要阅读。

| 文档 | 内容 |
|---|---|
| [CONFIG.md](CONFIG.md) | 配置接口（TVBox 兼容）：站点、直播、解析、代理、规则等字段 |
| [LIVE.md](LIVE.md) | 直播源格式与频道组织 |
| [SPIDER.md](SPIDER.md) | 爬虫（spider）接口约定 |
| [LOCAL.md](LOCAL.md) | 本地 / 局域网内容播放 |
| [HEADLESS.md](HEADLESS.md) | CI 与无头 Android TV 测试（合成 mock，不能替代真机验收） |
| [DEVELOPING.md](DEVELOPING.md) | 开发、构建、发布与仓库约束 |
| [UPSTREAM.md](UPSTREAM.md) | 上游 FongMi/TV、FongMi/media 版本与 QBox 的改动范围 |
| [RELEASE-NOTES-2.0.4.md](RELEASE-NOTES-2.0.4.md) | QBox 2.0.4 发布说明（莫兰迪蓝图标、原生 Toast 与 mobile 构建闸门） |
| [RELEASE-NOTES-2.0.3.md](RELEASE-NOTES-2.0.3.md) | QBox 2.0.3 发布说明（续播、Toast、筛选阴影与配置页修复） |
| [RELEASE-NOTES-2.0.2.md](RELEASE-NOTES-2.0.2.md) | QBox 2.0.2 发布说明（选集滚动与线路恢复修复） |
| [RELEASE-NOTES-2.0.1.md](RELEASE-NOTES-2.0.1.md) | QBox 2.0.1 发布说明（图标与文档更新） |
| [RELEASE-NOTES-2.0.0.md](RELEASE-NOTES-2.0.0.md) | QBox 2.0.0 发布说明（内核大对齐） |

播放内核基于 [FongMi/TV](https://github.com/FongMi/TV) 5.6.1，媒体层（media3 1.11.0）由
[FongMi/media](https://github.com/FongMi/media) 的 `release-1.11.0-fongmi` 分支自编，
两者均以 GPL-3.0 发布。
