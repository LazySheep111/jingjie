# 视频创作工作台镜头生产线 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将视频创作工作台重构为镜头生产线布局，同时保持既有生成、上传、轮询和版本操作逻辑。

**Architecture:** 保持 `video-generation.js` 的动态渲染、`data-action`、`data-role` 和 API 调用不变，仅为动态节点补充稳定的布局类与状态数据属性。由 `workbenches-new.css` 提供桌面三栏、平板两栏和窄屏单栏的布局规则；前端静态测试验证保留元素与新布局契约。

**Tech Stack:** 原生 HTML、CSS、JavaScript、Node 内置测试、Spring Boot 静态资源。

## Global Constraints

- 不修改后端接口、数据库结构、API 地址、请求参数或响应数据格式。
- 保留 `sceneList`、`pageStatus`、`pageEmpty`、`data-action`、`data-role`、首帧预览对话框与全部既有事件逻辑。
- 不通过浏览器验收触发真实模型生成、上传、删除或重试操作。
- 当前仓库包含用户已有暂存/未提交文件；不得自动暂存或提交。

---

### Task 1: 为动态视频分镜建立可测试的生产线结构

**Files:**
- Modify: `src/main/resources/static/js/video-generation.js:57-126`
- Test: `src/test/frontend/video-generation-workspace.test.js`

**Interfaces:**
- Consumes: 分镜 `scene` 对象的 `id`、`sequence`、`durationSec`、`shotType`、`shotPlan`、`location`、`timeOfDay`。
- Produces: 保持既有 `data-action` / `data-role` 的 `video-production-stage` 卡片，并增加 `video-scene-status`、`video-primary-canvas`、`video-output-console` 布局类。

- [ ] **Step 1: 写入失败测试**

```js
assert.match(script, /class="video-scene-status"/);
assert.match(script, /class="video-primary-canvas"/);
assert.match(script, /class="video-output-console"/);
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node src/test/frontend/video-generation-workspace.test.js`

Expected: 失败，提示动态结构中缺少新的生产线布局类。

- [ ] **Step 3: 最小化修改动态模板**

```js
<span class="video-scene-status" data-role="status">尚未生成</span>
<div class="video-stage-canvas video-primary-canvas">...</div>
<section class="video-output-dock video-output-console">...</section>
```

仅添加 class，不修改已有 `data-role`、按钮文字、事件绑定或 fetch 请求。

- [ ] **Step 4: 运行测试并确认通过**

Run: `node src/test/frontend/video-generation-workspace.test.js`

Expected: `video generation workspace checks passed`。

### Task 2: 实现镜头生产线桌面、平板和窄屏布局

**Files:**
- Modify: `src/main/resources/static/studio/css/workbenches-new.css:182-235`
- Test: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: Task 1 新增的 `video-scene-status`、`video-primary-canvas`、`video-output-console` 类和既有 `video-stage-layout`、`video-stage-rail` 类。
- Produces: 宽屏三栏镜头卡、平板双栏输出区、720px 以下单栏纵向工作流。

- [ ] **Step 1: 写入失败测试**

```js
assert.match(css, /\.video-production-list \.video-primary-canvas/);
assert.match(css, /\.video-production-list \.video-output-console/);
assert.match(css, /@media \(max-width: ?760px\)[\s\S]*?\.video-stage-layout \{ grid-template-columns:1fr/);
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: 失败，提示缺少镜头主画布或输出控制台规则。

- [ ] **Step 3: 添加布局规则**

```css
.video-production-list .video-stage-layout {
  display:grid;
  grid-template-columns:88px minmax(0,1fr) minmax(280px,.42fr);
  gap:16px;
}
.video-production-list .video-output-console { display:grid; gap:14px; }
.video-production-list .video-scene-status { display:inline-flex; width:fit-content; }
@media (max-width:1199px) {
  .video-production-list .video-stage-layout { grid-template-columns:76px minmax(0,1fr); }
  .video-production-list .video-output-console { grid-column:2; }
}
@media (max-width:760px) {
  .video-production-list .video-stage-layout { grid-template-columns:1fr; }
  .video-production-list .video-output-console { grid-column:auto; }
}
```

保留已有图片、视频和脚本文本收缩规则，避免恢复横向溢出。

- [ ] **Step 4: 运行测试并确认通过**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: 4 个子测试全部通过。

### Task 3: 强化生成控制台和状态反馈的视觉层级

**Files:**
- Modify: `src/main/resources/static/studio/css/workbenches-new.css:视频生产线规则块`
- Test: `src/test/frontend/video-generation-workspace.test.js`

**Interfaces:**
- Consumes: `data-role="status"`、`data-role="result"`、`data-role="history"` 与现有 `data-action` 按钮。
- Produces: 明确的首帧、视频、失败重试与历史版本分组，且不改变行为。

- [ ] **Step 1: 写入失败测试**

```js
assert.match(css, /\.video-production-list \[data-role="status"\]/);
assert.match(css, /\.video-production-list \.video-action-groups/);
assert.match(css, /\.video-production-list \.video-history/);
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node src/test/frontend/video-generation-workspace.test.js`

Expected: 失败，提示缺少生产线控制台状态规则。

- [ ] **Step 3: 添加紧凑控制台样式**

```css
.video-production-list .video-action-groups { display:grid; gap:10px; }
.video-production-list .video-frame-actions,
.video-production-list .video-primary-actions { display:flex; flex-wrap:wrap; gap:8px; }
.video-production-list .video-history { border-top:1px solid var(--new-line); }
.video-production-list [data-role="status"].error { border-left-color:var(--new-danger); }
```

每个状态块使用文字、边框和背景共同表达，不仅依赖颜色。

- [ ] **Step 4: 运行测试并确认通过**

Run: `node src/test/frontend/video-generation-workspace.test.js`

Expected: `video generation workspace checks passed`。

### Task 4: 复制资源并进行浏览器验收

**Files:**
- Modify: `src/main/resources/static/video-generation-new.html:样式缓存查询参数`
- Test: `src/test/frontend/video-generation-page.test.js`, `src/test/frontend/video-generation-workspace.test.js`, `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: 上述 HTML、CSS、JS 和当前本地服务器数据。
- Produces: 可刷新的新视频工作台页面，桌面和窄屏均无横向溢出。

- [ ] **Step 1: 更新 CSS 缓存版本**

```html
<link rel="stylesheet" href="/studio/css/workbenches-new.css?v=20260918f">
```

- [ ] **Step 2: 运行前端检查**

Run: `node src/test/frontend/video-generation-page.test.js; node src/test/frontend/video-generation-workspace.test.js; node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: 所有检查通过。

- [ ] **Step 3: 复制静态资源并打包**

Run: `mvn package -DskipTests`

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 浏览器只读验收**

打开：`http://localhost:8081/video-generation-new.html?novelId=54&chapterNum=1`

检查：镜头列表、当前镜头脚本、参考资产、首帧操作、视频操作、任务状态和历史按钮可见；1280px 与 720px 均无页面级横向滚动。
