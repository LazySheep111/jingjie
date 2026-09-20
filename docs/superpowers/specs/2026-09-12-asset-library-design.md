# 全局资产库设计

## 目标

新增桌面端全局资产库页面，集中展示所有小说的人物和场景资产，并支持按小说、资产类型和名称筛选。用户可以查看三视图和三组提示词、编辑资产并保存为新版本，也可以将资产复用到指定小说的指定章节。

## 页面与导航

- 新增路由：`/asset-library.html`
- 全局导航顺序：新建创作、资产库、我的历史作品
- 资产库显示所有小说，支持切换“全部小说”和当前小说

## 页面布局

桌面端使用三栏布局：

1. 左栏：小说列表、小说搜索、资产类型筛选。
2. 中栏：资产搜索、人物/场景卡片、分页。
3. 右栏：选中资产的详情、三视图预览、核心特征、正面/侧面/背面提示词、保存新版本和复用操作。

资产图片点击后使用现有的大图预览交互。

## 资产版本规则

- 保存编辑内容时创建新版本，不覆盖旧版本。
- 新版本成为资产当前版本。
- 已有章节继续引用原来的资产版本。
- 资产库详情显示当前版本和历史版本入口，第一版至少保留当前版本编辑能力。

## 资产复用规则

点击“复用资产”后选择目标小说、目标章节和资产角色，后端只建立章节与资产版本的关联，不重复生成图片。

## 前端接口契约

### 查询小说列表

`GET /api/novels`

返回小说 ID、名称、类型、创建时间及资产统计信息。

### 查询资产库

`GET /api/visual-assets/library?novelId=&assetType=&keyword=&page=&pageSize=`

返回分页记录，每条记录包含：`assetId`、`novelId`、`novelTitle`、`assetType`、`assetName`、`version`、`coreFeatures`、三组提示词、`compositeImagePath`、`reuseCount` 和关联章节列表。

### 查询资产详情

`GET /api/visual-assets/{assetId}`

### 保存新版本

`POST /api/visual-assets/{assetId}/versions`

请求体包含 `coreFeatures`、`frontPrompt`、`sidePrompt`、`backPrompt`。

### 复用资产

`POST /api/visual-assets/{assetId}/reuse`

请求体包含 `targetNovelId`、`targetChapterNum` 和 `assetRole`。

## 页面状态

前端处理加载中、空列表、图片待生成、图片生成失败、保存成功、复用成功和接口错误状态。后端错误信息只展示用户可理解的提示，详细错误保留在控制台。

## 范围限制

第一版只实现桌面端，不新增移动端布局；不修改图片生成流程；不改变已有章节资产页面的行为；不在本次功能中实现登录权限体系。
