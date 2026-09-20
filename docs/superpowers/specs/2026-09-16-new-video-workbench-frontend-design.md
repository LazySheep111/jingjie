# 新视频创作工作台前端设计

## 目标

新增一套与旧页面完全隔离的原生 HTML/CSS/JS 前端，以专业视频创作工具的工作台方式呈现现有能力。旧 `templates/index.html`、`static/*.html`、`static/css/outline.css` 与 `static/js/*.js` 不修改、不重命名、不删除。

## 新文件与路由

新增文件统一放在 `novel/src/main/resources/static/studio/`：

- `index.html`：新前端入口与项目选择。
- `workbench.html`：分镜、资产、首帧、视频生成的核心工作台。
- `assets.html`：独立资产库。
- `history.html`：作品历史。
- `css/studio.css`：独立令牌、布局与响应式样式。
- `js/api.js`：响应解包、请求、轮询、错误规范化。
- `js/workbench.js`：工作台渲染与交互。
- `js/assets.js`、`js/history.js`：独立页面脚本。

路由采用静态文件路径，例如 `/studio/workbench.html?novelId={id}&chapterNum={num}`。旧路由保持不变。

## 视觉与布局

桌面端（>=1200px）采用左侧项目导航 232px、中部可伸缩画布、右侧检查器 336px 的三栏结构。顶栏固定展示作品名、当前章节、任务入口和唯一主操作。中部上方为横向场景轨道；选中场景下方为脚本与预览画布；生成控制置于画布下方的全宽操作带，禁止置于窄侧栏。

右侧检查器仅包含人物/场景资产、生成设置和任务状态。所有状态同时使用文本、图标和语义色。界面采用冷白背景、深蓝灰文字和单一青蓝强调色；正常正文对比度不低于 4.5:1。

中等屏（720px–1199px）隐藏常驻右侧检查器，改为画布后的可展开面板；小屏（<720px）隐藏左侧栏，场景轨道横向滚动，脚本、预览、资产、任务按内容优先级单列，所有操作按钮全宽。

## 功能与接口映射

| 新前端模块 | 读取接口 | 写入接口 | 状态处理 |
| --- | --- | --- | --- |
| 项目/章节导航 | `GET /api/novel/history`、`GET /api/novel/{novelId}/chapters` | 无 | 空项目提示导入或新建 |
| 分镜轨道与脚本 | `GET /api/novel/{novelId}/chapters/{chapterNum}/storyboard` | `POST .../storyboard/generate`、`PUT .../storyboard` | 显示生成中、空分镜、失败重试 |
| 场景资产 | `GET /api/novel/{novelId}/chapters/{chapterNum}/assets`、`GET /api/novel/{novelId}/assets` | `DELETE .../storyboard/{sceneId}/assets/{assetId}` | 删除只解除引用，不删除库资产 |
| 首帧 | `GET .../first-frames`、`GET /api/first-frame-tasks/{id}` | `POST .../first-frame-tasks`、`POST .../first-frames/upload`、`DELETE /api/storyboard-first-frames/{id}` | 轮询任务，选择状态持久于当前页面内存 |
| 视频任务 | `GET .../videos`、`GET /api/video-tasks/{id}` | `POST .../video-tasks`、`POST .../videos/upload`、`POST /api/video-tasks/{id}/retry`、`PUT /api/storyboard-videos/{id}/current` | 轮询直到成功/失败；失败显示原因与恢复路径 |
| 安全改写 | 无 | `POST /api/video-prompts/safety-rewrite` | 只在安全审核失败时出现“安全改写后重试” |
| 资产库 | `GET /api/novels`、`GET /api/visual-assets/library`、`GET /api/visual-assets/{id}` | `POST .../versions`、`POST .../reuse` | 资产详情在检查器显示，长列表分页/虚拟化 |

## 交互和反馈

每个异步按钮在提交后禁用以防重复请求，并立即显示“排队中/生成中/处理中”的内联状态。超过一秒的加载使用与最终内容尺寸一致的骨架屏。失败文案必须包含原因与下一步，例如“尚未选择首帧。请选择已有首帧，或先生成首帧。”

按钮最小高度 44px；图标按钮具有可访问名称与 tooltip；键盘 Tab 顺序遵循视觉顺序；焦点不被顶栏或操作带遮挡。删除首帧或视频前显示确认；解除资产引用明确不影响资产库。

## 数据适配边界

`js/api.js` 是唯一允许直接调用 `fetch` 的新增模块。它负责解包当前后端的 `success/data/errorMsg` 响应、抛出可显示错误、处理 multipart 上传和可取消轮询。页面模块只接收领域对象，不能依赖旧页面 DOM 或旧脚本全局变量。

## 验收

- 旧页面文件的 Git diff 为零。
- 新工作台可使用现有接口完成查看分镜、生成/上传/选择首帧、生成/上传/选择视频、重试与安全改写。
- 1440px、1024px、720px、375px 下无水平滚动或被裁剪操作按钮。
- 进行中、成功、失败、空状态均可见且包含可执行下一步。
- 每个新模块具有静态结构测试；`mvn test` 通过。
