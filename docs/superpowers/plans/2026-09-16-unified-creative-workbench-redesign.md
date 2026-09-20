# Unified Creative Workbench Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild all creator-facing pages around one responsive workbench shell while preserving existing URLs, backend APIs, data, asynchronous tasks, and legacy page backups.

**Architecture:** Keep the current static HTML plus native JavaScript stack. Add a shared workbench shell, design tokens, status/task/asset/scene primitives, and page-specific renderers that consume the existing API endpoints. Existing routes remain valid; page-specific content moves into the shared shell instead of introducing a new framework or changing backend contracts.

**Tech Stack:** Spring Boot static resources, HTML, CSS, browser JavaScript, existing REST APIs, existing Node `assert` frontend smoke tests, Maven.

## Global Constraints

- Do not delete the existing `.legacy.html` and `.legacy.js` backups.
- Do not change backend controller paths, request payload contracts, task status values, or database behavior.
- Preserve direct navigation through `/outline`, `/history-list.html`, `/novel-detail.html?novelId=...`, `/asset-library.html`, `/storyboard-workbench.html`, and `/video-generation.html?novelId=...&chapterNum=...`.
- Keep the implementation dependency-free; use semantic HTML, native CSS, and native JavaScript.
- Every async action must expose loading, success, empty, and failure states and prevent duplicate submission.
- All layout regions must support 1440px, 1024px, 720px, and 375px widths without horizontal scrolling.
- Do not use emoji as system icons; use text labels or existing icon-free controls with accessible labels.

---

### Task 1: Add the shared workbench shell and design tokens

**Files:**
- Create: `novel/src/main/resources/static/studio/css/workbench-shell.css`
- Create: `novel/src/main/resources/static/studio/js/workbench-shell.js`
- Modify: `novel/src/main/resources/templates/index.html`
- Modify: `novel/src/main/resources/static/asset-library.html`
- Modify: `novel/src/main/resources/static/history-list.html`
- Modify: `novel/src/main/resources/static/novel-detail.html`
- Modify: `novel/src/main/resources/static/storyboard-workbench.html`
- Modify: `novel/src/main/resources/static/video-generation.html`
- Test: `novel/src/test/frontend/workbench-shell-page.test.js`

**Interfaces:**
- `workbench-shell.js` consumes `novelId`, `chapterNum`, and optional `data-shell-page` attributes from each page.
- It produces navigation links with the current query context, a page-level `aria-live` region, and a shared task drawer toggle.
- Page scripts continue to own API calls and business actions; the shell only owns navigation, context rendering, drawer state, and shared feedback.

- [ ] **Step 1: Write the failing smoke test** that asserts each page loads `workbench-shell.css` and `workbench-shell.js`, contains `data-shell-page`, and exposes a labeled task drawer trigger.

```js
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../../main/resources/static');
for (const file of ['asset-library.html', 'history-list.html', 'novel-detail.html', 'storyboard-workbench.html', 'video-generation.html']) {
  const html = fs.readFileSync(path.join(root, file), 'utf8');
  assert.match(html, /workbench-shell\.css/);
  assert.match(html, /workbench-shell\.js/);
  assert.match(html, /data-shell-page/);
}
```

- [ ] **Step 2: Run the test and confirm it fails** because the shared shell files and attributes do not exist yet.

Run: `node novel/src/test/frontend/workbench-shell-page.test.js`

Expected: FAIL with an assertion for the missing shell stylesheet or script.

- [ ] **Step 3: Implement the shell** with a three-column desktop grid, collapsible inspector, top project context, bottom action dock slot, focus-visible states, and responsive breakpoints. Keep CSS tokens in `:root` and use semantic status variables for success, warning, and error.

```html
<body class="workbench-page" data-shell-page="storyboard">
  <div class="workbench-shell">
    <aside class="workbench-nav" aria-label="工作区导航"></aside>
    <div class="workbench-main">
      <header class="workbench-context"></header>
      <main class="workbench-content"></main>
    </div>
    <aside class="workbench-inspector" aria-label="上下文面板"></aside>
  </div>
  <div class="workbench-live" aria-live="polite"></div>
</body>
```

- [ ] **Step 4: Run the smoke test** and confirm all five pages reference the shared shell.

Run: `node novel/src/test/frontend/workbench-shell-page.test.js`

Expected: PASS with five page checks.

### Task 2: Rebuild the project history and creation entry

**Files:**
- Modify: `novel/src/main/resources/templates/index.html`
- Modify: `novel/src/main/resources/static/history-list.html`
- Modify: `novel/src/main/resources/static/js/history-list.js`
- Modify: `novel/src/main/resources/static/js/storyboard-workbench.js`
- Test: `novel/src/test/frontend/history-list-page.test.js`
- Test: `novel/src/test/frontend/storyboard-workbench-page.test.js`

**Interfaces:**
- Keep `/api/novel/history`, `/api/novel/import`, and existing outline generation calls unchanged.
- History cards must continue to call the existing view, copy overview, and delete actions.
- Selecting a project routes to `/novel-detail.html?novelId=...` and carries the selected project context into the shell.

- [ ] **Step 1: Add assertions** for a project summary card, project status text, recent activity area, import entry, and retained existing action selectors.
- [ ] **Step 2: Run the history and storyboard page tests** to record the failing selectors.
- [ ] **Step 3: Replace the loose card/list layout** with a compact project workspace: summary row, filter/status strip, project grid, and a clear “进入工作台” action. Move destructive actions into a secondary menu while keeping their handlers.
- [ ] **Step 4: Update navigation URLs** so every project action includes `novelId` and routes to the new workbench shell; preserve outline restore behavior.
- [ ] **Step 5: Run both page tests** and verify no existing API path was removed.

Run: `node novel/src/test/frontend/history-list-page.test.js; node novel/src/test/frontend/storyboard-workbench-page.test.js`

Expected: PASS with no missing action or API assertions.

### Task 3: Rebuild the novel detail page as the project context hub

**Files:**
- Modify: `novel/src/main/resources/static/novel-detail.html`
- Modify: `novel/src/main/resources/static/js/novel-detail.js`
- Modify: `novel/src/main/resources/static/css/outline.css`
- Test: `novel/src/test/frontend/novel-detail-page.test.js`

**Interfaces:**
- Preserve visual-style endpoints, chapter endpoints, storyboard generate/save/export endpoints, asset extraction/polling endpoints, composite-image endpoints, download, save, and regenerate actions.
- `novel-detail.js` remains the owner of current chapter data and active tab state; the new layout only changes rendering containers.

- [ ] **Step 1: Add test coverage** for the chapter rail, chapter editor, visual-style inspector, storyboard summary, asset summary, and task status region.
- [ ] **Step 2: Run the detail page test** and confirm the new semantic regions are missing.
- [ ] **Step 3: Recompose the page** into a project header, chapter rail, central reading/editing surface, and right inspector with collapsible visual style, storyboard, and assets sections. Keep all existing IDs used by the script or update the script references in the same change.
- [ ] **Step 4: Replace alert-only async feedback** with inline status regions while keeping alert fallback only for missing required context. Add `aria-live="polite"` to generation and extraction status elements.
- [ ] **Step 5: Verify extraction state transitions**: idle → submitting → polling → completed/failed, with retry and refresh controls at the failed state.

Run: `node novel/src/test/frontend/novel-detail-page.test.js`

Expected: PASS and the browser shows all chapter, style, storyboard, asset, and task regions without duplicated script panels.

### Task 4: Rebuild the asset library with grid + inspector workflow

**Files:**
- Modify: `novel/src/main/resources/static/asset-library.html`
- Modify: `novel/src/main/resources/static/js/asset-library.js`
- Modify: `novel/src/main/resources/static/studio/js/assets.js`
- Test: `novel/src/test/frontend/asset-library-page.test.js`

**Interfaces:**
- Keep `/api/novels`, `/api/visual-assets/library`, `/api/visual-assets/{assetId}`, `/versions`, `/reuse`, chapter asset loading, save, merge, and composite-image polling calls.
- Asset selection must update a right-side inspector without leaving the current page.

- [ ] **Step 1: Add assertions** for filters, asset grid, selected-asset inspector, version list, reuse/merge controls, and empty/loading/error states.
- [ ] **Step 2: Run the asset page test** and confirm missing regions are reported.
- [ ] **Step 3: Implement the asset workspace**: left filter rail, responsive asset grid, right inspector drawer, thumbnail aspect-ratio reservation, concise metadata, and expandable prompt fields.
- [ ] **Step 4: Wire existing handlers** so save, three-view generation, merge, reuse, and refresh update only the selected asset and task status instead of replacing the whole page.
- [ ] **Step 5: Run the page test and manually verify** character/location filters, empty library, failed image task, and successful refresh.

Run: `node novel/src/test/frontend/asset-library-page.test.js`

Expected: PASS; asset operations remain available at desktop and become a drawer on narrow screens.

### Task 5: Rebuild the storyboard workbench around a scene timeline

**Files:**
- Modify: `novel/src/main/resources/static/storyboard-workbench.html`
- Modify: `novel/src/main/resources/static/js/storyboard-workbench.js`
- Modify: `novel/src/main/resources/static/studio/workbench.html`
- Modify: `novel/src/main/resources/static/studio/js/workbench.js`
- Modify: `novel/src/main/resources/static/studio/css/studio.css`
- Test: `novel/src/test/frontend/storyboard-workbench-page.test.js`

**Interfaces:**
- Preserve storyboard load/save/generate/export endpoints, first-frame task/upload/preview/delete endpoints, asset reference data, and video task entry points.
- Scene selection is the shared state boundary: the timeline, editor, asset inspector, first-frame state, and video status all render from the selected scene.

- [ ] **Step 1: Add test assertions** for scene timeline, selected-scene editor, first-frame preview, reference assets, action dock, task panel, and status/error regions.
- [ ] **Step 2: Run the test** and confirm the old duplicate layout does not satisfy the new semantic structure.
- [ ] **Step 3: Implement the timeline** with compact scene chips showing sequence, duration, shot type, first-frame state, and video state. Provide keyboard-accessible selection and visible selected state.
- [ ] **Step 4: Implement the selected-scene editor** with summary metadata, one script editor, first-frame preview/actions, reference asset chips, and a single bottom action dock. Do not render the same script in a second “镜头脚本” box.
- [ ] **Step 5: Integrate task/error feedback** into the right inspector and action dock with retry, safety rewrite, upload, and refresh operations.
- [ ] **Step 6: Run the page test and browser-check** at 1440px, 1024px, 720px, and 375px; confirm no action button is clipped or outside the viewport.

Run: `node novel/src/test/frontend/storyboard-workbench-page.test.js`

Expected: PASS; one authoritative script editor is visible and generation controls remain reachable at every supported width.

### Task 6: Rebuild video generation as an output console

**Files:**
- Modify: `novel/src/main/resources/static/video-generation.html`
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Modify: `novel/src/main/resources/static/css/outline.css`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- Keep video task creation, first-frame generation/upload/delete, video upload, polling, safety rewrite, current-version selection, history loading, and delete endpoints.
- Consume storyboard data as a compact read-only summary; the page must not create a second editable script textarea.

- [ ] **Step 1: Add test assertions** for output summary, scene output cards, resolution selector, first-frame state, current video, version history, task progress, and inline recovery actions.
- [ ] **Step 2: Run the test** to confirm the output-console structure is not present.
- [ ] **Step 3: Recompose each scene card** into preview header, prompt summary, first-frame strip, generation controls, current result, and collapsible history. Use a stable media aspect ratio so loading does not shift the page.
- [ ] **Step 4: Move generation status into each scene card** and keep the global page status for chapter-level failures only. Disable duplicate submissions and expose retry/safety rewrite actions at the failed task.
- [ ] **Step 5: Run the video page test and manually verify** no script duplication appears when navigating from detail or storyboard.

Run: `node novel/src/test/frontend/video-generation-page.test.js`

Expected: PASS; video generation is visually distinct from storyboard editing while sharing scene context.

### Task 7: Compatibility, responsive QA, and full verification

**Files:**
- Modify: `novel/src/main/resources/static/css/outline.css`
- Modify: `novel/src/main/resources/static/studio/css/studio.css`
- Modify: `novel/src/main/resources/static/js/*.js` only where query-context navigation is required
- Test: all files under `novel/src/test/frontend/`

**Interfaces:**
- No new backend calls are introduced in this task.
- All shared shell navigation links must preserve `novelId` and `chapterNum` when available.

- [ ] **Step 1: Run every frontend smoke test** and record the failing files, if any.

Run: `Get-ChildItem novel/src/test/frontend -Filter *.test.js | ForEach-Object { node $_.FullName }`

Expected: all tests exit with code 0.

- [ ] **Step 2: Run the Maven test suite** to ensure static-page changes did not affect backend behavior.

Run: `mvn test -f novel/pom.xml`

Expected: `BUILD SUCCESS` with zero test failures.

- [ ] **Step 3: Synchronize static resources** and verify each production route returns 200.

Run: `mvn resources:resources -f novel/pom.xml`

Expected: resource copy completes successfully; requests to `/outline`, `/history-list.html`, `/novel-detail.html?novelId=54`, `/asset-library.html`, `/storyboard-workbench.html`, and `/video-generation.html?novelId=54&chapterNum=1` return 200.

- [ ] **Step 4: Perform browser QA** at 1440px, 1024px, 720px, and 375px. Check navigation, chapter selection, extraction status, asset inspector, scene selection, first-frame actions, video task polling, retry/error states, and focus visibility.
- [ ] **Step 5: Run `git diff --check`** and inspect the final diff to confirm legacy backups remain and no unrelated backend files changed.

Run: `git diff --check; git status --short`

Expected: no whitespace errors; only intended frontend, test, and documentation files are modified; legacy files remain present.
