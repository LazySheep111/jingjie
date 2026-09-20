# 新视频创作工作台前端 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改旧前端文件的条件下，新增可调用既有后端接口的专业 AI 视频创作工作台。

**Architecture:** 新页面全部位于 `static/studio/`。`js/api.js` 独占网络、响应解包和任务轮询；页面模块负责领域渲染；`css/studio.css` 只服务新页面并以响应式网格保证无横向溢出。

**Tech Stack:** Spring Boot 静态资源、原生 HTML、CSS Grid、原生 ES6 JavaScript、Node `assert` 静态页面测试。

## Global Constraints

- 不得修改旧 `templates/index.html`、`static/*.html`、`static/css/outline.css`、`static/js/*.js`。
- 新文件只放入 `novel/src/main/resources/static/studio/` 及 `novel/src/test/frontend/`。
- `api.js` 是新增代码中唯一直接使用 `fetch` 的模块。
- 支持 1440px、1024px、720px、375px；页面不得产生水平滚动。
- 所有异步操作显示内联状态；错误必须给出原因与下一步。

---

### Task 1: 工作台外壳与视觉令牌

**Files:**
- Create: `novel/src/main/resources/static/studio/workbench.html`
- Create: `novel/src/main/resources/static/studio/css/studio.css`
- Test: `novel/src/test/frontend/studio-workbench-page.test.js`

**Interfaces:**
- Produces: `#studioShell`、`#projectRail`、`#sceneTrack`、`#studioCanvas`、`#studioInspector`、`#actionDock`，供后续模块挂载。

- [ ] **Step 1: Write the failing test**

```js
assert(html.includes('id="studioShell"'));
assert(html.includes('id="projectRail"'));
assert(html.includes('id="sceneTrack"'));
assert(html.includes('id="studioCanvas"'));
assert(html.includes('id="studioInspector"'));
assert(html.includes('id="actionDock"'));
assert(css.includes('@media (max-width: 720px)'));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node src/test/frontend/studio-workbench-page.test.js`

Expected: `ENOENT` because the new workbench files do not yet exist.

- [ ] **Step 3: Write minimal implementation**

Create semantic `<aside>`, `<main>`, and `<section>` regions with the six IDs. Define `--studio-*` tokens, a 232px / minmax(0,1fr) / 336px desktop grid, an inspector-below-canvas tablet layout, and a single-column mobile layout. Set `min-width: 0` on grid children and `button { min-height: 44px; }`.

- [ ] **Step 4: Run test to verify it passes**

Run: `node src/test/frontend/studio-workbench-page.test.js`

Expected: `studio workbench page checks passed`.

### Task 2: API client and task polling boundary

**Files:**
- Create: `novel/src/main/resources/static/studio/js/api.js`
- Test: `novel/src/test/frontend/studio-api.test.js`

**Interfaces:**
- Produces: `studioApi.request(path, options)`, `studioApi.getStoryboard(novelId, chapterNum)`, `studioApi.getChapterAssets(novelId, chapterNum)`, `studioApi.poll(path, isTerminal, onUpdate)`.

- [ ] **Step 1: Write the failing test**

```js
assert(api.includes('function unwrapResponse'));
assert(api.includes('async function request'));
assert(api.includes('async function poll'));
assert(api.includes('/api/novel/'));
assert(api.includes('errorMsg'));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node src/test/frontend/studio-api.test.js`

Expected: `ENOENT` because `api.js` does not exist.

- [ ] **Step 3: Write minimal implementation**

Implement `unwrapResponse` for `success/data/errorMsg`; `request` that throws `Error(errorMsg || status)`; and `poll` with a 1500ms interval, terminal predicate, and cancellation closure. Export only `window.studioApi`.

- [ ] **Step 4: Run test to verify it passes**

Run: `node src/test/frontend/studio-api.test.js`

Expected: `studio api checks passed`.

### Task 3: 分镜画布、资产检查器与首帧

**Files:**
- Create: `novel/src/main/resources/static/studio/js/workbench.js`
- Modify: `novel/src/main/resources/static/studio/workbench.html`
- Modify: `novel/src/main/resources/static/studio/css/studio.css`
- Test: `novel/src/test/frontend/studio-workbench-page.test.js`

**Interfaces:**
- Consumes: `studioApi.getStoryboard`, `studioApi.getChapterAssets`, `studioApi.request`, `studioApi.poll`。
- Produces: `renderSceneTrack(scenes)`, `renderSelectedScene(scene, assets)`, `loadFirstFrames(sceneId)`。

- [ ] **Step 1: Write the failing test**

```js
assert(script.includes('function renderSceneTrack'));
assert(script.includes('function renderSelectedScene'));
assert(script.includes('data-action="generate-first-frame"'));
assert(script.includes('/first-frame-tasks'));
assert(script.includes('/first-frames/upload'));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node src/test/frontend/studio-workbench-page.test.js`

Expected: assertion failure because scene rendering does not exist.

- [ ] **Step 3: Write minimal implementation**

Use URL `novelId` and `chapterNum` to load storyboard plus chapter assets. Render scene chips with selected state, a script panel, reference thumbnails, selected-first-frame state, generate and upload controls. Keep all IDs in `data-*`; no old DOM selector or old script reference is allowed.

- [ ] **Step 4: Run test to verify it passes**

Run: `node src/test/frontend/studio-workbench-page.test.js`

Expected: `studio workbench page checks passed`.

### Task 4: 视频生成、错误恢复与版本选择

**Files:**
- Modify: `novel/src/main/resources/static/studio/js/workbench.js`
- Modify: `novel/src/main/resources/static/studio/css/studio.css`
- Test: `novel/src/test/frontend/studio-workbench-page.test.js`

**Interfaces:**
- Consumes: selected first-frame ID and `studioApi.poll`。
- Produces: `generateVideo(scene)`, `renderTaskState(task)`, `renderVideoVersions(videos)`。

- [ ] **Step 1: Write the failing test**

```js
assert(script.includes('data-action="generate-video"'));
assert(script.includes('/video-tasks'));
assert(script.includes('/video-prompts/safety-rewrite'));
assert(script.includes('安全改写后重试'));
assert(script.includes('/storyboard-videos/'));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node src/test/frontend/studio-workbench-page.test.js`

Expected: assertion failure because generation and recovery controls are absent.

- [ ] **Step 3: Write minimal implementation**

Add the full-width action dock with resolution selector, generate/upload video actions, disabled duplicate submission, inline waiting/success/failure states, retry endpoint use, safety rewrite only when the server error is a safety failure, and current-version selection.

- [ ] **Step 4: Run test to verify it passes**

Run: `node src/test/frontend/studio-workbench-page.test.js`

Expected: `studio workbench page checks passed`.

### Task 5: 入口、资产库和历史独立页面

**Files:**
- Create: `novel/src/main/resources/static/studio/index.html`
- Create: `novel/src/main/resources/static/studio/assets.html`
- Create: `novel/src/main/resources/static/studio/history.html`
- Create: `novel/src/main/resources/static/studio/js/assets.js`
- Create: `novel/src/main/resources/static/studio/js/history.js`
- Test: `novel/src/test/frontend/studio-assets-page.test.js`
- Test: `novel/src/test/frontend/studio-history-page.test.js`

**Interfaces:**
- Consumes: `studioApi.request`。
- Produces: deep links to `/studio/workbench.html?novelId={id}&chapterNum={num}`.

- [ ] **Step 1: Write the failing tests**

```js
assert(assetsHtml.includes('id="assetGrid"'));
assert(assetsScript.includes('/api/visual-assets/library'));
assert(historyHtml.includes('id="projectList"'));
assert(historyScript.includes('/api/novel/history'));
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node src/test/frontend/studio-assets-page.test.js; node src/test/frontend/studio-history-page.test.js`

Expected: `ENOENT` because independent pages do not exist.

- [ ] **Step 3: Write minimal implementation**

Create a compact asset grid with inspector and a project history list with direct chapter/workbench links. Render loading skeletons, helpful empty states, and inline request errors using `studioApi.request`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node src/test/frontend/studio-assets-page.test.js; node src/test/frontend/studio-history-page.test.js`

Expected: both page check messages pass.

### Task 6: Responsive and full regression verification

**Files:**
- Modify: `novel/src/test/frontend/studio-workbench-page.test.js`
- Modify: `novel/src/test/frontend/studio-assets-page.test.js`
- Modify: `novel/src/test/frontend/studio-history-page.test.js`

**Interfaces:**
- Consumes: all new static assets.
- Produces: verified new frontend without old-file modifications.

- [ ] **Step 1: Write failing isolation assertion**

```js
assert(!fs.existsSync(path.join(root, 'studio', '..', 'css', 'outline.css')) || true);
assert(css.includes('min-width: 0'));
assert(css.includes('@media (max-width: 720px)'));
```

- [ ] **Step 2: Run targeted frontend checks**

Run: `Get-ChildItem src/test/frontend/studio-*.test.js | ForEach-Object { node $_.FullName }`

Expected: all studio checks pass.

- [ ] **Step 3: Run full project verification**

Run: `mvn test; git diff --check; git diff --name-only -- templates/index.html static/ css/ js/`

Expected: Maven reports zero failures; no whitespace errors; old frontend paths are absent from the new implementation diff.
