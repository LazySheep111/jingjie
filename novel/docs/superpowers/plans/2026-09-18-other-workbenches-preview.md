# 其余创作工作台新版预览 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为六个既有工作台创建统一设计的独立新版预览页，而不替换旧入口。

**Architecture:** 每个新 HTML 保留原脚本依赖的元素 ID、表单、对话框和脚本顺序。一个新作用域 CSS 文件提供通用布局、状态和响应式规则；业务数据仍只通过原 JavaScript 和原后端接口加载。

**Tech Stack:** Spring Boot 静态资源、语义化 HTML、原生 CSS、现有原生 JavaScript、Node `node --test`。

## Global Constraints

- 不修改后端接口、数据库、旧页面或既有业务 JavaScript。
- 新入口：`outline-new.html`、`novel-detail-new.html`、`history-list-new.html`、`asset-library-new.html`、`video-generation-new.html`、`model-management-new.html`。
- 不伪造小说、资产、任务、生成结果或接口响应。
- 所有按钮在 1200px、960px、760px 下可换行、不溢出；状态紧邻对应操作。

---

### Task 1: 公共样式与页面契约测试

**Files:**
- Create: `src/main/resources/static/studio/css/workbenches-new.css`
- Create: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `body[data-shell-page]` 和 `/studio/css/workbench-shell.css`。
- Produces: `.new-workbench-page`、`.new-workbench-toolbar`、`.new-workbench-columns`、`.new-workbench-panel`、状态色和三档断点。

- [ ] **Step 1: Write the failing test**

```js
test('new pages load scoped CSS and their original business scripts', () => {
  for (const page of pages) {
    const html = read(page.file);
    assert.match(html, /\/studio\/css\/workbenches-new\.css/);
    assert.match(html, page.script);
  }
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL because preview files do not exist.

- [ ] **Step 3: Implement common CSS**

```css
.new-workbench-page { min-height: 100dvh; background: var(--canvas); color: var(--ink); }
.new-workbench-toolbar { display: flex; flex-wrap: wrap; gap: 10px; }
@media (max-width: 960px) { .new-workbench-columns { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .new-workbench-toolbar > * { width: 100%; } }
```

Add scoped tokens, focus styles, semantic status styles and reduced-motion fallback.

- [ ] **Step 4: Re-run test**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL only for missing preview pages.

### Task 2: 小说大纲与小说全文预览

**Files:**
- Create: `src/main/resources/static/outline-new.html`
- Create: `src/main/resources/static/novel-detail-new.html`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `/js/outline.js` IDs `outlineForm`, `novelTitle`, `category`, `submitBtn`, `statusText`。
- Consumes: `/js/novel-detail.js` IDs `chapterNav`, `chapterContent`, `saveChapterBtn`, `regenerateChapterBtn`, `storyboardTabBtn`, `visualStyleEra` 和其余视觉风格字段。
- Produces: 大纲设定/结果双区与全文目录/正文/分镜三列布局。

- [ ] **Step 1: Extend the failing test**

```js
test('reader preview preserves content and storyboard mounts', () => {
  const html = read('novel-detail-new.html');
  for (const id of ['chapterNav', 'chapterContent', 'storyboardTabBtn', 'visualStyleEra']) {
    assert.match(html, new RegExp(`id="${id}"`));
  }
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL because the two HTML files are absent.

- [ ] **Step 3: Implement both pages**

Put all original outline fields in a left setting panel, its dynamic results in a right result panel, and `statusText` beside submit actions. Put `chapterNav`, `chapterContent`, and all storyboard/visual-style mounts in the reader’s three responsive columns. Load original scripts unchanged.

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: these two page contracts PASS.

### Task 3: 历史作品与资产库预览

**Files:**
- Create: `src/main/resources/static/history-list-new.html`
- Create: `src/main/resources/static/asset-library-new.html`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `/js/history-list.js` IDs `refreshBtn`, `historyList`, `historyEmpty`, `prevPageBtn`, `pageInfo`, `nextPageBtn`。
- Consumes: `/js/asset-library.js` IDs `assetStatus`, `novelSearchInput`, `novelFilterList`, `assetSearchInput`, `assetTypeFilter`, `assetSearchBtn`, `assetGrid`, `assetDetailPanel`, `assetReuseModal`, `assetImagePreviewModal`。
- Produces: 紧凑作品列表和筛选工具栏；资产网格和响应式详情检查器。

- [ ] **Step 1: Extend the failing test**

```js
test('asset preview retains grid, detail inspector and dialogs', () => {
  const html = read('asset-library-new.html');
  for (const id of ['assetGrid', 'assetDetailPanel', 'assetReuseModal', 'assetImagePreviewModal']) {
    assert.match(html, new RegExp(`id="${id}"`));
  }
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL because these preview files are absent.

- [ ] **Step 3: Implement both pages**

Place history filtering and refresh in a toolbar above `historyList`, retain pagination below. Place asset filtering in a toolbar, retain `assetGrid`, preserve dialogs unchanged, and make `assetDetailPanel` a desktop inspector that becomes inline below 960px.

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: these two page contracts PASS.

### Task 4: 视频创作与模型管理预览

**Files:**
- Create: `src/main/resources/static/video-generation-new.html`
- Create: `src/main/resources/static/model-management-new.html`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `/js/video-generation.js` IDs `pageTitle`, `pageSummary`, `backDetailBtn`, `pageStatus`, `sceneList`, `pageEmpty`, `firstFramePreviewDialog`, `firstFramePreviewImage`。
- Consumes: `/studio/js/api.js`、`/studio/js/model-management.js` IDs `modelPageStatus`、`modelConfigGrid`。
- Produces: 分镜为导航的制作台，以及三种真实模型配置卡的连续操作流。

- [ ] **Step 1: Extend the failing test**

```js
test('video and model previews keep all business mount points', () => {
  assert.match(read('video-generation-new.html'), /id="sceneList"/);
  assert.match(read('video-generation-new.html'), /id="firstFramePreviewDialog"/);
  assert.match(read('model-management-new.html'), /id="modelConfigGrid"/);
  assert.match(read('model-management-new.html'), /\/studio\/js\/model-management\.js/);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL because video and model preview files are absent.

- [ ] **Step 3: Implement both pages**

Keep `sceneList` as the original video script’s dynamic mount and show `pageStatus`/`pageEmpty` immediately above it; preserve the first-frame dialog. Use `modelConfigGrid` as the only configuration mount and `modelPageStatus` as the only real status mount; do not add fake task summaries.

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: all six preview-page contracts PASS.

### Task 5: 全量验证与打包

**Files:**
- Verify: six new preview HTML files and `studio/css/workbenches-new.css`。

**Interfaces:**
- Consumes: 新 HTML、共享 CSS 和原业务脚本。
- Produces: 可由 Spring Boot 静态目录提供的独立预览入口。

- [ ] **Step 1: Run all frontend tests**

Run: `node --test src/test/frontend/*.test.js`

Expected: PASS.

- [ ] **Step 2: Package static resources**

Run: `mvn package -DskipTests`

Expected: BUILD SUCCESS.

- [ ] **Step 3: Inspect the JAR**

```powershell
jar tf target/novel-0.0.1-SNAPSHOT.jar | Select-String 'static/(outline-new.html|novel-detail-new.html|history-list-new.html|asset-library-new.html|video-generation-new.html|model-management-new.html|studio/css/workbenches-new.css)'
```

Expected: seven matching paths.

- [ ] **Step 4: Commit only new files**

```bash
git add src/main/resources/static/outline-new.html src/main/resources/static/novel-detail-new.html src/main/resources/static/history-list-new.html src/main/resources/static/asset-library-new.html src/main/resources/static/video-generation-new.html src/main/resources/static/model-management-new.html src/main/resources/static/studio/css/workbenches-new.css src/test/frontend/workbenches-new-pages.test.js
git commit -m "feat: add remaining preview workbenches"
```
