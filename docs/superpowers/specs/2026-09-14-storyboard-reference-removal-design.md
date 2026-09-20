# 分镜参考资产移除设计

## 目标

让用户在视频生成页面移除当前分镜不需要的参考资产，同时保留资产库、其他分镜关联和既有视频任务的资产快照。

## 交互

- 每张参考资产卡片右上角显示删除按钮。
- 点击后要求用户确认，明确说明仅移除当前分镜引用。
- 成功后重新加载当前章节分镜数据，已移除卡片立即不再显示。
- 请求失败时保留现有卡片并显示错误信息。

## 后端

- 新增 `DELETE /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/assets/{assetId}?assetRole=CHARACTER|LOCATION`。
- 后端校验角色只能是 `CHARACTER` 或 `LOCATION`，并仅删除 `novel_storyboard_asset_ref` 中同时匹配分镜 ID、资产 ID 和角色的关联行。
- 不删除 `visual_asset`、`visual_asset_version`、图片文件、首帧、视频或已创建的视频任务资产快照。

## 验收

1. 删除人物或场景卡片后，当前分镜不再返回该资产。
2. 资产仍可在资产库、其他分镜及既有视频记录中使用。
3. 取消确认不发送删除请求。
4. 非法角色参数被后端拒绝。
5. 后端单元测试和前端页面契约测试通过。
