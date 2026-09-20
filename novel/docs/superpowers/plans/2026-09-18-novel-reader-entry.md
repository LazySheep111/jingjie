# 小说全文工作台入口整合 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 TXT/DOCX 小说导入和作品选择入口整合进新版小说全文工作台，并移除新版侧栏的重复分镜入口。

**Architecture:** `novel-detail-new.html` 添加一个仅在无当前作品时可见的导入空状态和一个已选作品时可用的“导入新小说”弹层。`novel-detail.js` 复用现有 `/api/novel/import` 协议，负责导入状态、保存 `mirror:selectedNovelId` 和跳转。共享侧栏只删除新版分镜入口，不删除旧页面文件或后端接口。

**Tech Stack:** 静态 HTML/CSS/JavaScript、Node 内置测试、Spring Boot 静态资源复制。

## Global Constraints

- 不修改后端接口、请求参数、数据库结构和上传限制。
- 不删除 `storyboard-workbench-new.html` 或旧版分镜页面。
- 保留全文页面既有元素 ID、章节编辑、分镜、资产和视频跳转逻辑。
- 新导航仅移除新版“分镜脚本工作台”项。
- 不创建 Git 提交：仓库含有用户已有的暂存及未提交改动。

---

### Task 1: 小说全文工作台导入入口与空状态

**Files:**
- Modify: `src/main/resources/static/novel-detail-new.html`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Test: `src/test/frontend/novel-detail-page.test.js`

**Interfaces:**
- Consumes: `#novelTitle`、`#generationStatus`、`#readerLayout` 与既有全文页面布局。
- Produces: `#importNovelBtn`、`#readerEntryState`、`#readerImportDialog`、`#readerImportFile`、`#readerImportTitle`、`#readerImportSubmitBtn`、`#readerImportStatus`、`#readerHistoryBtn`。

- [ ] **Step 1: Write the failing test**

在 `novel-detail-page.test.js` 增加断言，要求新版全文页面包含下列入口元素，并且样式表具备隐藏弹层规则：

```js
assert.match(newHtml, /id="readerEntryState"/);
assert.match(newHtml, /id="readerImportDialog"[^>]*hidden/);
assert.match(newHtml, /id="readerImportFile"[^>]*accept="\.txt,\.docx,text\/plain,application\/vnd\.openxmlformats-officedocument\.wordprocessingml\.document"/);
assert.match(newHtml, /id="readerImportSubmitBtn"/);
assert.match(newHtml, /id="readerHistoryBtn"/);
assert.match(css, /\.new-workbench-page \.reader-import-dialog\[hidden\]\s*\{\s*display:\s*none/);
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node src/test/frontend/novel-detail-page.test.js`

Expected: failure because the new import-entry IDs and dialog CSS do not exist.

- [ ] **Step 3: Add minimal page markup and styles**

在页头按钮组新增：

```html
<button id="importNovelBtn" type="button" class="secondary-button">导入新小说</button>
```

在 `reader-workspace` 前添加无作品入口：

```html
<section id="readerEntryState" class="reader-entry-state" hidden>
  <p class="eyebrow">开始创作</p>
  <h2>导入小说后开始章节与分镜创作</h2>
  <p>支持 TXT、DOCX 文件。也可以从已保存的历史作品继续创作。</p>
  <div class="card-actions">
    <button id="readerEntryImportBtn" type="button">导入 TXT / DOCX</button>
    <button id="readerHistoryBtn" type="button" class="secondary-button">从历史作品选择</button>
  </div>
</section>
```

在页面末尾新增隐藏弹层：

```html
<div id="readerImportDialog" class="reader-import-dialog" hidden role="dialog" aria-modal="true" aria-labelledby="readerImportTitle">
  <section class="reader-import-card">
    <div class="new-workbench-toolbar"><h2 id="readerImportTitle">导入新小说</h2><button id="closeReaderImportBtn" type="button" class="secondary-button">关闭</button></div>
    <label>小说文件<input id="readerImportFile" type="file" accept=".txt,.docx,text/plain,application/vnd.openxmlformats-officedocument.wordprocessingml.document"></label>
    <label>作品名称（可选）<input id="readerImportTitleInput" type="text" placeholder="默认使用文件名"></label>
    <p id="readerImportStatus" class="status" aria-live="polite"></p>
    <div class="card-actions"><button id="readerImportSubmitBtn" type="button">导入并开始创作</button><button id="cancelReaderImportBtn" type="button" class="secondary-button">取消</button></div>
  </section>
</div>
```

在 `workbenches-new.css` 添加 `.reader-entry-state` 的居中卡片样式，以及：

```css
.new-workbench-page .reader-import-dialog[hidden] { display:none; }
.new-workbench-page .reader-import-dialog { position:fixed; z-index:30; inset:0; display:grid; place-items:center; padding:20px; background:rgba(21,29,30,.52); }
.new-workbench-page .reader-import-card { width:min(520px,100%); border-radius:14px; background:var(--new-panel); padding:22px; box-shadow:var(--new-shadow); }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `node src/test/frontend/novel-detail-page.test.js`

Expected: `novel-detail visual-style checks passed`.

### Task 2: 复用导入 API 与当前作品上下文

**Files:**
- Modify: `src/main/resources/static/js/novel-detail.js`
- Test: `src/test/frontend/novel-detail-page.test.js`

**Interfaces:**
- Consumes: Task 1 的导入元素，以及 `POST /api/novel/import` 返回的 `{ novelId }`。
- Produces: `openReaderImportDialog()`、`closeReaderImportDialog()`、`importNovelFromReader()`；成功时写入 `localStorage['mirror:selectedNovelId']` 并跳转到 `/novel-detail-new.html?novelId=...`。

- [ ] **Step 1: Write the failing test**

在 `novel-detail-page.test.js` 增加：

```js
assert(script.includes("fetch('/api/novel/import', {method: 'POST', body: formData})"));
assert(script.includes("localStorage.setItem('mirror:selectedNovelId', String(result.novelId))"));
assert(script.includes('window.location.href = `/novel-detail-new.html?novelId=${encodeURIComponent(result.novelId)}`'));
assert(script.includes("generationStatus.textContent = '请选择或导入一部小说后开始创作。'"));
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node src/test/frontend/novel-detail-page.test.js`

Expected: failure because the reader page does not yet own import behavior or the new empty-state copy.

- [ ] **Step 3: Implement the minimal import behavior**

在 `novel-detail.js` 获取 Task 1 的元素；仅在这些元素存在时绑定事件。实现：

```js
async function importNovelFromReader() {
  const file = readerImportFile.files && readerImportFile.files[0];
  if (!file) return setReaderImportStatus('请选择 TXT 或 DOCX 文件。', true);
  if (!/\.(txt|docx)$/i.test(file.name)) return setReaderImportStatus('暂只支持 TXT 和 DOCX 文件。', true);
  const formData = new FormData();
  formData.append('file', file);
  formData.append('novelTitle', readerImportTitleInput.value.trim());
  readerImportSubmitBtn.disabled = true;
  setReaderImportStatus('正在读取小说并识别章节，请稍候...');
  try {
    const response = await fetch('/api/novel/import', {method: 'POST', body: formData});
    const payload = await response.json().catch(() => ({}));
    if (!response.ok || payload.success === false) throw new Error(payload.errorMsg || payload.message || `导入失败：${response.status}`);
    const result = payload.data || payload;
    if (!result.novelId) throw new Error('导入成功但未返回小说 ID');
    localStorage.setItem('mirror:selectedNovelId', String(result.novelId));
    window.location.href = `/novel-detail-new.html?novelId=${encodeURIComponent(result.novelId)}`;
  } catch (error) {
    setReaderImportStatus(error.message, true);
  } finally {
    readerImportSubmitBtn.disabled = false;
  }
}
```

将无 `novelId` 分支改为显示 `readerEntryState`、隐藏正文布局，并设置：

```js
novelTitle.textContent = '加载完成';
generationStatus.textContent = '请选择或导入一部小说后开始创作。';
readerEntryState.hidden = false;
readerLayout.hidden = true;
```

将“返回创作页”保留原功能；“回到历史列表”和空状态的历史入口统一导航到 `/history-list-new.html`。

- [ ] **Step 4: Run the test to verify it passes**

Run: `node src/test/frontend/novel-detail-page.test.js`

Expected: `novel-detail visual-style checks passed`.

### Task 3: 清理新版侧栏重复入口

**Files:**
- Modify: `src/main/resources/static/studio/js/workbench-shell.js`
- Test: `src/test/frontend/workbench-shell-page.test.js`

**Interfaces:**
- Consumes: `selectedNovelId`、`videoContextQuery` 与现有新版导航数组。
- Produces: 无“分镜脚本工作台”导航项，保留“视频创作工作台”的上下文参数。

- [ ] **Step 1: Write the failing test**

在 `workbench-shell-page.test.js` 增加：

```js
assert.ok(!shellJs.includes("['/storyboard-workbench-new.html'"), 'new navigation should not include the redundant storyboard workspace');
assert.ok(shellJs.includes("['/video-generation-new.html' + videoContextQuery, '视频创作工作台', 'video']"));
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node src/test/frontend/workbench-shell-page.test.js`

Expected: failure because the storyboard navigation item is still present.

- [ ] **Step 3: Remove only the navigation item**

从 `navItems` 删除：

```js
['/storyboard-workbench-new.html' + videoContextQuery, '分镜脚本工作台', 'storyboard'],
```

不要修改 `storyboard-workbench-new.html`、`storyboard-workbench.js`、视频上下文读取函数或任何后端接口。

- [ ] **Step 4: Run the test to verify it passes**

Run: `node src/test/frontend/workbench-shell-page.test.js`

Expected: `workbench shell checks passed`.

### Task 4: 集成验证与本地资源同步

**Files:**
- Modify: `src/main/resources/static/novel-detail-new.html`（将 `workbenches-new.css` 查询版本递增为 `20260918k`）
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`

**Interfaces:**
- Consumes: Tasks 1–3 的静态资源。
- Produces: 本地 `target/classes` 中可被 localhost 服务读取的新版资源。

- [ ] **Step 1: Run all targeted frontend checks**

Run each command separately:

```powershell
node src/test/frontend/novel-detail-page.test.js
node src/test/frontend/workbench-shell-page.test.js
node --test src/test/frontend/workbenches-new-pages.test.js
```

Expected: each command exits `0`.

- [ ] **Step 2: Copy static resources and package**

Run each command separately:

```powershell
mvn resources:resources -DskipTests
mvn package -DskipTests
```

Expected: both commands end with `BUILD SUCCESS`.

- [ ] **Step 3: Browser verification without triggering import**

Open `/novel-detail-new.html` with no query parameters and verify:

```js
({
  entryVisible: !document.getElementById('readerEntryState').hidden,
  readerHidden: document.getElementById('readerLayout').hidden,
  importDialogHidden: document.getElementById('readerImportDialog').hidden,
  status: document.getElementById('generationStatus').textContent
})
```

Expected: `entryVisible: true`, `readerHidden: true`, `importDialogHidden: true`, and the status is `请选择或导入一部小说后开始创作。`.

- [ ] **Step 4: Browser verification with an existing novel**

Open `/novel-detail-new.html?novelId=54` and verify `#importNovelBtn` is visible, `#readerEntryState` remains hidden, and the existing reader layout/API requests still load. Do not submit an upload, generate assets, or generate video during this check.
