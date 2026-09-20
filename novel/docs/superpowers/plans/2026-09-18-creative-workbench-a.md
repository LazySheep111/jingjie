# 创作工作台 A 方案 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将七个新版工作台统一为以“镜头剪辑台”为核心的专业创作控制台，同时不破坏既有页面脚本、接口和数据格式。

**Architecture:** `workbench-shell.css` 提供全局导航与上下文栏，`workbenches-new.css` 提供大纲、正文、历史、资产、视频和模型页的共享密集工作区组件，`storyboard-workbench-new.css` 只处理分镜脚本工作台的镜头序列与生成控制。HTML 仅增加稳定的语义包装和辅助状态节点；原有元素 ID、`data-action`、已有 JS 文件和 API 调用保持不变。

**Tech Stack:** 静态 HTML、原生 CSS、原生 JavaScript、Node `node:test` 静态页面检查、Maven 资源打包。

## Global Constraints

- 不修改后端接口、数据库结构、API 地址、请求参数、返回数据格式或原有业务脚本契约。
- 不删除旧页面；只改 `*-new.html` 与共享样式。
- 保留所有现有元素 ID、`data-action` 属性、脚本引入顺序和现有 JS 动态生成的 DOM 选择器。
- 桌面端以 1440px 为主，960px 以下移除固定三栏，720px 以下主编辑区优先堆叠。
- 状态文案必须在关联操作附近展示，避免独立右侧“当前任务 / 工作提示”占位栏。

---

## 文件结构

- Modify: `src/main/resources/static/studio/css/workbench-shell.css` — 全局导航、上下文栏、响应式外壳和统一状态条。
- Modify: `src/main/resources/static/studio/css/workbenches-new.css` — 六个常规新版页面的控制台组件与响应式布局。
- Modify: `src/main/resources/static/studio/css/storyboard-workbench-new.css` — 分镜镜头剪辑台的网格、首帧输出和任务面板。
- Modify: `src/main/resources/static/outline-new.html` — 大纲输入/结果的分段与内联状态语义。
- Modify: `src/main/resources/static/novel-detail-new.html` — 目录、正文、分镜三段工作区和折叠视觉风格。
- Modify: `src/main/resources/static/history-list-new.html` — 作品项目卡与上下文入口。
- Modify: `src/main/resources/static/storyboard-workbench-new.html` — 当前镜头、资产、首帧/任务连续工作流。
- Modify: `src/main/resources/static/video-generation-new.html` — 单分镜生成单元及其局部状态。
- Modify: `src/main/resources/static/asset-library-new.html` — 筛选、资产网格和详情面板。
- Modify: `src/main/resources/static/model-management-new.html` — 模型配置、测试反馈和安全状态。
- Modify: `src/test/frontend/workbench-shell-page.test.js` — 外壳、导航与响应式关键选择器检查。
- Modify: `src/test/frontend/workbenches-new-pages.test.js` — 六个新版页面的集成点、状态区域与窄屏样式检查。
- Modify: `src/test/frontend/storyboard-workbench-new-page.test.js` — 分镜工作台的现有 ID 和新工作流区域检查。

## Task 1: 统一控制台外壳与视觉令牌

**Files:**
- Modify: `src/main/resources/static/studio/css/workbench-shell.css`
- Modify: `src/test/frontend/workbench-shell-page.test.js`

**Interfaces:**
- Consumes: `body.workbench-page`、`.workbench-shell`、`.workbench-nav`、`.workbench-context`。
- Produces: `.workbench-status-strip`、`.workbench-context-actions`、`.workbench-mobile-nav-toggle` 的稳定样式接口，供所有新版页面复用。

- [ ] **Step 1: 写入会失败的静态断言**

```js
assert.match(css, /--wb-canvas:/);
assert.match(css, /\.workbench-status-strip/);
assert.match(css, /@media \(max-width: 960px\)/);
assert.match(css, /@media \(max-width: 720px\)/);
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/workbench-shell-page.test.js`

Expected: FAIL，提示缺少 `--wb-canvas` 或 `.workbench-status-strip`。

- [ ] **Step 3: 最小化实现统一外壳**

```css
:root {
  --wb-canvas: #f3f2ed;
  --wb-surface: #fffefa;
  --wb-ink: #1d2828;
  --wb-accent: #617b3e;
  --wb-action: #dc7b35;
}
.workbench-status-strip { display:flex; gap:8px; align-items:center; min-width:0; }
@media (max-width:960px) { .workbench-shell { grid-template-columns:184px minmax(0,1fr); } }
@media (max-width:720px) { .workbench-shell { display:block; } }
```

保持旧选择器可用，删除仅用于空白占位的固定 inspector 布局规则。

- [ ] **Step 4: 运行断言并确认通过**

Run: `node src/test/frontend/workbench-shell-page.test.js`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add src/main/resources/static/studio/css/workbench-shell.css src/test/frontend/workbench-shell-page.test.js
git commit -m "style: unify creative workbench shell"
```

## Task 2: 大纲与历史作品重构为项目入口

**Files:**
- Modify: `src/main/resources/static/outline-new.html`
- Modify: `src/main/resources/static/history-list-new.html`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `outline.js` 的 `outlineForm`、`submitBtn`、`statusText`、`outlineResult`；`history-list.js` 的 `historyList`、`historyEmpty`、分页 ID。
- Produces: `.outline-stage`、`.project-list-stage`、`.inline-work-status`，不改变原 ID。

- [ ] **Step 1: 写入会失败的页面结构断言**

```js
assert.match(outlineHtml, /class="outline-stage"/);
assert.match(outlineHtml, /id="statusText"[^>]*aria-live="polite"/);
assert.match(historyHtml, /class="project-list-stage"/);
assert.match(css, /\.outline-stage/);
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示缺少 `outline-stage`。

- [ ] **Step 3: 实现项目入口布局**

```html
<section class="outline-stage">
  <section class="outline-brief">…保留 #outlineForm…</section>
  <section class="outline-result-stage">…保留 #outlineResult 与 #emptyResult…</section>
</section>
```

```css
.outline-stage { display:grid; grid-template-columns:minmax(300px,.72fr) minmax(0,1.28fr); gap:18px; }
.project-list-stage { display:grid; gap:12px; }
@media (max-width:960px) { .outline-stage { grid-template-columns:1fr; } }
```

历史作品中的进入全文操作保持原 click handler；主操作置于卡片首行，删除和复制保留在次级操作区。

- [ ] **Step 4: 运行断言并确认通过**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add src/main/resources/static/outline-new.html src/main/resources/static/history-list-new.html src/main/resources/static/studio/css/workbenches-new.css src/test/frontend/workbenches-new-pages.test.js
git commit -m "style: redesign outline and project entry workbenches"
```

## Task 3: 小说全文三段编辑工作区

**Files:**
- Modify: `src/main/resources/static/novel-detail-new.html`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `novel-detail.js` 的 `chapterNav`、`chapterContent`、`storyboardList`、`assetList`、视觉风格 ID 和 `goVideoGenerationBtn`。
- Produces: `.reader-workspace`、`.chapter-rail`、`.storyboard-workflow-panel`；正文、分镜和视觉设定的阅读优先级。

- [ ] **Step 1: 写入会失败的布局与保留 ID 断言**

```js
assert.match(detailHtml, /class="reader-workspace"/);
assert.match(detailHtml, /class="chapter-rail"/);
assert.match(detailHtml, /id="goVideoGenerationBtn"/);
assert.match(css, /grid-template-columns: minmax\(230px/);
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示缺少 `reader-workspace`。

- [ ] **Step 3: 实现正文优先布局**

```css
.reader-workspace { display:grid; grid-template-columns:minmax(230px,.62fr) minmax(520px,1.2fr) minmax(390px,.94fr); gap:16px; }
.chapter-rail, .storyboard-workflow-panel { max-height:calc(100dvh - 160px); overflow:auto; }
.reader-panel { min-height:calc(100dvh - 160px); }
```

将 `details.visual-style-section` 默认收起；保留其内部 ID、表单字段和保存按钮。将分镜操作、摘要和“前往视频生成”保持在分镜面板顶部。

- [ ] **Step 4: 运行断言并确认通过**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add src/main/resources/static/novel-detail-new.html src/main/resources/static/studio/css/workbenches-new.css src/test/frontend/workbenches-new-pages.test.js
git commit -m "style: prioritize chapter reader and storyboard workflow"
```

## Task 4: 分镜脚本镜头剪辑台

**Files:**
- Modify: `src/main/resources/static/storyboard-workbench-new.html`
- Modify: `src/main/resources/static/studio/css/storyboard-workbench-new.css`
- Modify: `src/test/frontend/storyboard-workbench-new-page.test.js`

**Interfaces:**
- Consumes: `storyboard-workbench.js` 的 `sceneTrack`、`sceneEditor`、`assetInspector`、`videoStatus`、`videoResolution`、`data-action="generate-video"`；`workbench.js` 的场景渲染。
- Produces: `.storyboard-cut-layout`、`.storyboard-current-shot`、`.storyboard-output-rail`，当前镜头、资产和首帧输出连续可见。

- [ ] **Step 1: 写入会失败的分镜工作流断言**

```js
assert(html.includes('class="storyboard-cut-layout"'));
assert(html.includes('class="storyboard-current-shot"'));
assert(html.includes('class="storyboard-output-rail"'));
assert(css.includes('.storyboard-cut-layout'));
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/storyboard-workbench-new-page.test.js`

Expected: FAIL，提示缺少 `storyboard-cut-layout`。

- [ ] **Step 3: 实现镜头剪辑台**

```css
.storyboard-cut-layout { display:grid; grid-template-columns:minmax(210px,.52fr) minmax(520px,1.2fr) minmax(320px,.74fr); gap:16px; }
.storyboard-output-rail { position:sticky; top:16px; align-self:start; }
@media (max-width:1100px) { .storyboard-cut-layout { grid-template-columns:220px minmax(0,1fr); } .storyboard-output-rail { grid-column:1 / -1; position:static; } }
```

镜头脚本编辑区优先保留，`sceneTrack` 不使用 `position: sticky`，避免滚动时覆盖脚本；`actionDock` 仅在输出栏内排列，不再浮在正文上方。

- [ ] **Step 4: 运行断言并确认通过**

Run: `node src/test/frontend/storyboard-workbench-new-page.test.js`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add src/main/resources/static/storyboard-workbench-new.html src/main/resources/static/studio/css/storyboard-workbench-new.css src/test/frontend/storyboard-workbench-new-page.test.js
git commit -m "style: turn storyboard page into a cut workspace"
```

## Task 5: 视频分镜生成单元

**Files:**
- Modify: `src/main/resources/static/video-generation-new.html`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `video-generation.js` 的 `sceneList`、`pageStatus`、`pageEmpty`、`firstFramePreviewDialog` 以及动态 scene card 内所有 `data-action` / `data-role` / `data-field`。
- Produces: `.video-production-list`、`.video-scene-unit`、`.video-unit-status`，局部失败和重试区域。

- [ ] **Step 1: 写入会失败的状态区断言**

```js
assert.match(videoHtml, /class="video-production-list"/);
assert.match(videoHtml, /id="pageStatus"[^>]*aria-live="polite"/);
assert.match(css, /\.video-scene-unit/);
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示缺少 `video-production-list`。

- [ ] **Step 3: 实现分镜生成单元布局**

```css
.video-production-list { display:grid; gap:14px; }
.video-scene-unit { display:grid; grid-template-columns:minmax(0,1fr) minmax(280px,.5fr); gap:16px; }
.video-unit-status { border-left:3px solid var(--wb-accent); padding-left:10px; }
@media (max-width:760px) { .video-scene-unit { grid-template-columns:1fr; } }
```

不改 `video-generation.js` 动态生成卡片的 `data-action`。空状态文字保持“请先选择小说”及“请先在历史作品中选择小说，再继续视频生成。”；加载状态仅在真正请求期间出现。

- [ ] **Step 4: 运行断言并确认通过**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add src/main/resources/static/video-generation-new.html src/main/resources/static/studio/css/workbenches-new.css src/test/frontend/workbenches-new-pages.test.js
git commit -m "style: organize video generation into scene units"
```

## Task 6: 资产库视觉检索与详情抽屉

**Files:**
- Modify: `src/main/resources/static/asset-library-new.html`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `asset-library.js` 的 `novelSearchInput`、`novelFilterList`、`assetGrid`、`assetDetailPanel`、`assetReuseModal`、`assetImagePreviewModal`。
- Produces: `.asset-search-workspace`、`.asset-filter-rail`、`.asset-detail-drawer`。

- [ ] **Step 1: 写入会失败的资产布局断言**

```js
assert.match(assetHtml, /class="asset-search-workspace"/);
assert.match(assetHtml, /class="asset-filter-rail"/);
assert.match(css, /\.asset-detail-drawer/);
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示缺少 `asset-search-workspace`。

- [ ] **Step 3: 实现筛选、网格与详情结构**

```css
.asset-search-workspace { display:grid; grid-template-columns:220px minmax(0,1fr); gap:16px; }
.asset-grid { grid-template-columns:repeat(auto-fill,minmax(210px,1fr)); }
.asset-detail-drawer { position:sticky; top:16px; }
@media (max-width:960px) { .asset-search-workspace { grid-template-columns:1fr; } .asset-detail-drawer { position:static; } }
```

保留现有模态框 ID 与复用/预览行为；详情区无选中资产时只显示说明空态，选中后显示原有动态详情。

- [ ] **Step 4: 运行断言并确认通过**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add src/main/resources/static/asset-library-new.html src/main/resources/static/studio/css/workbenches-new.css src/test/frontend/workbenches-new-pages.test.js
git commit -m "style: redesign asset library as a visual workspace"
```

## Task 7: 模型管理配置反馈与全量验证

**Files:**
- Modify: `src/main/resources/static/model-management-new.html`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`
- Modify: `src/test/frontend/workbench-shell-page.test.js`

**Interfaces:**
- Consumes: `model-management.js` 的 `modelPageStatus`、`modelConfigGrid` 及每个动态配置表单内的原有字段/按钮。
- Produces: `.model-config-console`、`.model-card-status`、`.model-test-feedback`，按模型隔离的测试信息。

- [ ] **Step 1: 写入会失败的模型配置断言**

```js
assert.match(modelHtml, /class="model-config-console"/);
assert.match(modelHtml, /id="modelPageStatus"[^>]*aria-live="polite"/);
assert.match(css, /\.model-test-feedback/);
```

- [ ] **Step 2: 运行断言并确认失败**

Run: `node src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示缺少 `model-config-console`。

- [ ] **Step 3: 实现模型控制台与局部错误样式**

```css
.model-config-console { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; }
.model-test-feedback { min-height:28px; margin-top:12px; padding:9px 10px; border-radius:8px; background:#f1f3ed; }
.model-test-feedback.is-error { background:#fff0ec; color:#9a382b; }
@media (max-width:960px) { .model-config-console { grid-template-columns:1fr; } }
```

原 API Key 加密、测试连接与保存逻辑保持不变；错误文本仍来自后端，只改变显示位置与可读性。

- [ ] **Step 4: 运行完整前端与打包检查**

Run: `Get-ChildItem src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }; mvn package -DskipTests`

Expected: 所有 Node 静态检查通过；Maven 输出 `BUILD SUCCESS`。

- [ ] **Step 5: 手动验证 1440px、960px、720px**

Run: 在本地应用中依次访问 `/outline-new.html`、`/novel-detail-new.html?novelId=54`、`/storyboard-workbench-new.html?novelId=54&chapterNum=1`、`/video-generation-new.html?novelId=54&chapterNum=1`、`/asset-library-new.html`、`/model-management-new.html`。

Expected: 无水平滚动条；窄屏下按钮不折成单字纵列；分镜脚本不被固定按钮覆盖；历史作品切换仍能更新上下文。

- [ ] **Step 6: 提交本任务**

```bash
git add src/main/resources/static/model-management-new.html src/main/resources/static/studio/css/workbenches-new.css src/test/frontend/workbenches-new-pages.test.js src/test/frontend/workbench-shell-page.test.js
git commit -m "style: complete creative workbench system refresh"
```

## 自检

- 规范中的全局导航、上下文、状态、响应式、七个页面布局均在 Task 1-7 中覆盖。
- 每个生产改动均以失败的静态测试开始，并给出对应的通过命令。
- 计划不改后端、数据库、业务脚本 ID/API 契约；每项提交只包含当前任务文件。
