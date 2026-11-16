# 文档

面向使用者与二次开发；界面截图在 `screenshots/ui-cinema/`。

| 文档 | 内容 |
|---|---|
| [CONFIG.md](CONFIG.md) | 配置接口（TVBox 兼容）：站点、直播、解析、代理、规则等字段 |
| [LIVE.md](LIVE.md) | 直播源格式与频道组织 |
| [SPIDER.md](SPIDER.md) | 爬虫（spider）接口约定 |
| [LOCAL.md](LOCAL.md) | 本地 / 局域网内容播放 |
| [HEADLESS.md](HEADLESS.md) | CI 与无头 Android TV 测试（合成 mock，不能替代真机验收） |

播放内核基于 [FongMi/TV](https://github.com/FongMi/TV) 5.6.1，媒体层（media3 1.11.0）由
[FongMi/media](https://github.com/FongMi/media) 的 `release-1.11.0-fongmi` 分支自编，
两者均以 GPL-3.0 发布。
