# 全站雾蓝编辑台视觉改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一全部创作页面为雾蓝编辑台视觉风格，并保留现有业务功能。

**Architecture:** 通过每页 `studio-theme` body 类限定全局主题，再由现有页面类提供局部布局差异。动态生成内容仍沿用现有类名与事件绑定，静态测试只新增主题标记断言。

**Tech Stack:** 原生 HTML、CSS、JavaScript、Node 静态测试、Maven。

## Global Constraints

- 冷白、雾蓝、深蓝灰和单一青蓝强调色构成全站唯一视觉调色板。
- 保持页面路由、导航文本、ID、事件动作和 API 地址不变。
- 桌面保持原有布局语义，窄屏下改为单列或可滚动导航。

---

### Task 1: 新增主题回归断言

**Files:**
- Modify: `novel/src/test/frontend/*.test.js`

- [ ] **Step 1: 为每个页面加入主题类断言**

```js
assert(html.includes('class="studio-theme'));
```

- [ ] **Step 2: 运行对应静态测试并确认旧页面失败**

Run: `node src/test/frontend/<page>.test.js`

Expected: 主题类断言失败，既有动作断言仍通过。

### Task 2: 为所有页面设置主题范围

**Files:**
- Modify: `novel/src/main/resources/templates/index.html`
- Modify: `novel/src/main/resources/static/{storyboard-workbench,novel-detail,asset-library,history-list,video-generation}.html`

- [ ] **Step 1: 在每个 body 增加 `studio-theme` 和页面标识类**

```html
<body class="studio-theme studio-outline-page">
```

- [ ] **Step 2: 运行静态测试**

Run: `node src/test/frontend/<page>.test.js`

Expected: 主题类断言与既有动作断言通过。

### Task 3: 实现共享雾蓝样式与页面级层次

**Files:**
- Modify: `novel/src/main/resources/static/css/outline.css`

- [ ] **Step 1: 添加 `.studio-theme` 颜色 token 与共享控件规则**

```css
.studio-theme { color: #1f2d3d; background: #f4f7fb; }
.studio-theme button { color: #fff; background: #177e9f; }
.studio-theme .secondary-button { color: #216179; background: #e6f1f5; }
```

- [ ] **Step 2: 添加首页、正文、资产库、历史页、工作台和视频页的局部布局样式**

```css
.studio-theme .panel { border-color: #d6e2ea; box-shadow: 0 12px 30px rgba(31, 67, 87, .08); }
.studio-theme .top-nav { background: #fff; border-bottom-color: #d8e4eb; }
```

- [ ] **Step 3: 运行静态测试**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: `video-generation page checks passed`。

### Task 4: 完整验证

**Files:**
- Verify: `novel/src/test/frontend/*.test.js`
- Verify: `novel/pom.xml`

- [ ] **Step 1: 运行全部前端静态测试**

Run: `Get-ChildItem src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }`

Expected: 每个脚本输出检查通过。

- [ ] **Step 2: 运行 Maven 全量测试**

Run: `mvn test`

Expected: 构建成功，失败与错误均为 0。

- [ ] **Step 3: 检查差异空白错误**

Run: `git diff --check`

Expected: 无输出。
