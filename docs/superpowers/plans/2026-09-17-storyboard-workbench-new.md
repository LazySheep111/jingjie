# 分镜脚本工作台独立预览页 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增不覆盖旧页面的 `/storyboard-workbench-new.html`，以三栏高密度创作台布局复用现有分镜、资产、首帧与视频生成接口。

**Architecture:** 新 HTML 页面保留已有业务脚本依赖的全部 DOM 元素 ID 与 `data-action`，所以 JavaScript 继续请求现有 API，不引入新的接口或数据适配层。新的页面专用 CSS 在共享工作台壳层之后加载，重组页面内部区域为作品上下文、当前分镜和生成控制三部分，并在窄屏变为单列。

**Tech Stack:** Spring Boot 静态资源、原生 HTML/CSS/JavaScript、Node 内置 `assert` 前端静态契约检查。

## Global Constraints

- 不修改后端接口、数据库结构、旧页面 `/storyboard-workbench.html`、现有业务脚本请求地址或请求参数。
- 复用 `storyboard-workbench.js`、`studio/js/workbench.js`、`studio/js/api.js`、`studio/js/workbench-shell.js`。
- 保留既有 DOM 接口：`novelImportTitleInput`、`novelImportFile`、`novelImportBtn`、`novelImportStatus`、`refreshRecentBtn`、`recentNovelList`、`recentNovelEmpty`、`workbenchTitle`、`sceneTrack`、`sceneEditor`、`assetInspector`、`videoStatus`、`videoResolution`、`studioInspector`。
- 使用 `[data-action="generate-video"]` 保留视频生成事件绑定；不伪造作品、分镜、资产或任务数据。
- 新页面必须在 1120px 以下转为两列，并在 760px 以下转为单列且无横向溢出。

---

### Task 1: 新页面功能契约测试

**Files:**
- Create: `novel/src/test/frontend/storyboard-workbench-new-page.test.js`
- Produces: 对独立新页面、关键 DOM 接口、原业务脚本及页面专用样式的静态回归保护。

- [ ] **Step 1: Write the failing test**

```js
const html = fs.readFileSync(path.join(root, 'static/storyboard-workbench-new.html'), 'utf8');
assert(html.includes('id="novelImportFile"'));
assert(html.includes('id="sceneTrack"'));
assert(html.includes('id="sceneEditor"'));
assert(html.includes('id="assetInspector"'));
assert(html.includes('id="videoStatus"'));
assert(html.includes('data-action="generate-video"'));
assert(html.includes('/studio/css/storyboard-workbench-new.css'));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test novel/src/test/frontend/storyboard-workbench-new-page.test.js`

Expected: `ENOENT` because `storyboard-workbench-new.html` does not exist yet.

### Task 2: 独立工作台结构与专用视觉系统

**Files:**
- Create: `novel/src/main/resources/static/storyboard-workbench-new.html`
- Create: `novel/src/main/resources/static/studio/css/storyboard-workbench-new.css`
- Consumes: Task 1 的页面接口契约及既有四个前端脚本。
- Produces: 可由现有脚本填充的三栏桌面工作台与窄屏单列体验。

- [ ] **Step 1: Write the minimal independent HTML shell**

```html
<body class="studio-theme studio-workbench-page storyboard-workbench-new-page" data-shell-page="storyboard">
  <main class="storyboard-new-workspace">...</main>
  <aside id="studioInspector">...</aside>
  <script src="/studio/js/api.js"></script>
  <script src="/js/storyboard-workbench.js"></script>
  <script src="/studio/js/workbench.js"></script>
  <script src="/studio/js/workbench-shell.js"></script>
</body>
```

- [ ] **Step 2: Add desktop, status and responsive CSS**

```css
.storyboard-workbench-new-page .storyboard-new-workspace {
  display: grid;
  grid-template-columns: minmax(240px, .6fr) minmax(480px, 1.5fr);
  gap: 16px;
}
@media (max-width: 1120px) {
  .storyboard-workbench-new-page .storyboard-new-workspace { grid-template-columns: minmax(0, 1fr); }
}
```

- [ ] **Step 3: Run focused test to verify it passes**

Run: `node --test novel/src/test/frontend/storyboard-workbench-new-page.test.js`

Expected: no assertion failures and a passing Node test process.

### Task 3: 回归验证与视觉检查

**Files:**
- Verify: `novel/src/test/frontend/*.test.js`
- Verify: `novel/src/main/resources/static/storyboard-workbench-new.html`

- [ ] **Step 1: Run all front-end static checks**

Run: `node --test novel/src/test/frontend/*.test.js`

Expected: all front-end checks pass without assertion failures.

- [ ] **Step 2: Run compilation or project test check**

Run: `mvn test -pl novel -DskipTests=false`

Expected: compilation succeeds. If the known `AiModelConfigServiceImplTest` failures recur, record their exact names and do not attribute them to this static front-end change.

- [ ] **Step 3: Inspect desktop and narrow preview states**

Open: `/storyboard-workbench-new.html?novelId=54&chapterNum=1`

Expected: no overlapping controls, current scene is central, the right panel keeps `videoStatus` and video controls visible, and narrow screen becomes a single column.

- [ ] **Step 4: Final diff and status check**

Run: `git diff --check` and `git status --short`

Expected: no whitespace errors; only the intended preview page, CSS, test and design records are changed.
