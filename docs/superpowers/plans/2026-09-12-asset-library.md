# Global Asset Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a desktop global asset-library page that groups visual assets by novel, supports filtering and editing, and lets users reuse a selected asset in a target chapter.

**Architecture:** Add a standalone `asset-library.html` page with its own JavaScript module and shared stylesheet rules. The page consumes paginated library data, novel choices, asset detail data, version-save responses, and reuse responses through the documented REST endpoints. Existing chapter asset behavior remains unchanged; the global page only creates new-version and chapter-reference requests.

**Tech Stack:** Existing Spring Boot static resources, vanilla HTML/CSS/JavaScript, Fetch API, MySQL-backed REST endpoints supplied by the backend.

## Global Constraints

- Desktop-only first version; do not add mobile layout work.
- Preserve existing chapter asset extraction and composite-image generation behavior.
- Saving edits creates a new asset version; never overwrite an existing version.
- Reusing an asset creates a chapter reference; never regenerate the image.
- Do not add a frontend dependency or change the image-generation provider.
- Do not push to Gitee.

---

### Task 1: Add the global navigation entry and page shell

**Files:**
- Create: `novel/src/main/resources/static/asset-library.html`
- Create: `novel/src/main/resources/static/js/asset-library.js`
- Modify: `novel/src/main/resources/static/novel-detail.html`
- Modify: `novel/src/main/resources/static/history-list.html`
- Modify: `novel/src/main/resources/static/css/outline.css`

**Interfaces:**
- Produces the `/asset-library.html` route and navigation links used by all three existing static pages.
- The page contains `#novelFilterList`, `#assetGrid`, `#assetDetailPanel`, `#assetDetailForm`, `#assetStatus`, `#assetSearchInput`, `#assetTypeFilter`, and `#assetPagination`.

- [ ] **Step 1: Add failing DOM assertions in a lightweight Node test**

Create `novel/src/test/frontend/asset-library-page.test.js` that reads `asset-library.html` and asserts that the page contains the three layout regions, the asset filter controls, and the navigation link `/asset-library.html`.

- [ ] **Step 2: Run the DOM assertion and verify it fails**

Run from the `novel/` application directory:

```powershell
node src/test/frontend/asset-library-page.test.js
```

Expected result: FAIL because the asset-library page and navigation links do not yet exist.

- [ ] **Step 3: Add the page shell and navigation links**

Use the existing header style and add this navigation block to `novel-detail.html` and `history-list.html`:

```html
<nav class="top-nav">
    <a href="/outline">新建创作</a>
    <a href="/asset-library.html">资产库</a>
    <a href="/history-list.html">我的历史作品</a>
</nav>
```

Create `asset-library.html` with a header and three desktop columns: novel filters, asset grid, and detail editor. Load `/css/outline.css` and `/js/asset-library.js`.

- [ ] **Step 4: Add shared desktop layout styles**

Add `.asset-library-layout`, `.asset-library-sidebar`, `.asset-library-main`, `.asset-library-detail`, `.asset-library-card`, `.asset-library-image`, `.asset-library-empty`, `.asset-library-pagination`, and `.asset-library-modal` rules to `outline.css`. Keep the existing colors, spacing, and image-preview behavior.

- [ ] **Step 5: Run the DOM assertion and syntax check**

```powershell
node src/test/frontend/asset-library-page.test.js
node -e "const fs=require('fs'); new Function(fs.readFileSync('src/main/resources/static/js/asset-library.js','utf8')); console.log('asset-library.js syntax ok')"
```

Expected result: both commands pass.

- [ ] **Step 6: Commit**

```powershell
git add novel/src/main/resources/static/asset-library.html novel/src/main/resources/static/js/asset-library.js novel/src/main/resources/static/novel-detail.html novel/src/main/resources/static/history-list.html novel/src/main/resources/static/css/outline.css novel/src/test/frontend/asset-library-page.test.js
git commit -m "feat: add asset library page shell"
```

### Task 2: Implement novel grouping, filtering, cards, and pagination

**Files:**
- Modify: `novel/src/main/resources/static/js/asset-library.js`
- Modify: `novel/src/test/frontend/asset-library-page.test.js`

**Interfaces:**
- Consumes `GET /api/novels` and `GET /api/visual-assets/library?novelId=&assetType=&keyword=&page=&pageSize=`.
- Produces selected asset state and card-click events for Task 3.

- [ ] **Step 1: Add test fixtures and pure rendering assertions**

Add fixture data containing two novels, one character, and one location. Assert that `buildAssetQuery({novelId: 53, assetType: 'CHARACTER', keyword: '凌', page: 2, pageSize: 20})` returns the correct encoded query and that `normalizeLibraryResponse` reads `records`, `total`, `page`, and `pageSize`.

- [ ] **Step 2: Run the test and verify the missing functions fail**

```powershell
node src/test/frontend/asset-library-page.test.js
```

Expected result: FAIL because the query and response-normalization helpers are not implemented.

- [ ] **Step 3: Implement loading and rendering**

Implement `loadNovels()`, `loadAssets()`, `buildAssetQuery(filters)`, `normalizeLibraryResponse(data)`, `renderNovelFilters(novels)`, `renderAssetCards(records)`, and `renderPagination(meta)`. The default filter is all novels, all asset types, page 1, page size 20. Each card displays asset name, novel title, asset type, version, reuse count, and thumbnail or placeholder.

- [ ] **Step 4: Wire interactions**

Wire novel selection, asset type selection, keyword search on Enter or a search button, page changes, and card selection. Every filter change resets the page to 1 and reloads the library endpoint.

- [ ] **Step 5: Run tests and syntax checks**

```powershell
node src/test/frontend/asset-library-page.test.js
node -e "const fs=require('fs'); new Function(fs.readFileSync('src/main/resources/static/js/asset-library.js','utf8')); console.log('asset-library.js syntax ok')"
```

Expected result: PASS.

- [ ] **Step 6: Commit**

```powershell
git add novel/src/main/resources/static/js/asset-library.js novel/src/test/frontend/asset-library-page.test.js
git commit -m "feat: browse assets by novel and type"
```

### Task 3: Implement asset detail editing and new-version save

**Files:**
- Modify: `novel/src/main/resources/static/js/asset-library.js`
- Modify: `novel/src/test/frontend/asset-library-page.test.js`

**Interfaces:**
- Consumes `GET /api/visual-assets/{assetId}`.
- Produces `POST /api/visual-assets/{assetId}/versions` with `coreFeatures`, `frontPrompt`, `sidePrompt`, and `backPrompt`.

- [ ] **Step 1: Add request-payload tests**

Assert that `buildVersionPayload(formValues)` includes exactly the four editable fields and that empty fields remain strings rather than becoming `undefined`.

- [ ] **Step 2: Run the test and verify it fails**

```powershell
node src/test/frontend/asset-library-page.test.js
```

Expected result: FAIL because the payload helper is missing.

- [ ] **Step 3: Implement detail loading and rendering**

Implement `loadAssetDetail(assetId)` and `renderAssetDetail(asset)`. Show image preview, asset name, novel title, type, version, reuse count, core features, three prompts, and a “保存为新版本” button. Reuse the existing full-image preview behavior for the composite image.

- [ ] **Step 4: Implement version save**

Implement `saveAssetVersion(assetId)`. Disable the save button while submitting, send JSON to the version endpoint, show success or error status, replace the detail with the returned current version, and refresh the card list without losing the selected asset.

- [ ] **Step 5: Run tests and syntax checks**

```powershell
node src/test/frontend/asset-library-page.test.js
node -e "const fs=require('fs'); new Function(fs.readFileSync('src/main/resources/static/js/asset-library.js','utf8')); console.log('asset-library.js syntax ok')"
```

Expected result: PASS.

- [ ] **Step 6: Commit**

```powershell
git add novel/src/main/resources/static/js/asset-library.js novel/src/test/frontend/asset-library-page.test.js
git commit -m "feat: edit asset prompts as new versions"
```

### Task 4: Implement asset reuse and final verification

**Files:**
- Modify: `novel/src/main/resources/static/js/asset-library.js`
- Modify: `novel/src/test/frontend/asset-library-page.test.js`

**Interfaces:**
- Consumes `GET /api/novel/{novelId}/chapters` for target chapters.
- Produces `POST /api/visual-assets/{assetId}/reuse` with `targetNovelId`, `targetChapterNum`, and `assetRole`.

- [ ] **Step 1: Add reuse-payload tests**

Assert that `buildReusePayload({targetNovelId: 54, targetChapterNum: 3, assetRole: 'CHARACTER'})` returns the exact expected object and rejects missing target novel or chapter values.

- [ ] **Step 2: Run the test and verify it fails**

```powershell
node src/test/frontend/asset-library-page.test.js
```

Expected result: FAIL because reuse helpers are missing.

- [ ] **Step 3: Implement reuse dialog and request**

Implement `openReuseDialog(asset)`, `loadTargetChapters(targetNovelId)`, `submitReuse(assetId)`, and `buildReusePayload(values)`. The dialog contains target novel, target chapter, and asset role fields. On success, close the dialog, refresh library results, and show a success message.

- [ ] **Step 4: Run frontend checks and backend compile**

```powershell
node src/test/frontend/asset-library-page.test.js
node -e "const fs=require('fs'); new Function(fs.readFileSync('src/main/resources/static/js/asset-library.js','utf8')); console.log('asset-library.js syntax ok')"
mvn -q -DskipTests compile
```

Expected result: all commands pass.

- [ ] **Step 5: Manual browser verification**

With the Spring Boot server running, open `/asset-library.html` and verify: navigation opens the page; novels load; type and keyword filters reload cards; clicking a card loads detail; clicking a generated image opens the large preview; saving creates a new version; reuse sends the selected novel, chapter, and role.

- [ ] **Step 6: Commit**

```powershell
git add novel/src/main/resources/static/js/asset-library.js novel/src/test/frontend/asset-library-page.test.js
git commit -m "feat: reuse visual assets from library"
```
