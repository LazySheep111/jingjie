# 分镜脚本工作台独立预览页设计

## 目标

为 AI 视频创作流程新增一个独立的分镜脚本工作台预览页。新页面必须让创作者在同一视图中高效完成作品切换、分镜浏览、脚本查看、资产确认、首帧处理和视频生成；不得覆盖现有 `/storyboard-workbench.html`。

## 范围与约束

- 新入口为 `/storyboard-workbench-new.html`，旧入口和旧页面文件不修改。
- 后端接口、数据库结构、请求路径、请求参数与接口返回数据格式不变。
- 复用 `/js/storyboard-workbench.js`、`/studio/js/workbench.js`、`/studio/js/api.js` 与 `/studio/js/workbench-shell.js` 的业务逻辑。
- 保留既有 DOM 接口：`novelImportTitleInput`、`novelImportFile`、`novelImportBtn`、`novelImportStatus`、`refreshRecentBtn`、`recentNovelList`、`recentNovelEmpty`、`workbenchTitle`、`sceneTrack`、`sceneEditor`、`assetInspector`、`videoStatus`、`videoResolution`、`[data-action="generate-video"]` 与 `studioInspector`。
- 不伪造作品、分镜、资产、首帧、视频或任务数据；空状态只说明当前真实数据尚未加载或不存在。
- 中文界面，支持桌面端和窄屏单列布局，避免横向溢出与按钮挤压。

## 信息架构

### 桌面端

页面在共享工作台壳层内使用三段式创作画布：

1. **作品上下文栏（左）**：TXT/DOCX 导入、导入状态、历史作品列表与刷新动作。此区只解决“当前创作对象是谁”。
2. **当前分镜区（中）**：顶部为分镜轨道，主体为选中场景的地点、时段和脚本。它是最高视觉权重区域。
3. **生成与资产区（右）**：关联人物/场景资产、首帧生成/上传/选择、分辨率、视频生成按钮和 `videoStatus`。失败信息与重试动作留在此处，避免状态漂移。

### 窄屏

在 1120px 以下切换为两列：当前分镜为主列，生成与资产区移动至其下；作品上下文保留在顶端或侧栏。760px 以下为单列，分镜轨道横向可滚动，所有操作按钮和表单控件全宽显示。

## 视觉系统

- 语义：深石墨工作区、暖白内容表面、琥珀色主操作、青灰色辅助操作。
- 以细分隔线、低对比表面层级和紧凑间距组织信息，不使用营销型大横幅、渐变球或无意义装饰。
- 字体使用系统中文无衬线栈；标题与正文以尺寸、字重和间距建立层级。
- 焦点、禁用、加载、空、成功、失败状态均使用文字和颜色双重表达；所有可操作控件保留明显键盘焦点。

## 数据与交互流

- 导入继续 POST `/api/novel/import`，成功后跳转至原有小说全文页。
- 历史作品继续读取 `/api/novel/history?page=1&pageSize=10`，选择后沿用现有跳转路径。
- 有 `novelId` 与 `chapterNum` 时，继续并行读取分镜和章节资产；无参数时显示真实历史作品入口。
- 分镜选择继续由 `sceneTrack` 驱动；当前场景渲染到 `sceneEditor`，关联资产渲染到 `assetInspector`。
- 首帧生成、上传、选择与视频生成继续走现有端点。`videoStatus` 作为唯一任务状态来源，负责加载、成功、失败原因和原有重试/安全改写操作。

## 错误与状态

- 导入、历史作品、分镜加载、首帧生成/上传、视频生成均保留原有脚本的状态文案与错误信息。
- CSS 为 `.error`、禁用按钮、空内容、生成中的文本和成功提示提供稳定容器与可读颜色，不改变 API 失败判断逻辑。
- 现有脚本没有向 `taskInspector` 写入独立任务数据，因此页面不制造第二份任务列表；该区域改为承载 `videoStatus`，确保状态可见且不与真实数据冲突。

## 测试与验证

- 新增 Node 静态页面检查，断言独立页面、关键 DOM ID、原有脚本引用、专用样式和无障碍状态容器存在。
- 先运行该测试确认失败，再创建页面和样式使其通过。
- 运行现有前端测试集合；如 Maven 全量测试仍出现已知的模型配置服务失败，报告为既有问题且不归因于本次纯前端改动。

## 文件边界

- 新增 `novel/src/main/resources/static/storyboard-workbench-new.html`：独立语义结构与原脚本需要的元素 ID。
- 新增 `novel/src/main/resources/static/studio/css/storyboard-workbench-new.css`：新页面唯一的布局、组件与响应式样式。
- 新增 `novel/src/test/frontend/storyboard-workbench-new-page.test.js`：静态功能契约检查。
- 新增实施计划文件：记录 TDD 顺序、确切文件和验证命令。
