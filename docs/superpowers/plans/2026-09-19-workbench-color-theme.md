# 工作台主题配色 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将新版工作台和 AI 助手面板统一为参考图中的深墨绿、米灰、暖白和鼠尾草绿主题，同时保留旧页面和现有交互逻辑。

**Architecture:** 以 `workbench-shell.css` 的新版主题变量为全局工作台色彩来源，覆盖导航、画布、卡片、边框、按钮和状态色；以 `assistant-panel.css` 的变量同步 AI 助手按钮、抽屉和消息状态。只修改新版页面引用的 CSS 和缓存版本，不触碰 API、DOM ID 或业务脚本。

**Tech Stack:** 原生 HTML/CSS/JavaScript、Node.js 前端静态检查。

## Global Constraints

- 不修改后端接口、数据库、DOM ID、事件逻辑和数据格式。
- 不删除旧页面；旧版页面继续使用原有样式。
- 统一采用深墨绿侧栏、米灰背景、暖白卡片、鼠尾草绿强调色。
- 保留错误状态的红色语义，不能用主题绿替代错误提示。
- 修改后必须执行前端检查和 `git diff --check`。

---

### Task 1: 统一新版工作台主题变量

**Files:**
- Modify: `novel/src/main/resources/static/studio/css/workbench-shell.css`
- Modify: 新版页面中的 `workbench-shell.css` 缓存版本引用

**Interfaces:**
- Consumes: 现有 `.workbench-*`、`.panel`、页面组合样式。
- Produces: 新版页面可复用的主题变量：`--wb-bg`、`--wb-surface`、`--wb-ink`、`--wb-muted`、`--wb-line`、`--wb-accent`、`--wb-accent-dark`、`--wb-success`、`--wb-warning`、`--wb-danger`。

- [ ] **Step 1: 调整主题变量和共用控件颜色**

将新版主题变量固定为：

```css
:root {
    --wb-canvas: #f0f2ec;
    --wb-bg: var(--wb-canvas);
    --wb-surface: #fffefa;
    --wb-surface-muted: #eef1e9;
    --wb-ink: #21332e;
    --wb-muted: #718078;
    --wb-line: #d9dfd6;
    --wb-accent: #55765b;
    --wb-accent-dark: #48694f;
    --wb-success: #55765b;
    --wb-warning: #b77935;
    --wb-danger: #a94d42;
}
```

并同步导航、焦点环、卡片阴影、资产选中态和视频控件的残留蓝色。

- [ ] **Step 2: 更新新版页面 CSS 缓存版本**

将新版 HTML 中的 `/studio/css/workbench-shell.css` 查询参数统一提升为新版本，确保浏览器不会继续使用旧缓存；不修改旧页面引用。

- [ ] **Step 3: 运行页面静态检查**

Run: `node novel/src/test/frontend/workbench-shell-page.test.js`

Expected: `workbench shell checks passed`。

### Task 2: 同步 AI 助手主题

**Files:**
- Modify: `novel/src/main/resources/static/studio/css/assistant-panel.css`
- Modify: 新版页面中的 `assistant-panel.css` 缓存版本引用

**Interfaces:**
- Consumes: 现有 `.assistant-trigger`、`.assistant-drawer`、`.assistant-message` 样式。
- Produces: 与工作台一致的助手按钮、抽屉、输入框和消息状态样式。

- [ ] **Step 1: 替换助手主题变量**

将助手变量与工作台主题保持一致：

```css
:root {
    --assistant-ink: #21332e;
    --assistant-muted: #718078;
    --assistant-line: #d9dfd6;
    --assistant-paper: #fffefa;
    --assistant-soft: #eef1e9;
    --assistant-accent: #55765b;
    --assistant-accent-dark: #48694f;
}
```

助手固定悬浮按钮使用鼠尾草绿实心样式，抽屉保持暖白，用户消息使用浅绿底，错误消息继续使用浅红底。

- [ ] **Step 2: 清理不再使用的旧助手定位样式**

删除未被脚本使用的 `.assistant-trigger-inline`、`.assistant-trigger-context` 和相关媒体查询，保留 `.assistant-trigger-float` 的固定定位样式。

- [ ] **Step 3: 更新助手脚本和样式缓存版本**

将新版页面的 `/studio/css/assistant-panel.css` 和 `/studio/js/assistant-panel.js` 查询参数统一提升，保证固定悬浮按钮和主题样式一起刷新。

### Task 3: 回归验证

**Files:**
- Test: `novel/src/test/frontend/assistant-panel.test.js`
- Test: `novel/src/test/frontend/workbench-shell-page.test.js`

- [ ] **Step 1: 运行 AI 助手检查**

Run: `node novel/src/test/frontend/assistant-panel.test.js`

Expected: `assistant panel checks passed`。

- [ ] **Step 2: 检查差异格式**

Run: `git diff --check`

Expected: 无错误退出，换行符提示不视为失败。

- [ ] **Step 3: 检查功能约束**

确认本次 diff 仅包含 CSS 和新版页面缓存版本，不包含 API 地址、DOM ID、业务 JavaScript 和旧页面删除。
