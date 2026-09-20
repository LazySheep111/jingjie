# 创作工作台布局重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将小说创作流程的六个页面重构为具有明确任务层级的雾蓝创作工作台。

**Architecture:** 静态 HTML 增加页面级工作区容器；现有 JavaScript 的查询 ID、动作属性和请求代码保持不变，必要时只增加渲染容器类。`outline.css` 以页面级类定义布局，避免影响非目标页面。

**Tech Stack:** HTML、CSS、原生 JavaScript、Node 静态断言、Maven。

## Global Constraints

- 保持所有路由、导航文字、ID、`data-action`、`data-role` 和 API 地址不变。
- 青蓝 `#177e9f` 为唯一高强调色；不引入第三方 UI 库。
- 多栏布局在 900px 以下折叠为单栏。

---

### Task 1: 编写布局结构的失败断言

**Files:**
- Modify: `novel/src/test/frontend/{storyboard-workbench-page,novel-detail-page,asset-library-page,video-generation-page,history-list-page}.test.js`

- [ ] **Step 1: 断言核心页面工作区类**

```js
assert(html.includes('class="workspace-header"'));
assert(html.includes('class="workspace-shell"'));
```

- [ ] **Step 2: 运行对应测试并确认失败**

Run: `node src/test/frontend/storyboard-workbench-page.test.js`

Expected: 结构断言失败，证明旧布局未满足新设计。

### Task 2: 重组静态工作区结构

**Files:**
- Modify: `novel/src/main/resources/templates/index.html`
- Modify: `novel/src/main/resources/static/{storyboard-workbench,novel-detail,asset-library,history-list,video-generation}.html`

- [ ] **Step 1: 添加不承载业务状态的语义容器**

```html
<header class="workspace-header">...</header>
<div class="workspace-shell">...</div>
```

- [ ] **Step 2: 重跑前端测试**

Run: `node src/test/frontend/<page>.test.js`

Expected: 新结构与既有动作断言全部通过。

### Task 3: 为动态数据添加页面语义类

**Files:**
- Modify: `novel/src/main/resources/static/js/{history-list,asset-library,video-generation}.js`
- Test: `novel/src/test/frontend/{history-list-page,asset-library-page,video-generation-page}.test.js`

- [ ] **Step 1: 断言画廊、项目行和阶段容器类**

```js
assert(script.includes('workspace-project-row'));
assert(script.includes('asset-gallery-card'));
```

- [ ] **Step 2: 为既有动态节点添加类名，不改变字段或事件绑定**

```js
card.className = 'history-card workspace-project-row';
```

- [ ] **Step 3: 运行对应静态测试**

Run: `node src/test/frontend/history-list-page.test.js`

Expected: `history-list page checks passed`。

### Task 4: 实现页面级布局和响应式样式

**Files:**
- Modify: `novel/src/main/resources/static/css/outline.css`

- [ ] **Step 1: 定义工作区头部、左右侧栏、内容画廊和操作停靠区**

```css
.workspace-header { display: flex; justify-content: space-between; }
.studio-asset-library-page .workspace-shell { display: grid; grid-template-columns: 220px minmax(0, 1fr) 320px; }
```

- [ ] **Step 2: 为窄屏写入单栏规则**

```css
@media (max-width: 900px) {
  .workspace-shell { grid-template-columns: 1fr; }
}
```

- [ ] **Step 3: 运行全部前端静态测试**

Run: `Get-ChildItem src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }`

Expected: 每个脚本通过。

### Task 5: 完整验证

**Files:**
- Verify: `novel/src/test/frontend/*.test.js`
- Verify: `novel/pom.xml`

- [ ] **Step 1: 执行全量前端静态测试**

Run: `Get-ChildItem src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }`

Expected: 所有页面检查通过。

- [ ] **Step 2: 执行 Maven 全量测试**

Run: `mvn test`

Expected: 构建成功，失败与错误均为 0。

- [ ] **Step 3: 执行差异格式检查**

Run: `git diff --check`

Expected: 无输出。
