# 骨架屏截图

骨架屏（`el-skeleton`）在 API 加载数据期间显示，替代旧版旋转加载图标，提升用户感知性能。

所有截图统一 800×396（800px 宽等比缩放）。

## 骨架屏效果

| 页面 | 骨架屏 | 结构说明 |
|------|--------|----------|
| IntroPage (系统简介) | <img src="skeleton-intro.png" width="400"> | 单 `el-card`，8 条骨架（1 h3 + 6 p + 1 text） |
| ArchPage (环境架构) | <img src="skeleton-arch.png" width="400"> | 双 Tab（测试/生产），各含 8 条骨架 |
| ServerPage (服务器配置) | <img src="skeleton-server.png" width="400"> | 三 Tab（基础设施/网络策略/数据库），各含 8 条骨架 |
| GuidePage (接入指南) | <img src="skeleton-guide.png" width="400"> | 单 `el-card`，8 条骨架（1 h3 + 6 p + 1 text） |

## 空状态对比（修复前）

无骨架屏时，API 快速失败直接显示空状态：

| 页面 | 空状态 |
|------|--------|
| IntroPage | <img src="empty-intro.png" width="400"> |
| ArchPage | <img src="empty-arch.png" width="400"> |
| ServerPage | <img src="empty-server.png" width="400"> |
| GuidePage | <img src="empty-guide.png" width="400"> |

## 骨架条宽度分布（每容器 8 条）

```
h3    40%       — 标题
p    100%      — 段落
p     80%      — 段落
p     60%      — 段落
text  35%      — 间隔/小元素
p     90%      — 段落
p    100%      — 段落
p     70%      — 段落
```

## 注意事项

- 截图尺寸 800×396（等比缩小至 800px 宽）
- `empty-*` 为修复前截图（无后端时直接显示空状态）
- `skeleton-*` 为修复后截图（通过强制 `loading=true` 获取骨架屏效果）
- 实际运行时骨架屏会在 API 请求期间自动显示
- `MIN_LOADING_MS = 300` 保证骨架屏至少展示 300ms，防止极快响应时闪烁
